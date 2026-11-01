package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Catégorie de produits du catalogue.
 * Entrée : Champs JSON renvoyés par GET /api/categories.
 * Sortie : Modèle sérialisable et validation.
 */
public class Category extends BaseModel {
    private String name;
    private String description;
    private String iconName;

    public Category() {
        super();
    }

    public Category(String id, String name, String description, String iconName) {
        super(id);
        this.name = name;
        this.description = description;
        this.iconName = iconName;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            errors.add("Le nom de la catégorie est obligatoire");
        }
        return errors;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}
