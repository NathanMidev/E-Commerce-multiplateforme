package com.marketplace.core.model;

import java.io.Serializable;

/** Ligne de commande : titre et prix sont figés au moment de l'achat. */
public class OrderItem implements Serializable {
    private String productId;     // null si le vendeur a supprimé le produit depuis
    private String productTitle;
    private String thumbnailUrl;
    private double unitPrice;
    private int quantity;
    private double lineTotal;

    public OrderItem() {}

    public OrderItem(String productId, String productTitle, String thumbnailUrl, double unitPrice, int quantity) {
        this.productId = productId;
        this.productTitle = productTitle;
        this.thumbnailUrl = thumbnailUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = Math.round(unitPrice * 100) * quantity / 100.0;
    }

    public String getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public double getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public double getLineTotal() { return lineTotal; }
}
