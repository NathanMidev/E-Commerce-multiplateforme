package com.marketplace.app.base;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Rôle : Classe de base pour tous les modèles métier de l'application.
 * Entrée : Champs communs et sérialisation Serializable.
 * Sortie : Validation unifiée et identifiant.
 */
public abstract class BaseModel implements Serializable {
    private String id;

    public BaseModel() {}

    public BaseModel(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Valide l'état du modèle.
     * @return Liste des messages d'erreur de saisie (vide si valide).
     */
    public List<String> validate() {
        return Collections.emptyList();
    }
}
