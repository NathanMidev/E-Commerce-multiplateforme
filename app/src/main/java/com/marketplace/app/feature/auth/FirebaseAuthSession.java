package com.marketplace.app.feature.auth;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.feature.import1688.AdminAccessControl;
import com.marketplace.app.model.User;

/**
 * Rôle : Synchroniseur sécurisé entre Firebase Authentication et le singleton AuthSession de l'application.
 * Entrée : Événements Firebase Auth (connexion, déconnexion, renouvellement de l'ID token et custom claims).
 * Sortie : Mise à jour cohérente de AuthSession.getInstance(), cohérence displayName/photoURL/email et AdminAccessControl.
 */
public final class FirebaseAuthSession {

    private static boolean listenerRegistered = false;

    private FirebaseAuthSession() {}

    /**
     * Initialise l'écouteur d'état Firebase Auth (une seule fois par processus).
     */
    public static synchronized void initialize() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            syncSession(auth.getCurrentUser());
            if (!listenerRegistered) {
                auth.addAuthStateListener(firebaseAuth -> syncSession(firebaseAuth.getCurrentUser()));
                listenerRegistered = true;
            }
        } catch (Throwable ignored) {
        }
    }

    public static void signOut() {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Throwable ignored) {
        }
        AuthSession.getInstance().clear();
        AdminAccessControl.grantAdminForUid(null, false);
    }

    private static void syncSession(FirebaseUser user) {
        if (user != null) {
            final String uid = user.getUid();
            String existingToken = AuthSession.getInstance().getIdToken();
            AuthSession.getInstance().updateSession(uid, existingToken);
            mergeFirebaseIdentityIntoSessionUser(user);

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
     * S'assure que le UID et l'ID Token Firebase sont prêts dans AuthSession avant d'appeler le backend REST.
     */
    public static void ensureAuthenticatedSessionReady(Runnable onReady) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                if (onReady != null) onReady.run();
                return;
            }
            final String uid = user.getUid();
            AuthSession.getInstance().updateSession(uid, AuthSession.getInstance().getIdToken());
            mergeFirebaseIdentityIntoSessionUser(user);

            user.getIdToken(false)
                    .addOnSuccessListener(result -> {
                        if (result != null) {
                            AuthSession.getInstance().updateSession(uid, result.getToken());
                            AdminAccessControl.refreshFromFirebaseClaims(result, uid);
                        }
                        if (onReady != null) onReady.run();
                    })
                    .addOnFailureListener(e -> {
                        if (onReady != null) onReady.run();
                    });
        } catch (Throwable t) {
            if (onReady != null) onReady.run();
        }
    }

    /**
     * Enrichit un objet User applicatif avec les informations de FirebaseUser (uid, displayName, email, photoUrl)
     * SANS jamais écraser une donnée métier déjà renseignée dans le profil applicatif (Section 6).
     */
    public static void mergeFirebaseIdentityIntoUser(FirebaseUser fbUser, User targetUser) {
        if (fbUser == null || targetUser == null) return;
        if (targetUser.getId() == null || targetUser.getId().trim().isEmpty()) {
            targetUser.setId(fbUser.getUid());
        }
        if ((targetUser.getName() == null || targetUser.getName().trim().isEmpty()
                || "Utilisateur".equalsIgnoreCase(targetUser.getName().trim()))
                && fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
            targetUser.setName(fbUser.getDisplayName().trim());
        }
        if ((targetUser.getEmail() == null || targetUser.getEmail().trim().isEmpty())
                && fbUser.getEmail() != null && !fbUser.getEmail().trim().isEmpty()) {
            targetUser.setEmail(fbUser.getEmail().trim());
        }
        if (!targetUser.hasCustomAvatar() && fbUser.getPhotoUrl() != null) {
            String fbPhoto = fbUser.getPhotoUrl().toString();
            if (!fbPhoto.trim().isEmpty()) {
                targetUser.setAvatarUrl(fbPhoto);
            }
        }
    }

    private static void mergeFirebaseIdentityIntoSessionUser(FirebaseUser fbUser) {
        if (fbUser == null) return;
        User current = AuthSession.getInstance().getCurrentUser();
        if (current != null) {
            mergeFirebaseIdentityIntoUser(fbUser, current);
        }
    }

    /**
     * Récupère le jeton JWT sur un thread d'arrière-plan (OkHttp interceptor).
     */
    public static String fetchFreshToken() {
        return fetchFreshToken(false);
    }

    /**
     * Récupère ou renouvelle (si forceRefresh == true) le jeton ID Firebase pour communiquer avec le backend.
     */
    public static String fetchFreshToken(boolean forceRefresh) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return null;
            GetTokenResult result = Tasks.await(user.getIdToken(forceRefresh));
            String token = result.getToken();
            AuthSession.getInstance().updateSession(user.getUid(), token);
            AdminAccessControl.refreshFromFirebaseClaims(result, user.getUid());
            return token;
        } catch (Throwable e) {
            return null;
        }
    }
}
