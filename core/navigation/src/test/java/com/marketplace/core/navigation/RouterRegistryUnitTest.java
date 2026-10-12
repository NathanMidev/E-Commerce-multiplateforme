package com.marketplace.core.navigation;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RouterRegistryUnitTest {

    @Before
    public void setUp() {
        RouterRegistry.getInstance().resetToNoOp();
    }

    @Test
    public void testRegistryDefaultNoOp_NotNull() {
        assertNotNull("ProductRouter ne doit pas être null par défaut", RouterRegistry.getInstance().getProductRouter());
        assertNotNull("ArRouter ne doit pas être null par défaut", RouterRegistry.getInstance().getArRouter());
        assertNotNull("ScannerRouter ne doit pas être null par défaut", RouterRegistry.getInstance().getScannerRouter());
        assertNotNull("ChatRouter ne doit pas être null par défaut", RouterRegistry.getInstance().getChatRouter());
        assertNotNull("AuthRouter ne doit pas être null par défaut", RouterRegistry.getInstance().getAuthRouter());
    }

    @Test
    public void testRegistryRegistration() {
        final boolean[] called = {false};
        ProductRouter customRouter = new ProductRouter() {
            @Override public void openCatalog(android.content.Context context) { called[0] = true; }
            @Override public void openCatalogWithCategory(android.content.Context context, String cId, String cName) {}
            @Override public void openProductDetail(android.content.Context context, String pId) {}
        };

        RouterRegistry.getInstance().registerProductRouter(customRouter);
        assertEquals(customRouter, RouterRegistry.getInstance().getProductRouter());
        RouterRegistry.getInstance().getProductRouter().openCatalog(null);
        assertTrue(called[0]);
    }
}
