Feature('apps.htmlbox');

Before(({ login }) => {
    login('admin');
});

Scenario('app htmlbox', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/bloky/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/htmlbox/htmlbox.png");

    Document.screenshotAppEditor(81104, "/redactor/apps/htmlbox/editor-block.png", function(Document, I, DT, DTE) {
        I.dtEditorSelectOption("codeType", "Web stránka");
        Document.screenshot("/redactor/apps/htmlbox/editor-doc.png");

        I.dtEditorSelectOption("codeType", "Hlavná šablona: Šablóny");
        Document.screenshot("/redactor/apps/htmlbox/editor-template.png");

        I.dtEditorSelectOption("codeType", "Predpripravený blok");
    });


});
