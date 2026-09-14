const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const previousCodeceptjs = global.codeceptjs;
global.codeceptjs = require("codeceptjs");
const AudioHelper = require("./audio_helper.js");
const {
  DEFAULT_MODEL_ID,
  DEFAULT_VOICE_ID,
  OUTPUT_FORMAT,
  generateAudioArtifacts,
  getAudioArtifactPath,
  getAudioSettings,
  requestSpeechAudio,
  writeFileAtomically
} = AudioHelper;

const ENVIRONMENT_NAMES = [
  "ELEVENLABS_API_KEY",
  "ELEVENLABS_MODEL_ID",
  "ELEVENLABS_VOICE_ID"
];
const originalEnvironment = Object.fromEntries(
  ENVIRONMENT_NAMES.map((name) => [name, process.env[name]])
);
const originalFetch = global.fetch;

test.beforeEach(() => {
  for (const name of ENVIRONMENT_NAMES) delete process.env[name];
  global.fetch = originalFetch;
});

test.after(() => {
  for (const [name, value] of Object.entries(originalEnvironment)) {
    if (value === undefined) delete process.env[name];
    else process.env[name] = value;
  }
  global.fetch = originalFetch;
  if (previousCodeceptjs === undefined) delete global.codeceptjs;
  else global.codeceptjs = previousCodeceptjs;
});

async function withOutputDirectory(callback) {
  const outputDirectory = await fs.mkdtemp(path.join(os.tmpdir(), "wj-audio-helper-"));
  try {
    return await callback(outputDirectory);
  } finally {
    await fs.rm(outputDirectory, { recursive: true, force: true });
  }
}

function mockGenerationFetch(generate) {
  return async (url, options) => url.endsWith('/user/subscription')
    ? Response.json({ character_count: 100, character_limit: 1000 })
    : generate(url, options);
}

function successfulResponse(data = "generated-mp3") {
  return new Response(Buffer.from(data), {
    status: 200,
    headers: { "Content-Type": "audio/mpeg" }
  });
}

function createAudioTest(file, overrides = {}) {
  return {
    file,
    tags: ["@audio"],
    title: "ElevenLabs @audio",
    ...overrides
  };
}

function registerAudioTest(helper, audioTest) {
  helper._beforeSuite({ suites: [], tests: [audioTest] });
  helper._test(audioTest);
}

function createLongPlan() {
  return { language: "sk", shots: ["first", "second", "third"].map((id, index) => ({
    id, type: index === 1 ? "manual" : "auto", title: id, durationSeconds: 10,
    "text-sk": `${id} ${"x".repeat(3000)}`,
    shot: () => { throw new Error("Audio must not execute browser callbacks"); }
  })) };
}

test("generates numbered parts sequentially, logs their exact text and reports the total charge", async t => {
  const lines = [];
  t.mock.method(console, "log", line => lines.push(line));
  await withOutputDirectory(async outputDirectory => {
    process.env.ELEVENLABS_API_KEY = "test-api-key";
    const plan = createLongPlan();
    const expectedPaths = [1, 2, 3].map(part => path.join(outputDirectory, `long-sk-${part}.mp3`));
    let subscriptions = 0;
    const texts = [];
    global.fetch = async (url, options) => {
      if (url.endsWith("/user/subscription")) {
        subscriptions++;
        assert.equal(lines.filter(line => line.startsWith("[ElevenLabs audio] Text to generate:")).length, 3,
          "All part texts must be printed before contacting ElevenLabs");
        return Response.json({ character_count: subscriptions * 100, character_limit: 1000, next_character_count_reset_unix: 9000 });
      }
      const part = texts.length + 1;
      if (part > 1) assert.equal(await fs.readFile(expectedPaths[part - 2], "utf8"), `audio-${part - 1}`,
        "The previous part must be saved before starting the next request");
      texts.push(JSON.parse(options.body).text);
      return new Response(`audio-${part}`, { headers: { "Content-Type": "audio/mpeg", "character-cost": String(part * 10) } });
    };
    const helper = new AudioHelper({ generationEnabled: true, featureVideoDirectory: outputDirectory });
    const scenario = createAudioTest("/project/video/long.js");
    registerAudioTest(helper, scenario);
    assert.deepEqual(await helper.generateAudio(plan), expectedPaths);
    assert.deepEqual(texts, plan.shots.map(shot => shot["text-sk"]));
    assert.deepEqual(scenario.artifacts, Object.fromEntries(expectedPaths.map((file, index) => [`audio-${index + 1}`, file])));
    assert.deepEqual((await fs.readdir(outputDirectory)).sort(), expectedPaths.map(file => path.basename(file)));
    const reports = lines.filter(line => line.includes("Credits used:"));
    assert.equal(reports.length, 1);
    assert.match(reports[0], /Credits used: 60 \(TTS response\)/);
    assert.equal(subscriptions, 2);
  });
});

