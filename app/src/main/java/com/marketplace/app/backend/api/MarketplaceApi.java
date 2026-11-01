package com.marketplace.app.backend.api;

import com.marketplace.app.model.Cart;
import com.marketplace.app.model.Category;
import com.marketplace.app.model.ContactRequest;
import com.marketplace.app.model.Order;
import com.marketplace.app.model.Payment;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.User;

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

/**
 * Rôle : Définition des endpoints REST Retrofit conformes à backend/README.md.
 * Entrée : Appels HTTP avec sérialisation JSON Gson.
 * Sortie : Objets Call<T> Retrofit.
 */
public interface MarketplaceApi {

    // --- Santé
    @GET("health")
    Call<Map<String, String>> health();

    // --- Catégories & Produits
    @GET("categories")
    Call<List<Category>> getCategories();

    @GET("products")
    Call<List<Product>> getProducts(@QueryMap Map<String, String> filters);

    @GET("products/{id}")
    Call<Product> getProductById(@Path("id") String productId);

    @POST("products")
    Call<Product> createProduct(@Body Product product);

    @PATCH("products/{id}")
    Call<Product> updateProduct(@Path("id") String productId, @Body Map<String, Object> body);

    @PATCH("products/{id}/model3d")
    Call<Void> updateProductModel3d(@Path("id") String productId, @Body Map<String, String> body);

    @DELETE("products/{id}")
    Call<Void> deleteProduct(@Path("id") String productId);

    // --- Profils Utilisateurs
    @GET("users/me")
    Call<User> getMyProfile();

    @PATCH("users/me")
    Call<User> updateMyProfile(@Body Map<String, String> fields);

    @GET("users/{id}")
    Call<User> getUserProfile(@Path("id") String userId);

    // --- Panier
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

    // --- Commandes
    @POST("orders/checkout")
    Call<Map<String, List<Order>>> checkout(@Body Map<String, Object> request);

    @GET("orders")
    Call<List<Order>> getOrders(@Query("role") String role, @Query("status") String status);

    @GET("orders/{id}")
    Call<Order> getOrderById(@Path("id") String orderId);

    @PATCH("orders/{id}/status")
    Call<Order> updateOrderStatus(@Path("id") String orderId, @Body Map<String, String> body);

    // --- Contacts (Extensions contractuelles)
    @POST("contact-requests")
    Call<ContactRequest> createContactRequest(@Body ContactRequest request);

    @GET("contact-requests")
    Call<List<ContactRequest>> getContactRequests(@Query("role") String role);

    @PATCH("contact-requests/{id}/answered")
    Call<ContactRequest> markContactAnswered(@Path("id") String id);

    // --- Paiements (Extensions contractuelles)
    @POST("orders/{orderId}/payment")
    Call<Payment> createPayment(@Path("orderId") String orderId, @Body Payment payment);

    @GET("orders/{orderId}/payment")
    Call<Payment> getPayment(@Path("orderId") String orderId);

    @PATCH("orders/{orderId}/payment")
    Call<Payment> updatePaymentStatus(@Path("orderId") String orderId, @Body Map<String, String> body);
}
