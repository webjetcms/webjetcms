const videoConfig = require("./codecept.video.conf.js").config;

// Reuse the recording viewport and native zoom, but produce no video or paid media.
exports.config = {
  ...videoConfig,
  helpers: {
    ...videoConfig.helpers,
    Playwright: {
      ...videoConfig.helpers.Playwright,
      video: false,
      keepVideoForPassedTests: false,
      recordVideo: undefined
    },
    VideoHelper: {
      ...videoConfig.helpers.VideoHelper,
      titleMode: true
    }
  }
};
