const assert = require("node:assert/strict");
const test = require("node:test");
const { resolveVideoPlan, getPlanNarration, formatShotPlan, getRecordingShots } = require("./feature_video_plan.js");
const { getVideoShot } = require("./video_settings.js");

test.beforeEach(t => {
  const previousShot = process.env.VIDEO_SHOT;
  delete process.env.VIDEO_SHOT;
  t.after(() => {
    if (previousShot === undefined) delete process.env.VIDEO_SHOT;
    else process.env.VIDEO_SHOT = previousShot;
  });
});

function createPlan() {
  return {
    language: "sk",
    shots: [
      { id: "intro", type: "manual", durationSeconds: 5, title: "Intro", "text-sk": "Introduction.", "text-en": "English intro." },
      { id: "edit", type: "auto", durationSeconds: 10, title: "Editing", "text-sk": "Edit the content.", "text-en": "English editing.", shot: async () => {} },
      { id: "preview", type: "auto", durationSeconds: 15, title: "Preview", "text-sk": "Preview the result.", "text-en": "English preview.", shot: async () => {} }
    ]
  };
}

test("normalizes VIDEO_SHOT and rejects malformed IDs", () => {
  assert.equal(getVideoShot({}), "");
  for (const value of ["", "  ", "\t\n"]) assert.equal(getVideoShot({ VIDEO_SHOT: value }), "");
  assert.equal(getVideoShot({ VIDEO_SHOT: " outro \n" }), "outro");
  assert.equal(getVideoShot({ VIDEO_SHOT: "text-editing" }), "text-editing");
  for (const value of ["Outro", "../outro", "outro,edit", "out*", "text_editing", "-outro", "outro--edit"]) {
    assert.throws(() => getVideoShot({ VIDEO_SHOT: value }), /VIDEO_SHOT must be a lowercase hyphenated shot ID/);
  }
});

test("selects one shot without changing the full-plan numbering, timing or narration", () => {
  const plan = createPlan();
  process.env.VIDEO_SHOT = "preview";
  const [shot] = getRecordingShots(plan, "en", getVideoShot());
  assert.deepEqual([shot.id, shot.number, shot.total, shot.startSeconds, shot.endSeconds], ["preview", 3, 3, 15, 30]);
  assert.equal(shot.shot, plan.shots[2].shot);
  assert.equal(shot.narration, "English preview.");
  assert.equal(getPlanNarration(plan), "Introduction.\n\nEdit the content.\n\nPreview the result.");
  assert.match(formatShotPlan(plan), /0:05-0:15 \| AUTO \| Shot 2 \[edit\]/);
  assert.equal(getRecordingShots(plan).length, 3, "Selection must be explicit in the pure plan helper");
  assert.equal(getRecordingShots(plan, undefined, "").length, 3);
});

test("records only the selected shot with setup, preparation, slates, holds and cleanup", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  process.env.VIDEO_SHOT = " preview ";
  const plan = createPlan();
  const events = [];
  const messages = [];
  const unexpected = async () => { throw new Error("Unselected shot callbacks must not execute"); };
  for (const shot of plan.shots) shot.prepare = shot.shot = unexpected;
  plan.shots[2].prepare = async ({ shot }) => events.push(`PREPARE:${shot.id}`);
  plan.shots[2].shot = async ({ shot }) => events.push(`RUN:${shot.id}`);
  await recordVideoPlan({
    say: async message => messages.push(message),
    videoTitle: async shot => events.push(typeof shot === "string" ? shot : `SLATE:${shot.number}/${shot.total}:${shot.id}`),
    wait: async seconds => events.push(`HOLD:${seconds}`)
  }, {
    plan,
    setup: async () => events.push("SETUP"),
    prepare: async shot => events.push(`BASELINE:${shot.id}`),
    cleanup: async shot => events.push(`CLEANUP:${shot.id}`)
  });
  assert.deepEqual(events, [
    "SETUP", "SETUP shot 3/3 preview (15s)", "BASELINE:preview", "PREPARE:preview", "SLATE:3/3:preview",
    "HOLD:2", "RUN:preview", "HOLD:2", "CLEANUP:preview"
  ]);
  assert.deepEqual(messages.filter(message => message.startsWith("Recording ")), ["Recording shot 3/3 preview (15s)"]);
});

