Feature('admin.cronjob');

var add_button = locate("button.btn.btn-sm.buttons-create.btn-success");
var delete_button = locate("button.btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider");
var edit_button = locate("button.btn.btn-sm.buttons-selected.buttons-edit.btn-warning");
var refresh_button = locate('button.btn.btn-sm.btn-outline-secondary.buttons-refresh.buttons-right');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/settings/cronjob/");
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        jobName = `This is param ${randomNumber}`;
        newTask = `sk.iway.iwcm.${randomNumber}`;
    }
});

Scenario('cronjob base test', async ({ I, DataTables }) => {
    await DataTables.baseTest({
        dataTable: 'cronTable',
        perms: 'cmp_crontab',
        skipSwitchDomain: true
    });
});

/**
 * There was an bug, where change of existing cronjob was not reflected, also delete of cronjob was not reflected
 */
Scenario('Verification of manifestation of changes in the cronjob', ({ I, DT, DTE }) => {
    I.say('Creating a new cronjob without auditing');
    addNewCronjob(I, DTE,'sk.iway.iwcm.system.cron.Echo', jobName, '*/5', false, true, false);

    I.say('Verification of a new cronjob');
    I.amOnPage('/admin/v9/apps/audit-search/');
    I.wait(6);
    I.dontSee(`Cron task executed: sk.iway.iwcm.system.cron.Echo [${jobName}]`);

    I.say('Edit cronjob to enable auditing');
    I.amOnPage("/admin/v9/settings/cronjob");
    DT.filter('taskName', `autotest-${randomNumber}`);
    I.click('button.buttons-select-all');
    I.click(edit_button);
    DTE.waitForEditor();
    I.checkOption('#DTE_Field_audit_0');
    DTE.save();

    I.say('Verification of a modified cronjob');
    I.amOnPage('/admin/v9/apps/audit-search/');
    I.wait(6);
    I.click(refresh_button);
    I.see(`Cron task executed: sk.iway.iwcm.system.cron.Echo [${jobName}]`);
});

Scenario('Delete new cronjob', ({ I, DT, DTE }) => {
    DT.filter('taskName', 'autotest');
    I.click('button.buttons-select-all');
    I.click(delete_button);
    DTE.waitForEditor();
    DTE.save();
});

Scenario('Verification that the deleted task does not manifest', async ({ I, DT }) => {
    I.say('Verification of a deleted cronjob');
    I.amOnPage('/admin/v9/apps/audit-search/');
    DT.filter('description', `Cron task executed: sk.iway.iwcm.system.cron.Echo [${jobName}]`);
    I.wait(6);
    I.click(refresh_button);
    let datatableInfo = await I.grabTextFrom('#datatableInit_info');
    const amoutOfRecordsOld = datatableInfo.match(/z (\d+)/)[1];

    I.wait(6);
    I.click(refresh_button);
    datatableInfo = await I.grabTextFrom('#datatableInit_info');
    const amoutOfRecordsNew = datatableInfo.match(/z (\d+)/)[1];

    I.assertEqual(amoutOfRecordsOld, amoutOfRecordsNew, 'There is a different number of records, despite the deletion of the Cronjob!');
});

Scenario('Delete all echo cronjobs', async ({ I, DT, DTE }) => {
    DT.filter('taskName', 'sk.iway.iwcm.system.cron.Echo');
    var totalRows = await I.getTotalRows();
    if (totalRows > 0) {
        DT.deleteAll();
    }
});


function addNewCronjob(I, DTE, task, params, seconds, runAtStartup, enableTask, audit) {
    let [years, daysOfMonth, daysOfWeek, months, hours, minutes] = Array(6).fill('*');
    I.click(add_button);
    DTE.waitForEditor();

    DTE.fillField('taskName', `autotest-${randomNumber}`);
    DTE.fillField('task', task);
    DTE.fillField('params', params);
    DTE.fillField('years', years);
    DTE.fillField('daysOfMonth', daysOfMonth);
    DTE.fillField('daysOfWeek', daysOfWeek);
    DTE.fillField('months', months);
    DTE.fillField('hours', hours);
    DTE.fillField('minutes', minutes);
    DTE.fillField('seconds', seconds);

    //verify default value for clusterNode
    I.seeInField("#DTE_Field_clusterNode", "all");

    if (runAtStartup) I.checkOption('#DTE_Field_runAtStartup_0');
    else I.checkOption('#DTE_Field_runAtStartup_0');
    if (enableTask) I.checkOption('#DTE_Field_enableTask_0');
    else  I.uncheckOption('#DTE_Field_enableTask_0');
    if (audit) I.checkOption('#DTE_Field_audit_0');
    else I.uncheckOption('#DTE_Field_audit_0');

    DTE.save();
}