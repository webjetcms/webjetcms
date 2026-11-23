const { HEAD_GREP, resolveHeadScenario } = require("./helpers/head_runner.js");
const headFile = process.env.CODECEPT_HEAD_FILE;

exports.config = {
  tests: typeof headFile === "string" && headFile.trim() !== "" ? [resolveHeadScenario(headFile)] : [],
  grep: HEAD_GREP,
  output: "../../../build/test",
  helpers: { HeadHelper: { require: "./helpers/head_helper.js", generationEnabled: true } },
  plugins: false,
  name: "webapp-head"
};