test("rejects unknown selections and invalid unselected callbacks before setup", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const unexpected = async () => { throw new Error("Setup and actor steps must not execute"); };
  const I = { say: unexpected, videoTitle: unexpected, wait: unexpected };
  const plan = createPlan();
  process.env.VIDEO_SHOT = "missing";
  await assert.rejects(recordVideoPlan(I, { plan, setup: unexpected }),
    /Unknown video shot: missing\. Available shot IDs: intro, edit, preview\./);
  process.env.VIDEO_SHOT = "preview";
  delete plan.shots[1].shot;
  await assert.rejects(recordVideoPlan(I, { plan, setup: unexpected }), /Missing video action.*edit/);
});

test("selected manual and head shots keep full-plan warning numbers and skip automatic lifecycle", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  process.env.VIDEO_SHOT = "intro";
  for (const type of ["manual", "head"]) {
    const plan = createPlan();
    plan.shots = [plan.shots[1], plan.shots[2], { ...plan.shots[0], type }];
    const unexpected = async () => { throw new Error("Automatic lifecycle must not execute"); };
    for (const shot of plan.shots) shot.prepare = shot.shot = unexpected;
    const events = [];
    const messages = [];
    await recordVideoPlan({
      say: async message => messages.push(message),
      videoTitle: async shot => events.push(`${shot.type}:${shot.number}/${shot.total}:${shot.id}`),
      wait: unexpected
    }, { plan, setup: async () => events.push("SETUP"), prepare: unexpected, cleanup: unexpected });
    assert.deepEqual(events, ["SETUP", `${type}:3/3:intro`]);
    assert.match(messages.find(message => message.startsWith("WARNING:")), /Shot 3\/3 \[intro\]/);
  }
});

test("reordering shots changes narration, timing and automatic callback order together", async () => {
  const plan = createPlan();
  plan.shots = [plan.shots[2], plan.shots[0], plan.shots[1]];
  const original = JSON.stringify(plan);
  const calls = [];
  for (const shot of plan.shots) shot.shot = async () => calls.push(shot.id);
  const recording = getRecordingShots(plan);
  assert.deepEqual(recording.map(shot => shot.id), ["preview", "intro", "edit"]);
  for (const shot of recording.filter(shot => shot.type === "auto")) await shot.shot();
  assert.deepEqual(calls, ["preview", "edit"]);
  assert.deepEqual(resolveVideoPlan(plan).shots.map(shot => [shot.id, shot.number, shot.startSeconds, shot.endSeconds]), [
    ["preview", 1, 0, 15], ["intro", 2, 15, 20], ["edit", 3, 20, 30]
  ]);
  assert.equal(getPlanNarration(plan), "Preview the result.\n\nIntroduction.\n\nEdit the content.");
  assert.equal(getPlanNarration(plan, "en"), "English preview.\n\nEnglish intro.\n\nEnglish editing.");
  assert.match(formatShotPlan(plan), /0:15-0:20 \| MANUAL \| Shot 2 \[intro\]/);
  assert.deepEqual(recording.map(shot => shot.total), [3, 3, 3]);
  assert.equal(JSON.stringify(plan), original, "Derived numbering must not mutate the editable plan");
});

test("requires a translation for every shot and allows explicitly silent shots", () => {
  const plan = createPlan();
  assert.throws(() => getPlanNarration(plan, "cs"), /intro is missing text-cs/);
  plan.shots[1]["text-sk"] = "";
  assert.equal(getPlanNarration(plan), "Introduction.\n\nPreview the result.");
  plan.shots.forEach(shot => shot["text-sk"] = "");
  assert.throws(() => getPlanNarration(plan), /narration must not be empty/);
});

