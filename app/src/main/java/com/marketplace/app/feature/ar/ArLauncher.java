package com.marketplace.app.feature.ar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.ar.core.ArCoreApk;
import com.marketplace.app.navigation.Router;

/**
 * Rôle : Vérificateur de compatibilité ARCore et lanceur sécurisé de l'expérience ARCore (Section 6).
 * Avant d'ouvrir l'expérience ARCore, vérifie :
 * 1. La compatibilité du téléphone et la disponibilité d'ARCore
 * 2. La validité du modèle .glb
 * 3. L'autorisation caméra nécessaire.
 * Si ARCore n'est pas disponible, affiche un message explicatif et conserve le visualiseur 3D classique.
 */
public final class ArLauncher {

    /** Permet de simuler la disponibilité ou non d'ARCore pendant les tests unitaires JVM. */
    private static Boolean testArSupportOverride = null;

    private ArLauncher() {}

    public static void setTestArSupportOverride(Boolean override) {
        testArSupportOverride = override;
    }

    public static final class ArReadinessResult {
        private final boolean ready;
        private final boolean arCoreSupported;
        private final boolean validGlbModel;
        private final boolean cameraPermissionGranted;
        private final String message;

        public ArReadinessResult(boolean ready, boolean arCoreSupported,
                                 boolean validGlbModel, boolean cameraPermissionGranted,
                                 String message) {
            this.ready = ready;
            this.arCoreSupported = arCoreSupported;
            this.validGlbModel = validGlbModel;
            this.cameraPermissionGranted = cameraPermissionGranted;
            this.message = message;
        }

        public boolean isReady() { return ready; }
        public boolean isArCoreSupported() { return arCoreSupported; }
        public boolean isValidGlbModel() { return validGlbModel; }
        public boolean isCameraPermissionGranted() { return cameraPermissionGranted; }
        public String getMessage() { return message; }
    }

    public static boolean isArSupported(Context context) {
        if (testArSupportOverride != null) {
            return testArSupportOverride;
        }
        if (context == null) return false;

        // 1. Vérifier si Google Play Services for AR (com.google.ar.core) est installé sur l'appareil
        try {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                pm.getPackageInfo("com.google.ar.core", 0);
                return true;
            }
        } catch (Throwable ignored) {
        }

        // 2. Vérifier via l'API officielle ArCoreApk (y compris l'état transitoire UNKNOWN_CHECKING au 1er appel)
        try {
            ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(context);
            if (availability != null && (availability.isSupported() || availability.isTransient())) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        // 3. Fallback caméra physique disponible sur le téléphone
        try {
            PackageManager pm = context.getPackageManager();
            if (pm != null && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean hasCameraPermission(Context context) {
        if (context == null) return false;
        try {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Évalue toutes les conditions requises avant le lancement d'ARCore (Section 6).
     */
    public static ArReadinessResult evaluateReadiness(Context context, String model3dUrl, boolean cameraPermissionGranted) {
        boolean validGlb = GlbValidator.isValidGlbExtension(model3dUrl);
        if (!validGlb) {
            return new ArReadinessResult(
                    false, false, false, cameraPermissionGranted,
                    "Impossible d'ouvrir la Réalité Augmentée : aucun modèle .glb valide n'est associé à ce produit."
            );
        }

        boolean arSupported = isArSupported(context);
        if (!arSupported) {
            return new ArReadinessResult(
                    false, false, true, cameraPermissionGranted,
                    "Votre téléphone n'est pas compatible avec ARCore. Vous pouvez continuer à observer le produit sous tous les angles dans le visualiseur 3D classique."
            );
        }

        if (!cameraPermissionGranted) {
            return new ArReadinessResult(
                    false, true, true, false,
                    "L'autorisation Caméra est requise pour détecter les surfaces en Réalité Augmentée."
            );
        }

        return new ArReadinessResult(
                true, true, true, true,
                "ARCore prêt : détection des surfaces activée."
        );
    }

    /**
     * Ouvre l'expérience ARCore (ArViewerActivity) si l'appareil et le modèle .glb sont valides,
     * sinon affiche le message explicatif et conserve la visualisation 3D classique.
     */
    public static boolean launchArExperienceIfCompatible(Activity activity, String productId, String productName, String modelUrl) {
        if (activity == null) return false;
        if (!GlbValidator.isValidGlbExtension(modelUrl)) {
            Toast.makeText(activity,
                    "Modèle 3D invalide : seul un fichier .glb valide peut être ouvert en AR.",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (!isArSupported(activity)) {
            Toast.makeText(activity,
                    "ARCore n'est pas disponible sur ce téléphone. Utilisez le visualiseur 3D interactif (rotation/zoom) ci-dessous.",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        Router.openArViewer(activity, productId, productName, modelUrl);
        return true;
    }
}
