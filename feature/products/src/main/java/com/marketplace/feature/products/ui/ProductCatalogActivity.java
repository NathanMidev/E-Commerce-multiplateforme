package com.marketplace.feature.products.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.marketplace.core.model.Category;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Product;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.core.network.MockPostgresBackendService;
import com.marketplace.core.network.PostgresBackendApi;
import com.marketplace.feature.products.R;

import java.util.ArrayList;
import java.util.List;

public class ProductCatalogActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "EXTRA_CATEGORY_ID";
    public static final String EXTRA_CATEGORY_NAME = "EXTRA_CATEGORY_NAME";

    private Spinner spinnerCategories;
    private CheckBox cbArOnly;
    private RecyclerView rvProducts;
    private View layoutEmptyState;
    private TextView tvEmptyState;
    private ProgressBar progressBar;

    private PostgresBackendApi backendApi;
    private ProductAdapter adapter;
    private final List<Category> categoriesList = new ArrayList<>();
    private final FilterCriteria currentFilter = new FilterCriteria();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_catalog);

        backendApi = new MockPostgresBackendService();

        initViews();
        setupRecyclerView();
        loadCategories();
        applyInitialCategoryFromIntent();
    }

    private void initViews() {
        spinnerCategories = findViewById(R.id.spinner_categories);
        cbArOnly = findViewById(R.id.cb_ar_only);
        rvProducts = findViewById(R.id.rv_products);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyState = findViewById(R.id.tv_empty_message);
        progressBar = findViewById(R.id.progress_bar);

        cbArOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentFilter.setArOnly(isChecked);
                loadProducts();
            }
        });
    }

    private void setupRecyclerView() {
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // Routage interne au module produit
                RouterRegistry.getInstance().getProductRouter().openProductDetail(ProductCatalogActivity.this, product.getId());
            }

            @Override
            public void onArClick(Product product) {
                // Routage découplé vers le module :feature:ar3d (aucune dépendance directe)
                RouterRegistry.getInstance().getArRouter().openArViewer(
                        ProductCatalogActivity.this,
                        product.getId(),
                        product.getTitle(),
                        product.getModel3dUrl()
                );
            }
        });
        rvProducts.setAdapter(adapter);
    }

    private void loadCategories() {
        backendApi.getCategories(new PostgresBackendApi.ApiCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                categoriesList.clear();
                categoriesList.add(new Category("", "Toutes les catégories", "", ""));
                categoriesList.addAll(result);

                List<String> names = new ArrayList<>();
                for (Category c : categoriesList) {
                    names.add(c.getName());
                }

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        ProductCatalogActivity.this,
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
            public void onError(String errorMessage) {
                Toast.makeText(ProductCatalogActivity.this, "Erreur catégories : " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyInitialCategoryFromIntent() {
        String categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        if (categoryId != null && !categoryId.isEmpty()) {
            currentFilter.setCategoryId(categoryId);
        }
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        backendApi.getProducts(currentFilter, new PostgresBackendApi.ApiCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                progressBar.setVisibility(View.GONE);
                if (result == null || result.isEmpty()) {
                    adapter.submitList(new ArrayList<Product>());
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Aucun produit trouvé pour ces critères de filtre.");
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    adapter.submitList(result);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Erreur réseau (PostgreSQL) : " + errorMessage);
            }
        });
    }
}
