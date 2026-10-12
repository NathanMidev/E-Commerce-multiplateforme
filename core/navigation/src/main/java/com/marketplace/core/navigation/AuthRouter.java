package com.marketplace.core.navigation;

import android.content.Context;

/**
 * Contrat de routage pour le module Authentification & Profil (feature:auth).
 */
public interface AuthRouter {
    void openLogin(Context context);
    void openRegister(Context context);
    void openProfile(Context context, String userId);
}
