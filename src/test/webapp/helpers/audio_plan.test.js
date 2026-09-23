const assert = require("node:assert/strict");
const test = require("node:test");
const { resolveAudioPlan } = require("./audio_plan.js");
const { getPlanNarration } = require("./feature_video_plan.js");

const options = { modelId: "eleven_v3", voiceId: "test-voice" };
const shot = (id, text, type = "auto") => ({ id, type, durationSeconds: 1, title: id, "text-sk": text });

test("packs whole shots up to the limit including paragraph separators and Unicode characters", () => {
  const plan = { language: "sk", shots: [shot("first", "😀".repeat(2500)), shot("second", "x".repeat(2498)), shot("third", "End.")] };
  const { chunks } = resolveAudioPlan(plan, options);
  assert.deepEqual(chunks.map(chunk => chunk.shotIds), [["first", "second"], ["third"]]);
  assert.deepEqual(chunks.map(chunk => chunk.characters), [5000, 4]);
  assert.equal(chunks.map(chunk => chunk.text).join("\n\n"), getPlanNarration(plan));
});

test("follows reordered localized narration including manual/head shots and skips silent shots", () => {
  const shots = [shot("last", "Last.", "head"), shot("silent", ""), shot("first", "First.", "manual")];
  for (const shot of shots) {
    shot["text-en"] = shot.id === "silent" ? "" : `${shot.id} translated`;
    shot.shot = shot.prepare = () => { throw new Error("Browser callbacks must not execute"); };
  }
  const plan = { language: "sk", shots };
  const original = JSON.stringify(plan);
  const result = resolveAudioPlan(plan, { ...options, language: "en" });
  assert.equal(result.language, "en");
  assert.deepEqual(result.chunks.map(chunk => chunk.shotIds), [["last", "first"]]);
  assert.equal(result.chunks[0].text, "last translated\n\nfirst translated");
  assert.equal(JSON.stringify(plan), original);
  assert.throws(() => resolveAudioPlan(plan, { ...options, language: "cs" }), /missing text-cs/);
  assert.throws(() => resolveAudioPlan({ shots: [shot("silent", "")] }, options), /narration must not be empty/);
});

test("rejects a single oversized shot without splitting or truncating its text", () => {
  const plan = { shots: [shot("valid", "Short."), shot("oversized", "x".repeat(5001))] };
  assert.throws(() => resolveAudioPlan(plan, options), /Shot oversized has 5001 characters.*5000-character limit.*eleven_v3/);
  assert.equal(resolveAudioPlan({ shots: [shot("exact", "x".repeat(5000))] }, options).chunks.length, 1);
});

test("uses model-specific limits and a conservative fallback for unlisted models", () => {
  const plan = { shots: [shot("first", "a".repeat(3000)), shot("second", "b".repeat(3000))] };
  for (const [modelId, limit] of [["eleven_multilingual_v2", 10000], ["eleven_flash_v2_5", 40000], ["eleven_flash_v2", 30000]]) {
    const result = resolveAudioPlan(plan, { ...options, modelId });
    assert.equal(result.characterLimit, limit);
    assert.equal(result.chunks.length, 1);
  }
  assert.equal(resolveAudioPlan(plan, { ...options, modelId: "custom-model" }).chunks.length, 2);
});

test("keeps short legacy narration in one part and requires shots for oversized legacy text", () => {
  const result = resolveAudioPlan("\r\nFirst line.\r\nSecond line.\r\n", options);
  assert.equal(result.language, null);
  assert.equal(result.chunks[0].text, "First line.\nSecond line.");
  assert.throws(() => resolveAudioPlan("x".repeat(5001), options), /Legacy narration has 5001 characters.*Split it into smaller shots/);
});
