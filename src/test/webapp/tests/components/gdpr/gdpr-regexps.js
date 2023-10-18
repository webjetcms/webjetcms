Feature('components.gdpr.regexps');

Before(({ I, login }) => {
    login('admin');
});

Scenario('gdpr-regexps-zakladne testy', async ({I, DataTables }) => {
    //start in new fresh tab
    I.openNewTab();
    //close other tabs if there are left any open from previous tests
    I.closeOtherTabs();

    I.amOnPage("/apps/gdpr/admin/regexps/");
    await DataTables.baseTest({
        dataTable: 'regExpDataTable',
        perms: 'menuGDPRregexp',
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

Scenario('logout', async ({I}) => {
    I.logout();
});
