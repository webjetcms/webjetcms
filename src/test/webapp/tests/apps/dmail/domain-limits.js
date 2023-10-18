Feature('apps.dmail.domain-limits');

Before(({ I, login }) => {
    login('admin');
});

Scenario('domain-limits-zakladne testy', async ({I, DataTables}) => {

    I.amOnPage("/apps/dmail/admin/domain-limits/");
    await DataTables.baseTest({
        dataTable: 'domainLimitsDataTable',
        perms: 'menuEmail',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});
