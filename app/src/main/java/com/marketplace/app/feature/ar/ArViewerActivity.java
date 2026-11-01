package com.marketplace.app.feature.ar;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.marketplace.app.R;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.navigation.Router;

/**
 * Rôle : Écran de visualisation et passerelle AR pour l'acheteur.
 * Entrée : EXTRA_PRODUCT_ID, EXTRA_MODEL_URL.
 * Sortie : Lancement Scene Viewer ou aperçu interactif.
 */
public class ArViewerActivity extends BaseActivity {

    private TextView tvProductName;
    private Button btnLaunchArCore;
    private String productId;
    private String productName;
    private String modelUrl;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ar_viewer;
    }

    @Override
    protected void bindViews() {
        productId = getIntent().getStringExtra(Router.EXTRA_PRODUCT_ID);
        productName = getIntent().getStringExtra("EXTRA_PRODUCT_NAME");
        modelUrl = getIntent().getStringExtra(Router.EXTRA_MODEL_URL);

        tvProductName = findViewById(R.id.tv_ar_product_name);
        btnLaunchArCore = findViewById(R.id.btn_place_object);

        if (productName != null && tvProductName != null) {
            tvProductName.setText(productName);
        }

        if (btnLaunchArCore != null) {
            btnLaunchArCore.setText("Ouvrir en Réalité Augmentée 1:1");
            btnLaunchArCore.setOnClickListener(v -> ArLauncher.launchSceneViewer(this, modelUrl, productName));
        }
    }

    @Override
    protected void loadData() {}

    @Override
    protected void render(Object data) {}
}
