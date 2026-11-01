package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Commande d'achat / vente passée sur la marketplace.
 * Entrée : Champs JSON retournés par /api/orders.
 * Sortie : Calculs d'autorisation par rôle et gestion des transitions de statut.
 */
public class Order extends BaseModel {
    private long orderNumber;
    private String reference;
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

    public Order() {
        super();
        this.status = OrderStatus.EN_ATTENTE;
        this.paymentMethod = PaymentMethod.A_LA_LIVRAISON;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    public Order(String id, long orderNumber, String buyerId, String buyerName, String sellerId, String sellerName,
                 PaymentMethod paymentMethod, String deliveryAddress, String deliveryPhone, String note,
                 List<OrderItem> items) {
        super(id);
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

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            errors.add("L'adresse de livraison est obligatoire");
        }
        if (deliveryPhone == null || deliveryPhone.trim().isEmpty()) {
            errors.add("Le téléphone de livraison est obligatoire");
        }
        return errors;
    }

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

    public boolean canSellerConfirm() { return status == OrderStatus.EN_ATTENTE; }
    public boolean canSellerShip() { return status == OrderStatus.CONFIRMEE; }
    public boolean canSellerCancel() { return status == OrderStatus.EN_ATTENTE || status == OrderStatus.CONFIRMEE; }
    public boolean canBuyerCancel() { return status == OrderStatus.EN_ATTENTE; }
    public boolean canBuyerConfirmDelivery() { return status == OrderStatus.EXPEDIEE; }

    public long getOrderNumber() { return orderNumber; }
    public void setOrderNumber(long orderNumber) { this.orderNumber = orderNumber; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getDeliveryPhone() { return deliveryPhone; }
    public void setDeliveryPhone(String deliveryPhone) { this.deliveryPhone = deliveryPhone; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Long getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Long confirmedAt) { this.confirmedAt = confirmedAt; }

    public Long getShippedAt() { return shippedAt; }
    public void setShippedAt(Long shippedAt) { this.shippedAt = shippedAt; }

    public Long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Long deliveredAt) { this.deliveredAt = deliveredAt; }

    public Long getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Long cancelledAt) { this.cancelledAt = cancelledAt; }

    public List<OrderItem> getItems() { return items != null ? items : new ArrayList<>(); }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
