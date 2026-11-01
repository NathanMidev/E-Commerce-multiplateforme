package com.marketplace.app.feature.auth;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.feature.import1688.AdminAccessControl;

/**
 * Rôle : Synchroniseur entre Firebase Auth et le singleton AuthSession de l'application.
 * Entrée : Événements Firebase Auth (connexion, déconnexion, jeton ID et custom claims).
 * Sortie : Mise à jour en continu de AuthSession.getInstance() et AdminAccessControl.
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

    public static void signOut() {
        FirebaseAuth.getInstance().signOut();
        AuthSession.getInstance().clear();
        AdminAccessControl.grantAdminForUid(null, false);
    }

    private static void syncSession(FirebaseUser user) {
        if (user != null) {
            final String uid = user.getUid();
            AuthSession.getInstance().updateSession(uid, null);
            user.getIdToken(false).addOnSuccessListener(result -> {
                if (result != null) {
                    AuthSession.getInstance().updateSession(uid, result.getToken());
                    AdminAccessControl.refreshFromFirebaseClaims(result, uid);
                }
            });
        } else {
            AuthSession.getInstance().clear();
            AdminAccessControl.grantAdminForUid(null, false);
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
            AdminAccessControl.refreshFromFirebaseClaims(result, user.getUid());
            return token;
        } catch (Exception e) {
            return null;
        }
    }
}
