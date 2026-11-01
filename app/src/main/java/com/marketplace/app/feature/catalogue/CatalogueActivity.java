package com.marketplace.app.feature.catalogue;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.marketplace.app.R;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Category;
import com.marketplace.app.model.FilterCriteria;
import com.marketplace.app.model.Product;
import com.marketplace.app.navigation.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Écran d'accueil et catalogue principal de produits (présentation 2 colonnes, filtres).
 * Entrée : Filtres par catégorie et option 3D/AR.
 * Sortie : Affichage des fiches et lancement de la visualisation AR ou détail.
 */
public class CatalogueActivity extends BaseActivity {

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
                Router.openArViewer(CatalogueActivity.this, product.getId(), product.getTitle(), product.getModel3dUrl());
            }
        });
        rvProducts.setAdapter(adapter);

        cbArOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentFilter.setArOnly(isChecked);
            loadProducts();
        });
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
                tvEmptyState.setText("Aucun produit trouvé.");
            } else {
                layoutEmptyState.setVisibility(View.GONE);
            }
        }
    }

    private void loadCategories() {
        productRepository.listCategories(new BaseRepository.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                categoriesList.clear();
                categoriesList.add(new Category("", "Toutes les catégories", "", ""));
                if (result != null) categoriesList.addAll(result);

                List<String> names = new ArrayList<>();
                for (Category c : categoriesList) names.add(c.getName());

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        CatalogueActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        names
                );
                spinnerCategories.setAdapter(spinnerAdapter);
                spinnerCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Category selected = categoriesList.get(position);
                        currentFilter.setCategoryId(selected.getId());
                        loadProducts();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
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
