# Guide Pratique de Modification - Soutenance Universitaire

Ce document permet de répondre rapidement aux demandes du jury lors d'une soutenance (ex: « Ajoutez un champ », « Ajoutez un moyen de paiement », « Changez les couleurs de la charte », « Ajoutez un écran »).

---

## 1. Ajouter un Champ sur une Entité Existante

*Exemple : Ajouter le champ `brand` (marque) sur le produit (`Product`).*

1. **Modifier le Modèle** dans [`app/src/main/java/com/marketplace/app/model/Product.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/model/Product.java) :
   - Ajouter le champ privé :
     ```java
     private String brand;
     ```
   - Ajouter le getter et le setter :
     ```java
     public String getBrand() { return brand; }
     public void setBrand(String brand) { this.brand = brand; }
     ```
   - Adapter le constructeur complet si souhaité.

2. **Afficher le champ dans la Fiche Produit** dans [`app/src/main/res/layout/activity_product_detail.xml`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/res/layout/activity_product_detail.xml) :
   - Ajouter un `TextView` :
     ```xml
     <TextView
         android:id="@+id/tv_product_brand"
         android:layout_width="wrap_content"
         android:layout_height="wrap_content"
         android:textColor="#757575"
         android:textSize="14sp" />
     ```

3. **Lier la vue dans le contrôleur** dans [`app/src/main/java/com/marketplace/app/feature/catalogue/ProductDetailActivity.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/feature/catalogue/ProductDetailActivity.java) :
   - Dans `bindViews()` :
     ```java
     tvBrand = findViewById(R.id.tv_product_brand);
     ```
   - Dans `render()` :
     ```java
     if (product.getBrand() != null && !product.getBrand().isEmpty()) {
         tvBrand.setText("Marque : " + product.getBrand());
         tvBrand.setVisibility(View.VISIBLE);
     } else {
         tvBrand.setVisibility(View.GONE);
     }
     ```

4. **Si mode Mock activé** :
   - Définir une valeur d'exemple dans [`app/src/main/java/com/marketplace/app/backend/mock/MockBackendService.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/backend/mock/MockBackendService.java) lors de l'instanciation des produits factices.

---

## 2. Ajouter une Nouvelle Entité Métier

*Exemple : Ajouter une entité `Review` (Avis client).*

1. **Créer la classe Modèle** dans `app/src/main/java/com/marketplace/app/model/Review.java` :
   ```java
   package com.marketplace.app.model;

   import com.marketplace.app.base.BaseModel;
   import java.util.ArrayList;
   import java.util.List;

   public class Review extends BaseModel {
       private String id;
       private String productId;
       private String authorName;
       private int rating;
       private String comment;

       public Review() {}
       // Getters, Setters et validate()
       @Override
       public List<String> validate() {
           List<String> errors = new ArrayList<>();
           if (rating < 1 || rating > 5) errors.add("Note invalide (1-5)");
           return errors;
       }
   }
   ```

2. **Ajouter les endpoints dans Retrofit** dans [`app/src/main/java/com/marketplace/app/backend/api/MarketplaceApi.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/backend/api/MarketplaceApi.java) :
   ```java
   @GET("/api/products/{id}/reviews")
   Call<List<Review>> getProductReviews(@Path("id") String productId);

   @POST("/api/products/{id}/reviews")
   Call<Review> addReview(@Path("id") String productId, @Body Review review);
   ```

3. **Créer le Repository** dans `app/src/main/java/com/marketplace/app/backend/repository/ReviewRepository.java` étendant `BaseRepository`.

---

## 3. Ajouter un Nouveau Canal de Contact (ex: Telegram)

1. **Ajouter la valeur dans l'Enum** [`app/src/main/java/com/marketplace/app/model/ContactChannel.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/model/ContactChannel.java) :
   ```java
   public enum ContactChannel {
       WHATSAPP,
       MESSENGER,
       APPEL,
       TELEGRAM
   }
   ```

