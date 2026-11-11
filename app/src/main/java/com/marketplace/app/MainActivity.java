package com.marketplace.app;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.ar.ArLauncher;
import com.marketplace.app.feature.catalogue.ProductCardAdapter;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.ProductStatus;
import com.marketplace.app.navigation.Router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rôle : Écran d'accueil principal (Dashboard inspiré de Deal.mg, Taobao et 1688).
 * Entrée : Destinations Accueil, Nouveautés dynamiques, Catalogue, Panier, Commandes, Scanner 3D, Profil.
 * Sortie : Routage unifié et grille dynamique des nouveaux produits publiés récemment sur le Marketplace local.
 */
public class MainActivity extends BaseActivity {

    private static final int PAGE_SIZE = 6;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvNewProducts;
    private View layoutEmptyProducts;
    private ProgressBar progressPagination;
    private ProductCardAdapter productCardAdapter;
    private ProductRepository productRepository;

    private final List<Product> eligibleProducts = new ArrayList<>();
    private final List<Product> displayedProducts = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoadingMore = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void bindViews() {
        productRepository = new ProductRepository();

        View btnSearchTrigger = findViewById(R.id.layout_search_trigger);
        View btnPublishMain = findViewById(R.id.btn_publish_main);

        View catHighTech = findViewById(R.id.btn_quick_cat_hightech);
        View catFurniture = findViewById(R.id.btn_quick_cat_furniture);
        View catShoes = findViewById(R.id.btn_quick_cat_shoes);
        View catAr = findViewById(R.id.btn_quick_cat_ar);
        View tvSeeAll = findViewById(R.id.tv_see_all_categories);
        View tvSeeMoreNew = findViewById(R.id.tv_main_see_more_new);

        View cardCatalog = findViewById(R.id.card_shortcut_catalog);
        View cardOrders = findViewById(R.id.card_shortcut_orders);

        View btnBottomHome = findViewById(R.id.btn_bottom_home);
        View btnBottomCatalog = findViewById(R.id.btn_bottom_catalog);
        View btnBottomImport1688 = findViewById(R.id.btn_bottom_import1688);
        View fabImport1688Circle = findViewById(R.id.fab_import1688_circle);
        View btnBottomCart = findViewById(R.id.btn_bottom_cart);
        View btnBottomProfile = findViewById(R.id.btn_bottom_profile);

        // Section Nouveautés dynamiques
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_main);
        rvNewProducts = findViewById(R.id.recycler_main_new_products);
        layoutEmptyProducts = findViewById(R.id.layout_main_empty_products);
        progressPagination = findViewById(R.id.progress_main_pagination);

        if (rvNewProducts != null) {
            rvNewProducts.setLayoutManager(new GridLayoutManager(this, 2));
            productCardAdapter = new ProductCardAdapter(new ProductCardAdapter.OnProductActionClickListener() {
                @Override
                public void onProductClick(Product product) {
                    if (product != null && product.getId() != null) {
                        Router.openProductDetail(MainActivity.this, product.getId());
                    }
                }

                @Override
                public void onArClick(Product product) {
                    if (product != null) {
                        ArLauncher.launchArExperienceIfCompatible(
                                MainActivity.this,
                                product.getId(),
                                product.getTitle(),
                                product.getModel3dUrl()
                        );
                    }
                }
            });
            rvNewProducts.setAdapter(productCardAdapter);
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.primary);
            swipeRefreshLayout.setOnRefreshListener(() -> loadProducts(true));
        }

        // Pagination fluide au défilement
        NestedScrollView scrollView = findViewById(R.id.scroll_view_main);
        if (scrollView != null) {
            scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > oldScrollY) {
                    View child = v.getChildAt(v.getChildCount() - 1);
                    if (child != null) {
                        int diff = (child.getBottom() - (v.getHeight() + scrollY));
                        if (diff <= 350 && !isLoadingMore && (currentPage * PAGE_SIZE < eligibleProducts.size())) {
                            isLoadingMore = true;
                            if (progressPagination != null) {
                                progressPagination.setVisibility(View.VISIBLE);
                            }
                            v.postDelayed(this::loadNextPage, 250);
                        }
                    }
                }
            });
        }

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
        if (tvSeeMoreNew != null) tvSeeMoreNew.setOnClickListener(v -> Router.openCatalogue(this));
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

        // Déposer annonce (inclut l'import optionnel d'un modèle 3D .glb)
        if (btnPublishMain != null) btnPublishMain.setOnClickListener(v -> requireLogin(() -> Router.openProductPublish(this)));

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
    protected void loadData() {
        loadProducts(false);
    }

    /**
     * Charge les véritables produits publiés sur le Marketplace local.
     */
    private void loadProducts(boolean isRefresh) {
        if (isRefresh && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        productRepository.list(null, new BaseRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                processEligibleProducts(result);
            }

            @Override
            public void onError(String message) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (displayedProducts.isEmpty()) {
                    showEmptyState(true);
                }
            }
        });
    }

    /**
     * Filtre les produits publiés et applique un mélange aléatoire raisonnable
     * pour renouveler les découvertes à chaque actualisation.
     */
    private void processEligibleProducts(List<Product> allProducts) {
        eligibleProducts.clear();
        if (allProducts != null) {
            for (Product product : allProducts) {
                if (product != null && isProductPublished(product)) {
                    eligibleProducts.add(product);
                }
            }
        }

        // Mélange aléatoire raisonnable pour renouveler les découvertes
        Collections.shuffle(eligibleProducts);

        currentPage = 0;
        displayedProducts.clear();
        loadNextPage();
    }

    private boolean isProductPublished(@NonNull Product product) {
        ProductStatus status = product.getStatus();
        return status == ProductStatus.DISPONIBLE || status == ProductStatus.NOUVEAU;
    }

    /**
     * Pagination progressive : charge un sous-ensemble de produits supplémentaires.
     */
    private void loadNextPage() {
        if (eligibleProducts.isEmpty()) {
            showEmptyState(true);
            return;
        }

        showEmptyState(false);
        int start = currentPage * PAGE_SIZE;
        if (start >= eligibleProducts.size()) {
            if (progressPagination != null) {
                progressPagination.setVisibility(View.GONE);
            }
            isLoadingMore = false;
            return;
        }

        int end = Math.min(start + PAGE_SIZE, eligibleProducts.size());
        List<Product> pageItems = eligibleProducts.subList(start, end);
        displayedProducts.addAll(pageItems);

        if (productCardAdapter != null) {
            productCardAdapter.setItems(new ArrayList<>(displayedProducts));
        }

        currentPage++;
        isLoadingMore = false;
        if (progressPagination != null) {
            progressPagination.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean empty) {
        if (layoutEmptyProducts != null) {
            layoutEmptyProducts.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (rvNewProducts != null) {
            rvNewProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void render(Object data) {}
}
