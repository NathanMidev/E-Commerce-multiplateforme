package com.marketplace.app.backend.repository;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Cart;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Rôle : Repository de gestion du panier d'achat.
 * Entrée : Ajout, modification de quantité, suppression et vidage.
 * Sortie : Panier Cart mis à jour.
 */
public class CartRepository extends BaseRepository<Cart> {

    private final boolean useMock;

    public CartRepository() {
        this(false);
    }

    public CartRepository(boolean useMock) {
        this.useMock = useMock;
    }

    @Override
    public void list(Object filter, Callback<List<Cart>> callback) {
        postSuccess(callback, Collections.emptyList());
    }

    @Override
    public void getById(String id, Callback<Cart> callback) {
        getCart(callback);
    }

    public void getCart(Callback<Cart> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            if (uid == null) {
                postError(callback, "Connexion requise");
                return;
            }
            postSuccess(callback, MockBackendService.getInstance().getCart(uid));
            return;
        }

        ApiClient.getApi().getCart().enqueue(new retrofit2.Callback<Cart>() {
            @Override
            public void onResponse(Call<Cart> call, Response<Cart> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void addItem(String productId, int quantity, Callback<Cart> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            try {
                Cart cart = MockBackendService.getInstance().addToCart(uid, productId, quantity);
                postSuccess(callback, cart);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("productId", productId);
        body.put("quantity", quantity);
        ApiClient.getApi().addToCart(body).enqueue(new retrofit2.Callback<Cart>() {
            @Override
            public void onResponse(Call<Cart> call, Response<Cart> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void setItemQuantity(String productId, int quantity, Callback<Cart> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            try {
                Cart cart = MockBackendService.getInstance().setCartItemQuantity(uid, productId, quantity);
                postSuccess(callback, cart);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("quantity", quantity);
        ApiClient.getApi().setCartItemQuantity(productId, body).enqueue(new retrofit2.Callback<Cart>() {
            @Override
            public void onResponse(Call<Cart> call, Response<Cart> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void removeItem(String productId, Callback<Cart> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            postSuccess(callback, MockBackendService.getInstance().removeFromCart(uid, productId));
            return;
        }

        ApiClient.getApi().removeFromCart(productId).enqueue(new retrofit2.Callback<Cart>() {
            @Override
            public void onResponse(Call<Cart> call, Response<Cart> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void clear(Callback<Cart> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            postSuccess(callback, MockBackendService.getInstance().clearCart(uid));
            return;
        }

        ApiClient.getApi().clearCart().enqueue(new retrofit2.Callback<Cart>() {
            @Override
            public void onResponse(Call<Cart> call, Response<Cart> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void create(Cart item, Callback<Cart> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void update(Cart item, Callback<Cart> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void delete(String id, Callback<Boolean> callback) {
        clear(new Callback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                postSuccess(callback, true);
            }

            @Override
            public void onError(String message) {
                postError(callback, message);
            }
        });
    }
}
