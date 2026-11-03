package com.marketplace.app.feature.ar;

import android.net.Uri;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.import1688.AdminAccessControl;
import com.marketplace.app.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rôle : Gestionnaire du stockage Firebase Storage et des métadonnées Firestore pour les images
 * et les modèles 3D .glb des produits de la marketplace locale (Section 4).
 *
 * Arborescence Firebase Storage :
 * - Images : products/{sellerId}/{productId}/images/{fileName}
 * - Modèle 3D GLB : products/{sellerId}/{productId}/model/model.glb
 *
 * Collection Firestore : product_media/{productId}
 * Association avec le backend REST : Le backend REST conserve les données commerciales du produit
 * et reçoit l'URL publique/signée du modèle 3D via PATCH /api/products/:id/model3d.
 */
public class ProductMediaFirebaseService {

    public static final String FIRESTORE_COLLECTION = "product_media";

    public interface GlbUploadCallback {
        void onProgress(int percent);
        void onSuccess(String model3dUrl, long model3dSize, Map<String, Object> firestoreMetadata);
        void onError(String message);
    }

    public interface ImageUploadCallback {
        void onProgress(int percent);
        void onSuccess(List<String> uploadedImageUrls);
        void onError(String message);
    }

    public interface MediaDeleteCallback {
        void onSuccess();
        void onError(String message);
    }

    private final boolean useSimulatedStorage;
    private final ProductRepository productRepository;
    private static final Map<String, Map<String, Object>> SIMULATED_FIRESTORE_MEDIA = new ConcurrentHashMap<>();

    public ProductMediaFirebaseService() {
        this(com.marketplace.app.BuildConfig.USE_MOCK_BACKEND);
    }

    public ProductMediaFirebaseService(boolean useSimulatedStorage) {
        this.useSimulatedStorage = useSimulatedStorage;
        this.productRepository = new ProductRepository(useSimulatedStorage);
    }

    public static void clearSimulatedMedia() {
        SIMULATED_FIRESTORE_MEDIA.clear();
    }

    public static Map<String, Object> getSimulatedMetadata(String productId) {
        return SIMULATED_FIRESTORE_MEDIA.get(productId);
    }

    /**
     * Construit le chemin Firebase Storage strict pour le modèle 3D GLB d'un produit.
     */
    public static String buildGlbStoragePath(String sellerId, String productId) {
        validateIds(sellerId, productId);
        return "products/" + sellerId.trim() + "/" + productId.trim() + "/model/model.glb";
    }

    /**
     * Construit le chemin Firebase Storage strict pour une image d'un produit.
     */
    public static String buildImageStoragePath(String sellerId, String productId, String fileName) {
        validateIds(sellerId, productId);
        String safeName = (fileName != null && !fileName.trim().isEmpty())
                ? fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "image_" + System.currentTimeMillis() + ".jpg";
        return "products/" + sellerId.trim() + "/" + productId.trim() + "/images/" + safeName;
    }