test("head shots retain localized narration and timing while skipping all shot callbacks", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const plan = createPlan();
  const intro = plan.shots[0];
  intro.type = "head";
  intro.notes = "Insert the generated talking-head clip.";
  intro.shot = intro.prepare = async () => { throw new Error("Head callbacks must not execute"); };
  plan.shots = [plan.shots[1], intro, plan.shots[2]];
  assert.equal(getPlanNarration(plan, "en"), "English editing.\n\nEnglish intro.\n\nEnglish preview.");
  assert.deepEqual(resolveVideoPlan(plan, "en").shots.map(shot => [shot.id, shot.number, shot.startSeconds]), [
    ["edit", 1, 0], ["intro", 2, 10], ["preview", 3, 15]
  ]);
  assert.match(formatShotPlan(plan), /0:10-0:15 \| HEAD \| Shot 2 \[intro\]/);
  const events = [];
  const messages = [];
  for (const shot of plan.shots.filter(shot => shot.type === "auto")) shot.shot = async () => events.push(`RUN:${shot.id}`);
  await recordVideoPlan({
    say: async message => messages.push(message),
    wait: async seconds => events.push(`HOLD:${seconds}`),
    videoTitle: async shot => events.push(typeof shot === "string" ? shot : `${shot.type}:${shot.id}:${shot.narration}`)
  }, { plan, language: "en", prepare: async shot => events.push(`PREPARE:${shot.id}`), cleanup: async shot => events.push(`CLEANUP:${shot.id}`) });
  assert.deepEqual(events, [
    "SETUP shot 1/3 edit (10s)", "PREPARE:edit", "auto:edit:English editing.", "HOLD:2", "RUN:edit", "HOLD:2", "CLEANUP:edit",
    "head:intro:English intro.",
    "SETUP shot 3/3 preview (15s)", "PREPARE:preview", "auto:preview:English preview.", "HOLD:2", "RUN:preview", "HOLD:2", "CLEANUP:preview"
  ]);
  assert.ok(messages.includes("WARNING: head video | Shot 2/3 [intro]: Intro (5s)\nEnglish intro.\nInsert the generated talking-head clip."));
});

test("rejects ambiguous metadata and missing actions before recording", () => {
  const invalid = [
    [plan => plan.shots.push({ ...plan.shots[0] }), /Duplicate shot id/],
    [plan => plan.shots[0].type = "auto1", /type must be auto, manual or head/],
    [plan => plan.shots[0].durationSeconds = -1, /positive integer/],
    [plan => plan.language = "../en", /language code/]
  ];
  for (const [mutate, message] of invalid) {
    const plan = createPlan();
    mutate(plan);
    assert.throws(() => resolveVideoPlan(plan), message);
  }
  const plan = createPlan();
  delete plan.shots[2].shot;
  assert.throws(() => getRecordingShots(plan), /Missing video action.*preview/);
});

test("records reordered shots and manual warning slates without running manual callbacks", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const plan = createPlan();
  plan.shots = [plan.shots[2], plan.shots[0], plan.shots[1]];
  plan.shots[1].notes = "Film the opening card separately.";
  const events = [];
  const messages = [];
  const callback = name => async shot => {
    await new Promise(resolve => setImmediate(resolve));
    events.push(`${name}:${shot.id}`);
  };
  const I = {
    say: async message => messages.push(message),
    wait: async seconds => events.push(`HOLD:${seconds}`),
    videoTitle: async shot => events.push(typeof shot === "string" ? shot :
      shot.type === "manual" ? `MANUAL:${shot.id}:${shot.notes}` : `SLATE:${shot.id}`)
  };
  const helpers = { selector: ".content" };
  const context = { helpers, I: "must not override the actor", shot: "must not override metadata" };
  const inlineCallback = name => async ({ I: actor, shot, helpers: receivedHelpers }) => {
    assert.equal(actor, I);
    assert.equal(receivedHelpers, helpers);
    assert.equal(shot.narration, shot["text-en"]);
    await callback(name)(shot);
  };
  for (const shot of plan.shots) shot.shot = inlineCallback("RUN");
  plan.shots[0].prepare = inlineCallback("PREPARE");
  plan.shots[1].prepare = async () => { throw new Error("Manual preparation must not run"); };
  await recordVideoPlan(I, {
    plan,
    context,
    language: "en",
    setup: async () => events.push("LOGIN"),
    prepare: callback("BASELINE"),
    cleanup: callback("CLEANUP")
  });
  assert.deepEqual(events, [
    "LOGIN", "SETUP shot 1/3 preview (15s)", "BASELINE:preview", "PREPARE:preview", "SLATE:preview", "HOLD:2", "RUN:preview", "HOLD:2", "CLEANUP:preview",
    "MANUAL:intro:Film the opening card separately.",
    "SETUP shot 3/3 edit (10s)", "BASELINE:edit", "SLATE:edit", "HOLD:2", "RUN:edit", "HOLD:2", "CLEANUP:edit"
  ]);
  assert.deepEqual(messages.filter(message => message.startsWith("Recording ")), [
    "Recording shot 1/3 preview (15s)",
    "Recording shot 3/3 edit (10s)"
  ], "Progress must use full-plan numbering, including manual shots between automatic shots");
  assert.equal(messages.find(message => message.startsWith("WARNING:")),
    "WARNING: manual steps | Shot 2/3 [intro]: Intro (5s)\nFilm the opening card separately.");
  assert.equal(context.shot, "must not override metadata", "Recording must not mutate caller context");
});

