const assert = require("node:assert/strict");
const path = require("node:path");
const test = require("node:test");
const { getVideoArtifactName } = require("./video_playwright_helper.js");

test("keeps the scenario file name stable for tagged and untagged runs", () => {
  const scenarioFile = path.join("project", "video", "293-config-jstree-view.js");

  assert.equal(
    getVideoArtifactName({
      file: scenarioFile,
      title: "293-config-jstree-view"
    }, true),
    "293-config-jstree-view.webm"
  );
  assert.equal(
    getVideoArtifactName({
      file: scenarioFile,
      title: "293-config-jstree-view @current",
      tags: ["@current"]
    }, true),
    "293-config-jstree-view.webm"
  );
  assert.equal(
    getVideoArtifactName({
      file: scenarioFile,
      title: "293-config-jstree-view @current",
      tags: ["@current"]
    }, false),
    "293-config-jstree-view.failed.webm"
  );
});

test("strips CodeceptJS tags when scenario file metadata is unavailable", () => {
  assert.equal(
    getVideoArtifactName({
      title: "293-config-jstree-view @current @video",
      tags: ["@current", "@video"]
    }, true),
    "293-config-jstree-view.webm"
  );
});
