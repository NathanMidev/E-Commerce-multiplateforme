package com.marketplace.app.feature.scan3d;

import android.net.Uri;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.ScanJob;
import com.marketplace.app.model.ScanJobStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rôle : Passerelle Firebase Storage & Firestore avec le worker PC (Meta SAM + COLMAP).
 * Entrée : Upload photos dans scans/{uid}/{jobId}/ et document scan_jobs/{jobId}.
 * Sortie : Écoute temps réel des changements d'état et mise à jour automatique via PATCH /products/:id/model3d.
 */
public class FirebaseScanBridge implements ScanBridge {

    private static final Map<String, ScanJob> ACTIVE_JOBS = new ConcurrentHashMap<>();
    private final ProductRepository productRepository = new ProductRepository();

    @Override
    public ScanJob createJob(String productId, String sellerId, String categoryId) {
        String id = "scan_" + UUID.randomUUID().toString().substring(0, 8);
        ScanJob job = new ScanJob(id, productId != null ? productId : "", sellerId != null ? sellerId : "", categoryId != null ? categoryId : "");
        ACTIVE_JOBS.put(id, job);
        return job;
    }

    @Override
    public void submitScan(String jobId, List<File> photos, ScanListener listener) {
        String uid = AuthSession.getInstance().getUserId();
        if (uid == null || uid.trim().isEmpty()) {
            if (listener != null) listener.onError("Connexion Firebase requise pour envoyer un scan 3D");
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        ScanJob existingJob = ACTIVE_JOBS.get(jobId);
        String productId = existingJob != null ? existingJob.getProductId() : "";

        if (photos == null || photos.isEmpty()) {
            writeFirestoreJobDocument(firestore, jobId, uid, productId, Collections.emptyList(), listener);
            return;
        }

        StorageReference scanDirRef = storage.getReference().child("scans").child(uid).child(jobId);
        List<String> photoUrls = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < photos.size(); i++) {
            File photo = photos.get(i);
            StorageReference fileRef = scanDirRef.child("frame_" + i + ".jpg");
            fileRef.putFile(Uri.fromFile(photo)).addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    photoUrls.add(uri.toString());
                    if (photoUrls.size() == photos.size()) {
                        writeFirestoreJobDocument(firestore, jobId, uid, productId, photoUrls, listener);
                    }
                });
            }).addOnFailureListener(e -> {
                if (listener != null) listener.onError("Erreur upload photo Storage: " + e.getMessage());
            });
        }
    }

    private void writeFirestoreJobDocument(
            FirebaseFirestore firestore,
            String jobId,
            String uid,
            String productId,
            List<String> photoUrls,
            ScanListener listener
    ) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("jobId", jobId);
        doc.put("userId", uid);
        doc.put("productId", productId != null ? productId : "");
        doc.put("status", "EN_ATTENTE");
        doc.put("progress", 20);
        doc.put("photoUrls", photoUrls);
        doc.put("modelUrl", "");
        doc.put("createdAt", System.currentTimeMillis());

        firestore.collection("scan_jobs").document(jobId).set(doc)
                .addOnSuccessListener(aVoid -> listenToJobUpdates(jobId, firestore, listener))
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Erreur Firestore scan_jobs: " + e.getMessage());
                });
    }

    private void listenToJobUpdates(String jobId, FirebaseFirestore firestore, ScanListener listener) {
        firestore.collection("scan_jobs").document(jobId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (listener != null) listener.onError(error.getMessage());
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                String statusStr = snapshot.getString("status");
                Long progressVal = snapshot.getLong("progress");
                String modelUrl = snapshot.getString("modelUrl");
                String productId = snapshot.getString("productId");

                ScanJob job = ACTIVE_JOBS.getOrDefault(jobId, new ScanJob());
                job.setId(jobId);
                if (productId != null) job.setProductId(productId);
                job.setProgress(progressVal != null ? progressVal.intValue() : 0);

                if ("TERMINE".equals(statusStr)) {
                    job.setStatus(ScanJobStatus.TERMINE);
                    job.setModelUrl(modelUrl);

                    if (productId != null && !productId.isEmpty() && modelUrl != null && !modelUrl.isEmpty()) {
                        productRepository.updateModel3d(productId, modelUrl, new BaseRepository.Callback<Boolean>() {
                            @Override public void onSuccess(Boolean result) {}
                            @Override public void onError(String message) {}
                        });
                    }

                    if (listener != null) listener.onCompleted(modelUrl);
                } else if ("ECHEC".equals(statusStr)) {
                    job.setStatus(ScanJobStatus.ECHEC);
                    if (listener != null) listener.onError(snapshot.getString("errorMessage"));
                } else {
                    job.setStatus(ScanJobStatus.EN_COURS);
                    if (listener != null) listener.onProgress(job);
                }
            }
        });
    }

    @Override
    public ScanJob getJob(String jobId) {
        ScanJob job = ACTIVE_JOBS.get(jobId);
        return job != null ? job : new ScanJob(jobId, "", "", "");
    }
}
