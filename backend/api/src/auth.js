import { HttpError, unauthorized } from './errors.js';

/**
 * Deux modes d'authentification (AUTH_MODE) :
 *  - "firebase" : le client envoie « Authorization: Bearer <ID token Firebase> » ; le jeton est vérifié.
 *  - "dev"      : pour les tests locaux SANS Firebase : en-tête « X-Dev-Uid: <identifiant> ».
 *                 Refusé au démarrage si NODE_ENV=production.
 */
export function createAuthenticator(config, pool) {
  let verify;

  if (config.authMode === 'dev') {
    console.warn('⚠️  AUTH_MODE=dev : authentification par en-tête X-Dev-Uid (jamais en production !)');
    verify = async (req) => {
      const uid = req.get('x-dev-uid');
      if (!uid) return null;
      return {
        uid: uid.trim(),
        name: req.get('x-dev-name') || null,
        email: req.get('x-dev-email') || null,
        picture: null,
      };
    };
  } else {
    let authPromise;
    const getFirebaseAuth = () => {
      authPromise ??= (async () => {
        const { initializeApp, getApps } = await import('firebase-admin/app');
        const { getAuth } = await import('firebase-admin/auth');
        const app = getApps()[0] ?? initializeApp({ projectId: config.firebaseProjectId });
        return getAuth(app);
      })();
      return authPromise;
    };

    verify = async (req) => {
      const match = /^Bearer\s+(.+)$/i.exec(req.get('authorization') || '');
      if (!match) return null;
      try {
        const decoded = await (await getFirebaseAuth()).verifyIdToken(match[1]);
        return {
          uid: decoded.uid,
          name: decoded.name || null,
          email: decoded.email || null,
          picture: decoded.picture || null,
        };
      } catch {
        throw new HttpError(401, 'INVALID_TOKEN', "Jeton d'authentification invalide ou expiré");
      }
    };
  }

  /** Crée la ligne users au premier appel authentifié (les autres tables y font référence). */
  async function ensureUser(identity) {
    const name = identity.name || (identity.email ? identity.email.split('@')[0] : null) || 'Utilisateur';
    const first = await pool.query(
      `INSERT INTO users (id, name, email, avatar_url) VALUES ($1, $2, $3, $4)
       ON CONFLICT DO NOTHING RETURNING id`,
      [identity.uid, name, identity.email, identity.picture],
    );
    if (first.rowCount === 0) {
      // Soit l'utilisateur existe déjà (cas normal), soit l'email est déjà pris par un autre compte.
      const exists = await pool.query('SELECT 1 FROM users WHERE id = $1', [identity.uid]);
      if (exists.rowCount === 0) {
        await pool.query(
          'INSERT INTO users (id, name, avatar_url) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING',
          [identity.uid, name, identity.picture],
        );
      }
    }
  }

  const requireAuth = async (req, res, next) => {
    const identity = await verify(req);
    if (!identity) throw unauthorized();
    await ensureUser(identity);
    req.auth = identity;
    next();
  };

  // Authentification facultative : sans jeton on continue en anonyme ; un jeton invalide reste refusé.
  const optionalAuth = async (req, res, next) => {
    const identity = await verify(req);
    if (identity) {
      await ensureUser(identity);
      req.auth = identity;
    }
    next();
  };

  return { requireAuth, optionalAuth };
}
