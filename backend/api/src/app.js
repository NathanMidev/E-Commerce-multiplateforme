import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import { createAuthenticator } from './auth.js';
import { errorHandler, notFoundHandler } from './errors.js';
import { catalogRouter } from './routes/catalog.js';
import { usersRouter } from './routes/users.js';
import { cartRouter } from './routes/cart.js';
import { ordersRouter } from './routes/orders.js';

export function createApp({ pool, config }) {
  const app = express();
  app.disable('x-powered-by');
  app.use(helmet());

  // CORS pour la version web : liste blanche dans CORS_ORIGINS (l'app Android n'envoie pas d'Origin).
  app.use(cors({
    origin(origin, callback) {
      if (!origin) return callback(null, true);
      if (config.authMode === 'dev' && config.corsOrigins.length === 0) return callback(null, true);
      callback(null, config.corsOrigins.includes(origin));
    },
    allowedHeaders: ['Authorization', 'Content-Type', 'X-Dev-Uid', 'X-Dev-Name', 'X-Dev-Email'],
  }));
  app.use(express.json({ limit: '100kb' }));

  app.use((req, res, next) => {
    const start = Date.now();
    res.on('finish', () => {
      console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl} -> ${res.statusCode} (${Date.now() - start}ms)`);
    });
    next();
  });

  const auth = createAuthenticator(config, pool);
  const deps = { pool, auth, config };
  const api = express.Router();

  api.get('/health', async (req, res) => {
    await pool.query('SELECT 1');
    res.json({ status: 'ok', auth: config.authMode });
  });
  api.use(catalogRouter(deps));
  api.use(usersRouter(deps));
  api.use(cartRouter(deps));
  api.use(ordersRouter(deps));

  app.use('/api', api);
  app.use(notFoundHandler);
  app.use(errorHandler);
  return app;
}
