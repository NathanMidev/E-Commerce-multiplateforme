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
