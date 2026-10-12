package com.marketplace.feature.ar3d;

import com.marketplace.feature.ar3d.router.ArModuleRouter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test unitaire isolé du module Réalité Augmentée 3D (:feature:ar3d).
 * Aucun autre module (produits, chat, etc.) n'est appelé.
 */
public class ArUnitTest {

    @Test
    public void testArModuleRouterIsolated() {
        ArModuleRouter router = new ArModuleRouter();
        assertNotNull("Le routeur AR doit s'instancier isolément", router);
        router.openArViewer(null, "p1", "Fauteuil", "https://url.glb");
        router.open3dPreview(null, "p1", "https://url.glb");
    }
}
