package com.marketplace.app.feature.contact;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Rôle : Test unitaire JVM pour la construction des liens de contact vendeur.
 * Entrée : Numéros malgaches avec espaces, caractères spéciaux et liens Facebook.
 * Sortie : Validation des formats wa.me, m.me et encodage UTF-8.
 */
public class ContactLinksTest {

    @Test
    public void testNormalizeMalagasyPhone() {
        assertEquals("261341234567", ContactLinks.normalizePhoneForWhatsapp("034 12 345 67"));
        assertEquals("261341234567", ContactLinks.normalizePhoneForWhatsapp("+261 34 12 345 67"));
        assertEquals("261341234567", ContactLinks.normalizePhoneForWhatsapp("00261341234567"));
        assertEquals("261331122334", ContactLinks.normalizePhoneForWhatsapp("033.11.223.34"));
    }

    @Test
    public void testBuildWhatsAppUrlEncoding() {
        String url = ContactLinks.buildWhatsAppUrl("0341234567", "Table à manger", 150000, "Marketplace");
        assertNotNull(url);
        assertTrue(url.startsWith("https://wa.me/261341234567?text="));
        assertTrue(url.contains("Bonjour"));
        assertTrue(url.contains("Table"));
    }

    @Test
    public void testBuildMessengerUrlExtraction() {
        assertEquals("https://m.me/boutique.officielle",
                ContactLinks.buildMessengerUrl("https://www.facebook.com/boutique.officielle?ref=ts"));

        assertEquals("https://m.me/seller123",
                ContactLinks.buildMessengerUrl("facebook.com/seller123"));

        assertNull(ContactLinks.buildMessengerUrl(null));
    }
}
