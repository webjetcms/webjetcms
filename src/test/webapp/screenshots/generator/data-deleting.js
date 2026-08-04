Feature('admin.data-deleting');

Before(({ login }) => {
    login('admin');
});

Scenario('data-deletion screenshots', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/settings/database-delete/");
    I.fillField("div.dt-extfilter-from input.dt-filter-to-from", "01.08.2026 13:45");
    I.clickCss("div.dt-extfilter-from button.dt-filtrujem-from");
    DT.waitForLoader();
    Document.screenshot("/sysadmin/data-deleting/database-delete.png", 1280, 1200);

    I.amOnPage("/admin/v9/settings/cache-objects/");
    Document.screenshot("/sysadmin/data-deleting/cache-objects.png");

    I.amOnPage("/admin/v9/settings/persistent-cache-objects/");
    Document.screenshot("/sysadmin/data-deleting/persistent-cache-objects.png");
});

