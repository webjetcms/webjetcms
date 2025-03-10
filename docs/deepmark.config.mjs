// deepmark.config.mjs

/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    outputLanguages: ['cs'],
    directories: [
        ['sk', '$langcode$'],
    ],
    translationEngine: "google"
};