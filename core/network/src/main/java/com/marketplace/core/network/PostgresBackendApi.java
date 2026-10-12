package com.marketplace.core.network;

import com.marketplace.core.model.Category;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.User;

import java.util.List;

/**
 * Interface du client API pour le serveur backend (PostgreSQL).
 * Dans une architecture mobile sécurisée, le mobile dialogue avec un serveur d'API REST/gRPC
 * qui exécute les requêtes SQL dans la base PostgreSQL.
 */
public interface PostgresBackendApi {
    void getProducts(FilterCriteria filter, ApiCallback<List<Product>> callback);
    void getProductById(String productId, ApiCallback<Product> callback);
    void getCategories(ApiCallback<List<Category>> callback);
    void createProduct(Product product, ApiCallback<Product> callback);
    void updateProductModel3d(String productId, String model3dUrl, ApiCallback<Boolean> callback);
    void getUserProfile(String userId, ApiCallback<User> callback);

    interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
}
