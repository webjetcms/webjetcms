const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const {
  AUDIO_GREP,
  runAudio,
  validateAudioScenarioSource
} = require("./audio_runner.js");

const VALID_AUDIO_METADATA = `
Scenario("ElevenLabs", ({ I }) => {
  I.generateAudio(\`
Valid narration.
\`);
}).tag("@audio");
`;
const VALID_AUDIO_SCENARIO = `Feature("video.scenario");\n${VALID_AUDIO_METADATA}`;

function withFeature(source) {
  return `Feature("video.invalid");\n${source}`;
}

function createWebappFixture() {
  const webappRoot = fs.mkdtempSync(path.join(os.tmpdir(), "wj-audio-runner-"));
  const videoDirectory = path.join(webappRoot, "video");
  fs.mkdirSync(videoDirectory);
  fs.writeFileSync(path.join(videoDirectory, "scenario.js"), VALID_AUDIO_SCENARIO);
  fs.writeFileSync(path.join(videoDirectory, "invalid.js"), "Feature(\"video.invalid\");");
  fs.writeFileSync(path.join(videoDirectory, "scenario.txt"), "fixture");
  fs.mkdirSync(path.join(videoDirectory, "directory.js"));
  fs.writeFileSync(path.join(webappRoot, "outside.js"), "// fixture");
  return webappRoot;
}

function runWithFixture(webappRoot, argv, overrides = {}) {
  const errors = [];
  let spawnCalls = 0;
  let spawnArguments;
  const status = runAudio(argv, {
    webappRoot,
    codeceptBin: path.join(webappRoot, "node_modules", "codeceptjs", "bin", "codecept.js"),
    stderr: (message) => errors.push(message),
    spawnSync: (...args) => {
      spawnCalls++;
      spawnArguments = args;
      return { status: 0 };
    },
    ...overrides
  });
  return { errors, spawnArguments, spawnCalls, status };
}

test("runs local CodeceptJS for exactly one validated audio scenario", () => {
  const webappRoot = createWebappFixture();
  try {
    const result = runWithFixture(webappRoot, ["video/scenario.js"]);
    const scenarioPath = fs.realpathSync(path.join(webappRoot, "video", "scenario.js"));

    assert.equal(result.status, 0);
    assert.equal(result.spawnCalls, 1);
    assert.equal(result.spawnArguments[0], process.execPath);
    assert.deepEqual(result.spawnArguments[1], [
      path.join(webappRoot, "node_modules", "codeceptjs", "bin", "codecept.js"),
      "run",
      "-c",
      path.join(webappRoot, "codecept.audio.conf.js"),
      "--steps",
      "--grep",
      AUDIO_GREP
    ]);
    assert.equal(result.spawnArguments[2].cwd, webappRoot);
    assert.equal(result.spawnArguments[2].env.CODECEPT_AUDIO_FILE, scenarioPath);
    assert.equal(result.spawnArguments[2].stdio, "inherit");
    assert.deepEqual(result.errors, []);
  } finally {
    fs.rmSync(webappRoot, { recursive: true, force: true });
  }
});

test("rejects missing and multiple scenario arguments before starting CodeceptJS", () => {
  const webappRoot = createWebappFixture();
  try {
    const missing = runWithFixture(webappRoot, []);
    const multiple = runWithFixture(webappRoot, ["video/scenario.js", "video/other.js"]);

    assert.equal(missing.status, 1);
    assert.equal(multiple.status, 1);
    assert.equal(missing.spawnCalls, 0);
    assert.equal(multiple.spawnCalls, 0);
    assert.match(missing.errors[0], /Usage: npm run audio/);
    assert.match(multiple.errors[0], /Usage: npm run audio/);
  } finally {
    fs.rmSync(webappRoot, { recursive: true, force: true });
  }
});

test("rejects missing, non-JavaScript, non-file, and outside paths", () => {
  const webappRoot = createWebappFixture();
  try {
    const cases = [
      { argument: "video/missing.js", message: /does not exist/ },
      { argument: "video/scenario.txt", message: /must be a \.js file/ },
      { argument: "video/directory.js", message: /must be a file/ },
      { argument: "outside.js", message: /inside the video directory/ }
    ];

    for (const item of cases) {
      const result = runWithFixture(webappRoot, [item.argument]);
      assert.equal(result.status, 1, item.argument);
      assert.equal(result.spawnCalls, 0, item.argument);
      assert.match(result.errors[0], item.message);
    }
  } finally {
    fs.rmSync(webappRoot, { recursive: true, force: true });
  }
});

test("rejects a video-directory symlink that resolves outside the directory", () => {
  const webappRoot = createWebappFixture();
  try {
    fs.symlinkSync(
      path.join(webappRoot, "outside.js"),
      path.join(webappRoot, "video", "linked.js")
    );

    const result = runWithFixture(webappRoot, ["video/linked.js"]);
    assert.equal(result.status, 1);
    assert.equal(result.spawnCalls, 0);
    assert.match(result.errors[0], /inside the video directory/);
  } finally {
    fs.rmSync(webappRoot, { recursive: true, force: true });
  }
});

