Feature('apps.app-smartsupp');
Before(({ login }) => {
    login('admin');
});

Scenario('Live chat (SmartsUpp)', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/live-chat-smartsupp");
    DT.waitForLoader();
    I.switchTo('#widgetButtonFrame');
    I.clickCss('#smartsupp-widget-button');
    I.switchTo();
    Document.screenshot("/redactor/apps/app-smartsupp/app-smartsupp.png");
    Document.screenshotAppEditor(105920, "/redactor/apps/app-smartsupp/editor.png");
});


