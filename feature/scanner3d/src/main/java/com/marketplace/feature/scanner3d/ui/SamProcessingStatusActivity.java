package com.marketplace.feature.scanner3d.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.core.firebase.FirebaseSamBridgeService;
import com.marketplace.core.model.SamScanJob;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.feature.scanner3d.R;

import java.io.File;
import java.util.ArrayList;

public class SamProcessingStatusActivity extends AppCompatActivity {

    public static final String EXTRA_JOB_ID = "EXTRA_JOB_ID";

    private TextView tvStatusHeader;
    private TextView tvStatusDetail;
    private ProgressBar progressBar;
    private Button btnViewInAr;

    private final FirebaseSamBridgeService samService = new FirebaseSamBridgeService();
    private String generatedModelUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sam_processing_status);

        String jobId = getIntent().getStringExtra(EXTRA_JOB_ID);
        if (jobId == null) jobId = "sam_demo_job";

        tvStatusHeader = findViewById(R.id.tv_status_header);
        tvStatusDetail = findViewById(R.id.tv_status_detail);
        progressBar = findViewById(R.id.progress_bar_sam);
        btnViewInAr = findViewById(R.id.btn_view_in_ar);

        startTrackingSamProcessing(jobId);

        btnViewInAr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (generatedModelUrl != null) {
                    RouterRegistry.getInstance().getArRouter().openArViewer(
                            SamProcessingStatusActivity.this,
                            "product_scanned",
                            "Mon Objet Scanné 3D",
                            generatedModelUrl
                    );
                }
            }
        });
    }

    private void startTrackingSamProcessing(String jobId) {
        tvStatusHeader.setText("Démarrage du traitement SAM (Meta)...");
        tvStatusDetail.setText("Synchronisation via Firebase avec l'ordinateur...");
        progressBar.setProgress(10);

        // Simulation / Déclenchement du flux via le service Firebase SAM
        samService.submitScanForSamProcessing(jobId, new ArrayList<File>(), new FirebaseSamBridgeService.SamStatusListener() {
            @Override
            public void onProgress(SamScanJob job) {
                progressBar.setProgress(job.getProgressPercentage());
                switch (job.getStatus()) {
                    case UPLOADING_IMAGES:
                        tvStatusHeader.setText("Étape 1/3 : Envoi des photos");
                        tvStatusDetail.setText("Téléversement sécurisé vers Firebase Storage...");
                        break;
                    case SAM_PROCESSING:
                        tvStatusHeader.setText("Étape 2/3 : Traitement IA Meta SAM");
                        tvStatusDetail.setText("L'ordinateur exécute la segmentation automatique du produit...");
                        break;
                    case RECONSTRUCTING_3D:
                        tvStatusHeader.setText("Étape 3/3 : Reconstruction 3D");
                        tvStatusDetail.setText("Génération du maillage polygonal et des textures (.glb)...");
                        break;
                    case COMPLETED:
                        tvStatusHeader.setText("Modèle 3D prêt !");
                        tvStatusDetail.setText("Traitement SAM terminé avec succès.");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onCompleted(String model3dUrl) {
                generatedModelUrl = model3dUrl;
                progressBar.setProgress(100);
                btnViewInAr.setVisibility(View.VISIBLE);
                Toast.makeText(SamProcessingStatusActivity.this, "Scan 3D finalisé !", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                tvStatusHeader.setText("Erreur");
                tvStatusDetail.setText(errorMessage);
            }
        });
    }
}
