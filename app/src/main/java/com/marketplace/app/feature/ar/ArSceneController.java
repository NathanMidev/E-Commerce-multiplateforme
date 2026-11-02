package com.marketplace.app.feature.ar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rôle : Contrôleur de scène ARCore gérant :
 * - La détection des surfaces planes (horizontales et verticales)
 * - Le partage d'une ressource GLB unique en mémoire (sans re-téléchargement lors des duplications)
 * - Le placement, la sélection, le déplacement indépendant, la rotation, la mise à l'échelle,
 *   la duplication, la suppression d'instance et la réinitialisation complète de la scène (Section 7).
 */
public class ArSceneController {

    private GlbModelCache.SharedGlbResource sharedResource;
    private int resourceBindCount = 0;

    private boolean surfaceDetected = false;
    private String detectedPlaneType = "NONE";

    private final List<ArPlacedInstance> instances = new ArrayList<>();
    private ArPlacedInstance selectedInstance = null;
    private int sequence = 0;

    // Pose 3D de la caméra dans le repère du monde réel (World Space, sol physique à Y = 0.0f)
    public static final float DEFAULT_CAMERA_HEIGHT_METERS = 1.25f;
    public static final float DEFAULT_CAMERA_PITCH_DEGREES = -24.0f;

    private float cameraX = 0.0f;
    private float cameraY = DEFAULT_CAMERA_HEIGHT_METERS;
    private float cameraZ = 0.0f;
    private float cameraYawDegrees = 0.0f;
    private float cameraPitchDegrees = DEFAULT_CAMERA_PITCH_DEGREES;
    private float cameraRollDegrees = 0.0f;

    // Matrices 4x4 natives ARCore (Camera.getViewMatrix & Camera.getProjectionMatrix)
    private final float[] arCoreViewProjectionMatrix = new float[16];
    private boolean hasNativeArCoreMatrices = false;

    // Polygones 3D réels des surfaces détectées par ARCore (Session.getAllTrackables(Plane.class))
    private volatile List<float[][]> trackedPlanePolygons = Collections.emptyList();

    // Impact temps réel du réticule central issu de Frame.hitTest(cx, cy) d'ARCore
    private volatile boolean hasNativeReticleHit = false;
    private final float[] nativeReticleHitPos = new float[]{0.0f, 0.0f, -2.0f};

    /**
     * Injecte la matrice View-Projection 4x4 native calculée par ARCore (Camera.getProjectionMatrix * Camera.getViewMatrix)
     * ainsi que la position 3D exacte de la caméra ARCore (Camera.getPose()).
     */
    public synchronized void setNativeArCoreViewProjection(float[] viewProjMatrix, float camX, float camY, float camZ) {
        if (viewProjMatrix != null && viewProjMatrix.length >= 16) {
            System.arraycopy(viewProjMatrix, 0, this.arCoreViewProjectionMatrix, 0, 16);
            this.hasNativeArCoreMatrices = true;
            this.cameraX = camX;
            this.cameraY = camY;
            this.cameraZ = camZ;
        }
    }

    public boolean hasNativeArCoreMatrices() {
        return hasNativeArCoreMatrices;
    }

    public void setTrackedPlanePolygons(List<float[][]> polygons) {
        this.trackedPlanePolygons = polygons != null ? polygons : Collections.emptyList();
    }

    public List<float[][]> getTrackedPlanePolygons() {
        return trackedPlanePolygons;
    }

    public void setNativeReticleHit(boolean hasHit, float wx, float wy, float wz) {
        this.hasNativeReticleHit = hasHit;
        if (hasHit) {
            this.nativeReticleHitPos[0] = wx;
            this.nativeReticleHitPos[1] = wy;
            this.nativeReticleHitPos[2] = wz;
        }
    }

    public boolean hasNativeReticleHit() {
        return hasNativeReticleHit;
    }

    /**
     * Met à jour l'orientation 3D de la caméra (Yaw / Pitch / Roll en degrés) à partir des capteurs ou d'ARCore.
     * Les objets déjà placés conservent leurs coordonnées (X, Y, Z) dans le monde réel et ne suivent pas l'écran.
     */
    public void updateCameraOrientation(float yawDegrees, float pitchDegrees, float rollDegrees) {
        this.cameraYawDegrees = yawDegrees;
        // Borner le pitch entre -82° (regard vers le sol) et +45° (regard vers le haut)
        this.cameraPitchDegrees = Math.max(-82.0f, Math.min(45.0f, pitchDegrees));
        this.cameraRollDegrees = Math.max(-60.0f, Math.min(60.0f, rollDegrees));
    }

