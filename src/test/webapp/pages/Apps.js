const { I } = inject();
const DTE = require("./DTE");
var doc_add_button = (locate("#datatableInit_wrapper > div.dt-header-row.clearfix.wp-header > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success"));
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
     */
    async assertParams(expectedParams) {
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
        I.click('.cke_wysiwyg_frame.cke_reset');
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
    openAppEditor(docId = null){
        I.say('Opening app editor');
        if(docId){
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
        }
        DTE.waitForEditor();
        //I.wait(5);
        I.waitForElement(".cke_wysiwyg_frame.cke_reset");
        I.wait(1);
        I.switchTo(".cke_wysiwyg_frame.cke_reset");
        I.waitForElement("iframe.wj_component");
        I.wait(1);

        within({frame: [".cke_wysiwyg_frame.cke_reset", "iframe.wj_component"]}, () => {
            I.click("a.inlineComponentButton.cke_button");
        });
        I.waitForInvisible('Čakajte prosím');
        I.switchTo('.cke_dialog_ui_iframe');
        I.switchTo('#editorComponent');
        DTE.waitForModal('component-datatable_modal');
    },

    /**
     * Inserts an application into the page.
     * @param {string} applicationName - The name of the application to insert.
     * @param {string} applicationSelector - The selector for the application in the app menu.
     * @param {string|null} docId - Optional document ID for the page to insert the app into.
     */
    insertApp(applicationName, applicationSelector, docId = null){
        I.say('Inserting an app');
        I.closeOtherTabs();
        if (docId){
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
        } else{
            I.amOnPage('/admin/v9/webpages/web-pages-list/');
            I.click(doc_add_button);
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
        I.waitForInvisible('div.promoApp', 10);
        I.waitForElement(applicationSelector, 10);
        I.wait(1);
        I.clickCss(applicationSelector); //I.clickCss('div.menu-app[data-app-action^="sk.iway"]')
        I.waitForInvisible("div.appStore > div.block-header", 30);
        I.wait(1);
        I.click("Vložiť do stránky"); //I.clickCss('a.buy');
        DTE.waitForEditor("component-datatable");
        I.switchTo();
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok')
    }
}