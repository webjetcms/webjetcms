const path = require("node:path");
const { resolveVideoPlan } = require("./feature_video_plan.js");
const { getAudioSettings, getRequiredText } = require("./elevenlabs_client.js");

const DEFAULT_HEAD_MODEL_ID = "creatify-aurora";
const DEFAULT_HEAD_IMAGE = path.resolve(__dirname, "../video/assets/head/jack-home-vlog-style.png");

/** Validates literal helper/shot options before any paid request. */
function validateHeadOptions(options = {}, allowLanguage = true) {
  if (!options || typeof options !== "object" || Array.isArray(options)) throw new Error("Head options must be an object.");
  const names = ["imagePath", "modelId", "resolution", "audio", ...(allowLanguage ? ["language"] : [])];
  for (const [name, value] of Object.entries(options)) {
    if (!names.includes(name)) throw new Error(`Unknown head option: ${name}`);
    if (name === "audio") {
      if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("Head audio options must be an object.");
      for (const [key, setting] of Object.entries(value)) {
        if (!["modelId", "voiceId"].includes(key)) throw new Error(`Unknown head audio option: ${key}`);
        getRequiredText(setting, `audio.${key}`);
      }
    } else getRequiredText(value, name);
  }
  if (options.resolution !== undefined && !["480p", "720p"].includes(options.resolution)) {
    throw new Error("Head resolution must be 480p or 720p.");
  }
  return options;
}

/** Resolves head shots in plan order, retaining all localized plan validation without invoking callbacks. */
function getHeadShots(plan, options = {}, scenarioFile) {
  validateHeadOptions(options);
  const { language, shots } = resolveVideoPlan(plan, options.language);
  const heads = shots.filter(shot => shot.type === "head").map(shot => {
    if (!shot.narration) throw new Error(`Head shot ${shot.id} narration must not be empty.`);
    const overrides = validateHeadOptions(shot.head, false);
    const settings = { ...options, ...overrides };
    const imagePath = settings.imagePath == null ? DEFAULT_HEAD_IMAGE : getRequiredText(settings.imagePath, "imagePath");
    return {
      ...shot,
      imagePath: scenarioFile ? path.resolve(path.dirname(scenarioFile), imagePath) : imagePath,
      modelId: settings.modelId?.trim() || DEFAULT_HEAD_MODEL_ID,
      resolution: settings.resolution || "720p",
      audio: getAudioSettings({ ...options.audio, ...overrides.audio })
    };
  });
  return { language, shots: heads };
}

module.exports = { DEFAULT_HEAD_MODEL_ID, DEFAULT_HEAD_IMAGE, validateHeadOptions, getHeadShots };