    /**
     * Met à jour la pose 6DoF complète (position 3D + orientation 3D) de la caméra dans l'espace réel.
     */
    public void updateCameraPose(float camX, float camY, float camZ,
                                 float yawDegrees, float pitchDegrees, float rollDegrees) {
        this.cameraX = camX;
        this.cameraY = Math.max(0.25f, camY);
        this.cameraZ = camZ;
        updateCameraOrientation(yawDegrees, pitchDegrees, rollDegrees);
    }

    public void resetCameraPose() {
        this.cameraX = 0.0f;
        this.cameraY = DEFAULT_CAMERA_HEIGHT_METERS;
        this.cameraZ = 0.0f;
        this.cameraYawDegrees = 0.0f;
        this.cameraPitchDegrees = DEFAULT_CAMERA_PITCH_DEGREES;
        this.cameraRollDegrees = 0.0f;
    }

    public float getCameraX() { return cameraX; }
    public float getCameraY() { return cameraY; }
    public float getCameraZ() { return cameraZ; }
    public float getCameraYawDegrees() { return cameraYawDegrees; }
    public float getCameraPitchDegrees() { return cameraPitchDegrees; }
    public float getCameraRollDegrees() { return cameraRollDegrees; }

    /**
     * Projette un point 3D du monde réel (worldX, worldY, worldZ) vers les coordonnées pixels de l'écran
     * en utilisant la matrice de vue-projection 3D de la caméra.
     *
     * @return tableau float[]{screenX, screenY, depthMeters} où depthMeters > 0.12f indique que le point est devant la caméra.
     */
    public synchronized float[] projectWorldToScreen(float worldX, float worldY, float worldZ, int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return new float[]{0f, 0f, -1f};
        }

        // Si la Session native ARCore est active, utiliser directement sa matrice 4x4 (Projection * View)
        if (hasNativeArCoreMatrices) {
            float[] vp = arCoreViewProjectionMatrix;
            float clipX = vp[0] * worldX + vp[4] * worldY + vp[8] * worldZ + vp[12];
            float clipY = vp[1] * worldX + vp[5] * worldY + vp[9] * worldZ + vp[13];
            float clipW = vp[3] * worldX + vp[7] * worldY + vp[11] * worldZ + vp[15];

            if (clipW <= 0.10f) {
                return new float[]{-10000f, -10000f, clipW};
            }
            float ndcX = clipX / clipW;
            float ndcY = clipY / clipW;
            float screenX = (ndcX + 1.0f) * 0.5f * screenWidth;
            float screenY = (1.0f - ndcY) * 0.5f * screenHeight;
            return new float[]{screenX, screenY, clipW};
        }

        // 1. Translation Monde -> Caméra
        float dx = worldX - cameraX;
        float dy = worldY - cameraY;
        float dz = worldZ - cameraZ;