test("rejects an oversized later shot before any API call or output write", async () => {
  await withOutputDirectory(async outputDirectory => {
    process.env.ELEVENLABS_API_KEY = "test-api-key";
    const plan = createLongPlan();
    plan.shots[2]["text-sk"] = "x".repeat(5001);
    let requests = 0;
    global.fetch = async () => { requests++; throw new Error("No API calls expected"); };
    const helper = new AudioHelper({ generationEnabled: true, featureVideoDirectory: outputDirectory });
    registerAudioTest(helper, createAudioTest("/project/video/long.js"));
    await assert.rejects(helper.generateAudio(plan), /Shot third has 5001 characters/);
    assert.equal(requests, 0);
    assert.deepEqual(await fs.readdir(outputDirectory), []);
  });
});

test("checks every part output before spending credits and removes all reservations on failure", async () => {
  await withOutputDirectory(async outputDirectory => {
    const first = path.join(outputDirectory, "part-1.mp3");
    const second = path.join(outputDirectory, "part-2.mp3");
    await fs.writeFile(first, "previous audio");
    await fs.mkdir(second);
    let requests = 0;
    await assert.rejects(generateAudioArtifacts({
      chunks: [first, second].map(targetPath => ({ targetPath, text: "Narration" })),
      apiKey: "test-api-key", modelId: DEFAULT_MODEL_ID, voiceId: DEFAULT_VOICE_ID, creditLabel: "audio",
      fetchImpl: async () => { requests++; throw new Error("No API calls expected"); }
    }), /Unable to prepare generated audio output.*not a regular file/);
    assert.equal(requests, 0);
    assert.equal(await fs.readFile(first, "utf8"), "previous audio");
    assert.deepEqual((await fs.readdir(outputDirectory)).sort(), ["part-1.mp3", "part-2.mp3"]);
  });
});

test("stops at a failed part without retrying and preserves completed and previous outputs", async t => {
  const lines = [];
  t.mock.method(console, "log", line => lines.push(line));
  await withOutputDirectory(async outputDirectory => {
    process.env.ELEVENLABS_API_KEY = "test-api-key";
    const paths = [1, 2, 3].map(part => path.join(outputDirectory, `long-sk-${part}.mp3`));
    for (const file of paths) await fs.writeFile(file, "previous audio");
    let requests = 0;
    global.fetch = mockGenerationFetch(async () => ++requests === 1
      ? successfulResponse("new first part") : new Response("Generation failed", { status: 500 }));
    const helper = new AudioHelper({ generationEnabled: true, featureVideoDirectory: outputDirectory });
    registerAudioTest(helper, createAudioTest("/project/video/long.js"));
    await assert.rejects(helper.generateAudio(createLongPlan()), /Audio part 2\/3 \(long-sk-2\.mp3\).*HTTP 500/);
    assert.equal(requests, 2);
    assert.deepEqual(await Promise.all(paths.map(file => fs.readFile(file, "utf8"))), ["new first part", "previous audio", "previous audio"]);
    assert.deepEqual((await fs.readdir(outputDirectory)).sort(), paths.map(file => path.basename(file)));
    assert.equal(lines.filter(line => line.includes("Credits used:")).length, 1);
  });
});

