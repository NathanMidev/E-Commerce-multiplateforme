package com.marketplace.core.network;

import com.marketplace.core.model.Cart;
import com.marketplace.core.model.Category;
import com.marketplace.core.model.CheckoutRequest;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Order;
import com.marketplace.core.model.OrderStatus;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.User;

import java.util.List;
import java.util.Map;

/**
 * Interface du client API pour le serveur backend (PostgreSQL).
 * Le mobile dialogue avec l'API REST (dossier backend/), qui est aussi celle utilisée par la version web :
 * les deux partagent ainsi la même base PostgreSQL.
 */
public interface PostgresBackendApi {

    // ---------------------------------------------------------------- catalogue
    void getProducts(FilterCriteria filter, ApiCallback<List<Product>> callback);
    void getProductById(String productId, ApiCallback<Product> callback);
    void getCategories(ApiCallback<List<Category>> callback);
    void createProduct(Product product, ApiCallback<Product> callback);
    void updateProductModel3d(String productId, String model3dUrl, ApiCallback<Boolean> callback);

    // ------------------------------------------------------------------ profils
    /** Profil public d'un vendeur (les moyens de contact exigent d'être connecté). */
    void getUserProfile(String userId, ApiCallback<User> callback);

    /** Mon profil complet ; il est créé automatiquement à la première connexion. */
    void getMyProfile(ApiCallback<User> callback);

    /**
     * Modifie mon profil. Clés acceptées : name, avatarUrl, city, address, phoneNumber, whatsapp, facebookLink.
     * Les statistiques (ventes, réactivité) et le niveau ne peuvent pas être modifiés par le client.
     */
    void updateMyProfile(Map<String, String> fields, ApiCallback<User> callback);

    // ------------------------------------------------------------------- panier
    void getCart(ApiCallback<Cart> callback);
    /** Ajoute au panier ; la quantité se cumule si le produit y est déjà. */
    void addToCart(String productId, int quantity, ApiCallback<Cart> callback);
    /** Fixe la quantité d'un article (0 = retirer). */
    void setCartItemQuantity(String productId, int quantity, ApiCallback<Cart> callback);
    void removeFromCart(String productId, ApiCallback<Cart> callback);
    void clearCart(ApiCallback<Cart> callback);

    // ---------------------------------------------------------------- commandes
    /** Passe commande : une commande est créée par vendeur, le stock est réservé et le panier vidé. */
    void checkout(CheckoutRequest request, ApiCallback<List<Order>> callback);
    /** Mes commandes : achats (asSeller = false) ou ventes (asSeller = true). */
    void getOrders(boolean asSeller, ApiCallback<List<Order>> callback);
    void getOrderById(String orderId, ApiCallback<Order> callback);
    /** Fait avancer une commande selon le rôle (vendeur : confirmer/expédier ; acheteur : confirmer la réception). */
    void updateOrderStatus(String orderId, OrderStatus newStatus, ApiCallback<Order> callback);

    interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
}
