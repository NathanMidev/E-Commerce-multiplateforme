package com.marketplace.app.ui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Rôle : Formateur de prix configurable (Ariary par défaut, séparateur de milliers, sans décimales).
 * Entrée : Valeur numérique en double.
 * Sortie : Chaîne formatée (ex: "189 000 Ar").
 */
public final class PriceFormatter {

    private static String defaultCurrency = "Ar";

    private PriceFormatter() {}

    public static void setDefaultCurrency(String currency) {
        if (currency != null && !currency.trim().isEmpty()) {
            defaultCurrency = currency.trim();
        }
    }

    public static String format(double price) {
        return format(price, defaultCurrency);
    }

    public static String format(double price, String currency) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        String curr = (currency != null && !currency.trim().isEmpty()) ? " " + currency.trim() : "";
        return df.format(Math.round(price)) + curr;
    }
}
