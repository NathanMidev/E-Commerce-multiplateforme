package com.marketplace.core.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ModelUnitTest {

    @Test
    public void testFilterCriteria_CategoryMatch() {
        Product product = new Product();
        product.setId("p1");
        product.setTitle("Fauteuil Design");
        product.setCategoryId("furniture");
        product.setPrice(150.0);
        product.setArSupported(true);
        product.setModel3dUrl("https://storage.firebase.com/models/chair.glb");

        FilterCriteria criteria = new FilterCriteria();
        criteria.setCategoryId("furniture");
        criteria.setArOnly(true);
        criteria.setMinPrice(100.0);
        criteria.setMaxPrice(200.0);

        assertTrue("Le produit correspond aux filtres de catégorie et AR", criteria.matches(product));
    }

    @Test
    public void testFilterCriteria_NonMatchingCategory() {
        Product product = new Product();
        product.setId("p2");
        product.setTitle("Smartphone Pro");
        product.setCategoryId("electronics");
        product.setPrice(800.0);

        FilterCriteria criteria = new FilterCriteria();
        criteria.setCategoryId("furniture");

        assertFalse("Le produit ne doit pas correspondre à une autre catégorie", criteria.matches(product));
    }

    @Test
    public void testSamScanJobProgress() {
        SamScanJob job = new SamScanJob("job-01", "p1", "seller-01");
        assertEquals(5, job.getProgressPercentage());

        job.setStatus(SamScanJob.Status.SAM_PROCESSING);
        job.setTotalFrames(100);
        job.setProcessedFrames(50);
        assertEquals(45, job.getProgressPercentage());

        job.setStatus(SamScanJob.Status.COMPLETED);
        assertEquals(100, job.getProgressPercentage());
    }
}
