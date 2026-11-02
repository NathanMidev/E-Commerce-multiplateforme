package com.marketplace.app;

import android.app.Application;
import android.os.Build;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.mock.MockBackendService;
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

        // 1. Configuration du mode mock :
        // Si l'URL pointe sur 10.0.2.2 (boucle locale émulateur uniquement) alors que l'app tourne
        // sur un vrai téléphone physique, basculer directement en mode autonome sans attente réseau.
        boolean shouldUseMock = BuildConfig.USE_MOCK_BACKEND
                || (BuildConfig.API_BASE_URL.contains("10.0.2.2") && !isRunningOnEmulator());
        ApiClient.setUseMock(shouldUseMock);
        ApiClient.init(BuildConfig.API_BASE_URL);

        // 2. Synchronisation de la session Firebase Auth
        FirebaseAuthSession.initialize();

        // 3. Initialisation des données de démonstration (vendeur exemple Canapé Bubble + base_basic_shaded.glb)
        MockBackendService.getInstance().resetData();
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
