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
    I.forceClick("Zrušiť");

    //Tooltip editor
    I.click("button.buttons-create");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/tooltip/tooltip-editor.png");
});

Scenario('tooltip editor', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=19801");
    DTE.waitForEditor();
    I.wait(5);
    Document.screenshotElement("span.cke_button__tooltip_icon", "/redactor/apps/tooltip/editor-tooltip-icon.png");

    I.click("span.cke_button__tooltip_icon");
    I.fillField("Text na ktorý bude aplikovaný tooltip:", "odborný výraz");
    I.fillField("Tooltip (kľúč):", "Test");
    I.wait(2);
    Document.screenshot("/redactor/apps/tooltip/editor-tooltip-dialog.png");

    I.amOnPage("/teeeeestststst/tooltip-test.html");
    I.moveCursorTo(".wjtooltip");
    Document.screenshotElement("article div.container", "/redactor/apps/tooltip/webpage-tooltip.png");
});