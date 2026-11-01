package com.marketplace.app.feature.scan3d;

import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.marketplace.app.R;
import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.model.ScanJob;
import com.marketplace.app.navigation.Router;

/**
 * Rôle : Écran de capture 3D guidé pour le vendeur.
 * Entrée : ID produit et catégorie attendue.
 * Sortie : Capture de clichés et envoi vers le pont de reconstruction SAM + COLMAP (Firebase ou Simulé).
 */
public class ScanCaptureActivity extends BaseActivity {

    private TextView tvExpectedCategory;
    private TextView tvFramesCount;
    private TextView tvInstruction;
    private ProgressBar progressCapture;
    private Button btnCapture;
    private Button btnFinish;

    private int capturedFrames = 0;
    private static final int TARGET_FRAMES = 12;
    private String productId;
    private String categoryId;
    private ScanBridge bridge;
    private ScanJob currentJob;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_scan_capture;
    }

    @Override
    protected void bindViews() {
        bridge = ApiClient.isUseMock() ? new SimulatedScanBridge() : new FirebaseScanBridge();
        productId = getIntent().getStringExtra(Router.EXTRA_PRODUCT_ID);
        categoryId = getIntent().getStringExtra(Router.EXTRA_CATEGORY_ID);

        tvExpectedCategory = findViewById(R.id.tv_expected_category);
        tvFramesCount = findViewById(R.id.tv_frames_count);
        tvInstruction = findViewById(R.id.tv_guide_instruction);
        progressCapture = findViewById(R.id.progress_capture);
        btnCapture = findViewById(R.id.btn_capture_frame);
        btnFinish = findViewById(R.id.btn_finish_and_process);

        if (categoryId != null) {
            tvExpectedCategory.setText("Catégorie : " + categoryId);
        }

        String sellerId = AuthSession.getInstance().getUserId() != null
                ? AuthSession.getInstance().getUserId() : "seller_me";
        currentJob = bridge.createJob(productId, sellerId, categoryId);

        btnCapture.setOnClickListener(v -> {
            capturedFrames += 3;
            if (capturedFrames > TARGET_FRAMES) capturedFrames = TARGET_FRAMES;
            updateProgress();
        });

        btnFinish.setOnClickListener(v -> {
            Router.openScanJobStatus(this, currentJob.getId());
            finish();
        });
    }

    private void updateProgress() {
        progressCapture.setProgress(capturedFrames * 100 / TARGET_FRAMES);
        tvFramesCount.setText("Clichés : " + capturedFrames + " / " + TARGET_FRAMES);
        if (capturedFrames >= TARGET_FRAMES) {
            tvInstruction.setText("Capture complète ! Cliquez sur 'Terminer et traiter' ci-dessous.");
            btnFinish.setEnabled(true);
        }
    }

    @Override
    protected void loadData() {}

    @Override
    protected void render(Object data) {}
}
