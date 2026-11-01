package com.marketplace.core.model;

/**
 * Statuts détaillés du pipeline de scan 3D combiné :
 * Mobile (ARCore + YOLO) -> Firebase -> PC (Meta SAM + COLMAP) -> Mobile (ARCore Viewer).
 */
public enum ScanJobStatus {
    SURFACE_DETECTION,       // ARCore recherche un plan / une surface stable pour poser l'ancre
    YOLO_VALIDATING,         // YOLO analyse en temps réel pour étiqueter l'objet et valider la catégorie
    CATEGORY_MISMATCH,       // L'étiquette YOLO ne correspond pas à la catégorie du produit
    CAPTURING_FRAMES,        // ARCore guide la capture multi-angles (360°)
    UPLOADING_TO_FIREBASE,   // Envoi des clichés capturés vers Firebase Storage & métadonnées Firestore
    SAM_SEGMENTING,          // PC (GPU) : Meta SAM segmente l'objet et retire l'arrière-plan
    COLMAP_RECONSTRUCTING,   // PC (GPU) : COLMAP exécute SfM + MVS pour reconstruire le maillage 3D
    COMPLETED,               // Modèle .glb généré, stocké sur Firebase et prêt pour la visualisation ARCore
    FAILED                   // Erreur technique survenue durant le traitement
}
