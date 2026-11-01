package com.marketplace.core.model;

/**
 * Rôles possibles d'un utilisateur dans le marketplace.
 *
 * - GUEST  : Visiteur non connecté. Peut parcourir le catalogue et voir les fiches produits.
 *            L'accès à la visualisation AR/3D et à l'achat est bloqué → redirige vers Login.
 * - BUYER  : Acheteur authentifié. Peut visualiser en AR/3D, ajouter au panier et commander.
 * - SELLER : Vendeur authentifié. Dispose de tous les droits du BUYER, plus la création de
 *            produits et le scanner 3D (pipeline SAM Meta).
 */
public enum UserRole {
    GUEST,
    BUYER,
    SELLER,
    PRO
}