test("rejects an invalid audio scenario before starting CodeceptJS", () => {
  const webappRoot = createWebappFixture();
  try {
    const result = runWithFixture(webappRoot, ["video/invalid.js"]);

    assert.equal(result.status, 1);
    assert.equal(result.spawnCalls, 0);
    assert.match(result.errors[0], /expected exactly one Scenario\("ElevenLabs"\)/);
  } finally {
    fs.rmSync(webappRoot, { recursive: true, force: true });
  }
});

test("validates exactly one static ElevenLabs audio scenario", () => {
  const validOverride = VALID_AUDIO_SCENARIO.replace(
    "Valid narration.\n\`);",
    "Valid narration.\n\`, { modelId: \"eleven_v3\", voiceId: \"voice-id\" });"
  );
  assert.doesNotThrow(() => validateAudioScenarioSource(validOverride, "valid.js"));

  const cases = [
    {
      source: withFeature(
        `Scenario("Other", ({ I }) => { I.generateAudio(\`Text\`); }).tag("@audio");`
      ),
      message: /exactly one Scenario\("ElevenLabs"\), found 0/
    },
    {
      source: `${VALID_AUDIO_SCENARIO}\n${VALID_AUDIO_METADATA}`,
      message: /exactly one Scenario\("ElevenLabs"\), found 2/
    },
    {
      source: withFeature(
        `Scenario("ElevenLabs", ({ I }) => { I.generateAudio(\`Text\`); });`
      ),
      message: /exactly one scenario tagged @audio, found 0/
    },
    {
      source: `${VALID_AUDIO_SCENARIO}\nScenario("Other", ({ I }) => { I.say("Text"); }).tag("@audio");`,
      message: /exactly one scenario tagged @audio, found 2/
    },
    {
      source: withFeature(`
        Scenario("ElevenLabs", ({ I }) => { I.generateAudio(\`Text\`); });
        Scenario("Other", ({ I }) => { I.say("Text"); }).tag("@audio");
      `),
      message: /must be the scenario tagged @audio/
    },
    {
      source: withFeature(`Scenario("ElevenLabs", ({ I }) => {
        I.generateAudio(\`First\`);
        I.generateAudio(\`Second\`);
      }).tag("@audio");`),
      message: /must contain only one I\.generateAudio call/
    },
    {
      source: withFeature(
        `Scenario("ElevenLabs", ({ I }) => { I.generateAudio(\`Text \${value}\`); }).tag("@audio");`
      ),
      message: /without interpolation/
    },
    {
      source: `
        Before(({ login }) => login("admin"));
        ${VALID_AUDIO_SCENARIO}
      `,
      message: /Before hooks are not allowed/
    },
    {
      source: VALID_AUDIO_SCENARIO.replace(
        `Feature("video.scenario")`,
        `Feature("video.scenario @audio")`
      ),
      message: /@audio must not appear in the Feature title or tags/
    },
    {
      source: `${VALID_AUDIO_SCENARIO}\nScenario("Other @audio", ({ I }) => { I.say("Text"); });`,
      message: /@audio must not appear in a Scenario title/
    },
    {
      source: VALID_AUDIO_SCENARIO.replace(
        `Feature("video.scenario")`,
        `Feature("video.scenario").tag("@audio")`
      ),
      message: /@audio must not appear in the Feature title or tags/
    },
    {
      source: `${VALID_AUDIO_SCENARIO}\nScenario.only("ElevenLabs", ({ I }) => {
        I.generateAudio(\`Wrong narration\`);
      }).tag("@audio");`,
      message: /Only direct Feature and Scenario declarations are allowed/
    },
    {
      source: `${VALID_AUDIO_SCENARIO}\nData(["wrong"]).Scenario("Wrong", ({ I }) => {
        I.generateAudio(\`Wrong narration\`);
      }).tag("@audio");`,
      message: /Only direct Feature and Scenario declarations are allowed/
    },
    {
      source: `${VALID_AUDIO_SCENARIO}\nif (true) {
        Scenario.only("ElevenLabs", ({ I }) => { I.generateAudio(\`Wrong\`); }).tag("@audio");
      }`,
      message: /only static declarations and direct Feature or Scenario calls/
    },
    {
      source: `${VALID_AUDIO_SCENARIO}\nconst duplicate = Scenario.only(
        "ElevenLabs", ({ I }) => { I.generateAudio(\`Wrong\`); }
      ).tag("@audio");`,
      message: /only static declarations and direct Feature or Scenario calls/
    },
    {
      source: `Scenario("ElevenLabs", ({ I }) => {`,
      message: /JavaScript parsing failed/
    }
  ];

  for (const item of cases) {
    assert.throws(
      () => validateAudioScenarioSource(item.source, "invalid.js"),
      item.message
    );
  }
});

test("preserves the CodeceptJS exit status and reports launch failures", () => {
  const webappRoot = createWebappFixture();
  try {
    const failedRun = runWithFixture(webappRoot, ["video/scenario.js"], {
      spawnSync: () => ({ status: 7 })
    });
    const launchFailure = runWithFixture(webappRoot, ["video/scenario.js"], {
      spawnSync: () => ({ error: new Error("spawn failed"), status: null })
    });

    assert.equal(failedRun.status, 7);
    assert.equal(launchFailure.status, 1);
    assert.match(launchFailure.errors[0], /Unable to start CodeceptJS: spawn failed/);
  } finally {
    fs.rmSync(webappRoot, { recursive: true, force: true });
  }
});
