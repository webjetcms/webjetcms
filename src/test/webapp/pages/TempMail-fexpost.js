const { I } = inject();

/**
 * Funkcie pre pracu s https://tempmail.plus
 */

module.exports = {

    /**
     * Prihlási sa do TempMail a zobrazí e-maily.
     * @param {string} name - Názov e-mailového účtu.
     * @param {string} [emailDomain="fexpost.com"] - Doména e-mailového účtu, predvolene "fexpost.com".
     */
    async login(name, emailDomain = "fexpost.com"){
        //if name contains '@' then split it and use the first part as name and the second part as emailDomain
        if (name.includes('@')) {
            const parts = name.split('@');
            name = parts[0];
            emailDomain = parts[1];
        }

        I.say('Prihlasujem sa do TempMail-u');
        await I.executeScript(() => {
            window.location.href = 'https://tempmail.plus';
        });
        await I.wait(5);
        I.switchTo();
        I.fillField('input#pre_button', name);
        I.clickCss('button#domain');
        I.click(locate("button.dropdown-item").withText(emailDomain));
        I.wait(1);
    },

    /**
     * Otvorí najnovší email v inboxe.
     * Je potrebné zavolať TempMail.login() predtým
     */
    openLatestEmail(){
        I.say('Otvaram najnovsi mail');
        I.waitForElement("div.inbox", 10);
        I.waitForElement("div.inbox > div:nth-of-type(2) div.subj", 60);
        I.clickCss("div.inbox > div:nth-of-type(2) div.subj");
        I.waitForElement("div#info", 10);
        I.wait(1);
    },

    /**
     * Skontroluje, či je e-mailová schránka prázdna.
     * @returns {boolean} Vracia `true`, ak je e-mailová schránka prázdna (t.j. ak nie sú žiadne viditeľné e-maily na vymazanie); inak `false`.
     */
    async isInboxEmpty() {
        const numberOfEmails = await I.grabNumberOfVisibleElements('#delete');
        return numberOfEmails === 0;
    },

    /**
     * Zatvorí otvorený email.
     * Je potrebné zavolať TempMail.login() predtým
     */
    closeEmail(){
        I.clickCss("button#back");
        I.waitForElement("div.inbox", 10);
    },

    /**
     * Delete currently opened email
     */
    deleteCurrentEmail() {
        I.clickCss("#delete_mail");
        I.waitForElement("#modal-destroy-mail", 10);
        I.waitForElement("#confirm_mail");
        I.wait(1);
        I.clickCss("#confirm_mail");
        I.waitForInvisible("#modal-destroy-mail", 60);
    },

    /**
     * Vymaže všetky e-maily.
     * Je potrebné zavolať TempMail.login() predtým
     */
    async destroyInbox(emailAddress = null) {
        if (emailAddress != null) {
            await this.login(emailAddress);
        }

        I.say('Vymazávam všetky e-maily');
        let numberOfEmails = await I.grabNumberOfVisibleElements("#delete");
        if(numberOfEmails > 0) {
            I.clickCss("button#delete", );
            I.clickCss("button#confirm");
        }

        I.waitForElement(".loading.m-auto", 60);
    },

    getContentSelector() {
        return "div#info > div.overflow-auto";
    },

    getSubjectSelector() {
        return "div.subject";
    },

    /**
     * @returns domain name with @
     */
    getTempMailDomain() {
        return "@fexpost.com";
    },

    checkAttachments(attachmentsNames = []) {
        I.say("Checking attachments in email");
        attachmentsNames.forEach(attachmentName => {
            I.seeElement( locate("div.attachments").find( locate("a").withText(attachmentName) ) );
        });
    }
}