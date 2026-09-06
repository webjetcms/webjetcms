const fs = require("node:fs/promises");
const path = require("node:path");
const { setTimeout: sleep } = require("node:timers/promises");
const { FEATURE_VIDEO_DIRECTORY } = require("./feature_video_paths.js");
const { getHeadShots } = require("./head_settings.js");
const { getRequiredText, readApiErrorDetail, requestSpeechAudio, prepareAtomicWrite, commitAtomicWrite, discardAtomicWrite } = require("./elevenlabs_client.js");
const { startCreditReport } = require("./elevenlabs_credits.js");

const VIDEO_API_URL = "https://api.elevenlabs.io/v1/flows/video";
const GENERATION_TIMEOUT_MS = 30 * 60_000;
const INLINE_LIMIT_BYTES = 25 * 1024 * 1024;

/** Detects supported image containers from their bytes rather than trusting a filename extension. */
function getImageMimeType(data) {
  if (data.length >= 24 && data.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])) &&
    data.toString("ascii", 12, 16) === "IHDR" && data.readUInt32BE(16) > 0 && data.readUInt32BE(20) > 0) return "image/png";
  if (data.length >= 4 && data[0] === 255 && data[1] === 216 && data[2] === 255) return "image/jpeg";
  if (data.length >= 20 && data.toString("ascii", 0, 4) === "RIFF" && data.toString("ascii", 8, 12) === "WEBP") return "image/webp";
  throw new Error("Head reference must be a non-empty PNG, JPEG or WebP image.");
}

function inlineReference(data, mimeType) {
  if (data.length === 0 || data.length > INLINE_LIMIT_BYTES) throw new Error("Inline media must contain between 1 byte and 25 MB.");
  return { type: "inline_base64", content_base64: data.toString("base64"), mime_type: mimeType };
}

/** Generates one lip-sync clip; POST is never retried and the generation ID survives later failures. */
async function requestHeadVideo({ apiKey, shot, image, audio, fetchImpl = globalThis.fetch,
  sleepImpl = sleep, now = Date.now, timeoutMs = GENERATION_TIMEOUT_MS, requestTimeoutMs = 120_000,
  onSubmitted = () => {}, onSettled = () => {}, log = console.log }) {
  const deadline = now() + timeoutMs;
  let generationId;
  const signal = () => {
    const remaining = deadline - now();
    if (remaining <= 0) throw new Error(`Video generation timed out after ${timeoutMs / 1000} seconds.`);
    return AbortSignal.timeout(Math.max(1, Math.ceil(Math.min(requestTimeoutMs, remaining))));
  };
  const jsonRequest = async (url, options) => {
    const response = await fetchImpl(url, { ...options, headers: {
      "xi-api-key": apiKey, Accept: "application/json", "Content-Type": "application/json"
    }, redirect: "error", signal: signal() });
    if (!response.ok) {
      if (options.method === "POST" && [400, 401, 403, 404, 422, 429].includes(response.status)) onSettled();
      throw new Error(`ElevenLabs video request failed with HTTP ${response.status}: ${await readApiErrorDetail(response)}`);
    }
    return response.json();
  };
  try {
    const body = { model_id: shot.modelId, resolution: shot.resolution, image, audio: inlineReference(audio, "audio/mpeg") };
    onSubmitted();
    const created = await jsonRequest(VIDEO_API_URL, { method: "POST", body: JSON.stringify(body) });
    generationId = getRequiredText(created.id, "Video generation id");
    log(`Head shot ${shot.id}: generation ${generationId}`);
    if (created.status !== "pending") throw new Error(`Unexpected initial video status: ${created.status}`);
    let interval = 10_000;
    while (true) {
      const remaining = deadline - now();
      if (remaining <= 0) throw new Error(`Video generation timed out after ${timeoutMs / 1000} seconds.`);
      await sleepImpl(Math.min(interval, remaining));
      const result = await jsonRequest(`${VIDEO_API_URL}/${encodeURIComponent(generationId)}`, { method: "GET" });
      if (result.id !== generationId) throw new Error("Video response generation id does not match the submitted job.");
      if (result.status === "failed") {
        onSettled();
        throw new Error(`Video generation failed: ${result.failure_reason}: ${result.error_message}`);
      }
      if (result.status === "completed") {
        onSettled();
        if (result.content_mime_type !== "video/mp4") throw new Error("Video generation did not return video/mp4.");
        const url = new URL(result.content_url);
        if (url.protocol !== "https:") throw new Error("Video download URL must use HTTPS.");
        const response = await fetchImpl(url.href, { signal: signal(), redirect: "error" });
        if (!response.ok) throw new Error(`Video download failed with HTTP ${response.status}.`);
        if (response.headers.get("content-type")?.split(";", 1)[0].trim().toLowerCase() !== "video/mp4") {
          throw new Error("Video download did not return video/mp4.");
        }
        const video = Buffer.from(await response.arrayBuffer());
        if (video.length < 12 || video.toString("ascii", 4, 8) !== "ftyp") throw new Error("Video download is empty or is not an MP4 container.");
        return video;
      }
      if (!["pending", "generating"].includes(result.status)) throw new Error(`Unexpected video status: ${result.status}`);
      interval = Math.min(interval * 2, 60_000);
    }
  } catch (error) {
    const message = error.name === "TimeoutError" ? "Video request timed out." : error.message;
    throw new Error(`Head shot ${shot.id}${generationId ? ` (generation ${generationId})` : ""}: ${message}`, { cause: error });
  }
}

