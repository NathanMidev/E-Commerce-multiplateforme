package com.marketplace.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Product implements Serializable {
    private String id;
    private String title;
    private String description;
    private double price;
    private String categoryId;
    private String sellerId;
    private String sellerName;
    private String model3dUrl;      // URL du fichier .glb / .gltf reconstruit par SAM
    private boolean arSupported;    // Si le produit est visualisable en Réalité Augmentée
    private List<String> imageUrls;
    private int stock;
    private long createdAt;

    public Product() {
        this.imageUrls = new ArrayList<>();
    }

    public Product(String id, String title, String description, double price,
                   String categoryId, String sellerId, String sellerName,
                   String model3dUrl, boolean arSupported, List<String> imageUrls,
                   int stock, long createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.model3dUrl = model3dUrl;
        this.arSupported = arSupported;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.stock = stock;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getModel3dUrl() { return model3dUrl; }
    public void setModel3dUrl(String model3dUrl) { this.model3dUrl = model3dUrl; }

    public boolean isArSupported() { return arSupported; }
    public void setArSupported(boolean arSupported) { this.arSupported = arSupported; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
