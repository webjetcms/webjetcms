Feature('apps.dmail.domain-limits');

Before(({ I, login }) => {
    login('admin');
});

Scenario('domain-limits-zakladne testy @baseTest', async ({I, DataTables}) => {

    I.amOnPage("/apps/dmail/admin/domain-limits/");
    await DataTables.baseTest({
        dataTable: 'domainLimitsDataTable',
        perms: 'cmp_dmail_domainlimits',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
        skipSwitchDomain: true
    });
});
