package com.marketplace.core.network;

import com.marketplace.core.model.Cart;
import com.marketplace.core.model.Category;
import com.marketplace.core.model.CheckoutRequest;
import com.marketplace.core.model.Order;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/** Routes REST du serveur (dossier backend/api). Base : http://hôte:8080/api/ */
public interface RetrofitService {

    // catalogue (public)
    @GET("products")
    Call<List<Product>> getProducts(@QueryMap Map<String, String> filters);

    @GET("products/{id}")
    Call<Product> getProductById(@Path("id") String productId);

    @GET("categories")
    Call<List<Category>> getCategories();

    // catalogue (vendeur connecté)
    @POST("products")
    Call<Product> createProduct(@Body ProductRequest product);

    @PATCH("products/{id}/model3d")
    Call<Void> updateProductModel3d(@Path("id") String productId, @Body Map<String, String> body);

    // profils
    @GET("users/{id}")
    Call<User> getUserProfile(@Path("id") String userId);

    @GET("users/me")
    Call<User> getMyProfile();

    @PATCH("users/me")
    Call<User> updateMyProfile(@Body Map<String, String> fields);

    // panier
    @GET("cart")
    Call<Cart> getCart();

    @POST("cart/items")
    Call<Cart> addToCart(@Body Map<String, Object> body);

    @PUT("cart/items/{productId}")
    Call<Cart> setCartItemQuantity(@Path("productId") String productId, @Body Map<String, Object> body);

    @DELETE("cart/items/{productId}")
    Call<Cart> removeFromCart(@Path("productId") String productId);

    @DELETE("cart")
    Call<Cart> clearCart();

    // commandes
    @POST("orders/checkout")
    Call<CheckoutResponse> checkout(@Body CheckoutRequest request);

    @GET("orders")
    Call<List<Order>> getOrders(@Query("role") String role);

    @GET("orders/{id}")
    Call<Order> getOrderById(@Path("id") String orderId);

    @PATCH("orders/{id}/status")
    Call<Order> updateOrderStatus(@Path("id") String orderId, @Body Map<String, String> body);
}
