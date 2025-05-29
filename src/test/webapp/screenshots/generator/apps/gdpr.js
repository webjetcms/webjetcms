Feature('apps.gdpr');

Before(({ login }) => {
    login('admin');
});

Scenario('Gdpr Cookies', ({ I, Document }) => {
    I.amOnPage("/apps/gdpr-cookies/?NO_WJTOOLBAR=true&language="+I.getConfLng());

    Document.screenshot("/redactor/apps/gdpr/gdpr.png");

    Document.screenshotAppEditor(25211, "/redactor/apps/gdpr/editor.png");
});
