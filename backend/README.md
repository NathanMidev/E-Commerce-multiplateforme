# Backend MarketplaceApp : PostgreSQL + API REST

Une seule base **PostgreSQL**, une seule **API REST**, deux clients : l'application Android et la version web.
Tout ce que l'un fait (publier un produit, remplir son panier, passer commande…) est immédiatement visible par l'autre.

```
 App Android (Java)  ──┐                              ┌── Firebase Auth
                       ├──►  API REST (Node/Express) ─┤     (vérifie QUI est l'utilisateur)
 Version web         ──┘        /api/...              └── PostgreSQL
                                                            (produits, profils, paniers, commandes)
```

- **Firebase Auth** ne sert qu'à l'identité (Google, email/mot de passe). Chaque requête porte le jeton Firebase ;
  l'API le vérifie et en déduit l'utilisateur.
- **PostgreSQL** contient toutes les données, y compris le profil (nom, contacts, ventes, niveau).
- Les règles importantes (stock, statuts de commande, niveau du vendeur) sont appliquées **côté serveur/base**,
  jamais par le client : ni l'app ni le site ne peuvent les contourner.

## Démarrage rapide (Docker)

```bash
cd backend
cp .env.example .env        # puis éditer .env (mot de passe, FIREBASE_PROJECT_ID)
docker compose up --build
curl http://localhost:8080/api/health      # {"status":"ok","auth":"firebase"}
curl http://localhost:8080/api/products    # les 3 produits de démonstration
```

`FIREBASE_PROJECT_ID` = le champ `project_id` de `app/google-services.json`.
Le schéma et les données de démo ne sont chargés qu'à la création du volume ; pour repartir de zéro : `docker compose down -v`.

### Sans Docker
```bash
# 1) PostgreSQL 14+ : créer le rôle et la base, puis charger le schéma
psql -U postgres -c "CREATE ROLE marketplace LOGIN PASSWORD 'marketplace' CREATEDB;"
psql -U postgres -c "CREATE DATABASE marketplace OWNER marketplace;"
psql -U marketplace -d marketplace -f db/01_schema.sql -f db/02_seed.sql

# 2) API (Node 20+)
cd api && npm install
DATABASE_URL=postgres://marketplace:marketplace@localhost:5432/marketplace \
FIREBASE_PROJECT_ID=votre-project-id npm start
```

### Essayer sans Firebase (local uniquement)
`AUTH_MODE=dev` remplace la vérification du jeton par un simple en-tête `X-Dev-Uid`. **N'importe qui peut alors se faire
passer pour n'importe qui** : c'est refusé au démarrage si `NODE_ENV=production`, mais ne l'exposez jamais sur Internet.
```bash
AUTH_MODE=dev npm start
curl -H "X-Dev-Uid: u_demo" -H "X-Dev-Name: Demo" http://localhost:8080/api/users/me
```

## Brancher l'application Android

Dans `gradle.properties` (à la racine du projet Android) :

| Situation | `apiBaseUrl` |
|---|---|
| Émulateur Android | `http://10.0.2.2:8080/api/` (10.0.2.2 = le PC) |
| Vrai téléphone, même Wi-Fi | `http://IP-DU-PC:8080/api/` (pare-feu : autoriser le port 8080) |
| Serveur en ligne | `https://votre-domaine/api/` |

