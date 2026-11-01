package com.marketplace.app.model;

/**
 * Rôle : Statuts du cycle de vie d'une commande.
 * Entrée : Énumération conforme au backend PostgreSQL (order_status).
 * Sortie : EN_ATTENTE, CONFIRMEE, EXPEDIEE, LIVREE, ANNULEE.
 */
public enum OrderStatus {
    EN_ATTENTE,
    CONFIRMEE,
    EXPEDIEE,
    LIVREE,
    ANNULEE;

    public boolean isFinal() {
        return this == LIVREE || this == ANNULEE;
    }
}
