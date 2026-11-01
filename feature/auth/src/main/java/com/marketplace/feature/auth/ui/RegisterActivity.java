package com.marketplace.feature.auth.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.marketplace.core.model.User;
import com.marketplace.feature.auth.R;
import com.marketplace.feature.auth.repository.AuthRepository;
import com.marketplace.feature.auth.router.AuthModuleRouter;

/** Création d'un compte email / mot de passe (Firebase Auth) puis finalisation du profil. */
public class RegisterActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6; // minimum imposé par Firebase

    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private Button btnRegister;

    private AuthRepository authRepository;
    private AuthModuleRouter router;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository();
        router = new AuthModuleRouter();

        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> register());
    }

    private void register() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (name.length() < 2) {
            editName.setError("Entrez votre nom ou le nom de votre boutique");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Adresse email invalide");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            editPassword.setError("6 caractères minimum");
            return;
        }

        btnRegister.setEnabled(false);
        authRepository.createAccount(name, email, password, new AuthRepository.OnProfileLoadedListener() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(RegisterActivity.this, "Compte créé !", Toast.LENGTH_SHORT).show();
                // Un nouveau compte n'a pas encore de WhatsApp : on finalise le profil
                router.navigateToProfileCompletion(RegisterActivity.this);
                finish();
            }

            @Override
            public void onError(String message) {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