        // 2. Rotation horizontale inverse autour de l'axe vertical Y du monde (Yaw)
        double yawRad = Math.toRadians(cameraYawDegrees);
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);
        float x1 = dx * cosY + dz * sinY;
        float z1 = dz * cosY - dx * sinY;

        // 3. Rotation verticale autour de l'axe X caméra (Pitch : négatif quand on regarde vers le sol)
        double pitchRad = Math.toRadians(cameraPitchDegrees);
        float cosP = (float) Math.cos(pitchRad);
        float sinP = (float) Math.sin(pitchRad);
        float yCam = dy * cosP + z1 * sinP;
        float zCam = -dy * sinP + z1 * cosP;

        // 4. Rotation de roulis autour de l'axe optique Z caméra (Roll)
        double rollRad = Math.toRadians(cameraRollDegrees);
        float cosR = (float) Math.cos(rollRad);
        float sinR = (float) Math.sin(rollRad);
        float xRolled = x1 * cosR - yCam * sinR;
        float yRolled = x1 * sinR + yCam * cosR;

        // En repère caméra OpenGL/ARCore, l'axe de visée est -Z, donc la profondeur devant la caméra est -zCam
        float depth = -zCam;
        if (depth <= 0.12f) {
            return new float[]{-10000f, -10000f, depth};
        }

        float focalLengthPx = Math.min(screenWidth, screenHeight) * 0.92f;
        float screenX = (screenWidth * 0.5f) + (xRolled / depth) * focalLengthPx;
        float screenY = (screenHeight * 0.5f) - (yRolled / depth) * focalLengthPx;
        return new float[]{screenX, screenY, depth};
    }

    /**
     * Lance un rayon 3D depuis le pixel (screenX, screenY) à travers la caméra actuelle et calcule
     * son intersection exacte (worldX, 0.0f, worldZ) avec le plan physique horizontal du sol (Y = 0).
     */
    public float[] screenRayToFloorPlane(float screenX, float screenY, int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return new float[]{cameraX, 0.0f, cameraZ - 2.2f};
        }

        float focalLengthPx = Math.min(screenWidth, screenHeight) * 0.92f;
        float rxRolled = (screenX - screenWidth * 0.5f) / focalLengthPx;
        float ryRolled = (screenHeight * 0.5f - screenY) / focalLengthPx;
        float rzCam = -1.0f;

        // 1. Inverse du Roll
        double rollRad = Math.toRadians(cameraRollDegrees);
        float cosR = (float) Math.cos(rollRad);
        float sinR = (float) Math.sin(rollRad);
        float rxCam = rxRolled * cosR + ryRolled * sinR;
        float ryCam = -rxRolled * sinR + ryRolled * cosR;

        // 2. Inverse du Pitch
        double pitchRad = Math.toRadians(cameraPitchDegrees);
        float cosP = (float) Math.cos(pitchRad);
        float sinP = (float) Math.sin(pitchRad);
        float dirY = ryCam * cosP - rzCam * sinP;
        float z1 = ryCam * sinP + rzCam * cosP;

        // 3. Inverse du Yaw -> Direction du rayon dans le Monde (World Space)
        double yawRad = Math.toRadians(cameraYawDegrees);
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);
        float dirX = rxCam * cosY - z1 * sinY;
        float dirZ = rxCam * sinY + z1 * cosY;

        // 4. Intersection avec le plan horizontal du sol (worldY = 0) : cameraY + t * dirY = 0
        float t;
        if (dirY < -0.06f) {
            t = -cameraY / dirY;
            t = Math.max(0.65f, Math.min(7.5f, t));
        } else {
            // Si le téléphone vise au-dessus de l'horizon, projeter sur le sol à 2.4m devant la direction visée
            t = 2.4f;
        }

        float hitWorldX = cameraX + t * dirX;
        float hitWorldZ = cameraZ + t * dirZ;
        return new float[]{hitWorldX, 0.0f, hitWorldZ};
    }

    /**
     * Retourne les coordonnées 3D (worldX, 0, worldZ) sur le sol physique visées par le réticule central de la caméra.
     */
    public float[] getReticleWorldHit(int screenWidth, int screenHeight) {
        if (hasNativeReticleHit) {
            return new float[]{nativeReticleHitPos[0], nativeReticleHitPos[1], nativeReticleHitPos[2]};
        }
        int w = screenWidth > 0 ? screenWidth : 1080;
        int h = screenHeight > 0 ? screenHeight : 1920;
        return screenRayToFloorPlane(w * 0.5f, h * 0.54f, w, h);
    }

    public void bindSharedGlbResource(GlbModelCache.SharedGlbResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("La ressource modèle .glb ne peut pas être nulle");
        }
        if (this.sharedResource == null || !this.sharedResource.getSourceUrl().equals(resource.getSourceUrl())) {
            this.sharedResource = resource;
            this.resourceBindCount++;
        }
    }

    public GlbModelCache.SharedGlbResource getSharedResource() {
        return sharedResource;
    }

    /**
     * Nombre de fois où le fichier .glb a été chargé dans ce contrôleur (doit rester à 1 même après N duplications).
     */
    public int getResourceBindCount() {
        return resourceBindCount;
    }

    public void onSurfaceDetected(String planeType) {
        this.surfaceDetected = true;
        this.detectedPlaneType = (planeType != null && !planeType.trim().isEmpty())
                ? planeType.trim().toUpperCase() : "HORIZONTAL";
    }

    public void clearSurfaceDetection() {
        this.surfaceDetected = false;
        this.detectedPlaneType = "NONE";
    }

    public boolean isSurfaceDetected() {
        return surfaceDetected;
    }

    public String getDetectedPlaneType() {
        return detectedPlaneType;
    }

    /**
     * Place un nouvel exemplaire du produit à l'emplacement touché sur la surface détectée.
     */
    public ArPlacedInstance placeInstanceOnSurface(float x, float y, float z) {
        if (!surfaceDetected) {
            throw new IllegalStateException("Aucune surface plane détectée. Scannez d'abord le sol, une table ou un mur.");
        }
        if (sharedResource == null) {
            throw new IllegalStateException("Le modèle 3D .glb n'est pas encore chargé.");
        }
        sequence++;
        ArPlacedInstance instance = new ArPlacedInstance(
                "inst_" + sequence,
                sequence,
                sharedResource,
                detectedPlaneType,
                x, y, z,
                0.0f,
                1.0f
        );
        instances.add(instance);
        selectedInstance = instance;
        return instance;
    }

    /**
     * Duplique l'instance actuellement sélectionnée (ou la dernière placée) vers un nouvel emplacement.
     * La nouvelle instance est strictement indépendante mais réutilise la même ressource SharedGlbResource.
     */
    public ArPlacedInstance duplicateSelectedInstance(float offsetX, float offsetZ) {
        if (!surfaceDetected) {
            throw new IllegalStateException("Aucune surface détectée pour placer la copie.");
        }
        if (sharedResource == null) {
            throw new IllegalStateException("Modèle .glb non chargé.");
        }
        ArPlacedInstance source = selectedInstance != null
                ? selectedInstance
                : (!instances.isEmpty() ? instances.get(instances.size() - 1) : null);
        if (source == null) {
            throw new IllegalStateException("Veuillez d'abord placer un premier exemplaire avant de le dupliquer.");
        }

        sequence++;
        ArPlacedInstance copy = new ArPlacedInstance(
                "inst_" + sequence,
                sequence,
                source.getSharedResource(), // Réutilisation directe du modèle GLB en mémoire
                source.getPlaneType(),
                source.getX() + offsetX,
                source.getY(),
                source.getZ() + offsetZ,
                source.getRotationDegrees(),
                source.getScaleFactor()
        );
        instances.add(copy);
        selectedInstance = copy;
        return copy;
    }

    /**
     * Duplique l'instance sélectionnée vers une Pose 3D exacte et lui attache une Anchor matérielle ARCore.
     */
    public synchronized ArPlacedInstance duplicateSelectedInstanceTo(
            float targetX, float targetY, float targetZ, com.google.ar.core.Anchor anchor) {
        if (!surfaceDetected) {
            throw new IllegalStateException("Aucune surface détectée pour placer la copie.");
        }
        if (sharedResource == null) {
            throw new IllegalStateException("Modèle .glb non chargé.");
        }
        ArPlacedInstance source = selectedInstance != null
                ? selectedInstance
                : (!instances.isEmpty() ? instances.get(instances.size() - 1) : null);
        if (source == null) {
            throw new IllegalStateException("Veuillez d'abord placer un premier exemplaire avant de le dupliquer.");
        }

        sequence++;
        ArPlacedInstance copy = new ArPlacedInstance(
                "inst_" + sequence,
                sequence,
                source.getSharedResource(),
                source.getPlaneType(),
                targetX,
                targetY,
                targetZ,
                source.getRotationDegrees(),
                source.getScaleFactor()
        );
        if (anchor != null) {
            copy.setArCoreAnchor(anchor);
        }
        instances.add(copy);
        selectedInstance = copy;
        return copy;
    }

    public boolean selectInstance(String instanceId) {
        if (instanceId == null) return false;
        for (ArPlacedInstance inst : instances) {
            if (instanceId.equals(inst.getInstanceId())) {
                selectedInstance = inst;
                return true;
            }
        }
        return false;
    }

    public ArPlacedInstance selectInstanceAtIndex(int index) {
        if (index < 0 || index >= instances.size()) return null;
        selectedInstance = instances.get(index);
        return selectedInstance;
    }

    public ArPlacedInstance getSelectedInstance() {
        return selectedInstance;
    }

    public List<ArPlacedInstance> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    public int getInstanceCount() {
        return instances.size();
    }

    /**
     * Déplace uniquement l'instance sélectionnée sans modifier les autres instances.
     */
    public void moveSelectedInstanceTo(float newX, float newY, float newZ) {
        if (selectedInstance == null) {
            throw new IllegalStateException("Aucune instance sélectionnée à déplacer");
        }
        selectedInstance.moveTo(newX, newY, newZ);
    }

    /**
     * Fait pivoter uniquement l'instance sélectionnée.
     */
    public void rotateSelectedInstanceBy(float deltaDegrees) {
        if (selectedInstance == null) {
            throw new IllegalStateException("Aucune instance sélectionnée à faire pivoter");
        }
        selectedInstance.rotateBy(deltaDegrees);
    }

    /**
     * Ajuste l'échelle uniquement de l'instance sélectionnée.
     */
    public void scaleSelectedInstance(float newScaleFactor) {
        if (selectedInstance == null) {
            throw new IllegalStateException("Aucune instance sélectionnée à redimensionner");
        }
        selectedInstance.setScaleFactor(newScaleFactor);
    }

    /**
     * Supprime uniquement l'instance actuellement sélectionnée.
     */
    public boolean deleteSelectedInstance() {
        if (selectedInstance == null) return false;
        selectedInstance.detachAnchor();
        boolean removed = instances.remove(selectedInstance);
        if (!instances.isEmpty()) {
            selectedInstance = instances.get(instances.size() - 1);
        } else {
            selectedInstance = null;
        }
        return removed;
    }

    /**
     * Réinitialise toute la scène en supprimant toutes les instances placées.
     */
    public void resetScene() {
        for (ArPlacedInstance inst : instances) {
            inst.detachAnchor();
        }
        instances.clear();
        selectedInstance = null;
    }

    /**
     * Libère toutes les ressources AR et 3D à la fermeture de l'écran.
     */
    public void release() {
        resetScene();
        sharedResource = null;
        surfaceDetected = false;
        detectedPlaneType = "NONE";
    }
}
