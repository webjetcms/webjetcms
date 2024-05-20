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

    if("sk" === I.getConfLng()) {
        I.forceClick("Zrušiť");
    } else if("en" === I.getConfLng()) { 
        I.forceClick("Cancel");
    }

    //Tooltip editor
    I.click("button.buttons-create");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/tooltip/tooltip-editor.png");
});

Scenario('tooltip editor', ({ I, DTE, Document }) => {
    let confLng = I.getConfLng();

    if("sk" === I.getConfLng()) {
        I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=19801");
    } else if("en" === I.getConfLng()) { 
        I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=81852");
    }

    DTE.waitForEditor();
    Document.screenshotElement("span.cke_button__tooltip_icon", "/redactor/apps/tooltip/editor-tooltip-icon.png");

    I.click("span.cke_button__tooltip_icon");

    if("sk" === I.getConfLng()) {
        I.fillField("Text na ktorý bude aplikovaný tooltip:", "odborný výraz");
        I.fillField("Tooltip (kľúč):", "Test");
    } else if("en" === I.getConfLng()) { 
        I.fillField("Text", "technical term");
        I.fillField("Content Tooltip", "Test");
    }

    I.wait(2);
    Document.screenshot("/redactor/apps/tooltip/editor-tooltip-dialog.png");

    if("sk" === I.getConfLng()) {
        I.amOnPage("/teeeeestststst/tooltip-test.html?NO_WJTOOLBAR=true");
    } else if("en" === I.getConfLng()) { 
        I.amOnPage("/teeeeestststst/tooltip-en.html?NO_WJTOOLBAR=true");
    }

    I.moveCursorTo(".wjtooltip");
    Document.screenshotElement("article div.container", "/redactor/apps/tooltip/webpage-tooltip.png");
});