test("uses an approximate total if any successful part lacks a charge header", async t => {
  const lines = [];
  t.mock.method(console, "log", line => lines.push(line));
  await withOutputDirectory(async outputDirectory => {
    let subscriptions = 0;
    let requests = 0;
    await generateAudioArtifacts({
      chunks: [1, 2].map(part => ({ targetPath: path.join(outputDirectory, `part-${part}.mp3`), text: "Narration" })),
      apiKey: "test-api-key", modelId: DEFAULT_MODEL_ID, voiceId: DEFAULT_VOICE_ID, creditLabel: "audio",
      fetchImpl: async url => {
        if (url.endsWith("/user/subscription")) return Response.json({ character_count: ++subscriptions * 100, character_limit: 1000, next_character_count_reset_unix: 9000 });
        return ++requests === 1
          ? new Response("audio", { headers: { "Content-Type": "audio/mpeg", "character-cost": "7" } })
          : successfulResponse();
      }
    });
    assert.match(lines.find(line => line.includes("Credits used:")), /Credits used: 100 \(approximate account delta/);
  });
});

test("generates the default MP3 request and registers the scenario-file artifact", async t => {
  const lines = [];
  t.mock.method(console, "log", line => lines.push(line));
  await withOutputDirectory(async (outputDirectory) => {
    assert.equal(DEFAULT_MODEL_ID, "eleven_v3");
    process.env.ELEVENLABS_API_KEY = "  test-api-key  ";
    process.env.ELEVENLABS_MODEL_ID = "   ";
    process.env.ELEVENLABS_VOICE_ID = "";
    const requests = [];
    global.fetch = mockGenerationFetch(async (url, options) => {
      assert.ok(lines.includes(`[ElevenLabs audio] Text to generate:\n${JSON.parse(options.body).text}\n`),
        "The complete normalized narration must be logged before the TTS request");
      requests.push({ url, options });
      return successfulResponse();
    });

    const scenario = createAudioTest(
      path.join("project", "video", "293-config-jstree-view.js")
    );
    const helper = new AudioHelper({
      generationEnabled: true,
      featureVideoDirectory: outputDirectory
    });
    registerAudioTest(helper, scenario);

    const result = await helper.generateAudio("\r\nFirst line.\r\nSecond line.\r\n");
    const expectedPath = path.join(
      outputDirectory,
      "293-config-jstree-view-1.mp3"
    );

    assert.deepEqual(result, [expectedPath]);
    assert.equal(await fs.readFile(expectedPath, "utf8"), "generated-mp3");
    assert.equal(scenario.artifacts["audio-1"], expectedPath);
    assert.equal(requests.length, 1, "Audio generation must make one HTTP request");

    const requestUrl = new URL(requests[0].url);
    assert.equal(requestUrl.pathname, `/v1/text-to-speech/${DEFAULT_VOICE_ID}`);
    assert.equal(requestUrl.searchParams.get("output_format"), OUTPUT_FORMAT);
    assert.equal(requests[0].options.method, "POST");
    assert.equal(requests[0].options.headers["xi-api-key"], "test-api-key");
    assert.equal(requests[0].options.headers.Accept, "audio/mpeg");
    assert.equal(requests[0].options.redirect, "error");
    assert.ok(requests[0].options.signal instanceof AbortSignal);
    assert.deepEqual(JSON.parse(requests[0].options.body), {
      text: "First line.\nSecond line.",
      model_id: DEFAULT_MODEL_ID
    });
    assert.equal(
      Object.hasOwn(JSON.parse(requests[0].options.body), "voice_settings"),
      false,
      "The request must leave ElevenLabs voice settings unchanged"
    );
  });
});

test("resolves the default audio artifact directly below docs/feature-video", () => {
  const scenario = createAudioTest(
    path.join("project", "video", "293-config-jstree-view.js")
  );

  assert.equal(
    getAudioArtifactPath(scenario),
    path.resolve(__dirname, "../../../../docs/feature-video/293-config-jstree-view-1.mp3")
  );
});

test("generates localized plan narration in shot order without executing inline callbacks", async t => {
  const lines = [];
  t.mock.method(console, "log", line => lines.push(line));
  await withOutputDirectory(async outputDirectory => {
    process.env.ELEVENLABS_API_KEY = "test-api-key";
    const requests = [];
    global.fetch = mockGenerationFetch(async (url, options) => {
      assert.ok(lines.includes(`[ElevenLabs audio] Text to generate:\n${JSON.parse(options.body).text}\n`),
        "The selected language and all shot texts must be logged before the TTS request");
      requests.push(JSON.parse(options.body));
      return successfulResponse();
    });
    const plan = { language: "sk", shots: [
      { id: "second", type: "auto", durationSeconds: 5, title: "Not spoken", "text-sk": "Second.", "text-en": "English second." },
      { id: "first", type: "manual", durationSeconds: 3, title: "Not spoken either", "text-sk": "First.", "text-en": "English first." }
    ] };
    let callbackCalls = 0;
    for (const shot of plan.shots) {
      shot.shot = async () => { callbackCalls++; };
      shot.prepare = async () => { callbackCalls++; };
    }
    for (const language of ["sk", "en"]) {
      const helper = new AudioHelper({ generationEnabled: true, featureVideoDirectory: outputDirectory });
      registerAudioTest(helper, createAudioTest("/project/video/plan.js"));
      const result = await helper.generateAudio(plan, { language });
      assert.deepEqual(result, [path.join(outputDirectory, `plan-${language}-1.mp3`)]);
      assert.equal(await fs.readFile(result[0], "utf8"), "generated-mp3");
    }
    assert.deepEqual(requests.map(request => request.text), ["Second.\n\nFirst.", "English second.\n\nEnglish first."]);
    assert.deepEqual((await fs.readdir(outputDirectory)).sort(), ["plan-en-1.mp3", "plan-sk-1.mp3"]);
    const invalidHelper = new AudioHelper({ generationEnabled: true, featureVideoDirectory: outputDirectory });
    registerAudioTest(invalidHelper, createAudioTest("/project/video/plan.js"));
    await assert.rejects(invalidHelper.generateAudio(plan, { language: "cs" }), /missing text-cs/);
    assert.equal(requests.length, 2, "Missing translations must fail before the paid API call");
    assert.equal(callbackCalls, 0, "Audio generation must never execute browser callbacks");
    assert.equal(lines.filter(line => line.startsWith("[ElevenLabs audio] Text to generate:")).length, 2,
      "Invalid narration must fail before logging a generation request");
  });
});

test("prefers helper overrides over environment settings", async () => {
  await withOutputDirectory(async (outputDirectory) => {
    process.env.ELEVENLABS_API_KEY = "test-api-key";
    process.env.ELEVENLABS_MODEL_ID = "environment-model";
    process.env.ELEVENLABS_VOICE_ID = "environment-voice";
    let request;
    global.fetch = mockGenerationFetch(async (url, options) => {
      request = { url, options };
      return successfulResponse();
    });

    const helper = new AudioHelper({
      generationEnabled: true,
      featureVideoDirectory: outputDirectory
    });
    registerAudioTest(helper, createAudioTest("/project/video/voice-over.js"));
    await helper.generateAudio("Narration", {
      modelId: "  explicit-model  ",
      voiceId: " explicit/voice "
    });

    assert.equal(new URL(request.url).pathname, "/v1/text-to-speech/explicit%2Fvoice");
    assert.deepEqual(JSON.parse(request.options.body), {
      text: "Narration",
      model_id: "explicit-model"
    });
  });
});

test("uses non-empty environment settings before defaults", () => {
  process.env.ELEVENLABS_MODEL_ID = " environment-model ";
  process.env.ELEVENLABS_VOICE_ID = " environment-voice ";

  assert.deepEqual(getAudioSettings(), {
    modelId: "environment-model",
    voiceId: "environment-voice"
  });
  assert.throws(
    () => getAudioSettings({ modelId: " " }),
    /modelId must be a non-empty string/
  );
});

test("requires the API key before making a request and guards the complete run to one call", async () => {
  await withOutputDirectory(async () => {
    let requestCount = 0;
    global.fetch = mockGenerationFetch(async () => {
      requestCount++;
      return successfulResponse();
    });

    const helper = new AudioHelper({ generationEnabled: true });
    registerAudioTest(helper, createAudioTest("/project/video/missing-key.js"));
    await assert.rejects(
      helper.generateAudio("Narration"),
      /ELEVENLABS_API_KEY must be set/
    );
    await assert.rejects(
      helper.generateAudio("Narration"),
      /Only one generateAudio call is allowed per audio run/
    );
    assert.equal(requestCount, 0, "A missing API key must fail before the HTTP request");
  });
});

test("keeps generation disabled unless the dedicated audio configuration enables it", async () => {
  const helper = new AudioHelper();
  helper._test(createAudioTest("/project/video/disabled.js"));

  await assert.rejects(
    helper.generateAudio("Narration"),
    /Audio generation is disabled/
  );
});

test("rejects a different runtime scenario before making a request", async () => {
  process.env.ELEVENLABS_API_KEY = "test-api-key";
  let requestCount = 0;
  global.fetch = mockGenerationFetch(async () => {
    requestCount++;
    return successfulResponse();
  });

  const helper = new AudioHelper({ generationEnabled: true });
  const wrongTest = createAudioTest("/project/video/wrong.js", {
    title: "Wrong @audio"
  });

  assert.throws(
    () => helper._beforeSuite({ suites: [], tests: [wrongTest] }),
    /allowed only in Scenario\("ElevenLabs"\) with the @audio tag/
  );
  assert.equal(requestCount, 0, "A different runtime scenario must not call ElevenLabs");
});

test("rejects duplicate registered audio scenarios before making a request", () => {
  let requestCount = 0;
  global.fetch = mockGenerationFetch(async () => {
    requestCount++;
    return successfulResponse();
  });

  const helper = new AudioHelper({ generationEnabled: true });
  const suite = {
    suites: [{
      suites: [],
      tests: [
        createAudioTest("/project/video/first.js"),
        createAudioTest("/project/video/second.js")
      ]
    }],
    tests: []
  };

  assert.throws(
    () => helper._beforeSuite(suite),
    /Expected exactly one registered @audio scenario, found 2/
  );
  assert.equal(requestCount, 0, "Duplicate audio scenarios must fail before ElevenLabs");
});

test("reports API statuses and plain-text errors without retrying", async () => {
  const cases = [
    { status: 401, body: JSON.stringify({ detail: { message: "Invalid API key" } }), detail: "Invalid API key" },
    { status: 422, body: "Unsupported model", detail: "Unsupported model" },
    { status: 429, body: "", detail: "HTTP 429" }
  ];

  for (const item of cases) {
    let requestCount = 0;
    const fetchImpl = async () => {
      requestCount++;
      return new Response(item.body, { status: item.status });
    };

    await assert.rejects(
      requestSpeechAudio({
        apiKey: "test-api-key",
        text: "Narration",
        modelId: DEFAULT_MODEL_ID,
        voiceId: DEFAULT_VOICE_ID,
        fetchImpl
      }),
      new RegExp(item.detail)
    );
    assert.equal(requestCount, 1, `HTTP ${item.status} must not be retried`);
  }
});

test("rejects a successful response that is not MPEG audio", async () => {
  await assert.rejects(
    requestSpeechAudio({
      apiKey: "test-api-key",
      text: "Narration",
      modelId: DEFAULT_MODEL_ID,
      voiceId: DEFAULT_VOICE_ID,
      fetchImpl: async () => new Response(JSON.stringify({ message: "proxy response" }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    }),
    /unexpected Content-Type \(application\/json\)/
  );
});

test("allows long narration requests beyond two minutes and aborts after ten minutes", async t => {
  t.mock.timers.enable({ apis: ["setTimeout"] });
  let signal;
  let requestCount = 0;
  const request = requestSpeechAudio({
    apiKey: "test-api-key",
    text: "Long narration",
    modelId: DEFAULT_MODEL_ID,
    voiceId: DEFAULT_VOICE_ID,
    fetchImpl: async (url, options) => {
      requestCount++;
      signal = options.signal;
      return new Promise((resolve, reject) => {
        signal.addEventListener("abort", () => reject(new Error("aborted")), { once: true });
      });
    }
  });
  t.mock.timers.tick(120_000);
  assert.equal(signal.aborted, false, "Long narration must survive the previous two-minute limit");
  t.mock.timers.tick(479_999);
  assert.equal(signal.aborted, false, "The request must remain active until the ten-minute deadline");
  t.mock.timers.tick(1);
  await assert.rejects(request, /timed out after 600 seconds/);
  assert.equal(requestCount, 1, "Timed-out generation must not make another paid request");
});

test("reports network failures and request timeouts without retrying", async () => {
  let networkRequestCount = 0;
  await assert.rejects(
    requestSpeechAudio({
      apiKey: "test-api-key",
      text: "Narration",
      modelId: DEFAULT_MODEL_ID,
      voiceId: DEFAULT_VOICE_ID,
      fetchImpl: async () => {
        networkRequestCount++;
        throw new Error("socket unavailable");
      }
    }),
    /request failed: socket unavailable/
  );
  assert.equal(networkRequestCount, 1);

  let timeoutRequestCount = 0;
  await assert.rejects(
    requestSpeechAudio({
      apiKey: "test-api-key",
      text: "Narration",
      modelId: DEFAULT_MODEL_ID,
      voiceId: DEFAULT_VOICE_ID,
      timeoutMs: 5,
      fetchImpl: async (url, options) => {
        timeoutRequestCount++;
        return new Promise((resolve, reject) => {
          const keepAlive = setTimeout(() => reject(new Error("timeout test did not abort")), 1000);
          options.signal.addEventListener("abort", () => {
            clearTimeout(keepAlive);
            const error = new Error("aborted");
            error.name = "AbortError";
            reject(error);
          }, { once: true });
        });
      }
    }),
    /timed out after 0.005 seconds/
  );
  assert.equal(timeoutRequestCount, 1);
});

test("atomically replaces a previous MP3 after a successful write", async () => {
  await withOutputDirectory(async (outputDirectory) => {
    const targetPath = path.join(outputDirectory, "videos", "scenario.mp3");
    await fs.mkdir(path.dirname(targetPath), { recursive: true });
    await fs.writeFile(targetPath, "previous-audio");

    await writeFileAtomically(targetPath, Buffer.from("new-audio"), {
      suffix: "test"
    });

    assert.equal(await fs.readFile(targetPath, "utf8"), "new-audio");
    await assert.rejects(
      fs.access(`${targetPath}.test.tmp`),
      (error) => error.code === "ENOENT"
    );
  });
});

test("checks the output path before making a paid API request", async () => {
  let requestCount = 0;
  const unavailableFileSystem = {
    mkdir: async () => {
      const error = new Error("permission denied");
      error.code = "EACCES";
      throw error;
    }
  };

  await assert.rejects(
    generateAudioArtifacts({
      chunks: [{ targetPath: "/unwritable/videos/scenario-1.mp3", text: "Narration" }],
      apiKey: "test-api-key",
      modelId: DEFAULT_MODEL_ID,
      voiceId: DEFAULT_VOICE_ID,
      fsImpl: unavailableFileSystem,
      fetchImpl: async () => {
        requestCount++;
        return successfulResponse();
      }
    }),
    /Unable to prepare generated audio output: permission denied/
  );
  assert.equal(requestCount, 0, "A local output failure must happen before the paid API request");
});

test("removes the reserved temporary file and preserves the previous MP3 after an API failure", async () => {
  await withOutputDirectory(async (outputDirectory) => {
    const videoDirectory = path.join(outputDirectory, "videos");
    const targetPath = path.join(videoDirectory, "scenario.mp3");
    await fs.mkdir(videoDirectory, { recursive: true });
    await fs.writeFile(targetPath, "previous-audio");

    await assert.rejects(
      generateAudioArtifacts({
        chunks: [{ targetPath, text: "Narration" }],
        apiKey: "test-api-key",
        modelId: DEFAULT_MODEL_ID,
        voiceId: DEFAULT_VOICE_ID,
        fetchImpl: async () => new Response("unauthorized", { status: 401 })
      }),
      /HTTP 401/
    );

    assert.equal(await fs.readFile(targetPath, "utf8"), "previous-audio");
    assert.deepEqual(await fs.readdir(videoDirectory), ["scenario.mp3"]);
  });
});

test("preserves the previous MP3 and removes the temporary file after a disk failure", async () => {
  await withOutputDirectory(async (outputDirectory) => {
    const targetPath = path.join(outputDirectory, "videos", "scenario.mp3");
    await fs.mkdir(path.dirname(targetPath), { recursive: true });
    await fs.writeFile(targetPath, "previous-audio");

    const failingFileSystem = {
      mkdir: fs.mkdir,
      writeFile: fs.writeFile,
      rename: async () => {
        throw new Error("simulated disk failure");
      },
      rm: fs.rm
    };

    await assert.rejects(
      writeFileAtomically(targetPath, Buffer.from("new-audio"), {
        fsImpl: failingFileSystem,
        suffix: "test"
      }),
      /simulated disk failure/
    );
    assert.equal(await fs.readFile(targetPath, "utf8"), "previous-audio");
    await assert.rejects(
      fs.access(`${targetPath}.test.tmp`),
      (error) => error.code === "ENOENT"
    );
  });
});
