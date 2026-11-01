package com.marketplace.app.model;

/**
 * Rôle : Statuts du cycle de vie d'un produit dans le catalogue.
 * Entrée : Énumération conforme au backend PostgreSQL (product_status).
 * Sortie : NOUVEAU, DISPONIBLE, INDISPONIBLE, A_VENDRE.
 */
public enum ProductStatus {
    NOUVEAU,
    DISPONIBLE,
    INDISPONIBLE,
    A_VENDRE
}
