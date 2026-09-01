const fs = require("fs").promises;
const path = require("path");
const { randomBytes } = require("crypto");
const { threadId } = require("worker_threads");
const { getVideoSettings } = require("./video_settings.js");

const PLAYWRIGHT_CORE_ROOT = path.dirname(require.resolve("playwright-core/package.json"));
const PLAYWRIGHT_VIDEO_OPTIONS = {
  "-b:v": { original: "1M", highQuality: "50M" },
  "-crf": { original: "8", highQuality: "0" },
  "-qmax": { original: "50", highQuality: "4" }
};
const PROCESS_LAUNCHER_PATCH = Symbol.for("webjet.video.high-quality.process-launcher");
const CR_SESSION_PATCH = Symbol.for("webjet.video.high-quality.cr-session");
const CHROMIUM_PROFILE_PATCH = Symbol.for("webjet.video.native-zoom.chromium-profile");
const CODECEPT_TEST_TAG_PATTERN = /@[a-zA-Z0-9_-]+/g;

function videoZoomToChromiumLevel(zoom) {
  // Chromium stores page zoom logarithmically: zoom factor = 1.2 ^ zoom level.
  return Math.log(zoom) / Math.log(1.2);
}

async function setChromiumDefaultZoom(userDataDir, zoom) {
  const profileDirectory = path.join(userDataDir, "Default");
  const preferencesPath = path.join(profileDirectory, "Preferences");
  let preferences = {};

  try {
    preferences = JSON.parse(await fs.readFile(preferencesPath, "utf8"));
  } catch (error) {
    if (error.code !== "ENOENT") throw error;
  }

  if (preferences.partition == null) preferences.partition = {};
  if (preferences.partition.default_zoom_level == null) {
    preferences.partition.default_zoom_level = {};
  }
  preferences.partition.default_zoom_level.x = videoZoomToChromiumLevel(zoom);

  await fs.mkdir(profileDirectory, { recursive: true });
  await fs.writeFile(preferencesPath, JSON.stringify(preferences));
}

function replacePlaywrightVideoOption(args, option, values, codecIndex) {
  const optionIndex = args.indexOf(option, codecIndex);
  if (optionIndex === -1 || args[optionIndex + 1] !== values.original) {
    throw new Error(
      `Unsupported Playwright video encoder signature for ${option}. ` +
      "Revalidate the high-quality video profile after upgrading Playwright."
    );
  }
  args[optionIndex + 1] = values.highQuality;
}

function installVideoProfile() {
  if (process.env.CODECEPT_VIDEO !== "true") return;

  const { Chromium } = require(path.join(PLAYWRIGHT_CORE_ROOT, "lib/server/chromium/chromium.js"));
  if (Chromium.prototype[CHROMIUM_PROFILE_PATCH] !== true) {
    const originalPrepareUserDataDir = Chromium.prototype.prepareUserDataDir;
    Chromium.prototype.prepareUserDataDir = async function(options, userDataDir) {
      await originalPrepareUserDataDir.call(this, options, userDataDir);
      await setChromiumDefaultZoom(userDataDir, getVideoSettings().zoom);
    };
    Chromium.prototype[CHROMIUM_PROFILE_PATCH] = true;
  }

  const processLauncher = require(path.join(PLAYWRIGHT_CORE_ROOT, "lib/utils/processLauncher.js"));
  if (processLauncher[PROCESS_LAUNCHER_PATCH] !== true) {
    const originalLaunchProcess = processLauncher.launchProcess;
    processLauncher.launchProcess = (options) => {
      const originalArgs = options.args;
      if (!Array.isArray(originalArgs)) return originalLaunchProcess(options);

      const codecIndex = originalArgs.findIndex((arg, index) => {
        return arg === "-c:v" && originalArgs[index + 1] === "vp8";
      });
      const outputFile = originalArgs[originalArgs.length - 1];
      const isPlaywrightVideoEncoder = codecIndex !== -1 &&
        typeof outputFile === "string" && outputFile.endsWith(".webm") &&
        path.basename(String(options.command)).startsWith("ffmpeg");
      if (!isPlaywrightVideoEncoder) return originalLaunchProcess(options);

      const args = [...originalArgs];
      for (const [option, values] of Object.entries(PLAYWRIGHT_VIDEO_OPTIONS)) {
        replacePlaywrightVideoOption(args, option, values, codecIndex);
      }
      return originalLaunchProcess({ ...options, args });
    };
    processLauncher[PROCESS_LAUNCHER_PATCH] = true;
  }

  const { CRSession } = require(path.join(PLAYWRIGHT_CORE_ROOT, "lib/server/chromium/crConnection.js"));
  if (CRSession.prototype[CR_SESSION_PATCH] !== true) {
    const originalSend = CRSession.prototype.send;
    CRSession.prototype.send = function(method, params) {
      if (method === "Page.startScreencast" && params?.format === "jpeg") {
        params = { ...params, quality: 100 };
      }
      return originalSend.call(this, method, params);
    };
    CRSession.prototype[CR_SESSION_PATCH] = true;
  }
}

installVideoProfile();

const Playwright = require("codeceptjs/lib/helper/Playwright");

function sanitizeScenarioName(test) {
  const scenarioFile = typeof test?.file === "string" ? test.file.trim() : "";
  const sourceName = scenarioFile !== ""
    ? path.basename(scenarioFile, path.extname(scenarioFile))
    : String(test?.title || "video").replace(CODECEPT_TEST_TAG_PATTERN, "").trim();
  return sourceName
    .replace(/[<>:"/\\|?*\u0000-\u001f]/g, "-")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-") || "video";
}

function getVideoArtifactName(test, passed) {
  const scenarioName = sanitizeScenarioName(test);
  return passed ? `${scenarioName}.webm` : `${scenarioName}.failed.webm`;
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
    this.videoArtifactPassed = true;
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
    this.videoArtifactPassed = false;
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
    const passed = this.videoArtifactPassed === true;
    const video = this.page?.video();
    const rawDirectory = this.videoRawDirectory;

    try {
      await super._after();
    } catch (error) {
      process.exitCode = 1;
      error.message = `${error.message}\nRaw video retained in ${rawDirectory}`;
      this.videoArtifactTest = null;
      this.videoArtifactPassed = null;
      this.videoRawDirectory = null;
      throw error;
    }

    if (test == null || video == null) {
      try {
        await removeRawDirectory(rawDirectory);
      } finally {
        this.videoArtifactTest = null;
        this.videoArtifactPassed = null;
        this.videoRawDirectory = null;
      }
      return;
    }

    const scenarioName = sanitizeScenarioName(test);
    const videoDirectory = path.join(global.output_dir, "videos");
    const targetName = getVideoArtifactName(test, passed);
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
      this.videoArtifactPassed = null;
      this.videoRawDirectory = null;
    }
  }
}

module.exports = VideoPlaywrightHelper;
module.exports.getVideoArtifactName = getVideoArtifactName;
