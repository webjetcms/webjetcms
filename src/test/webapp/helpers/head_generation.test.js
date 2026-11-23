const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const path = require("node:path");
const os = require("node:os");
const test = require("node:test");
const { generateHeadArtifacts, requestHeadVideo, getImageMimeType, inlineReference } = require("./head_generation.js");
const { getHeadShots, DEFAULT_HEAD_MODEL_ID } = require("./head_settings.js");

const PNG = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a1ZkAAAAASUVORK5CYII=", "base64");
const MP4 = Buffer.concat([Buffer.from([0, 0, 0, 20]), Buffer.from("ftypisom00000000")]);
const createShot = id => ({ id, type: "head", title: id, durationSeconds: 5, "text-sk": `Slovak ${id}`, "text-en": `English ${id}`,
  shot: () => { throw new Error("Head callbacks must not run"); }, prepare: () => { throw new Error("Head preparation must not run"); } });

async function fixture(callback) {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "wj-head-generation-"));
  try {
    await fs.writeFile(path.join(directory, "face.png"), PNG);
    await callback(directory);
  } finally { await fs.rm(directory, { recursive: true, force: true }); }
}

function server(override = () => null) {
  const state = { posts: [], speech: [], subscriptions: 0, downloads: 0, polls: 0, requests: [], elapsed: 0 };
  return { state, now: () => state.elapsed, sleepImpl: async milliseconds => { state.elapsed += milliseconds; },
    fetchImpl: async (url, options) => {
      state.requests.push({ url, options });
      const custom = await override(url, options, state);
      if (custom) return custom;
      if (url.endsWith("/user/subscription")) return Response.json({ character_count: ++state.subscriptions * 100, character_limit: 1000, next_character_count_reset_unix: 9000 });
      if (url.includes("/text-to-speech/")) {
        state.speech.push({ url, ...JSON.parse(options.body) });
        return new Response("speech", { headers: { "content-type": "audio/mpeg", "character-cost": "20" } });
      }
      if (options.method === "POST") {
        state.posts.push(JSON.parse(options.body));
        return Response.json({ id: `job-${state.posts.length}`, status: "pending" });
      }
      if (url.startsWith("https://media.example/")) {
        state.downloads++;
        assert.equal(options.headers, undefined, "Signed downloads must never receive the ElevenLabs API key");
        return new Response(MP4, { headers: { "content-type": "video/mp4" } });
      }
      state.polls++;
      return Response.json({ id: url.split("/").at(-1), status: "completed", content_mime_type: "video/mp4", content_url: "https://media.example/result.mp4" });
    }
  };
}

function parameters(directory, mock, overrides = {}) {
  return { plan: { language: "sk", shots: [createShot("intro")] }, options: { imagePath: "face.png" },
    scenarioFile: path.join(directory, "example.js"), outputDirectory: path.join(directory, "out"), apiKey: "key", log: () => {}, ...mock, ...overrides };
}

