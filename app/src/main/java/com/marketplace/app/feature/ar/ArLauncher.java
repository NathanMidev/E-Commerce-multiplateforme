package com.marketplace.app.feature.ar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;

/**
 * Rôle : Lanceur ARCore & Scene Viewer pour la visualisation 3D en Réalité Augmentée.
 * Entrée : URL HTTPS vers fichier .glb / .gltf.
 * Sortie : Vérification de compatibilité de l'appareil et ouverture Scene Viewer en mode ar_preferred.
 */
public final class ArLauncher {

    private ArLauncher() {}

    public static boolean isArSupported(Context context) {
        if (context == null) return false;
        try {
            ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(context);
            return availability.isSupported();
        } catch (Exception e) {
            return false;
        }
    }

    public static void launchSceneViewer(Activity activity, String modelUrl, String title) {
        if (activity == null || modelUrl == null || modelUrl.trim().isEmpty()) {
            return;
        }

        if (!isArSupported(activity)) {
            Toast.makeText(activity, "Votre appareil ne supporte pas ARCore pour la Réalité Augmentée.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent sceneViewerIntent = new Intent(Intent.ACTION_VIEW);
        Uri intentUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
                .appendQueryParameter("file", modelUrl)
                .appendQueryParameter("mode", "ar_preferred")
                .appendQueryParameter("title", title != null ? title : "Modèle 3D")
                .appendQueryParameter("resizable", "true")
                .build();

        sceneViewerIntent.setData(intentUri);
        sceneViewerIntent.setPackage("com.google.android.googlequicksearchbox");

        try {
            activity.startActivity(sceneViewerIntent);
        } catch (ActivityNotFoundException e) {
            // Repli vers navigateur si l'application Google n'est pas installée
            sceneViewerIntent.setPackage(null);
            try {
                activity.startActivity(sceneViewerIntent);
            } catch (Exception ex) {
                Toast.makeText(activity, "Impossible d'ouvrir le visualiseur Scene Viewer", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
