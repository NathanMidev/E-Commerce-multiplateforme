package com.marketplace.app.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * Rôle : Gestion standardisée des états de chargement, état vide et erreur avec bouton Réessayer.
 * Entrée : Méthodes showLoading(), showEmpty(msg), showError(msg, onRetry), showContent().
 * Sortie : Affichage uniforme dans toutes les vues sans boilerplate.
 */
public class StateLayout extends LinearLayout {

    private ProgressBar progressBar;
    private TextView tvMessage;
    private Button btnRetry;

    public StateLayout(Context context) {
        super(context);
        init(context);
    }

    public StateLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(32, 48, 32, 48);

        progressBar = new ProgressBar(context);
        tvMessage = new TextView(context);
        tvMessage.setPadding(0, 24, 0, 24);
        tvMessage.setGravity(Gravity.CENTER);

        btnRetry = new Button(context);
        btnRetry.setText("Réessayer");

        addView(progressBar);
        addView(tvMessage);
        addView(btnRetry);

        showContent();
    }

    public void showLoading(String message) {
        setVisibility(VISIBLE);
        progressBar.setVisibility(VISIBLE);
        tvMessage.setVisibility(VISIBLE);
        tvMessage.setText(message != null ? message : "Chargement en cours...");
        btnRetry.setVisibility(GONE);
    }

    public void showEmpty(String message) {
        setVisibility(VISIBLE);
        progressBar.setVisibility(GONE);
        tvMessage.setVisibility(VISIBLE);
        tvMessage.setText(message != null ? message : "Aucun élément trouvé.");
        btnRetry.setVisibility(GONE);
    }

    public void showError(String message, final Runnable onRetry) {
        setVisibility(VISIBLE);
        progressBar.setVisibility(GONE);
        tvMessage.setVisibility(VISIBLE);
        tvMessage.setText(message != null ? message : "Une erreur est survenue.");
        if (onRetry != null) {
            btnRetry.setVisibility(VISIBLE);
            btnRetry.setOnClickListener(v -> onRetry.run());
        } else {
            btnRetry.setVisibility(GONE);
        }
    }

    public void showContent() {
        setVisibility(GONE);
    }
}
