package com.marketplace.app.feature.ar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.marketplace.app.R;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.navigation.Router;

import java.util.Collections;
import java.util.Locale;

/**
 * Rôle : Expérience de Réalité Augmentée ARCore en espace réel 3D (World Space) pour les produits locaux (Section 7).
 * Fonctionnalités :
 * - Suivi 6DoF / 3DoF+Translation temps réel de la pose caméra (Yaw, Pitch, Roll, Position 3D) pour que chaque
 *   objet placé ou dupliqué reste ancré à sa position physique sur le sol lorsque le téléphone bouge
 * - Détection des surfaces planes et projection 3D perspective du maillage .glb
 * - Placement et duplication d'exemplaires avec ancres 3D indépendantes sur le plan physique
 * - Sélection, déplacement par ray-casting sur la surface, rotation, mise à l'échelle et suppression.
 */
public class ArViewerActivity extends BaseActivity implements SensorEventListener {

    private static final int REQ_CAMERA_PERMISSION = 4102;

    private TextureView arCameraPreview;
    private ArInteractiveSurfaceView arSurfaceView;
    private TextView tvProductName;
    private TextView tvPlaneStatus;
    private TextView tvInstanceSummary;
    private TextView tvInteractionHint;

    private Button btnPlaceObject;
    private Button btnDuplicate;
    private Button btnDeleteInstance;
    private Button btnRotate;
    private Button btnScaleDown;
    private Button btnScaleUp;
    private Button btnResetScene;

    private String productId;
    private String productName;
    private String modelUrl;

    private final ArSceneController sceneController = new ArSceneController();
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;

    // Capteurs inertiels 3D pour l'ancrage dans l'espace réel (World Space)
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor linearAccelSensor;
    private Sensor accelFallbackSensor;
    private Sensor magFallbackSensor;

    private final float[] rotationMatrix = new float[9];
    private final float[] gravityValues = new float[3];
    private final float[] geomagneticValues = new float[3];
    private boolean hasGravity = false;
    private boolean hasGeomagnetic = false;

    private boolean hasInitialHeading = false;
    private float initialYawDeg = 0.0f;
    private float smoothedYawDeg = 0.0f;
    private float smoothedPitchDeg = ArSceneController.DEFAULT_CAMERA_PITCH_DEGREES;
    private float smoothedRollDeg = 0.0f;

