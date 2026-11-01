package com.marketplace.app.feature.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.marketplace.app.R;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;

import java.util.HashMap;
import java.util.Map;

/**
 * Rôle : Écran d'inscription utilisateur (Email, Nom, Mot de passe).
 * Entrée : Formulaire de création de compte.
 * Sortie : Création du compte Firebase Auth, enregistrement du profil et redirection vers finalisation.
 */
public class RegisterActivity extends BaseActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private Button btnRegister;

    private UserRepository userRepository;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_register;
    }

    @Override
    protected void bindViews() {
        userRepository = new UserRepository();
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> performRegister());
    }

    @Override
    protected void loadData() {}

    @Override
    protected void render(Object data) {}

    private void performRegister() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (name.length() < 2) {
            editName.setError("Nom trop court");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Adresse email invalide");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            editPassword.setError("6 caractères minimum requis");
            return;
        }

        btnRegister.setEnabled(false);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    AuthSession.getInstance().updateSession(uid, null);

                    Map<String, String> fields = new HashMap<>();
                    fields.put("name", name);
                    userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
                        @Override
                        public void onSuccess(User result) {
                            btnRegister.setEnabled(true);
                            toast("Compte créé avec succès !");
                            Router.openProfile(RegisterActivity.this);
                            finish();
                        }

                        @Override
                        public void onError(String message) {
                            btnRegister.setEnabled(true);
                            Router.openProfile(RegisterActivity.this);
                            finish();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    showError("Impossible de créer le compte : " + e.getMessage(), null);
                });
    }
}
