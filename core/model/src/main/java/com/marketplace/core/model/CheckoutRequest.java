package com.marketplace.core.model;

import java.io.Serializable;

/**
 * Corps de POST /orders/checkout. Si l'adresse ou le téléphone de livraison sont omis (null),
 * le serveur utilise ceux du profil de l'acheteur.
 */
public class CheckoutRequest implements Serializable {
    private String deliveryAddress;
    private String deliveryPhone;
    private PaymentMethod paymentMethod;
    private String note;

    public CheckoutRequest() {
        this.paymentMethod = PaymentMethod.A_LA_LIVRAISON;
    }

    public CheckoutRequest(String deliveryAddress, String deliveryPhone, PaymentMethod paymentMethod, String note) {
        this.deliveryAddress = deliveryAddress;
        this.deliveryPhone = deliveryPhone;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.A_LA_LIVRAISON;
        this.note = note;
    }

    public String getDeliveryAddress() { return deliveryAddress; }
    public String getDeliveryPhone() { return deliveryPhone; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getNote() { return note; }
}
