package com.marketplace.app.feature.catalogue;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.marketplace.app.R;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.ar.ArLauncher;
import com.marketplace.app.model.Category;
import com.marketplace.app.model.FilterCriteria;
import com.marketplace.app.model.Product;
import com.marketplace.app.navigation.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Écran d'accueil et catalogue principal de produits style Deal.mg (grille 2 colonnes, chips, recherche).
 * Entrée : Filtres par mot-clé, catégorie et option 3D/AR.
 * Sortie : Affichage des fiches, lancement AR 3D, consultation détaillée et publication.
 */
public class CatalogueActivity extends BaseActivity {

    private EditText etSearchQuery;
    private ImageButton btnClearSearch;
    private Button btnPublishShortcut;
    private ChipGroup chipGroupCategories;
    private Chip chipFilterAr;
    private TextView tvResultsCount;
    private Button btnResetFilters;

    private Spinner spinnerCategories;
    private CheckBox cbArOnly;
    private RecyclerView rvProducts;
    private View layoutEmptyState;
    private TextView tvEmptyState;
    private ProgressBar progressBar;

    private ProductRepository productRepository;
    private ProductCardAdapter adapter;
    private final List<Category> categoriesList = new ArrayList<>();
    private final FilterCriteria currentFilter = new FilterCriteria();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_product_catalog;
    }

    @Override
    protected void bindViews() {
        productRepository = new ProductRepository();
        etSearchQuery = findViewById(R.id.et_search_query);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        btnPublishShortcut = findViewById(R.id.btn_publish_shortcut);
        chipGroupCategories = findViewById(R.id.chip_group_categories);
        chipFilterAr = findViewById(R.id.chip_filter_ar);
        tvResultsCount = findViewById(R.id.tv_results_count);
        btnResetFilters = findViewById(R.id.btn_reset_filters);

        spinnerCategories = findViewById(R.id.spinner_categories);
        cbArOnly = findViewById(R.id.cb_ar_only);
        rvProducts = findViewById(R.id.rv_products);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyState = findViewById(R.id.tv_empty_message);
        progressBar = findViewById(R.id.progress_bar);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductCardAdapter(new ProductCardAdapter.OnProductActionClickListener() {
            @Override
            public void onProductClick(Product product) {
                Router.openProductDetail(CatalogueActivity.this, product.getId());
            }

            @Override
            public void onArClick(Product product) {
                ArLauncher.launchArExperienceIfCompatible(
                        CatalogueActivity.this,
                        product.getId(),
                        product.getTitle(),
                        product.getModel3dUrl()
                );
            }
        });
        rvProducts.setAdapter(adapter);

        // Recherche dynamique
        if (etSearchQuery != null) {
            etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    return true;
                }
                return false;
            });

            etSearchQuery.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                    currentFilter.setKeyword(s.toString().trim());
                    loadProducts();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearchQuery != null) etSearchQuery.setText("");
            });
        }

        // Filtre Chips Catégories
        if (chipGroupCategories != null) {
            chipGroupCategories.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chip_cat_hightech) {
                    currentFilter.setCategoryId("cat_hightech");
                } else if (checkedId == R.id.chip_cat_furniture) {
                    currentFilter.setCategoryId("cat_furniture");
                } else if (checkedId == R.id.chip_cat_shoes) {
                    currentFilter.setCategoryId("cat_shoes");
                } else if (checkedId == R.id.chip_cat_decor) {
                    currentFilter.setCategoryId("cat_decor");
                } else {
                    currentFilter.setCategoryId(null);
                }
                loadProducts();
            });
        }

        if (chipFilterAr != null) {
            chipFilterAr.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentFilter.setArOnly(isChecked);
                loadProducts();
            });
        }

        if (btnPublishShortcut != null) {
            btnPublishShortcut.setOnClickListener(v -> requireLogin(() -> Router.openProductPublish(this)));
        }

        if (btnResetFilters != null) {
            btnResetFilters.setOnClickListener(v -> {
                if (etSearchQuery != null) etSearchQuery.setText("");
                if (chipGroupCategories != null) chipGroupCategories.check(R.id.chip_cat_all);
                if (chipFilterAr != null) chipFilterAr.setChecked(false);
                currentFilter.setKeyword(null);
                currentFilter.setCategoryId(null);
                currentFilter.setArOnly(false);
                loadProducts();
            });
        }
    }

    private void performSearch() {
        if (etSearchQuery != null) {
            currentFilter.setKeyword(etSearchQuery.getText().toString().trim());
            loadProducts();
        }
    }

    @Override
    protected void loadData() {
        loadCategories();
    }

    @Override
    protected void render(Object data) {
        if (data instanceof List) {
            List<Product> products = (List<Product>) data;
            adapter.setItems(products);
            if (products.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Aucune annonce trouvée.");
                if (tvResultsCount != null) tvResultsCount.setText("0 annonce trouvée");
            } else {
                layoutEmptyState.setVisibility(View.GONE);
                if (tvResultsCount != null) {
                    tvResultsCount.setText(products.size() + (products.size() > 1 ? " annonces trouvées" : " annonce trouvée"));
                }
            }
        }
    }

    private void loadCategories() {
        productRepository.listCategories(new BaseRepository.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                categoriesList.clear();
                if (result != null) categoriesList.addAll(result);
                loadProducts();
            }

            @Override
            public void onError(String message) {
                loadProducts();
            }
        });
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        productRepository.list(currentFilter, new BaseRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                progressBar.setVisibility(View.GONE);
                render(result);
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Erreur réseau : " + message);
            }
        });
    }
}
