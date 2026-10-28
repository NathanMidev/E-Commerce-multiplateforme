package com.marketplace.feature.scanner3d.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.core.firebase.FirebaseScan3dBridgeService;
import com.marketplace.core.model.Scan3dJob;
import com.marketplace.core.model.ScanJobStatus;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.feature.scanner3d.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Écran de capture 3D guidé pour le vendeur :
 * 1. Détection de plan/surface par ARCore.
 * 2. Inférence YOLO en temps réel pour étiqueter l'objet et vérifier l'adéquation avec la catégorie.
 * 3. Si validé : capture multi-angles à 360°.
 * 4. Téléversement vers Firebase pour segmentation SAM et reconstruction COLMAP sur PC/GPU.
 */
public class ScanCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";
    public static final String EXTRA_CATEGORY_ID = "EXTRA_CATEGORY_ID";

    private TextView tvScanStepStatus;
    private TextView tvExpectedCategory;
    private TextView tvYoloTag;
    private TextView tvFramesCount;
    private TextView tvGuideInstruction;
    private FrameLayout yoloBoundingBox;
    private ProgressBar progressCapture;
    private Button btnSimulateAiCheck;
    private Button btnCaptureFrame;
    private Button btnFinishAndProcess;

    private String productId;
    private String expectedCategoryId;
    private int capturedFrames = 0;
    private final int RECOMMENDED_FRAMES = 20;

    private final FirebaseScan3dBridgeService scanBridgeService = new FirebaseScan3dBridgeService();
    private Scan3dJob currentJob;
    private boolean isSurfaceDetected = false;
    private boolean isYoloValidated = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_capture);

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null) productId = "prod_sample";

        expectedCategoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        if (expectedCategoryId == null) expectedCategoryId = "cat_furniture";

        // Initialisation des vues
        tvScanStepStatus = findViewById(R.id.tv_scan_step_status);
        tvExpectedCategory = findViewById(R.id.tv_expected_category);
        tvYoloTag = findViewById(R.id.tv_yolo_tag);
        tvFramesCount = findViewById(R.id.tv_frames_count);
        tvGuideInstruction = findViewById(R.id.tv_guide_instruction);
        yoloBoundingBox = findViewById(R.id.yolo_bounding_box);
        progressCapture = findViewById(R.id.progress_capture);
        btnSimulateAiCheck = findViewById(R.id.btn_simulate_ai_check);
        btnCaptureFrame = findViewById(R.id.btn_capture_frame);
        btnFinishAndProcess = findViewById(R.id.btn_finish_and_process);

        tvExpectedCategory.setText("Catégorie attendue : " + expectedCategoryId);

        // Création du job initialisé dans Firebase Bridge
        currentJob = scanBridgeService.createJob(productId, "seller_current", expectedCategoryId);

        setupListeners();
    }

    private void setupListeners() {
        // Étape 1 & 2 : Détection de surface ARCore & Inférence YOLO
        btnSimulateAiCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runArCoreAndYoloDetection();
            }
        });

        // Étape 3 : Capture guidée multi-angles (360°)
        btnCaptureFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isYoloValidated) {
                    Toast.makeText(ScanCaptureActivity.this, "Veuillez d'abord valider l'objet avec YOLO", Toast.LENGTH_SHORT).show();
                    return;
                }
                capturedFrames += 4; // Rafale de clichés
                if (capturedFrames > RECOMMENDED_FRAMES) capturedFrames = RECOMMENDED_FRAMES;

                updateCaptureProgress();
            }
        });

        // Étape 4 : Fin de capture et envoi vers Firebase (pour SAM + COLMAP sur PC)
        btnFinishAndProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitToFirebaseAndPc();
            }
        });
    }

    /**
     * Détection de surface et validation de conformité YOLO
     */
    private void runArCoreAndYoloDetection() {
        isSurfaceDetected = true;
        tvScanStepStatus.setText("1. Surface plane détectée par ARCore ✅");
        tvScanStepStatus.setTextColor(Color.parseColor("#4CAF50"));

        // Simulation de la détection d'objet par le modèle YOLO
        String detectedLabel = "chair";
        float confidence = 0.94f;
        boolean matchesCategory = expectedCategoryId.contains("furniture") || expectedCategoryId.contains("cat_furniture");

        boolean isValidated = scanBridgeService.validateWithYolo(currentJob.getJobId(), detectedLabel, confidence, matchesCategory);

        if (isValidated) {
            isYoloValidated = true;
            tvYoloTag.setText("YOLO : " + detectedLabel + " (" + (int)(confidence * 100) + "%) ✅");
            tvYoloTag.setBackgroundColor(Color.parseColor("#2E7D32")); // Vert
            tvGuideInstruction.setText("Objet conforme à la catégorie ! Tournez lentement autour du produit et capturez les angles.");
            tvGuideInstruction.setTextColor(Color.parseColor("#81C784"));

            btnCaptureFrame.setEnabled(true);
            btnSimulateAiCheck.setEnabled(false);
            btnSimulateAiCheck.setText("Surface & Objet Validés ✅");
        } else {
            isYoloValidated = false;
            tvYoloTag.setText("YOLO : Rejeté (" + detectedLabel + " ≠ " + expectedCategoryId + ") ❌");
            tvYoloTag.setBackgroundColor(Color.parseColor("#C62828")); // Rouge
            tvGuideInstruction.setText("L'objet détecté ne correspond pas à la catégorie. Veuillez repositionner la caméra.");
            tvGuideInstruction.setTextColor(Color.parseColor("#E57373"));
            btnCaptureFrame.setEnabled(false);
            Toast.makeText(this, "Objet non conforme : SAM ne traitera pas ce scan.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateCaptureProgress() {
        tvFramesCount.setText("Clichés capturés : " + capturedFrames + " / " + RECOMMENDED_FRAMES);
        progressCapture.setProgress(capturedFrames);

        if (capturedFrames >= RECOMMENDED_FRAMES) {
            tvGuideInstruction.setText("Capture 360° complète ! Cliquez ci-dessous pour transmettre au PC.");
            btnFinishAndProcess.setEnabled(true);
        } else {
            tvGuideInstruction.setText("Continuez de tourner autour du produit (" + (RECOMMENDED_FRAMES - capturedFrames) + " clichés restants).");
        }
    }

    private void submitToFirebaseAndPc() {
        List<File> dummyImageFiles = new ArrayList<>();
        for (int i = 0; i < capturedFrames; i++) {
            dummyImageFiles.add(new File(getCacheDir(), "frame_" + i + ".jpg"));
        }

        // Lancement du traitement asynchrone via la passerelle Firebase
        scanBridgeService.submitScanForSamAndColmapProcessing(
                currentJob.getJobId(),
                dummyImageFiles,
                new FirebaseScan3dBridgeService.ScanStatusListener() {
                    @Override
                    public void onProgress(Scan3dJob job) {
                        // Notifié à chaque étape (Firebase -> SAM -> COLMAP)
                    }

                    @Override
                    public void onCompleted(String model3dUrl) {
                        // Modèle 3D prêt
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(ScanCaptureActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Ouvrir l'écran de statut pour suivre SAM & COLMAP
        RouterRegistry.getInstance().getScannerRouter().openSamJobStatus(ScanCaptureActivity.this, currentJob.getJobId());
        finish();
    }
}
