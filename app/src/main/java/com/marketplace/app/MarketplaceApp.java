package com.marketplace.app;

import android.app.Application;

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

        // 1. Configuration du mode mock (explicite via BuildConfig.USE_MOCK_BACKEND) et de l'URL API
        ApiClient.setUseMock(BuildConfig.USE_MOCK_BACKEND);
        ApiClient.init(BuildConfig.API_BASE_URL);

        // 2. Synchronisation de la session Firebase Auth
        FirebaseAuthSession.initialize();

        // 3. Préparation des données en mémoire uniquement si mode démo activé
        if (BuildConfig.USE_MOCK_BACKEND) {
            MockBackendService.getInstance().resetData();
        }
    }
}
