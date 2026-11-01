export class HttpError extends Error {
  constructor(status, code, message, details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

export const badRequest = (message, details) => new HttpError(400, 'BAD_REQUEST', message, details);
export const unauthorized = (message = 'Authentification requise') => new HttpError(401, 'AUTH_REQUIRED', message);
export const forbidden = (message, code = 'FORBIDDEN') => new HttpError(403, code, message);
export const notFound = (message = 'Ressource introuvable') => new HttpError(404, 'NOT_FOUND', message);
export const conflict = (message, code = 'CONFLICT', details) => new HttpError(409, code, message, details);

export function notFoundHandler(req, res) {
  res.status(404).json({ error: { code: 'NOT_FOUND', message: `Route inconnue : ${req.method} ${req.path}` } });
}

// Format d'erreur unique : { "error": { "code": "...", "message": "...", "details": ... } }
export function errorHandler(err, req, res, next) { // eslint-disable-line no-unused-vars
  if (res.headersSent) return next(err);

  if (err instanceof HttpError) {
    return res.status(err.status).json({ error: { code: err.code, message: err.message, details: err.details } });
  }
  if (err?.type === 'entity.parse.failed') {
    return res.status(400).json({ error: { code: 'BAD_JSON', message: 'Corps de requête JSON invalide' } });
  }
  if (err?.type === 'entity.too.large') {
    return res.status(413).json({ error: { code: 'TOO_LARGE', message: 'Corps de requête trop volumineux' } });
  }
  // Erreurs PostgreSQL connues
  if (err?.code === '23505') {
    return res.status(409).json({ error: { code: 'CONFLICT', message: 'Cette valeur existe déjà' } });
  }
  if (err?.code === '23503') {
    return res.status(400).json({ error: { code: 'BAD_REFERENCE', message: 'Référence invalide (élément lié introuvable)' } });
  }
  if (err?.code === '23514') {
    return res.status(409).json({ error: { code: 'INVALID_STATE', message: err.message } });
  }

  console.error(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`, err);
  res.status(500).json({ error: { code: 'INTERNAL', message: 'Erreur interne du serveur' } });
}
