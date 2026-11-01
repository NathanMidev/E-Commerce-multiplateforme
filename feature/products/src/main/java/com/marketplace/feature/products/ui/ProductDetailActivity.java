package com.marketplace.feature.products.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.core.model.Product;
import com.marketplace.core.navigation.RouterRegistry;
import com.marketplace.core.network.BackendProvider;
import com.marketplace.core.network.PostgresBackendApi;
import com.marketplace.feature.products.R;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";

    private TextView tvTitle;
    private TextView tvPrice;
    private TextView tvDescription;
    private TextView tvSeller;
    private TextView tvStock;
    private TextView tvStatus;
    private TextView tv3dBadge;
    private Button btnViewAr;
    private Button btnScan3d;

    private PostgresBackendApi backendApi;
    private Product currentProduct;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        backendApi = BackendProvider.get();

        tvTitle = findViewById(R.id.tv_detail_title);
        tvPrice = findViewById(R.id.tv_detail_price);
        tvDescription = findViewById(R.id.tv_detail_description);
        tvSeller = findViewById(R.id.tv_detail_seller);
        tvStock = findViewById(R.id.tv_detail_stock);
        tvStatus = findViewById(R.id.tv_detail_status);
        tv3dBadge = findViewById(R.id.tv_detail_3d_badge);
        btnViewAr = findViewById(R.id.btn_view_ar);
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
                tvSeller.setText("Boutique locale : " + product.getSellerName());
                tvStock.setText("Stock local : " + product.getStock() + " unités");

                if (product.getStatus() != null) {
                    tvStatus.setText(product.getStatus().name());
                    switch (product.getStatus()) {
                        case NOUVEAU:
                            tvStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                            tvStatus.setTextColor(Color.parseColor("#1565C0"));
                            break;
                        case DISPONIBLE:
                            tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                            tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                            break;
                        case INDISPONIBLE:
                            tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                            tvStatus.setTextColor(Color.parseColor("#C62828"));
                            break;
                        case A_VENDRE:
                            tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                            tvStatus.setTextColor(Color.parseColor("#E65100"));
                            break;
                    }
                }

                // Affichage du modèle 3D
                if (product.hasModele3d() && product.getModel3dUrl() != null && !product.getModel3dUrl().isEmpty()) {
                    btnViewAr.setVisibility(View.VISIBLE);
                    tv3dBadge.setVisibility(View.VISIBLE);
                } else {
                    btnViewAr.setVisibility(View.GONE);
                    tv3dBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProductDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActions() {
        // Bouton AR : Visualisation dans l'environnement de l'acheteur
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

        // Bouton Scanner 3D : Numérisation par le vendeur local (SAM + COLMAP)
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
