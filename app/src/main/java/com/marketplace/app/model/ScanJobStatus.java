package com.marketplace.app.model;

/**
 * Rôle : Statuts du pipeline de scan 3D (SAM / COLMAP).
 * Entrée : Énumération conforme au workflow de reconstruction 3D.
 * Sortie : EN_ATTENTE, EN_COURS, TERMINE, ECHEC.
 */
public enum ScanJobStatus {
    EN_ATTENTE,
    EN_COURS,
    TERMINE,
    ECHEC
}
