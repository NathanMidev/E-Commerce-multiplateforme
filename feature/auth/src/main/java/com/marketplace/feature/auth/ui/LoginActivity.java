package com.marketplace.feature.auth.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.feature.auth.R;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editPassword;
    private Button btnLogin;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
                String pass = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";

                if (email.isEmpty() || pass.isEmpty()) {
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Veuillez saisir votre email et mot de passe");
                    }
                    Toast.makeText(LoginActivity.this, "Champs requis", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(LoginActivity.this, "Connexion réussie : " + email, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
