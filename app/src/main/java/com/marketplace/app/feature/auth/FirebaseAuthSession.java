package com.marketplace.app.feature.auth;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.marketplace.app.backend.AuthSession;

/**
 * Rôle : Synchroniseur entre Firebase Auth et le singleton AuthSession de l'application.
 * Entrée : Événements Firebase Auth (connexion, déconnexion, jeton ID).
 * Sortie : Mise à jour en continu de AuthSession.getInstance().
 */
public final class FirebaseAuthSession {

    private FirebaseAuthSession() {}

    /**
     * Initialise l'écouteur d'état Firebase Auth.
     */
    public static void initialize() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        syncSession(auth.getCurrentUser());

        auth.addAuthStateListener(firebaseAuth -> syncSession(firebaseAuth.getCurrentUser()));
    }

    private static void syncSession(FirebaseUser user) {
        if (user != null) {
            String uid = user.getUid();
            // Le jeton est rafraîchi ou lu à la demande par OkHttp
            AuthSession.getInstance().updateSession(uid, null);
        } else {
            AuthSession.getInstance().clear();
        }
    }

    /**
     * Récupère le jeton JWT sur un thread d'arrière-plan (OkHttp interceptor).
     */
    public static String fetchFreshToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        try {
            GetTokenResult result = Tasks.await(user.getIdToken(false));
            String token = result.getToken();
            AuthSession.getInstance().updateSession(user.getUid(), token);
            return token;
        } catch (Exception e) {
            return null;
        }
    }
}
