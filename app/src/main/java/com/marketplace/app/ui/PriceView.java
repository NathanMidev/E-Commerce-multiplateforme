package com.marketplace.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Rôle : Affichage standardisé des prix en orange vif avec typographie commerciale 1688.
 * Entrée : Montant double.
 * Sortie : Texte formaté et mis en avant.
 */
public class PriceView extends AppCompatTextView {

    public PriceView(@NonNull Context context) {
        super(context);
        init();
    }

    public PriceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setTextColor(Color.parseColor("#FF4400"));
        setTypeface(getTypeface(), Typeface.BOLD);
    }

    public void setPrice(double price) {
        setText(PriceFormatter.format(price));
    }
}
