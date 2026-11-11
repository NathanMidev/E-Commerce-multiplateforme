import { Router } from 'express';
import { userJson } from '../mappers.js';
import { badRequest, notFound } from '../errors.js';
import { isFacebookUrl, normalizePhone, parse, profilePatchSchema } from '../validation.js';

const COLUMNS = {
  name: 'name',
  username: 'username',
  avatarUrl: 'avatar_url',
  city: 'city',
  address: 'address',
  phoneNumber: 'phone_number',
  whatsapp: 'whatsapp',
  facebookLink: 'facebook_link',
};

export function usersRouter({ pool, auth }) {
  const router = Router();

  const loadUser = async (id) => {
    const { rows } = await pool.query('SELECT * FROM v_users WHERE id = $1', [id]);
    return rows[0];
  };

  // Mon profil complet (créé au premier appel avec les infos du compte Google / email).
  router.get('/users/me', auth.requireAuth, async (req, res) => {
    res.json(userJson(await loadUser(req.auth.uid), 'self'));
  });

  // Modifier mon profil. Les statistiques et le niveau ne peuvent PAS être modifiés ici.
  router.patch('/users/me', auth.requireAuth, async (req, res) => {
    const body = parse(profilePatchSchema, req.body);

    for (const field of ['phoneNumber', 'whatsapp']) {
      if (body[field]) {
        const normalized = normalizePhone(body[field]);
        if (!normalized) throw badRequest(`${field} : numéro invalide (ex : 034 12 345 67 ou +261 34 12 345 67)`);
        body[field] = normalized;
      }
    }
    if (body.facebookLink && !isFacebookUrl(body.facebookLink)) {
      throw badRequest('facebookLink : lien Facebook invalide (ex : https://facebook.com/ma.boutique)');
    }

    const sets = [];
    const params = [];
    for (const [key, column] of Object.entries(COLUMNS)) {
      if (body[key] !== undefined) {
        params.push(body[key]);
        sets.push(`${column} = $${params.length}`);
      }
    }
    if (!sets.length) throw badRequest('Aucun champ à modifier');
    params.push(req.auth.uid);
    await pool.query(`UPDATE users SET ${sets.join(', ')} WHERE id = $${params.length}`, params);

    res.json(userJson(await loadUser(req.auth.uid), 'self'));
  });

  // Profil d'un vendeur : public ; les moyens de contact exigent d'être connecté.
  router.get('/users/:id', auth.optionalAuth, async (req, res) => {
    const user = await loadUser(req.params.id);
    if (!user) throw notFound('Utilisateur introuvable');
    const viewer = !req.auth ? 'anonymous' : req.auth.uid === user.id ? 'self' : 'member';
    res.json(userJson(user, viewer));
  });

  return router;
}
