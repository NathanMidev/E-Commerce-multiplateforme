package com.marketplace.app;

import com.marketplace.app.router.AppGlobalRouter;
import com.marketplace.core.firebase.FirebaseChatService;
import com.marketplace.core.firebase.FirebaseSamBridgeService;
import com.marketplace.core.model.Category;
import com.marketplace.core.model.ChatMessage;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.SamScanJob;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.core.network.MockPostgresBackendService;
import com.marketplace.core.network.PostgresBackendApi;
import com.marketplace.feature.ar3d.router.ArModuleRouter;
import com.marketplace.feature.auth.router.AuthModuleRouter;
import com.marketplace.feature.chat.router.ChatModuleRouter;
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
 * - Flux E-Commerce : Catalogue PostgreSQL -> Filtres Catégories -> Scan SAM Meta (Firebase) -> 3D AR -> Chat Vendeur
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

        assertTrue("ChatRouter doit être ChatModuleRouter",
                registry.getChatRouter() instanceof ChatModuleRouter);

        assertTrue("AuthRouter doit être AuthModuleRouter",
                registry.getAuthRouter() instanceof AuthModuleRouter);
    }

    @Test
    public void testGrandIntegration_CatalogToArAndChatFlow() {
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

                // Appel du routeur Chat avec le vendeur depuis la fiche produit
                RouterRegistry.getInstance().getChatRouter().openChatWithSeller(
                        null,
                        target.getSellerId(),
                        target.getSellerName(),
                        target.getId(),
                        target.getTitle()
                );

                catalogVerified[0] = true;
            }

            @Override
            public void onError(String errorMessage) {
                fail(errorMessage);
            }
        });

        assertTrue("Le flux Catalogue -> AR -> Chat doit réussir", catalogVerified[0]);
    }

    @Test
    public void testGrandIntegration_ScannerSamToFirebaseToChatPipeline() {
        // 3. Pipeline de scan vendeur : Capture -> SAM Meta via Firebase -> Chat
        FirebaseSamBridgeService samService = new FirebaseSamBridgeService();
        SamScanJob scanJob = samService.createJob("prod_scanned_1", "seller_john", 20);

        assertNotNull(scanJob);
        assertEquals(SamScanJob.Status.PENDING, scanJob.getStatus());

        // Simulation du traitement SAM (Meta)
        scanJob.setStatus(SamScanJob.Status.SAM_PROCESSING);
        scanJob.setProcessedFrames(10);
        assertEquals(45, scanJob.getProgressPercentage());

        // Reconstruction terminée
        scanJob.setStatus(SamScanJob.Status.COMPLETED);
        String finalGlbUrl = "https://storage.firebase.com/models/prod_scanned_1.glb";
        scanJob.setModel3dUrl(finalGlbUrl);
        assertEquals(100, scanJob.getProgressPercentage());

        // L'acheteur peut maintenant discuter avec le vendeur
        FirebaseChatService chatService = new FirebaseChatService();
        chatService.sendMessage("conv_1", "buyer_alice", "seller_john", "J'ai vu votre modèle 3D en AR, il est parfait !", "prod_scanned_1");

        List<ChatMessage> messages = chatService.getMessages("conv_1");
        assertEquals(1, messages.size());
        assertEquals("buyer_alice", messages.get(0).getSenderId());
        assertEquals("prod_scanned_1", messages.get(0).getProductId());
    }
}
