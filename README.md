# Marketplace Mobile 3D AR - Architecture Monolithe Modulaire (Android Java)

Application mobile Android de marketplace en ligne avec affichage de produits multi-vendeurs, essayage virtuel en **Réalité Augmentée (AR 3D)**, numérisation 3D via le modèle **Meta SAM (Segment Anything Model)**, filtrage multicritères par catégorie, et messagerie instantanée intégrée.

---

## 🛠️ Spécifications & Technologies

- **Langage** : Java (compatible Java 17 & Java 21)
- **Outil de build** : Gradle 8.10.2 & Android Gradle Plugin 8.7.3
- **Compatibilité Android** : `minSdkVersion 21` (Android 5.0 Lollipop) jusqu'à `compileSdkVersion 35` / `targetSdkVersion 35` (Android 15)
- **UI & Vues** : Android Views modernes avec modèle d'états vides (*Empty Views pattern*), Material Design 3, RecyclerView, CardView
- **Base de données backend** : PostgreSQL (modèles relationnels, DAO, client d'API REST)
- **Passerelle IA & Temps réel** : Firebase (Cloud Storage & Firestore / Realtime DB) servant de pont de synchronisation avec l'ordinateur exécutant le modèle **Meta SAM**
- **Moteur AR 3D** : Visualisation en réalité augmentée à échelle réelle dans l'environnement physique de l'acheteur (format standard 3D `.glb` / `.gltf`)

---

## 🏛️ Architecture : Monolithe Modulaire Découplé

L'architecture est organisée selon les principes du **Monolithe Modulaire** :
1. **Isolation stricte des modules de fonctionnalités (`feature`)** :
   - `:feature:products` ne dépend **PAS** de `:feature:chat` ni de `:feature:ar3d`.
   - Lors de l'exécution des **tests unitaires d'un module**, aucun autre module n'est appelé.
2. **Contrats de navigation découplés (`:core:navigation`)** :
   - Chaque module expose ou consomme des interfaces (`ProductRouter`, `ArRouter`, `ScannerRouter`, `ChatRouter`, `AuthRouter`).
   - Un registre central (`RouterRegistry`) fournit des implémentations *No-Op* par défaut lors des tests unitaires, évitant les crashs ou dépendances circulaires.
3. **Module Intégrateur (`:app`) & Le Grand Test** :
   - Le module principal `:app` est le point d'assemblage du monolithe.
   - `AppGlobalRouter` enregistre les routeurs concrets de tous les sous-modules.
   - Le **Grand Test d'Intégration** (`GrandIntegrationTest.java`) vérifie le fonctionnement de bout en bout : du catalogue filtré, au déclenchement du scan SAM, jusqu'à la discussion vendeur et l'essayage 3D AR.

---

## 📂 Organisation des Dossiers et Fichiers

```text
E-Commerce-multiplateforme/
│
├── gradlew, gradlew.bat, gradle/     --> Gradle Wrapper (8.10.2)
├── settings.gradle                   --> Déclaration des modules du monolithe
├── build.gradle                      --> Configuration racine des plugins AGP 8.7.3
├── gradle.properties                 --> JVM & configuration AndroidX / Java 21
│
├── app/                              --> [MODULE PRINCIPAL & GRAND TEST]
│   ├── build.gradle                  --> Dépend de tous les modules core & features
│   └── src/
│       ├── main/java/com/marketplace/app/
│       │   ├── MarketplaceApp.java       --> Application (Initialisation globale des routeurs)
│       │   ├── MainActivity.java         --> Dashboard principal avec Empty Views
│       │   └── router/
│       │       └── AppGlobalRouter.java  --> Fichier principal liant tous les routeurs
│       ├── main/res/                     --> Layouts (activity_main.xml), thèmes, couleurs
│       └── test/java/com/marketplace/app/
│           └── GrandIntegrationTest.java --> LE GRAND TEST D'INTÉGRATION
│
├── core/                             --> [MODULES CORE DU MONOLITHE]
│   ├── model/                        --> Modèles de données partagés
│   │   ├── Product.java              --> Produit, prix, vendeur, URL 3D GLB, statut AR
│   │   ├── Category.java             --> Catégorie de produits
│   │   ├── FilterCriteria.java       --> Logique de filtre par catégorie, prix et AR
│   │   ├── SamScanJob.java           --> Job de scan et progression IA SAM (Meta)
│   │   ├── ChatMessage.java          --> Message entre acheteur et vendeur
│   │   └── User.java                 --> Profil utilisateur / vendeur
│   │
│   ├── navigation/                   --> Contrats de routage & Registry
│   │   ├── RouterRegistry.java       --> Registre singleton (No-Op par défaut en tests)
│   │   ├── ProductRouter.java        --> Contrat de routage Catalogue & Détail
│   │   ├── ArRouter.java             --> Contrat de routage Visualiseur AR 3D
│   │   ├── ScannerRouter.java        --> Contrat de routage Scanner & Statut SAM
│   │   ├── ChatRouter.java           --> Contrat de routage Messagerie Vendeur
│   │   └── AuthRouter.java           --> Contrat de routage Connexion & Profil
│   │
│   ├── network/                      --> Couche Réseau PostgreSQL Backend
│   │   ├── PostgresBackendApi.java   --> Interface des endpoints REST/SQL
│   │   ├── MockPostgresBackendService.java --> Jeu de données marketplace (3D & AR prêts)
│   │   └── ApiResponse.java          --> Wrapper générique de réponse
│   │
│   ├── database/                     --> Cache local & persistence
│   │   └── LocalProductCache.java    --> Cache mémoire / favoris hors-ligne
│   │
│   └── firebase/                     --> Passerelle Firebase SAM (Meta) & Chat
│       ├── FirebaseSamBridgeService.java --> Synchronisation du scan vers le PC (IA SAM)
│       └── FirebaseChatService.java  --> Messagerie temps réel acheteur-vendeur
│
└── feature/                          --> [MODULES FONCTIONNALITÉS DÉCOUPLÉS]
    │
    ├── products/                     --> Catalogue, recherche et filtres catégories
    │   ├── router/ProductModuleRouter.java
    │   ├── ui/
    │   │   ├── ProductCatalogActivity.java  --> Liste avec filtres & Empty View
    │   │   ├── ProductDetailActivity.java   --> Fiche avec boutons AR, Chat & Scan
    │   │   └── ProductAdapter.java          --> Carte produit avec badge 3D AR
    │   └── src/test/.../ProductUnitTest.java--> Tests unitaires isolés (sans Chat ni AR)
    │
    ├── ar3d/                         --> Réalité Augmentée 3D (Essayage environnement)
    │   ├── router/ArModuleRouter.java
    │   ├── ui/
    │   │   ├── ArViewerActivity.java        --> Surface AR, ancrage sol & manipulation
    │   │   └── Model3dPreviewActivity.java  --> Aperçu 3D interactif
    │   └── src/test/.../ArUnitTest.java     --> Tests unitaires isolés
    │
    ├── scanner3d/                    --> Numérisation 3D multi-angles (Pipeline SAM)
    │   ├── router/ScannerModuleRouter.java
    │   ├── ui/
    │   │   ├── ScanCaptureActivity.java        --> Capture de clichés sous différents angles
    │   │   └── SamProcessingStatusActivity.java--> Progression SAM en direct (Firebase)
    │   └── src/test/.../ScannerUnitTest.java   --> Tests unitaires isolés
    │
    ├── chat/                         --> Messagerie instantanée acheteur / vendeur
    │   ├── router/ChatModuleRouter.java
    │   ├── ui/
    │   │   ├── ChatConversationActivity.java  --> Écran de discussion avec Empty State
    │   │   └── ChatListActivity.java          --> Liste des conversations
    │   └── src/test/.../ChatUnitTest.java     --> Tests unitaires isolés
    │
    └── auth/                         --> Authentification & Profil
        ├── router/AuthModuleRouter.java
        ├── ui/LoginActivity.java, RegisterActivity.java, UserProfileActivity.java
        └── src/test/.../AuthUnitTest.java     --> Tests unitaires isolés
```

---

## 🔄 Fonctionnement du Pipeline IA Meta SAM & 3D AR

```
[Vendeur Mobile (:feature:scanner3d)]
    │  (1. Capture multi-angles de l'objet)
    ▼
[Firebase Storage & Firestore (:core:firebase)]
    │  (2. Notification de nouveau Job SAM)
    ▼
[PC / Serveur GPU avec Meta SAM]
    │  (3. Détection de contour & segmentation SAM)
    │  (4. Reconstruction 3D -> fichier .glb)
    ▼
[Firebase & Base PostgreSQL (:core:network)]
    │  (5. Mise à jour de l'URL du modèle 3D)
    ▼
[Acheteur Mobile (:feature:ar3d)]
    │  (6. Visualisation AR à l'échelle dans sa pièce)
    ▼
[Discussion & Achat (:feature:chat)]
```

---

## 🧪 Exécution des Tests

### 1. Tests Unitaires Isolés par Module
Chaque module s'exécute de façon totalement autonome sans instancier ni appeler les autres :
```bash
# Test uniquement du catalogue et des filtres
./gradlew :feature:products:testDebugUnitTest

# Test uniquement de la messagerie
./gradlew :feature:chat:testDebugUnitTest

# Test uniquement du scanner SAM
./gradlew :feature:scanner3d:testDebugUnitTest
```

### 2. Le Grand Test d'Intégration
Vérifie la liaison de tous les modules via le routeur principal et le bon flux de données :
```bash
./gradlew :app:testDebugUnitTest --tests "com.marketplace.app.GrandIntegrationTest"
```
