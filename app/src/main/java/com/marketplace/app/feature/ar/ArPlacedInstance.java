package com.marketplace.app.feature.ar;

import com.google.ar.core.Anchor;

/**
 * Rôle : Instance virtuelle indépendante d'un produit 3D (.glb) placée sur une surface détectée par ARCore.
 * Chaque instance possède ses propres coordonnées (x, y, z), sa rotation et son échelle,
 * tout en partageant la même ressource SharedGlbResource en mémoire (Section 7.C & 7.D).
 */
public class ArPlacedInstance {

    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 3.00f;

    private final String instanceId;
    private final int instanceNumber;
    private final GlbModelCache.SharedGlbResource sharedResource;
    private String planeType;

    private float x;
    private float y;
    private float z;
    private float rotationDegrees;
    private float scaleFactor;
    private Anchor arCoreAnchor;

    public ArPlacedInstance(String instanceId, int instanceNumber,
                            GlbModelCache.SharedGlbResource sharedResource,
                            String planeType,
                            float x, float y, float z,
                            float rotationDegrees, float scaleFactor) {
        this.instanceId = instanceId;
        this.instanceNumber = instanceNumber;
        this.sharedResource = sharedResource;
        this.planeType = planeType != null ? planeType : "HORIZONTAL";
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotationDegrees = normalizeAngle(rotationDegrees);
        this.scaleFactor = clampScale(scaleFactor);
    }

    public void moveTo(float newX, float newY, float newZ) {
        this.x = newX;
        this.y = newY;
        this.z = newZ;
    }

    public void translateBy(float dx, float dy, float dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
    }

    public void rotateBy(float deltaDegrees) {
        this.rotationDegrees = normalizeAngle(this.rotationDegrees + deltaDegrees);
    }

    public void setRotationDegrees(float rotationDegrees) {
        this.rotationDegrees = normalizeAngle(rotationDegrees);
    }

    public void setScaleFactor(float newScale) {
        this.scaleFactor = clampScale(newScale);
    }

    public void setArCoreAnchor(Anchor anchor) {
        detachAnchor();
        this.arCoreAnchor = anchor;
    }

    public Anchor getArCoreAnchor() {
        return arCoreAnchor;
    }

    public void detachAnchor() {
        if (arCoreAnchor != null) {
            try {
                arCoreAnchor.detach();
            } catch (Throwable ignored) {
            }
            arCoreAnchor = null;
        }
    }

    public String getInstanceId() { return instanceId; }
    public int getInstanceNumber() { return instanceNumber; }
    public GlbModelCache.SharedGlbResource getSharedResource() { return sharedResource; }
    public String getPlaneType() { return planeType; }
    public void setPlaneType(String planeType) { this.planeType = planeType; }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getRotationDegrees() { return rotationDegrees; }
    public float getScaleFactor() { return scaleFactor; }

    private static float normalizeAngle(float deg) {
        return ((deg % 360.0f) + 360.0f) % 360.0f;
    }

    private static float clampScale(float scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }
}
