package com.marketplace.app.backend;

import com.marketplace.app.model.User;

/**
 * Rôle : Gestion unifiée de la session utilisateur connectée.
 * Entrée : ID utilisateur et token JWT (Firebase ou local).
 * Sortie : Source unique de vérité pour Session.currentUserId() et Session.isLoggedIn().
 */
public class AuthSession {
    private static AuthSession instance;

    private String userId;
    private String idToken;
    private User currentUser;

    public AuthSession() {}

    public static synchronized AuthSession getInstance() {
        if (instance == null) {
            instance = new AuthSession();
        }
        return instance;
    }

    public synchronized void updateSession(String userId, String idToken) {
        this.userId = userId;
        this.idToken = idToken;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public synchronized void clear() {
        this.userId = null;
        this.idToken = null;
        this.currentUser = null;
    }

    public synchronized boolean isLoggedIn() {
        return userId != null && !userId.trim().isEmpty();
    }

    public synchronized String getUserId() {
        return userId;
    }

    public synchronized String getIdToken() {
        return idToken;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }
}
