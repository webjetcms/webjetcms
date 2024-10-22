Feature('apps.search');

Before(({ login }) => {
    login('admin');
});

Scenario('vyhladavanie', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/vyhladavanie/?words=Kontakt&language="+I.getConfLng());
    Document.screenshot("/redactor/apps/search/search.png");

    Document.screenshotAppEditor(77875, "/redactor/apps/search/editor.png");
});
