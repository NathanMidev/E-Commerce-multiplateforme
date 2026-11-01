package com.marketplace.core.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Client HTTP vers l'API. Chaque requête reçoit automatiquement le jeton Firebase de l'utilisateur
 * connecté (en-tête Authorization: Bearer ...). À initialiser une fois, dans MarketplaceApp.
 *
 * Adresse du serveur :
 *   - émulateur Android : http://10.0.2.2:8080/api/   (10.0.2.2 = le PC hôte)
 *   - vrai téléphone    : http://IP-DU-PC:8080/api/   (même réseau Wi-Fi), ou l'URL HTTPS du serveur en ligne
 */
public final class RetrofitClient {

    private static RetrofitService service;

    private RetrofitClient() {}

    public static synchronized void init(String baseUrl, final AuthSession session) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl requise");
        }
        String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        if (session != null) {
                            try {
                                // On récupère le token à chaque requête pour éviter les jetons périmés
                                String token = session.getIdToken();
                                if (token != null) {
                                    request = request.newBuilder()
                                            .header("Authorization", "Bearer " + token)
                                            .build();
                                }
                            } catch (Exception e) {
                                // Pas de jeton : la requête part sans, le serveur répondra 401 si nécessaire.
                            }
                        }
                        return chain.proceed(request);
                    }
                })
                .build();

        service = new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RetrofitService.class);
    }

    public static synchronized RetrofitService getService() {
        if (service == null) {
            throw new IllegalStateException("RetrofitClient.init(...) n'a pas été appelé (voir MarketplaceApp)");
        }
        return service;
    }
}
