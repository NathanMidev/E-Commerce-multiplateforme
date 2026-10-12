package com.marketplace.feature.ar3d.router;

import android.content.Context;
import android.content.Intent;
import com.marketplace.core.navigation.ArRouter;
import com.marketplace.feature.ar3d.ui.ArViewerActivity;
import com.marketplace.feature.ar3d.ui.Model3dPreviewActivity;

/**
 * Routeur interne pour le module Réalité Augmentée 3D.
 * Implémente le contrat ArRouter défini dans :core:navigation.
 */
public class ArModuleRouter implements ArRouter {

    @Override
    public void openArViewer(Context context, String productId, String productName, String model3dUrl) {
        if (context == null) return;
        Intent intent = new Intent(context, ArViewerActivity.class);
        intent.putExtra(ArViewerActivity.EXTRA_PRODUCT_ID, productId);
        intent.putExtra(ArViewerActivity.EXTRA_PRODUCT_NAME, productName);
        intent.putExtra(ArViewerActivity.EXTRA_MODEL_URL, model3dUrl);
        context.startActivity(intent);
    }

    @Override
    public void open3dPreview(Context context, String productId, String model3dUrl) {
        if (context == null) return;
        Intent intent = new Intent(context, Model3dPreviewActivity.class);
        intent.putExtra("EXTRA_PRODUCT_ID", productId);
        intent.putExtra("EXTRA_MODEL_URL", model3dUrl);
        context.startActivity(intent);
    }
}
