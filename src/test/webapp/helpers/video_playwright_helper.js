const fs = require("fs").promises;
const path = require("path");
const { randomBytes } = require("crypto");
const { threadId } = require("worker_threads");
const Playwright = require("codeceptjs/lib/helper/Playwright");

function sanitizeScenarioName(test) {
  const title = String(test?.title || "video").trim();
  return title
    .replace(/[<>:"/\\|?*\u0000-\u001f]/g, "-")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-") || "video";
}

function escapeRegularExpression(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function isLegacyScenarioVideo(fileName, scenarioName) {
  const escapedScenarioName = escapeRegularExpression(scenarioName);
  const pattern = new RegExp(
    `^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}_${escapedScenarioName}\\.(passed|failed)\\.webm$`,
    "i"
  );
  return pattern.test(fileName);
}

async function removeLegacyScenarioVideos(videoDirectory, scenarioName, targetPath) {
  let fileNames = [];
  try {
    fileNames = await fs.readdir(videoDirectory);
  } catch (error) {
    if (error.code === "ENOENT") return;
    throw error;
  }

  await Promise.all(fileNames.map(async (fileName) => {
    const filePath = path.join(videoDirectory, fileName);
    if (filePath === targetPath) return;
    if (!isLegacyScenarioVideo(fileName, scenarioName)) return;
    await fs.rm(filePath, { force: true });
  }));
}

async function removeRawDirectory(rawDirectory) {
  if (rawDirectory == null) return;
  await fs.rm(rawDirectory, { recursive: true, force: true });
  await fs.rmdir(path.dirname(rawDirectory)).catch((error) => {
    if (error.code !== "ENOENT" && error.code !== "ENOTEMPTY") throw error;
  });
}

async function replaceFile(sourcePath, targetPath) {
  try {
    await fs.rename(sourcePath, targetPath);
  } catch (error) {
    if (error.code !== "EEXIST" && error.code !== "ENOTEMPTY" && error.code !== "EPERM") throw error;
    await fs.rm(targetPath, { force: true });
    await fs.rename(sourcePath, targetPath);
  }
}

class VideoPlaywrightHelper extends Playwright {

  async _before(test) {
    if (this.options.recordVideo != null) {
      const scenarioName = sanitizeScenarioName(test);
      const runId = `${process.pid}-${threadId}-${randomBytes(6).toString("hex")}`;
      this.videoRawDirectory = path.join(global.output_dir, ".video-raw", `${runId}-${scenarioName}`);
      await fs.rm(this.videoRawDirectory, { recursive: true, force: true });
      await fs.mkdir(this.videoRawDirectory, { recursive: true });
      this.options.recordVideo.dir = this.videoRawDirectory;
    }
    await super._before(test);
  }

  async _passed(test) {
    this.videoArtifactTest = test;
    const recordVideo = this.options.recordVideo;
    this.options.recordVideo = undefined;
    try {
      await super._passed(test);
    } finally {
      this.options.recordVideo = recordVideo;
    }
  }

  async _failed(test) {
    this.videoArtifactTest = test;
    const recordVideo = this.options.recordVideo;
    this.options.recordVideo = undefined;
    try {
      await super._failed(test);
    } finally {
      this.options.recordVideo = recordVideo;
    }
  }

  async _after() {
    const test = this.videoArtifactTest;
    const video = this.page?.video();
    const rawDirectory = this.videoRawDirectory;

    try {
      await super._after();
    } catch (error) {
      process.exitCode = 1;
      error.message = `${error.message}\nRaw video retained in ${rawDirectory}`;
      this.videoArtifactTest = null;
      this.videoRawDirectory = null;
      throw error;
    }

    if (test == null || video == null) {
      try {
        await removeRawDirectory(rawDirectory);
      } finally {
        this.videoArtifactTest = null;
        this.videoRawDirectory = null;
      }
      return;
    }

    const scenarioName = sanitizeScenarioName(test);
    const videoDirectory = path.join(global.output_dir, "videos");
    const targetName = `${scenarioName}.webm`;
    const targetPath = path.join(videoDirectory, targetName);
    const temporaryPath = path.join(rawDirectory, targetName);

    let finalized = false;
    try {
      await fs.mkdir(videoDirectory, { recursive: true });
      await fs.rm(temporaryPath, { force: true });
      await video.saveAs(temporaryPath);
      await replaceFile(temporaryPath, targetPath);
      if (test.artifacts == null) test.artifacts = {};
      test.artifacts.video = targetPath;
      await video.delete().catch(() => {});
      await removeLegacyScenarioVideos(videoDirectory, scenarioName, targetPath);
      finalized = true;
    } catch (error) {
      process.exitCode = 1;
      error.message = `${error.message}\nRaw video retained in ${rawDirectory}`;
      throw error;
    } finally {
      if (finalized) await removeRawDirectory(rawDirectory);
      this.videoArtifactTest = null;
      this.videoRawDirectory = null;
    }
  }
}

module.exports = VideoPlaywrightHelper;
