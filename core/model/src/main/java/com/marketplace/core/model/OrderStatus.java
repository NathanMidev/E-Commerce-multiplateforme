package com.marketplace.core.model;

/**
 * Statut d'une commande (mêmes valeurs que le type PostgreSQL order_status).
 *
 *   EN_ATTENTE -> CONFIRMEE (vendeur) | ANNULEE (acheteur ou vendeur)
 *   CONFIRMEE  -> EXPEDIEE  (vendeur) | ANNULEE (vendeur)
 *   EXPEDIEE   -> LIVREE    (acheteur : confirme la réception, ce qui compte comme une vente)
 */
public enum OrderStatus {
    EN_ATTENTE,
    CONFIRMEE,
    EXPEDIEE,
    LIVREE,
    ANNULEE;

    /** Plus aucune transition possible. */
    public boolean isFinal() {
        return this == LIVREE || this == ANNULEE;
    }
}
