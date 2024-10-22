Feature('apps.app-facebook_like_box');
Before(({ login }) => {
    login('admin');
});

Scenario('Facebook like tlacidlo', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/facebook-like-box");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-facebook_like_box/app-facebook_like_box.png");
    Document.screenshotAppEditor(104143, "/redactor/apps/app-facebook_like_box/editor.png");
});


