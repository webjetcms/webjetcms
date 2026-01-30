Feature('admin.audit-notifications');

const docId = 164;
const auditEvent = `UPDATE:
id: ${docId}
date_created: `;
var email = null;

Before(({ I, login, TempMail }) => {
    login('admin');

    if (email == null) email = 'auditnotification'+TempMail.getTempMailDomain();
});

Scenario('Test for Event Notification and Cache Handling in Audit Notifications', async ({ I, DT, DTE, TempMail}) => {
    await TempMail.login('auditnotification');
    await TempMail.destroyInbox();
    savePage(I, DTE);
    await verifyEmailNotification(I, TempMail, false);

    I.amOnPage('/admin/v9/apps/audit-notifications/');
    DT.waitForLoader();
    I.see('Zoznam notifikácií');
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    DTE.selectOption('adminlogType', 'SAVEDOC');
    DTE.fillField('text', auditEvent );
    DTE.fillField('email', email);
    DTE.save();

    savePage(I, DTE);
    await verifyEmailNotification(I, TempMail, true);
});

Scenario('Revert changes', ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/apps/audit-notifications/');
    DT.filterSelect('adminlogType', "SAVEDOC");
    DT.filterContains('email', email);
    I.clickCss('.buttons-select-all');
    I.click(DT.btn.delete_button);
    DTE.waitForEditor();
    DTE.save();
    I.see('Nenašli sa žiadne vyhovujúce záznamy');
});

function savePage(I, DTE) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=164');
    DTE.waitForEditor();
    I.wait(3);
    DTE.save();
    DTE.waitForModalClose();
}

async function verifyEmailNotification(I, TempMail, expectNotification){
    await TempMail.login('auditnotification');
    if (expectNotification) I.waitForText('Notifikácia akcie:SAVEDOC', 20);

    if (!await TempMail.isInboxEmpty())
        TempMail.openLatestEmail();
    if (expectNotification)
        I.see('Notifikácia akcie:SAVEDOC');
    else
        I.dontSee('Notifikácia akcie:SAVEDOC');
}