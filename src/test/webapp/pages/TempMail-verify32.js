const { I } = inject();

/**
 * Funkcie pre pracu s https://www.verify32.com
 * https://tempmail.sk
 */

module.exports = {

    /**
     * Prihlási sa do TempMail a zobrazí e-maily.
     * @param {string} name - Názov e-mailového účtu.
     * @param {string} [emailDomain="tempverify.com"] - Doména e-mailového účtu, predvolene "tempverify.com".
     */
    async login(name, emailDomain = "tempverify.com"){
        //if name contains '@' then split it and use the first part as name and the second part as emailDomain
        if (name.includes('@')) {
            const parts = name.split('@');
            name = parts[0];
            emailDomain = parts[1];
        }

        I.say('Prihlasujem sa do TempMail-u');
        await I.executeScript(() => {
            window.location.href = 'https://www.verify32.com';
        });
        I.wait(1);
        I.waitForElement('i.fa-plus-square', 10);
        I.click(locate("i.fa-plus-square"));
        I.fillField('input#user', name);
        //select domain from dropdown
        I.click(locate("input#domain"));
        I.click(locate("a.text-sm.ease-in-out").withText(emailDomain));

        I.click(locate("input#create"));
        I.wait(1);
        this._waitForRefresh();
    },

    /**
     * Otvorí najnovší email v inboxe.
     * Je potrebné zavolať TempMail.login() predtým
     */
    openLatestEmail(){
        I.say('Otvaram najnovsi mail');
        this._waitForRefresh();
        I.waitForElement("div.messages", 60);
        I.waitForElement("div.messages > div:nth-of-type(1) div.justify-between", 60);
        I.clickCss("div.messages > div:nth-of-type(1) div.justify-between");
        I.waitForElement("div.message-content iframe", 10);

        //because content is in iframe we need to copy content to
        I.executeScript(() => {
            const textarea = document.querySelector('textarea.hidden');
            let html = textarea.value;
            //add target="_blank" to all links
            html = html.replace(/<a /g, '<a target="_blank" ');
            let contentDiv = document.getElementById('tempMailEmailContent');
            if (!contentDiv) {
                contentDiv = document.createElement('div');
                contentDiv.id = 'tempMailEmailContent';
                contentDiv.style.maxHeight = '80px';
                contentDiv.style.overflow = 'scroll';
                contentDiv.style.fontSize = '3px';
                const navbar = document.querySelector('nav .items-baseline');
                navbar.appendChild(contentDiv);
            }
            contentDiv.innerHTML = html;
        });

        I.waitForElement("#tempMailEmailContent", 10);
        I.wait(1);
    },

    /**
     * Skontroluje, či je e-mailová schránka prázdna.
     * @returns {boolean} Vracia `true`, ak je e-mailová schránka prázdna (t.j. ak nie sú žiadne viditeľné e-maily na vymazanie); inak `false`.
     */
    async isInboxEmpty() {
        const numberOfEmails = await I.grabNumberOfVisibleElements('div.messages > div');
        return numberOfEmails === 0;
    },

    /**
     * Zatvorí otvorený email.
     * Je potrebné zavolať TempMail.login() predtým
     */
    closeEmail(){
        I.refreshPage();
        I.wait(3);
        this._waitForRefresh();
    },

    /**
     * Delete currently opened email
     */
    deleteCurrentEmail() {
        this._waitForRefresh();

        I.forceClick(locate("div.message-content button").withText("Delete"));
        I.wait(1);

        I.executeScript(() => {
            let contentDiv = document.getElementById('tempMailEmailContent');
            if (contentDiv) {
                contentDiv.innerHTML = "";
            }
        });

        this._waitForRefresh();
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
        this._waitForRefresh();
        let numberOfEmails = await I.grabNumberOfVisibleElements("div.messages > div");
        I.say(`Nájdených e-mailov: ${numberOfEmails}`);
        // Failsafe to prevent an infinite loop if the inbox view does not refresh correctly.
        // 10 iterations is sufficient for typical test inbox sizes; when exceeded, the loop stops
        // even if some emails remain, so that the test does not hang indefinitely.
        const MAX_DELETE_ITERATIONS = 10;
        let failsafe = 0;
        while(numberOfEmails > 0 && failsafe++ < MAX_DELETE_ITERATIONS) {
            await this.openLatestEmail();
            //this.deleteCurrentEmail();
            I.forceClick(locate("div.message-content button").withText("Delete"));
            I.wait(1);
            numberOfEmails = await I.grabNumberOfVisibleElements("div.messages > div");
        }
    },

    getContentSelector() {
        return "div#tempMailEmailContent";
    },

    getSubjectSelector() {
        return "div.message-content div.text-gray-900.text-lg";
    },

    /**
     * @returns domain name with @
     */
    getTempMailDomain() {
        return "@tempverify.com";
    },

    _waitForRefresh() {
        I.switchTo();
        I.waitForElement("img.max-w-logo", 10);
        I.waitForElement("i#refresh:not(.pause-spinner)", 60);
        I.waitForElement("i#refresh.pause-spinner", 60);
    }
}