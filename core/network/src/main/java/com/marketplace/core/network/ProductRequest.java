package com.marketplace.core.network;

import com.marketplace.core.model.Product;
import com.marketplace.core.model.ProductStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Corps de POST /products. Le serveur refuse les champs inconnus : on n'envoie donc ni id, ni vendeur,
 * ni date (le vendeur est celui du jeton d'authentification, l'id est généré par la base).
 */
class ProductRequest {
    private final String title;
    private final String description;
    private final double price;
    private final String categoryId;
    private final String thumbnailUrl;
    private final List<String> imageUrls;
    private final String model3dUrl;
    private final int stock;
    private final ProductStatus status;

    private ProductRequest(Product p) {
        this.title = p.getTitle();
        this.description = p.getDescription();
        this.price = p.getPrice();
        this.categoryId = blankToNull(p.getCategoryId());
        this.thumbnailUrl = blankToNull(p.getThumbnailUrl());
        this.imageUrls = p.getImageUrls() != null ? new ArrayList<>(p.getImageUrls()) : new ArrayList<String>();
        this.model3dUrl = blankToNull(p.getModel3dUrl());
        this.stock = p.getStock();
        this.status = p.getStatus();
    }

    static ProductRequest from(Product product) {
        return new ProductRequest(product);
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s;
    }
}
