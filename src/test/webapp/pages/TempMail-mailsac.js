const { I } = inject();

/**
 * Funkcie pre pracu s https://tempmail.plus
 */

module.exports = {

    /**
     * Prihlási sa do TempMail a zobrazí e-maily.
     * @param {string} name - Názov e-mailového účtu.
     * @param {string} [emailDomain="mailsac.com"] - Doména e-mailového účtu, predvolene "mailsac.com".
     */
    async login(name, emailDomain = "mailsac.com"){
        //if name contains '@' then split it and use the first part as name and the second part as emailDomain
        if (name.includes('@')) {
            const parts = name.split('@');
            name = parts[0];
            emailDomain = parts[1];
        }

        I.say('Prihlasujem sa do TempMail-u');

        I.amOnPage("https://mailsac.com/v2/dashboard");
        let isLogin = await I.grabNumberOfVisibleElements('input[name=username]');
        if (isLogin > 0) {
            I.fillField(locate("input[name=username]"), "webjetcms");
            I.fillField(locate("input[name=password]"), process.env.CODECEPT_TEMPMAIL);
            I.click(locate("button").withText("Sign In"));
            I.wait(3);
        }

        let url = 'https://mailsac.com/inbox/'+name+'%40'+emailDomain;
        /*await I.executeScript((url) => {
            window.location.href=url;
        }, url);
        I.wait(5);
        */

        I.amOnPage(url);
        I.waitForElement("div.inbox span.help-block", 10);

        I.switchTo();
        I.wait(1);
    },

    /**
     * Otvorí najnovší email v inboxe.
     * Je potrebné zavolať TempMail.login() predtým
     */
    openLatestEmail(){
        I.say('Otvaram najnovsi mail');
        I.refreshPage();
        I.waitForElement("table.inbox-table td.inbox-subject-td", 240);
        I.clickCss("table.inbox-table tr:nth-of-type(2) td.inbox-subject-td");
        I.waitForElement("table.inbox-table tr:nth-of-type(2) td.active", 10);
        I.wait(1);
    },

    /**
     * Skontroluje, či je e-mailová schránka prázdna.
     * @returns {boolean} Vracia `true`, ak je e-mailová schránka prázdna (t.j. ak nie sú žiadne viditeľné e-maily na vymazanie); inak `false`.
     */
    async isInboxEmpty() {
        const numberOfEmails = await I.grabNumberOfVisibleElements('table.inbox-table tr.clickable');
        return numberOfEmails === 0;
    },

    /**
     * Zatvorí otvorený email.
     * Je potrebné zavolať TempMail.login() predtým
     */
    closeEmail(){
        I.click(locate("button").withText("Close"));
    },

    /**
     * Delete currently opened email
     */
    deleteCurrentEmail() {
        I.click(locate("button").withText("Delete"));
        I.wait(1);
        I.click(locate("button").withText("Permanently delete"));
    },

    /**
     * Vymaže všetky e-maily.
     * Je potrebné zavolať TempMail.login() predtým
     */
    async destroyInbox(emailAddress = null) {
        if (emailAddress != null) {
            this.login(emailAddress);
        }

        I.say('Vymazávam všetky e-maily');

        let numberOfEmails = await I.grabNumberOfVisibleElements("table.inbox-table tr.clickable");
        let failsafe = 0;
        while(numberOfEmails > 0 && failsafe++ < 10) {
            this.openLatestEmail();
            this.deleteCurrentEmail();
            numberOfEmails = await I.grabNumberOfVisibleElements("table.inbox-table tr.clickable");
        }

        I.wait(1);
    },

    /**
     *
     * @returns domain name with @
     */
    getTempMailDomain() {
        return "@mailsac.com";
    }
}