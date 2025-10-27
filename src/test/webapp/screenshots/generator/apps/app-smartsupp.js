Feature('apps.app-smartsupp');
Before(({ login }) => {
    login('admin');
});

Scenario('Live chat (SmartsUpp)', ({ I, DT, Document }) => {
    I.amOnPageLng("/apps/live-chat-smartsupp");
    DT.waitForLoader();
    I.waitForElement("#widgetButtonFrame", 10);
    I.switchTo('#widgetButtonFrame');
    I.waitForElement("div[data-testid='widgetButtontext']", 10);
    I.forceClick('div[data-testid="widgetButtontext"]');
    I.switchTo();
    Document.screenshot("/redactor/apps/app-smartsupp/app-smartsupp.png");
    Document.screenshotAppEditor(105920, "/redactor/apps/app-smartsupp/editor.png", function(Document, I, DT, DTE) {
        DTE.fillField('kluc', 'd505a42ae6e813515gfbaae7a0eff418b87217ad');
    });
});