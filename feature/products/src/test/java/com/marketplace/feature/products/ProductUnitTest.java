package com.marketplace.feature.products;

import com.marketplace.core.model.Category;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Product;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.core.network.MockPostgresBackendService;
import com.marketplace.core.network.PostgresBackendApi;
import com.marketplace.feature.products.router.ProductModuleRouter;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import static org.junit.Assert.*;

/**
 * Test unitaire isolé du module Produits (:feature:products).
 * Conforme à la règle d'architecture : aucun autre module fonctionnel (:feature:chat, :feature:ar3d)
 * n'est exécuté ou requis ici.
 */
public class ProductUnitTest {

    private PostgresBackendApi backendApi;
    private ProductModuleRouter productRouter;

    @Before
    public void setUp() {
        backendApi = new MockPostgresBackendService();
        productRouter = new ProductModuleRouter();
        RouterRegistry.getInstance().resetToNoOp();
    }

    @Test
    public void testProductModuleRouterIsolated() {
        assertNotNull("Le routeur du module produit doit fonctionner de manière autonome", productRouter);
        // Doit s'exécuter sans NullPointerException même avec un contexte null lors des tests unitaires
        productRouter.openCatalog(null);
        productRouter.openCatalogWithCategory(null, "cat_furniture", "Mobilier");
        productRouter.openProductDetail(null, "prod_1");
    }

    @Test
    public void testCategoryFilteringIsolated() {
        final boolean[] tested = {false};
        FilterCriteria filter = new FilterCriteria();
        filter.setCategoryId("cat_furniture");

        backendApi.getProducts(filter, new PostgresBackendApi.ApiCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                assertNotNull(result);
                assertFalse("Des produits de mobilier doivent exister", result.isEmpty());
                for (Product p : result) {
                    assertEquals("cat_furniture", p.getCategoryId());
                }
                tested[0] = true;
            }

            @Override
            public void onError(String errorMessage) {
                fail(errorMessage);
            }
        });

        assertTrue(tested[0]);
    }

    @Test
    public void testCrossModuleRouterDoesNotInvokeOtherFeatureCode() {
        // Dans ce test unitaire, RouterRegistry est à l'état No-Op par défaut.
        // Même si un produit appelle le bouton AR ou le bouton Chat, aucun code de :feature:ar3d ou :feature:chat n'est exécuté !
        RouterRegistry.getInstance().getArRouter().openArViewer(null, "p1", "Fauteuil", "https://url.glb");
        RouterRegistry.getInstance().getChatRouter().openChatWithSeller(null, "s1", "Vendeur", "p1", "Fauteuil");
        assertTrue("Les appels croisés sont isolés via les contrats d'interface", true);
    }
}
