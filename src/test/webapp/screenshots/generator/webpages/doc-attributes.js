Feature('webpages.doc-attributes');

Before(({ I, login }) => {
    login('admin');
});

Scenario('datatable', ({I, Document}) => {
    I.amOnPage("/admin/v9/webpages/attributes/?id=1");
    Document.screenshot("/redactor/webpages/doc-attributes/doc-attributes-editor.png");
});

Scenario('editor and apps', ({I, DTE, Document}) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Aplikácie", "Atribúty stránky", "Monitory"]);

    I.click("Dell P2772");
    DTE.waitForEditor();
    I.seeElement("#pills-dt-datatableInit-attributes-tab");
    I.clickCss("#pills-dt-datatableInit-attributes-tab");

    Document.screenshot("/redactor/webpages/doc-attributes/page-editor.png");

    I.amOnPage("/apps/atributy-stranky/monitory/?NO_WJTOOLBAR=true");
    Document.screenshot("/redactor/webpages/doc-attributes/page-table.png");

    I.amOnPage("/apps/atributy-stranky/monitory/apple-5k.html?NO_WJTOOLBAR=true");
    Document.screenshot("/redactor/webpages/doc-attributes/page-attrs.png");
});