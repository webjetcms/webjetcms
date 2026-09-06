const assert = require("node:assert/strict");
const test = require("node:test");
const { readSubscription, formatCreditSummary, startCreditReport } = require("./elevenlabs_credits.js");
const { requestSpeechAudio } = require("./elevenlabs_client.js");

test("uses the response charge, including zero, instead of counting narration characters", async () => {
  for (const header of ["12.5", "0", null, "", "invalid", "-1"]) {
    let cost = null;
    const headers = { "content-type": "audio/mpeg" };
    if (header !== null) headers["character-cost"] = header;
    const audio = await requestSpeechAudio({ apiKey: "test-key", text: "Much longer narration.", modelId: "model", voiceId: "voice",
      fetchImpl: async () => new Response("audio", { headers }), onCost: value => { cost = value; } });
    assert.equal(audio.toString(), "audio");
    assert.equal(cost, ["12.5", "0"].includes(header) ? Number(header) : null);
  }
});

test("reports exact audio charges and the current remaining limit", () => {
  assert.equal(formatCreditSummary({ label: "audio", before: { used: 100, limit: 1000, reset: 5 },
    after: { used: 142, limit: 1000, reset: 5 }, cost: 42 }),
  "[ElevenLabs audio] Credits used: 42 (TTS response) | Credits remaining in current limit: 858");
});

test("labels the complete head account delta as approximate and unfinished jobs as provisional", () => {
  const line = formatCreditSummary({ label: "head", before: { used: 100, limit: 1000, reset: 5 },
    after: { used: 1150, limit: 1000, reset: 5 }, provisional: true });
  assert.match(line, /Credits used so far.*1050 \(approximate account delta/);
  assert.match(line, /Credits remaining in current limit: 0$/);
});

test("handles missing counters, changed periods and unavailable reports without inventing usage", () => {
  const before = { used: 100, limit: 1000, reset: 5 };
  for (const after of [{ used: 2, limit: 1000, reset: 6 }, { used: 2, limit: 1000, reset: 5 }, { used: 120, limit: 1000, reset: null }, { error: "HTTP 403" }]) {
    assert.match(formatCreditSummary({ label: "head", before, after }), /Credits used: unavailable/);
  }
  assert.match(formatCreditSummary({ label: "audio", before: { error: "HTTP 403" }, after: { error: "HTTP 403" }, cost: 2 }),
    /Credits used: 2.*Credits remaining in current limit: unavailable/);
});

test("subscription errors are bounded, sanitized and non-fatal", async () => {
  const result = await readSubscription({ apiKey: "private-key", fetchImpl: async (url, options) => {
    assert.equal(url, "https://api.elevenlabs.io/v1/user/subscription");
    assert.equal(options.headers["xi-api-key"], "private-key");
    assert.ok(options.signal instanceof AbortSignal);
    return new Response('private-key missing user_read', { status: 403 });
  } });
  assert.match(result.error, /HTTP 403/);
  assert.ok(!result.error.includes("private-key"));
  for (const data of [{}, { character_count: "1", character_limit: 100 }, { character_count: 2, character_limit: -1 }]) {
    assert.match((await readSubscription({ apiKey: "key", fetchImpl: async () => Response.json(data) })).error, /valid credit counters/);
  }
});

test("finishes one report after partial failure and never throws a logger failure", async () => {
  let calls = 0;
  const lines = [];
  const report = await startCreditReport({ apiKey: "key", label: "head", log: line => lines.push(line),
    fetchImpl: async () => Response.json({ character_count: ++calls * 100, character_limit: 1000, next_character_count_reset_unix: 9000 }) });
  report.recordCost(20);
  await report.finish({ provisional: true });
  assert.equal(calls, 2);
  assert.match(lines[0], /100 \(approximate account delta/);
  const broken = await startCreditReport({ apiKey: "key", label: "audio", log: () => { throw new Error("logger failure"); },
    fetchImpl: async () => { throw new Error("offline"); } });
  await assert.doesNotReject(broken.finish());
});

test("audio artifact reports its actual response cost even when billing reads fail, without masking generation errors", async () => {
  const fs = require("node:fs/promises");
  const path = require("node:path");
  const { generateAudioArtifact } = require("./elevenlabs_client.js");
  const directory = await fs.mkdtemp(path.join(require("node:os").tmpdir(), "wj-audio-credits-"));
  const targetPath = path.join(directory, "voice.mp3");
  const originalLog = console.log;
  const lines = [];
  console.log = line => lines.push(line);
  try {
    for (const failed of [false, true]) {
      let subscriptions = 0;
      const generation = generateAudioArtifact({ targetPath, apiKey: "key", text: "Narration", modelId: "model", voiceId: "voice", creditLabel: "audio",
        fetchImpl: async url => {
          if (url.endsWith("/subscription")) { subscriptions++; throw new Error("Billing service offline"); }
          return failed ? new Response("Voice unavailable", { status: 400 }) :
            new Response("valid audio", { headers: { "content-type": "audio/mpeg", "character-cost": "7" } });
        } });
      if (failed) await assert.rejects(generation, /HTTP 400: Voice unavailable/);
      else await generation;
      assert.equal(subscriptions, 2);
      assert.equal(await fs.readFile(targetPath, "utf8"), "valid audio");
    }
    assert.match(lines[0], /Credits used: 7 \(TTS response\).*Credits remaining in current limit: unavailable/);
    assert.match(lines[1], /Credits used: unavailable \(Billing service offline\)/);
  } finally {
    console.log = originalLog;
    await fs.rm(directory, { recursive: true, force: true });
  }
});
