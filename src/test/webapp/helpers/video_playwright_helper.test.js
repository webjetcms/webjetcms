const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");
const { spawnSync } = require("node:child_process");
const {
  finalizeVideoArtifact,
  getVideoArtifactName,
  getVideoArtifactPath,
  getVideoRawDirectory
} = require("./video_playwright_helper.js");

test.beforeEach(t => {
  const previousShot = process.env.VIDEO_SHOT;
  delete process.env.VIDEO_SHOT;
  t.after(() => {
    if (previousShot === undefined) delete process.env.VIDEO_SHOT;
    else process.env.VIDEO_SHOT = previousShot;
  });
});

test("rejects malformed VIDEO_SHOT while loading the video configuration", () => {
  const result = spawnSync(process.execPath, ["-e", "require('./codecept.video.conf.js')"], {
    cwd: path.resolve(__dirname, ".."), env: { ...process.env, VIDEO_SHOT: "../outro" }, encoding: "utf8"
  });
  assert.equal(result.status, 1);
  assert.match(result.stderr, /VIDEO_SHOT must be a lowercase hyphenated shot ID/);
});

test("names retakes with the normalized shot ID and preserves the full-run defaults", () => {
  const scenario = { file: "video/308-pb-redesign.js", title: "308-pb-redesign @current" };
  process.env.VIDEO_SHOT = " outro ";
  assert.equal(getVideoArtifactName(scenario, true), "308-pb-redesign-outro.webm");
  assert.equal(getVideoArtifactName(scenario, false), "308-pb-redesign-outro.failed.webm");
  assert.equal(getVideoArtifactPath(scenario, true),
    path.resolve(__dirname, "../../../../docs/feature-video/308-pb-redesign-outro.webm"));
  for (const value of ["", "  "]) {
    process.env.VIDEO_SHOT = value;
    assert.equal(getVideoArtifactName(scenario, true), "308-pb-redesign.webm");
    assert.equal(getVideoArtifactName(scenario, false), "308-pb-redesign.failed.webm");
  }
});

test("replaces only the matching retake and result status while retaining full recordings", async t => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "wj-video-retake-"));
  t.after(() => fs.rm(directory, { recursive: true, force: true }));
  const scenario = { file: "video/308-pb-redesign.js" };
  const originals = ["308-pb-redesign.webm", "308-pb-redesign.failed.webm", "308-pb-redesign-edit.webm",
    "308-pb-redesign-outro.webm", "308-pb-redesign-outro.failed.webm"];
  for (const name of originals) await fs.writeFile(path.join(directory, name), name);
  process.env.VIDEO_SHOT = "outro";
  for (const passed of [false, true]) {
    const rawPath = path.join(directory, "raw.webm");
    await fs.writeFile(rawPath, `retake:${passed}`);
    await finalizeVideoArtifact({ path: async () => rawPath }, path.join(directory, getVideoArtifactName(scenario, passed)));
    for (const name of originals) {
      const expected = name === "308-pb-redesign-outro.failed.webm" ? "retake:false"
        : name === "308-pb-redesign-outro.webm" && passed ? "retake:true" : name;
      assert.equal(await fs.readFile(path.join(directory, name), "utf8"), expected);
    }
  }
});

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

test("finalizes a raw recording with one same-filesystem rename", async () => {
  const outputDirectory = await fs.mkdtemp(path.join(os.tmpdir(), "wj-video-helper-"));
  const rawDirectory = path.join(outputDirectory, ".video-raw", "test-run");
  const rawPath = path.join(rawDirectory, "raw-video.webm");
  const targetPath = path.join(outputDirectory, "scenario.webm");

  try {
    await fs.mkdir(rawDirectory, { recursive: true });
    await fs.writeFile(rawPath, "new-video");
    await fs.writeFile(targetPath, "previous-video");
    await finalizeVideoArtifact({
      path: async () => rawPath,
      saveAs: async () => {
        throw new Error("saveAs must not be called");
      }
    }, targetPath);

    assert.equal(await fs.readFile(targetPath, "utf8"), "new-video");
    await assert.rejects(
      fs.access(rawPath),
      (error) => error.code === "ENOENT"
    );
  } finally {
    await fs.rm(outputDirectory, { recursive: true, force: true });
  }
});

test("preserves the previous recording when final replacement fails", async () => {
  const outputDirectory = await fs.mkdtemp(path.join(os.tmpdir(), "wj-video-helper-"));
  const rawDirectory = path.join(outputDirectory, ".video-raw", "test-run");
  const rawPath = path.join(rawDirectory, "raw-video.webm");
  const targetPath = path.join(outputDirectory, "scenario.webm");
  const failingFileSystem = {
    mkdir: fs.mkdir,
    rename: async () => {
      throw new Error("simulated rename failure");
    }
  };

  try {
    await fs.mkdir(rawDirectory, { recursive: true });
    await fs.writeFile(rawPath, "new-video");
    await fs.writeFile(targetPath, "previous-video");
    await assert.rejects(
      finalizeVideoArtifact({
        path: async () => rawPath
      }, targetPath, {
        fsImpl: failingFileSystem
      }),
      /simulated rename failure/
    );

    assert.equal(await fs.readFile(targetPath, "utf8"), "previous-video");
    assert.equal(await fs.readFile(rawPath, "utf8"), "new-video");
  } finally {
    await fs.rm(outputDirectory, { recursive: true, force: true });
  }
});

test("keeps raw recordings below docs/feature-video", () => {
  const scenario = {
    file: path.join("project", "video", "293-config-jstree-view.js"),
    title: "293-config-jstree-view @video",
    tags: ["@video"]
  };

  assert.equal(
    getVideoRawDirectory(scenario, "123-0-test"),
    path.resolve(
      __dirname,
      "../../../../docs/feature-video/.video-raw/123-0-test-293-config-jstree-view"
    )
  );
});

test("resolves final recordings directly below docs/feature-video", () => {
  const scenario = {
    file: path.join("project", "video", "293-config-jstree-view.js"),
    title: "293-config-jstree-view @video",
    tags: ["@video"]
  };

  assert.equal(
    getVideoArtifactPath(scenario, true),
    path.resolve(__dirname, "../../../../docs/feature-video/293-config-jstree-view.webm")
  );
  assert.equal(
    getVideoArtifactPath(scenario, false),
    path.resolve(__dirname, "../../../../docs/feature-video/293-config-jstree-view.failed.webm")
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
