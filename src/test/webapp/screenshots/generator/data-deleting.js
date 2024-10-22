Feature('admin.data-deleting');

Before(({ login }) => {
    login('admin');
});

Scenario('data-deletion screenshots', ({ I, Document }) => {
    I.amOnPage("/admin/v9/settings/database-delete/");
    Document.screenshot("/sysadmin/data-deleting/database-delete.png");

    I.amOnPage("/admin/v9/settings/cache-objects/");
    Document.screenshot("/sysadmin/data-deleting/cache-objects.png");

    I.amOnPage("/admin/v9/settings/persistent-cache-objects/");
    Document.screenshot("/sysadmin/data-deleting/persistent-cache-objects.png");
});

