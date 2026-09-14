const path = require("node:path");
const { FEATURE_VIDEO_DIRECTORY } = require("./feature_video_paths.js");
const { resolveAudioPlan, normalizeNarration } = require("./audio_plan.js");
const client = require("./elevenlabs_client.js");
const { getRequiredText, getEnvironmentOverride, generateAudioArtifacts } = client;
const { Helper } = codeceptjs;
const AUDIO_SCENARIO_TITLE = "ElevenLabs @audio";
const AUDIO_TAG = "@audio";

function getAudioArtifactName(test, part = 1, language = null) {
  const scenarioFile = typeof test?.file === "string" ? test.file.trim() : "";
  if (scenarioFile === "") {
    throw new Error("Unable to determine the audio artifact name because the scenario file is missing.");
  }

  const extension = path.extname(scenarioFile);
  return `${path.basename(scenarioFile, extension)}${language == null ? "" : `-${language}`}-${part}.mp3`;
}

function getAudioArtifactPath(test, outputDirectory = FEATURE_VIDEO_DIRECTORY, part = 1, language = null) {
  return path.join(outputDirectory, getAudioArtifactName(test, part, language));
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

class AudioHelper extends Helper {

  constructor(config = {}) {
    super(config);
    this.options = {
      generationEnabled: false,
      ...config
    };
    this.featureVideoDirectory = config.featureVideoDirectory == null
      ? FEATURE_VIDEO_DIRECTORY
      : path.resolve(getRequiredText(config.featureVideoDirectory, "featureVideoDirectory"));
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
   * Generates numbered MP3 parts, keeping every shot together within the model's character limit.
   * @param {string|object} text Legacy narration or a video plan with localized shot text; callbacks are not executed
   * @param {{modelId?: string, voiceId?: string, language?: string}} [options] Voice and language overrides
   * @returns {Promise<string[]>} Absolute paths of the generated MP3 parts in narration order
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
      throw new Error("Only one generateAudio call is allowed per audio run.");
    }
    this.audioGenerationStarted = true;

    const { modelId, voiceId, language, chunks } = resolveAudioPlan(text, options);
    const apiKey = getEnvironmentOverride("ELEVENLABS_API_KEY");
    if (apiKey == null) {
      throw new Error("ELEVENLABS_API_KEY must be set before generating audio.");
    }

    const paths = await generateAudioArtifacts({
      chunks: chunks.map((chunk, index) => ({
        ...chunk,
        targetPath: getAudioArtifactPath(this.audioTest, this.featureVideoDirectory, index + 1, language)
      })),
      apiKey,
      modelId,
      voiceId,
      creditLabel: "audio"
    });

    if (this.audioTest.artifacts == null) this.audioTest.artifacts = {};
    paths.forEach((targetPath, index) => { this.audioTest.artifacts[`audio-${index + 1}`] = targetPath; });
    return paths;
  }
}

module.exports = AudioHelper;
Object.assign(module.exports, client, {
  assertAudioTestIdentity, getRegisteredAudioTests, getAudioArtifactName, getAudioArtifactPath, normalizeNarration, resolveAudioPlan
});
