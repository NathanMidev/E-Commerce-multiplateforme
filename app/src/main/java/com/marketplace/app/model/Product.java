package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Entité Produit de la marketplace locale (avec support images multiples et modèle 3D GLB / ARCore).
 * Entrée : Champs JSON conformes au backend /api/products et métadonnées 3D Firebase.
 * Sortie : Validation métier et getters/setters complets.
 */
public class Product extends BaseModel {
    private String title;
    private String description;
    private double price;
    private ProductStatus status;
    private String categoryId;
    private String sellerId;
    private String sellerName;
    private String thumbnailUrl;

    // Propriétés 3D (compatibilité backend + évolution GLB)
    private boolean modele3d;
    private boolean has3DModel;
    private String model3dUrl;
    private String model3dFormat;
    private long model3dSize;

    private List<String> imageUrls;
    private int stock;
    private long createdAt;
    private long updatedAt;

    public Product() {
        super();
        this.status = ProductStatus.NOUVEAU;
        this.modele3d = false;
        this.has3DModel = false;
        this.imageUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public Product(String id, String title, String description, double price,
                   ProductStatus status, String categoryId, String sellerId, String sellerName,
                   String thumbnailUrl, boolean modele3d, String model3dUrl,
                   List<String> imageUrls, int stock, long createdAt) {
        super(id);
        this.title = title;
        this.description = description;
        this.price = price;
        this.status = status != null ? status : ProductStatus.NOUVEAU;
        this.categoryId = categoryId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.thumbnailUrl = thumbnailUrl;
        this.model3dUrl = model3dUrl;
        boolean valid3d = modele3d && model3dUrl != null && !model3dUrl.trim().isEmpty();
        this.modele3d = valid3d;
        this.has3DModel = valid3d;
        if (valid3d) {
            this.model3dFormat = "glb";
        }
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
        if (this.thumbnailUrl != null && !this.thumbnailUrl.isEmpty() && !this.imageUrls.contains(this.thumbnailUrl)) {
            this.imageUrls.add(0, this.thumbnailUrl);
        }
        this.stock = stock;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (title == null || title.trim().isEmpty()) {
            errors.add("Le titre du produit est obligatoire");
        }
        if (price < 0) {
            errors.add("Le prix ne peut pas être négatif");
        }
        if (stock < 0) {
            errors.add("Le stock ne peut pas être négatif");
        }
        if (has3DModel || (model3dUrl != null && !model3dUrl.trim().isEmpty())) {
            if (model3dFormat != null && !model3dFormat.trim().isEmpty()
                    && !"glb".equalsIgnoreCase(model3dFormat.trim())) {
                errors.add("Seul le format 3D .glb est autorisé");
            }
        }
        return errors;
    }

    /**
     * Vérifie si ce produit local possède un modèle 3D GLB valide.
     */
    public boolean hasValidGlbModel() {
        if (!(has3DModel || modele3d)) return false;
        if (model3dUrl == null || model3dUrl.trim().isEmpty()) return false;
        if (model3dFormat != null && !model3dFormat.trim().isEmpty()) {
            return "glb".equalsIgnoreCase(model3dFormat.trim());
        }
        String lower = model3dUrl.trim().toLowerCase();
        return lower.endsWith(".glb") || lower.contains(".glb?") || lower.contains("model.glb");
    }

    /**
     * Supprime les références au modèle 3D GLB du produit tout en conservant ses images classiques.
     */
    public void clear3DModel() {
        this.has3DModel = false;
        this.modele3d = false;
        this.model3dUrl = null;
        this.model3dFormat = null;
        this.model3dSize = 0L;
        this.updatedAt = System.currentTimeMillis();
    }

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

    public String getThumbnailUrl() {
        if ((thumbnailUrl == null || thumbnailUrl.isEmpty()) && imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
        if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
            if (this.imageUrls == null) {
                this.imageUrls = new ArrayList<>();
            }
            if (!this.imageUrls.contains(thumbnailUrl)) {
                this.imageUrls.add(0, thumbnailUrl);
            }
        }
    }

    public boolean isModele3d() { return modele3d || has3DModel; }
    public boolean hasModele3d() { return modele3d || has3DModel; }
    public void setModele3d(boolean modele3d) {
        this.modele3d = modele3d;
        this.has3DModel = modele3d;
    }

    public boolean isHas3DModel() { return has3DModel || modele3d; }
    public boolean has3DModel() { return has3DModel || modele3d; }
    public void setHas3DModel(boolean has3DModel) {
        this.has3DModel = has3DModel;
        this.modele3d = has3DModel;
    }

    public boolean isArSupported() { return hasValidGlbModel(); }
    public void setArSupported(boolean arSupported) {
        this.modele3d = arSupported;
        this.has3DModel = arSupported;
    }

    public String getModel3dUrl() { return model3dUrl; }
    public void setModel3dUrl(String model3dUrl) {
        this.model3dUrl = model3dUrl;
        boolean present = (model3dUrl != null && !model3dUrl.trim().isEmpty());
        this.modele3d = present;
        this.has3DModel = present;
        if (present && (this.model3dFormat == null || this.model3dFormat.isEmpty())) {
            this.model3dFormat = "glb";
        }
    }

    public String getModel3dFormat() { return model3dFormat; }
    public void setModel3dFormat(String model3dFormat) { this.model3dFormat = model3dFormat; }

    public long getModel3dSize() { return model3dSize; }
    public void setModel3dSize(long model3dSize) { this.model3dSize = model3dSize; }

    public List<String> getImageUrls() {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }
        if (imageUrls.isEmpty() && thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            imageUrls.add(thumbnailUrl);
        }
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
        if (!this.imageUrls.isEmpty() && (this.thumbnailUrl == null || this.thumbnailUrl.isEmpty())) {
            this.thumbnailUrl = this.imageUrls.get(0);
        }
    }

    public int getStock() { return stock; }
    public void setStock(int stock) {
        this.stock = stock;
        if (stock <= 0 && this.status != ProductStatus.INDISPONIBLE) {
            this.status = ProductStatus.INDISPONIBLE;
        }
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
