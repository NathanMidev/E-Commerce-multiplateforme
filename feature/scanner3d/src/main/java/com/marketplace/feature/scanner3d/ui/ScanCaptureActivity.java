package com.marketplace.feature.scanner3d.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class ScanCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";

    private TextView tvScanTitle;
    private TextView tvFramesCount;
    private Button btnCaptureFrame;
    private Button btnFinishAndProcess;

    private int capturedFrames = 0;
    private String productId;
    private final FirebaseSamBridgeService samService = new FirebaseSamBridgeService();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_capture);

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null) productId = "prod_sample";

        tvScanTitle = findViewById(R.id.tv_scan_title);
        tvFramesCount = findViewById(R.id.tv_frames_count);
        btnCaptureFrame = findViewById(R.id.btn_capture_frame);
        btnFinishAndProcess = findViewById(R.id.btn_finish_and_process);

        updateFramesUI();

        btnCaptureFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturedFrames += 5; // Simule une rafale de 5 clichés sous différents angles
                updateFramesUI();
                Toast.makeText(ScanCaptureActivity.this, "Angle capturé (" + capturedFrames + " clichés)", Toast.LENGTH_SHORT).show();
            }
        });

        btnFinishAndProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (capturedFrames < 10) {
                    Toast.makeText(ScanCaptureActivity.this, "Capturez au moins 10 angles pour le modèle SAM", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Créer le job Firebase pour l'ordinateur exécutant Meta SAM
                SamScanJob job = samService.createJob(productId, "seller_current", capturedFrames);

                // Ouvrir l'écran de statut du traitement
                RouterRegistry.getInstance().getScannerRouter().openSamJobStatus(ScanCaptureActivity.this, job.getJobId());
                finish();
            }
        });
    }

    private void updateFramesUI() {
        tvFramesCount.setText("Clichés capturés : " + capturedFrames + " / 30 recommandés");
        btnFinishAndProcess.setEnabled(capturedFrames >= 10);
    }
}
