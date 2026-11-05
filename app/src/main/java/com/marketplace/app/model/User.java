package com.marketplace.app.model;

import com.google.gson.annotations.SerializedName;
import com.marketplace.app.base.BaseModel;
import com.marketplace.app.feature.auth.PhoneNormalizer;
import com.marketplace.app.feature.auth.ProfileValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Entité Utilisateur (profil complet, nom d'utilisateur distinct, photo, numéro unique WhatsApp E.164,
 * lien Facebook et niveau de compte).
 * Entrée : Désérialisation JSON conforme au schéma /api/users (Gson) et enrichissement Firebase/Firestore.
 * Sortie : Validation des champs obligatoires et calcul dynamique du rôle.
 */
public class User extends BaseModel {
    private String name;
    @SerializedName(value = "username", alternate = {"userName"})
    private String username;
    private String email;
    @SerializedName(value = "avatarUrl", alternate = {"photoUrl"})
    private String avatarUrl;
    private String city;
    private String address;
    private String phoneNumber;
    @SerializedName(value = "whatsapp", alternate = {"whatsappNumber"})
    private String whatsapp;
    @SerializedName(value = "facebookLink", alternate = {"facebookUrl"})
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
        String contact = getWhatsapp();
        return getId() != null && !getId().trim().isEmpty()
                && contact != null && !contact.trim().isEmpty();
    }

    /**
     * Détermine si le profil est complété selon les règles métier obligatoires :
     * - Nom complet renseigné (et différent du placeholder générique "Utilisateur")
     * - Nom d'utilisateur unique (@username) défini
     * - Numéro de contact WhatsApp E.164 valide
     * - Ville & Adresse renseignées
     * Les champs photo et Facebook restent facultatifs et ne bloquent jamais la complétion.
     */
    public boolean isProfileComplete() {
        boolean hasName = name != null && !name.trim().isEmpty() && !"Utilisateur".equalsIgnoreCase(name.trim());
        boolean hasUsername = username != null && !username.trim().isEmpty();
        boolean hasPhone = (whatsapp != null && !whatsapp.trim().isEmpty()) || (phoneNumber != null && !phoneNumber.trim().isEmpty());
        boolean hasAddress = address != null && !address.trim().isEmpty();
        return hasName && hasUsername && hasPhone && hasAddress;
    }

    /** Alias uid sur l'identifiant utilisateur existant (Section 7). */
    public String getUid() { return getId(); }
    public void setUid(String uid) { setId(uid); }

    /** Nom complet / displayName (distinct de username). */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return name; }
    public void setDisplayName(String displayName) { this.name = displayName; }

    /** Nom d'utilisateur unique (distinct de displayName / nom complet). */
    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = (username != null && !username.trim().isEmpty())
                ? ProfileValidator.normalizeUsername(username)
                : null;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /** URL de la photo de profil (avec alias photoUrl). */
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPhotoUrl() { return avatarUrl; }
    public void setPhotoUrl(String photoUrl) { this.avatarUrl = photoUrl; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    /**
     * Unique numéro de contact : WhatsApp au format international E.164.
     * Le champ phoneNumber est synchronisé automatiquement avec whatsapp pour que
     * les appels téléphoniques (tel:) et WhatsApp (wa.me) utilisent le même numéro unique.
     */
    public String getPhoneNumber() {
        if (whatsapp != null && !whatsapp.trim().isEmpty()) {
            return whatsapp;
        }
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        if (this.whatsapp == null || this.whatsapp.trim().isEmpty()) {
            this.whatsapp = phoneNumber;
        }
    }

    public String getWhatsapp() {
        if (whatsapp != null && !whatsapp.trim().isEmpty()) {
            return whatsapp;
        }
        return phoneNumber;
    }

    public void setWhatsapp(String whatsapp) {
        String normalized = (whatsapp != null && !whatsapp.trim().isEmpty())
                ? PhoneNormalizer.normalizeToE164(PhoneNormalizer.DEFAULT_DIAL_CODE, whatsapp)
                : null;
        String finalValue = normalized != null ? normalized : whatsapp;
        this.whatsapp = finalValue;
        this.phoneNumber = finalValue;
    }

    public String getWhatsappNumber() { return getWhatsapp(); }
    public void setWhatsappNumber(String whatsappNumber) { setWhatsapp(whatsappNumber); }

    /** Lien du profil Facebook (avec alias facebookUrl). */
    public String getFacebookLink() { return facebookLink; }
    public void setFacebookLink(String facebookLink) { this.facebookLink = facebookLink; }

    public String getFacebookUrl() { return facebookLink; }
    public void setFacebookUrl(String facebookUrl) { this.facebookLink = facebookUrl; }

    public boolean hasFacebookProfile() {
        return facebookLink != null && !facebookLink.trim().isEmpty();
    }

    public boolean hasCustomAvatar() {
        return avatarUrl != null && !avatarUrl.trim().isEmpty()
                && !avatarUrl.contains("via.placeholder.com/150");
    }

    /**
     * Formate l'identité publique du vendeur en combinant le nom complet et le @username s'il est défini.
     */
    public String getFormattedSellerIdentity() {
        boolean hasName = name != null && !name.trim().isEmpty();
        boolean hasUser = username != null && !username.trim().isEmpty();
        if (hasName && hasUser) {
            return name.trim() + " (@" + username.trim() + ")";
        }
        if (hasUser) {
            return "@" + username.trim();
        }
        if (hasName) {
            return name.trim();
        }
        return "Vendeur Marketplace";
    }

    public int getTotalSales() { return totalSales; }
    public void setTotalSales(int totalSales) { this.totalSales = totalSales; }

    public double getResponseScore() { return responseScore; }
    public void setResponseScore(double responseScore) { this.responseScore = responseScore; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}
