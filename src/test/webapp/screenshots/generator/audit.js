Feature('app.audit');

Before(({ I, login }) => {
    login('admin');
});

Scenario("audit - screenshots", async ({ I, DT, DTE, Document, TempMail }) => {
    I.amOnPage("/admin/v9/apps/audit-search/");
    Document.screenshot("/sysadmin/audit/audit-search.png");
    I.wait(1);

    I.amOnPage("/admin/v9/apps/audit-changed-webpages/");
    DT.waitForLoader();
    Document.screenshot("/sysadmin/audit/audit-changed-webpages.png");
    I.wait(1);

    I.amOnPage("/admin/v9/apps/audit-awaiting-publish-webpages/");
    DT.waitForLoader();
    Document.screenshot("/sysadmin/audit/audit-awaiting-publish-webpages.png");

    I.amOnPage("/admin/v9/apps/audit-notifications/");
    DT.waitForLoader();
    Document.screenshot("/sysadmin/audit/audit-notification.png");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    const docId = 164;
    const auditEvent = `UPDATE:
id: ${docId}
date_created: `;
    const email = 'auditnotifi'+TempMail.getTempMailDomain();
    DTE.selectOption('adminlogType', 'SAVEDOC');
    DTE.fillField("text", auditEvent);
    DTE.fillField("email", email);
    Document.screenshot("/sysadmin/audit/audit-notification-editor.png", 1280, 450);
    DTE.cancel();
});

Scenario("log levels", async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/apps/audit-log-levels/");
    Document.screenshot("/sysadmin/audit/audit-log-levels-datatable.png");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    Document.screenshotElement("div.DTE_Action_Create", "/sysadmin/audit/audit-log-levels-editor.png");
});

Scenario("log files", async ({ I, Document }) => {
    I.amOnPage("/admin/v9/apps/audit-log-files/");
    Document.screenshot("/sysadmin/audit/audit-log-files-datatable.png");

    I.click(locate("a").withText("test-log-file.txt"));
    I.waitForElement("#modalIframe");

    I.wait(1);

    Document.screenshot("/sysadmin/audit/audit-log-files-file.png");
});