test("generates localized head shots in order with shared and per-shot overrides and one account summary", async () => {
  await fixture(async directory => {
    const mock = server();
    const lines = [];
    const plan = { language: "sk", shots: [createShot("last"), { ...createShot("auto"), type: "auto" }, createShot("first")] };
    plan.shots[2].head = { resolution: "480p", modelId: "compatible-lipsync", audio: { voiceId: "shot-voice" } };
    const original = JSON.stringify(plan);
    const result = await generateHeadArtifacts(parameters(directory, mock, { plan, log: line => lines.push(line),
      options: { language: "en", imagePath: "face.png", audio: { modelId: "tts-model", voiceId: "common-voice" } } }));
    assert.deepEqual(result.map(item => item.id), ["last", "first"]);
    assert.equal(JSON.stringify(plan), original);
    assert.deepEqual(mock.state.speech.map(item => item.text), ["English last", "English first"]);
    assert.ok(mock.state.speech[1].url.includes("shot-voice"));
    assert.equal(mock.state.speech[1].model_id, "tts-model");
    assert.equal(mock.state.posts[0].model_id, DEFAULT_HEAD_MODEL_ID);
    assert.equal(mock.state.posts[0].resolution, "720p");
    assert.equal(mock.state.posts[1].model_id, "compatible-lipsync");
    assert.equal(mock.state.posts[1].resolution, "480p");
    assert.deepEqual(Object.keys(mock.state.posts[0]).sort(), ["audio", "image", "model_id", "resolution"]);
    assert.equal(mock.state.posts[0].image.content_base64, PNG.toString("base64"));
    assert.equal(mock.state.posts[0].audio.content_base64, Buffer.from("speech").toString("base64"));
    for (const item of result) {
      assert.match(item.video, /example-(?:last|first)-en\.mp4$/);
      assert.deepEqual(await fs.readFile(item.video), MP4);
      assert.equal(await fs.readFile(item.audio, "utf8"), "speech");
    }
    assert.equal(mock.state.subscriptions, 2);
    assert.equal(lines.filter(line => line.includes("Credits used:")).length, 1);
    assert.match(lines.at(-1), /100 \(approximate account delta.*Credits remaining in current limit: 800$/);
    assert.equal((await fs.readdir(path.join(directory, "out"))).filter(file => file.endsWith(".tmp")).length, 0);
    await generateHeadArtifacts(parameters(directory, mock));
    await generateHeadArtifacts(parameters(directory, mock));
    assert.equal(mock.state.posts.length, 4, "Repeated runs must generate again, even when outputs exist");
  });
});

test("validates the entire plan, images and output paths before any subscription or paid API request", async () => {
  await fixture(async directory => {
    const invalidPlans = [
      { shots: [createShot("intro"), { ...createShot("bad"), "text-sk": "" }] },
      { shots: [createShot("intro"), { ...createShot("bad"), head: { resolution: "1080p" } }] },
      { shots: [createShot("intro"), { ...createShot("bad"), head: { imagePath: "missing.png" } }] }
    ];
    for (const plan of invalidPlans) {
      const mock = server();
      await assert.rejects(generateHeadArtifacts(parameters(directory, mock, { plan })));
      assert.equal(mock.state.requests.length, 0);
    }
    await fs.writeFile(path.join(directory, "invalid.png"), "not an image");
    const mock = server();
    await assert.rejects(generateHeadArtifacts(parameters(directory, mock, { options: { imagePath: "invalid.png" } })), /PNG, JPEG or WebP/);
    const brokenFs = { ...fs, writeFile: async (...args) => {
      if (args[0].includes(".mp4.")) throw new Error("Disk unavailable");
      return fs.writeFile(...args);
    } };
    await assert.rejects(generateHeadArtifacts(parameters(directory, mock, { fsImpl: brokenFs })), /Disk unavailable/);
    assert.equal(mock.state.requests.length, 0);
    assert.deepEqual(await fs.readdir(path.join(directory, "out")), []);
    await fs.mkdir(path.join(directory, "out", "example-intro-sk.mp4"));
    await assert.rejects(generateHeadArtifacts(parameters(directory, mock)), /not a regular file/);
    assert.equal(mock.state.requests.length, 0);
  });
});

test("a plan without head shots makes no API or filesystem calls and needs no API key", async () => {
  const mock = server();
  const result = await generateHeadArtifacts({ scenarioFile: "/example.js", ...mock, log: () => {},
    plan: { shots: [{ ...createShot("manual"), type: "manual", "text-sk": "" }] } });
  assert.deepEqual(result, []);
  assert.equal(mock.state.requests.length, 0);
});

test("generation failure preserves the previous MP4, retains new speech, stops later shots and reports credits", async () => {
  await fixture(async directory => {
    const lines = [];
    const mock = server((url, options) => url.includes("/flows/video/job-") ? Response.json({ id: "job-1", status: "failed", failure_reason: "model_error", error_message: "Provider failed" }) : null);
    await fs.mkdir(path.join(directory, "out"));
    const target = path.join(directory, "out", "example-intro-sk.mp4");
    await fs.writeFile(target, "previous video");
    await assert.rejects(generateHeadArtifacts(parameters(directory, mock, { log: line => lines.push(line),
      plan: { shots: [createShot("intro"), createShot("later")] } })), /generation job-1.*Provider failed/);
    assert.equal(await fs.readFile(target, "utf8"), "previous video");
    assert.equal(mock.state.posts.length, 1);
    assert.equal(mock.state.speech.length, 1);
    assert.match(lines.at(-1), /Credits used: 100/);
    assert.ok((await fs.readdir(path.join(directory, "out"))).every(name => !name.endsWith(".tmp")));
  });
});

test("polling backs off, has a ceiling and reports pending jobs without resubmitting", async () => {
  const waits = [];
  const mock = server((url, options) => url.includes("/flows/video/job-") ? Response.json({ id: "job-1", status: "generating" }) : null);
  let submitted = false;
  const request = { apiKey: "key", shot: { id: "intro", modelId: "creatify-aurora", resolution: "720p" },
    image: inlineReference(PNG, "image/png"), audio: Buffer.from("speech"), ...mock, log: () => {}, timeoutMs: 180_000,
    sleepImpl: async milliseconds => { waits.push(milliseconds); mock.state.elapsed += milliseconds; },
    onSubmitted: () => { submitted = true; }, onSettled: () => { submitted = false; } };
  await assert.rejects(requestHeadVideo(request), /generation job-1.*timed out/);
  assert.deepEqual(waits, [10_000, 20_000, 40_000, 60_000, 50_000]);
  assert.equal(mock.state.posts.length, 1);
  assert.equal(submitted, true);
});

test("HTTP and ambiguous network failures are not retried; invalid downloads are rejected", async () => {
  for (const failure of ["http", "network", "empty", "mime", "insecure"]) {
    const mock = server((url, options) => {
      if (options.method === "POST" && failure === "http") return new Response("Forbidden", { status: 403 });
      if (options.method === "POST" && failure === "network") throw new Error("Connection lost");
      if (url.startsWith("https://media.example/") && failure === "empty") return new Response("", { headers: { "content-type": "video/mp4" } });
      if (url.startsWith("https://media.example/") && failure === "mime") return new Response("HTML", { headers: { "content-type": "text/html" } });
      if (url.includes("/flows/video/job-") && failure === "insecure") return Response.json({ id: "job-1", status: "completed", content_mime_type: "video/mp4", content_url: "http://media.example/result.mp4" });
    });
    await assert.rejects(requestHeadVideo({ apiKey: "key", shot: { id: "intro", modelId: "model", resolution: "720p" },
      image: inlineReference(PNG, "image/png"), audio: Buffer.from("speech"), ...mock, log: () => {} }),
    /HTTP 403|Connection lost|MP4 container|video\/mp4|HTTPS/);
    assert.equal(mock.state.requests.filter(item => item.options.method === "POST").length, 1);
  }
});

test("an unfinished head run prints a provisional summary and a failed billing read never hides the job id", async () => {
  await fixture(async directory => {
    for (const billingUnavailable of [false, true]) {
      const lines = [];
      const mock = server(url => {
        if (url.endsWith("/subscription") && billingUnavailable) return new Response("Missing user_read", { status: 401 });
        if (url.includes("/flows/video/job-")) return Response.json({ id: "job-1", status: "generating" });
      });
      await assert.rejects(generateHeadArtifacts(parameters(directory, mock, { timeoutMs: 15_000, log: line => lines.push(line) })), /generation job-1.*timed out/);
      assert.match(lines.at(-1), /Credits used so far \(generation still pending or uncertain\)/);
      assert.match(lines.at(-1), billingUnavailable ? /unavailable \(HTTP 401: Missing user_read\)/ : /100 \(approximate account delta/);
      assert.equal(mock.state.posts.length, 1);
    }
  });
});

test("validates media sizes and per-shot option contracts", () => {
  assert.equal(getImageMimeType(PNG), "image/png");
  assert.throws(() => inlineReference(Buffer.alloc(0), "audio/mpeg"), /25 MB/);
  assert.throws(() => inlineReference(Buffer.alloc(25 * 1024 * 1024 + 1), "audio/mpeg"), /25 MB/);
  const plan = { shots: [createShot("intro")] };
  for (const head of [null, { language: "en" }, { audio: { apiKey: "secret" } }, { imagePath: "" }, { audio: null }]) {
    plan.shots[0].head = head;
    assert.throws(() => getHeadShots(plan));
  }
});
