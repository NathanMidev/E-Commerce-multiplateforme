package com.marketplace.app.feature.scan3d;

import com.marketplace.app.model.ScanJob;

import java.io.File;
import java.util.List;

/**
 * Rôle : Contrat d'interface commune pour le pont de traitement du scan 3D (SAM + COLMAP).
 * Entrée : Photos multi-angles et jobId.
 * Sortie : Progression et mise à disposition du modèle .glb.
 */
public interface ScanBridge {

    interface ScanListener {
        void onProgress(ScanJob job);
        void onCompleted(String model3dUrl);
        void onError(String message);
    }

    ScanJob createJob(String productId, String sellerId, String categoryId);
    void submitScan(String jobId, List<File> photos, ScanListener listener);
    ScanJob getJob(String jobId);
}
