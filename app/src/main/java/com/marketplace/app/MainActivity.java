package com.marketplace.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.core.navigation.RouterRegistry;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnNavCatalog = findViewById(R.id.btn_nav_catalog);
        Button btnNavScanner = findViewById(R.id.btn_nav_scanner);
        Button btnNavChat = findViewById(R.id.btn_nav_chat);
        Button btnNavAuth = findViewById(R.id.btn_nav_auth);

        // 1. Ouvrir le catalogue & filtres (module :feature:products)
        btnNavCatalog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouterRegistry.getInstance().getProductRouter().openCatalog(MainActivity.this);
            }
        });

        // 2. Ouvrir le scanner 3D / Meta SAM (module :feature:scanner3d)
        btnNavScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouterRegistry.getInstance().getScannerRouter().openScanner(MainActivity.this, "prod_demo");
            }
        });

        // 3. Ouvrir la messagerie (module :feature:chat)
        btnNavChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouterRegistry.getInstance().getChatRouter().openConversationsList(MainActivity.this);
            }
        });

        // 4. Ouvrir la connexion / profil (module :feature:auth)
        btnNavAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RouterRegistry.getInstance().getAuthRouter().openLogin(MainActivity.this);
            }
        });
    }
}
