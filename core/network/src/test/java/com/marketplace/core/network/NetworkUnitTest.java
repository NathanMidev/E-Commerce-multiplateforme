package com.marketplace.core.network;

import com.marketplace.core.model.Category;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Product;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import static org.junit.Assert.*;

public class NetworkUnitTest {

    private PostgresBackendApi api;

    @Before
    public void setUp() {
        api = new MockPostgresBackendService();
    }

    @Test
    public void testGetCategories() {
        final boolean[] success = {false};
        api.getCategories(new PostgresBackendApi.ApiCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                assertNotNull(result);
                assertFalse(result.isEmpty());
                success[0] = true;
            }

            @Override
            public void onError(String errorMessage) {
                fail(errorMessage);
            }
        });
        assertTrue(success[0]);
    }

    @Test
    public void testFilterProductsByAr() {
        FilterCriteria filter = new FilterCriteria();
        filter.setArOnly(true);

        api.getProducts(filter, new PostgresBackendApi.ApiCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                for (Product p : result) {
                    assertTrue("Les produits retournés doivent supporter l'AR", p.isArSupported());
                }
            }

            @Override
            public void onError(String errorMessage) {
                fail(errorMessage);
            }
        });
    }
}
