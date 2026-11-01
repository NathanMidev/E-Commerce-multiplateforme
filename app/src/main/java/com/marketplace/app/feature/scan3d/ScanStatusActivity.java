package com.marketplace.app.feature.scan3d;

import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.marketplace.app.R;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.model.ScanJob;
import com.marketplace.app.navigation.Router;

import java.io.File;
import java.util.ArrayList;

/**
 * Rôle : Écran de suivi du traitement SAM & COLMAP avec affichage transparent de l'état [SIMULÉ].
 * Entrée : EXTRA_JOB_ID.
 * Sortie : Progression en pourcentage et bouton 'Voir en 3D / AR' une fois le fichier .glb généré.
 */
public class ScanStatusActivity extends BaseActivity {

    private TextView tvHeader;
    private TextView tvDetail;
    private ProgressBar progressBar;
    private Button btnViewAr;

    private ScanBridge bridge;
    private String generatedModelUrl;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sam_processing_status;
    }

    @Override
    protected void bindViews() {
        bridge = new SimulatedScanBridge();
        String jobId = getIntent().getStringExtra(Router.EXTRA_JOB_ID);
        if (jobId == null) jobId = "demo_job";

        tvHeader = findViewById(R.id.tv_status_header);
        tvDetail = findViewById(R.id.tv_status_detail);
        progressBar = findViewById(R.id.progress_bar_sam);
        btnViewAr = findViewById(R.id.btn_view_in_ar);

        btnViewAr.setOnClickListener(v -> {
            if (generatedModelUrl != null) {
                Router.openArViewer(this, "scanned_prod", "Produit numérisé", generatedModelUrl);
                finish();
            }
        });

        startProcessing(jobId);
    }

    private void startProcessing(String jobId) {
        tvHeader.setText("[SIMULÉ] Démarrage du traitement SAM (Meta)...");
        tvDetail.setText("Segmentation et reconstruction du maillage 3D...");

        bridge.submitScan(jobId, new ArrayList<File>(), new ScanBridge.ScanListener() {
            @Override
            public void onProgress(ScanJob job) {
                progressBar.setProgress(job.getProgress());
                tvDetail.setText("[SIMULÉ] Progression : " + job.getProgress() + "%");
            }

            @Override
            public void onCompleted(String model3dUrl) {
                generatedModelUrl = model3dUrl;
                progressBar.setProgress(100);
                tvHeader.setText("[SIMULÉ] Modèle 3D prêt !");
                tvDetail.setText("Modèle .glb disponible pour la Réalité Augmentée.");
                btnViewAr.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                tvHeader.setText("Erreur");
                tvDetail.setText(message);
            }
        });
    }

    @Override
    protected void loadData() {}

    @Override
    protected void render(Object data) {}
}
