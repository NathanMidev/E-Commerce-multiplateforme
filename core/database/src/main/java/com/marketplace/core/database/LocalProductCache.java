package com.marketplace.core.database;

import com.marketplace.core.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestionnaire de stockage local / cache pour les produits et favoris.
 */
public class LocalProductCache {

    private final Map<String, Product> cache = new HashMap<>();
    private final List<String> favoriteIds = new ArrayList<>();

    public void saveProduct(Product product) {
        if (product != null && product.getId() != null) {
            cache.put(product.getId(), product);
        }
    }

    public Product getProduct(String id) {
        return cache.get(id);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(cache.values());
    }

    public void toggleFavorite(String productId) {
        if (favoriteIds.contains(productId)) {
            favoriteIds.remove(productId);
        } else {
            favoriteIds.add(productId);
        }
    }

    public boolean isFavorite(String productId) {
        return favoriteIds.contains(productId);
    }

    public void clear() {
        cache.clear();
        favoriteIds.clear();
    }
}
