Feature('apps.restaurant-menu.meals');

Before(({ login }) => {
    login('admin');
});

Scenario('zakladne testy @baseTest', async ({I, DataTables}) => {
    I.amOnPage("/apps/restaurant-menu/admin/meals/");
    await DataTables.baseTest({
        dataTable: 'mealsDataTable',
        perms: 'cmp_restaurant_menu',
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

Scenario('testy domainId', ({I, DT, Document}) => {
    I.amOnPage("/apps/restaurant-menu/admin/meals/");
    DT.waitForLoader();
    I.see("Gulášová", "#mealsDataTable");
    DT.filterContains("name", "Test23");
    I.dontSee("Test23 polievka", "#mealsDataTable");
    I.dontSee("Test23 jedlo", "#mealsDataTable");

    Document.switchDomain("test23.tau27.iway.sk");
    DT.waitForLoader();
    I.dontSee("Gulášová", "#mealsDataTable");
    I.see("Test23 polievka", "#mealsDataTable");
    I.see("Test23 jedlo", "#mealsDataTable");
});

Scenario('logoff', ({I}) => {
    I.logout();
});