package com.marketplace.core.network;

/**
 * Session de l'utilisateur connecté, fournie par l'application (implémentée avec Firebase Auth).
 * core:network ne dépend pas de Firebase : il demande seulement l'identifiant et le jeton.
 */
public interface AuthSession {

    /** Identifiant de l'utilisateur connecté, ou null s'il n'y en a pas. */
    String getUserId();

    /**
     * Jeton d'identité à envoyer au serveur (en-tête Authorization), ou null si personne n'est connecté.
     * Appelé depuis un thread réseau : peut bloquer.
     */
    String getIdToken() throws Exception;
}
