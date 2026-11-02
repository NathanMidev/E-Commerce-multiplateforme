package com.marketplace.app.feature.ar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.marketplace.app.R;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.navigation.Router;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Rôle : Expérience de Réalité Augmentée suivant l'architecture officielle de la documentation Google ARCore :
 * - Cycle de vie officiel ArCoreApk.getInstance().requestInstall(...) et Session (resume / pause / close)
 * - GLSurfaceView OpenGL ES 2.0 + ArCameraBackgroundRenderer (Session.setCameraTextureName & Session.update())
 * - Détection de surfaces native ARCore via Session.getAllTrackables(Plane.class) et Plane.getPolygon()
 * - Placement, déplacement et duplication via Frame.hitTest(x, y) et HitResult.createAnchor() / Session.createAnchor(Pose)
 * - Projection 3D des modèles .glb via les matrices natives Camera.getViewMatrix() et Camera.getProjectionMatrix().
 */
public class ArViewerActivity extends BaseActivity implements GLSurfaceView.Renderer {

    private static final int REQ_CAMERA_PERMISSION = 4102;

    private GLSurfaceView arGlSurfaceView;
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
    private final ArCameraBackgroundRenderer backgroundRenderer = new ArCameraBackgroundRenderer();

    private Session arCoreSession;
    private boolean installRequested = false;

