// deepmark.config.mjs

/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    outputLanguages: ['en-US'],
    //outputLanguages: ['cs'],
    directories: [
        ['sk', '$langcode$'],
    ],
    translationEngine: "deepl"
    //translationEngine: "google"
};