import fs from 'node:fs';
import { loadConfig } from './config.js';
import { createPool } from './db.js';
import { createApp } from './app.js';

for (const envPath of ['.env', '../.env', '../../.env']) {
  if (fs.existsSync(envPath)) {
    if (typeof process.loadEnvFile === 'function') {
      try { process.loadEnvFile(envPath); } catch {}
    }
    break;
  }
}

const config = loadConfig();
const pool = createPool(config.databaseUrl);
const app = createApp({ pool, config });

const server = app.listen(config.port, () => {
  console.log(`MarketplaceApp API : http://localhost:${config.port}/api  (auth: ${config.authMode})`);
});

const shutdown = () => {
  server.close(async () => { await pool.end(); process.exit(0); });
};
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
