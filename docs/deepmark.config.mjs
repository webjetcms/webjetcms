// deepmark.config.mjs

/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    outputLanguages: ['en-US'],
    //outputLanguages: ['cs'],
    directories: [
        //['sk', '$langcode$'],
        ['sk/frontend', '$langcode$/frontend'],
    ],
    //translationEngine: "deepl"
    translationEngine: "google"
};