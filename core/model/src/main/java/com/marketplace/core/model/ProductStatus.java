package com.marketplace.core.model;

/**
 * Statut d'un produit dans le marketplace.
 *
 * - NOUVEAU    : Produit récemment mis en ligne par le vendeur.
 * - DISPONIBLE : Produit en stock, achetable normalement.
 * - INDISPONIBLE     : Stock épuisé (stock = 0). Visible dans le catalogue mais non achetable.
 * - A_VENDRE   : Produit mis en avant ou en vente spéciale par le vendeur.
 */
public enum ProductStatus {
    NOUVEAU,
    DISPONIBLE,
    INDISPONIBLE,
    A_VENDRE
}
