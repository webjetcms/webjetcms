const { resolveVideoPlan } = require("./feature_video_plan.js");
const { DEFAULT_MODEL_ID, getAudioSettings, getRequiredText } = require("./elevenlabs_client.js");

// https://elevenlabs.io/docs/overview/models#character-limits
const MODEL_CHARACTER_LIMITS = {
  eleven_v3: 5000,
  eleven_multilingual_v2: 10000,
  eleven_multilingual_v1: 10000,
  eleven_flash_v2_5: 40000,
  eleven_flash_v2: 30000,
  eleven_turbo_v2_5: 40000,
  eleven_turbo_v2: 30000
};

function normalizeNarration(text) {
  return getRequiredText(text, "Audio narration").replace(/\r\n?/g, "\n");
}

/**
 * Packs complete localized shots into sequential TTS requests without executing callbacks.
 * @param {string|object} input Legacy narration or a video plan
 * @param {{modelId?: string, voiceId?: string, language?: string}} [options] Model, voice and language overrides
 * @returns {object} Resolved settings, language, character limit and chunks with text and shot IDs
 * @throws {Error} When narration is empty or an individual shot exceeds the model limit
 */
function resolveAudioPlan(input, options = {}) {
  const settings = getAudioSettings(options);
  const characterLimit = Object.hasOwn(MODEL_CHARACTER_LIMITS, settings.modelId)
    ? MODEL_CHARACTER_LIMITS[settings.modelId] : MODEL_CHARACTER_LIMITS[DEFAULT_MODEL_ID];
  const legacy = typeof input === "string";
  const { language, shots } = legacy
    ? { language: null, shots: [{ id: "narration", narration: normalizeNarration(input) }] }
    : resolveVideoPlan(input, options.language);
  const chunks = [];
  for (const shot of shots) {
    if (shot.narration === "") continue;
    const characters = Array.from(shot.narration).length;
    if (characters > characterLimit) {
      const label = legacy ? "Legacy narration" : `Shot ${shot.id}`;
      throw new Error(`${label} has ${characters} characters, exceeding the ${characterLimit}-character limit for ${settings.modelId}. Split it into smaller shots; audio never splits a shot.`);
    }
    let chunk = chunks.at(-1);
    if (!chunk || chunk.characters + 2 + characters > characterLimit) {
      chunk = { text: shot.narration, characters, shotIds: [shot.id] };
      chunks.push(chunk);
    } else {
      chunk.text += `\n\n${shot.narration}`;
      chunk.characters += 2 + characters;
      chunk.shotIds.push(shot.id);
    }
  }
  if (chunks.length === 0) throw new Error("Video plan narration must not be empty.");
  return { ...settings, language, characterLimit, chunks };
}

module.exports = { resolveAudioPlan, normalizeNarration };
