const assert = require("node:assert/strict");
const test = require("node:test");
const { resolveVideoPlan, getPlanNarration, formatShotPlan, getRecordingShots } = require("./feature_video_plan.js");

function createPlan() {
  return {
    language: "sk",
    shots: [
      { id: "intro", type: "manual", durationSeconds: 5, title: "Intro", "text-sk": "Introduction.", "text-en": "English intro." },
      { id: "edit", type: "auto", durationSeconds: 10, title: "Editing", "text-sk": "Edit the content.", "text-en": "English editing." },
      { id: "preview", type: "auto", durationSeconds: 15, title: "Preview", "text-sk": "Preview the result.", "text-en": "English preview." }
    ]
  };
}

test("reordering shots changes narration, timing and automatic callback order together", async () => {
  const plan = createPlan();
  plan.shots = [plan.shots[2], plan.shots[0], plan.shots[1]];
  const original = JSON.stringify(plan);
  const calls = [];
  const actions = { edit: async () => calls.push("edit"), preview: async () => calls.push("preview") };
  const recording = getRecordingShots(plan, actions);
  for (const shot of recording) await actions[shot.id]();
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
  assert.throws(() => getRecordingShots(createPlan(), { edit() {} }), /Missing video action.*preview/);
});

test("records reordered shots through setup, slate, preparation and cleanup without running manual shots", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const plan = createPlan();
  plan.shots = [plan.shots[2], plan.shots[0], plan.shots[1]];
  const events = [];
  const messages = [];
  const callback = name => async shot => {
    await new Promise(resolve => setImmediate(resolve));
    events.push(`${name}:${shot.id}`);
  };
  const I = {
    say: async message => messages.push(message),
    videoTitle: async shot => events.push(typeof shot === "string" ? shot : `SLATE:${shot.id}`)
  };
  await recordVideoPlan(I, {
    plan,
    scenarios: { edit: callback("RUN"), preview: callback("RUN") },
    setup: async () => events.push("LOGIN"),
    prepare: callback("BASELINE"),
    prepareShots: { preview: callback("PREPARE") },
    cleanup: callback("CLEANUP")
  });
  assert.deepEqual(events, [
    "LOGIN", "SETUP shot 1/2 preview", "BASELINE:preview", "PREPARE:preview", "SLATE:preview", "RUN:preview", "CLEANUP:preview",
    "SETUP shot 2/2 edit", "BASELINE:edit", "SLATE:edit", "RUN:edit", "CLEANUP:edit"
  ]);
  assert.deepEqual(messages.filter(message => message.startsWith("Recording ")), [
    "Recording shot 1/2 preview (15s)",
    "Recording shot 2/2 edit (10s)"
  ], "Progress must count only automatic shots, even with manual shots between them");
});

test("validates callbacks before setup and stops recording after a failed shot", async () => {
  const { recordVideoPlan } = require("./feature_video_plan.js");
  const events = [];
  const I = { say: async () => {}, videoTitle: async () => {} };
  const options = {
    plan: createPlan(),
    setup: async () => events.push("setup"),
    scenarios: { edit: async () => { throw new Error("shot failed"); } },
    cleanup: async () => events.push("cleanup")
  };
  await assert.rejects(recordVideoPlan(I, options), /Missing video action.*preview/);
  assert.deepEqual(events, []);
  options.scenarios.preview = async () => events.push("preview");
  await assert.rejects(recordVideoPlan(I, options), /shot failed/);
  assert.deepEqual(events, ["setup"], "A failed shot must not run further shots or mask the original error");
});
