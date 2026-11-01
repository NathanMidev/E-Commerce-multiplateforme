package com.marketplace.app.feature.import1688;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.marketplace.app.R;
import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.ImportOrderRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.ImportAgencyConfig;
import com.marketplace.app.model.ImportOrder;
import com.marketplace.app.model.ImportTrackingStatus;
import com.marketplace.app.model.PaymentStatus;
import com.marketplace.app.ui.PriceFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Interface Administrateur Agence Import 1688 (Section 19).
 *
 * Contrôle d'accès obligatoire :
 * - Vérifie {@link AdminAccessControl#isCurrentUserAdmin()}.
 * - Un client standard est bloqué (écran 403 Accès Refusé + vérification dans ImportOrderRepository).
 * - En mode démonstration simulée ({@link ApiClient#isUseMock()}), un bouton explicite
 *   permet d'activer/désactiver le profil Admin de test pendant la soutenance.
 */
public class Import1688AdminActivity extends AppCompatActivity {

    private ImportOrderRepository repository;
    private ImportOrdersAdapter ordersAdapter;
    private ImportOrder selectedOrder;
    private ImportAgencyConfig currentConfig;

    private TextView txtAdminAccessState;
    private MaterialButton btnToggleDemoAdmin;
    private View layoutAccessDenied;
    private View scrollAdminContent;

    private EditText edtCfgRate;
    private EditText edtCfgFeePercent;
    private EditText edtCfgMvolaPhone;
    private EditText edtCfgImporterName;
    private EditText edtCfgImporterPhone;
    private EditText edtCfgImporterAddress;

    private EditText edtAdminSearch;
    private View cardSelectedOrderActions;
    private TextView txtSelectedOrderHeader;
    private TextView txtSelectedOrderMeta;
    private EditText edtConfirmPriceCny;
    private EditText edtConfirmFeeMga;
    private TextView txtAdminMvolaRef;
    private EditText edtSupplierOrderRef;
    private EditText edtActualPurchaseCny;
    private EditText edtPurchaseDate;
    private EditText edtTrackingNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import1688_admin);

        if (ApiClient.isUseMock() && !AuthSession.getInstance().isLoggedIn()) {
            AuthSession.getInstance().updateSession("mock-admin-1", "mock-jwt-token");
        }

        repository = new ImportOrderRepository(ApiClient.isUseMock());

        txtAdminAccessState = findViewById(R.id.txt_admin_access_state);
        btnToggleDemoAdmin = findViewById(R.id.btn_toggle_demo_admin);
        layoutAccessDenied = findViewById(R.id.layout_access_denied);
        scrollAdminContent = findViewById(R.id.scroll_admin_content);

        edtCfgRate = findViewById(R.id.edt_cfg_rate);
        edtCfgFeePercent = findViewById(R.id.edt_cfg_fee_percent);
        edtCfgMvolaPhone = findViewById(R.id.edt_cfg_mvola_phone);
        edtCfgImporterName = findViewById(R.id.edt_cfg_importer_name);
        edtCfgImporterPhone = findViewById(R.id.edt_cfg_importer_phone);
        edtCfgImporterAddress = findViewById(R.id.edt_cfg_importer_address);

        edtAdminSearch = findViewById(R.id.edt_admin_search);
        cardSelectedOrderActions = findViewById(R.id.card_selected_order_actions);
        txtSelectedOrderHeader = findViewById(R.id.txt_selected_order_header);
        txtSelectedOrderMeta = findViewById(R.id.txt_selected_order_meta);
        edtConfirmPriceCny = findViewById(R.id.edt_confirm_price_cny);
        edtConfirmFeeMga = findViewById(R.id.edt_confirm_fee_mga);
        txtAdminMvolaRef = findViewById(R.id.txt_admin_mvola_ref);
        edtSupplierOrderRef = findViewById(R.id.edt_supplier_order_ref);
        edtActualPurchaseCny = findViewById(R.id.edt_actual_purchase_cny);
        edtPurchaseDate = findViewById(R.id.edt_purchase_date);
        edtTrackingNote = findViewById(R.id.edt_tracking_note);

        RecyclerView recycler = findViewById(R.id.recycler_admin_orders);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new ImportOrdersAdapter(this::selectOrderForAdminAction);
        recycler.setAdapter(ordersAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Bouton de bascule Admin de test disponible uniquement en mode simulé pour la soutenance
        if (ApiClient.isUseMock()) {
            btnToggleDemoAdmin.setVisibility(View.VISIBLE);
            btnToggleDemoAdmin.setOnClickListener(v -> {
                boolean next = !AdminAccessControl.isCurrentUserAdmin();
                String uid = AuthSession.getInstance().getUserId();
                if (uid == null || uid.isEmpty()) {
                    uid = "mock-admin-1";
                    AuthSession.getInstance().updateSession(uid, "mock-jwt-token");
                }
                AdminAccessControl.grantAdminForUid(uid, next);
                refreshAccessState();
            });
        } else {
            btnToggleDemoAdmin.setVisibility(View.GONE);
        }

        findViewById(R.id.btn_save_config).setOnClickListener(v -> saveAgencyConfig());
        findViewById(R.id.btn_admin_search).setOnClickListener(v -> loadAdminOrders());
        findViewById(R.id.btn_admin_open_1688).setOnClickListener(v -> {
            if (selectedOrder != null && selectedOrder.getProductUrl() != null) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(selectedOrder.getProductUrl())));
                } catch (Exception e) {
                    Toast.makeText(this, "Lien 1688 invalide", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_admin_confirm_amount).setOnClickListener(v -> confirmSelectedOrderAmount());
        findViewById(R.id.btn_admin_mvola_approve).setOnClickListener(v -> verifyMvolaPayment(true));
        findViewById(R.id.btn_admin_mvola_reject).setOnClickListener(v -> verifyMvolaPayment(false));
        findViewById(R.id.btn_admin_record_purchase).setOnClickListener(v -> recordReal1688Purchase());

        findViewById(R.id.btn_status_prep).setOnClickListener(v ->
                updateTrackingStatus(ImportTrackingStatus.PREPARATION_FOURNISSEUR));
        findViewById(R.id.btn_status_shipped).setOnClickListener(v ->
                updateTrackingStatus(ImportTrackingStatus.EXPEDIE_VERS_IMPORTATEUR));
        findViewById(R.id.btn_status_received).setOnClickListener(v ->
                updateTrackingStatus(ImportTrackingStatus.RECU_PAR_IMPORTATEUR));

        refreshAccessState();
    }

    private void refreshAccessState() {
        boolean isAdmin = AdminAccessControl.isCurrentUserAdmin();
        if (ApiClient.isUseMock()) {
            btnToggleDemoAdmin.setText(isAdmin ? "[Simulé : Désactiver Admin]" : "[Simulé : Activer Admin]");
        }

        if (!isAdmin) {
            txtAdminAccessState.setText("Accès refusé : Utilisateur standard (Non-Admin)");
            layoutAccessDenied.setVisibility(View.VISIBLE);
            scrollAdminContent.setVisibility(View.GONE);
        } else {
            txtAdminAccessState.setText("Connecté en tant qu'Administrateur (" + AuthSession.getInstance().getUserId() + ")");
            layoutAccessDenied.setVisibility(View.GONE);
            scrollAdminContent.setVisibility(View.VISIBLE);
            loadAgencyConfig();
            loadAdminOrders();
        }
    }

    private void loadAgencyConfig() {
        repository.getAgencyConfig(new BaseRepository.Callback<ImportAgencyConfig>() {
            @Override
            public void onSuccess(ImportAgencyConfig cfg) {
                currentConfig = cfg;
                edtCfgRate.setText(String.valueOf(cfg.getExchangeRateCnyToMga()));
                edtCfgFeePercent.setText(String.valueOf(cfg.getDefaultServiceFeePercent()));
                edtCfgMvolaPhone.setText(cfg.getMvolaPhoneNumber());
                edtCfgImporterName.setText(cfg.getImporterName());
                edtCfgImporterPhone.setText(cfg.getImporterPhone());
                edtCfgImporterAddress.setText(cfg.getImporterAddress());
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void saveAgencyConfig() {
        if (currentConfig == null) {
            currentConfig = ImportAgencyConfig.createDefaultConfig();
        }
        try {
            currentConfig.setExchangeRateCnyToMga(Double.parseDouble(edtCfgRate.getText().toString().trim()));
            currentConfig.setDefaultServiceFeePercent(Double.parseDouble(edtCfgFeePercent.getText().toString().trim()));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Taux ou pourcentage invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        currentConfig.setMvolaPhoneNumber(edtCfgMvolaPhone.getText().toString().trim());
        currentConfig.setImporterName(edtCfgImporterName.getText().toString().trim());
        currentConfig.setImporterPhone(edtCfgImporterPhone.getText().toString().trim());
        currentConfig.setImporterAddress(edtCfgImporterAddress.getText().toString().trim());

        repository.updateAgencyConfig(currentConfig, new BaseRepository.Callback<ImportAgencyConfig>() {
            @Override
            public void onSuccess(ImportAgencyConfig data) {
                Toast.makeText(Import1688AdminActivity.this,
                        "Configuration Taux (1 ¥ = " + Math.round(data.getExchangeRateCnyToMga()) + " Ar) et Importateur enregistrée",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(Import1688AdminActivity.this, "Erreur : " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadAdminOrders() {
        String query = edtAdminSearch.getText().toString().trim();
        repository.listOrders(true, query, new BaseRepository.Callback<List<ImportOrder>>() {
            @Override
            public void onSuccess(List<ImportOrder> orders) {
                ordersAdapter.submitList(orders);
                if (selectedOrder == null && orders != null && !orders.isEmpty()) {
                    selectOrderForAdminAction(orders.get(0));
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(Import1688AdminActivity.this, "Erreur : " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void selectOrderForAdminAction(ImportOrder order) {
        this.selectedOrder = order;
        cardSelectedOrderActions.setVisibility(View.VISIBLE);
        txtSelectedOrderHeader.setText("Gestion de la commande #" + order.getImportOrderId());
        txtSelectedOrderMeta.setText("Client : " + order.getCustomerName() + " (" + order.getCustomerPhone() + ")\n"
                + "Produit : " + order.getProductName() + " • Qté : " + order.getQuantity());

        edtConfirmPriceCny.setText(String.valueOf(order.getPriceCNY()));
        edtConfirmFeeMga.setText(String.valueOf(Math.round(order.getServiceFeeMGA())));

        String mvolaRef = (order.getPaymentReference() != null && !order.getPaymentReference().isEmpty())
                ? order.getPaymentReference() : "Aucune référence soumise";
        PaymentStatus ps = order.getPaymentStatus() != null ? order.getPaymentStatus() : PaymentStatus.EN_ATTENTE;
        txtAdminMvolaRef.setText("B. Paiement MVola (" + ps.name() + ") • Réf : " + mvolaRef);

        edtSupplierOrderRef.setText(order.getSupplierOrderReference() != null ? order.getSupplierOrderReference() : "");
        double defaultActualCny = order.getActualPurchasePriceCNY() > 0
                ? order.getActualPurchasePriceCNY() : (order.getPriceCNY() * order.getQuantity());
        edtActualPurchaseCny.setText(String.valueOf(defaultActualCny));

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        edtPurchaseDate.setText(order.getPurchaseDate() != null && !order.getPurchaseDate().isEmpty()
                ? order.getPurchaseDate() : today);
    }

    private void confirmSelectedOrderAmount() {
        if (selectedOrder == null) return;
        try {
            double priceCny = Double.parseDouble(edtConfirmPriceCny.getText().toString().trim());
            double feeMga = Double.parseDouble(edtConfirmFeeMga.getText().toString().trim());
            double rate = CurrencyConversionService.getInstance().getExchangeRate();

            repository.confirmOrderAmount(selectedOrder.getImportOrderId(), priceCny, rate, feeMga,
                    new BaseRepository.Callback<ImportOrder>() {
                        @Override
                        public void onSuccess(ImportOrder updated) {
                            selectOrderForAdminAction(updated);
                            loadAdminOrders();
                            Toast.makeText(Import1688AdminActivity.this,
                                    "Montant définitif confirmé : " + PriceFormatter.format(updated.getTotalAmountMGA()),
                                    Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(Import1688AdminActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Montant invalide", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyMvolaPayment(boolean approved) {
        if (selectedOrder == null) return;
        PaymentStatus decision = approved ? PaymentStatus.PAYE : PaymentStatus.ECHOUE;
        String note = approved
                ? "Transaction MVola (" + selectedOrder.getPaymentReference() + ") vérifiée et reçue par l'agence."
                : "Référence MVola introuvable ou montant incorrect. Veuillez vérifier votre transfert.";

        repository.verifyMvolaPayment(selectedOrder.getImportOrderId(), decision, note,
                new BaseRepository.Callback<ImportOrder>() {
                    @Override
                    public void onSuccess(ImportOrder updated) {
                        selectOrderForAdminAction(updated);
                        loadAdminOrders();
                        Toast.makeText(Import1688AdminActivity.this,
                                approved ? "Paiement MVola confirmé (PAYÉ)" : "Paiement MVola refusé (ÉCHOUÉ)",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(Import1688AdminActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void recordReal1688Purchase() {
        if (selectedOrder == null) return;
        String supplierRef = edtSupplierOrderRef.getText().toString().trim();
        if (supplierRef.isEmpty()) {
            Toast.makeText(this, "La référence de commande fournisseur 1688 est obligatoire", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double actualCny = Double.parseDouble(edtActualPurchaseCny.getText().toString().trim());
            String purchaseDate = edtPurchaseDate.getText().toString().trim();
            String note = edtTrackingNote.getText().toString().trim();

            repository.recordReal1688Purchase(selectedOrder.getImportOrderId(), supplierRef, actualCny, purchaseDate, note,
                    new BaseRepository.Callback<ImportOrder>() {
                        @Override
                        public void onSuccess(ImportOrder updated) {
                            selectOrderForAdminAction(updated);
                            loadAdminOrders();
                            Toast.makeText(Import1688AdminActivity.this,
                                    "Achat réel sur 1688 enregistré (ACHAT_EFFECTUE)",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(Import1688AdminActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Prix réel CNY invalide", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTrackingStatus(ImportTrackingStatus status) {
        if (selectedOrder == null) return;
        String note = edtTrackingNote.getText().toString().trim();
        if (note.isEmpty()) {
            note = status.getDescription();
        }

        repository.updateTrackingStatus(selectedOrder.getImportOrderId(), status, note,
                new BaseRepository.Callback<ImportOrder>() {
                    @Override
                    public void onSuccess(ImportOrder updated) {
                        edtTrackingNote.setText("");
                        selectOrderForAdminAction(updated);
                        loadAdminOrders();
                        Toast.makeText(Import1688AdminActivity.this,
                                "Statut mis à jour : " + status.getLabel(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(Import1688AdminActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
