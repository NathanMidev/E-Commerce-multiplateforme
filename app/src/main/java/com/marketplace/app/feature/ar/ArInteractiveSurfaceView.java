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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Rôle : Surface interactive de scène ARCore en espace réel 3D (World Space) affichant :
 * - La grille de surface physique ancrée sur le sol (Y = 0) et le réticule 3D projeté au sol
 * - Toutes les instances 3D (.glb) ancrées indépendamment sur la surface physique (qui restent fixes
 *   au sol lorsque le téléphone bouge, tourne ou s'incline)
 * - La sélection tactile 3D d'une instance, son déplacement par ray-casting sur le plan du sol et son redimensionnement.
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
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path facePath = new Path();
    private final Path ringPath = new Path();

    private final ScaleGestureDetector scaleDetector;
    private boolean isScaling = false;
    private boolean draggingSelectedInstance = false;

    public ArInteractiveSurfaceView(Context context) {
        this(context, null);
    }

    public ArInteractiveSurfaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        planeGridPaint.setColor(Color.parseColor("#5500E676"));
        planeGridPaint.setStyle(Paint.Style.STROKE);
        planeGridPaint.setStrokeWidth(2f);
        planeGridPaint.setPathEffect(new DashPathEffect(new float[]{14f, 10f}, 0f));

        reticlePaint.setColor(Color.parseColor("#00E676"));
        reticlePaint.setStyle(Paint.Style.STROKE);
        reticlePaint.setStrokeWidth(4.5f);

        selectionRingPaint.setColor(Color.parseColor("#FFAB00"));
        selectionRingPaint.setStyle(Paint.Style.STROKE);
        selectionRingPaint.setStrokeWidth(5f);

        shadowPaint.setColor(Color.parseColor("#55000000"));
        shadowPaint.setStyle(Paint.Style.FILL);

        facePaint.setStyle(Paint.Style.FILL);

        edgePaint.setColor(Color.parseColor("#440F172A"));
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(1.5f);

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(25f);
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        labelBgPaint.setColor(Color.parseColor("#B30F172A"));
        labelBgPaint.setStyle(Paint.Style.FILL);

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

        float[] floorHit = sceneController.screenRayToFloorPlane(event.getX(), event.getY(), w, h);
        float touchWorldX = floorHit[0];
        float touchWorldZ = floorHit[2];

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                ArPlacedInstance hit = findHitInstance(event.getX(), event.getY(), w, h);
                if (hit != null) {
                    sceneController.selectInstance(hit.getInstanceId());
                    draggingSelectedInstance = true;
                    notifySceneChanged();
                    invalidate();
                } else if (sceneController.getInstanceCount() == 0 && sceneController.getSharedResource() != null) {
                    // Premier toucher sur la surface détectée : ancre directement le 1er exemplaire à ce point 3D du sol
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
                    // Met uniquement à jour l'ancre 3D de l'exemplaire sélectionné sur le plan du sol (Y = 0)
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
        ArPlacedInstance closestHit = null;
        float bestDistSq = Float.MAX_VALUE;

        for (int i = list.size() - 1; i >= 0; i--) {
            ArPlacedInstance inst = list.get(i);
            float centerWorldY = inst.getY() + 0.22f * inst.getScaleFactor();
            float[] proj = sceneController.projectWorldToScreen(inst.getX(), centerWorldY, inst.getZ(), w, h);
            float depth = proj[2];
            if (depth <= 0.15f) continue;

            float focal = Math.min(w, h) * 0.92f;
            float hitRadiusPx = Math.max(70f, (0.65f * inst.getScaleFactor() / depth) * focal);
            float dx = screenX - proj[0];
            float dy = screenY - proj[1];
            float dSq = dx * dx + dy * dy;
            if (dSq <= hitRadiusPx * hitRadiusPx && dSq < bestDistSq) {
                bestDistSq = dSq;
                closestHit = inst;
            }
        }
        return closestHit;
    }

    public static float screenToWorldX(float screenX, int width) {
        if (width <= 0) return 0f;
        return ((screenX - width / 2f) / (width * 0.35f));
    }

    public static float screenToWorldZ(float screenY, int height) {
        if (height <= 0) return 0f;
        return ((screenY - height * 0.56f) / (height * 0.25f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || sceneController == null) return;

        // 1. Grille 3D du plan de surface ancrée dans l'espace réel (Y = 0) et réticule 3D au sol
        if (sceneController.isSurfaceDetected()) {
            drawWorldFloorGrid(canvas, w, h);
            drawWorldFloorReticle(canvas, w, h);
        }

        // 2. Trier les instances placées de la plus éloignée à la plus proche de la caméra 3D
        List<ArPlacedInstance> instances = new ArrayList<>(sceneController.getInstances());
        final float camX = sceneController.getCameraX();
        final float camY = sceneController.getCameraY();
        final float camZ = sceneController.getCameraZ();
        Collections.sort(instances, (a, b) -> {
            float distA = sqDist(a.getX() - camX, a.getY() - camY, a.getZ() - camZ);
            float distB = sqDist(b.getX() - camX, b.getY() - camY, b.getZ() - camZ);
            return Float.compare(distB, distA);
        });

        ArPlacedInstance selected = sceneController.getSelectedInstance();
        for (ArPlacedInstance inst : instances) {
            drawPlacedInstanceInWorldSpace(canvas, inst, inst == selected, w, h);
        }
    }

    private static float sqDist(float dx, float dy, float dz) {
        return dx * dx + dy * dy + dz * dz;
    }

    private void drawWorldFloorGrid(Canvas canvas, int w, int h) {
        // Grille fixe dans le monde réel : X de -2.4m à +2.4m, Z de -0.9m à -4.5m sur le plan Y = 0
        float minX = -2.4f;
        float maxX = 2.4f;
        float minZ = -4.5f;
        float maxZ = -0.9f;

        for (int i = 0; i <= 6; i++) {
            float z = maxZ + (i / 6.0f) * (minZ - maxZ);
            drawClippedFloorSegment(canvas, minX, z, maxX, z, w, h);
        }
        for (int j = -4; j <= 4; j++) {
            float x = j * 0.6f;
            drawClippedFloorSegment(canvas, x, maxZ, x, minZ, w, h);
        }
    }

    private void drawClippedFloorSegment(Canvas canvas, float x1, float z1, float x2, float z2, int w, int h) {
        int steps = 6;
        float prevSx = 0f, prevSy = 0f;
        boolean prevValid = false;
        for (int s = 0; s <= steps; s++) {
            float t = s / (float) steps;
            float wx = x1 + t * (x2 - x1);
            float wz = z1 + t * (z2 - z1);
            float[] proj = sceneController.projectWorldToScreen(wx, 0.0f, wz, w, h);
            boolean valid = proj[2] > 0.20f
                    && proj[0] > -w * 0.5f && proj[0] < w * 1.5f
                    && proj[1] > -h * 0.5f && proj[1] < h * 1.5f;
            if (valid && prevValid) {
                canvas.drawLine(prevSx, prevSy, proj[0], proj[1], planeGridPaint);
            }
            prevSx = proj[0];
            prevSy = proj[1];
            prevValid = valid;
        }
    }

    private void drawWorldFloorReticle(Canvas canvas, int w, int h) {
        float[] hit = sceneController.getReticleWorldHit(w, h);
        drawFloorCircle3D(canvas, hit[0], hit[2], 0.22f, reticlePaint, false, w, h);
        float[] centerProj = sceneController.projectWorldToScreen(hit[0], 0.0f, hit[2], w, h);
        if (centerProj[2] > 0.15f) {
            canvas.drawCircle(centerProj[0], centerProj[1], 5.5f, reticlePaint);
        }
    }

    private void drawFloorCircle3D(Canvas canvas, float centerX, float centerZ, float radiusMeters,
                                   Paint paint, boolean fill, int w, int h) {
        ringPath.reset();
        int segments = 20;
        boolean started = false;
        for (int i = 0; i <= segments; i++) {
            double ang = (2.0 * Math.PI * i) / segments;
            float wx = centerX + (float) Math.cos(ang) * radiusMeters;
            float wz = centerZ + (float) Math.sin(ang) * radiusMeters;
            float[] p = sceneController.projectWorldToScreen(wx, 0.0f, wz, w, h);
            if (p[2] <= 0.15f) return;
            if (!started) {
                ringPath.moveTo(p[0], p[1]);
                started = true;
            } else {
                ringPath.lineTo(p[0], p[1]);
            }
        }
        if (started) {
            ringPath.close();
            if (fill) {
                canvas.drawPath(ringPath, shadowPaint);
            }
            canvas.drawPath(ringPath, paint);
        }
    }

    private static final class ProjectedWorldFace {
        final int[] indices;
        final float depth;
        final float lightIntensity;
        final int faceIndex;

        ProjectedWorldFace(int[] indices, float depth, float lightIntensity, int faceIndex) {
            this.indices = indices;
            this.depth = depth;
            this.lightIntensity = lightIntensity;
            this.faceIndex = faceIndex;
        }
    }

    private void drawPlacedInstanceInWorldSpace(Canvas canvas, ArPlacedInstance inst, boolean isSelected, int w, int h) {
        float anchorX = inst.getX();
        float anchorY = inst.getY();
        float anchorZ = inst.getZ();

        float[] anchorProj = sceneController.projectWorldToScreen(anchorX, anchorY, anchorZ, w, h);
        if (anchorProj[2] <= 0.15f) {
            // L'objet est derrière la caméra actuelle : il reste ancré dans la pièce hors du champ de vision
            return;
        }

        // Facteur d'échelle en mètres dans l'espace réel (0.65m de demi-largeur = canapé de ~1.3m à l'échelle 1.0x)
        float worldScale = 0.65f * inst.getScaleFactor();

        // Ombre portée au sol + anneau de sélection 3D posé à plat sur le sol (Y = 0)
        if (isSelected) {
            drawFloorCircle3D(canvas, anchorX, anchorZ, worldScale * 1.08f, selectionRingPaint, true, w, h);
        } else {
            drawFloorCircle3D(canvas, anchorX, anchorZ, worldScale * 0.92f, shadowPaint, true, w, h);
        }

        GlbModelCache.SharedGlbResource res = inst.getSharedResource();
        if (res == null || res.getVertices() == null || res.getFaces() == null) return;

        float[][] verts = res.getVertices();
        int[][] faces = res.getFaces();

        double rotRad = Math.toRadians(inst.getRotationDegrees());
        float cosRot = (float) Math.cos(rotRad);
        float sinRot = (float) Math.sin(rotRad);

        // 1. Transformer chaque sommet du maillage .glb dans le repère 3D du monde (World Space) puis le projeter à l'écran
        float[][] worldVerts = new float[verts.length][3];
        float[][] screenVerts = new float[verts.length][3];
        float minScreenY = anchorProj[1];

        for (int i = 0; i < verts.length; i++) {
            float lx = verts[i][0] * worldScale;
            float ly = verts[i][1] * worldScale;
            float lz = verts[i][2] * worldScale;

            // Rotation propre de l'instance autour de l'axe vertical Y du sol
            float rx = lx * cosRot - lz * sinRot;
            float rz = lx * sinRot + lz * cosRot;

            float wx = anchorX + rx;
            float wy = anchorY + ly;
            float wz = anchorZ + rz;

            worldVerts[i][0] = wx;
            worldVerts[i][1] = wy;
            worldVerts[i][2] = wz;

            screenVerts[i] = sceneController.projectWorldToScreen(wx, wy, wz, w, h);
            if (screenVerts[i][2] > 0.15f && screenVerts[i][1] < minScreenY) {
                minScreenY = screenVerts[i][1];
            }
        }

        // 2. Trier les faces 3D par profondeur réelle par rapport à la caméra (Algorithme du peintre 3D)
        List<ProjectedWorldFace> sortedFaces = new ArrayList<>(faces.length);
        for (int fIdx = 0; fIdx < faces.length; fIdx++) {
            int[] f = faces[fIdx];
            if (f.length < 3) continue;
            boolean behindCamera = false;
            float avgDepth = 0f;
            for (int vIdx : f) {
                if (screenVerts[vIdx][2] <= 0.14f) {
                    behindCamera = true;
                    break;
                }
                avgDepth += screenVerts[vIdx][2];
            }
            if (behindCamera) continue;
            avgDepth /= f.length;

            // Calcul de la normale 3D de la face pour un ombrage réaliste du fichier .glb
            float[] v0 = worldVerts[f[0]];
            float[] v1 = worldVerts[f[1]];
            float[] v2 = worldVerts[f[2]];
            float ux = v1[0] - v0[0], uy = v1[1] - v0[1], uz = v1[2] - v0[2];
            float vx = v2[0] - v0[0], vy = v2[1] - v0[1], vz = v2[2] - v0[2];
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;
            float nLen = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            float light = 0.72f;
            if (nLen > 1e-5f) {
                nx /= nLen;
                ny /= nLen;
                nz /= nLen;
                // Lumière directionnelle zénithale + frontale
                float dot = Math.abs(nx * 0.30f + ny * 0.82f + nz * 0.48f);
                light = 0.52f + 0.48f * Math.min(1.0f, dot);
            }
            sortedFaces.add(new ProjectedWorldFace(f, avgDepth, light, fIdx));
        }

        // Dessiner les faces les plus éloignées en premier (depth décroissant)
        Collections.sort(sortedFaces, (a, b) -> Float.compare(b.depth, a.depth));

        boolean isBlueBubble = res.getSourceUrl() != null
                && res.getSourceUrl().toLowerCase().contains("base_basic_shaded");
        int baseColorHex = isBlueBubble
                ? (isSelected ? Color.parseColor("#2563EB") : Color.parseColor("#1D4ED8"))
                : (isSelected ? Color.parseColor("#FF6D00") : Color.parseColor("#00897B"));

        for (ProjectedWorldFace pf : sortedFaces) {
            facePath.reset();
            for (int k = 0; k < pf.indices.length; k++) {
                float px = screenVerts[pf.indices[k]][0];
                float py = screenVerts[pf.indices[k]][1];
                if (k == 0) facePath.moveTo(px, py);
                else facePath.lineTo(px, py);
            }
            facePath.close();

            facePaint.setColor(applyLighting(baseColorHex, pf.lightIntensity, pf.faceIndex));
            canvas.drawPath(facePath, facePaint);
            canvas.drawPath(facePath, edgePaint);
        }

        // 3. Étiquette d'ancrage 3D au-dessus de l'instance
        String tag = String.format(Locale.FRANCE, "#%d • %.1fm (%.1fx • %.0f°)",
                inst.getInstanceNumber(), anchorProj[2], inst.getScaleFactor(), inst.getRotationDegrees());
        float textW = labelPaint.measureText(tag) + 24f;
        float labelY = Math.max(60f, minScreenY - 18f);
        canvas.drawRoundRect(anchorProj[0] - textW / 2f, labelY - 26f,
                anchorProj[0] + textW / 2f, labelY + 8f, 12f, 12f, labelBgPaint);
        canvas.drawText(tag, anchorProj[0], labelY, labelPaint);
    }

    private static int applyLighting(int baseColor, float intensity, int faceIdx) {
        float puffVariation = ((faceIdx % 3) - 1) * 0.04f;
        float factor = Math.max(0.38f, Math.min(1.18f, intensity + puffVariation));
        int r = Math.min(255, Math.round(Color.red(baseColor) * factor));
        int g = Math.min(255, Math.round(Color.green(baseColor) * factor));
        int b = Math.min(255, Math.round(Color.blue(baseColor) * factor));
        return Color.rgb(r, g, b);
    }
}
