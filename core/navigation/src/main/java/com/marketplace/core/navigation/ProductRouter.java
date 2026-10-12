package com.marketplace.core.navigation;

import android.content.Context;

/**
 * Contrat de routage pour le module Produits (feature:products).
 * Permet aux autres modules d'appeler les écrans de ce module sans dépendance directe.
 */
public interface ProductRouter {
    void openCatalog(Context context);
    void openCatalogWithCategory(Context context, String categoryId, String categoryName);
    void openProductDetail(Context context, String productId);
}
