const audioFile = process.env.CODECEPT_AUDIO_FILE;
const { AUDIO_GREP, resolveAudioScenario } = require("./helpers/audio_runner.js");
const tests = typeof audioFile === "string" && audioFile.trim() !== ""
  ? [resolveAudioScenario(audioFile)]
  : [];

exports.config = {
  tests,
  grep: AUDIO_GREP,
  output: "../../../build/test",
  helpers: {
    AudioHelper: {
      require: "./helpers/audio_helper.js",
      generationEnabled: true
    }
  },
  plugins: false,
  name: "webapp-audio"
};
