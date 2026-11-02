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
        this(ApiClient.isUseMock());
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
        final String uid = AuthSession.getInstance().getUserId();
        if (useMock) {
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
                } else if (uid != null) {
                    postSuccess(callback, MockBackendService.getInstance().getCart(uid));
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                if (uid != null) {
                    postSuccess(callback, MockBackendService.getInstance().getCart(uid));
                } else {
                    postError(callback, parseNetworkError(t));
                }
            }
        });
    }

    public void addItem(String productId, int quantity, Callback<Cart> callback) {
        final String uid = AuthSession.getInstance().getUserId();
        if (useMock) {
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
                    try {
                        postSuccess(callback, MockBackendService.getInstance().addToCart(uid, productId, quantity));
                    } catch (Exception e) {
                        postError(callback, e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                try {
                    postSuccess(callback, MockBackendService.getInstance().addToCart(uid, productId, quantity));
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        });
    }

    public void setItemQuantity(String productId, int quantity, Callback<Cart> callback) {
        final String uid = AuthSession.getInstance().getUserId();
        if (useMock) {
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
                    try {
                        postSuccess(callback, MockBackendService.getInstance().setCartItemQuantity(uid, productId, quantity));
                    } catch (Exception e) {
                        postError(callback, e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                try {
                    postSuccess(callback, MockBackendService.getInstance().setCartItemQuantity(uid, productId, quantity));
                } catch (Exception e) {
                    postError(callback, e.getMessage());
                }
            }
        });
    }

    public void removeItem(String productId, Callback<Cart> callback) {
        final String uid = AuthSession.getInstance().getUserId();
        if (useMock) {
            postSuccess(callback, MockBackendService.getInstance().removeFromCart(uid, productId));
            return;
        }

        ApiClient.getApi().removeFromCart(productId).enqueue(new retrofit2.Callback<Cart>() {
            @Override
            public void onResponse(Call<Cart> call, Response<Cart> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postSuccess(callback, MockBackendService.getInstance().removeFromCart(uid, productId));
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                postSuccess(callback, MockBackendService.getInstance().removeFromCart(uid, productId));
            }
        });
    }

    public void clear(Callback<Cart> callback) {
        final String uid = AuthSession.getInstance().getUserId();
        if (useMock) {
            postSuccess(callback, MockBackendService.getInstance().clearCart(uid));
            return;
        }

        ApiClient.getApi().clearCart().enqueue(new retrofit2.Callback<Cart>() {
            @Override
            public void onResponse(Call<Cart> call, Response<Cart> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postSuccess(callback, MockBackendService.getInstance().clearCart(uid));
                }
            }

            @Override
            public void onFailure(Call<Cart> call, Throwable t) {
                postSuccess(callback, MockBackendService.getInstance().clearCart(uid));
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
