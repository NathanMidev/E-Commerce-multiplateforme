package com.marketplace.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Product implements Serializable {
    private String id;
    private String title;
    private String description;
    private double price;
    private ProductStatus status;
    private String categoryId;
    private String sellerId;
    private String sellerName;
    private String thumbnailUrl;
    private boolean modele3d;       // Indique si le modèle 3D existe (issu du scan ou non)
    private String model3dUrl;      // URL du fichier .glb / .gltf
    private List<String> imageUrls;
    private int stock;
    private long createdAt;

    public Product() {
        this.status = ProductStatus.NOUVEAU;
        this.modele3d = false;
        this.imageUrls = new ArrayList<>();
    }

    public Product(String id, String title, String description, double price,
                   ProductStatus status, String categoryId, String sellerId, String sellerName,
                   String thumbnailUrl, boolean modele3d, String model3dUrl,
                   List<String> imageUrls, int stock, long createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.status = status != null ? status : ProductStatus.NOUVEAU;
        this.categoryId = categoryId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.thumbnailUrl = thumbnailUrl;
        this.modele3d = modele3d;
        this.model3dUrl = model3dUrl;
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

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public boolean isModele3d() { return modele3d; }
    public boolean hasModele3d() { return modele3d; }
    public void setModele3d(boolean modele3d) { this.modele3d = modele3d; }

    // Compatibilité
    public boolean isArSupported() { return modele3d; }
    public void setArSupported(boolean arSupported) { this.modele3d = arSupported; }

    public String getModel3dUrl() { return model3dUrl; }
    public void setModel3dUrl(String model3dUrl) { 
        this.model3dUrl = model3dUrl; 
        this.modele3d = (model3dUrl != null && !model3dUrl.trim().isEmpty());
    }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public int getStock() { return stock; }
    public void setStock(int stock) { 
        this.stock = stock;
        if (stock <= 0 && this.status != ProductStatus.EPUISE) {
            this.status = ProductStatus.EPUISE;
        }
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
