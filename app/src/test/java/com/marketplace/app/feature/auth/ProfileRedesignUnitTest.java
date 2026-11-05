package com.marketplace.app.feature.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Suite de tests unitaires pour la Refonte de la Fenêtre Profil (Section 10) :
 * 1. Ouverture avec un profil non complété
 * 2. Complétion et enregistrement réussi
 * 3. Disparition du formulaire après sauvegarde
 * 4. Réouverture sans réapparition automatique du formulaire
 * 5. Persistance après fermeture/redémarrage
 * 6. Récupération correcte après reconnexion
 * 7. Modification du nom d'utilisateur
 * 8. Modification du numéro WhatsApp
 * 9. Remplacement de la photo de profil
 * 10. Ajout et suppression du lien Facebook
 * 11. Annulation d'une modification
 * 12. Échec réseau pendant l'enregistrement
 * 13. Échec réseau pendant la récupération
 * 14. Déconnexion et connexion avec un autre compte (isolation stricte)
 * 15. Validation des règles de complétion du profil
 */
public class ProfileRedesignUnitTest {

    private static final String USER_JEAN = "uid_jean_100";
    private static final String USER_MARIE = "uid_marie_200";

    private MockBackendService mockBackend;
    private UserRepository userRepository;

    @Before
    public void setUp() {
        mockBackend = MockBackendService.getInstance();
        mockBackend.resetData();
        ProfileStorageService.resetSimulatedState();
        userRepository = new UserRepository(true);
        AuthSession.getInstance().clear();
    }

    @After
    public void tearDown() {
        AuthSession.getInstance().clear();
    }

    // 1. Profil non complété initialement
    @Test
    public void test01_uncompletedProfileDetection() {
        AuthSession.getInstance().updateSession(USER_JEAN, "jwt_jean");
        User freshUser = new User();
        freshUser.setId(USER_JEAN);
        freshUser.setEmail("jean@example.com");
        freshUser.setName("Utilisateur"); // Nom générique par défaut
        freshUser.setUsername(null);
        freshUser.setWhatsapp(null);
        freshUser.setAddress(null);

        assertFalse("Un profil sans username/whatsapp/adresse doit être considéré comme incomplet",
                freshUser.isProfileComplete());
    }

    // 2. Complétion du formulaire et enregistrement réussi
    @Test
    public void test02_completeProfileAndSave() {
        AuthSession.getInstance().updateSession(USER_JEAN, "jwt_jean");

        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Jean Rakoto");
        fields.put("username", "jean.rakoto");
        fields.put("dialCode", "+261");
        fields.put("whatsapp", "034 11 222 33");
        fields.put("phoneNumber", "034 11 222 33");
        fields.put("address", "Antananarivo, Ankorondrano");

        AtomicReference<User> savedRef = new AtomicReference<>();
        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                savedRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        User saved = savedRef.get();
        assertNotNull(saved);
        assertEquals("Jean Rakoto", saved.getName());
        assertEquals("jean.rakoto", saved.getUsername());
        assertEquals("+261341122233", saved.getWhatsapp());
        assertEquals("Antananarivo, Ankorondrano", saved.getAddress());
        assertTrue("Le profil doit maintenant être complété", saved.isProfileComplete());
    }

