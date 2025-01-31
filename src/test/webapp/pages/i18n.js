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
     * Formats a date from the American format (MM/DD/YYYY) to the Slovak or Czech format (DD.MM.YYYY),
     * based on the currently selected language.
     *
     * @param {string} date - The date string in the American format MM/DD/YYYY.
     * @returns {string} - The date formatted as DD.MM.YYYY if the language is Slovak (sk) or Czech (cs),
     *                     or the original date string if the language is not supported or the format does not match.
     * Example:
     * getDate("07/30/2021") -> "30.07.2021" (if language is SK or CS)
     * getDate("07/30/2021") -> "07/30/2021" (if language is not SK or CS)
     */
    getDate(date) {
        var lng = I.getConfLng();
        if (lng == "sk" || lng == "cs") {
            let convertedDate = date;
            const regex = /^(\d{2})\/(\d{2})\/(\d{4})$/;
            if (regex.test(date)) {
                convertedDate = date.replace(regex, (match, month, day, year) => {
                    return `${day}.${month}.${year}`;
                });
            }
            return convertedDate;
        }
        return date;
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
    },

    /**
     * Shorcut to I.selectOption(context, this.get(text));
     * @param {*} text
     * @param {*} context
     */
    selectOption(context, text){
        I.selectOption(context, this.get(text));
    },

    /**
     * Shorcut to I.fillField(context, this.get(text));
     * @param {*} text
     * @param {*} context
     */
    fillField(context, text){
        I.fillField(context, this.get(text));
    }
}