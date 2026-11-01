-- =============================================================================
--  MarketplaceApp : schéma PostgreSQL (source de vérité partagée app mobile + web)
--  PostgreSQL 14+ (testé sur 16).  Les noms d'énumérations suivent ceux de l'app Android.
-- =============================================================================

-- ------------------------------------------------------------------ Types ----
CREATE TYPE product_status AS ENUM ('NOUVEAU', 'DISPONIBLE', 'INDISPONIBLE', 'A_VENDRE');
CREATE TYPE order_status   AS ENUM ('EN_ATTENTE', 'CONFIRMEE', 'EXPEDIEE', 'LIVREE', 'ANNULEE');
CREATE TYPE payment_method AS ENUM ('A_LA_LIVRAISON', 'MVOLA', 'ORANGE_MONEY', 'AIRTEL_MONEY');

-- ------------------------------------------------------- Fonctions communes --
CREATE FUNCTION set_updated_at() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END $$;

-- ------------------------------------------------------------------ users ----
-- id = uid Firebase Auth (l'authentification reste gérée par Firebase).
-- Les statistiques (total_sales, response_score) ne sont jamais écrites par un client :
-- total_sales est mis à jour par le trigger de commande, response_score par un traitement serveur.
CREATE TABLE users (
    id             TEXT PRIMARY KEY,
    name           TEXT         NOT NULL CHECK (char_length(btrim(name)) BETWEEN 1 AND 120),
    email          TEXT,
    avatar_url     TEXT,
    city           TEXT,                       -- public (quartier / ville)
    address        TEXT,                       -- privé (livraison)
    phone_number   TEXT,
    whatsapp       TEXT,
    facebook_link  TEXT,
    total_sales    INTEGER      NOT NULL DEFAULT 0 CHECK (total_sales >= 0),
    response_score NUMERIC(2,1) NOT NULL DEFAULT 0 CHECK (response_score BETWEEN 0 AND 5),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_email_unique ON users (lower(email)) WHERE email IS NOT NULL;
CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Niveau du compte (même règle que User.resolveRole() dans l'app Android) :
--   PRO    : >= 50 ventes ET réactivité >= 4.5
--   SELLER : au moins 1 vente
--   BUYER  : aucune vente
CREATE FUNCTION user_level(p_total_sales INTEGER, p_response_score NUMERIC) RETURNS TEXT
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE
        WHEN p_total_sales >= 50 AND p_response_score >= 4.5 THEN 'PRO'
        WHEN p_total_sales > 0 THEN 'SELLER'
        ELSE 'BUYER'
    END
$$;

CREATE VIEW v_users AS
    SELECT u.*, user_level(u.total_sales, u.response_score) AS level FROM users u;

-- ------------------------------------------------------------- categories ----
CREATE TABLE categories (
    id          TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    icon_name   TEXT
);

-- --------------------------------------------------------------- products ----
CREATE TABLE products (
    id            TEXT PRIMARY KEY DEFAULT ('prod_' || replace(gen_random_uuid()::text, '-', '')),
    title         TEXT           NOT NULL CHECK (char_length(btrim(title)) > 0),
    description   TEXT           NOT NULL DEFAULT '',
    price         NUMERIC(14,2)  NOT NULL CHECK (price >= 0),
    status        product_status NOT NULL DEFAULT 'NOUVEAU',
    category_id   TEXT REFERENCES categories (id),
    seller_id     TEXT           NOT NULL REFERENCES users (id),
    thumbnail_url TEXT,
    model3d_url   TEXT,
    -- vrai dès qu'un modèle 3D existe (équivalent de Product.setModel3dUrl côté Android)
    modele3d      BOOLEAN GENERATED ALWAYS AS (model3d_url IS NOT NULL AND btrim(model3d_url) <> '') STORED,
    image_urls    TEXT[]         NOT NULL DEFAULT '{}',
    stock         INTEGER        NOT NULL DEFAULT 0 CHECK (stock >= 0),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX products_category_idx ON products (category_id);
CREATE INDEX products_seller_idx   ON products (seller_id);
CREATE INDEX products_created_idx  ON products (created_at DESC);
CREATE TRIGGER trg_products_updated BEFORE UPDATE ON products FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Cohérence stock / statut (équivalent de Product.setStock) :
--   stock = 0 -> INDISPONIBLE ; réapprovisionnement d'un produit épuisé -> DISPONIBLE
CREATE FUNCTION products_sync_status() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.stock <= 0 THEN
        NEW.status := 'INDISPONIBLE';
    ELSIF TG_OP = 'UPDATE' AND OLD.stock <= 0
          AND OLD.status = 'INDISPONIBLE' AND NEW.status = 'INDISPONIBLE' THEN
        NEW.status := 'DISPONIBLE';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_products_status BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION products_sync_status();

-- ------------------------------------------------------------------ panier ----
-- Un seul panier par utilisateur ; le prix reste celui du produit (figé à la commande).
CREATE TABLE carts (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    TEXT        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    cart_id    BIGINT      NOT NULL REFERENCES carts (id)    ON DELETE CASCADE,
    product_id TEXT        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity   INTEGER     NOT NULL CHECK (quantity > 0),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (cart_id, product_id)
);

-- --------------------------------------------------------------- commandes ----
-- Le passage en caisse crée UNE commande par vendeur (le vendeur gère sa part).
CREATE TABLE orders (
    id               TEXT PRIMARY KEY DEFAULT ('ord_' || replace(gen_random_uuid()::text, '-', '')),
    order_number     BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,      -- numéro lisible : CMD-000123
    buyer_id         TEXT           NOT NULL REFERENCES users (id),
    seller_id        TEXT           NOT NULL REFERENCES users (id),
    status           order_status   NOT NULL DEFAULT 'EN_ATTENTE',
    payment_method   payment_method NOT NULL DEFAULT 'A_LA_LIVRAISON', -- déclaratif : aucun paiement en ligne
    delivery_address TEXT           NOT NULL CHECK (char_length(btrim(delivery_address)) > 0),
    delivery_phone   TEXT           NOT NULL CHECK (char_length(btrim(delivery_phone)) > 0),
    note             TEXT,
    total            NUMERIC(14,2)  NOT NULL CHECK (total >= 0),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    confirmed_at     TIMESTAMPTZ,
    shipped_at       TIMESTAMPTZ,
    delivered_at     TIMESTAMPTZ,
    cancelled_at     TIMESTAMPTZ,
    CONSTRAINT orders_buyer_not_seller CHECK (buyer_id <> seller_id)
);
CREATE INDEX orders_buyer_idx  ON orders (buyer_id,  created_at DESC);
CREATE INDEX orders_seller_idx ON orders (seller_id, created_at DESC);

-- Lignes de commande : titre et prix sont COPIÉS au moment de l'achat (historique inchangé
-- même si le vendeur modifie ou supprime son produit).
CREATE TABLE order_items (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id      TEXT          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    TEXT          REFERENCES products (id) ON DELETE SET NULL,
    product_title TEXT          NOT NULL,
    thumbnail_url TEXT,
    unit_price    NUMERIC(14,2) NOT NULL CHECK (unit_price >= 0),
    quantity      INTEGER       NOT NULL CHECK (quantity > 0),
    line_total    NUMERIC(14,2) GENERATED ALWAYS AS (unit_price * quantity) STORED,
    UNIQUE (order_id, product_id)
);

-- Cycle de vie d'une commande (défense en profondeur : l'API applique aussi ces règles) :
--   EN_ATTENTE -> CONFIRMEE | ANNULEE      CONFIRMEE -> EXPEDIEE | ANNULEE      EXPEDIEE -> LIVREE
CREATE FUNCTION orders_before_status_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status = OLD.status THEN
        RETURN NEW;
    END IF;

    IF NOT (   (OLD.status = 'EN_ATTENTE' AND NEW.status IN ('CONFIRMEE', 'ANNULEE'))
            OR (OLD.status = 'CONFIRMEE'  AND NEW.status IN ('EXPEDIEE', 'ANNULEE'))
            OR (OLD.status = 'EXPEDIEE'   AND NEW.status = 'LIVREE')) THEN
        RAISE EXCEPTION 'Transition de statut invalide : % -> %', OLD.status, NEW.status
            USING ERRCODE = '23514';
    END IF;

    NEW.updated_at := now();
    IF    NEW.status = 'CONFIRMEE' THEN NEW.confirmed_at := now();
    ELSIF NEW.status = 'EXPEDIEE'  THEN NEW.shipped_at   := now();
    ELSIF NEW.status = 'LIVREE'    THEN NEW.delivered_at := now();
    ELSIF NEW.status = 'ANNULEE'   THEN NEW.cancelled_at := now();
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_orders_before_status BEFORE UPDATE OF status ON orders
    FOR EACH ROW EXECUTE FUNCTION orders_before_status_change();

-- Effets d'un changement de statut :
--   LIVREE  (confirmée par l'acheteur) -> +1 vente pour le vendeur (sert au calcul du niveau)
--   ANNULEE -> le stock réservé au passage en caisse est restitué
CREATE FUNCTION orders_after_status_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status = OLD.status THEN
        RETURN NEW;
    END IF;

    IF NEW.status = 'LIVREE' THEN
        UPDATE users SET total_sales = total_sales + 1 WHERE id = NEW.seller_id;
    ELSIF NEW.status = 'ANNULEE' THEN
        UPDATE products p
           SET stock = p.stock + oi.quantity
          FROM order_items oi
         WHERE oi.order_id = NEW.id AND oi.product_id = p.id;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER trg_orders_after_status AFTER UPDATE OF status ON orders
    FOR EACH ROW EXECUTE FUNCTION orders_after_status_change();
