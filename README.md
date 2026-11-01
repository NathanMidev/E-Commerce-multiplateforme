# Marketplace Mobile 3D AR & Service d'Achat Agence Import 1688

Application Android native (**Java 17**, package `com.marketplace.app`) combinant :
1. **Marketplace Locale (inspirée de Deal.mg)** : Catalogue multi-vendeurs, recherche, filtres, panier, passage en caisse, commandes, contact direct vendeur (WhatsApp, Messenger, Appel) et visualisation **Réalité Augmentée (AR 3D)** / numérisation 3D **Meta SAM**.
2. **Service d'Achat Agence « Import 1688 »** : Accessible depuis le **bouton central circulaire « Import 1688 »** de la barre de navigation inférieure :
   - Navigation intégrée sécurisée sur **1688.com** ou partage/collage direct d'un lien produit 1688.
   - **Double affichage monétaire** : Prix original en **Yuan chinois (`CNY` / `¥`)** et conversion en **Ariary malgache (`MGA` / `Ar`)** via `CurrencyConversionService`, avec détail des frais de prestation d'achat de l'agence.
   - **Paiement MVola en deux étapes** : Confirmation du montant définitif par l'administrateur, puis envoi de la référence de transaction MVola par le client (statut maintenu à `EN_ATTENTE` jusqu'à la vérification administrative).
   - **Achat réel sur 1688 par l'agence** (avec la carte bancaire de l'agence) et **suivi logistique en 6 étapes** (`PAIEMENT_EN_ATTENTE`, `PAIEMENT_CONFIRME`, `ACHAT_EFFECTUE`, `PREPARATION_FOURNISSEUR`, `EXPEDIE_VERS_IMPORTATEUR`, `RECU_PAR_IMPORTATEUR`) jusqu'à la réception par l'importateur partenaire.

---

## 🛠️ Stack Technique

- **Plateforme** : Android (Java 17) — Module unique `:app` (`com.marketplace.app`)
- **Build System** : Gradle 8.10.2 & Android Gradle Plugin 8.7.3 (`minSdk 24`, `compileSdk 35`)
- **Réseau & API** : Retrofit 2.9.0, Gson 2.10.1, OkHttp 4.12.0 (avec injection automatique du jeton Firebase `Bearer`)
- **Firebase (`bizinevo-3d`)** : Firebase Auth (Email/Mot de passe + Google Sign-In), Firebase Storage (`scans/{uid}/{jobId}/`) & Cloud Firestore (`scan_jobs/{jobId}`)
- **Réalité Augmentée** : Google ARCore (`com.google.ar:core:1.46.0`) & Scene Viewer (`.glb` / `.gltf`)

---

## 🚀 Compilation et Tests

Exécutez à la racine du projet :

```powershell
# Nettoyer, exécuter tous les tests unitaires et compiler l'APK debug
.\gradlew.bat clean testDebugUnitTest assembleDebug
```

- **APK généré** : `app/build/outputs/apk/debug/app-debug.apk`
- **Rapport des tests unitaires** : `app/build/reports/tests/testDebugUnitTest/index.html`

---

## 🧪 Tests Unitaires Inclus

- **`Import1688UnitTest`** : Conversion CNY → MGA, calcul des quantités et commissions d'agence, validation et sérialisation JSON (`Gson`) de `ImportOrder`, interdiction d'auto-confirmation du paiement MVola, enregistrement de l'achat réel 1688, progression des 6 statuts de suivi jusqu'à `RECU_PAR_IMPORTATEUR`, contrôle d'accès administrateur (`AdminAccessControl`), et gestion des erreurs HTTP API.
- **`CommerceMockTest`** : Cycle commercial complet du panier et des commandes locales.
- **`AuthUnitTest`** : Règles d'authentification et permissions vendeur/AR.
- **`ContactLinksTest`** : Normalisation des numéros malgaches (`+261`) et génération d'URL WhatsApp/Messenger.
- **`ModelUnitTest`** : Validation des modèles métier.

---

## 📚 Documentation

- [`docs/ARCHITECTURE.md`](file:///c:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/docs/ARCHITECTURE.md) : Architecture complète, flux Import 1688, contrat REST API et règles de sécurité Firebase (`firestore.rules`, `storage.rules`).
- [`docs/MODIFIER.md`](file:///c:/Users/ASUS/StudioProjects/E-Commerce-multiplateforme/docs/MODIFIER.md) : Guide rapide pour modifier l'application pendant la soutenance.
