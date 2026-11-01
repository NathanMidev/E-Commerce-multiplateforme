package com.marketplace.app.model;

/**
 * Rôle : Modes de paiement déclaratifs supportés.
 * Entrée : Énumération conforme au backend PostgreSQL (payment_method).
 * Sortie : A_LA_LIVRAISON, MVOLA, ORANGE_MONEY, AIRTEL_MONEY.
 */
public enum PaymentMethod {
    A_LA_LIVRAISON,
    MVOLA,
    ORANGE_MONEY,
    AIRTEL_MONEY
}
