Feature('apps.htmlbox');

Before(({ login }) => {
    login('admin');
});

Scenario('app htmlbox', ({ I, DT, DTE, Document, i18n }) => {

    I.amOnPage("/apps/bloky/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/htmlbox/htmlbox.png");

    Document.screenshotAppEditor(81104, "/redactor/apps/htmlbox/editor-block.png", function(Document, I, DT, DTE) {
        DTE.selectOption("codeType", "Web stránka");
        Document.screenshot("/redactor/apps/htmlbox/editor-doc.png");

        DTE.selectOption("codeType", "Šablóny");
        Document.screenshot("/redactor/apps/htmlbox/editor-template.png");

        DTE.selectOption("codeType", "Predpripravený blok");
    });

    I.amOnPageLng("/admin/v9/webpages/web-pages-list/?docid=25");
    DTE.waitForEditor();
    I.wait(5);
    I.clickCss("a.cke_button.cke_button__htmlbox");
    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");
    I.wait(4);
    var basePath = Document.getWebappPath();
    var lngSuffix = i18n.getImgSuffix();
    Document.screenshot(basePath+"/components/htmlbox/screenshot-1"+lngSuffix+".jpg");
    DTE.selectOption("codeType", "Šablóny");
    Document.screenshot(basePath+"/components/htmlbox/screenshot-2"+lngSuffix+".jpg");
    DTE.selectOption("codeType", "Web stránka");
    Document.screenshot(basePath+"/components/htmlbox/screenshot-3"+lngSuffix+".jpg");

});