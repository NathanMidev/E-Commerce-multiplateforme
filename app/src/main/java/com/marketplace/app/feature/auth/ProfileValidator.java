package com.marketplace.app.feature.auth;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rôle : Validation stricte du Nom d'utilisateur (username, distinct du nom complet/displayName)
 * et du Lien du profil Facebook (Sections 3 & 5).
 */
public final class ProfileValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9_.]*[a-z0-9])?$");
    private static final Set<String> RESERVED_USERNAMES = new HashSet<>(Arrays.asList(
            "admin", "root", "support", "marketplace", "system", "api", "null", "undefined"
    ));

    private static final Set<String> FACEBOOK_DOMAINS = new HashSet<>(Arrays.asList(
            "facebook.com", "fb.com", "fb.me"
    ));

    private ProfileValidator() {}

    /**
     * Normalise un nom d'utilisateur (retire le '@' initial éventuel, trim et met en minuscules).
     */
    public static String normalizeUsername(String rawUsername) {
        if (rawUsername == null) return "";
        String s = rawUsername.trim();
        if (s.startsWith("@")) {
            s = s.substring(1).trim();
        }
        return s.toLowerCase(Locale.ROOT);
    }

    /**
     * Valide le format d'un nom d'utilisateur (username).
     * Règles :
     * - 3 à 30 caractères
     * - Caractères alphanumériques minuscules, '.' ou '_'
     * - Ne commence ni ne finit par '.' ou '_'
     * - Pas de '..' consécutifs
     * - Non réservé
     *
     * @return null si valide, sinon le message d'erreur explicite.
     */
    public static String validateUsername(String rawUsername) {
        String normalized = normalizeUsername(rawUsername);
        if (normalized.isEmpty()) {
            return "Le nom d'utilisateur (username) est obligatoire";
        }
        if (normalized.length() < 3) {
            return "Le nom d'utilisateur doit contenir au moins 3 caractères";
        }
        if (normalized.length() > 30) {
            return "Le nom d'utilisateur ne doit pas dépasser 30 caractères";
        }
        if (normalized.contains("..")) {
            return "Le nom d'utilisateur ne peut pas contenir deux points consécutifs (..)";
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            return "Format invalide : utilisez uniquement des lettres, chiffres, points (.) ou tirets bas (_)";
        }
        if (RESERVED_USERNAMES.contains(normalized)) {
            return "Ce nom d'utilisateur est réservé par le système";
        }
        return null;
    }

    /**
     * Normalise une URL de profil Facebook.
     * - Si vide ou null, retourne "" (champ facultatif / suppression du lien).
     * - Si l'utilisateur saisit "facebook.com/mon.profil" ou "fb.me/mon.profil" sans schéma, ajoute "https://".
     * - Si l'utilisateur saisit "m.me/mon.profil", le convertit en "https://facebook.com/mon.profil" pour compatibilité backend.
     * - Retourne null si l'URL n'est pas une URL de profil Facebook valide.
     */
    public static String normalizeFacebookUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return "";
        }
        String trimmed = rawUrl.trim();
        if (trimmed.contains(" ")) {
            return null;
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return null;
            }
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                return null;
            }
            String lowerHost = host.toLowerCase(Locale.ROOT);

            // Accepter m.me/identifiant en le normalisant vers https://facebook.com/identifiant (compatible backend)
            if ("m.me".equals(lowerHost) || lowerHost.endsWith(".m.me")) {
                String path = uri.getPath();
                if (path == null || path.replace("/", "").trim().isEmpty()) {
                    return null;
                }
                return "https://facebook.com" + (path.startsWith("/") ? path : "/" + path);
            }

            boolean validFbDomain = false;
            for (String domain : FACEBOOK_DOMAINS) {
                if (lowerHost.equals(domain) || lowerHost.endsWith("." + domain)) {
                    validFbDomain = true;
                    break;
                }
            }
            if (!validFbDomain) {
                return null;
            }

            String path = uri.getPath();
            String query = uri.getQuery();
            boolean hasPathSegment = path != null && !path.replace("/", "").trim().isEmpty();
            boolean isProfilePhpWithId = "/profile.php".equalsIgnoreCase(path)
                    && query != null && query.contains("id=");
            if (!hasPathSegment && !isProfilePhpWithId) {
                return null;
            }
            if ("/profile.php".equalsIgnoreCase(path) && !isProfilePhpWithId) {
                return null;
            }

            // Forcer https:// pour la sécurité
            if ("http".equalsIgnoreCase(scheme)) {
                trimmed = "https://" + trimmed.substring(7);
            }
            return trimmed;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Valide un lien Facebook facultatif.
     *
     * @return null si valide (ou vide), sinon un message d'erreur explicite.
     */
    public static String validateFacebookUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return null; // Champ facultatif
        }
        String normalized = normalizeFacebookUrl(rawUrl);
        if (normalized == null) {
            return "Lien Facebook invalide. Exemples acceptés : https://facebook.com/votre.profil ou https://fb.com/votre.boutique";
        }
        return null;
    }
}
