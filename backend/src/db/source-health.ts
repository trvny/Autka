export interface SourceHealth {
  offerCount: number;
  lastCompletedAtEpochMs: number | null;
  lastCompletedOk: boolean | null;
  lastOffersUpserted: number | null;
}

interface SourceCountRow {
  source_id: string;
  offer_count: number;
}

interface LastRunRow {
  source_id: string;
  started_at_ms: number;
  finished_at_ms: number | null;
  offers_upserted: number;
  ok: number;
}

/** Public-safe completed-ingestion health. Raw error strings stay server-side. */
export async function getSourceHealth(db: D1Database): Promise<Map<string, SourceHealth>> {
  const [countsResult, runsResult] = await Promise.all([
    db.prepare(
      "SELECT source_id, COUNT(*) AS offer_count FROM offers GROUP BY source_id",
    ).all<SourceCountRow>(),
    db.prepare(
      `WITH ranked AS (
         SELECT source_id, started_at_ms, finished_at_ms, offers_upserted, ok,
                ROW_NUMBER() OVER (
                  PARTITION BY source_id ORDER BY started_at_ms DESC, id DESC
                ) AS rn
         FROM ingest_runs
       )
       SELECT source_id, started_at_ms, finished_at_ms, offers_upserted, ok
       FROM ranked WHERE rn = 1`,
    ).all<LastRunRow>(),
  ]);

  const health = new Map<string, SourceHealth>();
  for (const row of countsResult.results) {
    health.set(row.source_id, {
      offerCount: Number(row.offer_count ?? 0),
      lastCompletedAtEpochMs: null,
      lastCompletedOk: null,
      lastOffersUpserted: null,
    });
  }
  for (const row of runsResult.results) {
    const current = health.get(row.source_id);
    health.set(row.source_id, {
      offerCount: current?.offerCount ?? 0,
      lastCompletedAtEpochMs: Number(row.finished_at_ms ?? row.started_at_ms),
      lastCompletedOk: Number(row.ok) === 1,
      lastOffersUpserted: Number(row.offers_upserted ?? 0),
    });
  }
  return health;
}
