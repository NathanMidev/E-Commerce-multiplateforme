import { Router } from 'express';
import { orderJson } from '../mappers.js';
import { badRequest, conflict, forbidden, notFound } from '../errors.js';
import { checkoutSchema, orderListQuerySchema, orderStatusSchema, parse } from '../validation.js';
import { withTransaction } from '../db.js';

const ORDER_SELECT = `
  SELECT o.*, b.name AS buyer_name, s.name AS seller_name
    FROM orders o
    JOIN users b ON b.id = o.buyer_id
    JOIN users s ON s.id = o.seller_id`;

// Qui peut faire quoi (la base refuse de toute façon les transitions invalides).
//   vendeur : confirme, expédie, peut annuler tant que la commande n'est pas expédiée
//   acheteur : confirme la RÉCEPTION (-> +1 vente pour le vendeur), peut annuler tant qu'elle est en attente
const SELLER_TRANSITIONS = { EN_ATTENTE: ['CONFIRMEE', 'ANNULEE'], CONFIRMEE: ['EXPEDIEE', 'ANNULEE'] };
const BUYER_TRANSITIONS = { EN_ATTENTE: ['ANNULEE'], EXPEDIEE: ['LIVREE'] };

async function fetchOrders(db, whereSql, params) {
  const orders = (await db.query(`${ORDER_SELECT} WHERE ${whereSql} ORDER BY o.created_at DESC, o.order_number DESC`, params)).rows;
  if (!orders.length) return [];
  const items = (await db.query(
    'SELECT * FROM order_items WHERE order_id = ANY($1) ORDER BY id', [orders.map((o) => o.id)])).rows;
  return orders.map((o) => orderJson(o, items.filter((i) => i.order_id === o.id)));
}

