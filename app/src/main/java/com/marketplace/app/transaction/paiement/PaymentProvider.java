package com.marketplace.app.transaction.paiement;

import com.marketplace.app.model.Payment;
import com.marketplace.app.model.PaymentMethod;
import com.marketplace.app.model.PaymentStatus;

/**
 * Rôle : Contrat d'interface pour les fournisseurs de paiement.
 * Entrée : Mode de paiement, montant et référence de commande.
 * Sortie : Validation déclarative ou perspective d'intégration API marchand (MVola, Orange Money).
 */
public interface PaymentProvider {
    void processPayment(String orderId, double amount, PaymentMethod method, PaymentCallback callback);

    interface PaymentCallback {
        void onSuccess(Payment payment);
        void onError(String message);
    }
}
