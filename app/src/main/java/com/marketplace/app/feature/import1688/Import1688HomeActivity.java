package com.marketplace.app.feature.import1688;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.marketplace.app.R;
import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.ImportOrderRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.ImportAgencyConfig;
import com.marketplace.app.model.ImportOrder;
import com.marketplace.app.navigation.Router;
import com.marketplace.app.ui.PriceFormatter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Écran d'accueil du module « Import 1688 » (Service d'achat par l'agence sur 1688.com).
 * Permet de :
 * - Consulter le taux de change CNY -> MGA et simuler une conversion rapide
 * - Parcourir 1688.com dans la WebView sécurisée ou coller un lien produit
 * - Recevoir un lien partagé depuis une autre application (ACTION_SEND)
 * - Suivre ses demandes et commandes d'achat 1688
 * - Accéder à l'espace administrateur (protégé par AdminAccessControl)
 */
public class Import1688HomeActivity extends AppCompatActivity {

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s]+)");

    private ImportOrderRepository repository;
    private ImportOrdersAdapter adapter;

    private TextView txtModeStatus;
    private SwitchCompat switchMockMode;
    private TextView txtExchangeRateBadge;
    private TextView txtRateUpdatedAt;
    private EditText edtQuickCny;
    private TextView txtQuickMgaResult;
    private ProgressBar progressOrders;
    private TextView txtOrdersEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import1688_home);

        ensureSessionInMockMode();
        repository = new ImportOrderRepository(ApiClient.isUseMock());

        txtModeStatus = findViewById(R.id.txt_mode_status);
        switchMockMode = findViewById(R.id.switch_mock_mode);
        txtExchangeRateBadge = findViewById(R.id.txt_exchange_rate_badge);
        txtRateUpdatedAt = findViewById(R.id.txt_rate_updated_at);
        edtQuickCny = findViewById(R.id.edt_quick_cny);
        txtQuickMgaResult = findViewById(R.id.txt_quick_mga_result);
        progressOrders = findViewById(R.id.progress_orders);
        txtOrdersEmpty = findViewById(R.id.txt_orders_empty);

        RecyclerView recycler = findViewById(R.id.recycler_import_orders);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ImportOrdersAdapter(order -> Router.openImport1688OrderDetail(this, order.getImportOrderId()));
        recycler.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_home_faq).setOnClickListener(v -> Router.openFaq(this));
        findViewById(R.id.btn_browse_1688).setOnClickListener(v -> Router.openImport1688Browser(this));
        findViewById(R.id.btn_new_request).setOnClickListener(v -> Router.openImport1688Request(this, "", ""));
        findViewById(R.id.btn_admin_space).setOnClickListener(v -> Router.openImport1688Admin(this));
        findViewById(R.id.btn_refresh_orders).setOnClickListener(v -> loadData());

        switchMockMode.setChecked(ApiClient.isUseMock());
        updateModeBanner(ApiClient.isUseMock());
        switchMockMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ApiClient.setUseMock(isChecked);
            ensureSessionInMockMode();
            repository = new ImportOrderRepository(isChecked);
            updateModeBanner(isChecked);
            loadData();
        });

        edtQuickCny.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateQuickConversion();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        handleIncomingSharedIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureSessionInMockMode();
        repository = new ImportOrderRepository(ApiClient.isUseMock());
        switchMockMode.setChecked(ApiClient.isUseMock());
        updateModeBanner(ApiClient.isUseMock());
        loadData();
    }

    private void ensureSessionInMockMode() {
        if (ApiClient.isUseMock() && !AuthSession.getInstance().isLoggedIn()) {
            AuthSession.getInstance().updateSession("mock-buyer-1", "mock-jwt-token");
        }
    }

    private void updateModeBanner(boolean isMock) {
        if (isMock) {
            txtModeStatus.setText("Mode actif : [SIMULÉ] Données locales de démonstration");
        } else {
            txtModeStatus.setText("Mode actif : API REST Réelle (/api/import1688/*)");
        }
    }

    private void updateQuickConversion() {
        String raw = edtQuickCny.getText().toString().trim();
        if (raw.isEmpty()) {
            txtQuickMgaResult.setText("= 0 Ar");
            return;
        }
        try {
            double cny = Double.parseDouble(raw);
            CurrencyConversionService service = CurrencyConversionService.getInstance();
            double prodMga = service.convertProductTotalToMga(cny, 1);
            double feeMga = service.calculateServiceFeeMga(prodMga);
            double totalMga = CurrencyConversionService.calculateAgencyTotalMga(prodMga, feeMga);
            txtQuickMgaResult.setText("≈ " + PriceFormatter.format(totalMga) + " (frais inclus)");
        } catch (NumberFormatException e) {
            txtQuickMgaResult.setText("Montant invalide");
        }
    }

    private void loadData() {
        repository.getAgencyConfig(new BaseRepository.Callback<ImportAgencyConfig>() {
            @Override
            public void onSuccess(ImportAgencyConfig config) {
                CurrencyConversionService service = CurrencyConversionService.getInstance();
                txtExchangeRateBadge.setText("1 ¥ = " + Math.round(service.getExchangeRate()) + " Ar");
                txtRateUpdatedAt.setText("Commission agence : " + config.getDefaultServiceFeePercent()
                        + "% • Mis à jour : " + service.getFormattedUpdateDate());
                updateQuickConversion();
            }

            @Override
            public void onError(String message) {
                CurrencyConversionService service = CurrencyConversionService.getInstance();
                txtExchangeRateBadge.setText("1 ¥ = " + Math.round(service.getExchangeRate()) + " Ar");
                txtRateUpdatedAt.setText("Commission agence : " + service.getConfig().getDefaultServiceFeePercent()
                        + "% • Taux local : " + service.getFormattedUpdateDate());
            }
        });

        progressOrders.setVisibility(View.VISIBLE);
        txtOrdersEmpty.setVisibility(View.GONE);
        repository.listOrders(false, null, new BaseRepository.Callback<List<ImportOrder>>() {
            @Override
            public void onSuccess(List<ImportOrder> data) {
                progressOrders.setVisibility(View.GONE);
                adapter.submitList(data);
                if (data == null || data.isEmpty()) {
                    txtOrdersEmpty.setVisibility(View.VISIBLE);
                    txtOrdersEmpty.setText("Aucune demande d'achat 1688 pour le moment.");
                }
            }

            @Override
            public void onError(String message) {
                progressOrders.setVisibility(View.GONE);
                adapter.submitList(null);
                txtOrdersEmpty.setVisibility(View.VISIBLE);
                txtOrdersEmpty.setText(message + "\nActivez le commutateur [Simulé] en haut pour tester le flux complet Import 1688.");
            }
        });
    }

    private void handleIncomingSharedIntent(Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.trim().isEmpty()) {
                String extractedUrl = extractFirstUrl(sharedText);
                String title = sharedText.replace(extractedUrl, "").trim();
                Toast.makeText(this, "Lien 1688 détecté", Toast.LENGTH_SHORT).show();
                Router.openImport1688Request(this, extractedUrl, title);
            }
        }
    }

    public static String extractFirstUrl(String text) {
        if (text == null) return "";
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return text.trim();
    }
}
