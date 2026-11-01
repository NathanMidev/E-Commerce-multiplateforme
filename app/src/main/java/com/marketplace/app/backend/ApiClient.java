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
        useMock = mock;
    }

    public static synchronized boolean isUseMock() {
        return useMock;
    }

    public static synchronized String getBaseUrl() {
        return currentBaseUrl != null ? currentBaseUrl : "http://10.0.2.2:8080/api/";
    }

    public static synchronized void init(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl requise");
        }
        currentBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        AuthSession session = AuthSession.getInstance();
                        if (session != null && session.isLoggedIn()) {
                            try {
                                String token = session.getIdToken();
                                if (token == null || token.trim().isEmpty()) {
                                    token = FirebaseAuthSession.fetchFreshToken();
                                }
                                if (token != null && !token.trim().isEmpty()) {
                                    request = request.newBuilder()
                                            .header("Authorization", "Bearer " + token)
                                            .build();
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        return chain.proceed(request);
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
            init("http://10.0.2.2:8080/api/");
        }
        return apiService;
    }

    public static synchronized void setApiForTesting(MarketplaceApi api) {
        apiService = api;
    }
}
