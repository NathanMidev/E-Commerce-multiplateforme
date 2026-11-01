package com.marketplace.app.feature.ar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.import1688.AdminAccessControl;
import com.marketplace.app.model.Product;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Rôle : Suite de tests unitaires couvrant les 19 scénarios obligatoires de l'évolution
 * du module 3D (Upload manuel de fichiers .glb, Firebase Storage/Firestore, isolation vendeurs,
 * visualiseur 3D interactif et expérience ARCore multi-instances avec duplication).
 */
public class GlbAndArCoreUnitTest {

    private static final String SELLER_A = "seller_101";
    private static final String SELLER_B = "seller_102";

    private MockBackendService mockBackend;
    private ProductRepository productRepository;
    private ProductMediaFirebaseService mediaService;

    @Before
    public void setUp() {
        mockBackend = MockBackendService.getInstance();
        mockBackend.resetData();
        productRepository = new ProductRepository(true);
        mediaService = new ProductMediaFirebaseService(true);
        ProductMediaFirebaseService.clearSimulatedMedia();
        GlbModelCache.getInstance().clearMemoryCache();
        GlbValidator.resetMaxGlbSizeBytes();
        ArLauncher.setTestArSupportOverride(null);
        AdminAccessControl.grantAdminForUid(null, false);
        AuthSession.getInstance().clear();
    }

    @After
    public void tearDown() {
        GlbValidator.resetMaxGlbSizeBytes();
        ArLauncher.setTestArSupportOverride(null);
        AdminAccessControl.grantAdminForUid(null, false);
        AuthSession.getInstance().clear();
    }

    // =========================================================================
    // 1. Publication d'un produit avec des images uniquement (sans modèle 3D)
    // =========================================================================
    @Test
    public void scenario01_publishProductWithImagesOnly() {
        AuthSession.getInstance().updateSession(SELLER_A, "jwt_seller_a");

        Product newProd = new Product();
        newProd.setTitle("Chaise Artisanale Palissandre");
        newProd.setDescription("Chaise fabriquée à la main, livrée assemblée.");
        newProd.setPrice(85.0);
        newProd.setStock(6);
        newProd.setCategoryId("cat_furniture");
        newProd.setSellerId(SELLER_A);
        newProd.setImageUrls(Arrays.asList(
                "https://storage.firebase.local/products/seller_101/prod_img_only/images/img1.jpg",
                "https://storage.firebase.local/products/seller_101/prod_img_only/images/img2.jpg"
        ));
        newProd.setHas3DModel(false);
        newProd.setModel3dUrl("");

        AtomicReference<Product> publishedRef = new AtomicReference<>();
        productRepository.create(newProd, new BaseRepository.Callback<Product>() {
            @Override
            public void onSuccess(Product data) {
                publishedRef.set(data);
            }

            @Override
            public void onError(String message) {}
        });

        Product created = publishedRef.get();
        assertNotNull(created);
        assertEquals(2, created.getImageUrls().size());
        assertFalse("Un produit publié sans .glb ne doit pas activer has3DModel", created.isHas3DModel());
        assertFalse("Un produit publié sans .glb ne doit pas être considéré comme ayant un GLB valide", created.hasValidGlbModel());
    }

