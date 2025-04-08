Feature('apps.app-facebook_like');
Before(({ login }) => {
    login('admin');
});

Scenario('Facebook like tlacidlo', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/facebook-like-tlacidlo");
    I.waitForElement("#facebook", 30);
    Document.screenshot("/redactor/apps/app-facebook_like/app-facebook_like.png");
    Document.screenshotAppEditor(104145, "/redactor/apps/app-facebook_like/editor.png");
});
