package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

/**
 * Rôle : Critères de filtrage et recherche pour le catalogue de produits.
 * Entrée : Filtres par catégorie, plage de prix, arOnly, mot-clé, tri.
 * Sortie : Fonction matches(Product) et critères pour requêtes API.
 */
public class FilterCriteria extends BaseModel {
    private String categoryId;
    private Double minPrice;
    private Double maxPrice;
    private boolean arOnly;
    private String searchQuery;
    private String sortBy; // "price_asc", "price_desc", "recent"
    private String sellerId;

    public FilterCriteria() {
        super();
        this.arOnly = false;
        this.sortBy = "recent";
    }

    public boolean matches(Product product) {
        if (product == null) return false;
        if (categoryId != null && !categoryId.isEmpty() && !categoryId.equalsIgnoreCase(product.getCategoryId())) {
            return false;
        }
        if (sellerId != null && !sellerId.isEmpty() && !sellerId.equalsIgnoreCase(product.getSellerId())) {
            return false;
        }
        if (minPrice != null && product.getPrice() < minPrice) {
            return false;
        }
        if (maxPrice != null && product.getPrice() > maxPrice) {
            return false;
        }
        if (arOnly && (!product.isArSupported() || product.getModel3dUrl() == null || product.getModel3dUrl().isEmpty())) {
            return false;
        }
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String queryLower = searchQuery.toLowerCase();
            boolean matchTitle = product.getTitle() != null && product.getTitle().toLowerCase().contains(queryLower);
            boolean matchDesc = product.getDescription() != null && product.getDescription().toLowerCase().contains(queryLower);
            if (!matchTitle && !matchDesc) return false;
        }
        return true;
    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

    public boolean isArOnly() { return arOnly; }
    public void setArOnly(boolean arOnly) { this.arOnly = arOnly; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public String getKeyword() { return searchQuery; }
    public void setKeyword(String keyword) { this.searchQuery = keyword; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
}
