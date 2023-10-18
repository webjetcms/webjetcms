Feature('components.forms-regexps');

Before(({ I, login }) => {
    login('admin');
});

Scenario('from-regexps zakladne testy', async ({I, DataTables}) => {
    I.amOnPage("/apps/form/admin/regexps/");
    await DataTables.baseTest({
        dataTable: 'regExpDataTable',
        perms: 'cmp_form',
        testingData: {
            type: "autotest-type"
        }
    });
});

Scenario('bug vyhladavanie', async ({I, DT}) => {
    //bol bug, ze pre jednoduche JpaRepository a searchByExample sa nepouzilo contains ale equals
    I.amOnPage("/apps/form/admin/regexps/");
    DT.filter("title", "phone");
    I.see("checkform.title.mobilePhone");
    I.see("checkform.title.fixedPhone");
    I.see("checkform.title.allPhone");

    I.dontSee("checkform.title.loginChars");
    I.dontSee("checkform.title.safeChars");
});