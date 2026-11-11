package com.marketplace.app.feature.import1688;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.marketplace.app.R;
import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.ImportOrderRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.contact.ContactLinks;
import com.marketplace.app.model.ImportAgencyConfig;
import com.marketplace.app.model.ImportOrder;
import com.marketplace.app.model.ImportStatusHistoryEntry;
import com.marketplace.app.model.ImportTrackingStatus;
import com.marketplace.app.model.PaymentStatus;
import com.marketplace.app.navigation.Router;
import com.marketplace.app.ui.PriceFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Écran de détail et de suivi d'une commande Import 1688 (Sections 15, 16, 17, 18).
 * - Affiche le récapitulatif financier (Prix CNY, Prix Ariary, Commission Agence, Total MVola).
 * - Permet au client d'envoyer sa référence de transaction MVola via le bouton
 *   « J'ai effectué le paiement » (le statut reste EN_ATTENTE jusqu'à validation par l'admin).
 * - Affiche la progression sur les 6 étapes de suivi jusqu'à RECU_PAR_IMPORTATEUR.
 * - Affiche le message final obligatoire et les coordonnées complètes de l'importateur.
 */
public class Import1688OrderDetailActivity extends AppCompatActivity {

    private ImportOrderRepository repository;
    private String orderId;
    private ImportOrder currentOrder;
    private ImportAgencyConfig agencyConfig;

    private TextView txtToolbarTitle;
    private TextView txtSimulatedTag;
    private View cardFinalImporterBanner;
    private TextView txtFinalImporterMessage;

    private TextView txtDetailOrderId;
    private TextView txtDetailProductName;
    private TextView txtDetailVariantQty;
    private TextView txtFinPriceCny;
    private TextView txtFinRate;
    private TextView txtFinConvertedMga;
    private TextView txtFinServiceFee;
    private TextView txtFinTotalMga;

    private TextView txtMvolaStatusBadge;
    private TextView txtMvolaRecipientInfo;
    private TextView txtMvolaInstructions;
    private EditText edtMvolaReference;
    private MaterialButton btnSubmitMvolaRef;

