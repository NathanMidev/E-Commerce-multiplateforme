package com.marketplace.app.feature.auth;

import com.marketplace.app.model.User;
import com.marketplace.app.model.UserRole;
import com.marketplace.app.navigation.Router;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Rôle : Test unitaire JVM pour la logique d'authentification et les droits de compte.
 * Entrée : Entité User et Router unique.
 * Sortie : Vérification des seuils de rôles et droits d'accès à l'AR et à la vente.
 */
public class AuthUnitTest {

    @Test
    public void testRouterSafeNullContext() {
        // Le routeur unique gère les contextes null sans planter
        Router.openLogin(null);
        Router.openRegister(null);
        Router.openProfile(null);
        Router.openCatalogue(null);
        Router.openCart(null);
    }

    @Test
    public void testDynamicRoleThresholds() {
        User user = new User("uid1", "Nathan", "nathan@example.mg", "");
        assertEquals("Aucune vente : acheteur", UserRole.BUYER, user.resolveRole());

        user.setTotalSales(3);
        assertEquals("Au moins une vente : vendeur", UserRole.SELLER, user.resolveRole());

        user.setTotalSales(50);
        user.setResponseScore(4.4);
        assertEquals("50 ventes mais réactivité insuffisante : reste vendeur", UserRole.SELLER, user.resolveRole());

        user.setResponseScore(4.5);
        assertEquals("50 ventes et réactivité >= 4.5 : pro", UserRole.PRO, user.resolveRole());
    }

    @Test
    public void testCanSellRequiresAccountAndWhatsapp() {
        User anonymous = new User();
        anonymous.setWhatsapp("+261340000000");
        assertFalse("Sans identifiant (non connecté), pas de vente", anonymous.canSell());

        User user = new User("uid1", "Nathan", "nathan@example.mg", "");
        assertFalse("Profil sans WhatsApp : pas de vente", user.canSell());
        user.setWhatsapp("+261340000000");
        assertTrue("Profil complété : vente autorisée", user.canSell());
    }

    @Test
    public void testCanAccessArRequiresLogin() {
        assertFalse(new User().canAccessAr());
        assertTrue(new User("uid1", "Nathan", "n@x.mg", "").canAccessAr());
    }
}
