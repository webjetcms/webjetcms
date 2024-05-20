Feature('app.audit');

Scenario("audit - screenshots", ({ I, Document }) => {
    I.amOnPage("/admin/v9/apps/audit-search/");
    Document.screenshot("/sysadmin/audit/audit-search.png");
    I.wait(1);

    I.amOnPage("/admin/v9/apps/audit-changed-webpages/");
    Document.screenshot("/sysadmin/audit/audit-changed-webpages.png");
    I.wait(1);

    I.amOnPage("/admin/v9/apps/audit-awaiting-publish-webpages/");
    Document.screenshot("/sysadmin/audit/audit-awaiting-publish-webpages.png");

    I.amOnPage("/admin/v9/apps/audit-notifications/");
    Document.screenshot("/sysadmin/audit/audit-notification.png");
});