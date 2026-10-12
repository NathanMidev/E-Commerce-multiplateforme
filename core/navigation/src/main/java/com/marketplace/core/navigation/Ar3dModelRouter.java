package com.marketplace.core.navigation;

import android.content.Context;
import androidx.annotation.Nullable;

/**
 * Contrat principal pour le package/module AR3dmodel.
 * 
 * Règle de routage exigée :
 * - Lance par défaut le SCAN 3D du produit pour permettre au vendeur de le capturer,
 * - Sauf si le produit possède déjà un modèle 3D valide (model3dUrl non vide),
 *   auquel cas il lance directement la visualisation en Réalité Augmentée (ARCore).
 */
public interface Ar3dModelRouter {

    /**
     * Routeur intelligent principal :
     * Si le produit possède déjà un modèle 3D -> ouvre ARCore Viewer.
     * Sinon (par défaut) -> ouvre le Scanner 3D guidé pour traitement SAM (Meta).
     */
    void routeToArOrScan(Context context, String productId, @Nullable String productName, @Nullable String model3dUrl);

    /**
     * Ouvre directement le scanner 3D du produit.
     */
    void openScan(Context context, String productId);

    /**
     * Ouvre directement le visualiseur AR (ARCore) pour tester le produit dans son environnement.
     */
    void openArViewer(Context context, String productId, String productName, String model3dUrl);

    /**
     * Ouvre l'écran de suivi de segmentation par le modèle SAM (Meta) et de reconstruction 3D.
     */
    void openSamJobStatus(Context context, String jobId);

    /**
     * Ouvre le visualiseur 3D interactif (inspection 360° tactile hors AR).
     */
    void open3dViewer(Context context, String productId, String model3dUrl);
}
