package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Panier d'achat global de l'utilisateur.
 * Entrée : Réponse de GET /api/cart.
 * Sortie : Calcul du nombre total d'articles et du montant global.
 */
public class Cart extends BaseModel {
    private List<CartItem> items = new ArrayList<>();
    private int itemCount;
    private double total;

    public Cart() {
        super();
    }

    public Cart(List<CartItem> items) {
        super();
        this.items = items != null ? items : new ArrayList<>();
        int count = 0;
        long totalCents = 0;
        for (CartItem item : this.items) {
            count += item.getQuantity();
            totalCents += Math.round(item.getLineTotal() * 100);
        }
        this.itemCount = count;
        this.total = totalCents / 100.0;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            errors.add("Le panier est vide");
        }
        return errors;
    }

    public boolean isEmpty() {
        return getItems().isEmpty();
    }

    public boolean isCheckoutPossible() {
        if (isEmpty()) return false;
        for (CartItem item : getItems()) {
            if (!item.isAvailable()) return false;
        }
        return true;
    }

    public List<CartItem> getItems() {
        return items != null ? items : new ArrayList<>();
    }
    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
