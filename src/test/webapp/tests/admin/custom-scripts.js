//verify insertion of custom CSS and scripts
Feature('admin.custom-scripts');

Before(({ I, login }) => {
    login('admin');
});

Scenario('Verify insertion of custom JavaScript', ({ I }) => {
    I.amOnPage('/admin/v9/');
    I.waitForElement('#toast-container-overview', 10);
    I.dontSee("Produkčné prostredie", '#toast-container-overview');

    I.amOnPage('/admin/v9/?act=adminScript');
    I.waitForElement('#toast-container-overview', 10);
    I.see("Produkčné prostredie", '#toast-container-overview');
});

Scenario('Verify insertion of custom CSS', ({ I }) => {
    const cssLink = '<link href="/components/aceintegration/admin/style-test.css" rel="stylesheet">';
    const scriptTag = '<script src="/components/aceintegration/admin/script-test.js" type="text/javascript"></script>';
    I.amOnPage('/admin/v9/');
    I.dontSeeInSource(cssLink);
    I.dontSeeInSource(scriptTag);

    I.amOnPage('/admin/v9/?act=adminScript');
    I.seeInSource(cssLink);
    I.seeInSource(scriptTag);
});