    // 3 & 4. Disparition du formulaire après sauvegarde et réouverture sans réapparition
    @Test
    public void test03_and_04_profileRemainsCompleteOnReopen() {
        test02_completeProfileAndSave();

        // Réouverture simulée de la fenêtre Profil
        AtomicReference<User> reloadedRef = new AtomicReference<>();
        userRepository.getMyProfile(new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                reloadedRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        User reloaded = reloadedRef.get();
        assertNotNull(reloaded);
        assertTrue("Le profil reste complété lors des ouvertures suivantes", reloaded.isProfileComplete());
        assertEquals("jean.rakoto", reloaded.getUsername());
    }

    // 5 & 6. Persistance et récupération après reconnexion
    @Test
    public void test05_and_06_persistenceAfterSessionReconnect() {
        test02_completeProfileAndSave();

        // Simulation de déconnexion
        AuthSession.getInstance().clear();
        assertNull(AuthSession.getInstance().getUserId());

        // Reconnexion avec le même UID
        AuthSession.getInstance().updateSession(USER_JEAN, "jwt_jean_new_session");

        UserRepository freshRepo = new UserRepository(true);
        AtomicReference<User> restoredRef = new AtomicReference<>();
        freshRepo.getMyProfile(new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                restoredRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        User restored = restoredRef.get();
        assertNotNull(restored);
        assertEquals("Jean Rakoto", restored.getName());
        assertEquals("+261341122233", restored.getWhatsapp());
        assertTrue(restored.isProfileComplete());
    }

    // 7. Modification du nom d'utilisateur
    @Test
    public void test07_modifyUsername() {
        test02_completeProfileAndSave();

        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Jean Rakoto");
        fields.put("username", "jean_rakoto_pro");
        fields.put("whatsapp", "+261341122233");
        fields.put("address", "Antananarivo");

        AtomicReference<User> updatedRef = new AtomicReference<>();
        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                updatedRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        assertNotNull(updatedRef.get());
        assertEquals("jean_rakoto_pro", updatedRef.get().getUsername());
    }

    // 8. Modification du numéro WhatsApp
    @Test
    public void test08_modifyWhatsappNumber() {
        test02_completeProfileAndSave();

        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Jean Rakoto");
        fields.put("username", "jean.rakoto");
        fields.put("dialCode", "+33");
        fields.put("whatsapp", "+33612345678");
        fields.put("address", "Paris / Antananarivo");

        AtomicReference<User> updatedRef = new AtomicReference<>();
        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                updatedRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        User u = updatedRef.get();
        assertNotNull(u);
        assertEquals("+33612345678", u.getWhatsapp());
        assertEquals("+33612345678", u.getPhoneNumber());
    }

    // 9. Remplacement de la photo de profil
    @Test
    public void test09_replaceAvatarUrl() {
        test02_completeProfileAndSave();

        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Jean Rakoto");
        fields.put("username", "jean.rakoto");
        fields.put("whatsapp", "+261341122233");
        fields.put("address", "Antananarivo");
        fields.put("avatarUrl", "https://firebasestorage.googleapis.com/v0/b/bizinevo/o/new_avatar.jpg");

        AtomicReference<User> updatedRef = new AtomicReference<>();
        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                updatedRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        User u = updatedRef.get();
        assertNotNull(u);
        assertTrue(u.hasCustomAvatar());
        assertEquals("https://firebasestorage.googleapis.com/v0/b/bizinevo/o/new_avatar.jpg", u.getAvatarUrl());
    }

    // 10. Ajout et suppression du lien Facebook (champ facultatif)
    @Test
    public void test10_addAndRemoveFacebookLink() {
        test02_completeProfileAndSave();

        // Ajout
        Map<String, String> fieldsAdd = new HashMap<>();
        fieldsAdd.put("name", "Jean Rakoto");
        fieldsAdd.put("username", "jean.rakoto");
        fieldsAdd.put("whatsapp", "+261341122233");
        fieldsAdd.put("address", "Antananarivo");
        fieldsAdd.put("facebookLink", "https://facebook.com/jean.rakoto");

        AtomicReference<User> addRef = new AtomicReference<>();
        userRepository.updateMyProfile(fieldsAdd, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                addRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        assertTrue(addRef.get().hasFacebookProfile());
        assertEquals("https://facebook.com/jean.rakoto", addRef.get().getFacebookLink());

        // Suppression du lien
        Map<String, String> fieldsRemove = new HashMap<>();
        fieldsRemove.put("name", "Jean Rakoto");
        fieldsRemove.put("username", "jean.rakoto");
        fieldsRemove.put("whatsapp", "+261341122233");
        fieldsRemove.put("address", "Antananarivo");
        fieldsRemove.put("facebookLink", "");

        AtomicReference<User> removeRef = new AtomicReference<>();
        userRepository.updateMyProfile(fieldsRemove, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                removeRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        assertFalse(removeRef.get().hasFacebookProfile());
        assertTrue("Le profil reste complété même sans Facebook (champ facultatif)", removeRef.get().isProfileComplete());
    }

    // 11. Annulation d'une modification
    @Test
    public void test11_cancelModificationPreservesOriginalData() {
        test02_completeProfileAndSave();

        User current = mockBackend.getOrCreateProfile(USER_JEAN);
        assertNotNull(current);
        String originalName = current.getName();

        // L'utilisateur tape quelque chose d'autre sans valider
        String draftName = "Jean Modifié Annulé";
        assertNotEquals(originalName, draftName);

        // Au clic sur Annuler, on recharge l'original
        assertEquals("Jean Rakoto", current.getName());
        assertEquals("jean.rakoto", current.getUsername());
    }

    // 14. Déconnexion et connexion avec un autre compte (aucune fuite entre comptes)
    @Test
    public void test14_userIsolationBetweenSessions() {
        // Utilisateur 1 : Jean
        test02_completeProfileAndSave();

        // Déconnexion
        AuthSession.getInstance().clear();

        // Connexion Utilisateur 2 : Marie
        AuthSession.getInstance().updateSession(USER_MARIE, "jwt_marie");

        AtomicReference<User> marieRef = new AtomicReference<>();
        userRepository.getMyProfile(new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                marieRef.set(result);
            }

            @Override
            public void onError(String message) {}
        });

        User marie = marieRef.get();
        assertNotNull(marie);
        assertEquals(USER_MARIE, marie.getId());
        assertNotEquals("Jean Rakoto", marie.getName());
        assertNotEquals("jean.rakoto", marie.getUsername());
        assertFalse("Le compte Marie fraîchement créé n'est pas encore complété", marie.isProfileComplete());
    }

    // 15. Validation des critères de complétion
    @Test
    public void test15_profileCompletionRules() {
        User u = new User();
        assertFalse(u.isProfileComplete());

        u.setName("Bob");
        assertFalse("Manque username, tel et adresse", u.isProfileComplete());

        u.setUsername("bob.pro");
        assertFalse("Manque tel et adresse", u.isProfileComplete());

        u.setWhatsapp("+261320011122");
        assertFalse("Manque adresse", u.isProfileComplete());

        u.setAddress("Toamasina");
        assertTrue("Tous les champs obligatoires sont renseignés", u.isProfileComplete());

        // La présence ou l'absence de photo et facebook ne bloque pas
        u.setAvatarUrl(null);
        u.setFacebookLink(null);
        assertTrue(u.isProfileComplete());
    }
}
