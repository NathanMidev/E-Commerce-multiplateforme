package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Ligne d'un article dans une commande passée.
 * Entrée : Champs JSON figés lors de l'enregistrement de la commande.
 * Sortie : Modèle sérialisable pour l'historique d'achat ou vente.
 */
public class OrderItem extends BaseModel {
    private String productId;
    private String productTitle;
    private String thumbnailUrl;
    private double unitPrice;
    private int quantity;
    private double lineTotal;

    public OrderItem() {
        super();
    }

    public OrderItem(String productId, String productTitle, String thumbnailUrl, double unitPrice, int quantity) {
        super(productId);
        this.productId = productId;
        this.productTitle = productTitle;
        this.thumbnailUrl = thumbnailUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = Math.round(unitPrice * 100) * quantity / 100.0;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (productTitle == null || productTitle.trim().isEmpty()) {
            errors.add("Le titre du produit commandé est obligatoire");
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

    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.lineTotal = Math.round(unitPrice * 100) * quantity / 100.0;
    }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}
