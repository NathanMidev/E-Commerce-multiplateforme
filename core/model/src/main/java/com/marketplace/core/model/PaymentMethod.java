package com.marketplace.core.model;

/**
 * Mode de paiement choisi à la commande. Il est purement déclaratif : l'application
 * n'effectue aucun paiement en ligne (le règlement se fait entre l'acheteur et le vendeur).
 */
public enum PaymentMethod {
    A_LA_LIVRAISON,
    MVOLA,
    ORANGE_MONEY,
    AIRTEL_MONEY
}
