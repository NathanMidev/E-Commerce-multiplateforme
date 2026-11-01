package com.marketplace.app.feature.scan3d;

import com.marketplace.app.model.ScanJob;

import java.io.File;
import java.util.List;

/**
 * Rôle : Contrat d'interface historique pour le pont de traitement du scan 3D automatique.
 * @deprecated Le scan 3D automatique (YOLO / SAM / COLMAP / worker NVIDIA) est temporairement désactivé
 * dans le parcours utilisateur au profit de l'importation manuelle de fichiers .glb via ProductMediaFirebaseService.
 */
@Deprecated
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
