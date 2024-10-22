Feature('apps.basket');
Before(({ login }) => {
    login('admin');
});

Scenario('Elektronický obchod', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/elektronicky-obchod");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/basket/basket.png");

    Document.screenshotAppEditor(106337, "/redactor/apps/basket/editor.png", function(Document, I, DT, DTE, Apps) {
        I.clickCss('#tabLink4');
        Document.screenshot("/redactor/apps/basket/editor-style.png");
        I.clickCss('#tabLink3');
        Document.screenshot("/redactor/apps/basket/editor-list.png");
        I.clickCss('#tabLink2');
        Document.screenshot("/redactor/apps/basket/editor-items.png");
        I.clickCss('#tabLink1');
    });
    


    
});
