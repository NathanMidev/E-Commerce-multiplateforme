package com.marketplace.feature.scanner3d.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.core.firebase.FirebaseScan3dBridgeService;
import com.marketplace.core.model.Scan3dJob;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.feature.scanner3d.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Écran de suivi du traitement en temps réel :
 * Mobile -> Firebase Storage -> PC (Meta SAM + COLMAP) -> Fichier .glb -> Visualisation AR.
 */
public class SamProcessingStatusActivity extends AppCompatActivity {

    public static final String EXTRA_JOB_ID = "EXTRA_JOB_ID";

    private TextView tvStatusHeader;
    private TextView tvStatusDetail;
    private ProgressBar progressBar;
    private Button btnViewInAr;

    private final FirebaseScan3dBridgeService bridgeService = new FirebaseScan3dBridgeService();
    private String generatedModelUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sam_processing_status);

        String jobId = getIntent().getStringExtra(EXTRA_JOB_ID);
        if (jobId == null) jobId = "scan_demo_job";

        tvStatusHeader = findViewById(R.id.tv_status_header);
        tvStatusDetail = findViewById(R.id.tv_status_detail);
        progressBar = findViewById(R.id.progress_bar_sam);
        btnViewInAr = findViewById(R.id.btn_view_in_ar);

        startTrackingProcessing(jobId);

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

    private void startTrackingProcessing(String jobId) {
        tvStatusHeader.setText("Démarrage du traitement SAM (Meta) & COLMAP...");
        tvStatusDetail.setText("Synchronisation via Firebase avec l'ordinateur...");
        progressBar.setProgress(15);

        // Déclenchement / Écoute du pipeline via FirebaseScan3dBridgeService
        bridgeService.submitScanForSamAndColmapProcessing(jobId, new ArrayList<File>(), new FirebaseScan3dBridgeService.ScanStatusListener() {
            @Override
            public void onProgress(Scan3dJob job) {
                progressBar.setProgress(job.getProgressPercentage());
                switch (job.getStatus()) {
                    case UPLOADING_TO_FIREBASE:
                        tvStatusHeader.setText("Étape 1/3 : Envoi vers Firebase");
                        tvStatusDetail.setText("Téléversement sécurisé des photos de capture multi-angles...");
                        break;
                    case SAM_SEGMENTING:
                        tvStatusHeader.setText("Étape 2/3 : Segmentation Meta SAM (PC)");
                        tvStatusDetail.setText("L'ordinateur supprime l'arrière-plan et isole précisément l'objet...");
                        break;
                    case COLMAP_RECONSTRUCTING:
                        tvStatusHeader.setText("Étape 3/3 : Reconstruction 3D COLMAP (PC)");
                        tvStatusDetail.setText("Calcul photogrammétrique SfM/MVS et génération du fichier .glb...");
                        break;
                    case COMPLETED:
                        tvStatusHeader.setText("Modèle 3D prêt !");
                        tvStatusDetail.setText("Reconstruction COLMAP et SAM terminée avec succès.");
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
                Toast.makeText(SamProcessingStatusActivity.this, "Scan 3D finalisé avec succès !", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                tvStatusHeader.setText("Erreur");
                tvStatusDetail.setText(errorMessage);
            }
        });
    }
}
