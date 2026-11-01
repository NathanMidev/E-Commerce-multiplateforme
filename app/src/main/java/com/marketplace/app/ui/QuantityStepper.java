package com.marketplace.app.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * Rôle : Sélecteur de quantité (+ / -) avec respect du stock disponible.
 * Entrée : Quantité initiale et stock max.
 * Sortie : Callback OnQuantityChangeListener.
 */
public class QuantityStepper extends LinearLayout {

    private int quantity = 1;
    private int maxStock = 999;
    private TextView tvQuantity;
    private Button btnMinus;
    private Button btnPlus;
    private OnQuantityChangeListener listener;

    public interface OnQuantityChangeListener {
        void onQuantityChanged(int newQuantity);
    }

    public QuantityStepper(Context context) {
        super(context);
        init(context);
    }

    public QuantityStepper(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        btnMinus = new Button(context, null, android.R.attr.buttonStyleSmall);
        btnMinus.setText("-");
        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateUi();
                if (listener != null) listener.onQuantityChanged(quantity);
            }
        });

        tvQuantity = new TextView(context);
        tvQuantity.setText(String.valueOf(quantity));
        tvQuantity.setPadding(24, 0, 24, 0);
        tvQuantity.setGravity(Gravity.CENTER);

        btnPlus = new Button(context, null, android.R.attr.buttonStyleSmall);
        btnPlus.setText("+");
        btnPlus.setOnClickListener(v -> {
            if (quantity < maxStock) {
                quantity++;
                updateUi();
                if (listener != null) listener.onQuantityChanged(quantity);
            }
        });

        addView(btnMinus);
        addView(tvQuantity);
        addView(btnPlus);
        updateUi();
    }

    public void setQuantity(int q) {
        this.quantity = Math.max(1, Math.min(q, maxStock));
        updateUi();
    }

    public void setMaxStock(int stock) {
        this.maxStock = Math.max(1, stock);
        if (quantity > maxStock) {
            quantity = maxStock;
        }
        updateUi();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener l) {
        this.listener = l;
    }

    private void updateUi() {
        if (tvQuantity != null) {
            tvQuantity.setText(String.valueOf(quantity));
        }
        if (btnMinus != null) {
            btnMinus.setEnabled(quantity > 1);
        }
        if (btnPlus != null) {
            btnPlus.setEnabled(quantity < maxStock);
        }
    }
}