    // =========================================================================
    // 2. Publication avec images et modèle .glb valide
    // =========================================================================
    @Test
    public void scenario02_publishProductWithImagesAndGlbModel() {
        AuthSession.getInstance().updateSession(SELLER_A, "jwt_seller_a");

        Product draft = new Product();
        draft.setTitle("Canapé Modulable 3 Places");
        draft.setDescription("Canapé contemporain avec modèle 3D .glb");
        draft.setPrice(450.0);
        draft.setStock(3);
        draft.setCategoryId("cat_furniture");
        draft.setSellerId(SELLER_A);
        draft.setImageUrls(Arrays.asList(
                "https://storage.firebase.local/products/seller_101/prod_canape/images/photo_1.jpg",
                "https://storage.firebase.local/products/seller_101/prod_canape/images/photo_2.jpg"
        ));

        Product created = mockBackend.createProduct(draft);
        assertNotNull(created);

        byte[] glbBytes = GlbValidator.createMinimalValidGlbBytes();
        AtomicReference<String> uploadedUrlRef = new AtomicReference<>();
        AtomicInteger lastProgress = new AtomicInteger(0);

        mediaService.uploadOrReplaceGlbModel(
                created,
                "canape_3_places.glb",
                3_145_728L,
                glbBytes,
                null,
                false,
                new ProductMediaFirebaseService.GlbUploadCallback() {
                    @Override
                    public void onProgress(int percent) {
                        lastProgress.set(percent);
                    }

                    @Override
                    public void onSuccess(String model3dUrl, long model3dSize, Map<String, Object> firestoreMetadata) {
                        uploadedUrlRef.set(model3dUrl);
                    }

                    @Override
                    public void onError(String message) {}
                }
        );

        assertEquals(100, lastProgress.get());
        assertNotNull(uploadedUrlRef.get());
        assertTrue(uploadedUrlRef.get().endsWith("/model/model.glb"));
        assertTrue(created.isHas3DModel());
        assertTrue(created.hasValidGlbModel());
        assertEquals("glb", created.getModel3dFormat());
        assertEquals(3_145_728L, created.getModel3dSize());
        assertEquals(2, created.getImageUrls().size());

        Map<String, Object> firestoreMeta = ProductMediaFirebaseService.getSimulatedMetadata(created.getId());
        assertNotNull(firestoreMeta);
        assertEquals(created.getId(), firestoreMeta.get("productId"));
        assertEquals(SELLER_A, firestoreMeta.get("sellerId"));
        assertEquals(Boolean.TRUE, firestoreMeta.get("has3DModel"));
        assertEquals("glb", firestoreMeta.get("model3dFormat"));
        assertEquals(3_145_728L, firestoreMeta.get("model3dSize"));
    }

