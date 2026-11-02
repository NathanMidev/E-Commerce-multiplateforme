package com.marketplace.app.backend.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.auth.FirebaseAuthSession;
import com.marketplace.app.feature.auth.PhoneNormalizer;
import com.marketplace.app.feature.auth.ProfileStorageService;
import com.marketplace.app.feature.auth.ProfileValidator;
import com.marketplace.app.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Rôle : Repository de gestion des profils utilisateurs.
 * Entrée : Appels API REST /api/users, enrichissement FirebaseAuth et métadonnées Firestore (username).
 * Sortie : Profils User complets et mise à jour sécurisée des informations de contact.
 */
public class UserRepository extends BaseRepository<User> {

    private final boolean useMock;
    private final ProfileStorageService profileStorageService;

    public UserRepository() {
        this(ApiClient.isUseMock());
    }

    public UserRepository(boolean useMock) {
        this.useMock = useMock;
        this.profileStorageService = new ProfileStorageService(useMock);
    }

    @Override
    public void list(Object filter, Callback<List<User>> callback) {
        postSuccess(callback, Collections.emptyList());
    }

    private void enrichUserMetadata(User user, boolean isSelf) {
        if (user == null) return;
        String storedUsername = ProfileStorageService.getStoredUsername(user.getId());
        if ((user.getUsername() == null || user.getUsername().trim().isEmpty())
                && storedUsername != null && !storedUsername.trim().isEmpty()) {
            user.setUsername(storedUsername);
        } else if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            ProfileStorageService.seedUsername(user.getId(), user.getUsername());
        }
        if (isSelf) {
            try {
                FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                FirebaseAuthSession.mergeFirebaseIdentityIntoUser(fbUser, user);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void getById(String id, Callback<User> callback) {
        if (useMock || "seller_bubble_design".equals(id)) {
            User user = MockBackendService.getInstance().getOrCreateProfile(id);
            enrichUserMetadata(user, false);
            postSuccess(callback, user);
            return;
        }

        ApiClient.getApi().getUserProfile(id).enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    enrichUserMetadata(user, false);
                    postSuccess(callback, user);
                } else {
                    User fallback = MockBackendService.getInstance().getOrCreateProfile(id);
                    enrichUserMetadata(fallback, false);
                    postSuccess(callback, fallback);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                User fallback = MockBackendService.getInstance().getOrCreateProfile(id);
                enrichUserMetadata(fallback, false);
                postSuccess(callback, fallback);
            }
        });
    }

    public void getMyProfile(Callback<User> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            if (uid == null || uid.trim().isEmpty()) {
                postError(callback, "Connexion requise");
                return;
            }
            User u = MockBackendService.getInstance().getOrCreateProfile(uid);
            enrichUserMetadata(u, true);
            AuthSession.getInstance().setCurrentUser(u);
            postSuccess(callback, u);
            return;
        }

        ApiClient.getApi().getMyProfile().enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User u = response.body();
                    enrichUserMetadata(u, true);
                    AuthSession.getInstance().setCurrentUser(u);
                    postSuccess(callback, u);
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void updateMyProfile(Map<String, String> fields, Callback<User> callback) {
        String uid = AuthSession.getInstance().getUserId();
        if (uid == null || uid.trim().isEmpty()) {
            postError(callback, "Connexion requise");
            return;
        }
        if (fields == null || fields.isEmpty()) {
            postError(callback, "Aucun champ à modifier");
            return;
        }

        // 1. Validation et sauvegarde du username (distinct de name/displayName)
        String requestedUsername = fields.containsKey("username") ? fields.get("username") : null;
        if (requestedUsername != null && !requestedUsername.trim().isEmpty()) {
            try {
                profileStorageService.saveUsernameForUser(uid, requestedUsername);
            } catch (Exception e) {
                postError(callback, e.getMessage());
                return;
            }
        }

        // 2. Validation et normalisation E.164 du numéro unique WhatsApp
        String rawWhatsapp = fields.containsKey("whatsapp")
                ? fields.get("whatsapp")
                : fields.get("whatsappNumber");
        String normalizedWhatsapp = null;
        if (rawWhatsapp != null && !rawWhatsapp.trim().isEmpty()) {
            String dialCode = fields.getOrDefault("dialCode", PhoneNormalizer.DEFAULT_DIAL_CODE);
            normalizedWhatsapp = PhoneNormalizer.normalizeToE164(dialCode, rawWhatsapp);
            if (normalizedWhatsapp == null) {
                postError(callback, "Numéro WhatsApp invalide : veuillez saisir un numéro international valide");
                return;
            }
        }

        // 3. Validation et normalisation du lien Facebook (facultatif, "" autorisé pour suppression)
        boolean hasFacebookField = fields.containsKey("facebookLink") || fields.containsKey("facebookUrl");
        String rawFacebook = fields.containsKey("facebookLink")
                ? fields.get("facebookLink")
                : fields.get("facebookUrl");
        String normalizedFacebook = null;
        if (hasFacebookField) {
            if (rawFacebook == null || rawFacebook.trim().isEmpty()) {
                normalizedFacebook = ""; // Suppression explicite du lien Facebook
            } else {
                String fbErr = ProfileValidator.validateFacebookUrl(rawFacebook);
                if (fbErr != null) {
                    postError(callback, fbErr);
                    return;
                }
                normalizedFacebook = ProfileValidator.normalizeFacebookUrl(rawFacebook);
            }
        }

        // 4. Construction de la requête stricte compatible avec PATCH /api/users/me du backend REST
        Map<String, String> backendFields = new HashMap<>();

        String displayName = fields.containsKey("name") ? fields.get("name") : fields.get("displayName");
        if (displayName != null && !displayName.trim().isEmpty()) {
            backendFields.put("name", displayName.trim());
        }

        boolean hasAvatarField = fields.containsKey("avatarUrl") || fields.containsKey("photoUrl");
        String avatarVal = fields.containsKey("avatarUrl") ? fields.get("avatarUrl") : fields.get("photoUrl");
        if (hasAvatarField && avatarVal != null) {
            backendFields.put("avatarUrl", avatarVal.trim());
        }

        if (fields.containsKey("city") && fields.get("city") != null) {
            backendFields.put("city", fields.get("city").trim());
        }
        if (fields.containsKey("address") && fields.get("address") != null) {
            backendFields.put("address", fields.get("address").trim());
        }

        if (normalizedWhatsapp != null) {
            // Un seul numéro : WhatsApp alimente à la fois whatsapp et phoneNumber pour tel: et wa.me
            backendFields.put("whatsapp", normalizedWhatsapp);
            backendFields.put("phoneNumber", normalizedWhatsapp);
        } else if (fields.containsKey("phoneNumber") && fields.get("phoneNumber") != null
                && !fields.get("phoneNumber").trim().isEmpty()) {
            String normPhone = PhoneNormalizer.normalizeToE164(PhoneNormalizer.DEFAULT_DIAL_CODE, fields.get("phoneNumber"));
            if (normPhone == null) {
                postError(callback, "Numéro de téléphone invalide");
                return;
            }
            backendFields.put("whatsapp", normPhone);
            backendFields.put("phoneNumber", normPhone);
        }

        if (hasFacebookField && normalizedFacebook != null) {
            backendFields.put("facebookLink", normalizedFacebook);
        }

        // Synchroniser FirebaseUser (displayName & photoURL) sans écraser les propriétés non fournies
        ProfileStorageService.syncFirebaseUserProfile(
                backendFields.get("name"),
                hasAvatarField ? backendFields.get("avatarUrl") : null
        );

        if (useMock) {
            User u = MockBackendService.getInstance().getOrCreateProfile(uid);
            for (Map.Entry<String, String> entry : backendFields.entrySet()) {
                switch (entry.getKey()) {
                    case "name":
                        u.setName(entry.getValue());
                        break;
                    case "whatsapp":
                        u.setWhatsapp(entry.getValue());
                        break;
                    case "phoneNumber":
                        u.setPhoneNumber(entry.getValue());
                        break;
                    case "address":
                        u.setAddress(entry.getValue());
                        break;
                    case "city":
                        u.setCity(entry.getValue());
                        break;
                    case "facebookLink":
                        u.setFacebookLink(entry.getValue().isEmpty() ? null : entry.getValue());
                        break;
                    case "avatarUrl":
                        u.setAvatarUrl(entry.getValue().isEmpty() ? null : entry.getValue());
                        break;
                    default:
                        break;
                }
            }
            if (requestedUsername != null && !requestedUsername.trim().isEmpty()) {
                u.setUsername(requestedUsername);
            }
            enrichUserMetadata(u, true);
            AuthSession.getInstance().setCurrentUser(u);
            postSuccess(callback, u);
            return;
        }

        if (backendFields.isEmpty()) {
            // Seul le username a été modifié
            getMyProfile(callback);
            return;
        }

        ApiClient.getApi().updateMyProfile(backendFields).enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User updated = response.body();
                    enrichUserMetadata(updated, true);
                    AuthSession.getInstance().setCurrentUser(updated);
                    postSuccess(callback, updated);
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void create(User item, Callback<User> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void update(User item, Callback<User> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void delete(String id, Callback<Boolean> callback) {
        postSuccess(callback, true);
    }
}
