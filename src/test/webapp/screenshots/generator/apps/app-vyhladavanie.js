Feature('apps.app-vyhladavanie');

Before(({ login }) => {
    login('admin');
});

Scenario('app vyhladavanie', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/google-vyhladavanie/");

    I.waitForElement(".gsc-control-cse");

    I.fillField('search', 'WebJET');
    I.clickCss('.gsc-search-button .gsc-search-button-v2');
    I.wait(2);

    Document.screenshot("/redactor/apps/app-vyhladavanie/app-vyhladavanie.png");

    Document.screenshotAppEditor(77770, "/redactor/apps/app-vyhladavanie/editor.png");
});