    // =========================================================================
    // 3. Remplacement d'un modèle .glb existant
    // =========================================================================
    @Test
    public void scenario03_replaceExistingGlbModel() {
        AuthSession.getInstance().updateSession(SELLER_A, "jwt_seller_a");
        Product product = mockBackend.getProductById("prod_1");
        assertNotNull(product);
        assertTrue(product.hasValidGlbModel());

        byte[] newGlbBytes = GlbValidator.createMinimalValidGlbBytes();
        long newFileSize = 4_800_000L;
        AtomicBoolean replaced = new AtomicBoolean(false);

        mediaService.uploadOrReplaceGlbModel(
                product,
                "fauteuil_v2_hd.glb",
                newFileSize,
                newGlbBytes,
                null,
                false,
                new ProductMediaFirebaseService.GlbUploadCallback() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String model3dUrl, long model3dSize, Map<String, Object> firestoreMetadata) {
                        replaced.set(true);
                    }

                    @Override
                    public void onError(String message) {}
                }
        );

        assertTrue(replaced.get());
        assertTrue(product.hasValidGlbModel());
        assertEquals(newFileSize, product.getModel3dSize());
        assertEquals("https://storage.firebase.local/products/seller_101/prod_1/model/model.glb", product.getModel3dUrl());
    }

    // =========================================================================
    // 4. Suppression d'un modèle .glb (conserve les images classiques)
    // =========================================================================
    @Test
    public void scenario04_deleteGlbModelPreservesImages() {
        AuthSession.getInstance().updateSession(SELLER_A, "jwt_seller_a");
        Product product = mockBackend.getProductById("prod_1");
        assertNotNull(product);
        int initialImagesCount = product.getImageUrls().size();
        assertTrue(initialImagesCount > 0);
        assertTrue(product.hasValidGlbModel());

        AtomicBoolean deleted = new AtomicBoolean(false);
        mediaService.deleteGlbModel(product, new ProductMediaFirebaseService.MediaDeleteCallback() {
            @Override
            public void onSuccess() {
                deleted.set(true);
            }

            @Override
            public void onError(String message) {}
        });

        assertTrue(deleted.get());
        assertFalse(product.isHas3DModel());
        assertFalse(product.hasValidGlbModel());
        assertTrue(product.getModel3dUrl() == null || product.getModel3dUrl().isEmpty());
        assertEquals(0L, product.getModel3dSize());
        assertEquals("La suppression du .glb ne doit jamais supprimer les photos du produit",
                initialImagesCount, product.getImageUrls().size());
    }

    // =========================================================================
    // 5. Upload d'un fichier incompatible (.obj, .fbx, corrompu ou trop lourd)
    // =========================================================================
    @Test
    public void scenario05_rejectIncompatibleOrCorruptedFiles() {
        byte[] validHeader = GlbValidator.createMinimalValidGlbBytes();

        // Format .obj refusé
        String errObj = GlbValidator.validateGlbHeader("chaise.obj", 1024, validHeader);
        assertNotNull(errObj);
        assertTrue(errObj.contains(".glb"));

        // Format .fbx refusé
        String errFbx = GlbValidator.validateGlbHeader("table.fbx", 2048, validHeader);
        assertNotNull(errFbx);

        // Fichier renommé en .glb mais avec entête non-glTF (ex: texte ou image JPEG)
        byte[] fakeBytes = "NOT_A_GLTF_BINARY_FILE_HEADER".getBytes(StandardCharsets.UTF_8);
        String errFake = GlbValidator.validateGlbHeader("faux_modele.glb", fakeBytes.length, fakeBytes);
        assertNotNull(errFake);
        assertTrue(errFake.contains("glTF"));

        // Fichier dépassant la limite configurable
        GlbValidator.setMaxGlbSizeBytes(5L * 1024L * 1024L); // 5 Mo max
        String errTooLarge = GlbValidator.validateGlbHeader("geant.glb", 12L * 1024L * 1024L, validHeader);
        assertNotNull(errTooLarge);
        assertTrue(errTooLarge.contains("taille maximale"));
    }

    // =========================================================================
    // 6. Échec ou interruption d'un upload
    // =========================================================================
    @Test
    public void scenario06_handleUploadFailureOrNetworkInterruption() {
        AuthSession.getInstance().updateSession(SELLER_A, "jwt_seller_a");
        Product product = mockBackend.getProductById("prod_4"); // Produit sans 3D
        assertNotNull(product);
        assertFalse(product.hasValidGlbModel());

        AtomicReference<String> errorRef = new AtomicReference<>();
        mediaService.uploadOrReplaceGlbModel(
                product,
                "table_palissandre.glb",
                1_500_000L,
                GlbValidator.createMinimalValidGlbBytes(),
                null,
                true, // Simulation d'une coupure réseau en plein transfert
                new ProductMediaFirebaseService.GlbUploadCallback() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String model3dUrl, long model3dSize, Map<String, Object> firestoreMetadata) {}

                    @Override
                    public void onError(String message) {
                        errorRef.set(message);
                    }
                }
        );

        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("interrompue"));
        assertFalse("Un upload interrompu ne doit pas activer has3DModel sur le produit", product.hasValidGlbModel());
    }

    // =========================================================================
    // 7. Consultation d'un produit sans modèle 3D
    // =========================================================================
    @Test
    public void scenario07_consultProductWithout3dModel() {
        Product productWithout3d = mockBackend.getProductById("prod_4");
        assertNotNull(productWithout3d);
        assertFalse(productWithout3d.isHas3DModel());
        assertFalse(productWithout3d.hasValidGlbModel());
        assertFalse(productWithout3d.getImageUrls().isEmpty());

        // Le lanceur ARCore doit refuser l'ouverture AR sur un produit sans .glb
        ArLauncher.ArReadinessResult readiness = ArLauncher.evaluateReadiness(
                null, productWithout3d.getModel3dUrl(), true);
        assertFalse(readiness.isReady());
        assertFalse(readiness.isValidGlbModel());
    }

    // =========================================================================
    // 8. Consultation d'un produit avec modèle .glb
    // =========================================================================
    @Test
    public void scenario08_consultProductWithValidGlbModel() {
        Product productWith3d = mockBackend.getProductById("prod_1");
        assertNotNull(productWith3d);
        assertTrue(productWith3d.isHas3DModel());
        assertTrue(productWith3d.hasValidGlbModel());
        assertEquals("glb", productWith3d.getModel3dFormat());
        assertTrue(productWith3d.getModel3dSize() > 0);

        GlbModelCache.SharedGlbResource loaded = GlbModelCache.getInstance().loadFromBytes(
                productWith3d.getModel3dUrl(),
                GlbValidator.createMinimalValidGlbBytes()
        );

        assertNotNull(loaded);
        assertEquals(2, loaded.getGltfVersion());
        assertTrue(loaded.getVertices().length > 0);
        assertSame(loaded, GlbModelCache.getInstance().getFromMemory(productWith3d.getModel3dUrl()));
    }

    // =========================================================================
    // 9. Rotation et zoom du modèle dans le visualiseur 3D interactif
    // =========================================================================
    @Test
    public void scenario09_interactive3dViewerRotationAndZoomBounds() {
        // Vérifie les bornes de sécurité de rotation (pitch) et de zoom du visualiseur 3D
        assertTrue(Interactive3dModelView.MIN_ZOOM >= 0.25f);
        assertTrue(Interactive3dModelView.MAX_ZOOM <= 5.0f);
        assertEquals(-75.0f, Interactive3dModelView.MIN_PITCH, 0.01f);
        assertEquals(75.0f, Interactive3dModelView.MAX_PITCH, 0.01f);
    }

    // =========================================================================
    // 10. Ouverture d'ARCore sur un téléphone compatible avec modèle .glb valide
    // =========================================================================
    @Test
    public void scenario10_openArCoreWhenDeviceCompatibleAndGlbValid() {
        ArLauncher.setTestArSupportOverride(true);
        Product product = mockBackend.getProductById("prod_1");

        ArLauncher.ArReadinessResult result = ArLauncher.evaluateReadiness(
                null, product.getModel3dUrl(), true);
        assertTrue(result.isReady());
        assertTrue(result.isArCoreSupported());
        assertTrue(result.isValidGlbModel());
        assertTrue(result.isCameraPermissionGranted());
    }

    // =========================================================================
    // 11. Détection d'une surface plane (horizontale ou verticale)
    // =========================================================================
    @Test
    public void scenario11_arSurfaceDetection() {
        ArSceneController scene = new ArSceneController();
        assertFalse(scene.isSurfaceDetected());
        assertEquals("NONE", scene.getDetectedPlaneType());

        scene.onSurfaceDetected("HORIZONTAL");
        assertTrue(scene.isSurfaceDetected());
        assertEquals("HORIZONTAL", scene.getDetectedPlaneType());

        scene.onSurfaceDetected("VERTICAL");
        assertTrue(scene.isSurfaceDetected());
        assertEquals("VERTICAL", scene.getDetectedPlaneType());
    }

    // =========================================================================
    // 12. Placement du produit sur une surface détectée
    // =========================================================================
    @Test
    public void scenario12_placeProductOnDetectedSurface() {
        ArSceneController scene = new ArSceneController();
        GlbModelCache.SharedGlbResource resource = GlbModelCache.getInstance()
                .loadFromBytes("https://storage.firebase.local/products/s1/p1/model/model.glb",
                        GlbValidator.createMinimalValidGlbBytes());
        scene.bindSharedGlbResource(resource);
        scene.onSurfaceDetected("HORIZONTAL");

        ArPlacedInstance first = scene.placeInstanceOnSurface(0.25f, 0.0f, -1.5f);
        assertNotNull(first);
        assertEquals(1, scene.getInstanceCount());
        assertSame(first, scene.getSelectedInstance());
        assertEquals(0.25f, first.getX(), 0.001f);
        assertEquals(-1.5f, first.getZ(), 0.001f);
        assertEquals(1.0f, first.getScaleFactor(), 0.001f);
    }

    // =========================================================================
    // 13. Déplacement d'une instance dans la scène AR
    // =========================================================================
    @Test
    public void scenario13_movePlacedInstance() {
        ArSceneController scene = new ArSceneController();
        GlbModelCache.SharedGlbResource resource = GlbModelCache.getInstance()
                .loadFromBytes("https://storage.firebase.local/products/s1/p1/model/model.glb",
                        GlbValidator.createMinimalValidGlbBytes());
        scene.bindSharedGlbResource(resource);
        scene.onSurfaceDetected("HORIZONTAL");

        ArPlacedInstance instance = scene.placeInstanceOnSurface(0.0f, 0.0f, -1.0f);
        scene.moveSelectedInstanceTo(0.60f, 0.0f, -1.80f);

        assertEquals(0.60f, instance.getX(), 0.001f);
        assertEquals(-1.80f, instance.getZ(), 0.001f);
    }

    // =========================================================================
    // 14. Duplication du produit et partage mémoire de la ressource .glb
    // =========================================================================
    @Test
    public void scenario14_duplicateProductSharesLoadedGlbResourceWithoutReloading() {
        ArSceneController scene = new ArSceneController();
        GlbModelCache.SharedGlbResource resource = GlbModelCache.getInstance()
                .loadFromBytes("https://storage.firebase.local/products/s1/p1/model/model.glb",
                        GlbValidator.createMinimalValidGlbBytes());
        scene.bindSharedGlbResource(resource);
        scene.onSurfaceDetected("HORIZONTAL");

        ArPlacedInstance original = scene.placeInstanceOnSurface(0.0f, 0.0f, -1.2f);
        ArPlacedInstance duplicate = scene.duplicateSelectedInstance(0.35f, -0.15f);

        assertEquals(2, scene.getInstanceCount());
        assertNotEquals(original.getInstanceId(), duplicate.getInstanceId());
        assertSame("La duplication doit partager exactement la même ressource .glb en mémoire sans re-téléchargement",
                original.getSharedResource(), duplicate.getSharedResource());
        assertEquals("Le fichier .glb ne doit avoir été lié qu'une seule fois", 1, scene.getResourceBindCount());
    }

    // =========================================================================
    // 15. Placement de plusieurs instances et indépendance stricte des transformations
    // =========================================================================
    @Test
    public void scenario15_multipleInstancesAreStrictlyIndependent() {
        ArSceneController scene = new ArSceneController();
        GlbModelCache.SharedGlbResource resource = GlbModelCache.getInstance()
                .loadFromBytes("https://storage.firebase.local/products/s1/p1/model/model.glb",
                        GlbValidator.createMinimalValidGlbBytes());
        scene.bindSharedGlbResource(resource);
        scene.onSurfaceDetected("HORIZONTAL");

        ArPlacedInstance inst1 = scene.placeInstanceOnSurface(-0.5f, 0.0f, -1.5f);
        ArPlacedInstance inst2 = scene.duplicateSelectedInstance(0.5f, 0.0f);
        ArPlacedInstance inst3 = scene.duplicateSelectedInstance(0.5f, 0.0f);

        assertEquals(3, scene.getInstanceCount());

        // Sélectionner uniquement l'instance #2 et modifier sa position, sa rotation et son échelle
        assertTrue(scene.selectInstance(inst2.getInstanceId()));
        scene.moveSelectedInstanceTo(1.25f, 0.0f, -2.40f);
        scene.rotateSelectedInstanceBy(45.0f);
        scene.scaleSelectedInstance(1.75f);

        // Vérifier que l'instance #2 a bien été modifiée
        assertEquals(1.25f, inst2.getX(), 0.001f);
        assertEquals(-2.40f, inst2.getZ(), 0.001f);
        assertEquals(45.0f, inst2.getRotationDegrees(), 0.001f);
        assertEquals(1.75f, inst2.getScaleFactor(), 0.001f);

        // Vérifier que l'instance #1 et l'instance #3 n'ont subi AUCUN déplacement ni transformation
        assertEquals(-0.5f, inst1.getX(), 0.001f);
        assertEquals(-1.5f, inst1.getZ(), 0.001f);
        assertEquals(0.0f, inst1.getRotationDegrees(), 0.001f);
        assertEquals(1.0f, inst1.getScaleFactor(), 0.001f);

        assertEquals(0.5f, inst3.getX(), 0.001f);
        assertEquals(-1.5f, inst3.getZ(), 0.001f);
        assertEquals(0.0f, inst3.getRotationDegrees(), 0.001f);
        assertEquals(1.0f, inst3.getScaleFactor(), 0.001f);
    }

    // =========================================================================
    // 16. Suppression d'une instance et réinitialisation de la scène
    // =========================================================================
    @Test
    public void scenario16_deleteSingleInstanceAndResetScene() {
        ArSceneController scene = new ArSceneController();
        GlbModelCache.SharedGlbResource resource = GlbModelCache.getInstance()
                .loadFromBytes("https://storage.firebase.local/products/s1/p1/model/model.glb",
                        GlbValidator.createMinimalValidGlbBytes());
        scene.bindSharedGlbResource(resource);
        scene.onSurfaceDetected("HORIZONTAL");

        ArPlacedInstance inst1 = scene.placeInstanceOnSurface(0.0f, 0.0f, -1.0f);
        ArPlacedInstance inst2 = scene.duplicateSelectedInstance(0.3f, -0.2f);
        assertEquals(2, scene.getInstanceCount());

        // Supprimer l'instance sélectionnée (inst2)
        scene.selectInstance(inst2.getInstanceId());
        assertTrue(scene.deleteSelectedInstance());
        assertEquals(1, scene.getInstanceCount());
        assertSame(inst1, scene.getSelectedInstance());

        // Réinitialiser toute la scène
        scene.resetScene();
        assertEquals(0, scene.getInstanceCount());
        assertNull(scene.getSelectedInstance());
    }

    // =========================================================================
    // 17. Consultation depuis un téléphone incompatible avec ARCore
    // =========================================================================
    @Test
    public void scenario17_consultFromPhoneIncompatibleWithArCore() {
        ArLauncher.setTestArSupportOverride(false);
        Product product = mockBackend.getProductById("prod_1");

        ArLauncher.ArReadinessResult readiness = ArLauncher.evaluateReadiness(
                null, product.getModel3dUrl(), true);
        assertFalse("ARCore ne doit pas se lancer sur un téléphone incompatible", readiness.isReady());
        assertFalse(readiness.isArCoreSupported());
        assertTrue("Le modèle .glb reste valide pour le visualiseur 3D classique", readiness.isValidGlbModel());
        assertTrue(readiness.getMessage().contains("visualiseur 3D classique"));
    }

    // =========================================================================
    // 18. Vérification des autorisations Firebase (utilisateur non connecté)
    // =========================================================================
    @Test
    public void scenario18_firebaseAuthorizationRejectsUnauthenticatedUser() {
        AuthSession.getInstance().clear();
        Product product = mockBackend.getProductById("prod_1");

        AtomicReference<String> uploadError = new AtomicReference<>();
        mediaService.uploadOrReplaceGlbModel(
                product,
                "modele.glb",
                1024L,
                GlbValidator.createMinimalValidGlbBytes(),
                null,
                false,
                new ProductMediaFirebaseService.GlbUploadCallback() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String model3dUrl, long model3dSize, Map<String, Object> firestoreMetadata) {}

                    @Override
                    public void onError(String message) {
                        uploadError.set(message);
                    }
                }
        );

        assertNotNull(uploadError.get());
        assertTrue(uploadError.get().contains("401"));
    }

    // =========================================================================
    // 19. Vérification de l'isolation des fichiers entre vendeurs
    // =========================================================================
    @Test
    public void scenario19_sellerIsolationPreventsCrossSellerModificationOrDeletion() {
        // Le produit prod_1 appartient à SELLER_A (seller_101)
        Product productOfSellerA = mockBackend.getProductById("prod_1");
        assertNotNull(productOfSellerA);
        assertEquals(SELLER_A, productOfSellerA.getSellerId());
        String originalGlbUrl = productOfSellerA.getModel3dUrl();

        // SELLER_B (seller_102) se connecte et tente d'écraser ou de supprimer le .glb de SELLER_A
        AuthSession.getInstance().updateSession(SELLER_B, "jwt_seller_b");

        AtomicReference<String> crossUploadError = new AtomicReference<>();
        mediaService.uploadOrReplaceGlbModel(
                productOfSellerA,
                "pirate.glb",
                2048L,
                GlbValidator.createMinimalValidGlbBytes(),
                null,
                false,
                new ProductMediaFirebaseService.GlbUploadCallback() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String model3dUrl, long model3dSize, Map<String, Object> firestoreMetadata) {}

                    @Override
                    public void onError(String message) {
                        crossUploadError.set(message);
                    }
                }
        );

        assertNotNull(crossUploadError.get());
        assertTrue(crossUploadError.get().contains("403"));
        assertEquals("L'URL du modèle de SELLER_A ne doit pas avoir été modifiée par SELLER_B",
                originalGlbUrl, productOfSellerA.getModel3dUrl());

        AtomicReference<String> crossDeleteError = new AtomicReference<>();
        mediaService.deleteGlbModel(productOfSellerA, new ProductMediaFirebaseService.MediaDeleteCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onError(String message) {
                crossDeleteError.set(message);
            }
        });

        assertNotNull(crossDeleteError.get());
        assertTrue(crossDeleteError.get().contains("403"));
        assertTrue("Le modèle de SELLER_A doit toujours être présent", productOfSellerA.hasValidGlbModel());

        // Vérifie également l'isolation stricte des chemins Firebase Storage entre vendeurs
        String pathA = ProductMediaFirebaseService.buildGlbStoragePath(SELLER_A, "prod_1");
        String pathB = ProductMediaFirebaseService.buildGlbStoragePath(SELLER_B, "prod_2");
        assertEquals("products/seller_101/prod_1/model/model.glb", pathA);
        assertEquals("products/seller_102/prod_2/model/model.glb", pathB);
        assertNotEquals(pathA, pathB);
    }
}
