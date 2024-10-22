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
    login(name, emailDomain = "fexpost.com"){
        I.say('Prihlasujem sa do TempMail-u');
        I.amOnPage('https://tempmail.plus/en/#!');
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
        I.waitForElement("div.inbox > div:nth-of-type(2) div.subj");
        I.clickCss("div.inbox > div:nth-of-type(2) div.subj");
        I.waitForElement("div#info");
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
        I.click("#delete_mail");
        I.waitForElement("#modal-destroy-mail");
        I.click("#confirm_mail");
        I.waitForInvisible("#modal-destroy-mail");
    },

    /**
     * Vymaže všetky e-maily.
     * Je potrebné zavolať TempMail.login() predtým
     */
    async destroyInbox(){
        I.say('Vymazávam všetky e-maily');
        let numberOfEmails = await I.grabNumberOfVisibleElements("#delete");
        if(numberOfEmails > 0) {
            I.clickCss("button#delete", );
            I.clickCss("button#confirm");
        }

        I.waitForElement(".loading.m-auto");
    },
}