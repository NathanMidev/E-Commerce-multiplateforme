package com.marketplace.core.model;

import java.io.Serializable;

/**
 * Entité Utilisateur mise à jour pour inclure les contacts directs (WhatsApp, Facebook)
 * et la gestion dynamique des rôles basée sur les performances.
 */
public class User implements Serializable {
    private String id;
    private String name;
    private String email;
    private String avatarUrl;

    // Champs spécifiques style deal.mg
    private String city;          // ville / quartier : public (l'adresse complète reste privée)
    private String address;
    private String whatsapp;
    private String facebookLink;
    private String phoneNumber;

    // Champs de performance pour le rôle dynamique
    private int totalSales;
    private double responseScore; // Score de réactivité (ex: de 0 à 5)
    private long createdAt;

    // Constructeur vide requis pour la désérialisation JSON (Gson)
    public User() {
        this.createdAt = System.currentTimeMillis();
        this.totalSales = 0;
        this.responseScore = 0.0;
    }

    // Constructeur complet
    public User(String id, String name, String email, String avatarUrl) {
        this();
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    // --- LOGIQUE DE RÔLE DYNAMIQUE ---

    /**
     * Calcule le rôle en fonction du volume de vente et de la réactivité.
     * Acheteur : 0 vente
     * Vendeur : > 0 vente
     * Pro : >= 50 ventes ET score de réactivité >= 4.5
     *
     * Volontairement nommée resolveRole() et non getDynamicRole() : un getter serait
     * sérialisé (JSON / base de données) et le niveau serait stocké au lieu d'être calculé.
     */
    public UserRole resolveRole() {
        if (totalSales >= 50 && responseScore >= 4.5) {
            return UserRole.PRO;
        } else if (totalSales > 0) {
            return UserRole.SELLER;
        } else {
            return UserRole.BUYER;
        }
    }

    // --- LOGIQUE D'ACCÈS ---

    public boolean canAccessAr() {
        // Tout utilisateur connecté peut voir la 3D/AR
        return id != null;
    }

    public boolean canSell() {
        // On peut vendre dès qu'on a un profil complété (WhatsApp présent)
        return id != null && whatsapp != null && !whatsapp.isEmpty();
    }

    // --- GETTERS ET SETTERS ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getFacebookLink() { return facebookLink; }
    public void setFacebookLink(String facebookLink) { this.facebookLink = facebookLink; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public int getTotalSales() { return totalSales; }
    public void setTotalSales(int totalSales) { this.totalSales = totalSales; }

    public double getResponseScore() { return responseScore; }
    public void setResponseScore(double responseScore) { this.responseScore = responseScore; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}