/** Validates and reserves all outputs, then generates fresh MP3/MP4 files sequentially with one credit summary. */
async function generateHeadArtifacts({ plan, options = {}, scenarioFile, apiKey, outputDirectory = FEATURE_VIDEO_DIRECTORY,
  fetchImpl = globalThis.fetch, fsImpl = fs, sleepImpl, now, timeoutMs, requestTimeoutMs, log = console.log }) {
  getRequiredText(scenarioFile, "Head scenario file");
  const resolved = getHeadShots(plan, options, scenarioFile);
  if (resolved.shots.length === 0) {
    log("No head shots in this video plan. Nothing to generate.");
    return [];
  }
  apiKey = getRequiredText(apiKey, "ELEVENLABS_API_KEY");
  const images = new Map();
  for (const shot of resolved.shots) {
    if (!images.has(shot.imagePath)) {
      const stat = await fsImpl.stat(shot.imagePath);
      if (!stat.isFile() || stat.size > INLINE_LIMIT_BYTES) throw new Error(`Invalid head image: ${shot.imagePath} (maximum 25 MB).`);
      const data = await fsImpl.readFile(shot.imagePath);
      images.set(shot.imagePath, inlineReference(data, getImageMimeType(data)));
    }
  }
  const writes = [];
  let report;
  let provisional = false;
  try {
    const basename = path.basename(scenarioFile, ".js");
    for (const shot of resolved.shots) {
      const stem = path.join(outputDirectory, `${basename}-${shot.id}-${resolved.language}`);
      for (const extension of ["mp3", "mp4"]) {
        const target = `${stem}.${extension}`;
        const existing = await fsImpl.stat(target).catch(error => {
          if (error.code !== "ENOENT") throw error;
          return null;
        });
        if (existing && !existing.isFile()) throw new Error(`Head output is not a regular file: ${target}`);
      }
      shot.audioWrite = await prepareAtomicWrite(`${stem}.mp3`, { fsImpl });
      writes.push(shot.audioWrite);
      shot.videoWrite = await prepareAtomicWrite(`${stem}.mp4`, { fsImpl });
      writes.push(shot.videoWrite);
    }
    report = await startCreditReport({ apiKey, label: "head", fetchImpl, log });
    const artifacts = [];
    for (const shot of resolved.shots) {
      log(`Generating head shot ${shot.number} [${shot.id}] (${resolved.language}, ${shot.modelId}, ${shot.resolution}).`);
      const audio = await requestSpeechAudio({ apiKey, text: shot.narration, ...shot.audio, fetchImpl });
      await commitAtomicWrite(shot.audioWrite, audio);
      const video = await requestHeadVideo({ apiKey, shot, image: images.get(shot.imagePath), audio,
        fetchImpl, sleepImpl, now, timeoutMs, requestTimeoutMs, log,
        onSubmitted: () => { provisional = true; }, onSettled: () => { provisional = false; }
      });
      await commitAtomicWrite(shot.videoWrite, video);
      artifacts.push({ id: shot.id, audio: shot.audioWrite.targetPath, video: shot.videoWrite.targetPath });
      log(`Saved head video: ${shot.videoWrite.targetPath}`);
    }
    return artifacts;
  } finally {
    await Promise.all(writes.map(discardAtomicWrite));
    await report?.finish({ provisional });
  }
}

module.exports = { VIDEO_API_URL, GENERATION_TIMEOUT_MS, getImageMimeType, inlineReference, requestHeadVideo, generateHeadArtifacts };
