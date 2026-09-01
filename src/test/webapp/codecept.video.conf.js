const baseConfig = require("./codecept.conf.js").config;

exports.config = {
  ...baseConfig,
  tests: "./video/**/*.js",
  helpers: {
    ...baseConfig.helpers,
    Playwright: {
      ...baseConfig.helpers.Playwright,
      require: "./helpers/video_playwright_helper.js"
    },
    AudioHelper: {
      require: "./helpers/audio_helper.js",
      generationEnabled: false
    }
  }
};
