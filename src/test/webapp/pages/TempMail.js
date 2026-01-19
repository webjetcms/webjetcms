const { I } = inject();

const fexPost = require("./TempMail-fexpost");
const mailSac = require("./TempMail-mailsac");
const noopmail = require("./TempMail-noopmail");
const verify32 = require("./TempMail-verify32");

/**
 * Funkcie pre pracu s https://tempmail.plus
 */

module.exports = {

    /**
     * If you change TempMail provider, run this SQL to update users in database:
     *
     * UPDATE users SET email = REPLACE(email, '@fexpost.com', '@mailsac.com') WHERE email LIKE '%@fexpost.com';
     * UPDATE users SET email = REPLACE(email, '@mailsac.com', '@noopmail.com') WHERE email LIKE '%@mailsac.com';
     * UPDATE users SET email = REPLACE(email, '@noopmail.com', '@tempverify.com') WHERE email LIKE '%@noopmail.com';
     * UPDATE users SET email = REPLACE(email, '@tempverify.com', '@noopmail.com') WHERE email LIKE '%@tempverify.com';
     *
     * UPDATE form_attributes SET value = REPLACE(value, '@fexpost.com', '@noopmail.com') WHERE value LIKE '%@fexpost.com';
     *
     * @returns TempMail provider implementation
     */
    getTempmailProvider() {
        var provider = process.env.CODECEPT_TEMPMAIL_PROVIDER;
        if (provider === "fexpost") {
            return fexPost;
        } else if (provider === "mailsac") {
            return mailSac;
        } else if (provider === "noopmail") {
            return noopmail;
        } else if (provider === "verify32") {
            return verify32;
        }
        //default
        return noopmail;
    },

    /**
     * @returns domain name with @ sign, e.g. "@fexpost.com"
     */
    getTempMailDomain() {
        var provider = this.getTempmailProvider();
        if (provider && provider.getTempMailDomain) {
            return provider.getTempMailDomain();
        }
        return "@fexpost.com";
    },

    /**
     * Prihlási sa do TempMail a zobrazí e-maily.
     * @param {string} name - Názov e-mailového účtu.
     * @param {string} [emailDomain="fexpost.com"] - Doména e-mailového účtu, predvolene "fexpost.com".
     */
    async login(name, emailDomain){
        var provider = this.getTempmailProvider();
        if (provider) {
            if (typeof emailDomain === 'undefined') {
                emailDomain = provider.getTempMailDomain().substring(1); //remove leading '@'
            }

            return provider.login(name, emailDomain);
        }
    },

    /**
     * Otvorí najnovší email v inboxe.
     * Je potrebné zavolať TempMail.login() predtým
     */
    openLatestEmail(){
        var provider = this.getTempmailProvider();
        if (provider) {
            return provider.openLatestEmail();
        }
    },

    /**
     * Skontroluje, či je e-mailová schránka prázdna.
     * @returns {boolean} Vracia `true`, ak je e-mailová schránka prázdna (t.j. ak nie sú žiadne viditeľné e-maily na vymazanie); inak `false`.
     */
    async isInboxEmpty() {
        var provider = this.getTempmailProvider();
        if (provider) {
            return provider.isInboxEmpty();
        }
    },

    /**
     * Zatvorí otvorený email.
     * Je potrebné zavolať TempMail.login() predtým
     */
    closeEmail(){
        var provider = this.getTempmailProvider();
        if (provider) {
            return provider.closeEmail();
        }
    },

    /**
     * Delete currently opened email
     */
    deleteCurrentEmail() {
        I.say('Mazem aktualny email');
        var provider = this.getTempmailProvider();
        if (provider) {
            return provider.deleteCurrentEmail();
        }
    },

    getContentSelector() {
        var provider = this.getTempmailProvider();
        if (provider && provider.getContentSelector) {
            return provider.getContentSelector();
        }
        return "";
    },

    getSubjectSelector() {
        var provider = this.getTempmailProvider();
        if (provider && provider.getSubjectSelector) {
            return provider.getSubjectSelector();
        }
        return "";
    },

    /**
     * Vymaže všetky e-maily.
     * Je potrebné zavolať TempMail.login() predtým
     */
    async destroyInbox(emailAddress = null) {
        var provider = this.getTempmailProvider();
        if (provider) {
            return provider.destroyInbox(emailAddress);
        }
    },
}