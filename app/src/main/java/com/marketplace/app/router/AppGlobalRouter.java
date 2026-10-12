package com.marketplace.app.router;

import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.feature.auth.router.AuthModuleRouter;
import com.marketplace.feature.ar3d.router.ArModuleRouter;
import com.marketplace.feature.chat.router.ChatModuleRouter;
import com.marketplace.feature.products.router.ProductModuleRouter;
import com.marketplace.feature.scanner3d.router.ScannerModuleRouter;

/**
 * Routeur global de l'application (Assemblage du Monolithe Modulaire).
 * 
 * Ce fichier est responsable d'enregistrer les routeurs concrets de chaque module
 * au démarrage de l'application ou avant le 'Grand Test'.
 */
public final class AppGlobalRouter {

    private AppGlobalRouter() {}

    /**
     * Initialise l'ensemble des modules dans le registre central.
     */
    public static void initializeRouters() {
        RouterRegistry registry = RouterRegistry.getInstance();

        // Enregistrement des sous-routeurs de chaque module
        registry.registerProductRouter(new ProductModuleRouter());
        registry.registerArRouter(new ArModuleRouter());
        registry.registerScannerRouter(new ScannerModuleRouter());
        registry.registerChatRouter(new ChatModuleRouter());
        registry.registerAuthRouter(new AuthModuleRouter());
    }
}
