package com.marketplace.app.feature.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Rôle : Validation téléphonique internationale multi-indicatifs, formatage national
 * et normalisation stricte au format E.164 (Section 4).
 *
 * Ce numéro WhatsApp unique sert de coordonnée de contact pour :
 * - Les appels téléphoniques (tel:+261XXXXXXXXX)
 * - Les discussions WhatsApp (https://wa.me/261XXXXXXXXX sans le '+')
 *
 * Note d'architecture : Ce numéro est une donnée de contact du profil et n'est pas
 * considéré automatiquement comme un numéro Firebase Authentication vérifié par SMS.
 */
public final class PhoneNormalizer {

    public static final String DEFAULT_DIAL_CODE = "+261";

    public static final class CountryDialOption {
        private final String isoCode;
        private final String countryName;
        private final String flagEmoji;
        private final String dialCode; // ex: "+261"
        private final int minNationalDigits;
        private final int maxNationalDigits;
        private final String nationalRegex;
        private final String exampleHint;

        public CountryDialOption(
                String isoCode,
                String countryName,
                String flagEmoji,
                String dialCode,
                int minNationalDigits,
                int maxNationalDigits,
                String nationalRegex,
                String exampleHint
        ) {
            this.isoCode = isoCode;
            this.countryName = countryName;
            this.flagEmoji = flagEmoji;
            this.dialCode = dialCode;
            this.minNationalDigits = minNationalDigits;
            this.maxNationalDigits = maxNationalDigits;
            this.nationalRegex = nationalRegex;
            this.exampleHint = exampleHint;
        }

        public String getIsoCode() { return isoCode; }
        public String getCountryName() { return countryName; }
        public String getFlagEmoji() { return flagEmoji; }
        public String getDialCode() { return dialCode; }
        public String getDigitsOnlyDialCode() { return dialCode.substring(1); }
        public int getMinNationalDigits() { return minNationalDigits; }
        public int getMaxNationalDigits() { return maxNationalDigits; }
        public String getNationalRegex() { return nationalRegex; }
        public String getExampleHint() { return exampleHint; }

        @Override
        public String toString() {
            return flagEmoji + " " + countryName + " (" + dialCode + ")";
        }
    }

    public static final class ParsedPhone {
        private final CountryDialOption country;
        private final String nationalDigits;
        private final String formattedNational;
        private final String e164;

        public ParsedPhone(CountryDialOption country, String nationalDigits, String formattedNational, String e164) {
            this.country = country;
            this.nationalDigits = nationalDigits;
            this.formattedNational = formattedNational;
            this.e164 = e164;
        }

        public CountryDialOption getCountry() { return country; }
        public String getNationalDigits() { return nationalDigits; }
        public String getFormattedNational() { return formattedNational; }
        public String getE164() { return e164; }
    }

    private static final List<CountryDialOption> SUPPORTED_COUNTRIES;

    static {
        List<CountryDialOption> list = new ArrayList<>();
        // Madagascar (+261) : 9 chiffres commençant par 32 (Orange), 33 (Airtel), 34 (Telma), 37, 38 ou 20
        list.add(new CountryDialOption(
                "MG", "Madagascar", "🇲🇬", "+261",
                9, 9, "^(3[23478]\\d{7}|20\\d{7})$", "34 12 345 67"
        ));
        // France (+33) : 9 chiffres commençant par 1..9 (généralement 6 ou 7 pour mobile)
        list.add(new CountryDialOption(
                "FR", "France", "🇫🇷", "+33",
                9, 9, "^[1-9]\\d{8}$", "6 12 34 56 78"
        ));
        // La Réunion / Mayotte (+262) : 9 chiffres commençant par 692, 693, 639 ou 262
        list.add(new CountryDialOption(
                "RE", "La Réunion / Mayotte", "🇷🇪", "+262",
                9, 9, "^(69[23]\\d{6}|639\\d{6}|26[29]\\d{6})$", "692 12 34 56"
        ));
        // Maurice (+230) : 7 ou 8 chiffres commençant par 2..9 (5XXXXXXX pour mobile)
        list.add(new CountryDialOption(
                "MU", "Maurice", "🇲🇺", "+230",
                7, 8, "^[2-9]\\d{6,7}$", "5123 4567"
        ));
        // Comores (+269) : 7 chiffres commençant par 3, 4 ou 7
        list.add(new CountryDialOption(
                "KM", "Comores", "🇰🇲", "+269",
                7, 7, "^[347]\\d{6}$", "312 34 56"
        ));
        // Chine (+86) : 11 chiffres commençant par 13..19
        list.add(new CountryDialOption(
                "CN", "Chine", "🇨🇳", "+86",
                11, 11, "^1[3-9]\\d{9}$", "138 0013 8000"
        ));
        // États-Unis / Canada (+1) : 10 chiffres (NPA-NXX-XXXX)
        list.add(new CountryDialOption(
                "US", "États-Unis / Canada", "🇺🇸", "+1",
                10, 10, "^[2-9]\\d{2}[2-9]\\d{6}$", "415 555 2671"
        ));
        SUPPORTED_COUNTRIES = Collections.unmodifiableList(list);
    }

    private PhoneNormalizer() {}

    public static List<CountryDialOption> getSupportedCountries() {
        return SUPPORTED_COUNTRIES;
    }

    public static CountryDialOption findCountryByDialCode(String dialCode) {
        if (dialCode == null) return SUPPORTED_COUNTRIES.get(0);
        String clean = dialCode.trim();
        if (!clean.startsWith("+")) clean = "+" + clean;
        for (CountryDialOption option : SUPPORTED_COUNTRIES) {
            if (option.getDialCode().equals(clean)) {
                return option;
            }
        }
        return SUPPORTED_COUNTRIES.get(0);
    }

    /**
     * Valide et normalise un numéro saisi avec un indicatif pays donné vers le format strict E.164 (+261XXXXXXXXX).
     * Retourne null si le numéro est invalide.
     */
    public static String normalizeToE164(String dialCode, String nationalOrFullInput) {
        if (nationalOrFullInput == null || nationalOrFullInput.trim().isEmpty()) {
            return null;
        }
        String raw = nationalOrFullInput.trim();
        // Rejeter toute chaîne contenant des lettres
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetter(c)) {
                return null;
            }
        }

        CountryDialOption country = findCountryByDialCode(dialCode != null ? dialCode : DEFAULT_DIAL_CODE);
        String cleaned = raw.replaceAll("[\\s().-]", "");

        // Si l'utilisateur a collé un numéro international complet (+... ou 00...)
        if (cleaned.startsWith("+") || cleaned.startsWith("00")) {
            String withoutPrefix = cleaned.startsWith("+") ? cleaned.substring(1) : cleaned.substring(2);
            if (!withoutPrefix.matches("^\\d{8,15}$")) {
                return null;
            }
            // Identifier si ce préfixe correspond à un de nos pays connus
            for (CountryDialOption candidate : SUPPORTED_COUNTRIES) {
                String codeDigits = candidate.getDigitsOnlyDialCode();
                if (withoutPrefix.startsWith(codeDigits)) {
                    String nat = withoutPrefix.substring(codeDigits.length());
                    if (nat.startsWith("0") && nat.length() == candidate.getMaxNationalDigits() + 1) {
                        nat = nat.substring(1);
                    }
                    if (Pattern.matches(candidate.getNationalRegex(), nat)) {
                        return candidate.getDialCode() + nat;
                    }
                    return null;
                }
            }
            // Autre pays international conforme ITU-T E.164 (8 à 15 chiffres, ne commençant pas par 0)
            if (withoutPrefix.matches("^[1-9]\\d{7,14}$")) {
                return "+" + withoutPrefix;
            }
            return null;
        }

        // Sinon, saisie nationale dans le pays sélectionné
        if (!cleaned.matches("^\\d+$")) {
            return null;
        }

        String codeDigits = country.getDigitsOnlyDialCode();
        // Si l'utilisateur a saisi 261341234567 sans le '+'
        if (cleaned.startsWith(codeDigits)
                && cleaned.length() == codeDigits.length() + country.getMaxNationalDigits()) {
            cleaned = cleaned.substring(codeDigits.length());
        } else if (cleaned.startsWith("0") && cleaned.length() == country.getMaxNationalDigits() + 1) {
            // Suppression du '0' national initial (ex: 034 12 345 67 -> 341234567, 06 12 34 56 78 -> 612345678)
            cleaned = cleaned.substring(1);
        }

        if (cleaned.length() < country.getMinNationalDigits() || cleaned.length() > country.getMaxNationalDigits()) {
            return null;
        }

        if (!Pattern.matches(country.getNationalRegex(), cleaned)) {
            return null;
        }

        return country.getDialCode() + cleaned;
    }

    /**
     * Retourne un message d'erreur explicite si le numéro WhatsApp est invalide, ou null s'il est valide.
     */
    public static String validatePhoneWithMessage(String dialCode, String nationalInput) {
        if (nationalInput == null || nationalInput.trim().isEmpty()) {
            return "Le numéro WhatsApp est obligatoire";
        }
        CountryDialOption country = findCountryByDialCode(dialCode);
        String e164 = normalizeToE164(country.getDialCode(), nationalInput);
        if (e164 == null) {
            return "Numéro WhatsApp invalide pour " + country.getCountryName()
                    + " (" + country.getDialCode() + "). Exemple attendu : " + country.getExampleHint();
        }
        return null;
    }

    /**
     * Formate un numéro national pour l'affichage pendant la saisie selon le pays choisi.
     */
    public static String formatNationalDisplay(String dialCode, String rawInput) {
        if (rawInput == null) return "";
        String digits = rawInput.replaceAll("\\D", "");
        CountryDialOption country = findCountryByDialCode(dialCode);

        // Retirer l'indicatif s'il a été inclus par erreur
        String codeDigits = country.getDigitsOnlyDialCode();
        if (digits.startsWith(codeDigits) && digits.length() > country.getMaxNationalDigits()) {
            digits = digits.substring(codeDigits.length());
        }
        // Retirer le 0 initial national
        if (digits.startsWith("0") && digits.length() > 1 && ("MG".equals(country.getIsoCode())
                || "FR".equals(country.getIsoCode()) || "RE".equals(country.getIsoCode()))) {
            digits = digits.substring(1);
        }
        if (digits.length() > country.getMaxNationalDigits()) {
            digits = digits.substring(0, country.getMaxNationalDigits());
        }

        switch (country.getIsoCode()) {
            case "MG": // 34 12 345 67 (2 - 2 - 3 - 2)
                return groupDigits(digits, new int[]{2, 2, 3, 2});
            case "FR": // 6 12 34 56 78 (1 - 2 - 2 - 2 - 2)
                return groupDigits(digits, new int[]{1, 2, 2, 2, 2});
            case "RE": // 692 12 34 56 (3 - 2 - 2 - 2)
                return groupDigits(digits, new int[]{3, 2, 2, 2});
            case "MU": // 5123 4567 (4 - 4)
                return groupDigits(digits, new int[]{4, 4});
            case "KM": // 312 34 56 (3 - 2 - 2)
                return groupDigits(digits, new int[]{3, 2, 2});
            case "CN": // 138 0013 8000 (3 - 4 - 4)
                return groupDigits(digits, new int[]{3, 4, 4});
            case "US": // 415 555 2671 (3 - 3 - 4)
                return groupDigits(digits, new int[]{3, 3, 4});
            default:
                return digits;
        }
    }

    private static String groupDigits(String digits, int[] groups) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (int g : groups) {
            if (idx >= digits.length()) break;
            if (sb.length() > 0) sb.append(' ');
            int end = Math.min(idx + g, digits.length());
            sb.append(digits, idx, end);
            idx = end;
        }
        if (idx < digits.length()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(digits.substring(idx));
        }
        return sb.toString();
    }

    /**
     * Analyse un numéro E.164 enregistré (ex: "+261341234567" ou "+33612345678")
     * pour pré-sélectionner le pays et afficher le numéro national formaté dans le formulaire.
     */
    public static ParsedPhone parseStoredPhone(String storedPhone) {
        if (storedPhone == null || storedPhone.trim().isEmpty()) {
            CountryDialOption def = SUPPORTED_COUNTRIES.get(0);
            return new ParsedPhone(def, "", "", "");
        }
        String e164 = normalizeToE164(DEFAULT_DIAL_CODE, storedPhone);
        if (e164 == null) {
            CountryDialOption def = SUPPORTED_COUNTRIES.get(0);
            return new ParsedPhone(def, storedPhone.trim(), storedPhone.trim(), storedPhone.trim());
        }
        for (CountryDialOption option : SUPPORTED_COUNTRIES) {
            if (e164.startsWith(option.getDialCode())) {
                String nat = e164.substring(option.getDialCode().length());
                String formatted = formatNationalDisplay(option.getDialCode(), nat);
                return new ParsedPhone(option, nat, formatted, e164);
            }
        }
        CountryDialOption def = SUPPORTED_COUNTRIES.get(0);
        return new ParsedPhone(def, e164, e164, e164);
    }

    /**
     * Construit l'URI d'appel téléphonique standard (tel:+261XXXXXXXXX) à partir du numéro unique WhatsApp.
     */
    public static String buildTelUri(String phone) {
        String e164 = normalizeToE164(DEFAULT_DIAL_CODE, phone);
        if (e164 == null || e164.isEmpty()) return null;
        return "tel:" + e164;
    }

    /**
     * Extrait les chiffres internationaux sans le '+' pour les liens https://wa.me/261XXXXXXXXX.
     */
    public static String buildWhatsAppDigits(String phone) {
        String e164 = normalizeToE164(DEFAULT_DIAL_CODE, phone);
        if (e164 == null || e164.isEmpty()) return null;
        return e164.startsWith("+") ? e164.substring(1) : e164;
    }
}
