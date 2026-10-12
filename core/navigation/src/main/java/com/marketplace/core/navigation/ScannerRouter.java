package com.marketplace.core.navigation;

import android.content.Context;

/**
 * Contrat de routage pour le module Scanner 3D & Traitement SAM Meta (feature:scanner3d).
 * Permet au vendeur de numériser un objet sous plusieurs angles puis suivre
 * l'envoi vers Firebase et le traitement SAM sur l'ordinateur.
 */
public interface ScannerRouter {
    void openScanner(Context context, String productId);
    void openSamJobStatus(Context context, String jobId);
}
