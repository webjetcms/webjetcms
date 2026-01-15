const { I } = inject();

const fexPost = require("./TempMail-fexpost");
const mailSac = require("./TempMail-mailsac");
const noopmail = require("./TempMail-noopmail");

/**
 * Funkcie pre pracu s https://tempmail.plus
 */

module.exports = {

    getTempmailProvider() {
        var provider = process.env.CODECEPT_TEMPMAIL_PROVIDER;
        if (provider === "fexpost") {
            return fexPost;
        }
        else if (provider === "mailSac") {
            return mailSac;
        }
        else if (provider === "noopmail") {
            return noopmail;
        }
        //default
        return mailSac;
    },

    getTempMailDomain() {
        var provider = this.getTempmailProvider();
        if (provider && provider.getTempMailDomain) {
            return provider.getTempMailDomain();
        }
        return "fexpost.com";
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
     * Prihlási sa do TempMailu a zobrazí e-maily, nepouzije sa funkcia I.amOnPage.
     * @param {string} name - Názov e-mailového účtu.
     * @param {string} [emailDomain="fexpost.com"] - Doména e-mailového účtu, predvolene "fexpost.com".
     */
    async loginAsync(name, emailDomain = "fexpost.com"){
        var provider = this.getTempmailProvider();
        if (provider) {
            return provider.loginAsync(name, emailDomain);
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
        var provider = this.getTempmailProvider();
        if (provider) {
            return provider.deleteCurrentEmail();
        }
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