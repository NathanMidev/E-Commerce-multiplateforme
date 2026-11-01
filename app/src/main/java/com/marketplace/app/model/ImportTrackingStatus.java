package com.marketplace.app.model;

/**
 * Rôle : Énumération des 6 états officiels du suivi d'une commande d'importation 1688.
 * Entrée : Mises à jour administratives chronologiques.
 * Sortie : Libellé, description métier et ordre de progression strict.
 */
public enum ImportTrackingStatus {
    PAIEMENT_EN_ATTENTE(
            1,
            "Paiement en attente",
            "Le client a soumis sa demande, mais le paiement n'est pas encore vérifié."
    ),
    PAIEMENT_CONFIRME(
            2,
            "Paiement confirmé",
            "Notre agence a vérifié et confirmé le paiement."
    ),
    ACHAT_EFFECTUE(
            3,
            "Achat effectué sur 1688",
            "Notre agence a réellement acheté le produit sur 1688."
    ),
    PREPARATION_FOURNISSEUR(
            4,
            "Préparation fournisseur",
            "Le fournisseur prépare le produit."
    ),
    EXPEDIE_VERS_IMPORTATEUR(
            5,
            "Expédié vers l'importateur",
            "Le fournisseur a expédié le colis vers le prestataire logistique."
    ),
    RECU_PAR_IMPORTATEUR(
            6,
            "Reçu par l'importateur",
            "Le prestataire logistique a réceptionné le colis."
    );

    private final int stepOrder;
    private final String label;
    private final String description;

    ImportTrackingStatus(int stepOrder, String label, String description) {
        this.stepOrder = stepOrder;
        this.label = label;
        this.description = description;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Indique si cette étape est atteinte ou dépassée par rapport au statut actuel réellement confirmé.
     */
    public boolean isReached(ImportTrackingStatus currentActualStatus) {
        if (currentActualStatus == null) return false;
        return this.stepOrder <= currentActualStatus.stepOrder;
    }
}
