const path = require("node:path");
const { Helper } = codeceptjs;
const { getEnvironmentOverride } = require("./elevenlabs_client.js");
const { generateHeadArtifacts } = require("./head_generation.js");

class HeadHelper extends Helper {
  constructor(config = {}) {
    super(config);
    this.options = { generationEnabled: false, ...config };
    this.started = false;
  }

  _beforeSuite(suite) {
    if (this.options.generationEnabled !== true) return;
    while (suite.parent) suite = suite.parent;
    const collect = current => (current.tests || []).filter(test => test.tags?.includes("@head"))
      .concat((current.suites || []).flatMap(collect));
    const tests = collect(suite);
    if (tests.length !== 1 || tests[0].title !== "ElevenLabs Head @head" || tests[0].tags.length !== 1) {
      throw new Error('Expected exactly one Scenario("ElevenLabs Head") tagged only @head.');
    }
    this.allowedTest = tests[0];
  }

  _test(test) { this.currentTest = test; }

  /**
   * Generates fresh talking-head clips for the localized head shots, without executing browser callbacks.
   * @param {object} plan Static video plan
   * @param {{language?: string, imagePath?: string, modelId?: string, resolution?: string, audio?: {modelId?: string, voiceId?: string}}} [options] Defaults overridden by each shot's head settings
   * @returns {Promise<Array<{id: string, audio: string, video: string}>>} Generated artifact paths in plan order
   */
  async generateHead(plan, options = {}) {
    if (this.options.generationEnabled !== true) throw new Error("Head generation is disabled. Use npm run head video/<scenario>.js.");
    if (!this.allowedTest || this.currentTest !== this.allowedTest) throw new Error("Head scenario did not pass the suite preflight check.");
    if (this.started) throw new Error("Only one generateHead call is allowed per head run.");
    this.started = true;
    const artifacts = await generateHeadArtifacts({
      plan, options, scenarioFile: this.currentTest.file, apiKey: getEnvironmentOverride("ELEVENLABS_API_KEY"),
      outputDirectory: this.options.featureVideoDirectory == null ? undefined : path.resolve(this.options.featureVideoDirectory)
    });
    this.currentTest.artifacts ||= {};
    for (const artifact of artifacts) this.currentTest.artifacts[`head-${artifact.id}`] = artifact.video;
    return artifacts;
  }
}

module.exports = HeadHelper;
