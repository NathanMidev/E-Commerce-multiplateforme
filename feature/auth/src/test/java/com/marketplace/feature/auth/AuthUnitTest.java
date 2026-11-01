package com.marketplace.feature.auth;

import com.marketplace.core.model.User;
import com.marketplace.core.model.UserRole;
import com.marketplace.feature.auth.router.AuthModuleRouter;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthUnitTest {

    @Test
    public void testAuthRouterInstance() {
        AuthModuleRouter router = new AuthModuleRouter();
        assertNotNull("Le routeur Auth doit s'instancier isolément sans dépendance", router);
        // Test que le routeur gère un contexte null sans planter
        router.openLogin(null);
        router.openRegister(null);
        router.openProfile(null, "user_123");
        router.navigateToProfileCompletion(null);
        router.navigateToMain(null);
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
