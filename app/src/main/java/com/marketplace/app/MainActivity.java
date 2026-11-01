package com.marketplace.app;

import android.widget.Button;

import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.navigation.Router;

/**
 * Rôle : Écran d'accueil principal (Dashboard commercial avec navigation basse et accès rapide).
 * Entrée : Destinations Accueil, Catalogue, Panier, Commandes, Scanner 3D, Profil.
 * Sortie : Routage unifié vers les différentes fonctionnalités.
 */
public class MainActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void bindViews() {
        Button btnNavCatalog = findViewById(R.id.btn_nav_catalog);
        Button btnNavCart = findViewById(R.id.btn_nav_cart);
        Button btnNavOrders = findViewById(R.id.btn_nav_orders);
        Button btnNavAuth = findViewById(R.id.btn_nav_auth);
        Button btnScanCenter = findViewById(R.id.btn_scan_center);

        Button btnBottomHome = findViewById(R.id.btn_bottom_home);
        Button btnBottomCatalog = findViewById(R.id.btn_bottom_catalog);
        Button btnBottomCart = findViewById(R.id.btn_bottom_cart);
        Button btnBottomProfile = findViewById(R.id.btn_bottom_profile);

        btnNavCatalog.setOnClickListener(v -> Router.openCatalogue(this));
        btnBottomCatalog.setOnClickListener(v -> Router.openCatalogue(this));

        btnNavCart.setOnClickListener(v -> Router.openCart(this));
        btnBottomCart.setOnClickListener(v -> Router.openCart(this));

        btnNavOrders.setOnClickListener(v -> Router.openOrders(this));

        btnNavAuth.setOnClickListener(v -> requireLogin(() -> Router.openProfile(this)));
        btnBottomProfile.setOnClickListener(v -> requireLogin(() -> Router.openProfile(this)));

        btnScanCenter.setOnClickListener(v -> requireLogin(() -> Router.openScanner(this, "prod_new", "cat_furniture")));
    }

    @Override
    protected void loadData() {}

    @Override
    protected void render(Object data) {}
}
