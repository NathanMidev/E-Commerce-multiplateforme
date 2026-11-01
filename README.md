# Marketplace Mobile 3D AR - Application Android & API

Application mobile Android de marketplace moderne multi-vendeurs avec visualisation en **Réalité Augmentée (AR 3D)**, numérisation d'objets via **Meta SAM**, gestion des paniers, commandes, paiements locaux (Ariary, MVola, Orange Money, Airtel Money) et contact direct vendeur (WhatsApp, Messenger, Appel téléphonique).

---

## 🛠️ Stack Technique

- **Plateforme** : Android (Java 17 / Java 21)
- **Architecture** : Module unique `app` (Package `com.marketplace.app`), patron Clean Architecture découplé
- **Build System** : Gradle 8.10.2 & Android Gradle Plugin 8.7.3
- **Compatibilité** : `minSdkVersion 24` (Android 7.0), `compileSdkVersion 35` (Android 15)
- **Réseau & API** : Retrofit 2.9.0, Gson 2.10.1, OkHttp 4.12.0
- **Authentification & Passerelle 3D** : Firebase Auth (Email/MDP + Google Sign-In), Firebase Storage & Firestore
- **Réalité Augmentée** : Google ARCore (`com.google.ar:core:1.46.0`) & Scene Viewer Google (format standard `.glb` / `.gltf`)
- **Chargement d'images** : Glide 4.16.0
- **UI / Design** : Material Design 3, palette commerciale inspirée de 1688 (Orange dynamique `#FF6A00`, fond `#F8F9FA`, cartes blanches arrondies, devises en Ariary `Ar`)

---

## 🏛️ Structure du Projet

```text
E-Commerce-multiplateforme/
├── app/                                 --> Module Android unique
│   ├── src/main/java/com/marketplace/app/
│   │   ├── base/                        --> Classes abstraites (BaseModel, BaseActivity, BaseRepository, BaseAdapter)
│   │   ├── model/                       --> Modèles POJO (User, Product, Cart, Order, Payment, ContactRequest...)
│   │   ├── backend/
│   │   │   ├── api/                     --> Retrofit MarketplaceApi, ApiClient
│   │   │   ├── repository/              --> ProductRepository, CartRepository, OrderRepository...
│   │   │   └── mock/                    --> MockBackendService (Démonstration hors-ligne complète)
│   │   ├── navigation/                  --> Router (Navigation centralisée statique)
│   │   ├── transaction/                 --> Panier, Checkout, Commandes, Paiement simulé
│   │   ├── feature/
│   │   │   ├── auth/                    --> Connexion, Inscription, Profil, FirebaseAuthSession
│   │   │   ├── catalogue/               --> Grille produits, filtres, fiche détaillée, publication
│   │   │   ├── boutique/                --> Vitrine vendeur
│   │   │   ├── contact/                 --> ContactBottomSheet, liens WhatsApp / Messenger / Appel
│   │   │   ├── scan3d/                  --> Capture photo 360°, suivi IA Meta SAM, pont Firebase
│   │   │   └── ar/                      --> ArLauncher, visualiseur ARCore / Scene Viewer
│   │   ├── ui/                          --> PriceFormatter, PriceView, SellerBadge, QuantityStepper
│   │   ├── MarketplaceApp.java          --> Classe Application (Initialisation session et backend)
│   │   └── MainActivity.java            --> Accueil / Dashboard
│   ├── src/test/                        --> Tests unitaires JVM (CommerceMockTest, AuthUnitTest...)
│   └── google-services.json.example     --> Modèle de configuration Firebase
│
├── backend/                             --> API REST commune protégée (PostgreSQL / Express / Spring)
│   ├── README.md                        --> Documentation des routes de l'API commune
│   └── ...
│
└── docs/
    ├── ARCHITECTURE.md                  --> Documentation détaillée de l'architecture
    └── MODIFIER.md                      --> Guide des modifications en direct pour la soutenance
```

---

## 🚀 Démarrage Rapide

### 1. Configuration Firebase

1. Rendez-vous sur la console [Firebase](https://console.firebase.google.com/).
2. Créez un projet ou utilisez un projet existant avec le package **`com.marketplace.app`**.
3. Téléchargez le fichier `google-services.json` et déposez-le dans le dossier `app/` (remplacez ou configurez selon `app/google-services.json.example`).
4. Activez les fournisseurs **Email/Mot de passe** et **Google** dans *Authentication*.
5. Activez **Cloud Storage** et **Cloud Firestore** si vous testez le pont de scan SAM réel.

### 2. Configuration de l'API / Mode Hors-Ligne (Mock)

Dans [`MarketplaceApp.java`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/app/src/main/java/com/marketplace/app/MarketplaceApp.java) :

```java
// Pour une démonstration 100% autonome sans serveur (recommandé pour la soutenance) :
ApiClient.setUseMock(true);

// Pour se connecter à votre serveur local ou distant :
ApiClient.setUseMock(false);
ApiClient.setBaseUrl("http://10.0.2.2:3000/"); // 10.0.2.2 pour l'émulateur Android vers localhost
```

### 3. Compilation et Tests

Exécutez dans un terminal à la racine :

```bash
# Lancer tous les tests unitaires
./gradlew testDebugUnitTest

# Compiler l'application et générer l'APK de débogage
./gradlew assembleDebug
```

L'APK généré se trouvera dans :
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🥽 Réalité Augmentée & Modèles 3D

L'application prend en charge les modèles 3D au format `.glb` / `.gltf` :
- **Sur appareil physique compatible ARCore** : Scene Viewer de Google s'ouvre automatiquement en réalité augmentée à l'échelle réelle dans la pièce.
- **Sur émulateur ou appareil sans ARCore** : Un visualiseur 3D alternatif ou le mode 3D interactif est utilisé sans faire planter l'application.

---

## 🧪 Tests Unitaires Intégrés

1. **`CommerceMockTest`** : Vérifie l'ensemble du cycle commercial (ajout au panier, atomicité des stocks, découpage des commandes par vendeur, cycle de vie et restockage lors d'une annulation, conversion QueryMap).
2. **`AuthUnitTest`** : Vérifie les règles de permissions métier (accès AR restreint aux utilisateurs connectés, exigences pour devenir vendeur, transitions de rôles).
3. **`ContactLinksTest`** : Vérifie la normalisation des numéros de téléphone malgaches (`+261`) et la génération des liens d'encodage WhatsApp et Messenger.
4. **`ModelUnitTest`** : Vérifie la validation stricte de tous les modèles POJO (`Product`, `Cart`, `Order`, `User`).

---

## 📚 Documentation Additionnelle

- Consultez [`docs/ARCHITECTURE.md`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/docs/ARCHITECTURE.md) pour les diagrammes et détails techniques complets.
- Consultez [`docs/MODIFIER.md`](file:///C:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/docs/MODIFIER.md) pour les pas-à-pas de modification en temps réel devant le jury de soutenance.
