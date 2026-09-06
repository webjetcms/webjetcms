/** Validates shot metadata and derives the edited timeline from the array order. */
function resolveVideoPlan(plan, language = plan?.language || "sk") {
  if (!plan || typeof plan !== "object" || !Array.isArray(plan.shots) || plan.shots.length === 0) {
    throw new Error("Video plan must contain a non-empty shots array.");
  }
  if (typeof language !== "string" || !/^[a-z]{2,3}(?:-[A-Za-z0-9]+)*$/.test(language)) {
    throw new Error("Video language must be a language code such as sk, cs or en.");
  }
  const ids = new Set();
  let elapsed = 0;
  const shots = plan.shots.map((shot, index) => {
    if (!shot || typeof shot.id !== "string" || !/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(shot.id)) {
      throw new Error(`Shot ${index + 1} must have a stable lowercase hyphenated id.`);
    }
    if (ids.has(shot.id)) throw new Error(`Duplicate shot id: ${shot.id}`);
    ids.add(shot.id);
    if (!["auto", "manual"].includes(shot.type)) {
      throw new Error(`Shot ${shot.id} type must be auto or manual.`);
    }
    if (!Number.isInteger(shot.durationSeconds) || shot.durationSeconds <= 0) {
      throw new Error(`Shot ${shot.id} durationSeconds must be a positive integer.`);
    }
    if (typeof shot.title !== "string" || shot.title.trim() === "") {
      throw new Error(`Shot ${shot.id} must have a title.`);
    }
    const text = shot[`text-${language}`];
    if (typeof text !== "string") {
      throw new Error(`Shot ${shot.id} is missing text-${language}; use an empty string for an intentional silent shot.`);
    }
    const startSeconds = elapsed;
    elapsed += shot.durationSeconds;
    return {
      ...shot,
      number: index + 1,
      startSeconds,
      endSeconds: elapsed,
      narration: text.trim().replace(/\r\n?/g, "\n")
    };
  });
  return { language, shots };
}

/** Joins only localized narration, including manual shots, in the edited order. */
function getPlanNarration(plan, language) {
  const resolved = resolveVideoPlan(plan, language);
  const text = resolved.shots.map(shot => shot.narration).filter(Boolean).join("\n\n");
  if (text === "") throw new Error("Video plan narration must not be empty.");
  return text;
}

function formatTime(seconds) {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}

/** Formats a shot plan without duplicating timing, titles or narration in the scenario. */
function formatShotPlan(plan, language) {
  const resolved = resolveVideoPlan(plan, language);
  return [
    `Language: ${resolved.language}. Times describe the edited timeline; adjust durations to the recorded voice.`,
    "Cut setup, cleanup and two-second slates out of the recording. Manual shots require separate footage.",
    plan.notes || "",
    ...resolved.shots.map(shot => [
      `${formatTime(shot.startSeconds)}-${formatTime(shot.endSeconds)} | ${shot.type.toUpperCase()} | Shot ${shot.number} [${shot.id}] | ${shot.title}`,
      shot.notes || "",
      shot.narration
    ].filter(Boolean).join("\n"))
  ].filter(Boolean).join("\n\n");
}

/** Checks every automatic callback before recording and preserves the plan's shot order. */
function getRecordingShots(plan, language) {
  const shots = resolveVideoPlan(plan, language).shots.filter(shot => shot.type === "auto");
  for (const shot of shots) {
    if (typeof shot.shot !== "function") {
      throw new Error(`Missing video action for automatic shot: ${shot.id}`);
    }
    if (shot.prepare !== undefined && typeof shot.prepare !== "function") {
      throw new Error(`Shot ${shot.id} prepare must be a function.`);
    }
  }
  return shots;
}

/**
 * Records automatic shots in plan order with setup slates and scenario-specific lifecycle callbacks.
 * Call this plain async function from a Scenario, outside the CodeceptJS helper step queue.
 * @param {object} I CodeceptJS actor
 * @param {object} options Shot plan and recording callbacks
 * @param {object} options.plan Shot plan with a shot function and optional prepare function on each automatic shot
 * @param {object} [options.context] Dependencies passed to inline callbacks, augmented with the actor I and resolved shot
 * @param {Function} [options.setup] One-time login and shared setup, after plan validation
 * @param {Function} [options.prepare] Baseline preparation before every shot; receives the resolved shot
 * @param {Function} [options.cleanup] Cleanup after each successful shot; receives the resolved shot
 * @param {string} [options.language] Narration language override
 * @returns {Promise<void>} Resolves after every automatic shot and its cleanup
 */
async function recordVideoPlan(I, { plan, context = {}, setup, prepare, cleanup, language }) {
  const recordingShots = getRecordingShots(plan, language);
  if (setup) await setup();
  for (const [index, shot] of recordingShots.entries()) {
    const shotContext = { ...context, I, shot };
    const shotLabel = `shot ${index + 1}/${recordingShots.length} ${shot.id}`;
    await I.say("----------------------------------------------------------------------------");
    await I.say(`Recording ${shotLabel} (${shot.durationSeconds}s)`);
    await I.videoTitle(`SETUP ${shotLabel} (${shot.durationSeconds}s)`);
    if (prepare) await prepare(shot);
    if (shot.prepare) await shot.prepare(shotContext);
    await I.videoTitle(shot);
    await shot.shot(shotContext);
    if (cleanup) await cleanup(shot);
  }
}

module.exports = { resolveVideoPlan, getPlanNarration, formatShotPlan, getRecordingShots, recordVideoPlan };
