Feature('apps.app-cookiebar');
Before(({ login }) => {
    login('admin');
});

Scenario('Cookie lista', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/cookies-lista");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-cookiebar/app-cookiebar.png");

    Document.screenshotAppEditor(25210, "/redactor/apps/app-cookiebar/editor.png", function(Document, I, DT, DTE, Apps) {
        Apps.switchToTabByIndex(1);
        DTE.fillField('color_background', '#f0f4f8');
        DTE.fillField('color_title', '#2c3e50');
        DTE.fillField('color_text', '#34495e');
        DTE.fillField('color_button', '#27ae60');
        DTE.fillField('color_buttonText', '#ffffff');
        DTE.selectOption('position', 'Dole');
        DTE.fillField('padding_top', '30');
        DTE.fillField('padding_bottom', '30');
        Document.screenshot("/redactor/apps/app-cookiebar/editor-style.png");
        Apps.switchToTabByIndex(0);
        DTE.fillField('cookie_title', 'Používanie cookies');
        DTE.fillField('cookie_text', 'Náš web používa súbory cookies, ktoré sú potrebné k jeho fungovaniu');
        DTE.fillField('cookie_ButtonText', 'Akceptovať');
        DTE.fillField('cookie_ButtonTextDecline', 'Zamietnuť');
    });

});
