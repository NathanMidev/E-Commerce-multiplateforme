package com.marketplace.app;

import android.app.Application;
import android.os.Build;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.feature.auth.FirebaseAuthSession;

/**
 * Rôle : Classe Application principale du projet monolithe unifié.
 * Entrée : Démarrage du processus Android.
 * Sortie : Initialisation de l'API REST, de la session Firebase et configuration du mock.
 */
public class MarketplaceApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialisation de l'API REST vers PostgreSQL
        String apiUrl = ApiClient.resolveConfiguredBaseUrl(this, BuildConfig.API_BASE_URL);
        android.util.Log.i("MarketplaceApp", "Configured API URL: " + apiUrl);
        ApiClient.init(apiUrl);

        // Synchronisation de la session Firebase Auth
        FirebaseAuthSession.initialize();
    }

    private static boolean isRunningOnEmulator() {
        return (Build.FINGERPRINT != null && (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.FINGERPRINT.contains("emulator")
                || Build.FINGERPRINT.contains("sdk_gphone")))
                || (Build.MODEL != null && (Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for")))
                || (Build.PRODUCT != null && (Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")));
    }
}
