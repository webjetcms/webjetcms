const baseConfig = require("./codecept.conf.js").config;

exports.config = {
  ...baseConfig,
  tests: "./video/**/*.js"
};
