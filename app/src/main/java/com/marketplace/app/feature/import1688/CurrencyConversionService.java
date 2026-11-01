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
     * Calcule le prix estimé du produit en ariary (MGA) pour la quantité demandée.
     */
    public synchronized double convertProductTotalToMga(double unitPriceCny, int quantity) {
        if (unitPriceCny <= 0 || quantity <= 0) return 0.0;
        return Math.round(unitPriceCny * quantity * config.getExchangeRateCnyToMga());
    }

    /**
     * Calcule un prix avec un taux explicite fourni.
     */
    public static double convertWithRate(double unitPriceCny, int quantity, double rateCnyToMga) {
        if (unitPriceCny <= 0 || quantity <= 0 || rateCnyToMga <= 0) return 0.0;
        return Math.round(unitPriceCny * quantity * rateCnyToMga);
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
     * Formate un résumé conforme au gabarit : Prix unitaire / Quantité / Prix estimé.
     */
    public synchronized String formatEstimateSummary(double unitPriceCny, int quantity) {
        double estMga = convertProductTotalToMga(unitPriceCny, quantity);
        return "Prix unitaire : " + formatCny(unitPriceCny)
                + "\nQuantité : " + quantity
                + "\nPrix estimé : " + PriceFormatter.format(estMga)
                + " (Taux administré : 1 ¥ = " + PriceFormatter.format(config.getExchangeRateCnyToMga())
                + ", màj " + getFormattedUpdateDate() + ")";
    }
}
