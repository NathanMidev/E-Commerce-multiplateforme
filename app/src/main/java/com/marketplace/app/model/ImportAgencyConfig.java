package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Configuration administrative du service Import 1688 (taux CNY/MGA, compte MVola agence, importateur).
 * Entrée : Paramètres gérés par l'administrateur.
 * Sortie : Coordonnées de paiement MVola, taux de change daté et informations du prestataire logistique.
 */
public class ImportAgencyConfig extends BaseModel {

    private double exchangeRateCnyToMga;
    private long exchangeRateUpdatedAt;
    private String exchangeRateUpdatedBy;
    private double defaultServiceFeePercent;

    private String mvolaPhoneNumber;
    private String mvolaBeneficiaryName;
    private String mvolaInstructions;

    private String importerName;
    private String importerCompany;
    private String importerPhone;
    private String importerWhatsapp;
    private String importerAddress;
    private String importerInstructions;

    public ImportAgencyConfig() {}

    public static ImportAgencyConfig createDefaultConfig() {
        ImportAgencyConfig cfg = new ImportAgencyConfig();
        cfg.exchangeRateCnyToMga = 650.0;
        cfg.exchangeRateUpdatedAt = System.currentTimeMillis();
        cfg.exchangeRateUpdatedBy = "Configuration initiale";
        cfg.defaultServiceFeePercent = 10.0;

        cfg.mvolaPhoneNumber = "+261 34 00 168 80";
        cfg.mvolaBeneficiaryName = "Agence Achat 1688 Marketplace";
        cfg.mvolaInstructions = "Effectuez le transfert MVola du montant exact confirmé en indiquant l'identifiant de votre commande en motif, puis saisissez la référence de transaction reçue par SMS.";

        cfg.importerName = "Rakoto Logistique Transit";
        cfg.importerCompany = "Madagascar China Cargo Transit SARL";
        cfg.importerPhone = "+261 34 11 223 34";
        cfg.importerWhatsapp = "+261341122334";
        cfg.importerAddress = "Entrepôt Guangzhou Baiyun (Chine) / Retrait Ankorondrano, Antananarivo";
        cfg.importerInstructions = "Les frais de transport international (maritime ou aérien) et de dédouanement ne sont pas inclus dans la prestation d'achat. Contactez directement l'importateur avec votre référence de commande pour connaître le tarif au kg/m³ et régler le transport.";
        return cfg;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (exchangeRateCnyToMga <= 0) {
            errors.add("Le taux de change CNY/MGA doit être strictement positif");
        }
        if (mvolaPhoneNumber == null || mvolaPhoneNumber.trim().isEmpty()) {
            errors.add("Le numéro MVola de l'agence est requis");
        }
        if (mvolaBeneficiaryName == null || mvolaBeneficiaryName.trim().isEmpty()) {
            errors.add("Le nom du bénéficiaire MVola est requis");
        }
        if (importerName == null || importerName.trim().isEmpty()) {
            errors.add("Le nom de l'importateur est requis");
        }
        if (importerPhone == null || importerPhone.trim().isEmpty()) {
            errors.add("Le téléphone de l'importateur est requis");
        }
        return errors;
    }

    public double getExchangeRateCnyToMga() { return exchangeRateCnyToMga; }
    public void setExchangeRateCnyToMga(double exchangeRateCnyToMga) { this.exchangeRateCnyToMga = exchangeRateCnyToMga; }

    public long getExchangeRateUpdatedAt() { return exchangeRateUpdatedAt; }
    public void setExchangeRateUpdatedAt(long exchangeRateUpdatedAt) { this.exchangeRateUpdatedAt = exchangeRateUpdatedAt; }

    public String getExchangeRateUpdatedBy() { return exchangeRateUpdatedBy; }
    public void setExchangeRateUpdatedBy(String exchangeRateUpdatedBy) { this.exchangeRateUpdatedBy = exchangeRateUpdatedBy; }

    public double getDefaultServiceFeePercent() { return defaultServiceFeePercent; }
    public void setDefaultServiceFeePercent(double defaultServiceFeePercent) { this.defaultServiceFeePercent = defaultServiceFeePercent; }

    public String getMvolaPhoneNumber() { return mvolaPhoneNumber; }
    public void setMvolaPhoneNumber(String mvolaPhoneNumber) { this.mvolaPhoneNumber = mvolaPhoneNumber; }

    public String getMvolaBeneficiaryName() { return mvolaBeneficiaryName; }
    public void setMvolaBeneficiaryName(String mvolaBeneficiaryName) { this.mvolaBeneficiaryName = mvolaBeneficiaryName; }

    public String getMvolaInstructions() { return mvolaInstructions; }
    public void setMvolaInstructions(String mvolaInstructions) { this.mvolaInstructions = mvolaInstructions; }

    public String getImporterName() { return importerName; }
    public void setImporterName(String importerName) { this.importerName = importerName; }

    public String getImporterCompany() { return importerCompany; }
    public void setImporterCompany(String importerCompany) { this.importerCompany = importerCompany; }

    public String getImporterPhone() { return importerPhone; }
    public void setImporterPhone(String importerPhone) { this.importerPhone = importerPhone; }

    public String getImporterWhatsapp() { return importerWhatsapp; }
    public void setImporterWhatsapp(String importerWhatsapp) { this.importerWhatsapp = importerWhatsapp; }

    public String getImporterAddress() { return importerAddress; }
    public void setImporterAddress(String importerAddress) { this.importerAddress = importerAddress; }

    public String getImporterInstructions() { return importerInstructions; }
    public void setImporterInstructions(String importerInstructions) { this.importerInstructions = importerInstructions; }
}
