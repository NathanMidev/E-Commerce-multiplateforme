package com.marketplace.app;

import android.app.Application;

import com.marketplace.app.router.AppGlobalRouter;
import com.marketplace.core.network.AuthSession;
import com.marketplace.core.network.BackendProvider;
import com.marketplace.feature.auth.session.FirebaseAuthSession;

/**
 * Classe Application principale.
 * Point d'entrée global orchestrant le monolithe modulaire.
 */
public class MarketplaceApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Backend : le serveur PostgreSQL partagé avec la version web, ou le mock hors ligne.
        AuthSession session = new FirebaseAuthSession();

        // On utilise le Mock par défaut pour supprimer la dépendance à BuildConfig
        BackendProvider.useMock(session);

        // Initialiser l'ensemble des routeurs de chaque module
        AppGlobalRouter.initializeRouters();
    }
}
