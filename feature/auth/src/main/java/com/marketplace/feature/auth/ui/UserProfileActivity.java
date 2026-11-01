package com.marketplace.feature.auth.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.marketplace.feature.auth.R;
import com.marketplace.feature.auth.repository.AuthRepository;
import com.marketplace.feature.auth.router.AuthModuleRouter;

/** Finalisation du profil : WhatsApp (obligatoire), adresse (obligatoire), téléphone. */
public class UserProfileActivity extends AppCompatActivity {

    private EditText etWhatsapp, etAddress, etPhone;
    private Button btnSave;
    private AuthRepository authRepository;
    private AuthModuleRouter router;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        authRepository = new AuthRepository();
        router = new AuthModuleRouter();

        etWhatsapp = findViewById(R.id.et_whatsapp);
        etAddress = findViewById(R.id.et_address);
        etPhone = findViewById(R.id.et_phone);
        btnSave = findViewById(R.id.btn_save_profile);

        btnSave.setOnClickListener(v -> performUpdate());
    }

    private void performUpdate() {
        String whatsapp = etWhatsapp.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (whatsapp.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        authRepository.updateFields(whatsapp, address, phone, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(UserProfileActivity.this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                router.navigateToMain(UserProfileActivity.this);
                finish();
            }

            @Override
            public void onError(String message) {
                btnSave.setEnabled(true);
                Toast.makeText(UserProfileActivity.this, "Erreur: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
