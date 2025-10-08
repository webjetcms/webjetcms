const { I } = inject();
const DTE = require("./DTE");
const DT = require("./DT");
/**
 * Functions for interacting with the application
 */
module.exports = {

    /**
     * Switches to a specified editor type.
     * @param {string} editorType - The type of editor to switch to ('html' or 'standard').
     */
    switchEditor(editorType) {
        I.say(`Switching editor to ${editorType}`);
        I.clickCss('.form-control.form-select');

        const editorSelector = editorType === 'html'
            ? locate('.dropdown-item').withText('HTML editor')
            : locate('.dropdown-item').withText('Štandardný');

        I.click(editorSelector);
        I.wait(1);

    },

    /**
     * Asserts that the parameters in the page match the expected ones.
     * @param {Object} expectedParams - An object of key-value pairs representing expected parameters.
     * @param {String} expectedPath - Optional expected path to by used in INCLUDE
     */
    async assertParams(expectedParams, expectedPath = null) {
        I.say('Parameters testing');
        this.switchEditor('html');
        const inputString = await I.grabTextFrom('.CodeMirror-code');
        this.switchEditor('standard');
        const cleanedString = inputString
            .replace(/&quot;/g, '"')
            .replace(/&nbsp;/g, ' ')
            .replace(/<\/?[^>]+(>|$)/g, '');
        const regex = /(\w+)=(".*?"|[^,)\s]+)/g;
        let match;
        const params = {};
        while ((match = regex.exec(cleanedString)) !== null) {
            params[match[1]] = match[2].replace(/"/g, '');
        }
        I.say('Params:'+ JSON.stringify(params));
        for (const [key, value] of Object.entries(expectedParams)) {
            const actualValue = params[key] !== undefined ? params[key] : '';
            I.assertEqual(actualValue, value, `Assertion failed for ${key}: expected '${value}', got '${actualValue}'`);
        }

        if(expectedPath != null && expectedPath !== '') {
            I.say("Test expected path " + expectedPath);
            const match = inputString.match(/\!INCLUDE\(([^,]+),/);
            I.say(match);

            if (match) {
                I.assertEqual(match[1], expectedPath, `Assertion failed for expectedPath: expected '${expectedPath}', got '${match[1]}'`);
            } else {
                I.assertEqual(null, expectedPath, `Assertion failed for expectedPath: expected '${expectedPath}', got 'null'`);
            }
        }

        return true;
    },

    /**
     * Switches to a tab based on its index.
     * @param {number} index - The index of the tab to switch to (0-based).
     */
    switchToTabByIndex(index) {
        const tabLinks = '#pills-dt-editor-component-datatable .nav-link';
        I.waitForElement(tabLinks, 5);
        I.seeElement(tabLinks);
        I.click(locate(tabLinks).at(index + 1));
        I.wait(1);
    },

    /**
     * Clears the content of the current page.
     * @param {string|null} docId - Optional document ID for the page to clear.
     */
    clearPageContent(docId = null) {
        I.say('Clearing page content');
        if (docId) {
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
        }
        DTE.waitForEditor();
        I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
        //I.click('.cke_wysiwyg_frame.cke_reset');
        this.switchEditor('html');
        I.click('.CodeMirror-code');

        I.pressKey(['CommandOrControl', 'A']);
        I.pressKey('Delete');

        this.switchEditor('standard');
    },

    /**
     * Opens the application editor.
     * @param {string|null} docId - Optional document ID for the page to edit.
     */
    openAppEditor(docId = null, modalId = "component-datatable_modal", editIcon = false){
        I.say('Opening app editor');
        I.switchTo();
        if(docId){
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
        }
        DTE.waitForEditor();
        //I.wait(5);
        I.waitForElement(".cke_wysiwyg_frame.cke_reset");
        I.wait(1);
        I.switchTo(".cke_wysiwyg_frame.cke_reset");
        I.waitForElement("iframe.wj_component", 10);
        I.wait(1);

        I.switchTo(locate("iframe.wj_component").first());
        I.wait(5);
        I.waitForElement(".inlineComponentEdit", 30);
        if (editIcon)
            I.click(locate('.inlineComponentEdit .inlineComponentButton.cke_button').first());
        else
            I.click(locate('.inlineComponentEdit').first());
        I.switchTo();
        I.waitForInvisible('Čakajte prosím', 20);
        I.switchTo('.cke_dialog_ui_iframe');
        I.wait(1);
        I.switchTo('#editorComponent');
        DTE.waitForModal(modalId);
    },

    /**
     * Inserts an application into the page.
     * @param {string} applicationName - The name of the application to insert.
     * @param {string} applicationSelector - The selector for the application in the app menu.
     * @param {string|null} docId - Optional document ID for the page to insert the app into.
     */
    insertApp(applicationName, applicationSelector, docId = null, shouldClickOkButton = true){
        I.say('Inserting an app');
        I.closeOtherTabs();
        if (docId){
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
        } else{
            I.amOnPage('/admin/v9/webpages/web-pages-list/');
            I.click(DT.btn.add_button);
        }
        DTE.waitForEditor();
        //I.wait(5);
        I.waitForElement(".cke_wysiwyg_frame.cke_reset");
        I.wait(1);

        I.clickCss('#pills-dt-datatableInit-content-tab');
        I.clickCss('.cke_button.cke_button__components.cke_button_off');
        I.switchTo('.cke_dialog_ui_iframe');
        I.waitForElement('#editorComponent', 10);
        I.switchTo('#editorComponent');
        I.waitForElement('#search', 10);
        I.wait(1);
        I.fillField('#search', applicationName);
        I.waitForInvisible('div.promoApp', 20);
        I.waitForElement(applicationSelector, 10);
        I.wait(1);
        I.clickCss(applicationSelector); //I.clickCss('div.menu-app[data-app-action^="sk.iway"]')
        I.waitForInvisible("div.appStore > div.block-header", 30);
        I.wait(1);
        I.click("Vložiť do stránky"); //I.clickCss('a.buy');
        DTE.waitForEditor("component-datatable");
        I.switchTo();
        I.switchTo();
        if (shouldClickOkButton)
            I.clickCss('.cke_dialog_ui_button_ok');
    },

    /**
     * Click on the OK button in the Editor App dialog to confirm app settings changes
     */
    confirm() {
        I.switchTo();
        I.clickCss("td.cke_dialog_footer .cke_dialog_ui_button_ok");
        //wait for component preview to load
        I.wait(3);
    },

    /**
     * Save DT editor without closing it
     */
    save() {
        I.pressKey(['CommandOrControl', 's']);
        DTE.waitForLoader();
        I.switchTo();
        I.switchTo();
        //I.waitForElement("div.toast.toast-success", 20);
        I.waitForText("bol uložený", 10, "div.toast-success");
        //I.toastrClose();
    }
}