package com.marketplace.feature.scanner3d;

import com.marketplace.core.firebase.FirebaseSamBridgeService;
import com.marketplace.core.model.SamScanJob;
import com.marketplace.feature.scanner3d.router.ScannerModuleRouter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test unitaire isolé du module Scanner 3D (:feature:scanner3d).
 * Ne dépend d'aucun autre module fonctionnel.
 */
public class ScannerUnitTest {

    @Test
    public void testScannerModuleRouterIsolated() {
        ScannerModuleRouter router = new ScannerModuleRouter();
        assertNotNull("Le routeur scanner doit être instanciable isolément", router);
        router.openScanner(null, "p_test");
        router.openSamJobStatus(null, "job_test");
    }

    @Test
    public void testFirebaseSamBridgeJobCreation() {
        FirebaseSamBridgeService service = new FirebaseSamBridgeService();
        SamScanJob job = service.createJob("prod_99", "seller_99", 25);
        assertNotNull(job);
        assertEquals("prod_99", job.getProductId());
        assertEquals("seller_99", job.getSellerId());
        assertEquals(25, job.getTotalFrames());
        assertEquals(SamScanJob.Status.PENDING, job.getStatus());
    }
}
