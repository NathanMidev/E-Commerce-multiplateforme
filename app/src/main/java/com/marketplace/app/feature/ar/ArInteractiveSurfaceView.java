package com.marketplace.app.feature.ar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Rôle : Surface interactive de scène ARCore affichant :
 * - La grille de surface détectée (horizontale / verticale) et le réticule de visée
 * - Toutes les instances 3D (.glb) placées et dupliquées indépendamment dans l'environnement
 * - La sélection tactile d'une instance, son déplacement par glisser-déposer et son redimensionnement.
 */
public class ArInteractiveSurfaceView extends View {

    public interface OnSceneInteractionListener {
        void onSceneStateChanged();
    }

    private ArSceneController sceneController;
    private OnSceneInteractionListener interactionListener;

    private final Paint planeGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint reticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path facePath = new Path();

    private final ScaleGestureDetector scaleDetector;
    private boolean isScaling = false;
    private boolean draggingSelectedInstance = false;

    public ArInteractiveSurfaceView(Context context) {
        this(context, null);
    }

    public ArInteractiveSurfaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        planeGridPaint.setColor(Color.parseColor("#6600E676"));
        planeGridPaint.setStyle(Paint.Style.STROKE);
        planeGridPaint.setStrokeWidth(2f);
        planeGridPaint.setPathEffect(new DashPathEffect(new float[]{12f, 10f}, 0f));

        reticlePaint.setColor(Color.parseColor("#00E676"));
        reticlePaint.setStyle(Paint.Style.STROKE);
        reticlePaint.setStrokeWidth(4f);

        selectionRingPaint.setColor(Color.parseColor("#FFAB00"));
        selectionRingPaint.setStyle(Paint.Style.STROKE);
        selectionRingPaint.setStrokeWidth(5f);

        facePaint.setStyle(Paint.Style.FILL);

