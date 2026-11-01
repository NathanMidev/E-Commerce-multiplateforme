package com.marketplace.app.feature.scan3d;

import android.os.Handler;
import android.os.Looper;

import com.marketplace.app.model.ScanJob;
import com.marketplace.app.model.ScanJobStatus;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rôle : Pont de scan 3D simulé (indiquant clairement [SIMULÉ] dans l'interface).
 * Entrée : ID produit et photos.
 * Sortie : Progression pas à pas jusqu'à la fourniture d'un modèle 3D de test GLB.
 */
public class SimulatedScanBridge implements ScanBridge {

    private final Map<String, ScanJob> jobs = new HashMap<>();

    private void runDelayed(Runnable r, long delay) {
        try {
            Looper looper = Looper.getMainLooper();
            if (looper != null) {
                new Handler(looper).postDelayed(r, delay);
                return;
            }
        } catch (Throwable ignored) {}
        r.run();
    }

    @Override
    public ScanJob createJob(String productId, String sellerId, String categoryId) {
        String id = "sim_job_" + UUID.randomUUID().toString().substring(0, 8);
        ScanJob job = new ScanJob(id, productId, sellerId, categoryId);
        jobs.put(id, job);
        return job;
    }

    @Override
    public void submitScan(String jobId, List<File> photos, ScanListener listener) {
        final ScanJob job = jobs.get(jobId);
        if (job == null) {
            if (listener != null) listener.onError("[SIMULÉ] Job introuvable");
            return;
        }

        job.setStatus(ScanJobStatus.EN_COURS);
        job.setProgress(30);
        if (listener != null) listener.onProgress(job);

        runDelayed(() -> {
            job.setProgress(70);
            if (listener != null) listener.onProgress(job);

            runDelayed(() -> {
                job.setStatus(ScanJobStatus.TERMINE);
                job.setProgress(100);
                String testGlb = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Duck/glTF-Binary/Duck.glb";
                job.setModelUrl(testGlb);
                if (listener != null) {
                    listener.onProgress(job);
                    listener.onCompleted(testGlb);
                }
            }, 1000);
        }, 1000);
    }

    @Override
    public ScanJob getJob(String jobId) {
        return jobs.get(jobId);
    }
}
