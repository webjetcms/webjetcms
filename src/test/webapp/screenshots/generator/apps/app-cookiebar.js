Feature('apps.app-cookiebar');
Before(({ login }) => {
    login('admin');
});

Scenario('Cookie lista', ({ I, DT, DTE, Document, i18n }) => {
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
        DTE.selectOption('position', i18n.get('Down'));
        DTE.fillField('padding_top', '30');
        DTE.fillField('padding_bottom', '30');
        Document.screenshot("/redactor/apps/app-cookiebar/editor-style.png");
        Apps.switchToTabByIndex(0);
        DTE.fillField('cookie_title', i18n.get('Use of cookies'));
        DTE.fillField('cookie_text', i18n.get('Our website uses cookies that are necessary for its functionality'));
        DTE.fillField('cookie_ButtonText', i18n.get('Accept'));
        DTE.fillField('cookie_ButtonTextDecline', i18n.get('Decline'));
    });

});
