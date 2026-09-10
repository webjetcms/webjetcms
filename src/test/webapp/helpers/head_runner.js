const path = require("node:path");
const { spawnSync } = require("node:child_process");
const { resolveAudioScenario, validateGenerationScenarioSource } = require("./audio_runner.js");

const HEAD_GREP = "(^|\\s)@head(\\s|$)";
const WEBAPP_ROOT = path.resolve(__dirname, "..");

function validateHeadScenarioSource(source, sourcePath) {
  return validateGenerationScenarioSource(source, sourcePath, "head");
}

function resolveHeadScenario(argument, options = {}) {
  try {
    return resolveAudioScenario(argument, { ...options, validateSource: validateHeadScenarioSource });
  } catch (error) {
    throw new Error(error.message.replaceAll("Audio scenario", "Head scenario").replaceAll("audio scenario", "head scenario"));
  }
}

/** Runs only one statically validated @head scenario using the browser-free configuration. */
function runHead(argv, options = {}) {
  const stderr = options.stderr || console.error;
  if (argv.length !== 1) {
    stderr("Usage: npm run head video/<scenario>.js");
    return 1;
  }
  try {
    const scenarioPath = resolveHeadScenario(argv[0], options);
    const webappRoot = options.webappRoot || WEBAPP_ROOT;
    const result = (options.spawnSync || spawnSync)(process.execPath, [
      options.codeceptBin || require.resolve("codeceptjs/bin/codecept.js"), "run", "-c",
      path.join(webappRoot, "codecept.head.conf.js"), "--steps", "--grep", HEAD_GREP
    ], { cwd: webappRoot, env: { ...process.env, CODECEPT_HEAD_FILE: scenarioPath }, stdio: "inherit" });
    if (result.error) throw result.error;
    return Number.isInteger(result.status) ? result.status : 1;
  } catch (error) {
    stderr(error.message);
    return 1;
  }
}

if (require.main === module) process.exitCode = runHead(process.argv.slice(2));
module.exports = { HEAD_GREP, validateHeadScenarioSource, resolveHeadScenario, runHead };
