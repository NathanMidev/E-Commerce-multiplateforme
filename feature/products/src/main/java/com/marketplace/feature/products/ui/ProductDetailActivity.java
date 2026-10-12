package com.marketplace.feature.products.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.core.model.Product;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.core.network.MockPostgresBackendService;
import com.marketplace.core.network.PostgresBackendApi;
import com.marketplace.feature.products.R;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";

    private TextView tvTitle;
    private TextView tvPrice;
    private TextView tvDescription;
    private TextView tvSeller;
    private Button btnViewAr;
    private Button btnChatSeller;
    private Button btnScan3d;

    private PostgresBackendApi backendApi;
    private Product currentProduct;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        backendApi = new MockPostgresBackendService();

        tvTitle = findViewById(R.id.tv_detail_title);
        tvPrice = findViewById(R.id.tv_detail_price);
        tvDescription = findViewById(R.id.tv_detail_description);
        tvSeller = findViewById(R.id.tv_detail_seller);
        btnViewAr = findViewById(R.id.btn_view_ar);
        btnChatSeller = findViewById(R.id.btn_chat_seller);
        btnScan3d = findViewById(R.id.btn_scan_3d);

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null) productId = "prod_1";

        loadProductDetails(productId);
        setupActions();
    }

    private void loadProductDetails(String productId) {
        backendApi.getProductById(productId, new PostgresBackendApi.ApiCallback<Product>() {
            @Override
            public void onSuccess(Product product) {
                currentProduct = product;
                tvTitle.setText(product.getTitle());
                tvPrice.setText(String.format("%.2f €", product.getPrice()));
                tvDescription.setText(product.getDescription());
                tvSeller.setText("Vendeur : " + product.getSellerName());

                if (product.isArSupported() && product.getModel3dUrl() != null && !product.getModel3dUrl().isEmpty()) {
                    btnViewAr.setVisibility(View.VISIBLE);
                } else {
                    btnViewAr.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProductDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActions() {
        // 1. Bouton AR : Appel au module :feature:ar3d via le Routeur (découplage total)
        btnViewAr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentProduct != null) {
                    RouterRegistry.getInstance().getArRouter().openArViewer(
                            ProductDetailActivity.this,
                            currentProduct.getId(),
                            currentProduct.getTitle(),
                            currentProduct.getModel3dUrl()
                    );
                }
            }
        });

        // 2. Bouton Chat : Appel au module :feature:chat via le Routeur (découplage total)
        btnChatSeller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentProduct != null) {
                    RouterRegistry.getInstance().getChatRouter().openChatWithSeller(
                            ProductDetailActivity.this,
                            currentProduct.getSellerId(),
                            currentProduct.getSellerName(),
                            currentProduct.getId(),
                            currentProduct.getTitle()
                    );
                }
            }
        });

        // 3. Bouton Scanner 3D : Appel au module :feature:scanner3d (scan SAM Meta)
        btnScan3d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentProduct != null) {
                    RouterRegistry.getInstance().getScannerRouter().openScanner(
                            ProductDetailActivity.this,
                            currentProduct.getId()
                    );
                }
            }
        });
    }
}
