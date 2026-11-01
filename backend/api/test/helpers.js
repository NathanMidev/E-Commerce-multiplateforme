import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import pg from 'pg';
import { createApp } from '../src/app.js';
import { createPool } from '../src/db.js';

const dbDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../db');
const adminUrl = process.env.DATABASE_URL || 'postgres://marketplace:marketplace@localhost:5432/marketplace';

/** Crée une base PostgreSQL temporaire (schéma + données de démo), démarre l'API dessus. */
export async function startTestServer() {
  const dbName = `marketplace_test_${process.pid}_${Date.now()}`;
  const admin = new pg.Client({ connectionString: adminUrl });
  await admin.connect();
  await admin.query(`CREATE DATABASE ${dbName}`);
  await admin.end();

  const url = new URL(adminUrl);
  url.pathname = `/${dbName}`;
  const pool = createPool(url.toString());
  for (const file of ['01_schema.sql', '02_seed.sql']) {
    await pool.query(await readFile(path.join(dbDir, file), 'utf8'));
  }

  const config = { nodeEnv: 'test', port: 0, databaseUrl: url.toString(), authMode: 'dev', firebaseProjectId: null, corsOrigins: [] };
  const server = createApp({ pool, config }).listen(0);
  await new Promise((resolve) => server.once('listening', resolve));
  const baseUrl = `http://127.0.0.1:${server.address().port}/api`;

  async function stop() {
    await new Promise((resolve) => server.close(resolve));
    await pool.end();
    const cleaner = new pg.Client({ connectionString: adminUrl });
    await cleaner.connect();
    await cleaner.query(`DROP DATABASE IF EXISTS ${dbName} WITH (FORCE)`);
    await cleaner.end();
  }

  /** Appel HTTP ; `as` = identifiant dev de l'utilisateur connecté (en-tête X-Dev-Uid). */
  async function call(method, route, { as, name, email, body } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (as) headers['X-Dev-Uid'] = as;
    if (name) headers['X-Dev-Name'] = name;
    if (email) headers['X-Dev-Email'] = email;
    const res = await fetch(baseUrl + route, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    const text = await res.text();
    return { status: res.status, body: text ? JSON.parse(text) : null };
  }

  return { pool, call, stop };
}
