package com.marketplace.core.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.marketplace.core.model.Cart;
import com.marketplace.core.model.Category;
import com.marketplace.core.model.CheckoutRequest;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Order;
import com.marketplace.core.model.OrderStatus;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implémentation réelle : dialogue avec le serveur (backend/api) via Retrofit.
 * Même contrat que MockPostgresBackendService. Les règles métier (stock, statuts, niveau du vendeur)
 * sont appliquées par le serveur et la base PostgreSQL ; ce client ne fait que transporter les données.
 */
public class PostgresBackendServiceImpl implements PostgresBackendApi {

    private final RetrofitService retrofitService;

    public PostgresBackendServiceImpl(RetrofitService retrofitService) {
        this.retrofitService = retrofitService;
    }

    // ---------------------------------------------------------------- catalogue

    @Override
    public void getProducts(FilterCriteria filter, ApiCallback<List<Product>> callback) {
        enqueue(retrofitService.getProducts(toQuery(filter)), callback);
    }

    @Override
    public void getProductById(String productId, ApiCallback<Product> callback) {
        enqueue(retrofitService.getProductById(productId), callback);
    }

    @Override
    public void getCategories(ApiCallback<List<Category>> callback) {
        enqueue(retrofitService.getCategories(), callback);
    }

    @Override
    public void createProduct(Product product, ApiCallback<Product> callback) {
        enqueue(retrofitService.createProduct(ProductRequest.from(product)), callback);
    }

    @Override
    public void updateProductModel3d(String productId, String model3dUrl, final ApiCallback<Boolean> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("model3dUrl", model3dUrl);
        retrofitService.updateProductModel3d(productId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(Boolean.TRUE);
                } else {
                    callback.onError(errorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError(networkMessage(t));
            }
        });
    }

    // ------------------------------------------------------------------ profils

    @Override
    public void getUserProfile(String userId, ApiCallback<User> callback) {
        enqueue(retrofitService.getUserProfile(userId), callback);
    }

    @Override
    public void getMyProfile(ApiCallback<User> callback) {
        enqueue(retrofitService.getMyProfile(), callback);
    }

    @Override
    public void updateMyProfile(Map<String, String> fields, ApiCallback<User> callback) {
        enqueue(retrofitService.updateMyProfile(fields), callback);
    }

    // ------------------------------------------------------------------- panier

    @Override
    public void getCart(ApiCallback<Cart> callback) {
        enqueue(retrofitService.getCart(), callback);
    }

    @Override
    public void addToCart(String productId, int quantity, ApiCallback<Cart> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", productId);
        body.put("quantity", quantity);
        enqueue(retrofitService.addToCart(body), callback);
    }

    @Override
    public void setCartItemQuantity(String productId, int quantity, ApiCallback<Cart> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("quantity", quantity);
        enqueue(retrofitService.setCartItemQuantity(productId, body), callback);
    }

    @Override
    public void removeFromCart(String productId, ApiCallback<Cart> callback) {
        enqueue(retrofitService.removeFromCart(productId), callback);
    }

    @Override
    public void clearCart(ApiCallback<Cart> callback) {
        enqueue(retrofitService.clearCart(), callback);
    }

    // ---------------------------------------------------------------- commandes

    @Override
    public void checkout(CheckoutRequest request, final ApiCallback<List<Order>> callback) {
        retrofitService.checkout(request).enqueue(new Callback<CheckoutResponse>() {
            @Override
            public void onResponse(Call<CheckoutResponse> call, Response<CheckoutResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getOrders());
                } else {
                    callback.onError(errorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<CheckoutResponse> call, Throwable t) {
                callback.onError(networkMessage(t));
            }
        });
    }

    @Override
    public void getOrders(boolean asSeller, ApiCallback<List<Order>> callback) {
        enqueue(retrofitService.getOrders(asSeller ? "seller" : "buyer"), callback);
    }

    @Override
    public void getOrderById(String orderId, ApiCallback<Order> callback) {
        enqueue(retrofitService.getOrderById(orderId), callback);
    }

    @Override
    public void updateOrderStatus(String orderId, OrderStatus newStatus, ApiCallback<Order> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("status", newStatus.name());
        enqueue(retrofitService.updateOrderStatus(orderId, body), callback);
    }

    // ---------------------------------------------------------------- utilitaires

    /** Convertit les critères de l'app en paramètres de requête du serveur (mêmes noms que FilterCriteria). */
    static Map<String, String> toQuery(FilterCriteria filter) {
        Map<String, String> query = new HashMap<>();
        if (filter == null) return query;
        if (filter.getCategoryId() != null && !filter.getCategoryId().isEmpty()) {
            query.put("categoryId", filter.getCategoryId());
        }
        if (filter.getMinPrice() != null) query.put("minPrice", String.valueOf(filter.getMinPrice()));
        if (filter.getMaxPrice() != null) query.put("maxPrice", String.valueOf(filter.getMaxPrice()));
        if (filter.isArOnly()) query.put("arOnly", "true");
        if (filter.getSearchQuery() != null && !filter.getSearchQuery().trim().isEmpty()) {
            query.put("searchQuery", filter.getSearchQuery().trim());
        }
        if (filter.getSortBy() != null && !filter.getSortBy().isEmpty()) {
            query.put("sortBy", filter.getSortBy());
        }
        return query;
    }

    private <T> void enqueue(Call<T> call, final ApiCallback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(errorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<T> c, Throwable t) {
                callback.onError(networkMessage(t));
            }
        });
    }

    /** Message lisible : celui du serveur ({"error":{"message":...}}) s'il existe, sinon le code HTTP. */
    static String errorMessage(Response<?> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                String raw = errorBody.string();
                JsonElement root = new JsonParser().parse(raw);
                if (root.isJsonObject()) {
                    JsonObject rootObj = root.getAsJsonObject();
                    JsonElement errorElement = rootObj.get("error");
                    if (errorElement != null) {
                        if (errorElement.isJsonObject()) {
                            JsonObject errorObj = errorElement.getAsJsonObject();
                            if (errorObj.has("message")) {
                                return errorObj.get("message").getAsString();
                            }
                        } else if (errorElement.isJsonPrimitive()) {
                            return errorElement.getAsString();
                        }
                    } else if (rootObj.has("message")) {
                        return rootObj.get("message").getAsString();
                    }
                }
            } catch (Exception ignored) {
                // corps illisible : on retombe sur le code HTTP
            }
        }
        if (response.code() == 401) return "Connexion requise";
        return "Erreur serveur : " + response.code();
    }

    static String networkMessage(Throwable t) {
        String message = t.getMessage();
        return message != null ? "Serveur injoignable : " + message : "Serveur injoignable";
    }
}
