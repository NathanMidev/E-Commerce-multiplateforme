package com.marketplace.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Panier d'un utilisateur (réponse de GET /cart) : un seul panier par compte. */
public class Cart implements Serializable {
    private List<CartItem> items = new ArrayList<>();
    private int itemCount;   // nombre total d'articles (somme des quantités)
    private double total;

    public Cart() {}

    public Cart(List<CartItem> items) {
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

    /** Jamais null, même si le serveur omet le champ. */
    public List<CartItem> getItems() { return items != null ? items : new ArrayList<>(); }
    public int getItemCount() { return itemCount; }
    public double getTotal() { return total; }
    public boolean isEmpty() { return getItems().isEmpty(); }

    /** Vrai si tous les articles peuvent être commandés dans la quantité demandée. */
    public boolean isCheckoutPossible() {
        if (isEmpty()) return false;
        for (CartItem item : getItems()) {
            if (!item.isAvailable()) return false;
        }
        return true;
    }
}
