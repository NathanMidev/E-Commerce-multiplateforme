// Les noms de champs JSON correspondent EXACTEMENT aux modèles Java de l'app Android (Gson),
// afin que l'app et le site web consomment la même réponse.
const ms = (d) => (d ? new Date(d).getTime() : null);

export function productJson(r) {
  return {
    id: r.id,
    title: r.title,
    description: r.description,
    price: r.price,
    status: r.status,
    categoryId: r.category_id,
    sellerId: r.seller_id,
    sellerName: r.seller_name ?? null,
    thumbnailUrl: r.thumbnail_url,
    modele3d: r.modele3d,
    model3dUrl: r.model3d_url,
    imageUrls: r.image_urls ?? [],
    stock: r.stock,
    createdAt: ms(r.created_at),
  };
}

export function categoryJson(r) {
  return { id: r.id, name: r.name, description: r.description, iconName: r.icon_name };
}

/**
 * Profil utilisateur selon le lecteur :
 *  - "self"      : tout (email, adresse complète…)
 *  - "member"    : profil public + moyens de contact (compte connecté requis)
 *  - "anonymous" : profil public uniquement
 */
export function userJson(r, viewer = 'anonymous') {
  const base = {
    id: r.id,
    name: r.name,
    avatarUrl: r.avatar_url,
    city: r.city,
    totalSales: r.total_sales,
    responseScore: r.response_score,
    level: r.level,
    createdAt: ms(r.created_at),
  };
  if (viewer === 'anonymous') return base;
  const withContacts = {
    ...base,
    phoneNumber: r.phone_number,
    whatsapp: r.whatsapp,
    facebookLink: r.facebook_link,
  };
  if (viewer === 'member') return withContacts;
  return { ...withContacts, email: r.email, address: r.address };
}

export function cartJson(rows) {
  const items = rows.map((r) => ({
    productId: r.product_id,
    title: r.title,
    thumbnailUrl: r.thumbnail_url,
    unitPrice: r.price,
    quantity: r.quantity,
    stock: r.stock,
    available: r.status !== 'INDISPONIBLE' && r.stock >= r.quantity,
    sellerId: r.seller_id,
    sellerName: r.seller_name,
    lineTotal: Math.round(r.price * 100) * r.quantity / 100,
  }));
  return {
    items,
    itemCount: items.reduce((n, i) => n + i.quantity, 0),
    total: items.reduce((t, i) => t + Math.round(i.lineTotal * 100), 0) / 100,
  };
}

export function orderJson(o, items) {
  return {
    id: o.id,
    orderNumber: o.order_number,
    reference: `CMD-${String(o.order_number).padStart(6, '0')}`,
    buyerId: o.buyer_id,
    buyerName: o.buyer_name,
    sellerId: o.seller_id,
    sellerName: o.seller_name,
    status: o.status,
    paymentMethod: o.payment_method,
    deliveryAddress: o.delivery_address,
    deliveryPhone: o.delivery_phone,
    note: o.note,
    total: o.total,
    createdAt: ms(o.created_at),
    updatedAt: ms(o.updated_at),
    confirmedAt: ms(o.confirmed_at),
    shippedAt: ms(o.shipped_at),
    deliveredAt: ms(o.delivered_at),
    cancelledAt: ms(o.cancelled_at),
    items: items.map((i) => ({
      productId: i.product_id,
      productTitle: i.product_title,
      thumbnailUrl: i.thumbnail_url,
      unitPrice: i.unit_price,
      quantity: i.quantity,
      lineTotal: i.line_total,
    })),
  };
}
