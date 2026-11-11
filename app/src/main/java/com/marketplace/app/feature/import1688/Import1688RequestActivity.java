package com.marketplace.app.feature.import1688;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.marketplace.app.model.ImportOrder;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;
import com.marketplace.app.ui.PriceFormatter;

import java.util.List;

/**
 * Formulaire de soumission d'une demande d'achat sur 1688 par l'agence (Sections 13 & 14).
 * Calcule en temps réel la conversion CNY (¥) -> Ariary (MGA) et les frais de service de l'agence.
 */
public class Import1688RequestActivity extends AppCompatActivity {

    private ImportOrderRepository repository;

    private EditText edtProductUrl;
    private EditText edtProductName;
    private EditText edtProductImage;
    private EditText edtProductVariant;
    private EditText edtPriceCny;
    private EditText edtQuantity;
    private EditText edtNotes;
    private EditText edtCustomerName;
    private EditText edtCustomerPhone;

    private TextView txtSummaryRate;
    private TextView txtSummaryCny;
    private TextView txtSummaryConvertedMga;
    private TextView txtSummaryServiceFee;
    private TextView txtSummaryTotalMga;
    private TextView txtSummaryDisclaimer;
    private MaterialButton btnSubmitRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import1688_request);

        repository = new ImportOrderRepository();

        edtProductUrl = findViewById(R.id.edt_product_url);
        edtProductName = findViewById(R.id.edt_product_name);
        edtProductImage = findViewById(R.id.edt_product_image);
        edtProductVariant = findViewById(R.id.edt_product_variant);
        edtPriceCny = findViewById(R.id.edt_price_cny);
        edtQuantity = findViewById(R.id.edt_quantity);
        edtNotes = findViewById(R.id.edt_notes);
        edtCustomerName = findViewById(R.id.edt_customer_name);
        edtCustomerPhone = findViewById(R.id.edt_customer_phone);

        txtSummaryRate = findViewById(R.id.txt_summary_rate);
        txtSummaryCny = findViewById(R.id.txt_summary_cny);
        txtSummaryConvertedMga = findViewById(R.id.txt_summary_converted_mga);
        txtSummaryServiceFee = findViewById(R.id.txt_summary_service_fee);
        txtSummaryTotalMga = findViewById(R.id.txt_summary_total_mga);
        txtSummaryDisclaimer = findViewById(R.id.txt_summary_disclaimer);
        btnSubmitRequest = findViewById(R.id.btn_submit_request);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_paste_url).setOnClickListener(v -> pasteUrlFromClipboard());

        // Pré-remplissage depuis le navigateur 1688 ou un lien partagé
        String prefillUrl = getIntent().getStringExtra(Router.EXTRA_IMPORT_PRODUCT_URL);
        String prefillTitle = getIntent().getStringExtra(Router.EXTRA_IMPORT_PRODUCT_NAME);
        if (prefillUrl != null && !prefillUrl.isEmpty()) {
            edtProductUrl.setText(prefillUrl);
        }
        if (prefillTitle != null && !prefillTitle.isEmpty()) {
            edtProductName.setText(prefillTitle);
        }

        // Pré-remplissage utilisateur connecté
        AuthSession session = AuthSession.getInstance();
        User currentUser = session.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.getName() != null && !currentUser.getName().isEmpty()) {
                edtCustomerName.setText(currentUser.getName());
            }
            if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
                edtCustomerPhone.setText(currentUser.getPhoneNumber());
            }
        }

        TextWatcher calculatorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateBreakdown();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        edtPriceCny.addTextChangedListener(calculatorWatcher);
        edtQuantity.addTextChangedListener(calculatorWatcher);

        txtSummaryDisclaimer.setText(CurrencyConversionService.TRANSPORT_DISCLAIMER);
        recalculateBreakdown();

        btnSubmitRequest.setOnClickListener(v -> submitImportRequest());
    }

    private void pasteUrlFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            if (item != null && item.getText() != null) {
                String extracted = Import1688HomeActivity.extractFirstUrl(item.getText().toString());
                edtProductUrl.setText(extracted);
                Toast.makeText(this, "Lien collé", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void recalculateBreakdown() {
        CurrencyConversionService service = CurrencyConversionService.getInstance();
        txtSummaryRate.setText("Taux : 1 CNY = " + Math.round(service.getExchangeRate())
                + " Ar • Commission service agence : " + service.getConfig().getDefaultServiceFeePercent()
                + "% (Mis à jour : " + service.getFormattedUpdateDate() + ")");

        double unitPriceCNY = parseDoubleSafe(edtPriceCny.getText().toString());
        int quantity = parseIntSafe(edtQuantity.getText().toString(), 1);
        if (quantity <= 0) quantity = 1;

        double totalCNY = unitPriceCNY * quantity;
        double convertedMGA = service.convertProductTotalToMga(unitPriceCNY, quantity);
        double serviceFeeMGA = service.calculateServiceFeeMga(convertedMGA);
        double totalMGA = CurrencyConversionService.calculateAgencyTotalMga(convertedMGA, serviceFeeMGA);

        txtSummaryCny.setText("Prix du produit en CNY : " + CurrencyConversionService.formatCny(totalCNY)
                + " (" + quantity + " × " + CurrencyConversionService.formatCny(unitPriceCNY) + ")");
        txtSummaryConvertedMga.setText("Prix converti en Ariary : " + PriceFormatter.format(convertedMGA));
        txtSummaryServiceFee.setText("Frais de service d'achat de l'agence : " + PriceFormatter.format(serviceFeeMGA));
        txtSummaryTotalMga.setText("Montant total à payer à l'agence : " + PriceFormatter.format(totalMGA));
    }

    private void submitImportRequest() {
        if (ApiClient.isUseMock() && !AuthSession.getInstance().isLoggedIn()) {
            AuthSession.getInstance().updateSession("mock-buyer-1", "mock-jwt-token");
        } else if (!AuthSession.getInstance().isLoggedIn()) {
            Toast.makeText(this, "Veuillez vous connecter pour soumettre une demande d'achat 1688", Toast.LENGTH_SHORT).show();
            Router.openLogin(this);
            return;
        }

        String url = edtProductUrl.getText().toString().trim();
        String name = edtProductName.getText().toString().trim();
        String image = edtProductImage.getText().toString().trim();
        String variant = edtProductVariant.getText().toString().trim();
        double priceCny = parseDoubleSafe(edtPriceCny.getText().toString());
        int quantity = parseIntSafe(edtQuantity.getText().toString(), 0);
        String notes = edtNotes.getText().toString().trim();
        String customerName = edtCustomerName.getText().toString().trim();
        String customerPhone = edtCustomerPhone.getText().toString().trim();

        CurrencyConversionService service = CurrencyConversionService.getInstance();
        double convertedMGA = service.convertProductTotalToMga(priceCny, Math.max(1, quantity));
        double serviceFeeMGA = service.calculateServiceFeeMga(convertedMGA);
        double totalMGA = CurrencyConversionService.calculateAgencyTotalMga(convertedMGA, serviceFeeMGA);

        ImportOrder order = new ImportOrder();
        order.setUserId(AuthSession.getInstance().getUserId());
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setProductUrl(url);
        order.setProductName(name);
        order.setProductImageUrl(image);
        order.setProductVariant(variant.isEmpty() ? "Standard" : variant);
        order.setPriceCNY(priceCny);
        order.setQuantity(quantity);
        order.setExchangeRate(service.getExchangeRate());
        order.setEstimatedPriceMGA(convertedMGA);
        order.setServiceFeeMGA(serviceFeeMGA);
        order.setTotalAmountMGA(totalMGA);
        order.setNotes(notes);

        List<String> errors = order.validate();
        if (!errors.isEmpty()) {
            Toast.makeText(this, errors.get(0), Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmitRequest.setEnabled(false);
        btnSubmitRequest.setText("Envoi en cours...");

        repository.create(order, new BaseRepository.Callback<ImportOrder>() {
            @Override
            public void onSuccess(ImportOrder created) {
                btnSubmitRequest.setEnabled(true);
                btnSubmitRequest.setText("Soumettre ma demande d'achat 1688");
                Toast.makeText(Import1688RequestActivity.this,
                        "Demande d'achat enregistrée : " + created.getImportOrderId(),
                        Toast.LENGTH_LONG).show();
                Router.openImport1688OrderDetail(Import1688RequestActivity.this, created.getImportOrderId());
                finish();
            }

            @Override
            public void onError(String message) {
                btnSubmitRequest.setEnabled(true);
                btnSubmitRequest.setText("Soumettre ma demande d'achat 1688");
                Toast.makeText(Import1688RequestActivity.this,
                        message + "\nAstuce : activez le mode [Simulé] sur l'accueil Import 1688 si le backend n'implémente pas encore cette route.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private double parseDoubleSafe(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int parseIntSafe(String val, int fallback) {
        if (val == null || val.trim().isEmpty()) return fallback;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