test("records a manual-only plan without automatic preparation, actions or cleanup", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const plan = createPlan();
  plan.shots = [plan.shots[0]];
  const unexpected = async () => { throw new Error("Automatic lifecycle must not run for a manual shot"); };
  plan.shots[0].prepare = unexpected;
  plan.shots[0].shot = unexpected;
  const events = [];
  await recordVideoPlan({
    say: async message => events.push(message),
    videoTitle: async shot => events.push(`MANUAL:${shot.id}`)
  }, { plan, setup: async () => events.push("SETUP"), prepare: unexpected, cleanup: unexpected });
  assert.equal(events[0], "SETUP");
  assert.equal(events.at(-1), "MANUAL:intro");
  assert.ok(events.some(message => message.includes("WARNING: manual steps | Shot 1/1")));
  assert.ok(events.some(message => message.includes("Add filming instructions to this shot's notes.")));
  assert.ok(events.every(message => !message.startsWith("Recording ")));
});

test("injects configured page objects into inline preparation and actions", async t => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const calls = [];
  const I = { say: async () => {}, videoTitle: async () => {}, wait: async () => {} };
  const support = {
    I: "must not override the recording actor",
    DTE: { waitForEditor: async () => calls.push("editor") },
    DT: { waitForLoader: async () => calls.push("loader") },
    Document: { resetPageBuilderMode: async () => calls.push("document") }
  };
  const originalInject = Object.getOwnPropertyDescriptor(globalThis, "inject");
  globalThis.inject = () => support;
  t.after(() => {
    if (originalInject) Object.defineProperty(globalThis, "inject", originalInject);
    else delete globalThis.inject;
  });
  const plan = createPlan();
  plan.shots = [plan.shots[1]];
  const selector = ".editor";
  plan.shots[0].prepare = async ({ I: actor, DTE, Document, selector: receivedSelector }) => {
    assert.equal(actor, I);
    assert.equal(receivedSelector, selector);
    await Document.resetPageBuilderMode();
    await DTE.waitForEditor();
  };
  plan.shots[0].shot = async ({ DT, shot }) => {
    assert.equal(shot.id, "edit");
    await DT.waitForLoader();
  };
  await recordVideoPlan(I, { plan, context: { selector } });
  assert.deepEqual(calls, ["document", "editor", "loader"]);

  calls.length = 0;
  const DTE = { waitForEditor: async () => calls.push("custom editor") };
  await recordVideoPlan(I, { plan, context: { selector, DTE } });
  assert.deepEqual(calls, ["document", "custom editor", "loader"]);
  assert.notEqual(support.DTE, DTE, "Explicit context overrides must not mutate CodeceptJS support objects");
});

test("validates callbacks before setup and stops recording after a failed shot", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const events = [];
  const I = { say: async () => {}, videoTitle: async () => {}, wait: async () => {} };
  const options = {
    plan: createPlan(),
    setup: async () => events.push("setup"),
    cleanup: async () => events.push("cleanup")
  };
  options.plan.shots[1].shot = async () => { throw new Error("shot failed"); };
  delete options.plan.shots[2].shot;
  await assert.rejects(recordVideoPlan(I, options), /Missing video action.*preview/);
  assert.deepEqual(events, []);
  options.plan.shots[2].shot = async () => events.push("preview");
  options.plan.shots[2].prepare = "invalid callback";
  await assert.rejects(recordVideoPlan(I, options), /preview prepare must be a function/);
  assert.deepEqual(events, [], "All inline preparation must be validated before setup");
  delete options.plan.shots[2].prepare;
  await assert.rejects(recordVideoPlan(I, options), /shot failed/);
  assert.deepEqual(events, ["setup"], "A failed shot must not run further shots or mask the original error");
});
