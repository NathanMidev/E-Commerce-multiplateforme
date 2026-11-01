import { Router } from 'express';
import { categoryJson, productJson } from '../mappers.js';
import { badRequest, conflict, forbidden, notFound } from '../errors.js';
import { parse, productCreateSchema, productPatchSchema, productQuerySchema, urlField } from '../validation.js';
import { z } from 'zod';

const PRODUCT_SELECT = `
  SELECT p.*, u.name AS seller_name
    FROM products p JOIN users u ON u.id = p.seller_id`;

const escapeLike = (s) => s.replace(/[\\%_]/g, '\\$&');

export function catalogRouter({ pool, auth }) {
  const router = Router();

  // ---------------------------------------------------------------- catégories
  router.get('/categories', async (req, res) => {
    const { rows } = await pool.query('SELECT * FROM categories ORDER BY name');
    res.json(rows.map(categoryJson));
  });

  // ------------------------------------------------------------------ produits
  // Filtres = ceux de FilterCriteria côté Android : categoryId, minPrice, maxPrice, arOnly, searchQuery, sortBy
  router.get('/products', async (req, res) => {
    const q = parse(productQuerySchema, req.query);
    const where = [];
    const params = [];
    const add = (sql, value) => { params.push(value); where.push(sql.replace('?', `$${params.length}`)); };

    if (q.categoryId) add('p.category_id = ?', q.categoryId);
    if (q.sellerId) add('p.seller_id = ?', q.sellerId);
    if (q.minPrice !== undefined) add('p.price >= ?', q.minPrice);
    if (q.maxPrice !== undefined) add('p.price <= ?', q.maxPrice);
    if (q.arOnly === 'true') where.push('p.modele3d = TRUE');
    if (q.searchQuery) {
      params.push(`%${escapeLike(q.searchQuery)}%`);
      where.push(`(p.title ILIKE $${params.length} OR p.description ILIKE $${params.length})`);
    }

    const order = {
      recent: 'p.created_at DESC',
      price_asc: 'p.price ASC, p.created_at DESC',
      price_desc: 'p.price DESC, p.created_at DESC',
    }[q.sortBy];

    params.push(q.limit, q.offset);
    const sql = `${PRODUCT_SELECT}
      ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
      ORDER BY ${order}
      LIMIT $${params.length - 1} OFFSET $${params.length}`;
    const { rows } = await pool.query(sql, params);
    res.json(rows.map(productJson));
  });

  router.get('/products/:id', async (req, res) => {
    const { rows } = await pool.query(`${PRODUCT_SELECT} WHERE p.id = $1`, [req.params.id]);
    if (!rows.length) throw notFound('Produit introuvable');
    res.json(productJson(rows[0]));
  });

  // Publier une annonce : il faut un moyen de contact (WhatsApp ou téléphone) sur son profil.
  router.post('/products', auth.requireAuth, async (req, res) => {
    const body = parse(productCreateSchema, req.body);
    const me = await pool.query('SELECT whatsapp, phone_number FROM users WHERE id = $1', [req.auth.uid]);
    if (!me.rows[0].whatsapp && !me.rows[0].phone_number) {
      throw forbidden('Complétez votre profil (WhatsApp ou téléphone) avant de publier un produit', 'PROFILE_INCOMPLETE');
    }

    const imageUrls = body.imageUrls ?? [];
    const thumbnail = body.thumbnailUrl ?? imageUrls[0] ?? null;
    const { rows } = await pool.query(
      `INSERT INTO products (title, description, price, status, category_id, seller_id,
                             thumbnail_url, model3d_url, image_urls, stock)
       VALUES ($1, $2, $3, COALESCE($4::product_status, 'NOUVEAU'), $5, $6, $7, $8, $9, $10)
       RETURNING id`,
      [body.title, body.description ?? '', body.price, body.status ?? null, body.categoryId ?? null,
        req.auth.uid, thumbnail, body.model3dUrl ?? null, imageUrls, body.stock ?? 1],
    );
    const created = await pool.query(`${PRODUCT_SELECT} WHERE p.id = $1`, [rows[0].id]);
    res.status(201).json(productJson(created.rows[0]));
  });

  async function loadOwnedProduct(id, uid) {
    const { rows } = await pool.query('SELECT id, seller_id FROM products WHERE id = $1', [id]);
    if (!rows.length) throw notFound('Produit introuvable');
    if (rows[0].seller_id !== uid) throw forbidden("Ce produit appartient à un autre vendeur");
  }

  router.patch('/products/:id', auth.requireAuth, async (req, res) => {
    const body = parse(productPatchSchema, req.body);
    await loadOwnedProduct(req.params.id, req.auth.uid);

    const columns = {
      title: 'title', description: 'description', price: 'price', status: 'status',
      categoryId: 'category_id', thumbnailUrl: 'thumbnail_url', imageUrls: 'image_urls',
      model3dUrl: 'model3d_url', stock: 'stock',
    };
    const sets = [];
    const params = [];
    for (const [key, column] of Object.entries(columns)) {
      if (body[key] !== undefined) {
        params.push(body[key]);
        sets.push(`${column} = $${params.length}`);
      }
    }
    if (!sets.length) throw badRequest('Aucun champ à modifier');
    params.push(req.params.id);
    await pool.query(`UPDATE products SET ${sets.join(', ')} WHERE id = $${params.length}`, params);

    const { rows } = await pool.query(`${PRODUCT_SELECT} WHERE p.id = $1`, [req.params.id]);
    res.json(productJson(rows[0]));
  });

  // Appelé par le pipeline de scan 3D (ou le vendeur) une fois le modèle .glb disponible.
  router.patch('/products/:id/model3d', auth.requireAuth, async (req, res) => {
    const { model3dUrl } = parse(z.object({ model3dUrl: urlField }).strict(), req.body);
    await loadOwnedProduct(req.params.id, req.auth.uid);
    await pool.query('UPDATE products SET model3d_url = $1 WHERE id = $2', [model3dUrl, req.params.id]);
    res.status(204).end();
  });

  router.delete('/products/:id', auth.requireAuth, async (req, res) => {
    await loadOwnedProduct(req.params.id, req.auth.uid);
    // Les commandes passées gardent titre et prix (copiés dans order_items) ; les paniers sont nettoyés.
    await pool.query('DELETE FROM products WHERE id = $1', [req.params.id]);
    res.status(204).end();
  });

  return router;
}
