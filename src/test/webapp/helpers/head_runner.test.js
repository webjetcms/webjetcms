const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");
const { HEAD_GREP, runHead, validateHeadScenarioSource } = require("./head_runner.js");
const { validateAudioScenarioSource } = require("./audio_runner.js");

const source = `Feature("video.example");
const videoPlan = { language: "en", shots: [
  { id: "intro", type: "head", title: "Intro", durationSeconds: 5, "text-en": "Welcome.",
    head: { resolution: "480p", audio: { voiceId: "voice" } },
    shot: async () => { throw new Error("must not execute"); } }
] };
Scenario("ElevenLabs", ({ I }) => { I.generateAudio(videoPlan); }).tag("@audio");
Scenario("ElevenLabs Head", ({ I }) => { I.generateHead(videoPlan); }).tag("@head");
Scenario("Video", async ({ I }) => { throw new Error("must not execute"); }).tag("@video");`;

test("audio and head preflights coexist and never execute inline callbacks", () => {
  assert.doesNotThrow(() => validateAudioScenarioSource(source));
  assert.doesNotThrow(() => validateHeadScenarioSource(source));
  assert.doesNotThrow(() => validateHeadScenarioSource(source.replace("generateHead(videoPlan)",
    'generateHead(videoPlan, { imagePath: "assets/face.png", modelId: "creatify-aurora", resolution: "720p", audio: { modelId: "eleven_v3" } })')));
  assert.doesNotThrow(() => validateHeadScenarioSource(source.replace('type: "head"', 'type: "manual"').replace('"Welcome."', '""')));
});

test("rejects unsafe or ambiguous head scenarios before starting CodeceptJS", () => {
  const invalid = [
    [source.replace('tag("@head")', 'tag("@video")'), /exactly one scenario tagged @head/],
    [source.replace('tag("@head")', 'tag("@head").tag("@current")'), /only the @head tag/],
    [source.replace('I.generateHead(videoPlan);', 'I.say("bad"); I.generateHead(videoPlan);'), /only one I.generateHead/],
    [source.replace('generateHead(videoPlan)', 'generateHead(`legacy text`)'), /static video plan/],
    [source.replace('generateHead(videoPlan)', 'generateHead(videoPlan, { apiKey: "secret" })'), /Unknown head option/],
    [source.replace('generateHead(videoPlan)', 'generateHead(videoPlan, { audio: { voiceId: "a", voiceId: "b" } })'), /must not be repeated/],
    [source.replace('"480p"', '"1080p"'), /resolution must be/],
    [source.replace('"Welcome."', '""'), /narration must not be empty/],
    [source + '\nBefore(() => {});', /hooks are not allowed/],
    [source + '\nconst sideEffect = require("fs");', /static declarations/],
    [source.replace('"Intro"', 'process.exit()'), /static declarations/],
    [source.replace('"video.example"', '"video.example @head"'), /must not appear in the Feature/]
  ];
  for (const [input, message] of invalid) assert.throws(() => validateHeadScenarioSource(input), message);
});

test("runs only one existing video file in the dedicated head configuration", () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "wj-head-runner-"));
  try {
    fs.mkdirSync(path.join(root, "video"));
    fs.writeFileSync(path.join(root, "video", "example.js"), source);
    fs.writeFileSync(path.join(root, "outside.js"), source);
    fs.symlinkSync(path.join(root, "outside.js"), path.join(root, "video", "link.js"));
    const errors = [];
    const calls = [];
    const options = { webappRoot: root, stderr: line => errors.push(line), spawnSync: (...args) => { calls.push(args); return { status: 0 }; } };
    for (const args of [[], ["video/example.js", "extra"], ["outside.js"], ["video/link.js"], ["video/missing.js"]]) {
      assert.equal(runHead(args, options), 1);
    }
    assert.equal(calls.length, 0);
    assert.equal(runHead(["video/example.js"], options), 0);
    assert.equal(calls.length, 1);
    assert.ok(calls[0][1].includes(path.join(root, "codecept.head.conf.js")));
    assert.equal(calls[0][1].at(-1), HEAD_GREP);
    assert.equal(calls[0][2].env.CODECEPT_HEAD_FILE, fs.realpathSync(path.join(root, "video", "example.js")));
    assert.equal(require("../codecept.head.conf.js").config.helpers.Playwright, undefined);
    assert.equal(runHead(["video/example.js"], { ...options, spawnSync: () => ({ status: 7 }) }), 7);
  } finally { fs.rmSync(root, { recursive: true, force: true }); }
});

test("head helper guards runtime identity and duplicate calls even for plans without heads", async () => {
  const previous = global.codeceptjs;
  global.codeceptjs = require("codeceptjs");
  try {
    const HeadHelper = require("./head_helper.js");
    await assert.rejects(new HeadHelper().generateHead({}), /disabled/);
    const helper = new HeadHelper({ generationEnabled: true });
    await assert.rejects(helper.generateHead({}), /preflight/);
    const scenario = { title: "ElevenLabs Head @head", tags: ["@head"], file: "/tmp/example.js" };
    assert.throws(() => helper._beforeSuite({ tests: [scenario, scenario] }), /exactly one/);
    helper._beforeSuite({ tests: [scenario] });
    helper._test({ ...scenario });
    await assert.rejects(helper.generateHead({}), /preflight/);
    helper._test(scenario);
    const result = await helper.generateHead({ language: "en", shots: [{ id: "manual", type: "manual", title: "Manual", durationSeconds: 2, "text-en": "" }] });
    assert.deepEqual(result, []);
    await assert.rejects(helper.generateHead({}), /Only one/);
  } finally {
    if (previous === undefined) delete global.codeceptjs;
    else global.codeceptjs = previous;
  }
});
