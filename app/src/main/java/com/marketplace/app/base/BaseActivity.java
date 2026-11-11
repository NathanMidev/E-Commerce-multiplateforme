package com.marketplace.app.base;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.navigation.Router;

/**
 * Rôle : Classe de base pour toutes les activités de l'application.
 * Entrée : Cycle de vie Android et gestion d'état UI.
 * Sortie : Ordre d'exécution strict (bindViews -> loadData -> render) et utilitaires unifiés.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutId = getLayoutId();
        if (layoutId != 0) {
            setContentView(layoutId);
        }
        bindViews();
        loadData();
    }

    @LayoutRes
    protected abstract int getLayoutId();

    protected abstract void bindViews();

    protected abstract void loadData();

    protected abstract void render(Object data);

    protected void onError(String message) {
        showError(message, this::loadData);
    }

    protected void showLoading(boolean loading) {
        // Implémentation optionnelle selon les vues de l'écran
    }

    protected void showEmpty(boolean empty, String message) {
        // Implémentation optionnelle selon les vues de l'écran
    }

    protected void showError(String message, @Nullable Runnable onRetry) {
        toast(message != null ? message : "Une erreur est survenue");
    }

    protected void toast(String message) {
        if (message != null && !isFinishing()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Vérifie si l'utilisateur est connecté, sinon redirige vers la connexion.
     */
    protected boolean requireLogin() {
        return requireLogin(null);
    }

    protected boolean requireLogin(@Nullable Runnable onLoggedIn) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            Router.openLogin(this);
            return false;
        }
        if (onLoggedIn != null) {
            onLoggedIn.run();
        }
        return true;
    }
}
