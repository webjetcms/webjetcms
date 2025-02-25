Feature('components.configuration');

var datatableName = "configurationDatatable";

Before(({ login }) => {
    login('admin');
});

Scenario('Components configuration', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/settings/configuration/");

    I.moveCursorTo('#toast-container-webjet');
    Document.screenshot("/admin/setup/configuration/page.png");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor(datatableName);
    I.clickCss("#DTE_Field_name");
    I.pressKey('A');
    I.pressKey('B');
    I.pressKey('T');
    I.pressKey('e');
    I.pressKey('s');
    Document.screenshotElement("#configurationDatatable_modal > div > div.DTE_Action_Create", "/admin/setup/configuration/editor_1.png");
    I.click( locate(".ui-menu-item-wrapper").withText("ABTesting") );

    Document.screenshotElement("#configurationDatatable_modal > div > div.DTE_Action_Create", "/admin/setup/configuration/editor_2.png");
});