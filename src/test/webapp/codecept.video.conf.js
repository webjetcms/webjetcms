const baseConfig = require("./codecept.conf.js").config;
const { getVideoSettings } = require("./helpers/video_settings.js");

const videoSettings = getVideoSettings();

console.log("videoSize=", `${videoSettings.width}x${videoSettings.height}`);
console.log("videoViewport=", `${videoSettings.viewportWidth}x${videoSettings.viewportHeight}`);
console.log("videoZoom=", videoSettings.zoom);

exports.config = {
  ...baseConfig,
  tests: "./video/**/*.js",
  helpers: {
    ...baseConfig.helpers,
    Playwright: {
      ...baseConfig.helpers.Playwright,
      require: "./helpers/video_playwright_helper.js",
      windowSize: `${videoSettings.width}x${videoSettings.height}`,
      video: true,
      keepVideoForPassedTests: true,
      recordVideo: {
        size: {
          width: videoSettings.width,
          height: videoSettings.height
        }
      }
    },
    VideoHelper: {
      require: "./helpers/video_helper.js"
    },
    AudioHelper: {
      require: "./helpers/audio_helper.js",
      generationEnabled: false
    }
  }
};
