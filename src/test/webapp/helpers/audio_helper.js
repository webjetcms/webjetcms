const fs = require("node:fs/promises");
const path = require("node:path");
const { randomBytes } = require("node:crypto");

const { Helper } = codeceptjs;

const DEFAULT_MODEL_ID = "eleven_multilingual_v2";
const DEFAULT_VOICE_ID = "Zai7B4Aol2bJtneyq0L1";
const OUTPUT_FORMAT = "mp3_44100_128";
const REQUEST_TIMEOUT_MS = 120_000;
const API_BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech";
const AUDIO_SCENARIO_TITLE = "ElevenLabs @audio";
const AUDIO_TAG = "@audio";

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

function normalizeNarration(text) {
  return getRequiredText(text, "Audio narration").replace(/\r\n?/g, "\n");
}

function getAudioArtifactName(test) {
  const scenarioFile = typeof test?.file === "string" ? test.file.trim() : "";
  if (scenarioFile === "") {
    throw new Error("Unable to determine the audio artifact name because the scenario file is missing.");
  }

  const extension = path.extname(scenarioFile);
  return `${path.basename(scenarioFile, extension)}.mp3`;
}

function assertAudioTestIdentity(test) {
  const hasOnlyAudioTag = Array.isArray(test?.tags) && test.tags.length === 1 &&
    test.tags[0] === AUDIO_TAG;
  if (test?.title !== AUDIO_SCENARIO_TITLE || !hasOnlyAudioTag) {
    throw new Error(
      `Audio generation is allowed only in Scenario("ElevenLabs") with the ${AUDIO_TAG} tag.`
    );
  }
}

function getRegisteredAudioTests(suite) {
  const tests = Array.isArray(suite?.tests) ? suite.tests : [];
  const childSuites = Array.isArray(suite?.suites) ? suite.suites : [];
  return tests
    .filter((test) => Array.isArray(test.tags) && test.tags.includes(AUDIO_TAG))
    .concat(childSuites.flatMap(getRegisteredAudioTests));
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
  timeoutMs = REQUEST_TIMEOUT_MS
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
  fsImpl = fs
}) {
  let preparedWrite;
  try {
    preparedWrite = await prepareAtomicWrite(targetPath, { fsImpl });
  } catch (error) {
    const message = getSafeErrorDetail(error?.message) || "Unknown file system error";
    throw new Error(`Unable to prepare generated audio output: ${message}`);
  }

  try {
    const audio = await requestSpeechAudio({
      apiKey,
      text,
      modelId,
      voiceId,
      fetchImpl
    });

    try {
      await commitAtomicWrite(preparedWrite, audio);
    } catch (error) {
      const message = getSafeErrorDetail(error?.message) || "Unknown file system error";
      throw new Error(`Unable to save generated audio: ${message}`);
    }
  } finally {
    await discardAtomicWrite(preparedWrite);
  }
}

class AudioHelper extends Helper {

  constructor(config = {}) {
    super(config);
    this.options = {
      generationEnabled: false,
      ...config
    };
    this.audioGenerationStarted = false;
    this.audioSuiteValidated = false;
    this.allowedAudioTest = null;
  }

  _beforeSuite(suite) {
    if (this.options.generationEnabled !== true) return;

    let rootSuite = suite;
    while (rootSuite?.parent != null) rootSuite = rootSuite.parent;
    const audioTests = getRegisteredAudioTests(rootSuite);
    if (audioTests.length !== 1) {
      throw new Error(
        `Expected exactly one registered ${AUDIO_TAG} scenario, found ${audioTests.length}.`
      );
    }
    assertAudioTestIdentity(audioTests[0]);
    this.allowedAudioTest = audioTests[0];
    this.audioSuiteValidated = true;
  }

  _test(test) {
    this.audioTest = test;
  }

  /**
   * Generates an MP3 narration for the current scenario file with ElevenLabs.
   * @param {string} text Narration sent to ElevenLabs
   * @param {{modelId?: string, voiceId?: string}} [options] Per-scenario model and voice overrides
   * @returns {Promise<string>} Absolute path of the generated MP3 file
   * @throws {Error} When generation is disabled, configuration is invalid, or generation fails
   */
  async generateAudio(text, options = {}) {
    if (this.options.generationEnabled !== true) {
      throw new Error(
        "Audio generation is disabled. Use npm run audio video/<scenario>.js to enable it safely."
      );
    }
    if (this.audioTest == null) {
      throw new Error("Audio generation must run inside a CodeceptJS scenario.");
    }
    if (!this.audioSuiteValidated || this.audioTest !== this.allowedAudioTest) {
      throw new Error("The current scenario did not pass the audio suite preflight check.");
    }
    assertAudioTestIdentity(this.audioTest);
    if (this.audioGenerationStarted) {
      throw new Error("Only one audio file can be generated per audio run.");
    }
    this.audioGenerationStarted = true;

    const narration = normalizeNarration(text);
    const { modelId, voiceId } = getAudioSettings(options);
    const apiKey = getEnvironmentOverride("ELEVENLABS_API_KEY");
    if (apiKey == null) {
      throw new Error("ELEVENLABS_API_KEY must be set before generating audio.");
    }

    const artifactName = getAudioArtifactName(this.audioTest);
    if (typeof global.output_dir !== "string" || global.output_dir.trim() === "") {
      throw new Error("CodeceptJS output directory is not configured.");
    }
    const targetPath = path.join(global.output_dir, "videos", artifactName);
    await generateAudioArtifact({
      targetPath,
      apiKey,
      text: narration,
      modelId,
      voiceId
    });

    if (this.audioTest.artifacts == null) this.audioTest.artifacts = {};
    this.audioTest.artifacts.audio = targetPath;
    return targetPath;
  }
}

module.exports = AudioHelper;
module.exports.DEFAULT_MODEL_ID = DEFAULT_MODEL_ID;
module.exports.DEFAULT_VOICE_ID = DEFAULT_VOICE_ID;
module.exports.OUTPUT_FORMAT = OUTPUT_FORMAT;
module.exports.REQUEST_TIMEOUT_MS = REQUEST_TIMEOUT_MS;
module.exports.assertAudioTestIdentity = assertAudioTestIdentity;
module.exports.getRegisteredAudioTests = getRegisteredAudioTests;
module.exports.getAudioArtifactName = getAudioArtifactName;
module.exports.getAudioSettings = getAudioSettings;
module.exports.normalizeNarration = normalizeNarration;
module.exports.prepareAtomicWrite = prepareAtomicWrite;
module.exports.requestSpeechAudio = requestSpeechAudio;
module.exports.generateAudioArtifact = generateAudioArtifact;
module.exports.writeFileAtomically = writeFileAtomically;
