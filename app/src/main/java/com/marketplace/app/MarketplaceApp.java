package com.marketplace.app;

import android.app.Application;
import com.marketplace.app.router.AppGlobalRouter;

/**
 * Classe Application principale.
 * Point d'entrée global orchestrant le monolithe modulaire.
 */
public class MarketplaceApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialiser l'ensemble des routeurs de chaque module
        AppGlobalRouter.initializeRouters();
    }
}
