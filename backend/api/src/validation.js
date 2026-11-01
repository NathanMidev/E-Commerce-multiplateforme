import { z } from 'zod';
import { badRequest } from './errors.js';

/** Valide `data` avec un schéma zod ; lève une erreur 400 lisible sinon. */
export function parse(schema, data) {
  const result = schema.safeParse(data ?? {});
  if (!result.success) {
    const issues = result.error.issues.map((i) => ({ field: i.path.join('.'), message: i.message }));
    const first = issues[0];
    throw badRequest(first ? `${first.field ? first.field + ' : ' : ''}${first.message}` : 'Requête invalide', issues);
  }
  return result.data;
}

export function isHttpUrl(value) {
  try {
    const u = new URL(value);
    return u.protocol === 'http:' || u.protocol === 'https:';
  } catch {
    return false;
  }
}

/**
 * Normalise un numéro de téléphone au format international.
 *   034 12 345 67 / +261 34 12 345 67 / 00261341234567  ->  +261341234567
 *   autre pays (diaspora) : accepté s'il est déjà au format +XXXXXXXX
 * Renvoie null si le numéro est invalide.
 */
export function normalizePhone(raw) {
  const s = String(raw).replace(/[\s().-]/g, '');
  let m = /^(?:\+|00)?261(3\d{8})$/.exec(s);
  if (m) return `+261${m[1]}`;
  m = /^0(3\d{8})$/.exec(s);
  if (m) return `+261${m[1]}`;
  if (/^\+[1-9]\d{7,14}$/.test(s)) return s;
  return null;
}

const FACEBOOK_HOSTS = ['facebook.com', 'fb.com', 'fb.me'];
export function isFacebookUrl(value) {
  if (!isHttpUrl(value)) return false;
  const host = new URL(value).hostname.toLowerCase();
  return FACEBOOK_HOSTS.some((h) => host === h || host.endsWith(`.${h}`));
}

// ---- schémas réutilisables
export const urlField = z.string().trim().max(2048).refine(isHttpUrl, 'URL http(s) invalide');
export const idField = z.string().trim().min(1).max(100);
export const moneyField = z.number().finite().min(0).max(1e12);

const emptyToNull = (schema) =>
  z.union([z.literal('').transform(() => null), schema]).nullable();

export const profilePatchSchema = z.object({
  name: z.string().trim().min(1).max(120),
  avatarUrl: emptyToNull(urlField),
  city: emptyToNull(z.string().trim().max(120)),
  address: emptyToNull(z.string().trim().max(300)),
  phoneNumber: emptyToNull(z.string().trim().max(40)),
  whatsapp: emptyToNull(z.string().trim().max(40)),
  facebookLink: emptyToNull(z.string().trim().max(300)),
}).partial().strict();

// ATTENTION : aucune valeur par défaut (.default) dans ce schéma. Avec zod 4 elles s'appliquent aussi
// dans une modification partielle (PATCH) et réinitialiseraient stock, description, images...
// Les valeurs par défaut de la création sont appliquées dans la route POST /products.
export const productCreateSchema = z.object({
  title: z.string().trim().min(1).max(200),
  description: z.string().trim().max(5000).optional(),
  price: moneyField,
  categoryId: idField.nullable().optional(),
  thumbnailUrl: urlField.nullable().optional(),
  imageUrls: z.array(urlField).max(10).optional(),
  model3dUrl: urlField.nullable().optional(),
  stock: z.number().int().min(0).max(1_000_000).optional(),
  status: z.enum(['NOUVEAU', 'DISPONIBLE', 'INDISPONIBLE', 'A_VENDRE']).optional(),
}).strict();

export const productPatchSchema = productCreateSchema.partial().strict();

export const productQuerySchema = z.object({
  categoryId: z.string().trim().min(1).optional(),
  minPrice: z.coerce.number().min(0).optional(),
  maxPrice: z.coerce.number().min(0).optional(),
  arOnly: z.enum(['true', 'false']).optional(),
  searchQuery: z.string().trim().max(100).optional(),
  sellerId: z.string().trim().min(1).optional(),
  sortBy: z.enum(['recent', 'price_asc', 'price_desc']).default('recent'),
  limit: z.coerce.number().int().min(1).max(100).default(50),
  offset: z.coerce.number().int().min(0).default(0),
});

export const cartAddSchema = z.object({
  productId: idField,
  quantity: z.number().int().min(1).max(999).default(1),
}).strict();

export const cartSetSchema = z.object({
  quantity: z.number().int().min(0).max(999),
}).strict();

export const PAYMENT_METHODS = ['A_LA_LIVRAISON', 'MVOLA', 'ORANGE_MONEY', 'AIRTEL_MONEY'];
export const ORDER_STATUSES = ['EN_ATTENTE', 'CONFIRMEE', 'EXPEDIEE', 'LIVREE', 'ANNULEE'];

export const checkoutSchema = z.object({
  deliveryAddress: z.string().trim().min(1).max(300).optional(),
  deliveryPhone: z.string().trim().min(1).max(40).optional(),
  paymentMethod: z.enum(PAYMENT_METHODS).default('A_LA_LIVRAISON'),
  note: z.string().trim().max(500).optional(),
}).strict();

export const orderStatusSchema = z.object({ status: z.enum(ORDER_STATUSES) }).strict();
export const orderListQuerySchema = z.object({
  role: z.enum(['buyer', 'seller']).default('buyer'),
  status: z.enum(ORDER_STATUSES).optional(),
});
