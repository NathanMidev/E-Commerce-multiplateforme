package com.marketplace.app.model;

/**
 * Rôle : Niveaux d'utilisateur et statuts de compte.
 * Entrée : Calculé par le serveur ou résolu dynamiquement.
 * Sortie : Rôles BUYER, SELLER, PRO et visiteur GUEST.
 */
public enum UserRole {
    GUEST,
    BUYER,
    SELLER,
    PRO
}
