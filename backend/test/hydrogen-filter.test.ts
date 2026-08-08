import { env, createExecutionContext, waitOnExecutionContext } from "cloudflare:test";
import { describe, expect, it } from "vitest";
import worker from "../src/index";

async function call(path: string) {
  const ctx = createExecutionContext();
  const res = await worker.fetch(new Request(`https://x${path}`), env, ctx);
  await waitOnExecutionContext(ctx);
  return res;
}

describe("hydrogen fuel filter", () => {
  it("accepts HYDROGEN as a normalized fuel type", async () => {
    const res = await call("/offers?fuelTypes=HYDROGEN");
    expect(res.status).toBe(200);
    const body = await res.json() as { offers: unknown[]; count: number };
    expect(body.offers).toEqual([]);
    expect(body.count).toBe(0);
  });
});
