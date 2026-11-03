package com.marketplace.app.feature.import1688;

import com.marketplace.app.model.ImportAgencyConfig;
import com.marketplace.app.ui.PriceFormatter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Rôle : Service centralisé de conversion monétaire CNY (Yuan) -> MGA (Ariary) et calcul de prestation.
 * Entrée : Prix unitaire en CNY, quantité, taux administré et frais de prestation agence.
 * Sortie : Prix estimé en MGA, détail des montants et mention explicite d'exclusion du transport international.
 */
public final class CurrencyConversionService {

    public static final String TRANSPORT_DISCLAIMER =
            "Prix converti à titre indicatif selon le taux administré. "
            + "Le montant définitif à payer par MVola est confirmé par notre agence après vérification "
            + "du produit et des variantes sur 1688. "
            + "Les frais de transport international ne sont pas inclus et sont réglés directement auprès de l'importateur.";

    // Majoration commerciale interne de 15 %
    private static final double INTERNAL_COMMERCIAL_MARKUP = 1.15;

    private static CurrencyConversionService instance;
    private ImportAgencyConfig config;

    private CurrencyConversionService() {
        this.config = ImportAgencyConfig.createDefaultConfig();
    }

    public static synchronized CurrencyConversionService getInstance() {
        if (instance == null) {
            instance = new CurrencyConversionService();
        }
        return instance;
    }

    public synchronized ImportAgencyConfig getConfig() {
        return config;
    }

    public synchronized void setConfig(ImportAgencyConfig newConfig) {
        if (newConfig != null && newConfig.getExchangeRateCnyToMga() > 0) {
            this.config = newConfig;
        }
    }

    public synchronized double getExchangeRate() {
        return config.getExchangeRateCnyToMga();
    }

    public synchronized long getExchangeRateUpdatedAt() {
        return config.getExchangeRateUpdatedAt();
    }

    public synchronized String getFormattedUpdateDate() {
        long ts = config.getExchangeRateUpdatedAt();
        if (ts <= 0) return "Non daté";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(new Date(ts));
    }

    /**
     * Met à jour le taux de change administré avec horodatage.
     */
    public synchronized void updateExchangeRate(double newRateCnyToMga, String adminIdentifier) {
        if (newRateCnyToMga <= 0) {
            throw new IllegalArgumentException("Le taux de change CNY/MGA doit être supérieur à 0");
        }
        config.setExchangeRateCnyToMga(newRateCnyToMga);
        config.setExchangeRateUpdatedAt(System.currentTimeMillis());
        config.setExchangeRateUpdatedBy(adminIdentifier != null ? adminIdentifier : "Admin");
    }

    /**
     * Calcule le prix affiché en Ariary (MGA) selon la formule interne :
     * Prix affiché en MGA = Prix en CNY × Taux de conversion CNY/MGA × 1,15.
     * Cette méthode applique la formule pour l'affichage unitaire ou total dans la WebView et les formulaires 1688.
     */
    public synchronized double convertCnyToMgaFinal(double unitPriceCny) {
        if (unitPriceCny <= 0) return 0.0;
        return Math.round(unitPriceCny * config.getExchangeRateCnyToMga() * INTERNAL_COMMERCIAL_MARKUP);
    }

    /**
     * Calcule le prix affiché en MGA avec un taux explicite.
     */
    public static double convertCnyToMgaFinalWithRate(double unitPriceCny, double rateCnyToMga) {
        if (unitPriceCny <= 0 || rateCnyToMga <= 0) return 0.0;
        return Math.round(unitPriceCny * rateCnyToMga * INTERNAL_COMMERCIAL_MARKUP);
    }

    /**
     * Calcule le prix estimé du produit en ariary (MGA) pour la quantité demandée
     * avec application de la majoration commerciale interne de 15 %.
     */
    public synchronized double convertProductTotalToMga(double unitPriceCny, int quantity) {
        if (unitPriceCny <= 0 || quantity <= 0) return 0.0;
        return Math.round(unitPriceCny * quantity * config.getExchangeRateCnyToMga() * INTERNAL_COMMERCIAL_MARKUP);
    }

    /**
     * Calcule un prix avec un taux explicite fourni et majoration interne 15 %.
     */
    public static double convertWithRate(double unitPriceCny, int quantity, double rateCnyToMga) {
        if (unitPriceCny <= 0 || quantity <= 0 || rateCnyToMga <= 0) return 0.0;
        return Math.round(unitPriceCny * quantity * rateCnyToMga * INTERNAL_COMMERCIAL_MARKUP);
    }

    /**
     * Calcule les frais de prestation suggérés pour l'agence (en MGA).
     */
    public synchronized double calculateServiceFeeMga(double estimatedProductMga) {
        if (estimatedProductMga <= 0) return 0.0;
        double percent = config.getDefaultServiceFeePercent();
        if (percent <= 0) return 0.0;
        return Math.round(estimatedProductMga * (percent / 100.0));
    }

    /**
     * Calcule le montant total demandé par notre agence (produit + prestation d'achat, hors transport).
     */
    public static double calculateAgencyTotalMga(double estimatedProductMga, double serviceFeeMga) {
        return Math.max(0.0, Math.round(estimatedProductMga + serviceFeeMga));
    }

    /**
     * Formate un montant en Yuan chinois (ex: "25 ¥" ou "25,50 ¥").
     */
    public static String formatCny(double amountCny) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = (amountCny == Math.floor(amountCny))
                ? new DecimalFormat("#,##0", symbols)
                : new DecimalFormat("#,##0.00", symbols);
        return df.format(amountCny) + " ¥";
    }

    /**
     * Formate un résumé : Prix unitaire estimé / Quantité / Prix total estimé (sans révéler la formule interne).
     */
    public synchronized String formatEstimateSummary(double unitPriceCny, int quantity) {
        double unitMga = convertCnyToMgaFinal(unitPriceCny);
        double totalMga = convertProductTotalToMga(unitPriceCny, quantity);
        return "Prix unitaire : " + PriceFormatter.format(unitMga)
                + "\nQuantité : " + quantity
                + "\nPrix estimé : " + PriceFormatter.format(totalMga)
                + "\n(Hors frais de transport international)";
    }
}
