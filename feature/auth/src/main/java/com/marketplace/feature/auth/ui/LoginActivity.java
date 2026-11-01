package com.marketplace.feature.auth.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.marketplace.core.model.User;
import com.marketplace.feature.auth.R;
import com.marketplace.feature.auth.repository.AuthRepository;
import com.marketplace.feature.auth.router.AuthModuleRouter;

/**
 * Connexion : email / mot de passe ou compte Google.
 * Au premier login, le document users/{uid} est créé ; si le WhatsApp n'est pas encore
 * renseigné, l'utilisateur est envoyé vers « Complétez votre profil ».
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private AuthRepository authRepository;
    private AuthModuleRouter router;

    private EditText editEmail;
    private EditText editPassword;
    private Button btnLogin;
    private Button btnGoogle;
    private Button btnGoRegister;
    private TextView tvError;

    /** Réception du profil une fois authentifié (email ou Google). */
    private final AuthRepository.OnProfileLoadedListener profileListener =
            new AuthRepository.OnProfileLoadedListener() {
                @Override
                public void onSuccess(User user) {
                    onAuthenticated(user);
                }

                @Override
                public void onError(String message) {
                    showError(message);
                    setBusy(false);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository();
        router = new AuthModuleRouter();

        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google);
        btnGoRegister = findViewById(R.id.btn_go_register);
        tvError = findViewById(R.id.tv_empty_state);

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        btnGoRegister.setOnClickListener(v -> router.openRegister(this));
    }

    // ------------------------------------------------------------ email / mot de passe

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
        authRepository.signInWithEmail(email, password, profileListener);
    }

    // ----------------------------------------------------------------------- Google

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

        // Exécuteur principal : les callbacks reviennent directement sur le thread UI.
        credentialManager.getCredentialAsync(this, request, null, ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleResult(result);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Credential Manager", e);
                        showError("Connexion Google impossible : " + e.getMessage());
                        setBusy(false);
                    }
                });
    }

    private void handleGoogleResult(GetCredentialResponse result) {
        try {
            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(result.getCredential().getData());
            authRepository.signInWithGoogleIdToken(googleCredential.getIdToken(), profileListener);
        } catch (Exception e) {
            Log.e(TAG, "Jeton Google invalide", e);
            showError("Connexion Google impossible. Réessayez.");
            setBusy(false);
        }
    }

    // ------------------------------------------------------------------ navigation

    private void onAuthenticated(User user) {
        // Profil incomplet (pas de WhatsApp) -> finalisation, sinon accueil
        if (user == null || TextUtils.isEmpty(user.getWhatsapp())) {
            router.navigateToProfileCompletion(this);
        } else {
            router.navigateToMain(this);
        }
        finish();
    }

    // -------------------------------------------------------------------- interface

    private void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnGoogle.setEnabled(!busy);
        btnGoRegister.setEnabled(!busy);
        if (busy) {
            tvError.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
