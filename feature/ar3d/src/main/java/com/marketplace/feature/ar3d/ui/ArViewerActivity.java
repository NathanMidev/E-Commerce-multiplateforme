package com.marketplace.feature.ar3d.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.feature.ar3d.R;

public class ArViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";
    public static final String EXTRA_PRODUCT_NAME = "EXTRA_PRODUCT_NAME";
    public static final String EXTRA_MODEL_URL = "EXTRA_MODEL_URL";

    private TextView tvProductName;
    private TextView tvModelStatus;
    private View arSurfacePlaceholder;
    private Button btnPlaceObject;
    private Button btnResetAr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_viewer);

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        String productName = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
        String modelUrl = getIntent().getStringExtra(EXTRA_MODEL_URL);

        tvProductName = findViewById(R.id.tv_ar_product_name);
        tvModelStatus = findViewById(R.id.tv_ar_model_status);
        arSurfacePlaceholder = findViewById(R.id.ar_surface_placeholder);
        btnPlaceObject = findViewById(R.id.btn_place_object);
        btnResetAr = findViewById(R.id.btn_reset_ar);

        if (productName != null) {
            tvProductName.setText(productName);
        }

        if (modelUrl != null && !modelUrl.isEmpty()) {
            tvModelStatus.setText("Modèle 3D prêt : " + modelUrl);
        } else {
            tvModelStatus.setText("Aucun maillage 3D trouvé.");
        }

        btnPlaceObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ArViewerActivity.this, "Objet 3D ancré au sol dans votre environnement !", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetAr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ArViewerActivity.this, "Position de l'objet réinitialisée.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
