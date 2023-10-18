Feature('apps.response-header');

Before(({ I, login }) => {
    login('admin');
});

Scenario('response headers screenshots', ({ I, DTE, Document }) => {
    I.amOnPage("/apps/response-header/admin/");

    Document.screenshot("/admin/settings/response-header/dataTable.png");

    I.fillField("input.dt-filter-url", "/apps/http-hlavicky/");
    I.pressKey('Enter', "input.dt-filter-url");
    DTE.waitForLoader();

    I.click("/apps/http-hlavicky/");

    Document.screenshot("/admin/settings/response-header/editor.png");
});