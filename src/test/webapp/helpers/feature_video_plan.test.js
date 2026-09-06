const assert = require("node:assert/strict");
const test = require("node:test");
const { resolveVideoPlan, getPlanNarration, formatShotPlan, getRecordingShots } = require("./feature_video_plan.js");

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

test("rejects ambiguous metadata and missing actions before recording", () => {
  const invalid = [
    [plan => plan.shots.push({ ...plan.shots[0] }), /Duplicate shot id/],
    [plan => plan.shots[0].type = "auto1", /type must be auto or manual/],
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
    "LOGIN", "SETUP shot 1/2 preview (15s)", "BASELINE:preview", "PREPARE:preview", "SLATE:preview", "RUN:preview", "CLEANUP:preview",
    "MANUAL:intro:Film the opening card separately.",
    "SETUP shot 2/2 edit (10s)", "BASELINE:edit", "SLATE:edit", "RUN:edit", "CLEANUP:edit"
  ]);
  assert.deepEqual(messages.filter(message => message.startsWith("Recording ")), [
    "Recording shot 1/2 preview (15s)",
    "Recording shot 2/2 edit (10s)"
  ], "Progress must count only automatic shots, even with manual shots between them");
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

test("validates callbacks before setup and stops recording after a failed shot", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const events = [];
  const I = { say: async () => {}, videoTitle: async () => {} };
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