- `useMockBackend=true` : démo **hors ligne** avec des données en mémoire (utile si le serveur n'est pas disponible le jour J).
- Le HTTP non chiffré n'est autorisé qu'en **debug** (`app/src/debug/AndroidManifest.xml`). En release, utilisez HTTPS.
- Après avoir modifié `gradle.properties` : *File ▸ Sync Project with Gradle Files*.

## Brancher la version web

1. Ajouter l'adresse du site dans `CORS_ORIGINS` (séparées par des virgules) du fichier `.env`.
2. Se connecter avec le **même projet Firebase** (SDK Web), puis envoyer le jeton à l'API :

```js
const token = await firebase.auth().currentUser.getIdToken();
const res = await fetch('https://votre-api/api/cart', { headers: { Authorization: `Bearer ${token}` } });
```
Le catalogue (`GET /api/products`, `/api/categories`) est public, sans jeton.

## Modèle de données

```mermaid
erDiagram
    users ||--o{ products : "vend"
    categories ||--o{ products : "classe"
    users ||--o| carts : "possède"
    carts ||--o{ cart_items : "contient"
    products ||--o{ cart_items : "est dans"
    users ||--o{ orders : "achète (buyer)"
    users ||--o{ orders : "vend (seller)"
    orders ||--|{ order_items : "détaille"
    products |o--o{ order_items : "référence"
```

| Table | Rôle |
|---|---|
| `users` | Profil (id = uid Firebase) : nom, pdp, ville, adresse, téléphone, WhatsApp, lien Facebook, `total_sales`, `response_score` |
| `categories`, `products` | Catalogue. `modele3d` est calculé depuis `model3d_url` ; le statut suit le stock |
| `carts`, `cart_items` | Un panier par utilisateur |
| `orders`, `order_items` | **Une commande par vendeur** à chaque passage en caisse. Titre et prix sont copiés dans `order_items` : l'historique ne change plus si l'annonce est modifiée ou supprimée |

Vue `v_users` et fonction `user_level()` : niveau du compte calculé en SQL.

## Règles métier

- **Niveau du compte** (jamais choisi, ni modifiable par le client) : `PRO` si ≥ 50 ventes **et** réactivité ≥ 4,5 ; `SELLER` dès 1 vente ; sinon `BUYER`. Même règle que `User.resolveRole()` côté Android.
- **Une vente** = une commande **confirmée reçue par l'acheteur** (`LIVREE`). Le vendeur ne peut pas se l'attribuer.
- **Stock** : réservé au passage en caisse ; restitué si la commande est annulée ; à 0 le produit passe `INDISPONIBLE`, et redevient `DISPONIBLE` au réapprovisionnement. Deux acheteurs simultanés sur la dernière unité : un seul réussit.
- **Passage en caisse atomique** : si un article n'est plus disponible, rien n'est commandé et le panier est conservé.
- **Cycle d'une commande** :

| De | Vers | Qui |
|---|---|---|
| `EN_ATTENTE` | `CONFIRMEE` | vendeur |
| `EN_ATTENTE` | `ANNULEE` | acheteur ou vendeur |
| `CONFIRMEE` | `EXPEDIEE` | vendeur |
| `CONFIRMEE` | `ANNULEE` | vendeur |
| `EXPEDIEE` | `LIVREE` | acheteur |

- On ne peut pas acheter son propre produit. Publier exige un moyen de contact (WhatsApp ou téléphone) sur son profil.
- Contacts (téléphone, WhatsApp, Facebook) visibles **uniquement par les comptes connectés** ; l'adresse complète et l'email restent privés.
- Les numéros sont normalisés (`034 12 345 67` → `+261341234567`).

## Référence de l'API (préfixe `/api`)

Erreurs : `{ "error": { "code": "…", "message": "…", "details": … } }`. 🔒 = jeton requis.

| Méthode | Route | Description |
|---|---|---|
| GET | `/health` | État du serveur |
| GET | `/categories` | Catégories |
| GET | `/products` | Catalogue. Filtres : `categoryId`, `minPrice`, `maxPrice`, `arOnly`, `searchQuery`, `sortBy` (`recent`, `price_asc`, `price_desc`), `sellerId`, `limit`, `offset` |
| GET | `/products/:id` | Détail d'un produit |
| POST 🔒 | `/products` | Publier un produit (le vendeur est l'utilisateur du jeton) |
| PATCH 🔒 | `/products/:id` | Modifier (propriétaire seulement) |
| PATCH 🔒 | `/products/:id/model3d` | Enregistrer l'URL du modèle 3D |
| DELETE 🔒 | `/products/:id` | Supprimer (propriétaire seulement) |
| GET 🔒 | `/users/me` | Mon profil (créé à la première connexion) |
| PATCH 🔒 | `/users/me` | Modifier : `name`, `avatarUrl`, `city`, `address`, `phoneNumber`, `whatsapp`, `facebookLink` |
| GET | `/users/:id` | Profil d'un vendeur (contacts si connecté) |
| GET 🔒 | `/cart` | Mon panier |
| POST 🔒 | `/cart/items` | Ajouter `{ productId, quantity }` (cumule) |
| PUT 🔒 | `/cart/items/:productId` | Fixer la quantité `{ quantity }` (0 = retirer) |
| DELETE 🔒 | `/cart/items/:productId`, `/cart` | Retirer un article / vider |
| POST 🔒 | `/orders/checkout` | Commander `{ deliveryAddress?, deliveryPhone?, paymentMethod?, note? }` → `{ orders: [...] }` |
| GET 🔒 | `/orders?role=buyer\|seller&status=` | Mes achats / mes ventes |
| GET 🔒 | `/orders/:id` | Détail (acheteur ou vendeur) |
| PATCH 🔒 | `/orders/:id/status` | `{ status }` selon le cycle ci-dessus |

Modes de paiement (`paymentMethod`) : `A_LA_LIVRAISON` (défaut), `MVOLA`, `ORANGE_MONEY`, `AIRTEL_MONEY`.
Le choix est **déclaratif** : l'application n'effectue aucun paiement.

## Tests

```bash
cd backend/api && npm test      # 26 tests de bout en bout, sur une base PostgreSQL temporaire
psql -U marketplace -d marketplace -f ../db/tests/rules_test.sql   # règles SQL (annulé à la fin)
```
`npm test` utilise `DATABASE_URL` (le rôle doit pouvoir créer une base) et supprime la base temporaire ensuite.

## Limites connues et pistes

- **Aucun paiement réel.** Pour Mvola / Orange Money / Airtel Money il faudrait intégrer leurs API marchands.
- **`response_score` n'est pas encore calculé** (il reste à 0) : tant qu'un traitement ne l'alimente pas, personne n'atteint le niveau `PRO`. À brancher sur la mesure de réactivité (délai de réponse aux demandes de contact).
- **Fraude aux ventes** : la confirmation par l'acheteur limite l'auto-attribution, mais un vendeur peut encore créer un second compte acheteur. Pistes : vérification du téléphone, limites par acheteur, modération.
- **Pas de limitation de débit** (rate limiting) ni de migrations versionnées : à ajouter avant une vraie mise en production (`express-rate-limit`, outil de migration).
- **Devise** : les prix sont des `NUMERIC(14,2)` sans devise ; les prix de démonstration reprennent ceux du mock Android.
- **Images et modèles 3D** : l'API stocke des **URL** ; l'hébergement des fichiers (photos de profil, `.glb`) est à choisir séparément.
- **Hébergement** : toute plateforme qui exécute Node 20+ et fournit un PostgreSQL 14+ convient. Lancez `db/01_schema.sql` puis `02_seed.sql` (facultatif) une fois, définissez `DATABASE_URL`, `FIREBASE_PROJECT_ID`, `CORS_ORIGINS`, et servez l'API en HTTPS.
