// deepmark.config.mjs

/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    outputLanguages: ['en-US', 'cs'],
    directories: [
        ['sk', '$langcode$'],
    ]
};