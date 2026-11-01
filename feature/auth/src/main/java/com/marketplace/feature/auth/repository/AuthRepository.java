package com.marketplace.feature.auth.repository;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.marketplace.core.model.User;
import com.marketplace.core.network.BackendProvider;
import com.marketplace.core.network.PostgresBackendApi;

import java.util.HashMap;
import java.util.Map;

/**
 * Connexion (Firebase Auth) + profil (base PostgreSQL, via le serveur).
 *
 * Firebase ne sert qu'à PROUVER qui est l'utilisateur. Son profil (nom, contacts, statistiques, niveau)
 * vit dans PostgreSQL : l'application mobile et la version web y lisent donc les mêmes données.
 * Le profil est créé par le serveur au premier appel authentifié, puis complété ici.
 */
public class AuthRepository {

    private final FirebaseAuth auth;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
    }

    private PostgresBackendApi backend() {
        return BackendProvider.get();
    }

    // ---------------------------------------------------------------- connexion

    /** Connexion par email / mot de passe, puis chargement du profil. */
    public void signInWithEmail(String email, String password, OnProfileLoadedListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> loadMyProfile(listener))
                .addOnFailureListener(e -> listener.onError(friendlyAuthError(e)));
    }

    /** Création d'un compte email / mot de passe ; le nom saisi est enregistré dans le profil. */
    public void createAccount(String name, String email, String password, OnProfileLoadedListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Map<String, String> fields = new HashMap<>();
                    fields.put("name", name);
                    backend().updateMyProfile(fields, profileCallback(listener));
                })
                .addOnFailureListener(e -> listener.onError(friendlyAuthError(e)));
    }

    /** Connexion avec le jeton d'identité Google obtenu via Credential Manager. */
    public void signInWithGoogleIdToken(String idToken, OnProfileLoadedListener listener) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> loadMyProfile(listener))
                .addOnFailureListener(e -> listener.onError(friendlyAuthError(e)));
    }

    public void signOut() {
        auth.signOut();
    }

    // ------------------------------------------------------------------- profil

    /** Charge mon profil (le serveur le crée à la première connexion, avec le nom et la photo du compte). */
    public void loadMyProfile(OnProfileLoadedListener listener) {
        backend().getMyProfile(profileCallback(listener));
    }

    /** Met à jour WhatsApp, adresse et téléphone. Le serveur valide et normalise les numéros. */
    public void updateFields(String whatsapp, String address, String phone, AuthCallback callback) {
        Map<String, String> fields = new HashMap<>();
        fields.put("whatsapp", whatsapp);
        fields.put("address", address);
        fields.put("phoneNumber", phone);

        backend().updateMyProfile(fields, new PostgresBackendApi.ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess();
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private PostgresBackendApi.ApiCallback<User> profileCallback(OnProfileLoadedListener listener) {
        return new PostgresBackendApi.ApiCallback<User>() {
            @Override
            public void onSuccess(User user) {
                listener.onSuccess(user);
            }

            @Override
            public void onError(String errorMessage) {
                listener.onError(errorMessage);
            }
        };
    }

    // ------------------------------------------------------------------ erreurs

    private String friendlyAuthError(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return "Pas de connexion internet. Vérifiez votre réseau.";
        }
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if (code != null) {
                switch (code) {
                    case "ERROR_INVALID_EMAIL":
                        return "Adresse email invalide.";
                    case "ERROR_WRONG_PASSWORD":
                    case "ERROR_INVALID_CREDENTIAL":
                    case "ERROR_USER_NOT_FOUND":
                        return "Email ou mot de passe incorrect.";
                    case "ERROR_EMAIL_ALREADY_IN_USE":
                        return "Un compte existe déjà avec cet email.";
                    case "ERROR_WEAK_PASSWORD":
                        return "Mot de passe trop faible (6 caractères minimum).";
                    case "ERROR_USER_DISABLED":
                        return "Ce compte est désactivé.";
                    case "ERROR_TOO_MANY_REQUESTS":
                        return "Trop de tentatives. Réessayez plus tard.";
                    default:
                        break;
                }
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Une erreur est survenue.";
    }

    // --------------------------------------------------------------- callbacks

    public interface AuthCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface OnProfileLoadedListener {
        void onSuccess(User user);
        void onError(String message);
    }
}
