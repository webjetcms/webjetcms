Feature('admin.audit-notifications');

var add_button = locate('button.btn.btn-sm.buttons-create.btn-success.buttons-divider');
var delete_button = locate('button.btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
const docId = 164;
const auditEvent = `UPDATE:
id: ${docId}
date_created: `;
const email = 'auditnotification@fexpost.com';

Before(({ I, login }) => {
    login('admin');
});

Scenario('Test for Event Notification and Cache Handling in Audit Notifications', async ({ I, DT, DTE, TempMail }) => {
    TempMail.login('auditnotification');
    await TempMail.destroyInbox();
    savePage(I, DTE);
    await verifyEmailNotification(I, TempMail, false);

    I.amOnPage('/admin/v9/apps/audit-notifications');
    DT.waitForLoader();
    I.see('Zoznam notifikácií');
    I.click(add_button);
    DTE.waitForEditor();
    DTE.selectOption('adminlogType', 'SAVEDOC');
    DTE.fillField('text', auditEvent );
    DTE.fillField('email', email);
    DTE.save();

    savePage(I, DTE);
    await verifyEmailNotification(I, TempMail, true);
});

Scenario('Revert changes', ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/apps/audit-notifications');
    DT.filterSelect('adminlogType', "SAVEDOC");
    DT.filter('email', email);
    I.clickCss('.buttons-select-all');
    I.click(delete_button);
    DTE.waitForEditor();
    DTE.save();
    I.see('Nenašli sa žiadne vyhovujúce záznamy');
});

async function savePage(I, DTE) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=164');
    DTE.waitForEditor();
    DTE.save();
}

async function verifyEmailNotification(I, TempMail, expectNotification){
    TempMail.login('auditnotification');
    if (!await TempMail.isInboxEmpty())
        TempMail.openLatestEmail();
    if (expectNotification)
        I.see('Notifikácia akcie:SAVEDOC');
    else
        I.dontSee('Notifikácia akcie:SAVEDOC');
}