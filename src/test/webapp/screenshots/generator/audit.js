Feature('app.audit');

Before(({ I, login }) => {
    login('admin');
});

Scenario("audit - screenshots", ({ I, DTE, Document }) => {
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
    I.click("button.buttons-create");
    DTE.waitForEditor();
    const docId = 164;
    const auditEvent = `UPDATE:
id: ${docId}
date_created: `;
    const email = 'auditnotifi@fexpost.com';
    DTE.selectOption('adminlogType', 'SAVEDOC');
    DTE.fillField("text", auditEvent);
    DTE.fillField("email", email);
    Document.screenshot("/sysadmin/audit/audit-notification-editor.png", 1280, 450);
    DTE.cancel();
});

Scenario("log levels", async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/apps/audit-log-levels/");
    Document.screenshot("/sysadmin/audit/audit-log-levels-datatable.png");
    I.click("button.buttons-create");
    DTE.waitForEditor();
    Document.screenshotElement("div.DTE_Action_Create", "/sysadmin/audit/audit-log-levels-editor.png");
});

Scenario("log files", async ({ I, Document }) => {
    I.amOnPage("/admin/v9/apps/audit-log-files/");
    Document.screenshot("/sysadmin/audit/audit-log-files-datatable.png");

    const fileFullPath = await I.grabTextFrom("table#datatableInit > tbody > tr > td:nth-child(1) > div > a");
    I.click(fileFullPath);
    I.waitForElement("#modalIframe");

    I.wait(1);

    Document.screenshot("/sysadmin/audit/audit-log-files-file.png");
});