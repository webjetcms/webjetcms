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

Scenario('Domain test', ({I, DT, Document}) => {
    I.amOnPage("/apps/gdpr/admin/regexps/");

    DT.filter("regexpName", "TestDomain_webjet9");
    I.see("TestDomain_webjet9");
    DT.filter("regexpName", "TestDomain_test23");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    Document.switchDomain("test23.tau27.iway.sk");

    DT.filter("regexpName", "TestDomain_webjet9");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filter("regexpName", "TestDomain_test23");
    I.see("TestDomain_test23");
});

Scenario('logout', async ({I}) => {
    I.logout();
});
