package com.marketplace.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente un Job complet de numérisation et reconstruction 3D :
 * Détection de surface (ARCore), validation d'étiquette catégorie (YOLO),
 * segmentation du sujet (Meta SAM) et reconstruction photogrammétrique (COLMAP).
 */
public class Scan3dJob implements Serializable {

    private String jobId;
    private String productId;
    private String sellerId;
    private String expectedCategoryId;  // Catégorie déclarée du produit

    // Informations d'analyse Edge AI (YOLO)
    private String detectedLabel;        // Étiquette détectée par YOLO (ex: "chair", "shoe")
    private float detectionConfidence;   // Indice de confiance de YOLO (0.0 à 1.0)
    private boolean categoryMatched;     // true si l'étiquette YOLO est compatible avec expectedCategoryId

    // Progression et statut du job
    private ScanJobStatus status;
    private int totalFrames;
    private int processedFrames;
    private String model3dUrl;           // URL finale du modèle .glb généré par COLMAP
    private String errorMessage;
    private List<String> uploadedImageUrls;
    private long createdAt;
    private long completedAt;

    public Scan3dJob() {
        this.status = ScanJobStatus.SURFACE_DETECTION;
        this.uploadedImageUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Scan3dJob(String jobId, String productId, String sellerId, String expectedCategoryId) {
        this.jobId = jobId;
        this.productId = productId;
        this.sellerId = sellerId;
        this.expectedCategoryId = expectedCategoryId;
        this.status = ScanJobStatus.SURFACE_DETECTION;
        this.uploadedImageUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public int getProgressPercentage() {
        switch (status) {
            case SURFACE_DETECTION:
                return 5;
            case YOLO_VALIDATING:
                return 15;
            case CATEGORY_MISMATCH:
            case FAILED:
                return 0;
            case CAPTURING_FRAMES:
                return 30;
            case UPLOADING_TO_FIREBASE:
                return 45;
            case SAM_SEGMENTING:
                if (totalFrames > 0) {
                    return 45 + (int) ((processedFrames / (float) totalFrames) * 20);
                }
                return 55;
            case COLMAP_RECONSTRUCTING:
                return 85;
            case COMPLETED:
                return 100;
            default:
                return 0;
        }
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getExpectedCategoryId() { return expectedCategoryId; }
    public void setExpectedCategoryId(String expectedCategoryId) { this.expectedCategoryId = expectedCategoryId; }

    public String getDetectedLabel() { return detectedLabel; }
    public void setDetectedLabel(String detectedLabel) { this.detectedLabel = detectedLabel; }

    public float getDetectionConfidence() { return detectionConfidence; }
    public void setDetectionConfidence(float detectionConfidence) { this.detectionConfidence = detectionConfidence; }

    public boolean isCategoryMatched() { return categoryMatched; }
    public void setCategoryMatched(boolean categoryMatched) { this.categoryMatched = categoryMatched; }

    public ScanJobStatus getStatus() { return status; }
    public void setStatus(ScanJobStatus status) { this.status = status; }

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
}
