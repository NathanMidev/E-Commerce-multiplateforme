package com.marketplace.app.backend;

import com.marketplace.app.backend.api.MarketplaceApi;
import com.marketplace.app.feature.auth.FirebaseAuthSession;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Rôle : Client HTTP centralisé (Retrofit / OkHttp) et indicateur de mode mock.
 * Entrée : URL de base et intercepteur d'authentification Bearer Firebase.
 * Sortie : Instance unique de MarketplaceApi.
 */
public final class ApiClient {

    private static MarketplaceApi apiService;
    private static String currentBaseUrl;
    private static boolean useMock = false;

    private ApiClient() {}

    public static synchronized void setUseMock(boolean mock) {
        // Mode mock définitivement désactivé : l'application utilise l'API PostgreSQL
        useMock = false;
    }

    public static synchronized boolean isUseMock() {
        return false;
    }

    public static synchronized String getBaseUrl() {
        return currentBaseUrl != null ? currentBaseUrl : "http://192.168.1.204:8080/api/";
    }

    public static synchronized void init(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl requise");
        }
        currentBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        AuthSession session = AuthSession.getInstance();
                        if (session != null && session.isLoggedIn()) {
                            try {
                                String token = session.getIdToken();
                                if (token == null || token.trim().isEmpty()) {
                                    token = FirebaseAuthSession.fetchFreshToken(false);
                                }
                                if (token != null && !token.trim().isEmpty()) {
                                    request = request.newBuilder()
                                            .header("Authorization", "Bearer " + token)
                                            .build();
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        android.util.Log.i("ApiClient", ">>> SENDING: " + request.method() + " " + request.url());
                        Response response;
                        try {
                            response = chain.proceed(request);
                            android.util.Log.i("ApiClient", "<<< RECEIVED: " + response.code() + " for " + request.url());
                        } catch (IOException e) {
                            android.util.Log.e("ApiClient", "!!! FAILED: " + request.url() + " -> " + e.getClass().getName() + ": " + e.getMessage(), e);
                            throw e;
                        }
                        // En cas d'expiration du jeton ID Firebase (401), forcer son renouvellement une seule fois
                        if (response.code() == 401 && session != null && session.isLoggedIn()
                                && request.header("X-Token-Retried") == null) {
                            try {
                                String refreshed = FirebaseAuthSession.fetchFreshToken(true);
                                if (refreshed != null && !refreshed.trim().isEmpty()) {
                                    response.close();
                                    Request retryReq = request.newBuilder()
                                            .header("Authorization", "Bearer " + refreshed)
                                            .header("X-Token-Retried", "1")
                                            .build();
                                    return chain.proceed(retryReq);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        return response;
                    }
                })
                .build();

        apiService = new Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MarketplaceApi.class);
    }

    public static synchronized MarketplaceApi getApi() {
        if (apiService == null) {
            init("http://192.168.1.204:8080/api/");
        }
        return apiService;
    }

    public static synchronized void setBaseUrl(android.content.Context context, String newBaseUrl) {
        if (newBaseUrl == null || newBaseUrl.trim().isEmpty()) {
            return;
        }
        String cleanUrl = newBaseUrl.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "http://" + cleanUrl;
        }
        if (!cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl + "/";
        }
        if (!cleanUrl.contains("/api/")) {
            cleanUrl = cleanUrl.endsWith("/") ? cleanUrl + "api/" : cleanUrl + "/api/";
        }
        if (context != null) {
            context.getSharedPreferences("bizinevo_network_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString("custom_api_base_url", cleanUrl)
                    .apply();
        }
        init(cleanUrl);
    }

    public static synchronized String resolveConfiguredBaseUrl(android.content.Context context, String defaultUrl) {
        if (context != null) {
            String saved = context.getSharedPreferences("bizinevo_network_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("custom_api_base_url", null);
            if (saved != null && !saved.trim().isEmpty()
                    && !saved.contains("10.0.2.2") && !saved.contains("127.0.0.1") && !saved.contains("localhost")) {
                return saved;
            }
        }
        return defaultUrl;
    }

    public static synchronized void setApiForTesting(MarketplaceApi api) {
        apiService = api;
    }
}
