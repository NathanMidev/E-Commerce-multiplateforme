import { after, before, describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { startTestServer } from './helpers.js';
import { loadConfig } from '../src/config.js';
import { normalizePhone, isFacebookUrl } from '../src/validation.js';

let t;
before(async () => { t = await startTestServer(); });
after(async () => { await t.stop(); });

const get = (route, opts) => t.call('GET', route, opts);
const post = (route, opts) => t.call('POST', route, opts);
const patch = (route, opts) => t.call('PATCH', route, opts);
const put = (route, opts) => t.call('PUT', route, opts);
const del = (route, opts) => t.call('DELETE', route, opts);
const stockOf = async (id) => (await get(`/products/${id}`)).body.stock;

describe('configuration et utilitaires', () => {
  it('refuse AUTH_MODE=dev en production et exige FIREBASE_PROJECT_ID', () => {
    assert.throws(() => loadConfig({ AUTH_MODE: 'dev', NODE_ENV: 'production' }), /interdit/);
    assert.throws(() => loadConfig({}), /FIREBASE_PROJECT_ID/);
    assert.equal(loadConfig({ AUTH_MODE: 'dev' }).authMode, 'dev');
  });
  it('normalise les numéros malgaches et refuse le reste', () => {
    assert.equal(normalizePhone('034 12 345 67'), '+261341234567');
    assert.equal(normalizePhone('+261 34 12 345 67'), '+261341234567');
    assert.equal(normalizePhone('00261331234567'), '+261331234567');
    assert.equal(normalizePhone('+33612345678'), '+33612345678');
    assert.equal(normalizePhone('12'), null);
    assert.equal(normalizePhone('abc'), null);
  });
  it('valide les liens Facebook', () => {
    assert.ok(isFacebookUrl('https://www.facebook.com/ma.boutique'));
    assert.ok(isFacebookUrl('https://fb.me/x'));
    assert.ok(!isFacebookUrl('https://facebook.com.evil.com/x'));
    assert.ok(!isFacebookUrl('javascript:alert(1)'));
  });
});

describe('catalogue public', () => {
  it('health, catégories, produits de démonstration', async () => {
    assert.equal((await get('/health')).body.status, 'ok');
    assert.equal((await get('/categories')).body.length, 4);
    const products = (await get('/products')).body;
    assert.equal(products.length, 3);
    const p1 = products.find((p) => p.id === 'prod_1');
    assert.equal(p1.price, 189.99);
    assert.equal(p1.sellerName, 'Atelier Nordique');
    assert.equal(p1.modele3d, true);
    assert.equal(typeof p1.createdAt, 'number');
  });
  it('filtres de FilterCriteria : catégorie, prix, recherche, AR, tri', async () => {
    assert.deepEqual((await get('/products?categoryId=cat_fashion')).body.map((p) => p.id), ['prod_3']);
    assert.deepEqual((await get('/products?minPrice=100&maxPrice=200')).body.map((p) => p.id), ['prod_1']);
    assert.deepEqual((await get('/products?searchQuery=lampe')).body.map((p) => p.id), ['prod_2']);
    assert.equal((await get('/products?arOnly=true')).body.length, 3);
    assert.deepEqual((await get('/products?sortBy=price_asc')).body.map((p) => p.id), ['prod_2', 'prod_1', 'prod_3']);
    assert.deepEqual((await get('/products?sortBy=price_desc')).body.map((p) => p.id), ['prod_3', 'prod_1', 'prod_2']);
    assert.equal((await get('/products?sortBy=hack')).status, 400);
    assert.equal((await get('/products?searchQuery=%25')).body.length, 0, 'le caractère % est échappé');
  });
  it('produit introuvable -> 404 au format d\'erreur commun', async () => {
    const r = await get('/products/nope');
    assert.equal(r.status, 404);
    assert.equal(r.body.error.code, 'NOT_FOUND');
  });
  it('les routes protégées exigent une authentification', async () => {
    for (const [m, route] of [['GET', '/cart'], ['GET', '/orders'], ['GET', '/users/me'], ['POST', '/products']]) {
      const r = await t.call(m, route, { body: m === 'POST' ? {} : undefined });
      assert.equal(r.status, 401, `${m} ${route}`);
      assert.equal(r.body.error.code, 'AUTH_REQUIRED');
    }
  });
});

describe('profil utilisateur', () => {
  it('crée le profil au premier appel avec les infos du compte', async () => {
    const r = await get('/users/me', { as: 'u_alice', name: 'Alice Rakoto', email: 'alice@example.invalid' });
    assert.equal(r.status, 200);
    assert.equal(r.body.name, 'Alice Rakoto');
    assert.equal(r.body.email, 'alice@example.invalid');
    assert.equal(r.body.level, 'BUYER');
    assert.equal(r.body.totalSales, 0);
  });
  it('valide et normalise téléphone, WhatsApp et lien Facebook', async () => {
    assert.equal((await patch('/users/me', { as: 'u_alice', body: { phoneNumber: '12' } })).status, 400);
    assert.equal((await patch('/users/me', { as: 'u_alice', body: { facebookLink: 'https://evil.com/alice' } })).status, 400);
    const r = await patch('/users/me', { as: 'u_alice', body: {
      phoneNumber: '034 12 345 67', whatsapp: '0331234567', facebookLink: 'https://www.facebook.com/alice.rakoto',
      address: 'Lot 12 Ankorondrano', city: 'Antananarivo' } });
    assert.equal(r.status, 200);
    assert.equal(r.body.phoneNumber, '+261341234567');
    assert.equal(r.body.whatsapp, '+261331234567');
  });
  it('un client ne peut pas modifier ses statistiques ni son niveau', async () => {
    for (const body of [{ totalSales: 999 }, { level: 'PRO' }, { responseScore: 5 }, { id: 'x' }]) {
      assert.equal((await patch('/users/me', { as: 'u_alice', body })).status, 400, JSON.stringify(body));
    }
    assert.equal((await get('/users/me', { as: 'u_alice' })).body.totalSales, 0);
  });
  it('profil public : contacts visibles seulement pour un compte connecté', async () => {
    const anonymous = (await get('/users/u_alice')).body;
    assert.equal(anonymous.name, 'Alice Rakoto');
    assert.equal(anonymous.city, 'Antananarivo');
    for (const hidden of ['whatsapp', 'phoneNumber', 'facebookLink', 'email', 'address']) {
      assert.ok(!(hidden in anonymous), `${hidden} ne doit pas être public`);
    }
    await get('/users/me', { as: 'u_bob', name: 'Bob Atelier', email: 'bob@example.invalid' });
    const member = (await get('/users/u_alice', { as: 'u_bob' })).body;
    assert.equal(member.whatsapp, '+261331234567');
    assert.ok(!('address' in member) && !('email' in member), 'adresse et email restent privés');
    const self = (await get('/users/u_alice', { as: 'u_alice' })).body;
    assert.equal(self.address, 'Lot 12 Ankorondrano');
  });
});

describe('publication de produits', () => {
  it('exige un moyen de contact sur le profil', async () => {
    const r = await post('/products', { as: 'u_bob', body: { title: 'Table basse', price: 120 } });
    assert.equal(r.status, 403);
    assert.equal(r.body.error.code, 'PROFILE_INCOMPLETE');
    await patch('/users/me', { as: 'u_bob', body: { whatsapp: '0341111111', address: 'Lot 5 Isotry', phoneNumber: '0341111111' } });
  });
  it('crée, modifie et protège un produit', async () => {
    const created = await post('/products', { as: 'u_bob', body: {
      title: 'Table basse', description: 'Bois massif', price: 120, categoryId: 'cat_furniture', stock: 3,
      model3dUrl: 'https://exemple.invalid/table.glb', imageUrls: ['https://exemple.invalid/t1.jpg'] } });
    assert.equal(created.status, 201);
    assert.equal(created.body.sellerName, 'Bob Atelier');
    assert.equal(created.body.modele3d, true);
    assert.equal(created.body.thumbnailUrl, 'https://exemple.invalid/t1.jpg');
    t.tableId = created.body.id;

    assert.equal((await patch(`/products/${t.tableId}`, { as: 'u_alice', body: { price: 1 } })).status, 403);
    const edited = (await patch(`/products/${t.tableId}`, { as: 'u_bob', body: { price: 125.5 } })).body;
    assert.equal(edited.price, 125.5);
    assert.equal(edited.stock, 3, 'une modification partielle ne touche pas aux autres champs');
    assert.equal(edited.description, 'Bois massif');
    assert.equal(edited.imageUrls.length, 1);
    assert.equal((await post('/products', { as: 'u_bob', body: { title: 'Sans options', price: 1 } })).body.stock, 1, 'défauts de création : stock 1');
    await del(`/products/${(await get('/products?searchQuery=Sans options')).body[0].id}`, { as: 'u_bob' });
    assert.equal((await patch(`/products/${t.tableId}/model3d`, { as: 'u_bob', body: { model3dUrl: 'https://exemple.invalid/v2.glb' } })).status, 204);
    assert.equal((await get(`/products/${t.tableId}`)).body.model3dUrl, 'https://exemple.invalid/v2.glb');
  });
  it('refuse les données invalides', async () => {
    const bad = (body) => post('/products', { as: 'u_bob', body });
    assert.equal((await bad({ title: '', price: 1 })).status, 400);
    assert.equal((await bad({ title: 'x', price: -5 })).status, 400);
    assert.equal((await bad({ title: 'x', price: 1, model3dUrl: 'javascript:alert(1)' })).status, 400);
    assert.equal((await bad({ title: 'x', price: 1, categoryId: 'cat_inconnue' })).status, 400);
    assert.equal((await bad({ title: 'x', price: 1, sellerId: 'u_alice' })).status, 400, 'le vendeur vient du jeton, pas du corps');
  });
});

describe('panier', () => {
  it('ajoute, cumule, plafonne au stock, modifie et retire', async () => {
    let cart = (await post('/cart/items', { as: 'u_alice', body: { productId: 'prod_1', quantity: 2 } })).body;
    assert.equal(cart.itemCount, 2);
    assert.equal(cart.total, 379.98);
    cart = (await post('/cart/items', { as: 'u_alice', body: { productId: 'prod_1' } })).body;
    assert.equal(cart.items[0].quantity, 3, 'les ajouts se cumulent');

    const over = await post('/cart/items', { as: 'u_alice', body: { productId: 'prod_1', quantity: 3 } });
    assert.equal(over.status, 409);
    assert.equal(over.body.error.code, 'INSUFFICIENT_STOCK');

    cart = (await put('/cart/items/prod_1', { as: 'u_alice', body: { quantity: 2 } })).body;
    assert.equal(cart.items[0].quantity, 2);
    assert.equal((await put('/cart/items/prod_1', { as: 'u_alice', body: { quantity: 6 } })).status, 409);
    cart = (await put('/cart/items/prod_1', { as: 'u_alice', body: { quantity: 0 } })).body;
    assert.equal(cart.items.length, 0, 'quantité 0 = retrait');
  });
  it('refuse son propre produit et un produit inconnu', async () => {
    assert.equal((await post('/cart/items', { as: 'u_bob', body: { productId: t.tableId } })).status, 400);
    assert.equal((await post('/cart/items', { as: 'u_alice', body: { productId: 'nope' } })).status, 404);
    assert.equal((await post('/cart/items', { as: 'u_alice', body: { productId: 'prod_1', quantity: 0 } })).status, 400);
  });
  it('chaque utilisateur a son propre panier', async () => {
    await post('/cart/items', { as: 'u_alice', body: { productId: 'prod_2' } });
    assert.equal((await get('/cart', { as: 'u_bob' })).body.items.length, 0);
    assert.equal((await del('/cart', { as: 'u_alice' })).body.items.length, 0);
  });
});

describe('commande : passage en caisse', () => {
  it('refuse un panier vide ou sans adresse de livraison', async () => {
    assert.equal((await post('/orders/checkout', { as: 'u_alice', body: {} })).body.error.message, 'Votre panier est vide');
    await post('/cart/items', { as: 'u_carol', body: { productId: 'prod_2' } });
    const r = await post('/orders/checkout', { as: 'u_carol', body: {} });
    assert.equal(r.status, 400);
    assert.match(r.body.error.message, /Adresse de livraison/);
    assert.equal(await stockOf('prod_2'), 12, 'rien n\'est modifié en cas de refus');
    await del('/cart', { as: 'u_carol' });
  });

  it('crée UNE commande par vendeur, réserve le stock et vide le panier', async () => {
    await post('/cart/items', { as: 'u_alice', body: { productId: 'prod_1', quantity: 2 } });
    await post('/cart/items', { as: 'u_alice', body: { productId: 'prod_3', quantity: 1 } });
    await post('/cart/items', { as: 'u_alice', body: { productId: t.tableId, quantity: 1 } });

    const r = await post('/orders/checkout', { as: 'u_alice', body: { paymentMethod: 'MVOLA', note: 'Appeler avant' } });
    assert.equal(r.status, 201);
    const orders = r.body.orders;
    assert.equal(orders.length, 3, 'un vendeur = une commande');
    const bySeller = Object.fromEntries(orders.map((o) => [o.sellerId, o]));
    assert.equal(bySeller.seller_101.total, 379.98);
    assert.equal(bySeller.seller_103.total, 220);
    assert.equal(bySeller.u_bob.total, 125.5);
    for (const o of orders) {
      assert.equal(o.status, 'EN_ATTENTE');
      assert.equal(o.paymentMethod, 'MVOLA');
      assert.equal(o.deliveryAddress, 'Lot 12 Ankorondrano', 'adresse reprise du profil');
      assert.equal(o.deliveryPhone, '+261341234567');
      assert.match(o.reference, /^CMD-\d{6}$/);
      assert.equal(o.items.length, 1);
    }
    t.orders = bySeller;

    assert.equal(await stockOf('prod_1'), 3);
    assert.equal(await stockOf('prod_3'), 1);
    assert.equal(await stockOf(t.tableId), 2);
    assert.equal((await get('/cart', { as: 'u_alice' })).body.items.length, 0, 'panier vidé');
  });

  it('fige titre et prix dans la commande', async () => {
    await patch('/products/' + t.tableId, { as: 'u_bob', body: { title: 'Table renommée', price: 999 } });
    const order = (await get(`/orders/${t.orders.u_bob.id}`, { as: 'u_alice' })).body;
    assert.equal(order.items[0].productTitle, 'Table basse');
    assert.equal(order.items[0].unitPrice, 125.5);
  });

  it('liste mes achats / mes ventes ; un étranger ne voit rien', async () => {
    assert.equal((await get('/orders', { as: 'u_alice' })).body.length, 3);
    const sales = (await get('/orders?role=seller', { as: 'u_bob' })).body;
    assert.equal(sales.length, 1);
    assert.equal(sales[0].buyerName, 'Alice Rakoto');
    assert.equal(sales[0].deliveryAddress, 'Lot 12 Ankorondrano', 'le vendeur voit où livrer');
    assert.equal((await get(`/orders/${t.orders.u_bob.id}`, { as: 'u_carol' })).status, 404);
    assert.equal((await get('/orders?role=seller', { as: 'u_carol' })).body.length, 0);
  });
});

describe('commande : cycle de vie', () => {
  it('le vendeur confirme et expédie ; seul l\'acheteur confirme la réception', async () => {
    const id = t.orders.u_bob.id;
    const status = (as, s) => patch(`/orders/${id}/status`, { as, body: { status: s } });

    assert.equal((await status('u_alice', 'CONFIRMEE')).body.error.code, 'FORBIDDEN_TRANSITION', 'l\'acheteur ne peut pas confirmer');
    assert.equal((await status('u_bob', 'EXPEDIEE')).body.error.code, 'INVALID_TRANSITION', 'on ne saute pas d\'étape');
    assert.equal((await status('u_carol', 'CONFIRMEE')).status, 404, 'un étranger ne peut rien');
    assert.equal((await status('u_bob', 'CONFIRMEE')).body.status, 'CONFIRMEE');
    assert.equal((await status('u_alice', 'EXPEDIEE')).body.error.code, 'FORBIDDEN_TRANSITION', 'l\'acheteur ne peut pas expédier');
    assert.equal((await status('u_alice', 'LIVREE')).body.error.code, 'INVALID_TRANSITION', 'pas de livraison avant expédition');
    const shipped = (await status('u_bob', 'EXPEDIEE')).body;
    assert.equal(shipped.status, 'EXPEDIEE');
    assert.ok(shipped.confirmedAt && shipped.shippedAt);

    const self = await status('u_bob', 'LIVREE');
    assert.equal(self.status, 403, 'le vendeur ne peut pas se déclarer payé/livré');
    assert.match(self.body.error.message, /acheteur/);
    assert.equal((await get('/users/u_bob')).body.totalSales, 0, 'pas de vente avant confirmation de l\'acheteur');

    const delivered = (await status('u_alice', 'LIVREE')).body;
    assert.equal(delivered.status, 'LIVREE');
    assert.ok(delivered.deliveredAt);
    const bob = (await get('/users/u_bob')).body;
    assert.equal(bob.totalSales, 1, 'la livraison confirmée compte comme une vente');
    assert.equal(bob.level, 'SELLER', 'le niveau est calculé à partir des ventes');

    assert.equal((await status('u_alice', 'ANNULEE')).status, 409, 'plus annulable une fois livrée');
  });

  it('l\'annulation restitue le stock', async () => {
    const id = t.orders.seller_101.id;
    assert.equal(await stockOf('prod_1'), 3);
    assert.equal((await patch(`/orders/${id}/status`, { as: 'u_alice', body: { status: 'ANNULEE' } })).body.status, 'ANNULEE');
    assert.equal(await stockOf('prod_1'), 5);
    const cancelled = (await get(`/orders/${id}`, { as: 'u_alice' })).body;
    assert.ok(cancelled.cancelledAt);
    assert.equal((await get('/users/seller_101')).body.totalSales, 0, 'une annulation n\'est pas une vente');
  });

  it('un acheteur ne peut annuler que tant que la commande est en attente', async () => {
    const id = t.orders.seller_103.id;
    await patch(`/orders/${id}/status`, { as: 'seller_103', body: { status: 'CONFIRMEE' } });
    const r = await patch(`/orders/${id}/status`, { as: 'u_alice', body: { status: 'ANNULEE' } });
    assert.equal(r.status, 403);
    assert.equal(await stockOf('prod_3'), 1);
  });
});

describe('stock : concurrence et atomicité', () => {
  it('deux acheteurs simultanés sur la dernière unité : un seul gagne, jamais de stock négatif', async () => {
    assert.equal(await stockOf('prod_3'), 1);
    for (const u of ['u_eve', 'u_frank', 'u_grace']) {
      await post('/cart/items', { as: u, body: { productId: 'prod_3' } });
    }
    await post('/cart/items', { as: 'u_grace', body: { productId: 'prod_2' } });

    const delivery = { deliveryAddress: 'Lot 1 Analakely', deliveryPhone: '0341234500' };
    const [a, b] = await Promise.all([
      post('/orders/checkout', { as: 'u_eve', body: delivery }),
      post('/orders/checkout', { as: 'u_frank', body: delivery }),
    ]);
    assert.deepEqual([a.status, b.status].sort(), [201, 409], 'exactement un succès et un refus');
    const loser = a.status === 409 ? a : b;
    assert.equal(loser.body.error.code, 'CHECKOUT_CONFLICT');
    assert.equal(loser.body.error.details.items[0].productId, 'prod_3');

    assert.equal(await stockOf('prod_3'), 0);
    assert.equal((await get('/products/prod_3')).body.status, 'INDISPONIBLE', 'stock 0 -> INDISPONIBLE');

    // Atomicité : grace a prod_2 (dispo) + prod_3 (épuisé entre-temps) -> rien n'est commandé du tout.
    const stock2 = await stockOf('prod_2');
    const g = await post('/orders/checkout', { as: 'u_grace', body: delivery });
    assert.equal(g.status, 409);
    assert.equal(await stockOf('prod_2'), stock2, 'prod_2 n\'a pas été touché');
    assert.equal((await get('/cart', { as: 'u_grace' })).body.items.length, 2, 'le panier est conservé');
    assert.equal((await get('/orders', { as: 'u_grace' })).body.length, 0);

    const cart = (await get('/cart', { as: 'u_grace' })).body;
    assert.equal(cart.items.find((i) => i.productId === 'prod_3').available, false);
  });

  it('suppression d\'un produit : les commandes passées sont conservées', async () => {
    assert.equal((await del(`/products/${t.tableId}`, { as: 'u_alice' })).status, 403);
    assert.equal((await del(`/products/${t.tableId}`, { as: 'u_bob' })).status, 204);
    assert.equal((await get(`/products/${t.tableId}`)).status, 404);
    const order = (await get(`/orders/${t.orders.u_bob.id}`, { as: 'u_alice' })).body;
    assert.equal(order.items[0].productTitle, 'Table basse');
    assert.equal(order.items[0].productId, null);
  });
});
