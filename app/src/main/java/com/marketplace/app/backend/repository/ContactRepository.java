package com.marketplace.app.backend.repository;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.ContactRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Rôle : Repository des demandes de contact initiées avec les vendeurs.
 * Entrée : Création d'intention de contact et historique vendeur/acheteur.
 * Sortie : Callbacks ContactRequest.
 */
public class ContactRepository extends BaseRepository<ContactRequest> {

    private final boolean useMock;

    public ContactRepository() {
        this(ApiClient.isUseMock());
    }

    public ContactRepository(boolean useMock) {
        this.useMock = useMock;
    }

    @Override
    public void list(Object filter, Callback<List<ContactRequest>> callback) {
        boolean asSeller = (filter instanceof Boolean) && (Boolean) filter;
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            postSuccess(callback, MockBackendService.getInstance().getContactRequests(uid, asSeller));
            return;
        }

        ApiClient.getApi().getContactRequests(asSeller ? "seller" : "buyer").enqueue(new retrofit2.Callback<List<ContactRequest>>() {
            @Override
            public void onResponse(Call<List<ContactRequest>> call, Response<List<ContactRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<List<ContactRequest>> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void getById(String id, Callback<ContactRequest> callback) {
        postSuccess(callback, null);
    }

    @Override
    public void create(ContactRequest item, Callback<ContactRequest> callback) {
        if (useMock) {
            postSuccess(callback, MockBackendService.getInstance().addContactRequest(item));
            return;
        }

        ApiClient.getApi().createContactRequest(item).enqueue(new retrofit2.Callback<ContactRequest>() {
            @Override
            public void onResponse(Call<ContactRequest> call, Response<ContactRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ContactRequest> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void markAnswered(String id, Callback<ContactRequest> callback) {
        if (useMock) {
            postSuccess(callback, new ContactRequest());
            return;
        }

        ApiClient.getApi().markContactAnswered(id).enqueue(new retrofit2.Callback<ContactRequest>() {
            @Override
            public void onResponse(Call<ContactRequest> call, Response<ContactRequest> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ContactRequest> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void update(ContactRequest item, Callback<ContactRequest> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void delete(String id, Callback<Boolean> callback) {
        postSuccess(callback, true);
    }
}
