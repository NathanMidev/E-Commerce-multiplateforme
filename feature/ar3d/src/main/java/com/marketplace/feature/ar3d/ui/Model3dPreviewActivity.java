package com.marketplace.feature.ar3d.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.feature.ar3d.R;

public class Model3dPreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model3d_preview);

        String modelUrl = getIntent().getStringExtra("EXTRA_MODEL_URL");
        TextView tv = findViewById(R.id.tv_3d_preview_info);
        if (tv != null) {
            tv.setText("Aperçu 3D interactif : " + (modelUrl != null ? modelUrl : "Non spécifié"));
        }
    }
}
