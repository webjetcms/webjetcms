const fs = require("node:fs/promises");
const path = require("node:path");
const { randomBytes } = require("node:crypto");
const DEFAULT_MODEL_ID = "eleven_v3";
const DEFAULT_VOICE_ID = "Zai7B4Aol2bJtneyq0L1";
const OUTPUT_FORMAT = "mp3_44100_128";
const REQUEST_TIMEOUT_MS = 120_000;
const API_BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech";

class ElevenLabsApiError extends Error {}

function getRequiredText(value, name) {
  if (typeof value !== "string" || value.trim() === "") {
    throw new Error(`${name} must be a non-empty string.`);
  }
  return value.trim();
}

function getOptionalOverride(value, name) {
  if (value == null) return null;
  return getRequiredText(value, name);
}

function getEnvironmentOverride(name) {
  const value = process.env[name];
  return typeof value === "string" && value.trim() !== "" ? value.trim() : null;
}

function getAudioSettings(options = {}) {
  if (options == null || typeof options !== "object" || Array.isArray(options)) {
    throw new Error("Audio generation options must be an object.");
  }

  return {
    modelId: getOptionalOverride(options.modelId, "modelId") ||
      getEnvironmentOverride("ELEVENLABS_MODEL_ID") || DEFAULT_MODEL_ID,
    voiceId: getOptionalOverride(options.voiceId, "voiceId") ||
      getEnvironmentOverride("ELEVENLABS_VOICE_ID") || DEFAULT_VOICE_ID
  };
}

function getSafeErrorDetail(value) {
  if (typeof value !== "string") return "";
  return value.replace(/\s+/g, " ").trim().slice(0, 500);
}

async function readApiErrorDetail(response) {
  let body;
  try {
    body = await response.text();
  } catch {
    return "";
  }

  const fallback = getSafeErrorDetail(body);
  if (fallback === "") return "";

  try {
    const parsed = JSON.parse(body);
    const detail = parsed?.detail;
    if (typeof detail === "string") return getSafeErrorDetail(detail);
    if (typeof detail?.message === "string") return getSafeErrorDetail(detail.message);
    if (typeof parsed?.message === "string") return getSafeErrorDetail(parsed.message);
    if (typeof parsed?.error?.message === "string") return getSafeErrorDetail(parsed.error.message);
  } catch {
    // A plain-text response is still useful when ElevenLabs or a proxy rejects the request.
  }
  return fallback;
}

