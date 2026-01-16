const { I } = inject();

/**
 * Funkcie pre pracu s https://tempmail.plus
 */

module.exports = {

    /**
     * Prihlási sa do TempMail a zobrazí e-maily.
     * @param {string} name - Názov e-mailového účtu.
     * @param {string} [emailDomain="noopmail.com"] - Doména e-mailového účtu, predvolene "noopmail.com".
     */
    async login(name, emailDomain = "noopmail.com"){
        //if name contains '@' then split it and use the first part as name and the second part as emailDomain
        if (name.includes('@')) {
            const parts = name.split('@');
            name = parts[0];
            emailDomain = parts[1];
        }

        I.say('Prihlasujem sa do TempMail-u');
        await I.executeScript(() => {
            window.location.href = 'https://noopmail.org';
        });
        I.wait(1);
        I.waitForElement(locate("h1").withText("NoopMail"), 10);
        //wait for cookies contsent banner to appear
        I.wait(3);

        let consent = await I.grabNumberOfVisibleElements('.fc-consent-root');
        if (consent > 0) {
            //do not consent to prevent ads
            I.clickCss('button.fc-cta-manage-options');
            I.wait(1);
            I.clickCss('button.fc-confirm-choices');
            I.wait(1);
        }


        I.fillField('input#e', name);
        I.selectOption('select#d', emailDomain);
        I.clickCss('button#c'); //Check inbox
        I.wait(1);
    },

    /**
     * Otvorí najnovší email v inboxe.
     * Je potrebné zavolať TempMail.login() predtým
     */
    openLatestEmail(){
        I.say('Otvaram najnovsi mail');
        I.waitForElement("div#mbox", 10);
        I.waitForElement("div#mbox > a", 60);
        I.clickCss("div#mbox > a:nth-of-type(1)");
        I.waitForElement("span#btmb", 10); //back button
        I.wait(1);


        //because content is in iframe we need to copy content to
        I.executeScript(() => {
            let html = document.getElementById("ehtmc").contentDocument.body.innerHTML;
            //add target="_blank" to all links
            html = html.replace(/<a /g, '<a target="_blank" ');
            let contentDiv = document.getElementById('tempMailEmailContent');
            if (!contentDiv) {
                contentDiv = document.createElement('div');
                contentDiv.id = 'tempMailEmailContent';
                contentDiv.style.maxHeight = '80px';
                contentDiv.style.overflow = 'scroll';
                contentDiv.style.fontSize = '3px';
                const navbar = document.querySelector('#etbc');
                navbar.appendChild(contentDiv);
            }
            contentDiv.innerHTML = html;
        });

        I.waitForElement("#tempMailEmailContent", 10);

    },

    /**
     * Skontroluje, či je e-mailová schránka prázdna.
     * @returns {boolean} Vracia `true`, ak je e-mailová schránka prázdna (t.j. ak nie sú žiadne viditeľné e-maily na vymazanie); inak `false`.
     */
    async isInboxEmpty() {
        const numberOfEmails = await I.grabNumberOfVisibleElements('div#mbox > a');
        return numberOfEmails === 0;
    },

    /**
     * Zatvorí otvorený email.
     * Je potrebné zavolať TempMail.login() predtým
     */
    closeEmail(){
        I.clickCss("span#btmb");
        I.waitForElement("div#mbox", 10);
    },

    /**
     * Delete currently opened email
     */
    deleteCurrentEmail() {
        I.clickCss("span#brm");
        I.waitForElement("div#mbox", 10);
        I.wait(1);
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
        let numberOfEmails = await I.grabNumberOfVisibleElements("div#mbox > a");
        // Failsafe to prevent an infinite loop if the inbox view does not refresh correctly.
        // 10 iterations is sufficient for typical test inbox sizes; when exceeded, the loop stops
        // even if some emails remain, so that the test does not hang indefinitely.
        const MAX_DELETE_ITERATIONS = 10;
        let failsafe = 0;
        while(numberOfEmails > 0 && failsafe++ < MAX_DELETE_ITERATIONS) {
            this.openLatestEmail();
            this.deleteCurrentEmail();
            numberOfEmails = await I.grabNumberOfVisibleElements("div#mbox > a");
        }

        I.waitForElement(locate("p").withText("Waiting for incoming emails"), 60);
    },

    getContentSelector() {
        return "div#tempMailEmailContent";
    },

    getSubjectSelector() {
        return "div#mifo .r.t3:nth-of-type(3) span.y2";
    },

    /**
     *
     * @returns domain name with @
     */
    getTempMailDomain() {
        return "@noopmail.com";
    }
}