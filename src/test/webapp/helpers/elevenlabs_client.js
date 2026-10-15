const fs = require("node:fs/promises");
const path = require("node:path");
const { randomBytes } = require("node:crypto");
const DEFAULT_MODEL_ID = "eleven_v3";
const DEFAULT_VOICE_ID = "Zai7B4Aol2bJtneyq0L1";
const OUTPUT_FORMAT = "mp3_44100_128";
const REQUEST_TIMEOUT_MS = 600_000;
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

  const settings = {
    modelId: getOptionalOverride(options.modelId, "modelId") ||
      getEnvironmentOverride("ELEVENLABS_MODEL_ID") || DEFAULT_MODEL_ID,
    voiceId: getOptionalOverride(options.voiceId, "voiceId") ||
      getEnvironmentOverride("ELEVENLABS_VOICE_ID") || DEFAULT_VOICE_ID
  };
  if (options.languageCode != null) {
    settings.languageCode = getRequiredText(options.languageCode, "languageCode").toLowerCase();
    if (!/^[a-z]{2}$/.test(settings.languageCode)) {
      throw new Error("languageCode must be a two-letter ISO 639-1 code, such as sk.");
    }
  }
  if (options.voiceSettings != null) {
    if (typeof options.voiceSettings !== "object" || Array.isArray(options.voiceSettings)) {
      throw new Error("voiceSettings must be an object.");
    }
    settings.voiceSettings = {};
    for (const [name, value] of Object.entries(options.voiceSettings)) {
      if (!["stability", "similarityBoost"].includes(name)) {
        throw new Error(`Unsupported voiceSettings option: ${name}. Use stability or similarityBoost.`);
      }
      if (typeof value !== "number" || !Number.isFinite(value) || value < 0 || value > 1) {
        throw new Error(`voiceSettings.${name} must be a number between 0 and 1.`);
      }
      settings.voiceSettings[name] = value;
    }
    if (Object.keys(settings.voiceSettings).length === 0) delete settings.voiceSettings;
  }
  return settings;
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
  languageCode,
  voiceSettings,
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
          model_id: modelId,
          language_code: languageCode,
          voice_settings: voiceSettings == null ? undefined : {
            stability: voiceSettings.stability,
            similarity_boost: voiceSettings.similarityBoost
          }
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

/**
 * Reserves all part outputs before API calls, then generates and atomically saves them in order.
 * @param {object} options Request settings, prevalidated chunks and optional I/O replacements
 * @returns {Promise<string[]>} Saved MP3 paths in chunk order
 */
async function generateAudioArtifacts({
  chunks,
  apiKey,
  modelId,
  voiceId,
  languageCode,
  voiceSettings,
  fetchImpl = globalThis.fetch,
  fsImpl = fs,
  creditLabel,
  log = console.log
}) {
  const writes = [];
  let report;
  let totalCost = 0;
  let allCostsKnown = true;
  try {
    for (const chunk of chunks) {
      try {
        writes.push(await prepareAtomicWrite(chunk.targetPath, { fsImpl }));
        const existing = await fsImpl.stat(chunk.targetPath).catch(error => {
          if (error.code !== "ENOENT") throw error;
          return null;
        });
        if (existing && !existing.isFile()) throw new Error(`Audio output is not a regular file: ${chunk.targetPath}`);
      } catch (error) {
        const message = getSafeErrorDetail(error?.message) || "Unknown file system error";
        throw new Error(`Unable to prepare generated audio output: ${message}`);
      }
    }

    log(`[ElevenLabs audio] Settings: ${JSON.stringify({ modelId, voiceId, languageCode, voiceSettings })}`);
    chunks.forEach((chunk, index) => {
      log(`[ElevenLabs audio] Part ${index + 1}/${chunks.length} | ${Array.from(chunk.text).length} characters | Shots: ${chunk.shotIds?.join(", ") || "narration"} | Output: ${chunk.targetPath}`);
      log(`[ElevenLabs audio] Text to generate:\n${chunk.text}\n`);
    });
    report = creditLabel ? await require("./elevenlabs_credits.js").startCreditReport({
      apiKey, label: creditLabel, exactCost: true, fetchImpl, log
    }) : null;

    for (const [index, chunk] of chunks.entries()) {
      const label = `Audio part ${index + 1}/${chunks.length} (${path.basename(chunk.targetPath)})`;
      try {
        log(`[ElevenLabs audio] Generating part ${index + 1}/${chunks.length}.`);
        let cost = null;
        let audio;
        try {
          audio = await requestSpeechAudio({
            apiKey, text: chunk.text, modelId, voiceId, languageCode, voiceSettings, fetchImpl,
            onCost: value => { cost = value; }
          });
        } finally {
          if (cost == null) allCostsKnown = false;
          else totalCost += cost;
        }
        try {
          await commitAtomicWrite(writes[index], audio);
        } catch (error) {
          const message = getSafeErrorDetail(error?.message) || "Unknown file system error";
          throw new Error(`Unable to save generated audio: ${message}`);
        }
        log(`[ElevenLabs audio] Saved: ${chunk.targetPath}`);
      } catch (error) {
        throw new Error(`${label}: ${error.message}`, { cause: error });
      }
    }
    return chunks.map(chunk => chunk.targetPath);
  } finally {
    await Promise.all(writes.map(discardAtomicWrite));
    if (allCostsKnown) report?.recordCost(totalCost);
    await report?.finish();
  }
}

module.exports = { DEFAULT_MODEL_ID, DEFAULT_VOICE_ID, OUTPUT_FORMAT, REQUEST_TIMEOUT_MS, getRequiredText, getEnvironmentOverride, getAudioSettings, getSafeErrorDetail, readApiErrorDetail, requestSpeechAudio, prepareAtomicWrite, discardAtomicWrite, commitAtomicWrite, writeFileAtomically, generateAudioArtifacts };
