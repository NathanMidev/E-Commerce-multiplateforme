package com.marketplace.core.network;

/**
 * Point d'accès unique au backend pour toute l'application.
 * Initialisé dans MarketplaceApp : serveur réel (PostgreSQL, partagé avec la version web)
 * ou mock hors ligne (démo sans serveur).
 */
public final class BackendProvider {

    private static PostgresBackendApi instance;

    private BackendProvider() {}

    /** Utilise le vrai serveur : l'app et la version web partagent alors les mêmes données. */
    public static synchronized void init(String baseUrl, AuthSession session) {
        RetrofitClient.init(baseUrl, session);
        instance = new PostgresBackendServiceImpl(RetrofitClient.getService());
    }

    /** Mode démo hors ligne : données en mémoire, perdues à la fermeture de l'app. */
    public static synchronized void useMock(AuthSession session) {
        instance = new MockPostgresBackendService(session);
    }

    public static synchronized PostgresBackendApi get() {
        if (instance == null) {
            // Repli si rien n'a été initialisé (tests, aperçus) : mock sans utilisateur connecté.
            instance = new MockPostgresBackendService();
        }
        return instance;
    }

    /** Pour les tests : injecte une implémentation. */
    public static synchronized void setForTesting(PostgresBackendApi api) {
        instance = api;
    }
}
