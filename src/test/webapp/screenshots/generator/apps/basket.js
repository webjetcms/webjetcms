Feature('apps.basket');
Before(({ login }) => {
    login('admin');
});

Scenario('Elektronický obchod', ({ I, DT, Document }) => {
    I.amOnPage("/apps/elektronicky-obchod/produkty/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/basket/basket.png");

    Document.screenshotAppEditor(126186, "/redactor/apps/basket/editor.png", function(Document, I, DT, DTE, Apps) {
        I.clickCss('#tabLink4');
        Document.screenshot("/redactor/apps/basket/editor-style.png");
        I.clickCss('#tabLink3');
        Document.screenshot("/redactor/apps/basket/editor-list.png");
        I.clickCss('#tabLink2');
        Document.screenshot("/redactor/apps/basket/editor-items.png");
        I.clickCss('#tabLink1');
    });
});
