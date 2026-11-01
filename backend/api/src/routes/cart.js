import { Router } from 'express';
import { cartJson } from '../mappers.js';
import { badRequest, conflict, notFound } from '../errors.js';
import { parse, cartAddSchema, cartSetSchema } from '../validation.js';
import { withTransaction } from '../db.js';

export const CART_QUERY = `
  SELECT ci.product_id, ci.quantity, p.title, p.thumbnail_url, p.price, p.stock, p.status,
         p.seller_id, u.name AS seller_name
    FROM carts c
    JOIN cart_items ci ON ci.cart_id = c.id
    JOIN products p    ON p.id = ci.product_id
    JOIN users u       ON u.id = p.seller_id
   WHERE c.user_id = $1
   ORDER BY ci.added_at, p.id`;

export function cartRouter({ pool, auth }) {
  const router = Router();
  router.use('/cart', auth.requireAuth);

  const readCart = async (db, uid) => cartJson((await db.query(CART_QUERY, [uid])).rows);

  const ensureCart = async (db, uid) => {
    const { rows } = await db.query(
      `INSERT INTO carts (user_id) VALUES ($1)
       ON CONFLICT (user_id) DO UPDATE SET updated_at = now() RETURNING id`,
      [uid],
    );
    return rows[0].id;
  };

  router.get('/cart', async (req, res) => {
    res.json(await readCart(pool, req.auth.uid));
  });

  // Ajouter au panier (cumule la quantité si le produit y est déjà).
  router.post('/cart/items', async (req, res) => {
    const { productId, quantity } = parse(cartAddSchema, req.body);
    const uid = req.auth.uid;

    await withTransaction(pool, async (db) => {
      const product = (await db.query(
        'SELECT id, seller_id, stock, status FROM products WHERE id = $1', [productId])).rows[0];
      if (!product) throw notFound('Produit introuvable');
      if (product.seller_id === uid) throw badRequest('Vous ne pouvez pas acheter votre propre produit', undefined);
      if (product.status === 'INDISPONIBLE' || product.stock <= 0) {
        throw conflict('Produit indisponible', 'OUT_OF_STOCK');
      }

      const cartId = await ensureCart(db, uid);
      const existing = (await db.query(
        'SELECT quantity FROM cart_items WHERE cart_id = $1 AND product_id = $2', [cartId, productId])).rows[0];
      const total = (existing?.quantity ?? 0) + quantity;
      if (total > product.stock) {
        throw conflict(`Stock insuffisant : ${product.stock} disponible(s)`, 'INSUFFICIENT_STOCK', { available: product.stock });
      }
      await db.query(
        `INSERT INTO cart_items (cart_id, product_id, quantity) VALUES ($1, $2, $3)
         ON CONFLICT (cart_id, product_id) DO UPDATE SET quantity = $3`,
        [cartId, productId, total],
      );
    });
    res.status(201).json(await readCart(pool, uid));
  });

  // Fixer la quantité (0 = retirer l'article).
  router.put('/cart/items/:productId', async (req, res) => {
    const { quantity } = parse(cartSetSchema, req.body);
    const uid = req.auth.uid;

    await withTransaction(pool, async (db) => {
      const cartId = await ensureCart(db, uid);
      if (quantity === 0) {
        await db.query('DELETE FROM cart_items WHERE cart_id = $1 AND product_id = $2', [cartId, req.params.productId]);
        return;
      }
      const product = (await db.query(
        'SELECT stock, status, seller_id FROM products WHERE id = $1', [req.params.productId])).rows[0];
      if (!product) throw notFound('Produit introuvable');
      if (product.seller_id === uid) throw badRequest('Vous ne pouvez pas acheter votre propre produit');
      if (product.status === 'INDISPONIBLE' || quantity > product.stock) {
        throw conflict(`Stock insuffisant : ${Math.max(product.stock, 0)} disponible(s)`, 'INSUFFICIENT_STOCK', { available: product.stock });
      }
      await db.query(
        `INSERT INTO cart_items (cart_id, product_id, quantity) VALUES ($1, $2, $3)
         ON CONFLICT (cart_id, product_id) DO UPDATE SET quantity = $3`,
        [cartId, req.params.productId, quantity],
      );
    });
    res.json(await readCart(pool, uid));
  });

  router.delete('/cart/items/:productId', async (req, res) => {
    await pool.query(
      `DELETE FROM cart_items WHERE product_id = $2
         AND cart_id = (SELECT id FROM carts WHERE user_id = $1)`,
      [req.auth.uid, req.params.productId],
    );
    res.json(await readCart(pool, req.auth.uid));
  });

  router.delete('/cart', async (req, res) => {
    await pool.query(
      'DELETE FROM cart_items WHERE cart_id = (SELECT id FROM carts WHERE user_id = $1)', [req.auth.uid]);
    res.json(await readCart(pool, req.auth.uid));
  });

  return router;
}
