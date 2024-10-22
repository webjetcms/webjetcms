const { I } = inject();

const sk = require("./i18n.sk");
const cs = require("./i18n.cs");


/**
 * Simple translator, default language is english
 */
module.exports = {

    /**
     * Translate text to current language.
     * You can use :: to separate different translations of same source text, eg:
     * Gallery->Galérie
     * Gallery::fbrowser->Galéria
     * @param {*} text - ENGLISH text as default,
     * @returns
     */
    get(text) {
        var translated = text;
        var lng = I.getConfLng();
        if(lng == "sk") {
            translated = sk.keys()[text];
        } else if(lng == "cs") {
            translated = cs.keys()[text];
        }

        if (typeof translated == "undefined" || translated == null) {
            translated = text;
        }

        //you can use :: to separate different translations of same source text
        if (translated != null && translated.indexOf("::") > -1) {
            translated = translated.split("::")[0];
        }

        return translated;
    },

    /**
     * Shortcut to I.click(i18n.get(text), context, options)
     * @param {*} text
     * @param {*} context
     * @param {*} options
     */
    click(text, context, options) {
        I.click(this.get(text), context, options);
    },

    /**
     * Shortcut to I.see(i18n.get(text), context)
     * @param {*} text
     * @param {*} context
     */
    see(text, context) {
        I.see(this.get(text), context);
    },

    /**
     * Shortcut to I.waitForText(i18n.get(text), sec, context)
     * @param {*} text
     * @param {*} sec
     * @param {*} context
     */
    waitForText(text, sec, context) {
        I.waitForText(this.get(text), sec, context);
    }

}