        edgePaint.setColor(Color.parseColor("#1E293B"));
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(2.2f);

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(26f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (sceneController != null && sceneController.getSelectedInstance() != null) {
                    float current = sceneController.getSelectedInstance().getScaleFactor();
                    sceneController.scaleSelectedInstance(current * detector.getScaleFactor());
                    notifySceneChanged();
                    invalidate();
                }
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScaling = false;
            }
        });
    }

    public void setSceneController(ArSceneController controller) {
        this.sceneController = controller;
        invalidate();
    }

    public void setOnSceneInteractionListener(OnSceneInteractionListener listener) {
        this.interactionListener = listener;
    }

    private void notifySceneChanged() {
        if (interactionListener != null) {
            interactionListener.onSceneStateChanged();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (sceneController == null || !sceneController.isSurfaceDetected()) {
            return super.onTouchEvent(event);
        }
        scaleDetector.onTouchEvent(event);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return true;

        float touchWorldX = screenToWorldX(event.getX(), w);
        float touchWorldZ = screenToWorldZ(event.getY(), h);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                ArPlacedInstance hit = findHitInstance(event.getX(), event.getY(), w, h);
                if (hit != null) {
                    sceneController.selectInstance(hit.getInstanceId());
                    draggingSelectedInstance = true;
                    notifySceneChanged();
                    invalidate();
                } else if (sceneController.getInstanceCount() == 0 && sceneController.getSharedResource() != null) {
                    // Premier toucher sur la surface détectée : place directement le premier exemplaire
                    sceneController.placeInstanceOnSurface(touchWorldX, 0f, touchWorldZ);
                    draggingSelectedInstance = true;
                    notifySceneChanged();
                    invalidate();
                } else {
                    draggingSelectedInstance = false;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isScaling && event.getPointerCount() == 1 && draggingSelectedInstance
                        && sceneController.getSelectedInstance() != null) {
                    sceneController.moveSelectedInstanceTo(touchWorldX, 0f, touchWorldZ);
                    notifySceneChanged();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingSelectedInstance = false;
                isScaling = false;
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private ArPlacedInstance findHitInstance(float screenX, float screenY, int w, int h) {
        List<ArPlacedInstance> list = sceneController.getInstances();
        float hitRadiusPx = 95f;
        for (int i = list.size() - 1; i >= 0; i--) {
            ArPlacedInstance inst = list.get(i);
            float ix = worldToScreenX(inst.getX(), w);
            float iy = worldToScreenY(inst.getZ(), h);
            float dx = screenX - ix;
            float dy = screenY - iy;
            if (Math.hypot(dx, dy) <= hitRadiusPx * Math.max(0.75f, inst.getScaleFactor())) {
                return inst;
            }
        }
        return null;
    }

    public static float screenToWorldX(float screenX, int width) {
        if (width <= 0) return 0f;
        return ((screenX - width / 2f) / (width * 0.35f));
    }

    public static float screenToWorldZ(float screenY, int height) {
        if (height <= 0) return 0f;
        return ((screenY - height * 0.56f) / (height * 0.25f));
    }

    private static float worldToScreenX(float worldX, int width) {
        return width / 2f + worldX * (width * 0.35f);
    }

    private static float worldToScreenY(float worldZ, int height) {
        return height * 0.56f + worldZ * (height * 0.25f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || sceneController == null) return;

        // 1. Indicateur visuel de surface détectée (grille ARCore)
        if (sceneController.isSurfaceDetected()) {
            float topY = h * 0.36f;
            float bottomY = h * 0.78f;
            for (int i = 0; i <= 6; i++) {
                float frac = i / 6.0f;
                float y = topY + frac * (bottomY - topY);
                float margin = (1.0f - frac) * (w * 0.22f);
                canvas.drawLine(margin, y, w - margin, y, planeGridPaint);
            }
            for (int j = -3; j <= 3; j++) {
                float topX = w / 2f + j * (w * 0.08f);
                float botX = w / 2f + j * (w * 0.15f);
                canvas.drawLine(topX, topY, botX, bottomY, planeGridPaint);
            }

            // Réticule central de surface
            float rx = w / 2f;
            float ry = h * 0.56f;
            canvas.drawOval(rx - 55f, ry - 26f, rx + 55f, ry + 26f, reticlePaint);
            canvas.drawCircle(rx, ry, 6f, reticlePaint);
        }

        // 2. Rendu de chaque instance placée indépendamment
        List<ArPlacedInstance> instances = sceneController.getInstances();
        ArPlacedInstance selected = sceneController.getSelectedInstance();

        for (ArPlacedInstance inst : instances) {
            drawPlacedInstance(canvas, inst, inst == selected, w, h);
        }
    }

    private void drawPlacedInstance(Canvas canvas, ArPlacedInstance inst, boolean isSelected, int w, int h) {
        float cx = worldToScreenX(inst.getX(), w);
        float cy = worldToScreenY(inst.getZ(), h);
        float unitScale = Math.min(w, h) * 0.14f * inst.getScaleFactor();

        if (isSelected) {
            float ringW = Math.max(55f, 80f * inst.getScaleFactor());
            float ringH = Math.max(28f, 40f * inst.getScaleFactor());
            canvas.drawOval(cx - ringW, cy + unitScale * 0.35f - ringH,
                    cx + ringW, cy + unitScale * 0.35f + ringH, selectionRingPaint);
        }

        GlbModelCache.SharedGlbResource res = inst.getSharedResource();
        if (res != null && res.getVertices() != null && res.getFaces() != null) {
            float[][] verts = res.getVertices();
            int[][] faces = res.getFaces();
            double rad = Math.toRadians(inst.getRotationDegrees() + 28.0);
            float cosY = (float) Math.cos(rad);
            float sinY = (float) Math.sin(rad);
            float cosP = (float) Math.cos(Math.toRadians(18.0));
            float sinP = (float) Math.sin(Math.toRadians(18.0));

            boolean isBlueBubble = res.getSourceUrl() != null
                    && res.getSourceUrl().toLowerCase().contains("base_basic_shaded");
            int[] colors;
            if (isBlueBubble) {
                colors = isSelected
                        ? new int[]{Color.parseColor("#2563EB"), Color.parseColor("#3B82F6"), Color.parseColor("#1D4ED8")}
                        : new int[]{Color.parseColor("#1E40AF"), Color.parseColor("#1D4ED8"), Color.parseColor("#1E3A8A")};
            } else {
                colors = isSelected
                        ? new int[]{Color.parseColor("#FF6D00"), Color.parseColor("#FF9100"), Color.parseColor("#E65100")}
                        : new int[]{Color.parseColor("#00897B"), Color.parseColor("#26A69A"), Color.parseColor("#00695C")};
            }

            for (int fIdx = 0; fIdx < faces.length; fIdx++) {
                int[] f = faces[fIdx];
                facePath.reset();
                for (int k = 0; k < f.length; k++) {
                    float vx = verts[f[k]][0];
                    float vy = verts[f[k]][1];
                    float vz = verts[f[k]][2];
                    float rx = vx * cosY - vz * sinY;
                    float rz = vx * sinY + vz * cosY;
                    float ry = vy * cosP - rz * sinP;

                    float px = cx + rx * unitScale;
                    float py = cy - ry * unitScale;
                    if (k == 0) facePath.moveTo(px, py);
                    else facePath.lineTo(px, py);
                }
                facePath.close();
                facePaint.setColor(colors[fIdx % colors.length]);
                canvas.drawPath(facePath, facePaint);
                canvas.drawPath(facePath, edgePaint);
            }
        }

        String tag = String.format(Locale.FRANCE, "#%d (%.1fx • %.0f°)",
                inst.getInstanceNumber(), inst.getScaleFactor(), inst.getRotationDegrees());
        canvas.drawText(tag, cx, cy - unitScale - 14f, labelPaint);
    }
}
