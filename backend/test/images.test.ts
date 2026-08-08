import { describe, expect, it } from "vitest";
import { readBodyUpTo } from "../src/ingest/images";

function stream(...chunks: number[][]): ReadableStream<Uint8Array> {
  return new ReadableStream({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(Uint8Array.from(chunk));
      controller.close();
    },
  });
}

describe("readBodyUpTo", () => {
  it("accepts a body exactly at the limit", async () => {
    const body = await readBodyUpTo(stream([1, 2], [3, 4, 5]), 5);
    expect(body).toEqual(Uint8Array.from([1, 2, 3, 4, 5]));
  });

  it("rejects a chunked body once it crosses the limit", async () => {
    const body = await readBodyUpTo(stream([1, 2, 3], [4, 5, 6]), 5);
    expect(body).toBeNull();
  });
});