    private int viewportWidth = 0;
    private int viewportHeight = 0;
    private boolean viewportChanged = false;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];

    private int lastTrackedPlaneCount = -1;

    private interface FrameAction {
        void execute(Frame frame, Session session);
    }

    private final ConcurrentLinkedQueue<FrameAction> pendingFrameActions = new ConcurrentLinkedQueue<>();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ar_viewer;
    }

    @Override
    protected void bindViews() {
        productId = getIntent().getStringExtra(Router.EXTRA_PRODUCT_ID);
        productName = getIntent().getStringExtra("EXTRA_PRODUCT_NAME");
        modelUrl = getIntent().getStringExtra(Router.EXTRA_MODEL_URL);

        arGlSurfaceView = findViewById(R.id.ar_gl_surface_view);
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

        // Configuration officielle OpenGL ES 2.0 pour ARCore
        arGlSurfaceView.setPreserveEGLContextOnPause(true);
        arGlSurfaceView.setEGLContextClientVersion(2);
        arGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        arGlSurfaceView.setRenderer(this);
        arGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        arGlSurfaceView.setWillNotDraw(false);

        arSurfaceView.setSceneController(sceneController);
        arSurfaceView.setOnSceneInteractionListener(this::refreshUiState);
        arSurfaceView.setOnNativeArHitTestListener(new ArInteractiveSurfaceView.OnNativeArHitTestListener() {
            @Override
            public void onTapSurface(float screenX, float screenY) {
                enqueuePlaceAtScreenPoint(screenX, screenY);
            }

            @Override
            public void onDragSelectedInstance(float screenX, float screenY) {
                enqueueDragSelectedToScreenPoint(screenX, screenY);
            }
        });

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
            pendingFrameActions.add((frame, session) -> {
                sceneController.resetScene();
                runOnUiThread(() -> {
                    arSurfaceView.invalidate();
                    refreshUiState();
                    toast("Scène ARCore réinitialisée (ancres détachées)");
                });
            });
        });
    }

    @Override
    protected void loadData() {
        if (!GlbValidator.isValidGlbExtension(modelUrl)) {
            Toast.makeText(this,
                    "Modèle 3D invalide ou manquant : seul un fichier .glb est accepté en ARCore.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        loadSharedGlbModel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
            return;
        }

        try {
            if (arCoreSession == null) {
                ArCoreApk.InstallStatus installStatus =
                        ArCoreApk.getInstance().requestInstall(this, !installRequested);
                if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    installRequested = true;
                    return;
                }
                arCoreSession = new Session(this);
                Config config = new Config(arCoreSession);
                config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
                config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                arCoreSession.configure(config);
            }

            arCoreSession.resume();
            arGlSurfaceView.onResume();
        } catch (Exception e) {
            Toast.makeText(this,
                    "Impossible de démarrer la session ARCore : " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (arGlSurfaceView != null) {
            arGlSurfaceView.onPause();
        }
        if (arCoreSession != null) {
            arCoreSession.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "L'autorisation caméra est obligatoire pour la détection de surfaces ARCore.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // =========================================================================
    // GLSurfaceView.Renderer — Pipeline officiel Google ARCore
    // =========================================================================

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        backgroundRenderer.createOnGlThread();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        viewportWidth = width;
        viewportHeight = height;
        viewportChanged = true;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (arCoreSession == null) {
            return;
        }

        if (viewportChanged && viewportWidth > 0 && viewportHeight > 0) {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            Display display = wm != null ? wm.getDefaultDisplay() : null;
            int rotation = display != null ? display.getRotation() : 0;
            arCoreSession.setDisplayGeometry(rotation, viewportWidth, viewportHeight);
            viewportChanged = false;
        }

        try {
            arCoreSession.setCameraTextureName(backgroundRenderer.getTextureId());
            Frame frame = arCoreSession.update();
            Camera camera = frame.getCamera();

            // 1. Dessiner le flux vidéo de la caméra ARCore
            backgroundRenderer.draw(frame);

            if (camera.getTrackingState() != TrackingState.TRACKING) {
                return;
            }

            // 2. Récupérer les matrices 4x4 natives ARCore (Projection & View) et la Pose caméra
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);
            camera.getViewMatrix(viewMatrix, 0);
            Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            Pose camPose = camera.getPose();
            sceneController.setNativeArCoreViewProjection(
                    viewProjectionMatrix, camPose.tx(), camPose.ty(), camPose.tz()
            );

            // 3. Détection native des surfaces ARCore : Session.getAllTrackables(Plane.class)
            Collection<Plane> allPlanes = arCoreSession.getAllTrackables(Plane.class);
            List<float[][]> planePolygonsWorld = new ArrayList<>();
            String detectedType = "HORIZONTAL";

            for (Plane plane : allPlanes) {
                if (plane.getTrackingState() != TrackingState.TRACKING || plane.getSubsumedBy() != null) {
                    continue;
                }
                if (plane.getType() == Plane.Type.VERTICAL) {
                    detectedType = "VERTICAL";
                }
                FloatBuffer polygonBuf = plane.getPolygon();
                if (polygonBuf == null) continue;
                polygonBuf.rewind();
                int numPoints = polygonBuf.remaining() / 2;
                if (numPoints < 3) continue;

                Pose centerPose = plane.getCenterPose();
                float[][] worldPoly = new float[numPoints][3];
                float[] localPt = new float[3];
                for (int i = 0; i < numPoints; i++) {
                    localPt[0] = polygonBuf.get(i * 2);
                    localPt[1] = 0.0f;
                    localPt[2] = polygonBuf.get(i * 2 + 1);
                    worldPoly[i] = centerPose.transformPoint(localPt);
                }
                planePolygonsWorld.add(worldPoly);
            }

            sceneController.setTrackedPlanePolygons(planePolygonsWorld);
            int trackedCount = planePolygonsWorld.size();
            if (trackedCount > 0) {
                sceneController.onSurfaceDetected(detectedType);
            }

            if (trackedCount != lastTrackedPlaneCount) {
                lastTrackedPlaneCount = trackedCount;
                runOnUiThread(() -> {
                    if (trackedCount > 0) {
                        tvPlaneStatus.setText(String.format(Locale.FRANCE,
                                "✔ %d surface(s) plane(s) ARCore détectée(s) • Prêt à ancrer", trackedCount));
                    } else {
                        tvPlaneStatus.setText("Déplacez lentement le téléphone pour scanner le sol ou une table (ARCore)...");
                    }
                    refreshUiState();
                });
            }

            // 4. Hit-test natif ARCore au centre de l'écran pour le réticule 3D sur le plan
            if (viewportWidth > 0 && viewportHeight > 0) {
                HitResult centerHit = findBestSurfaceHit(frame, viewportWidth * 0.5f, viewportHeight * 0.54f);
                if (centerHit != null) {
                    Pose hitPose = centerHit.getHitPose();
                    sceneController.setNativeReticleHit(true, hitPose.tx(), hitPose.ty(), hitPose.tz());
                } else {
                    sceneController.setNativeReticleHit(false, 0f, 0f, 0f);
                }
            }

            // 5. Exécuter les actions d'ancrage ARCore en attente (Placer, Toucher, Glisser, Dupliquer)
            FrameAction action;
            boolean stateChanged = false;
            while ((action = pendingFrameActions.poll()) != null) {
                action.execute(frame, arCoreSession);
                stateChanged = true;
            }

            // 6. Mettre à jour la position 3D de chaque instance à partir de sa vraie Anchor matérielle ARCore
            for (ArPlacedInstance inst : sceneController.getInstances()) {
                Anchor anchor = inst.getArCoreAnchor();
                if (anchor != null && anchor.getTrackingState() == TrackingState.TRACKING) {
                    Pose anchorPose = anchor.getPose();
                    inst.moveTo(anchorPose.tx(), anchorPose.ty(), anchorPose.tz());
                }
            }

            // 7. Rafraîchir l'affichage 3D superposé
            if (arSurfaceView != null) {
                arSurfaceView.postInvalidateOnAnimation();
            }
            if (stateChanged) {
                runOnUiThread(this::refreshUiState);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Effectue le test d'intersection natif Frame.hitTest(px, py) sur les surfaces Plane (ou Point orienté) d'ARCore.
     */
    private static HitResult findBestSurfaceHit(Frame frame, float screenX, float screenY) {
        List<HitResult> hits = frame.hitTest(screenX, screenY);
        HitResult fallbackPlaneHit = null;
        for (HitResult hit : hits) {
            Trackable trackable = hit.getTrackable();
            if (trackable instanceof Plane) {
                Plane plane = (Plane) trackable;
                if (plane.getTrackingState() == TrackingState.TRACKING) {
                    if (plane.isPoseInPolygon(hit.getHitPose())) {
                        return hit;
                    }
                    if (fallbackPlaneHit == null) {
                        fallbackPlaneHit = hit;
                    }
                }
            } else if (trackable instanceof Point) {
                Point point = (Point) trackable;
                if (point.getOrientationMode() == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                        && fallbackPlaneHit == null) {
                    fallbackPlaneHit = hit;
                }
            }
        }
        return fallbackPlaneHit;
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

    private void enqueuePlaceAtScreenPoint(float screenX, float screenY) {
        pendingFrameActions.add((frame, session) -> {
            HitResult hit = findBestSurfaceHit(frame, screenX, screenY);
            if (hit == null) {
                runOnUiThread(() -> toast("Scannez d'abord une surface plane (grille verte ARCore) avant de placer"));
                return;
            }
            Anchor anchor = hit.createAnchor();
            Pose p = anchor.getPose();
            sceneController.onSurfaceDetected("HORIZONTAL");
            ArPlacedInstance placed = sceneController.placeInstanceOnSurface(p.tx(), p.ty(), p.tz());
            placed.setArCoreAnchor(anchor);
        });
    }

    private void enqueueDragSelectedToScreenPoint(float screenX, float screenY) {
        pendingFrameActions.add((frame, session) -> {
            ArPlacedInstance sel = sceneController.getSelectedInstance();
            if (sel == null) return;
            HitResult hit = findBestSurfaceHit(frame, screenX, screenY);
            if (hit != null) {
                Anchor newAnchor = hit.createAnchor();
                Pose p = newAnchor.getPose();
                sel.setArCoreAnchor(newAnchor);
                sel.moveTo(p.tx(), p.ty(), p.tz());
            }
        });
    }

    private void placeNewInstanceFromButton() {
        if (!sceneController.isSurfaceDetected()) {
            toast("Scannez d'abord le sol ou une table jusqu'à voir la surface verte ARCore");
            return;
        }
        if (sceneController.getSharedResource() == null) {
            toast("Le modèle .glb est encore en cours de chargement");
            return;
        }
        float cx = viewportWidth > 0 ? viewportWidth * 0.5f : 540f;
        float cy = viewportHeight > 0 ? viewportHeight * 0.54f : 1000f;
        enqueuePlaceAtScreenPoint(cx, cy);
    }

    private void duplicateCurrentInstance() {
        ArPlacedInstance sel = sceneController.getSelectedInstance();
        if (sel == null) {
            toast("Placez d'abord un premier exemplaire sur une surface ARCore");
            return;
        }

        pendingFrameActions.add((frame, session) -> {
            ArPlacedInstance currentSel = sceneController.getSelectedInstance();
            if (currentSel == null) return;

            float cx = viewportWidth > 0 ? viewportWidth * 0.5f : 540f;
            float cy = viewportHeight > 0 ? viewportHeight * 0.54f : 1000f;
            HitResult centerHit = findBestSurfaceHit(frame, cx, cy);

            Anchor copyAnchor = null;
            float targetX;
            float targetY;
            float targetZ;

            if (centerHit != null) {
                Pose hitPose = centerHit.getHitPose();
                double distFromOriginal = Math.hypot(
                        hitPose.tx() - currentSel.getX(),
                        hitPose.tz() - currentSel.getZ()
                );
                if (distFromOriginal >= 0.30) {
                    // Ancrer la copie exactement au point de la surface ARCore visé par la caméra
                    copyAnchor = centerHit.createAnchor();
                    Pose p = copyAnchor.getPose();
                    targetX = p.tx();
                    targetY = p.ty();
                    targetZ = p.tz();
                } else {
                    // Visée trop proche de l'original : créer une nouvelle Anchor ARCore décalée sur le même plan
                    int count = sceneController.getInstanceCount();
                    float stepX = (count % 2 == 1) ? 0.85f : -0.85f;
                    targetX = currentSel.getX() + stepX;
                    targetY = currentSel.getY();
                    targetZ = currentSel.getZ();
                    Pose offsetPose = new Pose(
                            new float[]{targetX, targetY, targetZ},
                            hitPose.getRotationQuaternion()
                    );
                    copyAnchor = session.createAnchor(offsetPose);
                }
            } else {
                int count = sceneController.getInstanceCount();
                float stepX = (count % 2 == 1) ? 0.85f : -0.85f;
                targetX = currentSel.getX() + stepX;
                targetY = currentSel.getY();
                targetZ = currentSel.getZ();
                Pose offsetPose = Pose.makeTranslation(targetX, targetY, targetZ);
                copyAnchor = session.createAnchor(offsetPose);
            }

            ArPlacedInstance copy = sceneController.duplicateSelectedInstanceTo(
                    targetX, targetY, targetZ, copyAnchor
            );
            runOnUiThread(() -> {
                arSurfaceView.invalidate();
                refreshUiState();
                toast("Exemplaire #" + copy.getInstanceNumber() + " ancré via ARCore Anchor");
            });
        });
    }

    private void deleteCurrentInstance() {
        ArPlacedInstance sel = sceneController.getSelectedInstance();
        if (sel != null) {
            int num = sel.getInstanceNumber();
            sceneController.deleteSelectedInstance();
            arSurfaceView.invalidate();
            refreshUiState();
            toast("Exemplaire #" + num + " supprimé (Anchor détachée)");
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
            tvInstanceSummary.setText("0 exemplaire placé • Visez le polygone vert ARCore et appuyez sur « Placer »");
            tvInteractionHint.setText("Scannez le sol avec la caméra jusqu'à l'apparition du polygone vert ARCore, puis touchez-le.");
        } else if (sel != null) {
            tvInstanceSummary.setText(String.format(Locale.FRANCE,
                    "%d exemplaire(s) ancré(s) • Sélection : #%d (Pos %.2f, %.2f • Échelle %.2fx • Rot %.0f°)",
                    count, sel.getInstanceNumber(), sel.getX(), sel.getZ(),
                    sel.getScaleFactor(), sel.getRotationDegrees()));
            tvInteractionHint.setText("Visez un autre endroit du polygone ARCore et appuyez sur « ➕ Dupliquer », ou glissez l'exemplaire #" + sel.getInstanceNumber() + ".");
        }
    }

    @Override
    protected void render(Object data) {}

    @Override
    protected void onDestroy() {
        sceneController.release();
        if (arCoreSession != null) {
            try {
                arCoreSession.close();
            } catch (Throwable ignored) {
            }
            arCoreSession = null;
        }
        super.onDestroy();
    }
}
