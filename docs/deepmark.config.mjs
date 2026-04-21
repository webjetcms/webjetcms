// deepmark.config.mjs

/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    outputLanguages: ['en-US'],
    //outputLanguages: ['cs'],
    directories: [
        //['sk', '$langcode$'],
        ['sk/custom-apps', '$langcode$/custom-apps'],
    ],
    //translationEngine: "deepl"
    translationEngine: "google"
};