package com.marketplace.core.model;

import java.io.Serializable;

/** Une ligne du panier (réponse de GET /cart). */
public class CartItem implements Serializable {
    private String productId;
    private String title;
    private String thumbnailUrl;
    private double unitPrice;
    private int quantity;
    private int stock;           // stock actuel du produit
    private boolean available;   // faux si le produit est épuisé ou si le stock est inférieur à la quantité
    private String sellerId;
    private String sellerName;
    private double lineTotal;

    public CartItem() {}

    public CartItem(String productId, String title, String thumbnailUrl, double unitPrice,
                    int quantity, int stock, String sellerId, String sellerName) {
        this.productId = productId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.stock = stock;
        this.available = stock > 0 && stock >= quantity;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.lineTotal = Math.round(unitPrice * 100) * quantity / 100.0;
    }

    public String getProductId() { return productId; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public double getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public int getStock() { return stock; }
    public boolean isAvailable() { return available; }
    public String getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public double getLineTotal() { return lineTotal; }
}
