package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Entité Utilisateur (profil, contacts directs, niveau de compte).
 * Entrée : Désérialisation JSON conforme au schéma /api/users (Gson).
 * Sortie : Validation des champs obligatoires et calcul dynamique du rôle.
 */
public class User extends BaseModel {
    private String name;
    private String email;
    private String avatarUrl;
    private String city;
    private String address;
    private String phoneNumber;
    private String whatsapp;
    private String facebookLink;
    private int totalSales;
    private double responseScore;
    private String level; // BUYER, SELLER, PRO retourné par l'API

    public User() {
        super();
        this.totalSales = 0;
        this.responseScore = 0.0;
    }

    public User(String id, String name, String email, String avatarUrl) {
        super(id);
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.totalSales = 0;
        this.responseScore = 0.0;
    }

    public User(String id, String name, String email, String avatarUrl, String city,
                String address, String phoneNumber, String whatsapp, String facebookLink,
                int totalSales, double responseScore, String level) {
        super(id);
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.city = city;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.whatsapp = whatsapp;
        this.facebookLink = facebookLink;
        this.totalSales = totalSales;
        this.responseScore = responseScore;
        this.level = level;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            errors.add("Le nom est obligatoire");
        }
        return errors;
    }

    public UserRole resolveRole() {
        if (level != null && !level.isEmpty()) {
            try {
                return UserRole.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        if (totalSales >= 50 && responseScore >= 4.5) {
            return UserRole.PRO;
        } else if (totalSales > 0) {
            return UserRole.SELLER;
        } else {
            return UserRole.BUYER;
        }
    }

    public boolean canAccessAr() {
        return getId() != null && !getId().trim().isEmpty();
    }

    public boolean canSell() {
        return getId() != null && whatsapp != null && !whatsapp.trim().isEmpty();
    }

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

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getFacebookLink() { return facebookLink; }
    public void setFacebookLink(String facebookLink) { this.facebookLink = facebookLink; }

    public int getTotalSales() { return totalSales; }
    public void setTotalSales(int totalSales) { this.totalSales = totalSales; }

    public double getResponseScore() { return responseScore; }
    public void setResponseScore(double responseScore) { this.responseScore = responseScore; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}