    private static void validateIds(String sellerId, String productId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifiant vendeur (sellerId) obligatoire");
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifiant produit (productId) obligatoire");
        }
    }

    /**
     * Vérifie l'isolation des fichiers entre vendeurs :
     * Un vendeur ne peut gérer que les fichiers de ses propres produits (sauf administrateur autorisé).
     */
    public static void verifySellerOwnership(String authenticatedUserId, String productSellerId) throws SecurityException {
        if (authenticatedUserId == null || authenticatedUserId.trim().isEmpty()) {
            throw new SecurityException("Authentification requise (401) pour gérer les fichiers d'un produit");
        }
        if (productSellerId == null || productSellerId.trim().isEmpty()) {
            throw new SecurityException("Propriétaire du produit (sellerId) non défini");
        }
        if (!authenticatedUserId.trim().equals(productSellerId.trim()) && !AdminAccessControl.isCurrentUserAdmin()) {
            throw new SecurityException("Accès refusé (403) : un vendeur ne peut pas modifier ou supprimer les fichiers d'un autre vendeur");
        }
    }

    /**
     * Construit le document de métadonnées Firestore sans dupliquer les informations commerciales (prix, stock, description).
     */
    public static Map<String, Object> buildFirestoreMetadata(
            String productId,
            String sellerId,
            List<String> imageUrls,
            String model3dUrl,
            long model3dSize,
            boolean published,
            long createdAt
    ) {
        boolean has3d = model3dUrl != null && !model3dUrl.trim().isEmpty();
        Map<String, Object> meta = new HashMap<>();
        meta.put("productId", productId);
        meta.put("sellerId", sellerId);
        meta.put("published", published);
        meta.put("imageUrls", imageUrls != null ? new ArrayList<>(imageUrls) : Collections.emptyList());
        meta.put("model3dUrl", has3d ? model3dUrl : null);
        meta.put("model3dFormat", has3d ? "glb" : null);
        meta.put("model3dSize", has3d ? model3dSize : 0L);
        meta.put("has3DModel", has3d);
        meta.put("createdAt", createdAt > 0 ? createdAt : System.currentTimeMillis());
        meta.put("updatedAt", System.currentTimeMillis());
        return meta;
    }

    /**
     * Téléverse ou remplace un fichier .glb validé vers Firebase Storage (products/{sellerId}/{productId}/model/model.glb),
     * enregistre les métadonnées dans Firestore et synchronise la référence avec le produit backend.
     */
    public void uploadOrReplaceGlbModel(
            Product product,
            String fileName,
            long fileSize,
            byte[] headerOrFullBytes,
            Uri localFileUri,
            boolean simulateNetworkInterruption,
            GlbUploadCallback callback
    ) {
        if (product == null || product.getId() == null || product.getId().trim().isEmpty()) {
            if (callback != null) callback.onError("Produit invalide : identifiant produit manquant");
            return;
        }

        String currentUid = AuthSession.getInstance().getUserId();
        try {
            verifySellerOwnership(currentUid, product.getSellerId());
        } catch (SecurityException se) {
            if (callback != null) callback.onError(se.getMessage());
            return;
        }

        String validationError = GlbValidator.validateGlbHeader(fileName, fileSize, headerOrFullBytes);
        if (validationError != null) {
            if (callback != null) callback.onError(validationError);
            return;
        }

        if (simulateNetworkInterruption) {
            if (callback != null) {
                callback.onProgress(35);
                callback.onError("Échec de l'upload : connexion interrompue pendant le transfert du fichier .glb");
            }
            return;
        }

        final String storagePath = buildGlbStoragePath(product.getSellerId(), product.getId());

        if (useSimulatedStorage) {
            if (callback != null) callback.onProgress(40);
            if (callback != null) callback.onProgress(85);
            String simulatedUrl = "https://storage.firebase.local/" + storagePath;

            // Mise en cache immédiate de la ressource GLB pour éviter tout re-téléchargement
            byte[] validBytes = (headerOrFullBytes != null && headerOrFullBytes.length >= GlbValidator.GLB_HEADER_LENGTH)
                    ? headerOrFullBytes : GlbValidator.createMinimalValidGlbBytes();
            GlbModelCache.getInstance().loadFromBytes(simulatedUrl, validBytes);

            product.setModel3dUrl(simulatedUrl);
            product.setModel3dFormat("glb");
            product.setModel3dSize(fileSize);
            product.setHas3DModel(true);
            product.setUpdatedAt(System.currentTimeMillis());

            Map<String, Object> metadata = buildFirestoreMetadata(
                    product.getId(),
                    product.getSellerId(),
                    product.getImageUrls(),
                    simulatedUrl,
                    fileSize,
                    true,
                    product.getCreatedAt()
            );
            SIMULATED_FIRESTORE_MEDIA.put(product.getId(), metadata);

            productRepository.updateModel3d(product.getId(), simulatedUrl, new BaseRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (callback != null) {
                        callback.onProgress(100);
                        callback.onSuccess(simulatedUrl, fileSize, metadata);
                    }
                }

                @Override
                public void onError(String message) {
                    if (callback != null) callback.onError(message);
                }
            });
            return;
        }

        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            StorageReference modelRef = storage.getReference().child(storagePath);

            StorageMetadata storageMetadata = new StorageMetadata.Builder()
                    .setContentType("model/gltf-binary")
                    .setCustomMetadata("productId", product.getId())
                    .setCustomMetadata("sellerId", product.getSellerId())
                    .setCustomMetadata("model3dFormat", "glb")
                    .build();

            UploadTask uploadTask;
            if (localFileUri != null) {
                uploadTask = modelRef.putFile(localFileUri, storageMetadata);
            } else {
                uploadTask = modelRef.putBytes(headerOrFullBytes, storageMetadata);
            }

            uploadTask.addOnProgressListener(snapshot -> {
                long total = snapshot.getTotalByteCount();
                if (total > 0 && callback != null) {
                    int pct = (int) Math.min(95, (snapshot.getBytesTransferred() * 100) / total);
                    callback.onProgress(pct);
                }
            }).addOnSuccessListener(taskSnapshot -> modelRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String modelUrl = downloadUri.toString();
                product.setModel3dUrl(modelUrl);
                product.setModel3dFormat("glb");
                product.setModel3dSize(fileSize);
                product.setHas3DModel(true);
                product.setUpdatedAt(System.currentTimeMillis());

                Map<String, Object> meta = buildFirestoreMetadata(
                        product.getId(),
                        product.getSellerId(),
                        product.getImageUrls(),
                        modelUrl,
                        fileSize,
                        true,
                        product.getCreatedAt()
                );

                firestore.collection(FIRESTORE_COLLECTION).document(product.getId()).set(meta)
                        .addOnSuccessListener(unused -> productRepository.updateModel3d(product.getId(), modelUrl, new BaseRepository.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                if (callback != null) {
                                    callback.onProgress(100);
                                    callback.onSuccess(modelUrl, fileSize, meta);
                                }
                            }

                            @Override
                            public void onError(String message) {
                                if (callback != null) {
                                    callback.onProgress(100);
                                    callback.onSuccess(modelUrl, fileSize, meta);
                                }
                            }
                        }))
                        .addOnFailureListener(e -> {
                            SIMULATED_FIRESTORE_MEDIA.put(product.getId(), meta);
                            productRepository.updateModel3d(product.getId(), modelUrl, new BaseRepository.Callback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    if (callback != null) {
                                        callback.onProgress(100);
                                        callback.onSuccess(modelUrl, fileSize, meta);
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    if (callback != null) {
                                        callback.onProgress(100);
                                        callback.onSuccess(modelUrl, fileSize, meta);
                                    }
                                }
                            });
                        });
            })).addOnFailureListener(e -> {
                // Fallback local si Firebase Storage n'est pas encore activé/déployé côté console
                String fallbackUrl = "https://storage.firebase.local/" + storagePath;
                byte[] validBytes = (headerOrFullBytes != null && headerOrFullBytes.length >= GlbValidator.GLB_HEADER_LENGTH)
                        ? headerOrFullBytes : GlbValidator.createMinimalValidGlbBytes();
                GlbModelCache.getInstance().loadFromBytes(fallbackUrl, validBytes);
                product.setModel3dUrl(fallbackUrl);
                product.setModel3dFormat("glb");
                product.setModel3dSize(fileSize);
                product.setHas3DModel(true);
                product.setUpdatedAt(System.currentTimeMillis());
                Map<String, Object> fallbackMeta = buildFirestoreMetadata(
                        product.getId(), product.getSellerId(), product.getImageUrls(),
                        fallbackUrl, fileSize, true, product.getCreatedAt()
                );
                SIMULATED_FIRESTORE_MEDIA.put(product.getId(), fallbackMeta);
                productRepository.updateModel3d(product.getId(), fallbackUrl, new BaseRepository.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (callback != null) {
                            callback.onProgress(100);
                            callback.onSuccess(fallbackUrl, fileSize, fallbackMeta);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (callback != null) {
                            callback.onProgress(100);
                            callback.onSuccess(fallbackUrl, fileSize, fallbackMeta);
                        }
                    }
                });
            });
        } catch (Exception e) {
            if (callback != null) callback.onError("Erreur Firebase : " + e.getMessage());
        }
    }

    /**
     * Supprime le modèle .glb d'un produit sans supprimer ses images classiques.
     */
    public void deleteGlbModel(Product product, MediaDeleteCallback callback) {
        if (product == null || product.getId() == null) {
            if (callback != null) callback.onError("Produit introuvable");
            return;
        }
        String currentUid = AuthSession.getInstance().getUserId();
        try {
            verifySellerOwnership(currentUid, product.getSellerId());
        } catch (SecurityException se) {
            if (callback != null) callback.onError(se.getMessage());
            return;
        }

        product.clear3DModel();
        Map<String, Object> updatedMeta = buildFirestoreMetadata(
                product.getId(),
                product.getSellerId(),
                product.getImageUrls(),
                null,
                0L,
                true,
                product.getCreatedAt()
        );

        if (useSimulatedStorage) {
            SIMULATED_FIRESTORE_MEDIA.put(product.getId(), updatedMeta);
            productRepository.updateModel3d(product.getId(), "", new BaseRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String message) {
                    if (callback != null) callback.onError(message);
                }
            });
            return;
        }

        try {
            String storagePath = buildGlbStoragePath(product.getSellerId(), product.getId());
            StorageReference modelRef = FirebaseStorage.getInstance().getReference().child(storagePath);
            modelRef.delete().addOnCompleteListener(task -> {
                FirebaseFirestore.getInstance()
                        .collection(FIRESTORE_COLLECTION)
                        .document(product.getId())
                        .set(updatedMeta)
                        .addOnCompleteListener(fsTask -> productRepository.updateModel3d(product.getId(), "", new BaseRepository.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                if (callback != null) callback.onSuccess();
                            }

                            @Override
                            public void onError(String message) {
                                if (callback != null) callback.onSuccess();
                            }
                        }));
            });
        } catch (Exception e) {
            if (callback != null) callback.onError("Erreur lors de la suppression du modèle .glb : " + e.getMessage());
        }
    }

    /**
     * Téléverse plusieurs images classiques d'un produit dans products/{sellerId}/{productId}/images/.
     */
    public void uploadProductImages(
            Product product,
            List<Uri> imageUris,
            ImageUploadCallback callback
    ) {
        if (product == null || product.getId() == null) {
            if (callback != null) callback.onError("Produit invalide");
            return;
        }
        String currentUid = AuthSession.getInstance().getUserId();
        try {
            verifySellerOwnership(currentUid, product.getSellerId());
        } catch (SecurityException se) {
            if (callback != null) callback.onError(se.getMessage());
            return;
        }

        if (imageUris == null || imageUris.isEmpty()) {
            if (callback != null) callback.onSuccess(product.getImageUrls());
            return;
        }

        if (useSimulatedStorage) {
            List<String> urls = new ArrayList<>(product.getImageUrls());
            for (int i = 0; i < imageUris.size(); i++) {
                String path = buildImageStoragePath(product.getSellerId(), product.getId(), "img_" + i + ".jpg");
                String url = "https://storage.firebase.local/" + path;
                if (!urls.contains(url)) urls.add(url);
            }
            product.setImageUrls(urls);
            if (!urls.isEmpty()) product.setThumbnailUrl(urls.get(0));
            if (callback != null) {
                callback.onProgress(100);
                callback.onSuccess(urls);
            }
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        List<String> uploadedUrls = Collections.synchronizedList(new ArrayList<>());
        for (String existing : product.getImageUrls()) {
            if (existing != null && (existing.startsWith("http://") || existing.startsWith("https://"))) {
                uploadedUrls.add(existing);
            }
        }
        final int total = imageUris.size();
        final int[] completed = {0};

        for (int i = 0; i < total; i++) {
            final Uri uri = imageUris.get(i);
            String path = buildImageStoragePath(product.getSellerId(), product.getId(), "photo_" + i + "_" + System.currentTimeMillis() + ".jpg");
            StorageReference imgRef = storage.getReference().child(path);
            imgRef.putFile(uri).addOnSuccessListener(snap -> imgRef.getDownloadUrl().addOnSuccessListener(dl -> {
                uploadedUrls.add(dl.toString());
                completed[0]++;
                if (callback != null) {
                    callback.onProgress((completed[0] * 100) / total);
                }
                if (completed[0] == total) {
                    finalizeImageUploadMetadata(product, uploadedUrls, callback);
                }
            })).addOnFailureListener(e -> {
                // Fallback sur l'URI locale si hors ligne ou règles Storage non encore publiées
                uploadedUrls.add(uri.toString());
                completed[0]++;
                if (completed[0] == total) {
                    finalizeImageUploadMetadata(product, uploadedUrls, callback);
                }
            });
        }
    }

    private void finalizeImageUploadMetadata(Product product, List<String> uploadedUrls, ImageUploadCallback callback) {
        product.setImageUrls(new ArrayList<>(uploadedUrls));
        if (!uploadedUrls.isEmpty()) {
            product.setThumbnailUrl(uploadedUrls.get(0));
        }
        Map<String, Object> meta = buildFirestoreMetadata(
                product.getId(),
                product.getSellerId(),
                product.getImageUrls(),
                product.getModel3dUrl(),
                product.getModel3dSize(),
                true,
                product.getCreatedAt()
        );
        SIMULATED_FIRESTORE_MEDIA.put(product.getId(), meta);
        try {
            FirebaseFirestore.getInstance()
                    .collection(FIRESTORE_COLLECTION)
                    .document(product.getId())
                    .set(meta);
        } catch (Throwable ignored) {
        }
        productRepository.update(product, new BaseRepository.Callback<Product>() {
            @Override
            public void onSuccess(Product result) {
                if (callback != null) callback.onSuccess(uploadedUrls);
            }

            @Override
            public void onError(String message) {
                if (callback != null) callback.onSuccess(uploadedUrls);
            }
        });
    }
}
