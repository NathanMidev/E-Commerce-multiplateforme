package com.marketplace.core.network;

import com.marketplace.core.model.Category;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockPostgresBackendService implements PostgresBackendApi {

    private final List<Product> products = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();

    public MockPostgresBackendService() {
        initInitialData();
    }

    private void initInitialData() {
        categories.add(new Category("cat_furniture", "Mobilier & Déco", "Meubles et objets de décoration scannables en 3D", "ic_furniture"));
        categories.add(new Category("cat_electronics", "Électronique", "Appareils, gadgets et accessoires", "ic_electronics"));
        categories.add(new Category("cat_fashion", "Mode & Chaussures", "Vêtements, sacs et sneakers avec essayage AR", "ic_fashion"));
        categories.add(new Category("cat_art", "Art & Sculptures", "Objets uniques numérisés par SAM", "ic_art"));

        Product p1 = new Product(
                "prod_1",
                "Fauteuil Scandinave Velours",
                "Fauteuil confortable avec structure bois massif. Modèle 3D reconstruit par IA Meta SAM.",
                189.99,
                "cat_furniture",
                "seller_101",
                "Atelier Nordique",
                "https://modelviewer.dev/shared-assets/models/Astronaut.glb", // Modèle de test AR GLB
                true,
                Arrays.asList("https://via.placeholder.com/400x300.png?text=Fauteuil+Nordique"),
                5,
                System.currentTimeMillis() - 86400000
        );

        Product p2 = new Product(
                "prod_2",
                "Lampe Design Industrielle",
                "Lampe à poser en acier brossé. Scannez et placez la lampe directement sur votre table en AR.",
                59.00,
                "cat_furniture",
                "seller_102",
                "Luminaires & Co",
                "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/BoomBox/glTF-Binary/BoomBox.glb",
                true,
                Arrays.asList("https://via.placeholder.com/400x300.png?text=Lampe+Design"),
                12,
                System.currentTimeMillis() - 43200000
        );

        Product p3 = new Product(
                "prod_3",
                "Sneakers Édition Limitée",
                "Paire de sneakers exclusives. Reconstitution 3D photoréaliste via Meta SAM.",
                220.00,
                "cat_fashion",
                "seller_103",
                "SneakersHub",
                "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Duck/glTF-Binary/Duck.glb",
                true,
                Arrays.asList("https://via.placeholder.com/400x300.png?text=Sneakers+3D"),
                2,
                System.currentTimeMillis() - 20000000
        );

        products.add(p1);
        products.add(p2);
        products.add(p3);
    }

    @Override
    public void getProducts(FilterCriteria filter, ApiCallback<List<Product>> callback) {
        if (filter == null) {
            callback.onSuccess(new ArrayList<>(products));
            return;
        }
        List<Product> filtered = new ArrayList<>();
        for (Product p : products) {
            if (filter.matches(p)) {
                filtered.add(p);
            }
        }
        callback.onSuccess(filtered);
    }

    @Override
    public void getProductById(String productId, ApiCallback<Product> callback) {
        for (Product p : products) {
            if (p.getId().equals(productId)) {
                callback.onSuccess(p);
                return;
            }
        }
        callback.onError("Produit introuvable: " + productId);
    }

    @Override
    public void getCategories(ApiCallback<List<Category>> callback) {
        callback.onSuccess(new ArrayList<>(categories));
    }

    @Override
    public void createProduct(Product product, ApiCallback<Product> callback) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId("prod_" + (products.size() + 1));
        }
        products.add(product);
        callback.onSuccess(product);
    }

    @Override
    public void updateProductModel3d(String productId, String model3dUrl, ApiCallback<Boolean> callback) {
        for (Product p : products) {
            if (p.getId().equals(productId)) {
                p.setModel3dUrl(model3dUrl);
                p.setArSupported(true);
                callback.onSuccess(true);
                return;
            }
        }
        callback.onError("Produit non trouvé pour la mise à jour 3D");
    }

    @Override
    public void getUserProfile(String userId, ApiCallback<User> callback) {
        User user = new User(userId, "Utilisateur Marketplace", "contact@marketplace.com", true, "", "Boutique Officielle");
        callback.onSuccess(user);
    }
}
