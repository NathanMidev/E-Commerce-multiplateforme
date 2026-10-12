package com.marketplace.feature.auth.router;

import android.content.Context;
import android.content.Intent;
import com.marketplace.core.navigation.AuthRouter;
import com.marketplace.feature.auth.ui.LoginActivity;
import com.marketplace.feature.auth.ui.RegisterActivity;
import com.marketplace.feature.auth.ui.UserProfileActivity;

/**
 * Routeur interne du module Authentification.
 * Implémente le contrat AuthRouter défini dans :core:navigation.
 */
public class AuthModuleRouter implements AuthRouter {

    @Override
    public void openLogin(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void openRegister(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, RegisterActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void openProfile(Context context, String userId) {
        if (context == null) return;
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra("EXTRA_USER_ID", userId);
        context.startActivity(intent);
    }
}
