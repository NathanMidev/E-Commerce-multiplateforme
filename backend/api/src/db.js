import pg from 'pg';

// NUMERIC (prix, totaux) et BIGINT (numéro de commande) sont renvoyés en nombre JS, pas en chaîne.
pg.types.setTypeParser(1700, (v) => (v === null ? null : Number(v)));
pg.types.setTypeParser(20, (v) => (v === null ? null : Number(v)));

export function createPool(connectionString) {
  return new pg.Pool({ connectionString, max: 10 });
}

/** Exécute fn(client) dans une transaction ; COMMIT si tout passe, ROLLBACK sinon. */
export async function withTransaction(pool, fn) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (err) {
    try { await client.query('ROLLBACK'); } catch { /* connexion déjà perdue */ }
    throw err;
  } finally {
    client.release();
  }
}
