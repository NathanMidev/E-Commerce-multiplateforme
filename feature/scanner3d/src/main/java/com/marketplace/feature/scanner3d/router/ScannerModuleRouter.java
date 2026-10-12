package com.marketplace.feature.scanner3d.router;

import android.content.Context;
import android.content.Intent;
import com.marketplace.core.navigation.ScannerRouter;
import com.marketplace.feature.scanner3d.ui.SamProcessingStatusActivity;
import com.marketplace.feature.scanner3d.ui.ScanCaptureActivity;

/**
 * Routeur interne pour le module Scanner 3D & Traitement SAM Meta.
 * Implémente le contrat ScannerRouter défini dans :core:navigation.
 */
public class ScannerModuleRouter implements ScannerRouter {

    @Override
    public void openScanner(Context context, String productId) {
        if (context == null) return;
        Intent intent = new Intent(context, ScanCaptureActivity.class);
        intent.putExtra(ScanCaptureActivity.EXTRA_PRODUCT_ID, productId);
        context.startActivity(intent);
    }

    @Override
    public void openSamJobStatus(Context context, String jobId) {
        if (context == null) return;
        Intent intent = new Intent(context, SamProcessingStatusActivity.class);
        intent.putExtra(SamProcessingStatusActivity.EXTRA_JOB_ID, jobId);
        context.startActivity(intent);
    }
}
