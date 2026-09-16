const baseConfig = require("./codecept.conf.js").config;
const { getVideoSettings, getVideoShot } = require("./helpers/video_settings.js");

const videoShot = getVideoShot();
const videoSettings = getVideoSettings();

console.log("videoSize=", `${videoSettings.width}x${videoSettings.height}`);
console.log("videoViewport=", `${videoSettings.viewportWidth}x${videoSettings.viewportHeight}`);
console.log("videoZoom=", videoSettings.zoom);
if (videoShot) console.log("videoShot=", videoShot);

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
    HeadHelper: {
      require: "./helpers/head_helper.js",
      generationEnabled: false
    },
    AudioHelper: {
      require: "./helpers/audio_helper.js",
      generationEnabled: false
    }
  }
};
