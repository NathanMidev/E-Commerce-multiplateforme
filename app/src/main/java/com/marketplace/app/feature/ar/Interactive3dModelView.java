package com.marketplace.app.feature.ar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Rôle : Visualiseur 3D interactif pour les modèles .glb des produits locaux (Section 5).
 * Permet :
 * - La rotation 360° (horizontale et verticale) au toucher (1 doigt) pour observer sous tous les angles
 * - Le zoom avant / arrière par pincement (2 doigts) ou contrôle programmatique
 * - Le rendu perspectif 3D avec tri de profondeur (algorithme du peintre) et grille au sol
 * - La libération explicite des ressources 3D à la fermeture de l'écran.
 */
public class Interactive3dModelView extends View {

    public static final float MIN_ZOOM = 0.45f;
    public static final float MAX_ZOOM = 3.20f;
    public static final float MIN_PITCH = -75.0f;
    public static final float MAX_PITCH = 75.0f;

    private GlbModelCache.SharedGlbResource modelResource;
    private float yawDegrees = 32.0f;
    private float pitchDegrees = 22.0f;
    private float zoomFactor = 1.0f;

    private float lastTouchX;
    private float lastTouchY;
    private boolean isScaling = false;
    private final ScaleGestureDetector scaleDetector;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path facePath = new Path();

    public Interactive3dModelView(Context context) {
        this(context, null);
    }

    public Interactive3dModelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gridPaint.setColor(Color.parseColor("#CBD5E1"));
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        facePaint.setStyle(Paint.Style.FILL);

        edgePaint.setColor(Color.parseColor("#1E293B"));
        edgePaint.setStrokeWidth(2.2f);
        edgePaint.setStyle(Paint.Style.STROKE);

