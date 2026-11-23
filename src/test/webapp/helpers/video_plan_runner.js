const fs = require("node:fs");
const { readVideoPlanSource, resolveVideoScenarioPath } = require("./video_plan_source.js");
const { resolveVideoPlan, formatShotPlan } = require("./feature_video_plan.js");

/** Prints narration and the edited shot timeline without running CodeceptJS, browser steps or API calls. */
function runVideoPlan(argv, options = {}) {
  const stdout = options.stdout || console.log;
  const stderr = options.stderr || console.error;
  const usage = "Usage: npm run video:plan video/<scenario>.js";
  if (argv.length === 1 && argv[0] === "--help") {
    stdout(usage);
    return 0;
  }
  if (argv.length !== 1) {
    stderr(usage);
    return 1;
  }
  try {
    const sourcePath = resolveVideoScenarioPath(argv[0], options);
    const plan = readVideoPlanSource((options.fsImpl || fs).readFileSync(sourcePath, "utf8"), sourcePath);
    const { language, shots } = resolveVideoPlan(plan);
    const narration = shots.map(shot => shot.narration).filter(Boolean).join("\n\n");
    const duration = shots.at(-1).endSeconds;
    const time = `${Math.floor(duration / 60)}:${String(duration % 60).padStart(2, "0")}`;
    stdout([
      `Video: ${sourcePath}`,
      `Language: ${language} | Shots: ${shots.length} | Estimated edited duration: ${time} (${duration}s)`,
      `Narration: ${narration.match(/\S+/gu)?.length || 0} words | ${Array.from(narration).length} characters`,
      "NARRATION\n=========",
      narration || "(No spoken narration.)",
      "SHOT PLAN\n=========",
      formatShotPlan(plan)
    ].join("\n\n"));
    return 0;
  } catch (error) {
    stderr(error.message);
    return 1;
  }
}

if (require.main === module) process.exitCode = runVideoPlan(process.argv.slice(2));
module.exports = { runVideoPlan };
