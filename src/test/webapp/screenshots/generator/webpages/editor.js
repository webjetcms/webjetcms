Feature('webpages.editor');

Before(({ I, login }) => {
    login('admin');
});

Scenario('editor', ({I, DTE, Document}) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");
    DTE.waitForEditor();
    Document.screenshot("/redactor/webpages/editor/tab-content.png");

    I.clickCss("a.cke_button__pastefromword");
    Document.screenshot("/redactor/webpages/editor/paste-word.png");
    I.clickCss("div.cke_dialog_container a.cke_dialog_ui_button.cke_dialog_ui_button_cancel");

    I.clickCss("#pills-dt-datatableInit-basic-tab");
    Document.screenshot("/redactor/webpages/editor/tab-basic.png");

    I.clickCss("#pills-dt-datatableInit-menu-tab");
    Document.screenshot("/redactor/webpages/editor/tab-menu.png");

    I.clickCss("#pills-dt-datatableInit-access-tab");
    Document.screenshot("/redactor/webpages/editor/tab-access.png");

    I.clickCss("#pills-dt-datatableInit-perex-tab");
    Document.screenshot("/redactor/webpages/editor/tab-perex.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=79020");
    DTE.waitForEditor();
    I.waitForElement("div.CodeMirror-code", 10);
    Document.screenshot("/redactor/webpages/editor/html-mode.png");
});