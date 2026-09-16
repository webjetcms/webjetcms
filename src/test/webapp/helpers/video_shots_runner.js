const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const { readVideoPlanSource, resolveVideoScenarioPath } = require("./video_plan_source.js");
const { resolveVideoPlan } = require("./feature_video_plan.js");

/** Records every shot serially through the existing single-shot video command. */
function runVideoShots(argv, options = {}) {
  const stdout = options.stdout || console.log;
  const stderr = options.stderr || console.error;
  const usage = "Usage: npm run video:shots video/<scenario>.js";
  if (argv.length === 1 && argv[0] === "--help") {
    stdout(usage);
    return 0;
  }
  if (argv.length !== 1) {
    stderr(usage);
    return 1;
  }
  try {
    const webappRoot = options.webappRoot || path.resolve(__dirname, "..");
    const sourcePath = resolveVideoScenarioPath(argv[0], { ...options, webappRoot });
    const plan = readVideoPlanSource(fs.readFileSync(sourcePath, "utf8"), sourcePath);
    const { shots } = resolveVideoPlan(plan);
    for (const shot of shots) {
      if (shot.type === "auto" && !Object.hasOwn(shot, "shot")) {
        throw new Error(`Missing video action for automatic shot: ${shot.id}`);
      }
    }
    const spawn = options.spawnSync || spawnSync;
    const environment = options.environment || process.env;
    const failed = [];
    for (const shot of shots) {
      stdout(`Recording shot ${shot.number}/${shot.total}: ${shot.id} (${shot.type})`);
      const result = spawn("npm", ["run", "video", "--", sourcePath], {
        cwd: webappRoot,
        env: { ...environment, VIDEO_SHOT: shot.id },
        stdio: "inherit"
      });
      if (result.error) throw result.error;
      if (result.signal) throw new Error(`Video recording interrupted by ${result.signal}.`);
      if (result.status !== 0) failed.push(shot.id);
    }
    stdout(`Recorded ${shots.length - failed.length}/${shots.length} shots successfully.`);
    if (failed.length) stderr(`Failed shots: ${failed.join(", ")}. Retry them with VIDEO_SHOT=<id> npm run video ${argv[0]}`);
    return failed.length ? 1 : 0;
  } catch (error) {
    stderr(error.message);
    return 1;
  }
}

if (require.main === module) process.exitCode = runVideoShots(process.argv.slice(2));
module.exports = { runVideoShots };
