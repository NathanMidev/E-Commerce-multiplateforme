package com.marketplace.app.feature.auth;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rôle : Gestionnaire sécurisé de la photo de profil dans Firebase Storage (users/{uid}/avatar/profile.jpg),
 * de l'unicité et de la persistance du username dans Firestore (user_profiles/{uid} & usernames/{username}),
 * et de la synchronisation avec le profil FirebaseAuth (UserProfileChangeRequest).
 *
 * Sécurité :
 * - Aucun mot de passe, jeton JWT ou credential sensible n'est stocké ni journalisé.
 * - Chaque utilisateur ne peut modifier ou supprimer que ses propres fichiers et métadonnées (verifyUserOwnership).
 */
public class ProfileStorageService {

    public static final String COLLECTION_USER_PROFILES = "user_profiles";
    public static final String COLLECTION_USERNAMES = "usernames";
    public static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024L * 1024L; // 5 Mo max

    public interface PhotoUploadCallback {
        void onProgress(int percent);
        void onSuccess(String photoUrl);
        void onError(String message);
    }

    public interface PhotoDeleteCallback {
        void onSuccess();
        void onError(String message);
    }

    private final boolean useSimulatedStorage;

    // Cache et stockage simulé pour le mode démo et les tests unitaires JVM
    private static final Map<String, String> USERNAME_TO_UID = new ConcurrentHashMap<>();
    private static final Map<String, String> UID_TO_USERNAME = new ConcurrentHashMap<>();
    private static final Map<String, String> UID_TO_AVATAR_URL = new ConcurrentHashMap<>();

    public ProfileStorageService() {
        this(ApiClient.isUseMock());
    }

    public ProfileStorageService(boolean useSimulatedStorage) {
        this.useSimulatedStorage = useSimulatedStorage;
    }

    public static void resetSimulatedState() {
        USERNAME_TO_UID.clear();
        UID_TO_USERNAME.clear();
        UID_TO_AVATAR_URL.clear();
        // Pré-enregistrement des usernames des vendeurs de démonstration
        USERNAME_TO_UID.put("atelier.nordique", "seller_101");
        UID_TO_USERNAME.put("seller_101", "atelier.nordique");
        USERNAME_TO_UID.put("luminaires.co", "seller_102");
        UID_TO_USERNAME.put("seller_102", "luminaires.co");
        USERNAME_TO_UID.put("sneakershub", "seller_103");
        UID_TO_USERNAME.put("seller_103", "sneakershub");
    }

