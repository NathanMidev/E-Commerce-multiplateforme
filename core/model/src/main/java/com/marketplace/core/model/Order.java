package com.marketplace.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande : le passage en caisse crée UNE commande par vendeur.
 * Les dates sont en millisecondes (comme Product.createdAt) ; null tant que l'étape n'est pas atteinte.
 */
public class Order implements Serializable {
    private String id;
    private long orderNumber;
    private String reference;          // ex : CMD-000123
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private String deliveryAddress;
    private String deliveryPhone;
    private String note;
    private double total;
    private long createdAt;
    private long updatedAt;
    private Long confirmedAt;
    private Long shippedAt;
    private Long deliveredAt;
    private Long cancelledAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(String id, long orderNumber, String buyerId, String buyerName, String sellerId, String sellerName,
                 PaymentMethod paymentMethod, String deliveryAddress, String deliveryPhone, String note,
                 List<OrderItem> items) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.reference = String.format("CMD-%06d", orderNumber);
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.status = OrderStatus.EN_ATTENTE;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.A_LA_LIVRAISON;
        this.deliveryAddress = deliveryAddress;
        this.deliveryPhone = deliveryPhone;
        this.note = note;
        this.items = items != null ? items : new ArrayList<>();
        long cents = 0;
        for (OrderItem item : this.items) cents += Math.round(item.getLineTotal() * 100);
        this.total = cents / 100.0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public long getOrderNumber() { return orderNumber; }
    public String getReference() { return reference; }
    public String getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public String getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public OrderStatus getStatus() { return status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getDeliveryPhone() { return deliveryPhone; }
    public String getNote() { return note; }
    public double getTotal() { return total; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public Long getConfirmedAt() { return confirmedAt; }
    public Long getShippedAt() { return shippedAt; }
    public Long getDeliveredAt() { return deliveredAt; }
    public Long getCancelledAt() { return cancelledAt; }
    public List<OrderItem> getItems() { return items != null ? items : new ArrayList<>(); }

    /** Utilisé par le mock hors-ligne ; le vrai serveur renvoie la commande mise à jour. */
    public void applyStatus(OrderStatus newStatus) {
        this.status = newStatus;
        long now = System.currentTimeMillis();
        this.updatedAt = now;
        switch (newStatus) {
            case CONFIRMEE: this.confirmedAt = now; break;
            case EXPEDIEE: this.shippedAt = now; break;
            case LIVREE: this.deliveredAt = now; break;
            case ANNULEE: this.cancelledAt = now; break;
            default: break;
        }
    }

    // --- Actions proposées à l'interface selon le rôle (le serveur reste l'arbitre final) ---

    /** Le vendeur peut confirmer une commande en attente. */
    public boolean canSellerConfirm() { return status == OrderStatus.EN_ATTENTE; }

    /** Le vendeur peut expédier une commande confirmée. */
    public boolean canSellerShip() { return status == OrderStatus.CONFIRMEE; }

    /** Le vendeur peut annuler tant que la commande n'est pas expédiée. */
    public boolean canSellerCancel() { return status == OrderStatus.EN_ATTENTE || status == OrderStatus.CONFIRMEE; }

    /** L'acheteur peut annuler uniquement tant que le vendeur n'a pas confirmé. */
    public boolean canBuyerCancel() { return status == OrderStatus.EN_ATTENTE; }

    /** L'acheteur confirme la réception d'une commande expédiée. */
    public boolean canBuyerConfirmDelivery() { return status == OrderStatus.EXPEDIEE; }
}
