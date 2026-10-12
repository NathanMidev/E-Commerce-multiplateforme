package com.marketplace.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SamScanJob implements Serializable {

    public enum Status {
        PENDING,            // Scan créé sur le mobile
        UPLOADING_IMAGES,   // Envoi des images capturées vers Firebase Storage
        SAM_PROCESSING,     // Détection & Segmentation du produit par le modèle SAM (Meta) sur l'ordinateur
        RECONSTRUCTING_3D,  // Reconstruction du maillage 3D / Texture (.glb) sur l'ordinateur
        COMPLETED,          // Modèle 3D prêt pour la Réalité Augmentée
        FAILED              // Erreur lors du traitement
    }

    private String jobId;
    private String productId;
    private String sellerId;
    private Status status;
    private int totalFrames;
    private int processedFrames;
    private String model3dUrl;       // URL du fichier .glb généré
    private String errorMessage;
    private List<String> uploadedImageUrls;
    private long createdAt;
    private long completedAt;

    public SamScanJob() {
        this.status = Status.PENDING;
        this.uploadedImageUrls = new ArrayList<>();
    }

    public SamScanJob(String jobId, String productId, String sellerId) {
        this.jobId = jobId;
        this.productId = productId;
        this.sellerId = sellerId;
        this.status = Status.PENDING;
        this.uploadedImageUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getTotalFrames() { return totalFrames; }
    public void setTotalFrames(int totalFrames) { this.totalFrames = totalFrames; }

    public int getProcessedFrames() { return processedFrames; }
    public void setProcessedFrames(int processedFrames) { this.processedFrames = processedFrames; }

    public String getModel3dUrl() { return model3dUrl; }
    public void setModel3dUrl(String model3dUrl) { this.model3dUrl = model3dUrl; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<String> getUploadedImageUrls() { return uploadedImageUrls; }
    public void setUploadedImageUrls(List<String> uploadedImageUrls) { this.uploadedImageUrls = uploadedImageUrls; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public int getProgressPercentage() {
        if (status == Status.COMPLETED) return 100;
        if (status == Status.PENDING) return 5;
        if (status == Status.UPLOADING_IMAGES) return 25;
        if (status == Status.SAM_PROCESSING) {
            if (totalFrames > 0) return 25 + (int) ((processedFrames / (float) totalFrames) * 40);
            return 45;
        }
        if (status == Status.RECONSTRUCTING_3D) return 85;
        return 0;
    }
}
