const path = require("node:path");
const { parseArgs } = require("node:util");
const { spawnSync } = require("node:child_process");
const { resolveVideoScenarioPath } = require("./video_plan_source.js");
const { validateVideoTitle } = require("./video_thumbnail.js");

/** Runs only the selected file's @title scenario; CLI text/style override its defaults. */
function runVideoTitle(argv, options = {}) {
  const stdout = options.stdout || console.log;
  const stderr = options.stderr || console.error;
  const usage = 'Usage: npm run video:title -- video/<scenario>.js [--text "Headline"] [--style glow|clean|bold] [--dry-run]';
  try {
    const { values, positionals } = parseArgs({ args: argv, allowPositionals: true, options: {
      text: { type: "string" }, style: { type: "string" }, help: { type: "boolean" }, "dry-run": { type: "boolean" }
    } });
    if (values.help) { stdout(usage); return 0; }
    if (positionals.length !== 1) throw new Error(usage);
    const file = resolveVideoScenarioPath(positionals[0], options);
    const env = { ...(options.environment || process.env) };
    if (values.text !== undefined) env.VIDEO_TITLE_TEXT = values.text;
    if (values.style !== undefined) env.VIDEO_TITLE_STYLE = values.style;
    validateVideoTitle(env.VIDEO_TITLE_TEXT ?? "Title", env.VIDEO_TITLE_STYLE ?? "glow");
    for (const [key, value] of Object.entries({
      CODECEPT_VIDEO_WIDTH: "1920", CODECEPT_VIDEO_HEIGHT: "1080", CODECEPT_VIDEO_ZOOM: "1.411764705882353",
      CODECEPT_URL: "http://iwcm.interway.sk", CODECEPT_SHOW: "true", CODECEPT_LNG: "sk"
    })) env[key] ||= value;
    Object.assign(env, { CODECEPT_VIDEO: "true", CODECEPT_VIDEO_CURSOR: "false", CODECEPT_RESTART: "context", VIDEO_SHOT: "" });
    const result = (options.spawnSync || spawnSync)(process.execPath, [
      require.resolve("codeceptjs/bin/codecept.js"), values["dry-run"] ? "dry-run" : "run",
      "-c", "codecept.title.conf.js", "--steps", "-p", "autoLogin", "--grep", "(^|\\s)@title(\\s|$)", file
    ], { cwd: options.webappRoot || path.resolve(__dirname, ".."), env, stdio: "inherit" });
    if (result.error) throw result.error;
    return result.status ?? 1;
  } catch (error) {
    stderr(error.message);
    return 1;
  }
}

if (require.main === module) process.exitCode = runVideoTitle(process.argv.slice(2));
module.exports = { runVideoTitle };
