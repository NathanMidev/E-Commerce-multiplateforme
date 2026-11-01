package com.marketplace.app.feature.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.marketplace.app.R;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;

/**
 * Rôle : Écran de connexion (Email / Mot de passe & Google Sign-In via Credential Manager).
 * Entrée : Saisie des identifiants utilisateur.
 * Sortie : Authentification Firebase, mise à jour session et routage vers accueil ou finalisation profil.
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    private EditText editEmail;
    private EditText editPassword;
    private Button btnLogin;
    private Button btnGoogle;
    private Button btnGoRegister;
    private TextView tvError;

    private UserRepository userRepository;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    protected void bindViews() {
        userRepository = new UserRepository();
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google);
        btnGoRegister = findViewById(R.id.btn_go_register);
        tvError = findViewById(R.id.tv_empty_state);

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        btnGoRegister.setOnClickListener(v -> Router.openRegister(this));
    }

    @Override
    protected void loadData() {
        // Pas de préchargement nécessaire
    }

    @Override
    protected void render(Object data) {
        // Utilisé après succès d'authentification
    }

    private void loginWithEmail() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Adresse email invalide");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editPassword.setError("Mot de passe requis");
            return;
        }

        setBusy(true);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> onFirebaseAuthenticated())
                .addOnFailureListener(e -> {
                    setBusy(false);
                    showError("Erreur d'authentification : " + e.getMessage(), null);
                });
    }

    private void startGoogleSignIn() {
        setBusy(true);
        CredentialManager credentialManager = CredentialManager.create(this);
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            GoogleIdTokenCredential googleCredential =
                                    GoogleIdTokenCredential.createFrom(result.getCredential().getData());
                            AuthCredential credential = GoogleAuthProvider.getCredential(googleCredential.getIdToken(), null);
                            FirebaseAuth.getInstance().signInWithCredential(credential)
                                    .addOnSuccessListener(authResult -> onFirebaseAuthenticated())
                                    .addOnFailureListener(e -> {
                                        setBusy(false);
                                        showError("Échec connexion Google : " + e.getMessage(), null);
                                    });
                        } catch (Exception e) {
                            Log.e(TAG, "Jeton Google invalide", e);
                            setBusy(false);
                            showError("Jeton Google non reconnu", null);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        setBusy(false);
                        showError("Connexion Google annulée ou impossible", null);
                    }
                });
    }

    private void onFirebaseAuthenticated() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            AuthSession.getInstance().updateSession(uid, null);
        }

        userRepository.getMyProfile(new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                setBusy(false);
                if (user == null || TextUtils.isEmpty(user.getWhatsapp())) {
                    Router.openProfile(LoginActivity.this);
                } else {
                    Router.openMain(LoginActivity.this);
                }
                finish();
            }

            @Override
            public void onError(String message) {
                setBusy(false);
                Router.openProfile(LoginActivity.this);
                finish();
            }
        });
    }

    private void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnGoogle.setEnabled(!busy);
        btnGoRegister.setEnabled(!busy);
    }
}
