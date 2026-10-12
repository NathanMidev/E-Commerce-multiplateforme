package com.marketplace.feature.auth;

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
    }
}
