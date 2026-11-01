package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Informations de paiement déclaratif associé à une commande.
 * Entrée : Champs déclaratifs issus de la commande ou de la mise à jour vendeur.
 * Sortie : Modèle sérialisable et validation de statut de paiement.
 */
public class Payment extends BaseModel {
    private String orderId;
    private PaymentMethod method;
    private PaymentStatus status;
    private double amount;
    private String transactionRef;
    private long updatedAt;

    public Payment() {
        super();
        this.status = PaymentStatus.EN_ATTENTE;
        this.method = PaymentMethod.A_LA_LIVRAISON;
        this.updatedAt = System.currentTimeMillis();
    }

    public Payment(String id, String orderId, PaymentMethod method, PaymentStatus status, double amount, String transactionRef) {
        super(id);
        this.orderId = orderId;
        this.method = method != null ? method : PaymentMethod.A_LA_LIVRAISON;
        this.status = status != null ? status : PaymentStatus.EN_ATTENTE;
        this.amount = amount;
        this.transactionRef = transactionRef;
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (orderId == null || orderId.trim().isEmpty()) {
            errors.add("L'identifiant de la commande est obligatoire");
        }
        if (amount < 0) {
            errors.add("Le montant ne peut pas être négatif");
        }
        return errors;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
