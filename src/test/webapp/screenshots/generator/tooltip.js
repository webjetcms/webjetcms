Feature('tooltip');

Before(({ I, login }) => {
    login('admin');
});

Scenario('tooltip', ({ I, Document }) => {
    I.amOnPage("/apps/tooltip/admin/");

    //Tooltip data table
    Document.screenshot("/redactor/apps/tooltip/tooltip-dataTable.png");

    //Tooltip import editor
    I.click({ css: 'button[data-dtbtn=import]' });
    Document.screenshotElement("div.modal-content", "/redactor/apps/tooltip/tooltip-import-editor.png");

    switch (I.getConfLng()) {
        case 'sk':
            I.forceClick("Zrušiť");
            break;
        case 'en':
            I.forceClick("Cancel");
            break;
        case 'cs':
            I.forceClick("Zrušit");
            break;
        default:
            throw new Error(`Unsupported language code: ${I.getConfLng()}`);
    }    
    //Tooltip editor
    I.click("button.buttons-create");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/tooltip/tooltip-editor.png");
});

Scenario('tooltip editor', ({ I, DTE, Document }) => {
    let confLng = I.getConfLng();

    switch (confLng) {
        case 'sk':
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=19801");
            break;
        case 'cs':
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=100101");
            break;
        case 'en':
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=81852");
            break;
        default:
            throw new Error(`Unsupported language code: ${I.getConfLng()}`);
    }    

    DTE.waitForEditor();
    Document.screenshotElement("span.cke_button__tooltip_icon", "/redactor/apps/tooltip/editor-tooltip-icon.png");

    I.click("span.cke_button__tooltip_icon");
    switch (I.getConfLng()) {
        case "sk":
            I.fillField("Text na ktorý bude aplikovaný tooltip:", "odborný výraz");
            I.fillField("Tooltip (kľúč):", "Test");
            break;
        case "en":
            I.fillField("Text", "technical term");
            I.fillField("Content Tooltip", "Test");
            break;
        case "cs":
            I.fillField("Text", "odborný výraz");
            I.fillField("Content Tooltip", "Test");
            break;
        default:
            throw new Error("Unknown language: " + I.getConfLng());
    }    

    I.wait(2);
    Document.screenshot("/redactor/apps/tooltip/editor-tooltip-dialog.png");
    switch (I.getConfLng()) {
        case "sk":
            I.amOnPage("/teeeeestststst/tooltip-test.html?NO_WJTOOLBAR=true");
            break;
        case "en":
            I.amOnPage("/teeeeestststst/tooltip-en.html?NO_WJTOOLBAR=true");
            break;
        case "cs":
            I.amOnPage("/teeeeestststst/tooltip-cs.html?NO_WJTOOLBAR=true");
            break;
        default:
            throw new Error("Unknown language: " + I.getConfLng());
    }
    

    I.moveCursorTo(".wjtooltip");
    Document.screenshotElement("article div.container", "/redactor/apps/tooltip/webpage-tooltip.png");
});