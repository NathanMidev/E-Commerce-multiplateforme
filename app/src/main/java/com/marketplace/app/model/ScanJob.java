package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Suivi du pipeline de numérisation 3D (Capture, YOLO, SAM, COLMAP).
 * Entrée : ID produit, vendeur, catégorie, images capturées.
 * Sortie : Progression et URL finale du fichier .glb.
 */
public class ScanJob extends BaseModel {
    private String productId;
    private String sellerId;
    private String categoryId;
    private ScanJobStatus status;
    private int progress;
    private List<String> photoUrls;
    private String modelUrl;
    private String errorMessage;
    private long createdAt;

    public ScanJob() {
        super();
        this.status = ScanJobStatus.EN_ATTENTE;
        this.progress = 0;
        this.photoUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public ScanJob(String id, String productId, String sellerId, String categoryId) {
        super(id);
        this.productId = productId;
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.status = ScanJobStatus.EN_ATTENTE;
        this.progress = 0;
        this.photoUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (productId == null || productId.trim().isEmpty()) {
            errors.add("L'identifiant du produit est obligatoire");
        }
        return errors;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public ScanJobStatus getStatus() { return status; }
    public void setStatus(ScanJobStatus status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public List<String> getPhotoUrls() { return photoUrls != null ? photoUrls : new ArrayList<>(); }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }

    public String getModelUrl() { return modelUrl; }
    public void setModelUrl(String modelUrl) { this.modelUrl = modelUrl; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
