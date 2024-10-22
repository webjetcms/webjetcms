Feature('apps.app-social_icon');

Before(({ login }) => {
    login('admin');
});

Scenario('odkazy na socialne siete', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/odkazy-socialne-siete/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-social_icon/app-social_icon.png");

    Document.screenshotAppEditor(77772, "/redactor/apps/app-social_icon/editor.png");
});
