package com.marketplace.app.feature.ar;

import java.util.Collections;
import java.util.List;

/**
 * Rôle : Contrôleur facultatif ML Kit pour l'analyse visuelle des produits (Edge AI).
 * Entrée : Image capturée.
 * Sortie : Validation de compatibilité de catégorie ou signalement transparent si non implémenté.
 */
public final class ProductVisionChecker {

    private ProductVisionChecker() {}

    /**
     * Analyse un objet. Indique explicitement que ML Kit n'est pas configuré pour une classification fine.
     */
    public static List<String> detectCategories(byte[] imageBytes) {
        // Transparent : ML Kit complet n'est pas déployé pour éviter des modèles lourds non sollicités
        return Collections.emptyList();
    }
}