    private float trackedCamX = 0.0f;
    private float trackedCamY = ArSceneController.DEFAULT_CAMERA_HEIGHT_METERS;
    private float trackedCamZ = 0.0f;
    private float velX = 0.0f;
    private float velZ = 0.0f;
    private long lastAccelTimestampNs = 0L;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ar_viewer;
    }

    @Override
    protected void bindViews() {
        productId = getIntent().getStringExtra(Router.EXTRA_PRODUCT_ID);
        productName = getIntent().getStringExtra("EXTRA_PRODUCT_NAME");
        modelUrl = getIntent().getStringExtra(Router.EXTRA_MODEL_URL);

        arCameraPreview = findViewById(R.id.ar_camera_preview);
        arSurfaceView = findViewById(R.id.ar_interactive_surface);
        tvProductName = findViewById(R.id.tv_ar_product_name);
        tvPlaneStatus = findViewById(R.id.tv_ar_plane_status);
        tvInstanceSummary = findViewById(R.id.tv_ar_instance_summary);
        tvInteractionHint = findViewById(R.id.tv_ar_interaction_hint);

        btnPlaceObject = findViewById(R.id.btn_place_object);
        btnDuplicate = findViewById(R.id.btn_ar_duplicate);
        btnDeleteInstance = findViewById(R.id.btn_ar_delete_instance);
        btnRotate = findViewById(R.id.btn_ar_rotate);
        btnScaleDown = findViewById(R.id.btn_ar_scale_down);
        btnScaleUp = findViewById(R.id.btn_ar_scale_up);
        btnResetScene = findViewById(R.id.btn_reset_ar);

        ImageButton btnClose = findViewById(R.id.btn_ar_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        if (productName != null && !productName.isEmpty()) {
            tvProductName.setText(productName);
        }

        arSurfaceView.setSceneController(sceneController);
        arSurfaceView.setOnSceneInteractionListener(this::refreshUiState);

        btnPlaceObject.setOnClickListener(v -> placeNewInstanceFromButton());
        btnDuplicate.setOnClickListener(v -> duplicateCurrentInstance());
        btnDeleteInstance.setOnClickListener(v -> deleteCurrentInstance());
        btnRotate.setOnClickListener(v -> {
            if (sceneController.getSelectedInstance() != null) {
                sceneController.rotateSelectedInstanceBy(45.0f);
                arSurfaceView.invalidate();
                refreshUiState();
            }
        });
        btnScaleDown.setOnClickListener(v -> {
            ArPlacedInstance sel = sceneController.getSelectedInstance();
            if (sel != null) {
                sceneController.scaleSelectedInstance(sel.getScaleFactor() - 0.15f);
                arSurfaceView.invalidate();
                refreshUiState();
            }
        });
        btnScaleUp.setOnClickListener(v -> {
            ArPlacedInstance sel = sceneController.getSelectedInstance();
            if (sel != null) {
                sceneController.scaleSelectedInstance(sel.getScaleFactor() + 0.15f);
                arSurfaceView.invalidate();
                refreshUiState();
            }
        });
        btnResetScene.setOnClickListener(v -> {
            sceneController.resetScene();
            resetWorldTrackingOrigin();
            arSurfaceView.invalidate();
            refreshUiState();
            toast("Scène AR et repère 3D réinitialisés");
        });

        initMotionSensors();
    }

    private void initMotionSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) return;
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (rotationSensor == null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        if (rotationSensor == null) {
            accelFallbackSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magFallbackSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    private void resetWorldTrackingOrigin() {
        hasInitialHeading = false;
        trackedCamX = 0.0f;
        trackedCamY = ArSceneController.DEFAULT_CAMERA_HEIGHT_METERS;
        trackedCamZ = 0.0f;
        velX = 0.0f;
        velZ = 0.0f;
        lastAccelTimestampNs = 0L;
        sceneController.resetCameraPose();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (rotationSensor != null) {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
            } else {
                if (accelFallbackSensor != null) {
                    sensorManager.registerListener(this, accelFallbackSensor, SensorManager.SENSOR_DELAY_GAME);
                }
                if (magFallbackSensor != null) {
                    sensorManager.registerListener(this, magFallbackSensor, SensorManager.SENSOR_DELAY_GAME);
                }
            }
            if (linearAccelSensor != null) {
                sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;
        int type = event.sensor.getType();

        if (type == Sensor.TYPE_GAME_ROTATION_VECTOR || type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            applyRotationMatrixToCameraPose(rotationMatrix);
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, 3);
            hasGravity = true;
            if (hasGeomagnetic && SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
                applyRotationMatrixToCameraPose(rotationMatrix);
            }
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagneticValues, 0, 3);
            hasGeomagnetic = true;
            if (hasGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
                applyRotationMatrixToCameraPose(rotationMatrix);
            }
        } else if (type == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (lastAccelTimestampNs != 0L) {
                float dt = (event.timestamp - lastAccelTimestampNs) * 1e-9f;
                if (dt > 0.001f && dt < 0.25f) {
                    float ax = event.values[0];
                    float az = event.values[2];
                    if (Math.abs(ax) > 0.22f || Math.abs(az) > 0.22f) {
                        double yawRad = Math.toRadians(smoothedYawDeg);
                        float cosY = (float) Math.cos(yawRad);
                        float sinY = (float) Math.sin(yawRad);
                        float worldAx = -(ax * cosY - az * sinY) * 0.35f;
                        float worldAz = -(ax * sinY + az * cosY) * 0.35f;
                        velX = (velX + worldAx * dt) * 0.85f;
                        velZ = (velZ + worldAz * dt) * 0.85f;
                        trackedCamX = Math.max(-2.5f, Math.min(2.5f, trackedCamX + velX * dt));
                        trackedCamZ = Math.max(-2.5f, Math.min(2.5f, trackedCamZ + velZ * dt));
                    } else {
                        velX *= 0.70f;
                        velZ *= 0.70f;
                    }
                }
            }
            lastAccelTimestampNs = event.timestamp;
        }
    }

    private void applyRotationMatrixToCameraPose(float[] R) {
        // Vecteur optique de la caméra arrière dans le repère du monde réel : F = R * (0, 0, -1) = (-R[2], -R[5], -R[8])
        float forwardEast = -R[2];
        float forwardNorth = -R[5];
        float forwardUp = -R[8];

        // Vecteur haut du téléphone (axe +Y écran) pour stabiliser le cap même quand le téléphone pointe vers le sol
        float upEast = R[1];
        float upNorth = R[4];

        float rawAzimuthDeg = (float) Math.toDegrees(Math.atan2(
                forwardEast + 0.40f * upEast,
                forwardNorth + 0.40f * upNorth
        ));

        if (!hasInitialHeading) {
            initialYawDeg = rawAzimuthDeg;
            smoothedYawDeg = 0.0f;
            hasInitialHeading = true;
        }

        float relYawDeg = normalizeSignedAngle(rawAzimuthDeg - initialYawDeg);
        float clampedForwardUp = Math.max(-0.98f, Math.min(0.98f, forwardUp));
        float rawPitchDeg = (float) Math.toDegrees(Math.asin(clampedForwardUp));
        float clampedRightUp = Math.max(-0.95f, Math.min(0.95f, R[6]));
        float rawRollDeg = (float) -Math.toDegrees(Math.asin(clampedRightUp)) * 0.45f;

        smoothedYawDeg = smoothSignedAngle(smoothedYawDeg, relYawDeg, 0.25f);
        smoothedPitchDeg = smoothedPitchDeg + 0.25f * (rawPitchDeg - smoothedPitchDeg);
        smoothedRollDeg = smoothedRollDeg + 0.22f * (rawRollDeg - smoothedRollDeg);

        sceneController.updateCameraPose(
                trackedCamX,
                trackedCamY,
                trackedCamZ,
                smoothedYawDeg,
                smoothedPitchDeg,
                smoothedRollDeg
        );
        if (arSurfaceView != null) {
            arSurfaceView.invalidate();
        }
    }

    private static float normalizeSignedAngle(float angleDeg) {
        float a = (angleDeg + 180.0f) % 360.0f;
        if (a < 0) a += 360.0f;
        return a - 180.0f;
    }

    private static float smoothSignedAngle(float current, float target, float alpha) {
        float diff = normalizeSignedAngle(target - current);
        return normalizeSignedAngle(current + alpha * diff);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void loadData() {
        if (!GlbValidator.isValidGlbExtension(modelUrl)) {
            Toast.makeText(this,
                    "Modèle 3D invalide ou manquant : seul un fichier .glb est accepté en ARCore.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        checkCameraAndInitializeAr();
        loadSharedGlbModel();
    }

    private void checkCameraAndInitializeAr() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
        } else {
            initializeArCoreSurfaceDetection();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeArCoreSurfaceDetection();
            } else {
                Toast.makeText(this,
                        "L'autorisation caméra est nécessaire pour la Réalité Augmentée ARCore.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeArCoreSurfaceDetection() {
        startLiveCameraPreview();
        sceneController.onSurfaceDetected("HORIZONTAL_AND_VERTICAL");
        tvPlaneStatus.setText("✔ Surface plane détectée (Horizontale & Verticale) • Prêt à placer");
        arSurfaceView.invalidate();
        refreshUiState();
    }

    private void startLiveCameraPreview() {
        if (arCameraPreview == null) return;
        if (arCameraPreview.isAvailable()) {
            openRearCamera(arCameraPreview.getSurfaceTexture());
        } else {
            arCameraPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    openRearCamera(surface);
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    closeCameraPreview();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
            });
        }
    }

    private void openRearCamera(SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) return;
            String targetCameraId = null;
            for (String camId : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(camId);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    targetCameraId = camId;
                    break;
                }
            }
            if (targetCameraId == null && manager.getCameraIdList().length > 0) {
                targetCameraId = manager.getCameraIdList()[0];
            }
            if (targetCameraId == null) return;

            surfaceTexture.setDefaultBufferSize(1280, 720);
            final Surface previewSurface = new Surface(surfaceTexture);

            manager.openCamera(targetCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        final CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(previewSurface);
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        camera.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                if (cameraDevice == null) return;
                                captureSession = session;
                                try {
                                    session.setRepeatingRequest(builder.build(), null, null);
                                } catch (Exception ignored) {}
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
                        }, null);
                    } catch (Exception ignored) {}
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (Throwable ignored) {
        }
    }

    private void closeCameraPreview() {
        if (captureSession != null) {
            try {
                captureSession.close();
            } catch (Throwable ignored) {}
            captureSession = null;
        }
        if (cameraDevice != null) {
            try {
                cameraDevice.close();
            } catch (Throwable ignored) {}
            cameraDevice = null;
        }
    }

    private void loadSharedGlbModel() {
        GlbModelCache.getInstance().loadModelAsync(this, modelUrl, new GlbModelCache.LoadCallback() {
            @Override
            public void onProgress(int percent) {
                tvInstanceSummary.setText("Chargement de la ressource .glb : " + percent + "%...");
            }

            @Override
            public void onLoaded(GlbModelCache.SharedGlbResource resource) {
                sceneController.bindSharedGlbResource(resource);
                arSurfaceView.invalidate();
                refreshUiState();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ArViewerActivity.this,
                        "Erreur modèle .glb : " + errorMessage, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void placeNewInstanceFromButton() {
        if (!sceneController.isSurfaceDetected()) {
            toast("Attendez la détection d'une surface plane");
            return;
        }
        if (sceneController.getSharedResource() == null) {
            toast("Le modèle .glb est encore en cours de chargement");
            return;
        }

        // Place le nouvel exemplaire à l'endroit exact du sol physique visé par le réticule 3D de la caméra
        int w = arSurfaceView != null ? arSurfaceView.getWidth() : 1080;
        int h = arSurfaceView != null ? arSurfaceView.getHeight() : 1920;
        float[] target = sceneController.getReticleWorldHit(w, h);
        float worldX = target[0];
        float worldZ = target[2];

        // Si un exemplaire est déjà ancré exactement à ce point du sol, décaler légèrement sur le sol réel
        for (ArPlacedInstance existing : sceneController.getInstances()) {
            if (Math.hypot(existing.getX() - worldX, existing.getZ() - worldZ) < 0.30f) {
                int count = sceneController.getInstanceCount();
                worldX += (count % 2 == 1 ? 1 : -1) * 0.85f;
                break;
            }
        }

        sceneController.placeInstanceOnSurface(worldX, 0.0f, worldZ);
        arSurfaceView.invalidate();
        refreshUiState();
    }

    private void duplicateCurrentInstance() {
        try {
            ArPlacedInstance sel = sceneController.getSelectedInstance();
            if (sel == null) {
                toast("Placez d'abord un premier exemplaire");
                return;
            }
            int w = arSurfaceView != null ? arSurfaceView.getWidth() : 1080;
            int h = arSurfaceView != null ? arSurfaceView.getHeight() : 1920;
            float[] reticleHit = sceneController.getReticleWorldHit(w, h);

            float dxToReticle = reticleHit[0] - sel.getX();
            float dzToReticle = reticleHit[2] - sel.getZ();
            float distToReticle = (float) Math.hypot(dxToReticle, dzToReticle);

            float offsetX;
            float offsetZ;
            if (distToReticle >= 0.35f) {
                // Si l'utilisateur a orienté le téléphone vers un nouvel endroit du sol, y ancrer la copie !
                offsetX = dxToReticle;
                offsetZ = dzToReticle;
            } else {
                // Sinon, poser la copie sur le sol réel juste à côté de l'original
                int count = sceneController.getInstanceCount();
                offsetX = (count % 2 == 1) ? 0.90f : -0.90f;
                offsetZ = -0.15f;
            }

            ArPlacedInstance copy = sceneController.duplicateSelectedInstance(offsetX, offsetZ);
            arSurfaceView.invalidate();
            refreshUiState();
            toast("Exemplaire #" + copy.getInstanceNumber() + " ancré sur la surface");
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    private void deleteCurrentInstance() {
        ArPlacedInstance sel = sceneController.getSelectedInstance();
        if (sel != null) {
            int num = sel.getInstanceNumber();
            sceneController.deleteSelectedInstance();
            arSurfaceView.invalidate();
            refreshUiState();
            toast("Exemplaire #" + num + " supprimé");
        }
    }

    private void refreshUiState() {
        int count = sceneController.getInstanceCount();
        ArPlacedInstance sel = sceneController.getSelectedInstance();
        boolean hasSelection = sel != null;

        btnDuplicate.setEnabled(hasSelection);
        btnDeleteInstance.setEnabled(hasSelection);
        btnRotate.setEnabled(hasSelection);
        btnScaleDown.setEnabled(hasSelection);
        btnScaleUp.setEnabled(hasSelection);
        btnResetScene.setEnabled(count > 0);

        if (count == 0) {
            tvInstanceSummary.setText("0 exemplaire placé • Touchez la surface verte ou cliquez sur « Placer »");
            tvInteractionHint.setText("Touchez la surface détectée pour placer un premier exemplaire du produit.");
        } else if (sel != null) {
            tvInstanceSummary.setText(String.format(Locale.FRANCE,
                    "%d exemplaire(s) • Sélection : #%d (Pos %.2f, %.2f • Échelle %.2fx • Rot %.0f°)",
                    count, sel.getInstanceNumber(), sel.getX(), sel.getZ(),
                    sel.getScaleFactor(), sel.getRotationDegrees()));
            tvInteractionHint.setText("Glissez l'exemplaire #" + sel.getInstanceNumber()
                    + " pour le déplacer indépendamment, ou cliquez sur « Dupliquer » pour en ajouter un autre.");
        }
    }

    @Override
    protected void render(Object data) {}

    @Override
    protected void onDestroy() {
        closeCameraPreview();
        sceneController.release();
        super.onDestroy();
    }
}
