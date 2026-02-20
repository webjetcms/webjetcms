// deepmark.config.mjs

/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    //outputLanguages: ['en-US'],
    outputLanguages: ['cs'],
    directories: [
        //['sk', '$langcode$'],
        ['sk/translation-test', '$langcode$/translation-test'],
    ],
    //translationEngine: "deepl"
    translationEngine: "google"
};