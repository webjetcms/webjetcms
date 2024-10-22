Feature('apps.gdpr');

Before(({ login }) => {
    login('admin');
});

Scenario('Gdpr Cookies', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/gdpr-cookies");
    Document.screenshot("/redactor/apps/gdpr/gdpr.png");

    Document.screenshotAppEditor(25211, "/redactor/apps/gdpr/editor.png");
});
