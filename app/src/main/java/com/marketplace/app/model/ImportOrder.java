package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Modèle complet d'une commande de prestation d'achat Import 1688.
 * Entrée : Demande client (lien 1688, variante, quantité, prix CNY) et validations administratives.
 * Sortie : Suivi financier (produit, prestation agence, paiement MVola) et logistique (jusqu'à l'importateur).
 */
public class ImportOrder extends BaseModel {

    public static final String FINAL_IMPORTER_MESSAGE =
            "Votre produit a été réceptionné par notre prestataire logistique.\n\n"
            + "Votre prestation d'achat est terminée.\n\n"
            + "Veuillez contacter directement l'importateur pour organiser la suite de l'acheminement et régler les frais de transport.\n\n"
            + "Attention : les frais de transport ne sont pas compris dans le paiement effectué à notre agence.";

    private String importOrderId;
    private String userId;
    private String customerName;
    private String customerPhone;
    private String productUrl;
    private String productName;
    private String productImageUrl;
    private String productVariant;
    private int quantity;
    private double priceCNY;
    private double exchangeRate;
    private double estimatedPriceMGA;
    private double serviceFeeMGA;
    private double totalAmountMGA;
    private boolean amountConfirmedByAdmin;
    private String notes;

    private String paymentReference;
    private PaymentStatus paymentStatus;
    private ImportTrackingStatus trackingStatus;

    private String supplierOrderReference;
    private double actualPurchasePriceCNY;
    private String purchaseDate;
    private String adminPurchaseNote;

    private String importerName;
    private String importerCompany;
    private String importerPhone;
    private String importerWhatsapp;
    private String importerAddress;
    private String importerInstructions;

    private List<ImportStatusHistoryEntry> statusHistory = new ArrayList<>();
    private long createdAt;
    private long updatedAt;
    private boolean simulated;

    public ImportOrder() {
        this.paymentStatus = PaymentStatus.EN_ATTENTE;
        this.trackingStatus = ImportTrackingStatus.PAIEMENT_EN_ATTENTE;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (customerName == null || customerName.trim().isEmpty()) {
            errors.add("Le nom complet du client est obligatoire");
        }
        if (customerPhone == null || customerPhone.trim().isEmpty()) {
            errors.add("Le numéro de téléphone du client est obligatoire");
        }
        if (productUrl == null || productUrl.trim().isEmpty()
                || !(productUrl.startsWith("http://") || productUrl.startsWith("https://"))) {
            errors.add("Le lien original du produit 1688 (http/https) est obligatoire");
        }
        if (productName == null || productName.trim().isEmpty()) {
            errors.add("Le nom du produit est obligatoire");
        }
        if (quantity <= 0) {
            errors.add("La quantité doit être supérieure ou égale à 1");
        }
        if (priceCNY <= 0) {
            errors.add("Le prix unitaire en yuan (CNY) doit être supérieur à 0");
        }
        if (exchangeRate <= 0) {
            errors.add("Le taux de change CNY/MGA doit être supérieur à 0");
        }
        return errors;
    }

    public boolean isReceivedByImporter() {
        return trackingStatus == ImportTrackingStatus.RECU_PAR_IMPORTATEUR;
    }

    public String getImportOrderId() { return importOrderId; }
    public void setImportOrderId(String importOrderId) { this.importOrderId = importOrderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public String getProductVariant() { return productVariant; }
    public void setProductVariant(String productVariant) { this.productVariant = productVariant; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPriceCNY() { return priceCNY; }
    public void setPriceCNY(double priceCNY) { this.priceCNY = priceCNY; }

    public double getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(double exchangeRate) { this.exchangeRate = exchangeRate; }

    public double getEstimatedPriceMGA() { return estimatedPriceMGA; }
    public void setEstimatedPriceMGA(double estimatedPriceMGA) { this.estimatedPriceMGA = estimatedPriceMGA; }

    public double getServiceFeeMGA() { return serviceFeeMGA; }
    public void setServiceFeeMGA(double serviceFeeMGA) { this.serviceFeeMGA = serviceFeeMGA; }

    public double getTotalAmountMGA() { return totalAmountMGA; }
    public void setTotalAmountMGA(double totalAmountMGA) { this.totalAmountMGA = totalAmountMGA; }

    public boolean isAmountConfirmedByAdmin() { return amountConfirmedByAdmin; }
    public void setAmountConfirmedByAdmin(boolean amountConfirmedByAdmin) { this.amountConfirmedByAdmin = amountConfirmedByAdmin; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public ImportTrackingStatus getTrackingStatus() { return trackingStatus; }
    public void setTrackingStatus(ImportTrackingStatus trackingStatus) { this.trackingStatus = trackingStatus; }

    public String getSupplierOrderReference() { return supplierOrderReference; }
    public void setSupplierOrderReference(String supplierOrderReference) { this.supplierOrderReference = supplierOrderReference; }

    public double getActualPurchasePriceCNY() { return actualPurchasePriceCNY; }
    public void setActualPurchasePriceCNY(double actualPurchasePriceCNY) { this.actualPurchasePriceCNY = actualPurchasePriceCNY; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getAdminPurchaseNote() { return adminPurchaseNote; }
    public void setAdminPurchaseNote(String adminPurchaseNote) { this.adminPurchaseNote = adminPurchaseNote; }

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

    public List<ImportStatusHistoryEntry> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<ImportStatusHistoryEntry> statusHistory) {
        this.statusHistory = statusHistory != null ? statusHistory : new ArrayList<>();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }
}
