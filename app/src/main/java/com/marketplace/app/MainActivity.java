package com.marketplace.app;

import android.view.View;
import android.widget.Button;

import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.navigation.Router;

/**
 * Rôle : Écran d'accueil principal (Dashboard inspiré de Deal.mg avec recherche, catégories et navigation).
 * Entrée : Destinations Accueil, Catalogue, Panier, Commandes, Scanner 3D, Déposer annonce, Profil.
 * Sortie : Routage unifié vers les différentes fonctionnalités.
 */
public class MainActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void bindViews() {
        View btnSearchTrigger = findViewById(R.id.layout_search_trigger);
        View btnPublishMain = findViewById(R.id.btn_publish_main);
        View btnScanCenter = findViewById(R.id.btn_scan_center);

        View catHighTech = findViewById(R.id.btn_quick_cat_hightech);
        View catFurniture = findViewById(R.id.btn_quick_cat_furniture);
        View catShoes = findViewById(R.id.btn_quick_cat_shoes);
        View catAr = findViewById(R.id.btn_quick_cat_ar);
        View tvSeeAll = findViewById(R.id.tv_see_all_categories);

        View cardCatalog = findViewById(R.id.card_shortcut_catalog);
        View cardOrders = findViewById(R.id.card_shortcut_orders);

        View btnBottomHome = findViewById(R.id.btn_bottom_home);
        View btnBottomCatalog = findViewById(R.id.btn_bottom_catalog);
        View btnBottomImport1688 = findViewById(R.id.btn_bottom_import1688);
        View fabImport1688Circle = findViewById(R.id.fab_import1688_circle);
        View btnBottomCart = findViewById(R.id.btn_bottom_cart);
        View btnBottomProfile = findViewById(R.id.btn_bottom_profile);

        // Bouton central Import 1688 avec animation discrète au clic
        if (btnBottomImport1688 != null) {
            btnBottomImport1688.setOnClickListener(v -> {
                View targetAnim = fabImport1688Circle != null ? fabImport1688Circle : btnBottomImport1688;
                targetAnim.animate()
                        .scaleX(0.88f)
                        .scaleY(0.88f)
                        .setDuration(90)
                        .withEndAction(() -> targetAnim.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(90)
                                .withEndAction(() -> Router.openImport1688(this))
                                .start())
                        .start();
            });
        }

        // Actions de recherche & catalogue
        if (btnSearchTrigger != null) btnSearchTrigger.setOnClickListener(v -> Router.openCatalogue(this));
        if (tvSeeAll != null) tvSeeAll.setOnClickListener(v -> Router.openCatalogue(this));
        if (cardCatalog != null) cardCatalog.setOnClickListener(v -> Router.openCatalogue(this));
        if (btnBottomCatalog != null) btnBottomCatalog.setOnClickListener(v -> Router.openCatalogue(this));

        // Catégories directes
        if (catHighTech != null) catHighTech.setOnClickListener(v -> Router.openCatalogue(this));
        if (catFurniture != null) catFurniture.setOnClickListener(v -> Router.openCatalogue(this));
        if (catShoes != null) catShoes.setOnClickListener(v -> Router.openCatalogue(this));
        if (catAr != null) catAr.setOnClickListener(v -> Router.openCatalogue(this));

        // Panier
        if (btnBottomCart != null) btnBottomCart.setOnClickListener(v -> Router.openCart(this));

        // Commandes
        if (cardOrders != null) cardOrders.setOnClickListener(v -> Router.openOrders(this));

        // Profil
        if (btnBottomProfile != null) btnBottomProfile.setOnClickListener(v -> requireLogin(() -> Router.openProfile(this)));

        // Déposer annonce & import manuel d'un modèle 3D .glb (le scan automatique SAM/YOLO est désactivé)
        if (btnPublishMain != null) btnPublishMain.setOnClickListener(v -> requireLogin(() -> Router.openProductPublish(this)));
        if (btnScanCenter != null) btnScanCenter.setOnClickListener(v -> requireLogin(() -> Router.openProductPublish(this)));

        // Boutons de compatibilité anciens tests
        Button btnNavCatalog = findViewById(R.id.btn_nav_catalog);
        if (btnNavCatalog != null) btnNavCatalog.setOnClickListener(v -> Router.openCatalogue(this));
        Button btnNavCart = findViewById(R.id.btn_nav_cart);
        if (btnNavCart != null) btnNavCart.setOnClickListener(v -> Router.openCart(this));
        Button btnNavOrders = findViewById(R.id.btn_nav_orders);
        if (btnNavOrders != null) btnNavOrders.setOnClickListener(v -> Router.openOrders(this));
        Button btnNavAuth = findViewById(R.id.btn_nav_auth);
        if (btnNavAuth != null) btnNavAuth.setOnClickListener(v -> requireLogin(() -> Router.openProfile(this)));
    }

    @Override
    protected void loadData() {}

    @Override
    protected void render(Object data) {}
}
