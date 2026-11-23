const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");
const { runVideoPlan } = require("./video_plan_runner.js");
const { readVideoPlanSource } = require("./video_plan_source.js");

function fixture(t, source) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "wj-video-plan-"));
  fs.mkdirSync(path.join(root, "video"));
  fs.writeFileSync(path.join(root, "video", "example.js"), source);
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  return root;
}

function run(webappRoot, argv = ["video/example.js"]) {
  const output = [];
  const errors = [];
  const status = runVideoPlan(argv, { webappRoot, stdout: line => output.push(line), stderr: line => errors.push(line) });
  return { status, output: output.join("\n"), errors };
}

function planSource(plan) {
  return `const videoPlan = ${JSON.stringify(plan)};`;
}

test("prints complete narration and an ordered timeline without executing any scenario code", t => {
  const source = `
throw new Error("Scenario source must never execute");
const videoPlan = {
  language: "sk",
  notes: "General filming notes.",
  shots: [
    { id: "preview", type: "auto", durationSeconds: 15, title: "Preview first", "text-sk": \`Preview narration.\`,
      prepare: async ({ I }) => { await I.amOnPage("https://example.com"); },
      shot: async ({ I }) => { throw new Error("Browser action must never execute"); }, },
    { id: "intro", type: "manual", durationSeconds: 5, title: "Opening card", "text-sk": "Intro narration.", notes: "Film the card separately." },
    { id: "presenter", type: "head", durationSeconds: 7, title: "Presenter", "text-sk": "Presenter narration." },
    { id: "edit", type: "auto", durationSeconds: 10, title: "Edit last", "text-sk": "Editing narration.", "text-en": "Unused translation." },
  ],
};
Scenario("ElevenLabs", ({ I }) => { I.generateAudio(videoPlan); }).tag("@audio");
Scenario("Shot plan", () => { throw new Error("Metadata scenario must never execute"); });
`;
  const result = run(fixture(t, source));
  assert.equal(result.status, 0);
  assert.deepEqual(result.errors, []);
  assert.match(result.output, /Language: sk \| Shots: 4 \| Estimated edited duration: 0:37 \(37s\)/);
  assert.ok(result.output.includes("NARRATION\n=========\n\nPreview narration.\n\nIntro narration.\n\nPresenter narration.\n\nEditing narration."));
  assert.ok(result.output.includes("SHOT PLAN\n========="));
  assert.match(result.output, /0:00-0:15 \| AUTO \| Shot 1 \[preview\] \| Preview first/);
  assert.match(result.output, /0:15-0:20 \| MANUAL \| Shot 2 \[intro\]/);
  assert.match(result.output, /0:20-0:27 \| HEAD \| Shot 3 \[presenter\]/);
  assert.match(result.output, /0:27-0:37 \| AUTO \| Shot 4 \[edit\]/);
  assert.ok(result.output.includes("General filming notes."));
  assert.ok(result.output.includes("Film the card separately."));
  assert.ok(!result.output.includes("Unused translation."));
});

test("previews draft plans without metadata scenarios, media assets or audio-size restrictions", t => {
  const narration = "x".repeat(6000);
  const root = fixture(t, planSource({ language: "en", shots: [{
    id: "draft", type: "head", durationSeconds: 120, title: "Draft narration", "text-en": narration,
    head: { imagePath: "missing.png" }
  }] }));
  const result = run(root);
  assert.equal(result.status, 0);
  assert.match(result.output, /Language: en.*2:00 \(120s\)/);
  assert.match(result.output, /Narration: 1 words \| 6000 characters/);
  assert.ok(result.output.includes(narration));
});

test("shows silent plans without requiring non-empty audio narration", t => {
  const root = fixture(t, planSource({ shots: [{ id: "silent", type: "manual", durationSeconds: 3, title: "Silent card", "text-sk": "" }] }));
  const result = run(root);
  assert.equal(result.status, 0);
  assert.match(result.output, /Narration: 0 words \| 0 characters/);
  assert.ok(result.output.includes("(No spoken narration.)"));
  assert.match(result.output, /0:00-0:03 \| MANUAL \| Shot 1 \[silent\]/);
});

test("rejects invalid or dynamic plan metadata without evaluating it", () => {
  const valid = 'const videoPlan = { shots: [{ id: "intro", type: "manual", durationSeconds: 1, title: "Intro", "text-sk": "Text" }] };';
  const cases = [
    ["const videoPlan = {", /Invalid video plan.*Unexpected token/],
    [valid.replace("const videoPlan", "let videoPlan"), /top-level const videoPlan/],
    [valid.replace('"text-sk": "Text"', '"text-sk": process.exit()'), /static literal data/],
    [valid.replace('title: "Intro"', 'get title() { throw new Error("Must not execute"); }'), /data properties/],
    [valid.replace('title: "Intro"', 'title: "First", title: "Second"'), /must not be repeated/]
  ];
  for (const [source, error] of cases) assert.throws(() => readVideoPlanSource(source), error);
});

test("reports missing translations without printing a partial overview", t => {
  const root = fixture(t, planSource({ language: "en", shots: [{ id: "intro", type: "manual", durationSeconds: 1, title: "Intro", "text-sk": "Text" }] }));
  const result = run(root);
  assert.equal(result.status, 1);
  assert.equal(result.output, "");
  assert.match(result.errors[0], /intro is missing text-en/);
});

test("handles help, invalid arguments and paths outside the video directory", t => {
  const root = fixture(t, "const videoPlan = {};");
  assert.equal(run(root, ["--help"]).status, 0);
  for (const argv of [[], ["video/example.js", "video/second.js"]]) {
    const result = run(root, argv);
    assert.equal(result.status, 1);
    assert.match(result.errors[0], /Usage: npm run video:plan/);
  }
  fs.writeFileSync(path.join(root, "outside.js"), "throw new Error('Must not execute');");
  fs.symlinkSync(path.join(root, "outside.js"), path.join(root, "video", "linked.js"));
  fs.mkdirSync(path.join(root, "video", "directory.js"));
  const cases = [
    ["video/missing.js", /does not exist/],
    ["video/example.txt", /must be a \.js file/],
    ["video/directory.js", /must be a file/],
    ["outside.js", /inside the video directory/],
    ["video/linked.js", /inside the video directory/]
  ];
  for (const [argument, error] of cases) {
    const result = run(root, [argument]);
    assert.equal(result.status, 1);
    assert.match(result.errors[0], error);
    assert.equal(result.output, "");
  }
});
