Feature('apps.tooltip');

var add_button = (locate('.tree-col').find('.btn.btn-sm.buttons-create.btn-success.buttons-divider'));

Before(({ I, login }) => {
    login('admin');
});

Scenario('zakladne testy', async ({I, DataTables}) => {
    I.amOnPage("/apps/tooltip/admin/");
    await DataTables.baseTest({
        dataTable: 'tooltipDataTable',
        perms: 'cmp_tooltip',
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

Scenario('test zmeny domeny pri vytvarani zaznamu', ({I}) => {
    I.amOnPage("/apps/tooltip/admin/");

    //domenovy selektor
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    //I.click(locate('.dropdown-item').withText("test23.tau27.iway.sk"));
    I.click("#bs-select-1-1");
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");
    I.wait(1);

    //kontrola predvyplneného pola domény
    I.click("button.buttons-create");
    I.click("Pridať");
    I.wait(1);
    I.see("test23.tau27.iway.sk", "div.DTE_Field_Name_domain div.filter-option div.filter-option-inner-inner");
});

Scenario('test zobrazenia na webe', ({I}) => {
    I.amOnPage("/teeeeestststst/tooltip-test.html");
    I.moveCursorTo(".wjtooltip");
    I.see("Toto je tooltip text ktorý ľudsky vysvetľuje odborný výraz");
});

Scenario('odhlasenie', ({I}) => {
    //lebo sme pred tym v teste zmenili domenu
    I.logout();
});
