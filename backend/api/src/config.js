// Configuration lue depuis les variables d'environnement (voir ../../.env.example).
export function loadConfig(env = process.env) {
  const authMode = (env.AUTH_MODE || 'firebase').toLowerCase();
  const nodeEnv = env.NODE_ENV || 'development';

  if (!['firebase', 'dev'].includes(authMode)) {
    throw new Error(`AUTH_MODE invalide : "${authMode}" (attendu : firebase ou dev)`);
  }
  if (authMode === 'dev' && nodeEnv === 'production') {
    throw new Error('AUTH_MODE=dev est interdit quand NODE_ENV=production');
  }
  if (authMode === 'firebase' && !env.FIREBASE_PROJECT_ID) {
    throw new Error(
      'FIREBASE_PROJECT_ID est requis (project_id de google-services.json). ' +
      'Pour un essai local sans Firebase : AUTH_MODE=dev.'
    );
  }

  return {
    nodeEnv,
    port: Number(env.PORT || 8080),
    databaseUrl: env.DATABASE_URL || 'postgres://marketplace:marketplace@localhost:5432/marketplace',
    authMode,
    firebaseProjectId: env.FIREBASE_PROJECT_ID || null,
    corsOrigins: (env.CORS_ORIGINS || '').split(',').map((s) => s.trim()).filter(Boolean),
  };
}
