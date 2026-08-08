import { env, createExecutionContext, waitOnExecutionContext } from "cloudflare:test";
import { beforeAll, describe, expect, it } from "vitest";
import worker from "../src/index";
import { getSourceHealth } from "../src/db/source-health";

async function call(path: string, init?: RequestInit) {
  const ctx = createExecutionContext();
  const res = await worker.fetch(new Request(`https://x${path}`, init), env, ctx);
  await waitOnExecutionContext(ctx);
  return res;
}

describe("sources health", () => {
  beforeAll(async () => {
    const mutableEnv = env as unknown as {
      ADMIN_TOKEN: string;
      ENABLE_MOCK_SOURCE: string;
    };
    mutableEnv.ADMIN_TOKEN = "test-token";
    mutableEnv.ENABLE_MOCK_SOURCE = "true";
    const res = await call("/admin/ingest", {
      method: "POST",
      headers: { authorization: "Bearer test-token" },
    });
    expect(res.status).toBe(200);
  });

  it("reports public-safe latest completed ingestion health", async () => {
    const res = await call("/sources");
    expect(res.status).toBe(200);
    const body = await res.json() as {
      sources: Array<{
        id: string;
        enabled: boolean;
        offerCount: number | null;
        lastCompletedAtEpochMs: number | null;
        lastCompletedOk: boolean | null;
        lastOffersUpserted: number | null;
      }>;
    };

    const mock = body.sources.find((source) => source.id === "mock");
    expect(mock).toMatchObject({ enabled: true, lastCompletedOk: true });
    expect(mock?.offerCount).toBeGreaterThan(0);
    expect(mock?.lastCompletedAtEpochMs).toEqual(expect.any(Number));
    expect(mock?.lastOffersUpserted).toBeGreaterThan(0);

    const disabled = body.sources.find((source) => source.id === "otomoto");
    expect(disabled).toMatchObject({ enabled: false, offerCount: 0 });
  });

  it("preserves the latest health row for a quiet source when pruning history", async () => {
    await env.DB.prepare(
      `INSERT INTO ingest_runs
       (source_id, started_at_ms, finished_at_ms, offers_upserted, ok, error)
       VALUES ('quiet-source', 1, 2, 7, 1, NULL)`,
    ).run();
    await env.DB.prepare(
      `WITH RECURSIVE seq(n) AS (
         SELECT 1
         UNION ALL
         SELECT n + 1 FROM seq WHERE n < 501
       )
       INSERT INTO ingest_runs
       (source_id, started_at_ms, finished_at_ms, offers_upserted, ok, error)
       SELECT 'noisy-source', 1000 + n, 1000 + n, 0, 1, NULL FROM seq`,
    ).run();

    try {
      const ingest = await call("/admin/ingest", {
        method: "POST",
        headers: { authorization: "Bearer test-token" },
      });
      expect(ingest.status).toBe(200);

      const quiet = (await getSourceHealth(env.DB)).get("quiet-source");
      expect(quiet).toMatchObject({
        lastCompletedAtEpochMs: 2,
        lastCompletedOk: true,
        lastOffersUpserted: 7,
      });
    } finally {
      await env.DB.prepare(
        "DELETE FROM ingest_runs WHERE source_id IN ('quiet-source', 'noisy-source')",
      ).run();
    }
  });
});
