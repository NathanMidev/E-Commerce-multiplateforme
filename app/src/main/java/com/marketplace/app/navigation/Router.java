package com.marketplace.app.navigation;

import android.content.Context;
import android.content.Intent;

/**
 * Rôle : Routeur unique et centralisé de l'application.
 * Entrée : Méthodes statiques openXxx(Context context, ...).
 * Sortie : Démarrage explicite d'activités avec gestion sécurisée des Intent.
 */
public final class Router {

    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";
    public static final String EXTRA_CATEGORY_ID = "EXTRA_CATEGORY_ID";
    public static final String EXTRA_SELLER_ID = "EXTRA_SELLER_ID";
    public static final String EXTRA_ORDER_ID = "EXTRA_ORDER_ID";
    public static final String EXTRA_MODEL_URL = "EXTRA_MODEL_URL";
    public static final String EXTRA_JOB_ID = "EXTRA_JOB_ID";

    private Router() {}

    private static void start(Context context, String className, Intent customIntent) {
        if (context == null) return;
        try {
            Class<?> clazz = Class.forName(className);
            Intent intent = customIntent != null ? customIntent : new Intent(context, clazz);
            intent.setClass(context, clazz);
            context.startActivity(intent);
        } catch (ClassNotFoundException ignored) {
        }
    }

    public static void openLogin(Context context) {
        start(context, "com.marketplace.app.feature.auth.LoginActivity", null);
    }

    public static void openRegister(Context context) {
        start(context, "com.marketplace.app.feature.auth.RegisterActivity", null);
    }

    public static void openProfile(Context context) {
        start(context, "com.marketplace.app.feature.auth.ProfileActivity", null);
    }

    public static void openMain(Context context) {
        if (context == null) return;
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        }
    }

    public static void openCatalogue(Context context) {
        start(context, "com.marketplace.app.feature.catalogue.CatalogueActivity", null);
    }

    public static void openProductDetail(Context context, String productId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        start(context, "com.marketplace.app.feature.catalogue.ProductDetailActivity", intent);
    }

    public static void openPublishProduct(Context context) {
        start(context, "com.marketplace.app.feature.catalogue.PublishProductActivity", null);
    }

    public static void openBoutique(Context context, String sellerId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SELLER_ID, sellerId);
        start(context, "com.marketplace.app.feature.boutique.BoutiqueActivity", intent);
    }

    public static void openCart(Context context) {
        start(context, "com.marketplace.app.transaction.panier.CartActivity", null);
    }

    public static void openCheckout(Context context) {
        start(context, "com.marketplace.app.transaction.commande.CheckoutActivity", null);
    }

    public static void openOrders(Context context) {
        start(context, "com.marketplace.app.transaction.commande.OrdersActivity", null);
    }

    public static void openOrderDetail(Context context, String orderId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ORDER_ID, orderId);
        start(context, "com.marketplace.app.transaction.commande.OrderDetailActivity", intent);
    }

    public static void openScanner(Context context, String productId, String categoryId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        intent.putExtra(EXTRA_CATEGORY_ID, categoryId);
        start(context, "com.marketplace.app.feature.scan3d.ScanCaptureActivity", intent);
    }

    public static void openScanJobStatus(Context context, String jobId) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_JOB_ID, jobId);
        start(context, "com.marketplace.app.feature.scan3d.ScanStatusActivity", intent);
    }

    public static void openArViewer(Context context, String productId, String productName, String modelUrl) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        intent.putExtra("EXTRA_PRODUCT_NAME", productName);
        intent.putExtra(EXTRA_MODEL_URL, modelUrl);
        start(context, "com.marketplace.app.feature.ar.ArViewerActivity", intent);
    }
}
