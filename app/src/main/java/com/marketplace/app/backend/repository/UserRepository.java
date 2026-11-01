package com.marketplace.app.backend.repository;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Rôle : Repository de gestion des profils utilisateurs.
 * Entrée : Appels API REST /api/users ou simulation en mémoire.
 * Sortie : Profils User et mise à jour des informations de contact.
 */
public class UserRepository extends BaseRepository<User> {

    private final boolean useMock;

    public UserRepository() {
        this(ApiClient.isUseMock());
    }

    public UserRepository(boolean useMock) {
        this.useMock = useMock;
    }

    @Override
    public void list(Object filter, Callback<List<User>> callback) {
        postSuccess(callback, Collections.emptyList());
    }

    @Override
    public void getById(String id, Callback<User> callback) {
        if (useMock) {
            User user = MockBackendService.getInstance().getOrCreateProfile(id);
            postSuccess(callback, user);
            return;
        }

        ApiClient.getApi().getUserProfile(id).enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
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

    public void getMyProfile(Callback<User> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            if (uid == null) {
                postError(callback, "Connexion requise");
                return;
            }
            User u = MockBackendService.getInstance().getOrCreateProfile(uid);
            AuthSession.getInstance().setCurrentUser(u);
            postSuccess(callback, u);
            return;
        }

        ApiClient.getApi().getMyProfile().enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthSession.getInstance().setCurrentUser(response.body());
                    postSuccess(callback, response.body());
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
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            if (uid == null) {
                postError(callback, "Connexion requise");
                return;
            }
            User u = MockBackendService.getInstance().getOrCreateProfile(uid);
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                switch (entry.getKey()) {
                    case "name": u.setName(entry.getValue()); break;
                    case "whatsapp": u.setWhatsapp(entry.getValue()); break;
                    case "address": u.setAddress(entry.getValue()); break;
                    case "phoneNumber": u.setPhoneNumber(entry.getValue()); break;
                    case "city": u.setCity(entry.getValue()); break;
                    case "facebookLink": u.setFacebookLink(entry.getValue()); break;
                    case "avatarUrl": u.setAvatarUrl(entry.getValue()); break;
                    default:
                        postError(callback, "Champ non modifiable : " + entry.getKey());
                        return;
                }
            }
            AuthSession.getInstance().setCurrentUser(u);
            postSuccess(callback, u);
            return;
        }

        ApiClient.getApi().updateMyProfile(fields).enqueue(new retrofit2.Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthSession.getInstance().setCurrentUser(response.body());
                    postSuccess(callback, response.body());
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
