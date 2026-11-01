package com.marketplace.app.transaction.paiement;

import com.marketplace.app.model.Payment;
import com.marketplace.app.model.PaymentMethod;
import com.marketplace.app.model.PaymentStatus;

import java.util.UUID;

/**
 * Rôle : Fournisseur de paiement simulé et déclaratif pour démonstrations et soutenances.
 * Entrée : Informations de paiement.
 * Sortie : Validation immédiate avec statut EN_ATTENTE de confirmation par le vendeur.
 */
public class SimulatedPaymentProvider implements PaymentProvider {

    @Override
    public void processPayment(String orderId, double amount, PaymentMethod method, PaymentCallback callback) {
        if (callback == null) return;

        Payment payment = new Payment(
                "sim_pay_" + UUID.randomUUID().toString().substring(0, 8),
                orderId,
                method,
                PaymentStatus.EN_ATTENTE,
                amount,
                "SIM-REF-" + System.currentTimeMillis()
        );
        callback.onSuccess(payment);
    }
}
