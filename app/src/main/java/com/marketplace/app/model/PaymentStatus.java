package com.marketplace.app.model;

/**
 * Rôle : Statuts du paiement déclaratif d'une commande.
 * Entrée : Énumération requise par le cahier des charges.
 * Sortie : EN_ATTENTE, PAYE, ECHOUE.
 */
public enum PaymentStatus {
    EN_ATTENTE,
    PAYE,
    ECHOUE
}
