-- Tests des règles métier portées par la base. Tout est annulé à la fin (ROLLBACK).
-- Lancer :  psql -h localhost -U marketplace -d marketplace -v ON_ERROR_STOP=1 -f db/tests/rules_test.sql
BEGIN;

DO $$
DECLARE
    v_order   TEXT;
    v_sales   INT;
    v_stock   INT;
    v_status  product_status;
    v_failed  BOOLEAN;
BEGIN
    INSERT INTO users (id, name, email) VALUES ('t_buyer', 'Acheteur Test', 'buyer.test@example.invalid');

    -- 1. Niveau du compte : mêmes seuils que User.resolveRole() côté Android
    ASSERT user_level(0, 0)    = 'BUYER',  'niveau BUYER';
    ASSERT user_level(3, 0)    = 'SELLER', 'niveau SELLER';
    ASSERT user_level(50, 4.4) = 'SELLER', '50 ventes mais score 4.4 -> SELLER';
    ASSERT user_level(50, 4.5) = 'PRO',    '50 ventes et score 4.5 -> PRO';

    -- 2. Statut produit synchronisé avec le stock
    INSERT INTO products (id, title, price, seller_id, stock, status)
         VALUES ('t_prod', 'Produit test', 10, 'seller_101', 0, 'DISPONIBLE');
    SELECT status INTO v_status FROM products WHERE id = 't_prod';
    ASSERT v_status = 'INDISPONIBLE', 'stock 0 -> INDISPONIBLE';
    UPDATE products SET stock = 4 WHERE id = 't_prod';
    SELECT status INTO v_status FROM products WHERE id = 't_prod';
    ASSERT v_status = 'DISPONIBLE', 'réapprovisionnement -> DISPONIBLE';

    -- 3. modele3d dérivé de model3d_url
    ASSERT (SELECT modele3d FROM products WHERE id = 't_prod') = FALSE, 'pas de modèle 3D';
    UPDATE products SET model3d_url = 'https://exemple.invalid/a.glb' WHERE id = 't_prod';
    ASSERT (SELECT modele3d FROM products WHERE id = 't_prod') = TRUE, 'modèle 3D détecté';

    -- 4. Commande : cycle de vie complet, +1 vente à la livraison
    UPDATE products SET stock = 2 WHERE id = 't_prod';       -- simule la réservation du stock (2 sur 4)
    INSERT INTO orders (buyer_id, seller_id, delivery_address, delivery_phone, total)
         VALUES ('t_buyer', 'seller_101', 'Lot 12, Antananarivo', '+261340000999', 20) RETURNING id INTO v_order;
    INSERT INTO order_items (order_id, product_id, product_title, unit_price, quantity)
         VALUES (v_order, 't_prod', 'Produit test', 10, 2);
    ASSERT (SELECT line_total FROM order_items WHERE order_id = v_order) = 20, 'line_total calculé';

    v_failed := FALSE;
    BEGIN
        UPDATE orders SET status = 'LIVREE' WHERE id = v_order;           -- saut interdit
    EXCEPTION WHEN check_violation THEN v_failed := TRUE;
    END;
    ASSERT v_failed, 'EN_ATTENTE -> LIVREE doit être refusé';

    UPDATE orders SET status = 'CONFIRMEE' WHERE id = v_order;
    UPDATE orders SET status = 'EXPEDIEE'  WHERE id = v_order;
    SELECT total_sales INTO v_sales FROM users WHERE id = 'seller_101';
    ASSERT v_sales = 0, 'pas de vente tant que non livrée';
    UPDATE orders SET status = 'LIVREE' WHERE id = v_order;
    SELECT total_sales INTO v_sales FROM users WHERE id = 'seller_101';
    ASSERT v_sales = 1, 'livraison -> +1 vente';
    ASSERT (SELECT level FROM v_users WHERE id = 'seller_101') = 'SELLER', 'vendeur devient SELLER';
    ASSERT (SELECT delivered_at FROM orders WHERE id = v_order) IS NOT NULL, 'delivered_at renseigné';

    v_failed := FALSE;
    BEGIN
        UPDATE orders SET status = 'ANNULEE' WHERE id = v_order;          -- livrée : plus annulable
    EXCEPTION WHEN check_violation THEN v_failed := TRUE;
    END;
    ASSERT v_failed, 'LIVREE -> ANNULEE doit être refusé';

    -- 5. Annulation : le stock est restitué
    INSERT INTO orders (buyer_id, seller_id, delivery_address, delivery_phone, total)
         VALUES ('t_buyer', 'seller_101', 'Lot 12', '+261340000999', 20) RETURNING id INTO v_order;
    INSERT INTO order_items (order_id, product_id, product_title, unit_price, quantity)
         VALUES (v_order, 't_prod', 'Produit test', 10, 2);
    UPDATE products SET stock = 0 WHERE id = 't_prod';                    -- tout réservé
    SELECT status INTO v_status FROM products WHERE id = 't_prod';
    ASSERT v_status = 'INDISPONIBLE', 'épuisé après réservation';
    UPDATE orders SET status = 'ANNULEE' WHERE id = v_order;
    SELECT stock, status INTO v_stock, v_status FROM products WHERE id = 't_prod';
    ASSERT v_stock = 2,           'annulation -> stock restitué';
    ASSERT v_status = 'DISPONIBLE', 'annulation -> produit de nouveau disponible';

    -- 6. Contraintes
    v_failed := FALSE;
    BEGIN
        INSERT INTO orders (buyer_id, seller_id, delivery_address, delivery_phone, total)
             VALUES ('seller_101', 'seller_101', 'x', 'y', 1);
    EXCEPTION WHEN check_violation THEN v_failed := TRUE;
    END;
    ASSERT v_failed, 'un vendeur ne peut pas commander ses propres produits';

    v_failed := FALSE;
    BEGIN
        UPDATE products SET stock = -1 WHERE id = 't_prod';
    EXCEPTION WHEN check_violation THEN v_failed := TRUE;
    END;
    ASSERT v_failed, 'stock négatif interdit';

    RAISE NOTICE 'TOUS LES TESTS SQL PASSENT';
END $$;

ROLLBACK;