        hudPaint.setColor(Color.parseColor("#475569"));
        hudPaint.setTextSize(26f);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                setZoomFactor(zoomFactor * detector.getScaleFactor());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScaling = false;
            }
        });
    }

    public void setModelResource(GlbModelCache.SharedGlbResource resource) {
        this.modelResource = resource;
        invalidate();
    }

    public GlbModelCache.SharedGlbResource getModelResource() {
        return modelResource;
    }

    public boolean hasLoadedModel() {
        return modelResource != null;
    }

    public float getYawDegrees() {
        return yawDegrees;
    }

    public float getPitchDegrees() {
        return pitchDegrees;
    }

    public float getZoomFactor() {
        return zoomFactor;
    }

    /**
     * Applique une rotation incrémentale (en degrés) autour des axes Y (yaw) et X (pitch).
     */
    public void rotateBy(float deltaYawDegrees, float deltaPitchDegrees) {
        this.yawDegrees = ((this.yawDegrees + deltaYawDegrees) % 360.0f + 360.0f) % 360.0f;
        this.pitchDegrees = clamp(this.pitchDegrees + deltaPitchDegrees, MIN_PITCH, MAX_PITCH);
        invalidate();
    }

    /**
     * Définit le facteur de zoom en le bornant entre MIN_ZOOM et MAX_ZOOM.
     */
    public void setZoomFactor(float newZoom) {
        this.zoomFactor = clamp(newZoom, MIN_ZOOM, MAX_ZOOM);
        invalidate();
    }

    public void resetCameraView() {
        this.yawDegrees = 32.0f;
        this.pitchDegrees = 22.0f;
        this.zoomFactor = 1.0f;
        invalidate();
    }

    /**
     * Libère les ressources 3D lorsque l'activité est fermée (Section 10).
     */
    public void releaseResources() {
        this.modelResource = null;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!isScaling && event.getPointerCount() == 1) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    rotateBy(dx * 0.45f, dy * 0.35f);
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isScaling = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2.0f;
        float cy = h * 0.54f;
        float baseScale = Math.min(w, h) * 0.28f * zoomFactor;

        if (modelResource == null) {
            hudPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Modèle 3D .glb non chargé", cx, cy, hudPaint);
            return;
        }

        double yawRad = Math.toRadians(yawDegrees);
        double pitchRad = Math.toRadians(pitchDegrees);
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);
        float cosP = (float) Math.cos(pitchRad);
        float sinP = (float) Math.sin(pitchRad);

        // 1. Dessiner le plateau circulaire / grille 3D au sol (y = -0.55f)
        for (int g = -2; g <= 2; g++) {
            float offset = g * 0.45f;
            float[] p1 = projectPoint(-0.9f, -0.56f, offset, cosY, sinY, cosP, sinP, cx, cy, baseScale);
            float[] p2 = projectPoint(0.9f, -0.56f, offset, cosY, sinY, cosP, sinP, cx, cy, baseScale);
            canvas.drawLine(p1[0], p1[1], p2[0], p2[1], gridPaint);

            float[] p3 = projectPoint(offset, -0.56f, -0.9f, cosY, sinY, cosP, sinP, cx, cy, baseScale);
            float[] p4 = projectPoint(offset, -0.56f, 0.9f, cosY, sinY, cosP, sinP, cx, cy, baseScale);
            canvas.drawLine(p3[0], p3[1], p4[0], p4[1], gridPaint);
        }

        // 2. Projeter les sommets du maillage .glb
        float[][] vertices = modelResource.getVertices();
        int[][] faces = modelResource.getFaces();
        if (vertices == null || faces == null) return;

        float[][] projected = new float[vertices.length][3];
        for (int i = 0; i < vertices.length; i++) {
            projected[i] = projectPoint(
                    vertices[i][0], vertices[i][1], vertices[i][2],
                    cosY, sinY, cosP, sinP, cx, cy, baseScale
            );
        }

        // 3. Trier les faces par profondeur Z (algorithme du peintre)
        List<ProjectedFace> sortedFaces = new ArrayList<>(faces.length);
        for (int i = 0; i < faces.length; i++) {
            int[] f = faces[i];
            float avgZ = 0f;
            for (int idx : f) {
                avgZ += projected[idx][2];
            }
            avgZ /= f.length;
            sortedFaces.add(new ProjectedFace(f, avgZ, i));
        }
        Collections.sort(sortedFaces, (a, b) -> Float.compare(b.depthZ, a.depthZ));

        // 4. Dessiner chaque face ombrée et ses arêtes
        int[] palette = {
                Color.parseColor("#FF7A1A"),
                Color.parseColor("#F97316"),
                Color.parseColor("#EA580C"),
                Color.parseColor("#FB923C"),
                Color.parseColor("#C2410C"),
                Color.parseColor("#FDBA74")
        };

        for (ProjectedFace pf : sortedFaces) {
            facePath.reset();
            for (int k = 0; k < pf.indices.length; k++) {
                float px = projected[pf.indices[k]][0];
                float py = projected[pf.indices[k]][1];
                if (k == 0) facePath.moveTo(px, py);
                else facePath.lineTo(px, py);
            }
            facePath.close();

            facePaint.setColor(palette[pf.faceIndex % palette.length]);
            canvas.drawPath(facePath, facePaint);
            canvas.drawPath(facePath, edgePaint);
        }

        // 5. Indicateur discret d'angle et de zoom en bas à gauche
        hudPaint.setTextAlign(Paint.Align.LEFT);
        String hud = String.format(Locale.FRANCE,
                "GLB 3D • Angle %.0f°/%.0f° • Zoom %.1fx (Glisser / Pincer)",
                yawDegrees, pitchDegrees, zoomFactor);
        canvas.drawText(hud, 20f, h - 20f, hudPaint);
    }

    private static float[] projectPoint(float x, float y, float z,
                                        float cosY, float sinY, float cosP, float sinP,
                                        float cx, float cy, float scale) {
        // Rotation autour de Y (Yaw)
        float rx = x * cosY - z * sinY;
        float rz = x * sinY + z * cosY;
        // Rotation autour de X (Pitch)
        float ry = y * cosP - rz * sinP;
        float rzFinal = y * sinP + rz * cosP;

        float cameraDist = 3.8f;
        float perspective = cameraDist / Math.max(1.2f, cameraDist + rzFinal * 0.45f);
        float screenX = cx + rx * scale * perspective;
        float screenY = cy - ry * scale * perspective;
        return new float[]{screenX, screenY, rzFinal};
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static final class ProjectedFace {
        final int[] indices;
        final float depthZ;
        final int faceIndex;

        ProjectedFace(int[] indices, float depthZ, int faceIndex) {
            this.indices = indices;
            this.depthZ = depthZ;
            this.faceIndex = faceIndex;
        }
    }
}
