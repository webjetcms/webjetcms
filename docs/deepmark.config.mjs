// deepmark.config.mjs

/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    //outputLanguages: ['en-US'],
    //outputLanguages: ['cs'],
    outputLanguages: ['en-US', 'cs'],
    directories: [
        ['sk', '$langcode$'],
        //['sk/sysadmin', '$langcode$/sysadmin'],
    ],
    //translationEngine: "deepl"
    translationEngine: "google"
};