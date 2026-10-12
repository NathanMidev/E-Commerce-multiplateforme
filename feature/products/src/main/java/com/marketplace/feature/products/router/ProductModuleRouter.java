package com.marketplace.feature.products.router;

import android.content.Context;
import android.content.Intent;
import com.marketplace.core.navigation.ProductRouter;
import com.marketplace.feature.products.ui.ProductCatalogActivity;
import com.marketplace.feature.products.ui.ProductDetailActivity;

/**
 * Routeur interne pour le module Produits.
 * Permet d'ouvrir le catalogue et la fiche produit.
 */
public class ProductModuleRouter implements ProductRouter {

    @Override
    public void openCatalog(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, ProductCatalogActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void openCatalogWithCategory(Context context, String categoryId, String categoryName) {
        if (context == null) return;
        Intent intent = new Intent(context, ProductCatalogActivity.class);
        intent.putExtra(ProductCatalogActivity.EXTRA_CATEGORY_ID, categoryId);
        intent.putExtra(ProductCatalogActivity.EXTRA_CATEGORY_NAME, categoryName);
        context.startActivity(intent);
    }

    @Override
    public void openProductDetail(Context context, String productId) {
        if (context == null) return;
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
        context.startActivity(intent);
    }
}