async function requestSpeechAudio({
  apiKey,
  text,
  modelId,
  voiceId,
  fetchImpl = globalThis.fetch,
  timeoutMs = REQUEST_TIMEOUT_MS,
  onCost
}) {
  if (typeof fetchImpl !== "function") {
    throw new Error("This Node.js version does not provide the fetch API required for audio generation.");
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  timeout.unref?.();

  try {
    const response = await fetchImpl(
      `${API_BASE_URL}/${encodeURIComponent(voiceId)}?output_format=${OUTPUT_FORMAT}`,
      {
        method: "POST",
        headers: {
          Accept: "audio/mpeg",
          "Content-Type": "application/json",
          "xi-api-key": apiKey
        },
        body: JSON.stringify({
          text,
          model_id: modelId
        }),
        redirect: "error",
        signal: controller.signal
      }
    );

    if (!response.ok) {
      const detail = await readApiErrorDetail(response);
      const suffix = detail === "" ? "" : `: ${detail}`;
      throw new ElevenLabsApiError(
        `ElevenLabs text-to-speech request failed with HTTP ${response.status}${suffix}`
      );
    }

    const costHeader = response.headers.get("character-cost");
    if (costHeader != null && costHeader.trim() !== "" && Number.isFinite(Number(costHeader)) && Number(costHeader) >= 0) {
      onCost?.(Number(costHeader));
    }

    const contentType = response.headers.get("content-type")
      ?.split(";", 1)[0]
      .trim()
      .toLowerCase();
    if (contentType !== "audio/mpeg") {
      const received = contentType == null || contentType === "" ? "missing" : contentType;
      throw new ElevenLabsApiError(
        `ElevenLabs returned an unexpected Content-Type (${received}) instead of audio/mpeg.`
      );
    }

    const audio = Buffer.from(await response.arrayBuffer());
    if (audio.length === 0) {
      throw new ElevenLabsApiError("ElevenLabs returned an empty audio response.");
    }
    return audio;
  } catch (error) {
    if (controller.signal.aborted) {
      throw new Error(`ElevenLabs text-to-speech request timed out after ${timeoutMs / 1000} seconds.`);
    }
    if (error instanceof ElevenLabsApiError) throw error;
    const message = getSafeErrorDetail(error?.message) || "Unknown network error";
    throw new Error(`ElevenLabs text-to-speech request failed: ${message}`);
  } finally {
    clearTimeout(timeout);
  }
}

async function prepareAtomicWrite(targetPath, options = {}) {
  const fsImpl = options.fsImpl || fs;
  const suffix = options.suffix || `${process.pid}-${randomBytes(8).toString("hex")}`;
  const temporaryPath = `${targetPath}.${suffix}.tmp`;

  await fsImpl.mkdir(path.dirname(targetPath), { recursive: true });
  try {
    await fsImpl.writeFile(temporaryPath, Buffer.alloc(0), { flag: "wx" });
  } catch (error) {
    if (error?.code !== "EEXIST") {
      await fsImpl.rm(temporaryPath, { force: true }).catch(() => {});
    }
    throw error;
  }

  return { fsImpl, targetPath, temporaryPath };
}

async function discardAtomicWrite(preparedWrite) {
  await preparedWrite.fsImpl.rm(preparedWrite.temporaryPath, { force: true }).catch(() => {});
}

async function commitAtomicWrite(preparedWrite, data) {
  await preparedWrite.fsImpl.writeFile(preparedWrite.temporaryPath, data);
  await preparedWrite.fsImpl.rename(preparedWrite.temporaryPath, preparedWrite.targetPath);
}

async function writeFileAtomically(targetPath, data, options = {}) {
  const preparedWrite = await prepareAtomicWrite(targetPath, options);
  try {
    await commitAtomicWrite(preparedWrite, data);
  } finally {
    await discardAtomicWrite(preparedWrite);
  }
}

async function generateAudioArtifact({
  targetPath,
  apiKey,
  text,
  modelId,
  voiceId,
  fetchImpl = globalThis.fetch,
  fsImpl = fs,
  creditLabel
}) {
  let preparedWrite;
  try {
    preparedWrite = await prepareAtomicWrite(targetPath, { fsImpl });
  } catch (error) {
    const message = getSafeErrorDetail(error?.message) || "Unknown file system error";
    throw new Error(`Unable to prepare generated audio output: ${message}`);
  }

  const report = creditLabel ? await require("./elevenlabs_credits.js").startCreditReport({
    apiKey, label: creditLabel, exactCost: true, fetchImpl
  }) : null;
  try {
    const audio = await requestSpeechAudio({
      apiKey,
      text,
      modelId,
      voiceId,
      fetchImpl,
      onCost: cost => report?.recordCost(cost)
    });

    try {
      await commitAtomicWrite(preparedWrite, audio);
    } catch (error) {
      const message = getSafeErrorDetail(error?.message) || "Unknown file system error";
      throw new Error(`Unable to save generated audio: ${message}`);
    }
  } finally {
    await discardAtomicWrite(preparedWrite);
    await report?.finish();
  }
}

module.exports = { DEFAULT_MODEL_ID, DEFAULT_VOICE_ID, OUTPUT_FORMAT, REQUEST_TIMEOUT_MS, getRequiredText, getEnvironmentOverride, getAudioSettings, getSafeErrorDetail, readApiErrorDetail, requestSpeechAudio, prepareAtomicWrite, discardAtomicWrite, commitAtomicWrite, writeFileAtomically, generateAudioArtifact };
