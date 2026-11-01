package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Ligne d'un article dans le panier de l'utilisateur.
 * Entrée : Champs JSON retournés par /api/cart.
 * Sortie : Calcul du montant de la ligne et validation de disponibilité.
 */
public class CartItem extends BaseModel {
    private String productId;
    private String title;
    private String thumbnailUrl;
    private double unitPrice;
    private int quantity;
    private int stock;
    private boolean available;
    private String sellerId;
    private String sellerName;
    private double lineTotal;

    public CartItem() {
        super();
    }

    public CartItem(String productId, String title, String thumbnailUrl, double unitPrice,
                    int quantity, int stock, String sellerId, String sellerName) {
        super(productId);
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

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (productId == null || productId.trim().isEmpty()) {
            errors.add("L'identifiant du produit est obligatoire");
        }
        if (quantity <= 0) {
            errors.add("La quantité doit être supérieure à zéro");
        }
        return errors;
    }

    public String getProductId() { return productId != null ? productId : getId(); }
    public void setProductId(String productId) {
        this.productId = productId;
        setId(productId);
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.lineTotal = Math.round(unitPrice * 100) * quantity / 100.0;
        this.available = stock > 0 && stock >= quantity;
    }

    public int getStock() { return stock; }
    public void setStock(int stock) {
        this.stock = stock;
        this.available = stock > 0 && stock >= quantity;
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}