    private TextView txtSupplierPurchaseBox;
    private TextView txtStepsChecklist;
    private TextView txtStatusHistory;
    private TextView txtImporterDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import1688_order_detail);

        repository = new ImportOrderRepository();
        orderId = getIntent().getStringExtra(Router.EXTRA_IMPORT_ORDER_ID);

        txtToolbarTitle = findViewById(R.id.txt_toolbar_title);
        txtSimulatedTag = findViewById(R.id.txt_simulated_tag);
        cardFinalImporterBanner = findViewById(R.id.card_final_importer_banner);
        txtFinalImporterMessage = findViewById(R.id.txt_final_importer_message);

        txtDetailOrderId = findViewById(R.id.txt_detail_order_id);
        txtDetailProductName = findViewById(R.id.txt_detail_product_name);
        txtDetailVariantQty = findViewById(R.id.txt_detail_variant_qty);
        txtFinPriceCny = findViewById(R.id.txt_fin_price_cny);
        txtFinRate = findViewById(R.id.txt_fin_rate);
        txtFinConvertedMga = findViewById(R.id.txt_fin_converted_mga);
        txtFinServiceFee = findViewById(R.id.txt_fin_service_fee);
        txtFinTotalMga = findViewById(R.id.txt_fin_total_mga);

        txtMvolaStatusBadge = findViewById(R.id.txt_mvola_status_badge);
        txtMvolaRecipientInfo = findViewById(R.id.txt_mvola_recipient_info);
        txtMvolaInstructions = findViewById(R.id.txt_mvola_instructions);
        edtMvolaReference = findViewById(R.id.edt_mvola_reference);
        btnSubmitMvolaRef = findViewById(R.id.btn_submit_mvola_ref);

        txtSupplierPurchaseBox = findViewById(R.id.txt_supplier_purchase_box);
        txtStepsChecklist = findViewById(R.id.txt_steps_checklist);
        txtStatusHistory = findViewById(R.id.txt_status_history);
        txtImporterDetails = findViewById(R.id.txt_importer_details);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_detail_faq).setOnClickListener(v -> Router.openFaq(this));

        findViewById(R.id.btn_open_1688_link).setOnClickListener(v -> {
            if (currentOrder != null && currentOrder.getProductUrl() != null) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentOrder.getProductUrl())));
                } catch (Exception e) {
                    Toast.makeText(this, "Lien 1688 invalide", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSubmitMvolaRef.setOnClickListener(v -> submitMvolaPayment());

        findViewById(R.id.btn_call_importer).setOnClickListener(v -> {
            String phone = getImporterPhone();
            if (!phone.isEmpty()) {
                try {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                } catch (Exception e) {
                    Toast.makeText(this, "Impossible de composer le numéro", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_whatsapp_importer).setOnClickListener(v -> {
            String phone = getImporterPhone();
            if (!phone.isEmpty()) {
                String url = ContactLinks.buildWhatsAppUrl(
                        phone,
                        "Commande 1688 #" + (currentOrder != null ? currentOrder.getImportOrderId() : ""),
                        0,
                        "Import 1688"
                );
                if (url != null) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        Toast.makeText(this, "Impossible d'ouvrir WhatsApp", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        loadOrderDetails();
    }

    private String getImporterPhone() {
        if (currentOrder != null && currentOrder.getImporterPhone() != null && !currentOrder.getImporterPhone().isEmpty()) {
            return currentOrder.getImporterPhone();
        }
        if (agencyConfig != null && agencyConfig.getImporterPhone() != null) {
            return agencyConfig.getImporterPhone();
        }
        return "";
    }

    private void loadOrderDetails() {
        repository.getAgencyConfig(new BaseRepository.Callback<ImportAgencyConfig>() {
            @Override
            public void onSuccess(ImportAgencyConfig data) {
                agencyConfig = data;
                renderAgencyConfig();
            }

            @Override
            public void onError(String message) {}
        });

        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Identifiant de commande manquant", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository.getById(orderId, new BaseRepository.Callback<ImportOrder>() {
            @Override
            public void onSuccess(ImportOrder order) {
                currentOrder = order;
                renderOrder(order);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(Import1688OrderDetailActivity.this,
                        "Erreur chargement commande : " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderAgencyConfig() {
        if (agencyConfig == null) return;
        txtMvolaRecipientInfo.setText("Numéro MVola Agence : " + agencyConfig.getMvolaPhoneNumber()
                + "\nBénéficiaire : " + agencyConfig.getMvolaBeneficiaryName()
                + (currentOrder != null ? "\nMontant à envoyer : " + PriceFormatter.format(currentOrder.getTotalAmountMGA())
                + "\nMotif MVola : " + currentOrder.getImportOrderId() : ""));
        if (agencyConfig.getMvolaInstructions() != null && !agencyConfig.getMvolaInstructions().isEmpty()) {
            txtMvolaInstructions.setText(agencyConfig.getMvolaInstructions());
        }
    }

    private void renderOrder(ImportOrder order) {
        txtToolbarTitle.setText("Suivi " + order.getImportOrderId());
        txtSimulatedTag.setVisibility(order.isSimulated() ? View.VISIBLE : View.GONE);

        // Message final lorsque RECU_PAR_IMPORTATEUR (Section 18)
        if (order.isReceivedByImporter()) {
            cardFinalImporterBanner.setVisibility(View.VISIBLE);
            txtFinalImporterMessage.setText(ImportOrder.FINAL_IMPORTER_MESSAGE);
        } else {
            cardFinalImporterBanner.setVisibility(View.GONE);
        }

        String dateStr = order.getCreatedAt() > 0
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(new Date(order.getCreatedAt()))
                : "-";
        txtDetailOrderId.setText("Commande #" + order.getImportOrderId() + " • Créée le " + dateStr);
        txtDetailProductName.setText(order.getProductName());
        txtDetailVariantQty.setText("Variante : " + order.getProductVariant() + " • Quantité : " + order.getQuantity());

        double totalCny = order.getPriceCNY() * order.getQuantity();
        txtFinPriceCny.setText("Prix du produit en CNY : " + CurrencyConversionService.formatCny(totalCny)
                + " (" + order.getQuantity() + " × " + CurrencyConversionService.formatCny(order.getPriceCNY()) + ")");
        txtFinRate.setText("Taux appliqué : 1 CNY = " + Math.round(order.getExchangeRate()) + " Ar");
        txtFinConvertedMga.setText("Prix converti en Ariary : " + PriceFormatter.format(order.getEstimatedPriceMGA()));
        txtFinServiceFee.setText("Frais de service d'achat de l'agence : " + PriceFormatter.format(order.getServiceFeeMGA()));

        String confirmLabel = order.isAmountConfirmedByAdmin()
                ? " (Montant définitif confirmé par l'admin)"
                : " (En attente de confirmation finale par l'admin)";
        txtFinTotalMga.setText("Montant total à payer à l'agence : "
                + PriceFormatter.format(order.getTotalAmountMGA()) + confirmLabel);

        renderAgencyConfig();

        // Statut MVola
        PaymentStatus ps = order.getPaymentStatus() != null ? order.getPaymentStatus() : PaymentStatus.EN_ATTENTE;
        txtMvolaStatusBadge.setText("MVola : " + ps.name());
        if (order.getPaymentReference() != null && !order.getPaymentReference().isEmpty()) {
            edtMvolaReference.setText(order.getPaymentReference());
        }

        if (ps == PaymentStatus.PAYE) {
            btnSubmitMvolaRef.setEnabled(false);
            btnSubmitMvolaRef.setText("Paiement MVola vérifié et confirmé par l'agence");
            edtMvolaReference.setEnabled(false);
        } else {
            btnSubmitMvolaRef.setEnabled(true);
            edtMvolaReference.setEnabled(true);
            if (order.getPaymentReference() != null && !order.getPaymentReference().isEmpty()) {
                btnSubmitMvolaRef.setText("Mettre à jour la référence MVola (En attente de vérification)");
            } else {
                btnSubmitMvolaRef.setText("J'ai effectué le paiement (Envoyer la référence)");
            }
        }

        // Informations sur l'achat réel 1688 (Section 16)
        if (order.getSupplierOrderReference() != null && !order.getSupplierOrderReference().isEmpty()) {
            txtSupplierPurchaseBox.setVisibility(View.VISIBLE);
            txtSupplierPurchaseBox.setText("Achat réel sur 1688 effectué par l'agence :\n"
                    + "• Réf. commande fournisseur 1688 : " + order.getSupplierOrderReference() + "\n"
                    + "• Montant payé par carte agence : " + CurrencyConversionService.formatCny(order.getActualPurchasePriceCNY()) + "\n"
                    + "• Date d'achat : " + (order.getPurchaseDate() != null ? order.getPurchaseDate() : "-"));
        } else {
            txtSupplierPurchaseBox.setVisibility(View.GONE);
        }

        // Checklist des 6 étapes de suivi (Section 17)
        ImportTrackingStatus currentStep = order.getTrackingStatus() != null
                ? order.getTrackingStatus() : ImportTrackingStatus.PAIEMENT_EN_ATTENTE;
        StringBuilder stepsSb = new StringBuilder();
        for (ImportTrackingStatus step : ImportTrackingStatus.values()) {
            boolean reached = step.isReached(currentStep);
            String prefix = (step == currentStep) ? "▶ [EN COURS] " : (reached ? "✔ " : "○ ");
            stepsSb.append(prefix)
                    .append(step.getStepOrder()).append(". ")
                    .append(step.getLabel())
                    .append("\n   ").append(step.getDescription())
                    .append("\n\n");
        }
        txtStepsChecklist.setText(stepsSb.toString().trim());

        // Historique horodaté (Section 17)
        List<ImportStatusHistoryEntry> history = order.getStatusHistory();
        if (history == null || history.isEmpty()) {
            txtStatusHistory.setText("Aucun historique disponible.");
        } else {
            StringBuilder histSb = new StringBuilder();
            for (ImportStatusHistoryEntry entry : history) {
                String stName = entry.getStatus() != null ? entry.getStatus().getLabel() : "-";
                histSb.append("• [").append(entry.getDate()).append(" à ").append(entry.getTime()).append("] ")
                        .append(stName).append("\n  ")
                        .append(entry.getNote() != null ? entry.getNote() : "")
                        .append(" (par ").append(entry.getUpdatedByAdmin() != null ? entry.getUpdatedByAdmin() : "système").append(")\n\n");
            }
            txtStatusHistory.setText(histSb.toString().trim());
        }

        // Coordonnées de l'importateur (Sections 17 & 18)
        String impName = order.getImporterName() != null ? order.getImporterName()
                : (agencyConfig != null ? agencyConfig.getImporterName() : "Cargo Express Madagascar");
        String impCompany = order.getImporterCompany() != null ? order.getImporterCompany()
                : (agencyConfig != null ? agencyConfig.getImporterCompany() : "");
        String impPhone = getImporterPhone();
        String impAddress = order.getImporterAddress() != null ? order.getImporterAddress()
                : (agencyConfig != null ? agencyConfig.getImporterAddress() : "");
        String impInstructions = order.getImporterInstructions() != null ? order.getImporterInstructions()
                : (agencyConfig != null ? agencyConfig.getImporterInstructions() : "");

        txtImporterDetails.setText("Importateur : " + impName
                + (impCompany != null && !impCompany.isEmpty() ? " (" + impCompany + ")" : "")
                + "\nTéléphone : " + impPhone
                + "\nAdresse de retrait : " + impAddress
                + "\nInstructions : " + impInstructions);
    }

    private void submitMvolaPayment() {
        String ref = edtMvolaReference.getText().toString().trim();
        if (ref.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir la référence de votre transaction MVola", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitMvolaRef.setEnabled(false);
        repository.submitMvolaPaymentReference(orderId, ref, new BaseRepository.Callback<ImportOrder>() {
            @Override
            public void onSuccess(ImportOrder updated) {
                currentOrder = updated;
                renderOrder(updated);
                Toast.makeText(Import1688OrderDetailActivity.this,
                        "Référence MVola enregistrée. Statut : EN_ATTENTE de vérification par l'administrateur.",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                btnSubmitMvolaRef.setEnabled(true);
                Toast.makeText(Import1688OrderDetailActivity.this,
                        "Erreur : " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
