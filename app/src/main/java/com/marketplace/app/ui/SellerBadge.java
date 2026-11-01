package com.marketplace.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.marketplace.app.R;
import com.marketplace.app.model.UserRole;

/**
 * Rôle : Badge visuel indiquant le niveau et la fiabilité du vendeur (BUYER, SELLER, PRO).
 * Entrée : UserRole (résolu selon les ventes effectives et la réactivité).
 * Sortie : Étiquette colorée stylisée.
 */
public class SellerBadge extends AppCompatTextView {

    public SellerBadge(@NonNull Context context) {
        super(context);
        init();
    }

    public SellerBadge(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setGravity(Gravity.CENTER);
        setTextSize(10f);
        setPadding(12, 4, 12, 4);
    }

    public void setRole(UserRole role) {
        if (role == null) {
            setVisibility(GONE);
            return;
        }

        setVisibility(VISIBLE);
        switch (role) {
            case PRO:
                setText("⭐ VENDEUR PRO");
                setBackgroundColor(Color.parseColor("#FFF8E6"));
                setTextColor(Color.parseColor("#B87400"));
                break;
            case SELLER:
                setText("✓ VENDEUR VÉRIFIÉ");
                setBackgroundColor(Color.parseColor("#E6F7FF"));
                setTextColor(Color.parseColor("#0070BA"));
                break;
            case BUYER:
            default:
                setText("MEMBRE");
                setBackgroundColor(Color.parseColor("#F5F5F5"));
                setTextColor(Color.parseColor("#757575"));
                break;
        }
    }
}
