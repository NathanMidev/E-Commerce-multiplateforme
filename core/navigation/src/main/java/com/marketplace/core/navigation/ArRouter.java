package com.marketplace.core.navigation;

import android.content.Context;

/**
 * Contrat de routage pour le module Réalité Augmentée 3D (feature:ar3d).
 * Permet de visualiser un produit 3D dans l'espace de l'acheteur.
 */
public interface ArRouter {
    void openArViewer(Context context, String productId, String productName, String model3dUrl);
    void open3dPreview(Context context, String productId, String model3dUrl);
}