2. **Gérer le lien dans `ContactLinks`** dans [`app/src/main/java/com/marketplace/app/feature/contact/ContactLinks.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/feature/contact/ContactLinks.java) :
   ```java
   public static String buildTelegramUrl(String username) {
       if (username == null || username.trim().isEmpty()) return null;
       String cleanUser = username.trim().replace("@", "");
       return "https://t.me/" + cleanUser;
   }
   ```

3. **Ajouter le bouton dans le BottomSheet** dans [`app/src/main/java/com/marketplace/app/feature/contact/ContactBottomSheet.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/feature/contact/ContactBottomSheet.java).

---

## 4. Ajouter un Moyen de Paiement (ex: BNI, Carte Bancaire)

1. **Ajouter la valeur dans l'Enum** [`app/src/main/java/com/marketplace/app/model/PaymentMethod.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/model/PaymentMethod.java) :
   ```java
   public enum PaymentMethod {
       A_LA_LIVRAISON,
       MVOLA,
       ORANGE_MONEY,
       AIRTEL_MONEY,
       CARTE_BANCAIRE
   }
   ```

2. **Ajouter l'option RadioButton dans le layout de commande** [`app/src/main/res/layout/activity_checkout.xml`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/res/layout/activity_checkout.xml).
3. **Mettre à jour `CheckoutActivity.java`** pour associer l'option sélectionnée à `PaymentMethod.CARTE_BANCAIRE`.

---

## 5. Modifier la Charte Graphique, Couleurs et Thème

Toutes les couleurs sont centralisées dans [`app/src/main/res/values/colors.xml`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/res/values/colors.xml) :

- Pour changer la couleur primaire d'action (actuellement Orange 1688 `#FF6A00`) :
  ```xml
  <color name="primary">#FF6A00</color>
  <color name="primary_dark">#E65100</color>
  ```
- Pour changer la couleur d'accent AR 3D (actuellement Teal `#00897B`) :
  ```xml
  <color name="accent_ar">#00897B</color>
  ```
- Pour changer la devise par défaut (Ariary "Ar") :
  Modifier la constante dans [`app/src/main/java/com/marketplace/app/ui/PriceFormatter.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/ui/PriceFormatter.java) :
  ```java
  private static final String DEFAULT_CURRENCY = "Ar";
  ```

---

## 6. Ajouter un Nouvel Écran dans la Navigation

1. Créer l'Activity étendant `BaseActivity`.
2. Déclarer l'Activity dans [`app/src/main/AndroidManifest.xml`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/AndroidManifest.xml).
3. Ajouter une méthode statique dans [`app/src/main/java/com/marketplace/app/navigation/Router.java`](file:///c:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/navigation/Router.java) :
   ```java
   public static void openMonNouvelEcran(Context context, String param) {
       if (context == null) return;
       Intent intent = new Intent(context, MonNouvelEcranActivity.class);
       intent.putExtra("param", param);
       context.startActivity(intent);
   }
   ```
4. Appeler `Router.openMonNouvelEcran(this, ...)` depuis n'importe quelle vue.

---

## 7. Modifier le Service d'Achat Agence « Import 1688 » en Soutenance

- **Changer le taux CNY → MGA par défaut ou la commission d'agence** :
  - Dans l'application en direct : Ouvrir le bouton central **Import 1688** → **Admin** → modifier le taux (ex: `660`) et cliquer sur *Enregistrer Taux & Coordonnées Importateur*.
  - Dans le code : [`ImportAgencyConfig.createDefaultConfig()`](file:///c:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/model/ImportAgencyConfig.java#L33-L51).
- **Ajouter ou modifier une étape de suivi Import 1688** :
  - Modifier l'énumération [`ImportTrackingStatus.java`](file:///c:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/model/ImportTrackingStatus.java) (`stepOrder`, `label`, `description`).
- **Modifier le message final affiché lorsque le colis arrive chez l'importateur (`RECU_PAR_IMPORTATEUR`)** :
  - Modifier la constante `FINAL_IMPORTER_MESSAGE` dans [`ImportOrder.java`](file:///c:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/model/ImportOrder.java#L15-L19).
