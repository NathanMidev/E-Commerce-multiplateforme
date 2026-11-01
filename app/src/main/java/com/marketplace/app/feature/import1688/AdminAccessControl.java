package com.marketplace.app.feature.import1688;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.model.User;

import java.util.Map;

/**
 * Rôle : Contrôle d'accès réel aux opérations administratives Import 1688.
 * Entrée : Session authentifiée et claims du jeton Firebase (ou jeton admin vérifié).
 * Sortie : Autorisation explicite ou rejet de sécurité (jamais basé sur la simple ouverture d'écran).
 */
public final class AdminAccessControl {

    private static boolean adminClaimVerified = false;
    private static String verifiedAdminUid = null;

    private AdminAccessControl() {}

    /**
     * Met à jour l'état d'administrateur à partir des custom claims Firebase (claim "admin": true ou "role": "ADMIN").
     */
    public static synchronized void refreshFromFirebaseClaims(GetTokenResult tokenResult, String uid) {
        if (tokenResult == null || uid == null) {
            adminClaimVerified = false;
            verifiedAdminUid = null;
            return;
        }
        Map<String, Object> claims = tokenResult.getClaims();
        boolean isAdmin = false;
        if (claims != null) {
            Object adminObj = claims.get("admin");
            if (Boolean.TRUE.equals(adminObj)) {
                isAdmin = true;
            }
            Object roleObj = claims.get("role");
            if ("ADMIN".equalsIgnoreCase(String.valueOf(roleObj))) {
                isAdmin = true;
            }
        }
        adminClaimVerified = isAdmin;
        verifiedAdminUid = isAdmin ? uid : null;
    }

    /**
     * Permet aux tests unitaires et au mode démo explicitement identifié d'accorder un jeton admin pour un UID précis.
     */
    public static synchronized void grantAdminForUid(String uid, boolean granted) {
        if (granted && uid != null && !uid.trim().isEmpty()) {
            adminClaimVerified = true;
            verifiedAdminUid = uid;
        } else {
            adminClaimVerified = false;
            verifiedAdminUid = null;
        }
    }

    /**
     * Vérifie si l'utilisateur actuellement authentifié dispose réellement des droits administrateur.
     */
    public static synchronized boolean isCurrentUserAdmin() {
        AuthSession session = AuthSession.getInstance();
        if (session == null || !session.isLoggedIn()) {
            return false;
        }
        String currentUid = session.getUserId();
        if (currentUid == null || currentUid.trim().isEmpty()) {
            return false;
        }
        if (adminClaimVerified && currentUid.equals(verifiedAdminUid)) {
            return true;
        }
        User user = session.getCurrentUser();
        if (user != null && "ADMIN".equalsIgnoreCase(user.getLevel())) {
            return true;
        }
        return false;
    }

    /**
     * Retourne l'identifiant de l'administrateur authentifié ou lève SecurityException si non autorisé.
     */
    public static synchronized String requireAdminUserId() throws SecurityException {
        if (!isCurrentUserAdmin()) {
            throw new SecurityException("Accès refusé : droits administrateur requis (403)");
        }
        return AuthSession.getInstance().getUserId();
    }

    /**
     * Vérifie de manière asynchrone les custom claims Firebase de l'utilisateur courant.
     */
    public static void verifyAdminClaimAsync(final AdminCheckCallback callback) {
        if (isCurrentUserAdmin()) {
            if (callback != null) callback.onResult(true);
            return;
        }
        try {
            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fbUser == null) {
                if (callback != null) callback.onResult(false);
                return;
            }
            fbUser.getIdToken(false)
                    .addOnSuccessListener(result -> {
                        refreshFromFirebaseClaims(result, fbUser.getUid());
                        if (callback != null) callback.onResult(isCurrentUserAdmin());
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onResult(false);
                    });
        } catch (Throwable ignored) {
            if (callback != null) callback.onResult(isCurrentUserAdmin());
        }
    }

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
    }
}
