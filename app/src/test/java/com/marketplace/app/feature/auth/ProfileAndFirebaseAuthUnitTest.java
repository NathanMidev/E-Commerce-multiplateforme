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
import com.marketplace.app.feature.contact.ContactLinks;
import com.marketplace.app.model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rôle : Suite de tests unitaires couvrant les 10 scénarios obligatoires de la Section 8 :
 * Complétion du profil utilisateur, photo de profil Firebase Storage, nom d'utilisateur unique,
 * numéro unique WhatsApp multi-indicatifs E.164, lien Facebook et sécurité Firebase Auth.
 */
public class ProfileAndFirebaseAuthUnitTest {

    private static final String USER_ALICE = "uid_alice_01";
    private static final String USER_BOB = "uid_bob_02";

    private MockBackendService mockBackend;
    private UserRepository userRepository;
    private ProfileStorageService profileStorageService;

    @Before
    public void setUp() {
        mockBackend = MockBackendService.getInstance();
        mockBackend.resetData();
        ProfileStorageService.resetSimulatedState();
        userRepository = new UserRepository(true);
        profileStorageService = new ProfileStorageService(true);
        AuthSession.getInstance().clear();
    }

    @After
    public void tearDown() {
        AuthSession.getInstance().clear();
    }

    // =========================================================================
    // 1. Création ou complétion d'un profil sans photo (image par défaut utilisée)
    // =========================================================================
    @Test
    public void test01_completeProfileWithoutPhoto() {
        AuthSession.getInstance().updateSession(USER_ALICE, "jwt_alice");

        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Alice Rasoa");
        fields.put("username", "alice.rasoa");
        fields.put("dialCode", "+261");
        fields.put("whatsapp", "034 12 345 67");
        fields.put("address", "Antananarivo, Analakely");

        AtomicReference<User> savedUser = new AtomicReference<>();
        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {
                savedUser.set(data);
            }

            @Override
            public void onError(String message) {}
        });

        User u = savedUser.get();
        assertNotNull(u);
        assertEquals(USER_ALICE, u.getUid());
        assertEquals("Alice Rasoa", u.getDisplayName());
        assertEquals("alice.rasoa", u.getUsername());
        assertEquals("+261341234567", u.getWhatsappNumber());
        assertFalse("Aucune photo personnalisée définie -> utilise l'avatar par défaut", u.hasCustomAvatar());
        assertTrue(u.canSell());
    }

    // =========================================================================
    // 2. Ajout, remplacement et suppression d'une photo de profil
    // =========================================================================
    @Test
    public void test02_addReplaceAndDeleteProfilePhoto() {
        AuthSession.getInstance().updateSession(USER_ALICE, "jwt_alice");
        byte[] dummyJpeg1 = "JPEG_IMAGE_DATA_V1".getBytes(StandardCharsets.UTF_8);
        byte[] dummyJpeg2 = "JPEG_IMAGE_DATA_V2_REPLACED".getBytes(StandardCharsets.UTF_8);

        // 1) Premier upload de photo vers Firebase Storage
        AtomicReference<String> firstUrlRef = new AtomicReference<>();
        profileStorageService.uploadOrReplaceProfilePhoto(USER_ALICE, null, dummyJpeg1,
                new ProfileStorageService.PhotoUploadCallback() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String photoUrl) {
                        firstUrlRef.set(photoUrl);
                    }

                    @Override
                    public void onError(String message) {}
                });

        assertNotNull(firstUrlRef.get());
        assertTrue(firstUrlRef.get().contains("users/" + USER_ALICE + "/avatar/profile.jpg"));

        Map<String, String> update1 = new HashMap<>();
        update1.put("photoUrl", firstUrlRef.get());
        userRepository.updateMyProfile(update1, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });
        assertEquals(firstUrlRef.get(), AuthSession.getInstance().getCurrentUser().getPhotoUrl());
        assertTrue(AuthSession.getInstance().getCurrentUser().hasCustomAvatar());

        // 2) Remplacement de la photo
        AtomicReference<String> secondUrlRef = new AtomicReference<>();
        profileStorageService.uploadOrReplaceProfilePhoto(USER_ALICE, null, dummyJpeg2,
                new ProfileStorageService.PhotoUploadCallback() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String photoUrl) {
                        secondUrlRef.set(photoUrl);
                    }

                    @Override
                    public void onError(String message) {}
                });
        assertNotNull(secondUrlRef.get());

        // 3) Suppression de la photo
        AtomicBoolean deleted = new AtomicBoolean(false);
        profileStorageService.deleteProfilePhoto(USER_ALICE, new ProfileStorageService.PhotoDeleteCallback() {
            @Override
            public void onSuccess() {
                deleted.set(true);
            }

            @Override
            public void onError(String message) {}
        });
        assertTrue(deleted.get());

        Map<String, String> clearAvatar = new HashMap<>();
        clearAvatar.put("avatarUrl", "");
        userRepository.updateMyProfile(clearAvatar, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });
        assertFalse(AuthSession.getInstance().getCurrentUser().hasCustomAvatar());
    }

    // =========================================================================
    // 3. Enregistrement, unicité et réaffichage du Nom d'utilisateur (username)
    // =========================================================================
    @Test
    public void test03_usernameDistinctFromFullNameAndUniqueness() {
        // Validation du format
        assertNotNull("Trop court", ProfileValidator.validateUsername("ab"));
        assertNotNull("Caractères interdits", ProfileValidator.validateUsername("jean rakoto!"));
        assertNotNull("Deux points consécutifs", ProfileValidator.validateUsername("jean..rakoto"));
        assertNotNull("Nom réservé", ProfileValidator.validateUsername("admin"));
        assertNull("Format valide", ProfileValidator.validateUsername("Jean.Rakoto_261"));
        assertEquals("jean.rakoto_261", ProfileValidator.normalizeUsername("@Jean.Rakoto_261"));

        // Alice enregistre son profil avec Nom complet ET Username distincts
        AuthSession.getInstance().updateSession(USER_ALICE, "jwt_alice");
        Map<String, String> aliceFields = new HashMap<>();
        aliceFields.put("name", "Alice Rakotomalala");
        aliceFields.put("username", "alice.tana");
        aliceFields.put("whatsapp", "+261341122334");
        userRepository.updateMyProfile(aliceFields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });

        User alice = AuthSession.getInstance().getCurrentUser();
        assertEquals("Alice Rakotomalala", alice.getDisplayName());
        assertEquals("alice.tana", alice.getUsername());
        assertNotEquals(alice.getDisplayName(), alice.getUsername());
        assertEquals("Alice Rakotomalala (@alice.tana)", alice.getFormattedSellerIdentity());

        // Bob tente d'utiliser le même username "alice.tana" -> refusé
        AuthSession.getInstance().updateSession(USER_BOB, "jwt_bob");
        assertFalse(ProfileStorageService.isUsernameAvailable("alice.tana", USER_BOB));

        Map<String, String> bobFields = new HashMap<>();
        bobFields.put("name", "Bob Randria");
        bobFields.put("username", "alice.tana");
        AtomicReference<String> errorRef = new AtomicReference<>();
        userRepository.updateMyProfile(bobFields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}

            @Override
            public void onError(String message) {
                errorRef.set(message);
            }
        });
        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("déjà utilisé"));
    }

    // =========================================================================
    // 4. Validation du numéro WhatsApp avec plusieurs indicatifs internationaux
    // =========================================================================
    @Test
    public void test04_whatsappValidationAcrossMultipleCountryDialCodes() {
        // Madagascar (+261)
        assertEquals("+261341234567", PhoneNormalizer.normalizeToE164("+261", "034 12 345 67"));
        assertEquals("+261329876543", PhoneNormalizer.normalizeToE164("+261", "32 98 765 43"));
        assertEquals("34 12 345 67", PhoneNormalizer.formatNationalDisplay("+261", "0341234567"));

        // France (+33)
        assertEquals("+33612345678", PhoneNormalizer.normalizeToE164("+33", "06 12 34 56 78"));
        assertEquals("6 12 34 56 78", PhoneNormalizer.formatNationalDisplay("+33", "0612345678"));

        // La Réunion (+262)
        assertEquals("+262692123456", PhoneNormalizer.normalizeToE164("+262", "0692 12 34 56"));

        // Maurice (+230)
        assertEquals("+23051234567", PhoneNormalizer.normalizeToE164("+230", "5123 4567"));

        // Comores (+269)
        assertEquals("+2693123456", PhoneNormalizer.normalizeToE164("+269", "312 34 56"));

        // Chine (+86)
        assertEquals("+8613800138000", PhoneNormalizer.normalizeToE164("+86", "138 0013 8000"));

        // États-Unis / Canada (+1)
        assertEquals("+14155552671", PhoneNormalizer.normalizeToE164("+1", "415-555-2671"));

        // Rejet des numéros invalides (trop courts, lettres, mauvais préfixe opérateur)
        assertNull(PhoneNormalizer.normalizeToE164("+261", "12345"));
        assertNull(PhoneNormalizer.normalizeToE164("+261", "034ABCD567"));
        assertNull(PhoneNormalizer.normalizeToE164("+33", "00123"));
    }

    // =========================================================================
    // 5. Vérification qu'aucun deuxième champ téléphone n'est nécessaire
    // =========================================================================
    @Test
    public void test05_singleWhatsappFieldPopulatesAllPhoneReferences() {
        AuthSession.getInstance().updateSession(USER_ALICE, "jwt_alice");

        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Alice Boutique");
        fields.put("username", "alice.boutique");
        fields.put("dialCode", "+33");
        fields.put("whatsapp", "06 98 76 54 32"); // Seul le champ WhatsApp est renseigné

        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });

        User u = AuthSession.getInstance().getCurrentUser();
        assertNotNull(u);
        assertEquals("+33698765432", u.getWhatsapp());
        assertEquals("+33698765432", u.getWhatsappNumber());
        assertEquals("Le numéro d'appel phoneNumber est automatiquement synchronisé avec WhatsApp",
                "+33698765432", u.getPhoneNumber());
    }

    // =========================================================================
    // 6. Vérification du bouton d'appel (tel:) et du lien WhatsApp (wa.me sans '+')
    // =========================================================================
    @Test
    public void test06_callUriAndWhatsAppLinkFromSingleNumber() {
        User seller = new User("s1", "Atelier Mada", "a@m.mg", "");
        seller.setWhatsappNumber("+261 34 12 345 67");

        // Lien d'appel téléphonique (tel:+261341234567)
        String telUri = ContactLinks.buildPhoneCallUri(seller.getWhatsapp());
        assertEquals("tel:+261341234567", telUri);

        // Lien de discussion WhatsApp (https://wa.me/261341234567 sans le '+')
        String waDigits = ContactLinks.normalizePhoneForWhatsapp(seller.getWhatsapp());
        assertEquals("261341234567", waDigits);
        assertFalse(waDigits.contains("+"));

        String waUrl = ContactLinks.buildWhatsAppUrl(seller.getWhatsapp(), "Fauteuil", 180.0, "Marketplace");
        assertNotNull(waUrl);
        assertTrue(waUrl.startsWith("https://wa.me/261341234567?text="));
    }

    // =========================================================================
    // 7. Ajout, modification et suppression du lien du profil Facebook
    // =========================================================================
    @Test
    public void test07_addModifyAndDeleteFacebookProfileLink() {
        // Formats Facebook valides
        assertEquals("https://facebook.com/atelier.nordique",
                ProfileValidator.normalizeFacebookUrl("facebook.com/atelier.nordique"));
        assertEquals("https://www.facebook.com/profile.php?id=1000998877",
                ProfileValidator.normalizeFacebookUrl("https://www.facebook.com/profile.php?id=1000998877"));
        assertEquals("https://fb.com/maboutique",
                ProfileValidator.normalizeFacebookUrl("http://fb.com/maboutique"));

        // Formats invalides rejetés
        assertNotNull(ProfileValidator.validateFacebookUrl("https://google.com/monprofil"));
        assertNotNull(ProfileValidator.validateFacebookUrl("https://facebook.com/"));

        AuthSession.getInstance().updateSession(USER_ALICE, "jwt_alice");

        // 1) Ajout du lien Facebook
        Map<String, String> addFb = new HashMap<>();
        addFb.put("facebookUrl", "facebook.com/alice.shop.mg");
        userRepository.updateMyProfile(addFb, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });
        assertTrue(AuthSession.getInstance().getCurrentUser().hasFacebookProfile());
        assertEquals("https://facebook.com/alice.shop.mg",
                AuthSession.getInstance().getCurrentUser().getFacebookUrl());

        // 2) Modification du lien Facebook
        Map<String, String> modFb = new HashMap<>();
        modFb.put("facebookUrl", "https://fb.me/alice.nouvelle.boutique");
        userRepository.updateMyProfile(modFb, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });
        assertEquals("https://fb.me/alice.nouvelle.boutique",
                AuthSession.getInstance().getCurrentUser().getFacebookUrl());

        // 3) Suppression du lien Facebook (champ vidé)
        Map<String, String> delFb = new HashMap<>();
        delFb.put("facebookUrl", "");
        userRepository.updateMyProfile(delFb, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });
        assertFalse("Après suppression du lien Facebook, hasFacebookProfile() doit être false",
                AuthSession.getInstance().getCurrentUser().hasFacebookProfile());
        assertNull(AuthSession.getInstance().getCurrentUser().getFacebookUrl());
    }

    // =========================================================================
    // 8. Déconnexion et reconnexion avec récupération complète du profil
    // =========================================================================
    @Test
    public void test08_logoutAndReloginRestoresCompleteProfile() {
        AuthSession.getInstance().updateSession(USER_ALICE, "jwt_alice");

        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Alice Artisanat");
        fields.put("username", "alice.artisanat");
        fields.put("whatsapp", "+261345566778");
        fields.put("facebookUrl", "https://facebook.com/alice.artisanat");
        fields.put("avatarUrl", "https://storage.firebase.local/users/uid_alice_01/avatar/profile.jpg");
        fields.put("address", "Antananarivo, Ivandry");

        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });

        // Déconnexion complète
        FirebaseAuthSession.signOut();
        assertFalse(AuthSession.getInstance().isLoggedIn());
        assertNull(AuthSession.getInstance().getCurrentUser());

        // Reconnexion avec le même UID
        AuthSession.getInstance().updateSession(USER_ALICE, "jwt_alice_new");
        AtomicReference<User> restoredRef = new AtomicReference<>();
        userRepository.getMyProfile(new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {
                restoredRef.set(data);
            }

            @Override
            public void onError(String message) {}
        });

        User restored = restoredRef.get();
        assertNotNull(restored);
        assertEquals(USER_ALICE, restored.getUid());
        assertEquals("Alice Artisanat", restored.getDisplayName());
        assertEquals("alice.artisanat", restored.getUsername());
        assertEquals("+261345566778", restored.getWhatsappNumber());
        assertEquals("https://facebook.com/alice.artisanat", restored.getFacebookUrl());
        assertEquals("https://storage.firebase.local/users/uid_alice_01/avatar/profile.jpg", restored.getPhotoUrl());
        assertEquals("Antananarivo, Ivandry", restored.getAddress());
    }

    // =========================================================================
    // 9. Cohérence entre Firebase Authentication et le backend (non-écrasement)
    // =========================================================================
    @Test
    public void test09_partialProfileUpdateDoesNotOverwriteExistingData() {
        AuthSession.getInstance().updateSession(USER_ALICE, "jwt_alice");

        // Enregistrement initial complet
        Map<String, String> initial = new HashMap<>();
        initial.put("name", "Alice Initial");
        initial.put("username", "alice.init");
        initial.put("whatsapp", "+261340011223");
        initial.put("facebookUrl", "https://facebook.com/alice.init");
        initial.put("avatarUrl", "https://storage.firebase.local/users/uid_alice_01/avatar/profile.jpg");
        userRepository.updateMyProfile(initial, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });

        // Mise à jour partielle uniquement de l'adresse (sans renvoyer avatarUrl ni facebookUrl)
        Map<String, String> partial = new HashMap<>();
        partial.put("address", "Nouvelle Adresse Ankorondrano");
        userRepository.updateMyProfile(partial, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User data) {}
            @Override
            public void onError(String message) {}
        });

        User current = AuthSession.getInstance().getCurrentUser();
        assertEquals("Nouvelle Adresse Ankorondrano", current.getAddress());
        assertEquals("Les données existantes ne doivent jamais être écrasées par une mise à jour partielle",
                "Alice Initial", current.getDisplayName());
        assertEquals("alice.init", current.getUsername());
        assertEquals("+261340011223", current.getWhatsappNumber());
        assertEquals("https://facebook.com/alice.init", current.getFacebookUrl());
        assertEquals("https://storage.firebase.local/users/uid_alice_01/avatar/profile.jpg", current.getPhotoUrl());
    }

    // =========================================================================
    // 10. Vérification des permissions et de la sécurité des fichiers
    // =========================================================================
    @Test
    public void test10_fileAndProfileSecurityEnforcesUserIsolation() {
        // 1) Utilisateur non connecté -> rejet 401
        AuthSession.getInstance().clear();
        AtomicReference<String> unauthError = new AtomicReference<>();
        profileStorageService.uploadOrReplaceProfilePhoto(USER_ALICE, null,
                "IMG".getBytes(StandardCharsets.UTF_8),
                new ProfileStorageService.PhotoUploadCallback() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String photoUrl) {}

                    @Override
                    public void onError(String message) {
                        unauthError.set(message);
                    }
                });
        assertNotNull(unauthError.get());
        assertTrue(unauthError.get().contains("401"));

        // 2) Utilisateur BOB connecté tente de modifier ou supprimer la photo de profil d'ALICE -> rejet 403
        AuthSession.getInstance().updateSession(USER_BOB, "jwt_bob");
        AtomicReference<String> crossUploadErr = new AtomicReference<>();
        profileStorageService.uploadOrReplaceProfilePhoto(USER_ALICE, null,
                "IMG".getBytes(StandardCharsets.UTF_8),
                new ProfileStorageService.PhotoUploadCallback() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String photoUrl) {}

                    @Override
                    public void onError(String message) {
                        crossUploadErr.set(message);
                    }
                });
        assertNotNull(crossUploadErr.get());
        assertTrue(crossUploadErr.get().contains("403"));

        AtomicReference<String> crossDeleteErr = new AtomicReference<>();
        profileStorageService.deleteProfilePhoto(USER_ALICE, new ProfileStorageService.PhotoDeleteCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onError(String message) {
                crossDeleteErr.set(message);
            }
        });
        assertNotNull(crossDeleteErr.get());
        assertTrue(crossDeleteErr.get().contains("403"));
    }
}
