package com.marketplace.app;

import com.marketplace.app.router.AppGlobalRouter;
import com.marketplace.core.firebase.FirebaseScan3dBridgeService;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.Scan3dJob;
import com.marketplace.core.model.ScanJobStatus;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.core.network.MockPostgresBackendService;
import com.marketplace.core.network.PostgresBackendApi;
import com.marketplace.feature.ar3d.router.ArModuleRouter;
import com.marketplace.feature.auth.router.AuthModuleRouter;
import com.marketplace.feature.products.router.ProductModuleRouter;
import com.marketplace.feature.scanner3d.router.ScannerModuleRouter;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import static org.junit.Assert.*;

/**
 * LE GRAND TEST (Test d'intégration global du Monolithe Modulaire).
 * 
 * Ce test principal orchestre et vérifie l'assemblage complet de l'ensemble
 * des modules de l'application :
 * - Initialisation via AppGlobalRouter
 * - Validation de tous les routeurs concrets
 * - Flux E-Commerce : Catalogue PostgreSQL -> Filtres Catégories -> Scan ARCore + YOLO + SAM + COLMAP (Firebase) -> 3D AR
 */
public class GrandIntegrationTest {

    @Before
    public void setUp() {
        // Le fichier principal initialise et relie tous les modules
        AppGlobalRouter.initializeRouters();
    }

    @Test
    public void testGrandIntegration_AllModuleRoutersRegistered() {
        RouterRegistry registry = RouterRegistry.getInstance();

        // 1. Vérifier que chaque module a bien branché son routeur concret
        assertTrue("ProductRouter doit être ProductModuleRouter",
                registry.getProductRouter() instanceof ProductModuleRouter);

        assertTrue("ArRouter doit être ArModuleRouter",
                registry.getArRouter() instanceof ArModuleRouter);

        assertTrue("ScannerRouter doit être ScannerModuleRouter",
                registry.getScannerRouter() instanceof ScannerModuleRouter);

        assertTrue("AuthRouter doit être AuthModuleRouter",
                registry.getAuthRouter() instanceof AuthModuleRouter);
    }

    @Test
    public void testGrandIntegration_CatalogToArFlow() {
        // 2. Simuler le catalogue issu de la base PostgreSQL
        PostgresBackendApi backendApi = new MockPostgresBackendService();
        final boolean[] catalogVerified = {false};

        // Filtre : Catégorie 'Mobilier' avec 3D AR
        FilterCriteria filter = new FilterCriteria();
        filter.setCategoryId("cat_furniture");
        filter.setArOnly(true);

        backendApi.getProducts(filter, new PostgresBackendApi.ApiCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> products) {
                assertNotNull(products);
                assertFalse("Il doit y avoir des meubles avec AR", products.isEmpty());

                Product target = products.get(0);
                assertEquals("cat_furniture", target.getCategoryId());
                assertTrue(target.isArSupported());
                assertNotNull(target.getModel3dUrl());

                // Appel du routeur AR depuis le catalogue
                RouterRegistry.getInstance().getArRouter().openArViewer(
                        null,
                        target.getId(),
                        target.getTitle(),
                        target.getModel3dUrl()
                );

                catalogVerified[0] = true;
            }

            @Override
            public void onError(String errorMessage) {
                fail(errorMessage);
            }
        });

        assertTrue("Le flux Catalogue -> AR doit réussir", catalogVerified[0]);
    }

    @Test
    public void testGrandIntegration_ScannerYoloSamColmapPipeline() {
        // 3. Pipeline de scan vendeur : ARCore surface -> YOLO Validation -> SAM (Meta) -> COLMAP -> GLB
        FirebaseScan3dBridgeService scanService = new FirebaseScan3dBridgeService();
        Scan3dJob scanJob = scanService.createJob("prod_scanned_1", "seller_john", "cat_furniture");

        assertNotNull(scanJob);
        assertEquals(ScanJobStatus.SURFACE_DETECTION, scanJob.getStatus());

        // Validation par YOLO
        boolean isValid = scanService.validateWithYolo(scanJob.getJobId(), "chair", 0.94f, true);
        assertTrue("YOLO doit valider la chaise pour Mobilier", isValid);
        assertEquals(ScanJobStatus.CAPTURING_FRAMES, scanJob.getStatus());

        // Simulation du traitement SAM (Meta) sur PC
        scanJob.setStatus(ScanJobStatus.SAM_SEGMENTING);
        scanJob.setTotalFrames(20);
        scanJob.setProcessedFrames(10);
        assertEquals(55, scanJob.getProgressPercentage());

        // Reconstruction COLMAP sur PC
        scanJob.setStatus(ScanJobStatus.COLMAP_RECONSTRUCTING);
        assertEquals(85, scanJob.getProgressPercentage());

        // Export GLB terminé
        scanJob.setStatus(ScanJobStatus.COMPLETED);
        String finalGlbUrl = "https://storage.firebase.com/models/prod_scanned_1.glb";
        scanJob.setModel3dUrl(finalGlbUrl);
        assertEquals(100, scanJob.getProgressPercentage());
    }
}
