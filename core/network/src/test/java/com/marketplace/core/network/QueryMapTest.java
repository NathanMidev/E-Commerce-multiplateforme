package com.marketplace.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.marketplace.core.model.FilterCriteria;

import org.junit.Test;

import java.util.Map;

/** Les filtres de l'app sont transmis au serveur avec les noms de paramètres que l'API attend. */
public class QueryMapTest {

    @Test
    public void noFilterMeansNoParameter() {
        assertTrue(PostgresBackendServiceImpl.toQuery(null).isEmpty());
    }

    @Test
    public void filterCriteriaBecomesServerParameters() {
        FilterCriteria f = new FilterCriteria();
        f.setCategoryId("cat_fashion");
        f.setMinPrice(100.0);
        f.setMaxPrice(250.5);
        f.setArOnly(true);
        f.setSearchQuery("  sneakers ");
        f.setSortBy("price_asc");

        Map<String, String> q = PostgresBackendServiceImpl.toQuery(f);
        assertEquals("cat_fashion", q.get("categoryId"));
        assertEquals("100.0", q.get("minPrice"));
        assertEquals("250.5", q.get("maxPrice"));
        assertEquals("true", q.get("arOnly"));
        assertEquals("sneakers", q.get("searchQuery"));
        assertEquals("price_asc", q.get("sortBy"));
    }

    @Test
    public void emptyValuesAreNotSent() {
        FilterCriteria f = new FilterCriteria();   // arOnly = false, tri "recent" par défaut
        f.setCategoryId("");
        f.setSearchQuery("   ");

        Map<String, String> q = PostgresBackendServiceImpl.toQuery(f);
        assertFalse(q.containsKey("categoryId"));
        assertFalse(q.containsKey("searchQuery"));
        assertFalse(q.containsKey("arOnly"));
        assertFalse(q.containsKey("minPrice"));
        assertEquals("recent", q.get("sortBy"));
    }
}
