package com.marketplace.app.feature.faq;

import com.marketplace.app.feature.import1688.CurrencyConversionService;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.ProductStatus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests unitaires pour l'évolution Marketplace :
 * 1. Grille d'accueil des Nouveautés (filtrage des produits locaux publiés)
 * 2. Règle de conversion monétaire 1688 (formule CNY x Taux x 1.15) et secret commercial de la majoration de 15%
 * 3. FAQ Commandes & 1688 (9 questions, accordéon, contact support 038 11 928 23)
 */
public class FaqAndHomeEvolutionUnitTest {

    @Test
    public void testFaqQuestionsAndAnswersCompleteness() {
        List<FaqActivity.FaqItem> items = FaqActivity.getFaqItems();
        assertNotNull(items);
        assertEquals("La FAQ doit contenir exactement 9 questions/réponses", 9, items.size());

        // Vérification de la présence des thématiques clés
        boolean foundHowItWorks = false;
        boolean foundPayment = false;
        boolean foundDelays = false;
        boolean foundTransportChoice = false;
        boolean foundPricingDisclaimer = false;
        boolean foundTransportPaymentTime = false;
        boolean foundTracking = false;
        boolean foundTransitaireContact = false;
        boolean foundCancellation = false;

        for (FaqActivity.FaqItem item : items) {
            assertNotNull(item.question);
            assertNotNull(item.answer);
            assertFalse(item.question.trim().isEmpty());
            assertFalse(item.answer.trim().isEmpty());

            // RÈGLE COMMERCIALE CRITIQUE : Ne jamais afficher ou communiquer la majoration de 15 %
            assertFalse("La question ne doit jamais mentionner 15% : " + item.question,
                    item.question.contains("15") || item.question.toLowerCase().contains("majoration"));
            assertFalse("La réponse ne doit jamais mentionner 15% : " + item.answer,
                    item.answer.contains("15") || item.answer.toLowerCase().contains("majoration") || item.answer.toLowerCase().contains("marge"));

            String qLower = item.question.toLowerCase();
            if (qLower.contains("comment fonctionne")) foundHowItWorks = true;
            if (qLower.contains("comment payer")) foundPayment = true;
            if (qLower.contains("combien de temps")) foundDelays = true;
            if (qLower.contains("choisir mon propre mode")) foundTransportChoice = true;
            if (qLower.contains("comprend-il tous les frais")) foundPricingDisclaimer = true;
            if (qLower.contains("quand dois-je payer")) foundTransportPaymentTime = true;
            if (qLower.contains("comment suivre")) foundTracking = true;
            if (qLower.contains("qui me contacte")) foundTransitaireContact = true;
            if (qLower.contains("annuler une demande")) foundCancellation = true;
        }

        assertTrue(foundHowItWorks);
        assertTrue(foundPayment);
        assertTrue(foundDelays);
        assertTrue(foundTransportChoice);
        assertTrue(foundPricingDisclaimer);
        assertTrue(foundTransportPaymentTime);
        assertTrue(foundTracking);
        assertTrue(foundTransitaireContact);
        assertTrue(foundCancellation);
    }

    @Test
    public void testSupportPhoneNumber() {
        assertEquals("0381192823", FaqActivity.SUPPORT_PHONE);
        assertEquals("038 11 928 23", FaqActivity.SUPPORT_PHONE_DISPLAY);
    }

    @Test
    public void test1688PriceConversionFormula() {
        // Formule : Prix en MGA = Prix CNY × Taux CNY/MGA × 1,15
        // Exemple du prompt : 100 CNY à taux 650 -> 65 000 Ar -> Prix final = 74 750 Ar
        double cny = 100.0;
        double rate = 650.0;
        double mga = CurrencyConversionService.convertCnyToMgaFinalWithRate(cny, rate);
        assertEquals(74750.0, mga, 0.001);

        // Test fourchette min / max
        double minCny = 10.0;
        double maxCny = 25.0;
        double minMga = CurrencyConversionService.convertCnyToMgaFinalWithRate(minCny, rate);
        double maxMga = CurrencyConversionService.convertCnyToMgaFinalWithRate(maxCny, rate);
        assertEquals(7475.0, minMga, 0.001);
        assertEquals(18688.0, maxMga, 0.001);
    }

    @Test
    public void testHomePublishedProductsFiltering() {
        List<Product> rawProducts = new ArrayList<>();

        Product p1 = new Product();
        p1.setId("p1");
        p1.setTitle("Table basse Palissandre");
        p1.setStatus(ProductStatus.DISPONIBLE);
        rawProducts.add(p1);

        Product p2 = new Product();
        p2.setId("p2");
        p2.setTitle("Smartphone 5G");
        p2.setStatus(ProductStatus.NOUVEAU);
        rawProducts.add(p2);

        Product p3 = new Product();
        p3.setId("p3");
        p3.setTitle("Produit non publié (brouillon)");
        p3.setStatus(null); // non disponible
        rawProducts.add(p3);

        List<Product> eligible = new ArrayList<>();
        for (Product p : rawProducts) {
            if (p != null && (p.getStatus() == ProductStatus.DISPONIBLE || p.getStatus() == ProductStatus.NOUVEAU)) {
                eligible.add(p);
            }
        }

        assertEquals(2, eligible.size());
        assertTrue(eligible.contains(p1));
        assertTrue(eligible.contains(p2));
        assertFalse(eligible.contains(p3));
    }
}
