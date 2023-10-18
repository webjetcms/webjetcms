Feature('apps.dmail.unsubscribed');

Before(({ I, login }) => {
    login('admin');
});

Scenario('unsubscribed-zakladne testy', async ({I, DataTables}) => {

    I.amOnPage("/apps/dmail/admin/unsubscribed/");
    await DataTables.baseTest({
        dataTable: 'unsubscribedDataTable',
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
