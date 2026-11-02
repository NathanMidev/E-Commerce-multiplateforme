package com.marketplace.app.feature.contact;

import com.marketplace.app.feature.auth.PhoneNormalizer;
import com.marketplace.app.feature.auth.ProfileValidator;

import java.net.URLEncoder;

/**
 * Rôle : Générateur unique de liens de redirection sécurisés vers WhatsApp, Appel téléphonique et Facebook.
 * Entrée : Numéro WhatsApp international unique E.164, lien Facebook, message prérempli.
 * Sortie : URL normalisées https://wa.me/ (sans '+'), tel:+..., https://facebook.com/... et https://m.me/.
 */
public final class ContactLinks {

    private ContactLinks() {}

    /**
     * Normalise un numéro malgache ou international en supprimant espaces, tirets et le + initial
     * pour la construction du lien https://wa.me/ (sans '+').
     */
    public static String normalizePhoneForWhatsapp(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String digits = PhoneNormalizer.buildWhatsAppDigits(raw);
        if (digits != null && !digits.isEmpty()) {
            return digits;
        }
        String s = raw.replaceAll("[\\s().-]", "");
        if (s.startsWith("+")) s = s.substring(1);
        if (s.startsWith("00")) s = s.substring(2);

        if (s.startsWith("03") && s.length() == 10) {
            return "261" + s.substring(1);
        }
        if (s.startsWith("261") && s.length() == 12) {
            return s;
        }
        return s.isEmpty() ? null : s;
    }

    /**
     * Construit l'URI d'appel téléphonique (tel:+261XXXXXXXXX) à partir du numéro unique WhatsApp.
     */
    public static String buildPhoneCallUri(String whatsappOrPhone) {
        if (whatsappOrPhone == null || whatsappOrPhone.trim().isEmpty()) return null;
        String telUri = PhoneNormalizer.buildTelUri(whatsappOrPhone);
        if (telUri != null) return telUri;
        String clean = whatsappOrPhone.trim().replaceAll("[\\s().-]", "");
        return clean.isEmpty() ? null : "tel:" + clean;
    }

    /**
     * Construit l'URL du profil Facebook normalisée (ou null si non renseignée / invalide).
     */
    public static String buildFacebookProfileUrl(String facebookLink) {
        String normalized = ProfileValidator.normalizeFacebookUrl(facebookLink);
        return (normalized != null && !normalized.isEmpty()) ? normalized : null;
    }

    /**
     * Construit le lien WhatsApp wa.me.
     */
    public static String buildWhatsAppUrl(String phone, String title, double price, String appName) {
        String normalized = normalizePhoneForWhatsapp(phone);
        if (normalized == null || normalized.isEmpty()) return null;

        String msg = "Bonjour, je suis intéressé par " + (title != null ? title : "votre article")
                + " vu sur " + (appName != null ? appName : "Marketplace") + ". Est-il toujours disponible ?";
        try {
            return "https://wa.me/" + normalized + "?text=" + URLEncoder.encode(msg, "UTF-8");
        } catch (Exception e) {
            return "https://wa.me/" + normalized;
        }
    }

    /**
     * Extrait l'identifiant d'une page Facebook pour construire l'URL directe Messenger m.me.
     */
    public static String buildMessengerUrl(String facebookLink) {
        if (facebookLink == null || facebookLink.trim().isEmpty()) return null;
        String trimmed = facebookLink.trim();
        if (trimmed.contains("m.me/")) {
            return trimmed.startsWith("http") ? trimmed : "https://" + trimmed;
        }
        String id = extractFacebookId(trimmed);
        if (id != null && !id.isEmpty()) {
            return "https://m.me/" + id;
        }
        return trimmed.startsWith("http") ? trimmed : "https://" + trimmed;
    }

    public static String extractFacebookId(String facebookUrl) {
        if (facebookUrl == null) return null;
        String clean = facebookUrl.replace("https://", "").replace("http://", "")
                .replace("www.", "").replace("m.", "").replace("facebook.com/", "").replace("fb.com/", "");
        int q = clean.indexOf('?');
        if (q != -1) clean = clean.substring(0, q);
        int slash = clean.indexOf('/');
        if (slash != -1) clean = clean.substring(0, slash);
        return clean.trim();
    }
}