    /**
     * Construit le chemin Firebase Storage strict pour la photo de profil d'un utilisateur.
     */
    public static String buildAvatarStoragePath(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifiant utilisateur (uid) obligatoire");
        }
        return "users/" + uid.trim() + "/avatar/profile.jpg";
    }

    /**
     * Vérifie que l'utilisateur connecté ne modifie que ses propres informations et fichiers (Section 6 & 8.10).
     */
    public static void verifyUserOwnership(String authenticatedUid, String targetUid) throws SecurityException {
        if (authenticatedUid == null || authenticatedUid.trim().isEmpty()) {
            throw new SecurityException("Authentification requise (401) pour modifier un profil ou une photo");
        }
        if (targetUid == null || targetUid.trim().isEmpty()) {
            throw new SecurityException("Identifiant cible (uid) manquant");
        }
        if (!authenticatedUid.trim().equals(targetUid.trim())) {
            throw new SecurityException("Accès refusé (403) : vous ne pouvez modifier ou supprimer que votre propre profil et photo");
        }
    }

    /**
     * Vérifie si un username est valide et disponible pour l'utilisateur donné.
     */
    public static boolean isUsernameAvailable(String rawUsername, String currentUid) {
        String err = ProfileValidator.validateUsername(rawUsername);
        if (err != null) return false;
        String normalized = ProfileValidator.normalizeUsername(rawUsername);
        String ownerUid = USERNAME_TO_UID.get(normalized);
        return ownerUid == null || (currentUid != null && ownerUid.equals(currentUid.trim()));
    }

    /**
     * Réserve et associe un username unique à un utilisateur.
     */
    public void saveUsernameForUser(String targetUid, String rawUsername) throws IllegalArgumentException, SecurityException {
        String currentUid = AuthSession.getInstance().getUserId();
        verifyUserOwnership(currentUid, targetUid);

        String formatErr = ProfileValidator.validateUsername(rawUsername);
        if (formatErr != null) {
            throw new IllegalArgumentException(formatErr);
        }
        String normalized = ProfileValidator.normalizeUsername(rawUsername);
        if (!isUsernameAvailable(normalized, targetUid)) {
            throw new IllegalArgumentException("Ce nom d'utilisateur (@" + normalized + ") est déjà utilisé par un autre compte");
        }

        // Libérer l'ancien username s'il a changé
        String previous = UID_TO_USERNAME.get(targetUid.trim());
        if (previous != null && !previous.equals(normalized)) {
            USERNAME_TO_UID.remove(previous);
        }
        USERNAME_TO_UID.put(normalized, targetUid.trim());
        UID_TO_USERNAME.put(targetUid.trim(), normalized);

        if (!useSimulatedStorage) {
            try {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                Map<String, Object> profileMeta = new HashMap<>();
                profileMeta.put("uid", targetUid.trim());
                profileMeta.put("username", normalized);
                profileMeta.put("updatedAt", System.currentTimeMillis());
                firestore.collection(COLLECTION_USER_PROFILES).document(targetUid.trim()).set(profileMeta);

                Map<String, Object> reservation = new HashMap<>();
                reservation.put("uid", targetUid.trim());
                reservation.put("username", normalized);
                reservation.put("updatedAt", System.currentTimeMillis());
                firestore.collection(COLLECTION_USERNAMES).document(normalized).set(reservation);
            } catch (Throwable ignored) {
            }
        }
    }

    public static String getStoredUsername(String uid) {
        if (uid == null) return null;
        return UID_TO_USERNAME.get(uid.trim());
    }

    public static void seedUsername(String uid, String username) {
        if (uid == null || username == null || username.trim().isEmpty()) return;
        String norm = ProfileValidator.normalizeUsername(username);
        UID_TO_USERNAME.put(uid.trim(), norm);
        USERNAME_TO_UID.put(norm, uid.trim());
    }

    /**
     * Téléverse ou remplace la photo de profil d'un utilisateur dans Firebase Storage (users/{uid}/avatar/profile.jpg).
     */
    public void uploadOrReplaceProfilePhoto(
            String targetUid,
            Uri localImageUri,
            byte[] imageBytes,
            PhotoUploadCallback callback
    ) {
        String currentUid = AuthSession.getInstance().getUserId();
        try {
            verifyUserOwnership(currentUid, targetUid);
        } catch (SecurityException se) {
            if (callback != null) callback.onError(se.getMessage());
            return;
        }

        if (imageBytes != null && imageBytes.length > MAX_AVATAR_SIZE_BYTES) {
            if (callback != null) callback.onError("La photo de profil dépasse la taille maximale autorisée (5 Mo)");
            return;
        }
        if (localImageUri == null && (imageBytes == null || imageBytes.length == 0)) {
            if (callback != null) callback.onError("Aucune image sélectionnée");
            return;
        }

        String storagePath = buildAvatarStoragePath(targetUid);

        if (useSimulatedStorage) {
            if (callback != null) callback.onProgress(50);
            String simulatedUrl = "https://storage.firebase.local/" + storagePath + "?v=" + System.currentTimeMillis();
            UID_TO_AVATAR_URL.put(targetUid.trim(), simulatedUrl);
            syncFirebaseUserProfile(null, simulatedUrl);
            if (callback != null) {
                callback.onProgress(100);
                callback.onSuccess(simulatedUrl);
            }
            return;
        }

        try {
            StorageReference avatarRef = FirebaseStorage.getInstance().getReference().child(storagePath);
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("uid", targetUid.trim())
                    .build();

            UploadTask uploadTask = (localImageUri != null)
                    ? avatarRef.putFile(localImageUri, metadata)
                    : avatarRef.putBytes(imageBytes, metadata);

            uploadTask.addOnProgressListener(snapshot -> {
                long total = snapshot.getTotalByteCount();
                if (total > 0 && callback != null) {
                    int pct = (int) Math.min(95, (snapshot.getBytesTransferred() * 100) / total);
                    callback.onProgress(pct);
                }
            }).addOnSuccessListener(taskSnapshot -> avatarRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String photoUrl = downloadUri.toString();
                UID_TO_AVATAR_URL.put(targetUid.trim(), photoUrl);
                syncFirebaseUserProfile(null, photoUrl);
                if (callback != null) {
                    callback.onProgress(100);
                    callback.onSuccess(photoUrl);
                }
            })).addOnFailureListener(e -> {
                if (callback != null) callback.onError("Échec de l'envoi de la photo vers Firebase Storage : " + e.getMessage());
            });
        } catch (Exception e) {
            if (callback != null) callback.onError("Erreur Firebase Storage : " + e.getMessage());
        }
    }

    /**
     * Supprime la photo de profil d'un utilisateur de Firebase Storage et réinitialise la référence à l'image par défaut.
     */
    public void deleteProfilePhoto(String targetUid, PhotoDeleteCallback callback) {
        String currentUid = AuthSession.getInstance().getUserId();
        try {
            verifyUserOwnership(currentUid, targetUid);
        } catch (SecurityException se) {
            if (callback != null) callback.onError(se.getMessage());
            return;
        }

        UID_TO_AVATAR_URL.remove(targetUid.trim());

        if (useSimulatedStorage) {
            if (callback != null) callback.onSuccess();
            return;
        }

        try {
            String storagePath = buildAvatarStoragePath(targetUid);
            StorageReference avatarRef = FirebaseStorage.getInstance().getReference().child(storagePath);
            avatarRef.delete().addOnCompleteListener(task -> {
                if (callback != null) callback.onSuccess();
            });
        } catch (Exception e) {
            if (callback != null) callback.onError("Erreur lors de la suppression de la photo : " + e.getMessage());
        }
    }

    /**
     * Synchronise le displayName et/ou la photoURL sur l'objet FirebaseUser de Firebase Authentication
     * sans jamais écraser une propriété existante lorsqu'une valeur n'est pas fournie (Section 6).
     */
    public static void syncFirebaseUserProfile(String displayName, String photoUrl) {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth == null) return;
            FirebaseUser fbUser = auth.getCurrentUser();
            if (fbUser == null) return;

            UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
            boolean changed = false;

            if (displayName != null && !displayName.trim().isEmpty()) {
                builder.setDisplayName(displayName.trim());
                changed = true;
            }
            if (photoUrl != null) {
                if (photoUrl.trim().isEmpty()) {
                    builder.setPhotoUri(null);
                } else {
                    builder.setPhotoUri(Uri.parse(photoUrl.trim()));
                }
                changed = true;
            }
            if (changed) {
                fbUser.updateProfile(builder.build());
            }
        } catch (Throwable ignored) {
            // En environnement de test JVM unitaire sans Google Play Services, ignorer proprement
        }
    }
}
