package com.marketplace.app.feature.import1688;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.marketplace.app.R;
import com.marketplace.app.navigation.Router;

/**
 * Navigateur WebView sécurisé pour parcourir le catalogue 1688.com.
 *
 * Règles de sécurité appliquées (Section 12 du cahier des charges) :
 * - Uniquement HTTPS autorisé (blocage des schémas non-HTTPS file://, javascript://, http://)
 * - Accès aux fichiers locaux désactivé (setAllowFileAccess(false), setAllowContentAccess(false))
 * - Aucun stockage d'identifiants utilisateur (setSaveFormData(false))
 * - Bouton direct « Importer ce produit » transférant l'URL et le titre vers Import1688RequestActivity.
 */
public class Import1688BrowserActivity extends AppCompatActivity {

    private static final String DEFAULT_1688_URL = "https://m.1688.com/";

    private WebView webView;
    private TextView txtBrowserUrl;
    private ProgressBar progressWebView;
    private String currentTitle = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import1688_browser);

        webView = findViewById(R.id.webview_1688);
        txtBrowserUrl = findViewById(R.id.txt_browser_url);
        progressWebView = findViewById(R.id.progress_webview);
        TextView txtRateReminder = findViewById(R.id.txt_rate_reminder);

        txtRateReminder.setText("Prix affichés en Ariary (Ar) • Transport intl non inclus");

        configureSecureWebView();

        findViewById(R.id.btn_close_browser).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reload).setOnClickListener(v -> webView.reload());

        findViewById(R.id.btn_open_external).setOnClickListener(v -> {
            String url = webView.getUrl() != null ? webView.getUrl() : DEFAULT_1688_URL;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(this, "Impossible d'ouvrir le navigateur externe", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_import_this_product).setOnClickListener(v -> {
            String url = webView.getUrl() != null ? webView.getUrl() : DEFAULT_1688_URL;
            Router.openImport1688Request(this, url, currentTitle);
        });

        webView.loadUrl(DEFAULT_1688_URL);
    }

    /**
     * Injecte un script JavaScript robuste et idempotent dans la WebView 1688.
     * Rôle :
     * 1. Détecte les balises de prix (¥, ￥, price, etc.).
     * 2. Calcule le prix final en MGA = CNY * rate * 1.15 (arrondi sans double conversion grâce à un attribut data-mga-converted).
     * 3. Remplace visuellement le montant en ¥ par son équivalent en Ariary (ex: "74 750 Ar").
     * 4. Gère les fourchettes de prix (ex: "¥ 10.00 - 25.00" -> "7 475 Ar - 18 688 Ar") et les variantes dynamiques via MutationObserver.
     * 5. Ne divulgue jamais le pourcentage ou la formule interne.
     */
    private void injectCurrencyConversionScript(WebView view) {
        if (view == null) return;
        double currentRate = CurrencyConversionService.getInstance().getExchangeRate();
        // Multiplicateur total = Taux * 1.15
        final double totalMultiplier = currentRate * 1.15;

        String js = "(function() {" +
                "   var multiplier = " + totalMultiplier + ";" +
                "   function formatMGA(amount) {" +
                "       return Math.round(amount).toString().replace(/\\B(?=(\\d{3})+(?!\\d))/g, ' ') + ' Ar';" +
                "   }" +
                "   function convertText(text) {" +
                "       if (!text || text.indexOf('Ar') !== -1) return text;" +
                "       // Pattern fourchette ex: ¥10.5 - 20.0 ou ￥ 10 - 20" +
                "       var rangeRegex = /[¥￥]\\s*([0-9]+(?:\\.[0-9]+)?)\\s*[-~至]\\s*([0-9]+(?:\\.[0-9]+)?)/g;" +
                "       if (rangeRegex.test(text)) {" +
                "           return text.replace(rangeRegex, function(match, p1, p2) {" +
                "               var v1 = parseFloat(p1) * multiplier;" +
                "               var v2 = parseFloat(p2) * multiplier;" +
                "               return formatMGA(v1) + ' - ' + formatMGA(v2);" +
                "           });" +
                "       }" +
                "       // Pattern prix simple ex: ¥ 100 ou ￥100.50" +
                "       var singleRegex = /[¥￥]\\s*([0-9]+(?:\\.[0-9]+)?)/g;" +
                "       return text.replace(singleRegex, function(match, p1) {" +
                "           var val = parseFloat(p1) * multiplier;" +
                "           return formatMGA(val);" +
                "       });" +
                "   }" +
                "   function processNode(node) {" +
                "       if (node.nodeType === 3) {" + // Text node
                "           var val = node.nodeValue;" +
                "           if (val && (val.indexOf('¥') !== -1 || val.indexOf('￥') !== -1)) {" +
                "               node.nodeValue = convertText(val);" +
                "           }" +
                "       } else if (node.nodeType === 1) {" + // Element node
                "           if (node.getAttribute && node.getAttribute('data-mga-converted')) return;" +
                "           if (node.tagName === 'SCRIPT' || node.tagName === 'STYLE' || node.tagName === 'INPUT') return;" +
                "           for (var i = 0; i < node.childNodes.length; i++) {" +
                "               processNode(node.childNodes[i]);" +
                "           }" +
                "           if (node.setAttribute) node.setAttribute('data-mga-converted', 'true');" +
                "       }" +
                "   }" +
                "   processNode(document.body);" +
                "   if (!window.__mgaObserverAttached) {" +
                "       window.__mgaObserverAttached = true;" +
                "       var observer = new MutationObserver(function(mutations) {" +
                "           mutations.forEach(function(m) {" +
                "               for (var i = 0; i < m.addedNodes.length; i++) {" +
                "                   processNode(m.addedNodes[i]);" +
                "               }" +
                "               if (m.target && m.target.nodeType === 3) {" +
                "                   var text = m.target.nodeValue;" +
                "                   if (text && (text.indexOf('¥') !== -1 || text.indexOf('￥') !== -1)) {" +
                "                       m.target.nodeValue = convertText(text);" +
                "                   }" +
                "               }" +
                "           });" +
                "       });" +
                "       observer.observe(document.body, { childList: true, subtree: true, characterData: true });" +
                "   }" +
                "})();";

        view.evaluateJavascript(js, null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureSecureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // Sécurité WebView : interdire l'accès aux fichiers et contenus locaux
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSaveFormData(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri == null) return true;
                String scheme = uri.getScheme();
                // N'autoriser que HTTPS
                if (!"https".equalsIgnoreCase(scheme)) {
                    Toast.makeText(Import1688BrowserActivity.this,
                            "Navigation non-HTTPS bloquée par sécurité (" + scheme + ")",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                txtBrowserUrl.setText(url);
                progressWebView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                txtBrowserUrl.setText(url);
                currentTitle = view.getTitle() != null ? view.getTitle() : "";
                progressWebView.setVisibility(View.GONE);
                injectCurrencyConversionScript(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressWebView.setProgress(newProgress);
                if (newProgress >= 100) {
                    progressWebView.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.clearHistory();
            webView.destroy();
        }
        super.onDestroy();
    }
}
