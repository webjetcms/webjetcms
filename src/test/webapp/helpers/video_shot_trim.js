const fs = require("node:fs/promises");
const path = require("node:path");
const { execFile } = require("node:child_process");
const { promisify } = require("node:util");

const FRAME_RATE = 25;
const TIMING_PATCH = Symbol.for("webjet.video.shot-timing");
const recordings = new Map();

/** Tracks Chromium's actual encoded frame positions, including rounding of repeated static frames. */
function installShotTiming(VideoRecorder) {
  if (VideoRecorder.prototype[TIMING_PATCH]) return;
  const launch = VideoRecorder.prototype._launch;
  const writeFrame = VideoRecorder.prototype.writeFrame;
  VideoRecorder.prototype._launch = async function(options) {
    await launch.call(this, options);
    this.shotTiming = { frames: [], frameCount: 0 };
    recordings.set(options.outputFile, this.shotTiming);
  };
  VideoRecorder.prototype.writeFrame = function(frame, timestamp) {
    if (!this._isStopped && this.shotTiming) {
      if (this._lastFrameBuffer) {
        this.shotTiming.frameCount += Math.max(1, Math.round(FRAME_RATE * (timestamp - this._lastFrameTimestamp)));
      }
      if (frame.length) this.shotTiming.frames.push({ timestamp, index: this.shotTiming.frameCount });
    }
    return writeFrame.call(this, frame, timestamp);
  };
  VideoRecorder.prototype[TIMING_PATCH] = true;
}

/** Removes setup at a frame boundary and keeps the original until the caller atomically publishes the result. */
async function trimVideoSetup(sourcePath, { startTime, endTime }, options = {}) {
  const timing = recordings.get(sourcePath);
  // Initial screencast frames can arrive after the paint callback; use the final static slate frame.
  // A static slate can also be captured just before the callback, with no further frames until removal.
  const start = timing?.frames.findLast(frame => frame.timestamp >= startTime && frame.timestamp < endTime) ||
    timing?.frames.findLast(frame => frame.timestamp <= startTime);
  if (!start) throw new Error("Cannot locate the shot slate in the Chromium recording; raw video retained.");
  const coreRoot = path.dirname(require.resolve("playwright-core/package.json"));
  const { registry } = require(path.join(coreRoot, "lib/server/registry/index.js"));
  const ffmpeg = registry.findExecutable("ffmpeg").executablePathOrDie("javascript");
  const trimmedPath = sourcePath.replace(/\.webm$/, ".trimmed.webm");
  try {
    await (options.execFile || promisify(execFile))(ffmpeg, [
      "-hide_banner", "-loglevel", "error", "-i", sourcePath,
      "-ss", String(start.index / FRAME_RATE), "-an", "-c:v", "vp8",
      "-qmin", "0", "-qmax", "4", "-crf", "0", "-b:v", "50M",
      "-deadline", "realtime", "-speed", "8", "-threads", "1", "-y", trimmedPath
    ]);
    return trimmedPath;
  } catch (error) {
    await fs.rm(trimmedPath, { force: true });
    throw error;
  } finally {
    recordings.delete(sourcePath);
  }
}

module.exports = { installShotTiming, trimVideoSetup };