export function ordersRouter({ pool, auth }) {
  const router = Router();
  router.use('/orders', auth.requireAuth);

  // Passer commande : une commande est créée PAR VENDEUR, le stock est réservé, le panier est vidé.
  // Tout se fait dans une transaction : en cas de problème rien n'est modifié.
  router.post('/orders/checkout', async (req, res) => {
    const body = parse(checkoutSchema, req.body);
    const uid = req.auth.uid;

    const orderIds = await withTransaction(pool, async (db) => {
      const me = (await db.query('SELECT address, phone_number, whatsapp FROM users WHERE id = $1', [uid])).rows[0];
      const deliveryAddress = body.deliveryAddress ?? me.address;
      const deliveryPhone = body.deliveryPhone ?? me.phone_number ?? me.whatsapp;
      if (!deliveryAddress) throw badRequest("Adresse de livraison requise (ou renseignez l'adresse de votre profil)");
      if (!deliveryPhone) throw badRequest('Téléphone de livraison requis (ou renseignez votre profil)');

      // Verrouille les produits concernés (ordre par id : évite les blocages mutuels entre acheteurs).
      const lines = (await db.query(
        `SELECT ci.product_id, ci.quantity, p.title, p.thumbnail_url, p.price, p.stock, p.status, p.seller_id
           FROM carts c
           JOIN cart_items ci ON ci.cart_id = c.id
           JOIN products p    ON p.id = ci.product_id
          WHERE c.user_id = $1
          ORDER BY p.id
            FOR UPDATE OF p`,
        [uid])).rows;
      if (!lines.length) throw badRequest('Votre panier est vide');

      const problems = [];
      for (const l of lines) {
        if (l.seller_id === uid) problems.push({ productId: l.product_id, reason: 'OWN_PRODUCT', title: l.title });
        else if (l.status === 'INDISPONIBLE' || l.stock < l.quantity) {
          problems.push({ productId: l.product_id, reason: 'INSUFFICIENT_STOCK', title: l.title, requested: l.quantity, available: Math.max(l.stock, 0) });
        }
      }
      if (problems.length) {
        throw conflict('Certains articles ne sont plus disponibles dans la quantité demandée', 'CHECKOUT_CONFLICT', { items: problems });
      }

      const bySeller = new Map();
      for (const l of lines) {
        if (!bySeller.has(l.seller_id)) bySeller.set(l.seller_id, []);
        bySeller.get(l.seller_id).push(l);
      }

      const ids = [];
      for (const [sellerId, items] of bySeller) {
        const order = (await db.query(
          `INSERT INTO orders (buyer_id, seller_id, payment_method, delivery_address, delivery_phone, note, total)
           VALUES ($1, $2, $3, $4, $5, $6, 0) RETURNING id`,
          [uid, sellerId, body.paymentMethod, deliveryAddress, deliveryPhone, body.note ?? null])).rows[0];
        for (const l of items) {
          // titre et prix sont copiés : l'historique ne change plus si le vendeur modifie son annonce
          await db.query(
            `INSERT INTO order_items (order_id, product_id, product_title, thumbnail_url, unit_price, quantity)
             VALUES ($1, $2, $3, $4, $5, $6)`,
            [order.id, l.product_id, l.title, l.thumbnail_url, l.price, l.quantity]);
          await db.query('UPDATE products SET stock = stock - $2 WHERE id = $1', [l.product_id, l.quantity]);
        }
        await db.query(
          'UPDATE orders SET total = (SELECT COALESCE(SUM(line_total), 0) FROM order_items WHERE order_id = $1) WHERE id = $1',
          [order.id]);
        ids.push(order.id);
      }

      await db.query('DELETE FROM cart_items WHERE cart_id = (SELECT id FROM carts WHERE user_id = $1)', [uid]);
      return ids;
    });

    res.status(201).json({ orders: await fetchOrders(pool, 'o.id = ANY($1)', [orderIds]) });
  });

  // Mes commandes : role=buyer (mes achats, défaut) ou role=seller (mes ventes).
  router.get('/orders', async (req, res) => {
    const q = parse(orderListQuerySchema, req.query);
    const params = [req.auth.uid];
    let where = q.role === 'seller' ? 'o.seller_id = $1' : 'o.buyer_id = $1';
    if (q.status) { params.push(q.status); where += ` AND o.status = $${params.length}`; }
    res.json(await fetchOrders(pool, where, params));
  });

  router.get('/orders/:id', async (req, res) => {
    const orders = await fetchOrders(pool, 'o.id = $1 AND (o.buyer_id = $2 OR o.seller_id = $2)', [req.params.id, req.auth.uid]);
    if (!orders.length) throw notFound('Commande introuvable');
    res.json(orders[0]);
  });

  router.patch('/orders/:id/status', async (req, res) => {
    const { status: target } = parse(orderStatusSchema, req.body);
    const uid = req.auth.uid;

    await withTransaction(pool, async (db) => {
      const order = (await db.query(
        'SELECT status, buyer_id, seller_id FROM orders WHERE id = $1 FOR UPDATE', [req.params.id])).rows[0];
      if (!order || (order.buyer_id !== uid && order.seller_id !== uid)) throw notFound('Commande introuvable');

      const isSeller = order.seller_id === uid;
      const mine = (isSeller ? SELLER_TRANSITIONS : BUYER_TRANSITIONS)[order.status] ?? [];
      if (!mine.includes(target)) {
        const others = (isSeller ? BUYER_TRANSITIONS : SELLER_TRANSITIONS)[order.status] ?? [];
        if (others.includes(target)) {
          throw forbidden(target === 'LIVREE'
            ? "Seul l'acheteur peut confirmer la réception de la commande"
            : isSeller ? "Action réservée à l'acheteur" : 'Action réservée au vendeur', 'FORBIDDEN_TRANSITION');
        }
        throw conflict(`Transition impossible : ${order.status} -> ${target}`, 'INVALID_TRANSITION');
      }
      await db.query('UPDATE orders SET status = $1 WHERE id = $2', [target, req.params.id]);
    });

    const orders = await fetchOrders(pool, 'o.id = $1', [req.params.id]);
    res.json(orders[0]);
  });

  return router;
}
