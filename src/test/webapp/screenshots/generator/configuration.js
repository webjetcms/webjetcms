Feature('components.configuration');

var datatableName = "configurationDatatable";

Before(({ login }) => {
    login('admin');
});

Scenario('Components configuration', ({ I, DT, DTE, Document }) => {
    Document.setConfigValue('editorAutoFillPublishStart', 'true');
    Document.setConfigValue('usersSplitByDomain', 'false');
    Document.setConfigValue('ragSemanticSearchEnabled', 'false');

    I.amOnPage("/admin/v9/settings/configuration/");
    I.waitForElement("#SomStromcek .jstree-anchor", 10);
    I.waitForText("Zmenené", 10, "#SomStromcek");

    //
    I.say("Creating temporary value");
    DT.filterContains("name", "editorAutoFillPublishStart");
    I.click("editorAutoFillPublishStart", ".dt-row-edit div a");
    DTE.waitForEditor("configurationDatatable");
    DTE.fillField("value", "false");
    I.checkOption("#DTE_Field_temporary_0");
    DTE.save();
    I.amOnPage("/admin/v9/settings/configuration/");
    DT.waitForLoader();

    const databaseValueSelector = ".configuration-value__database";
    I.waitForVisible(databaseValueSelector, 10);
    I.executeScript((selector) => {
        const element = document.querySelector(selector);
        if (element == null) throw new Error(`Element not found: ${selector}`);
        element.focus();
    }, databaseValueSelector);
    I.waitForVisible(".configuration-value-tooltip.show", 5);
    Document.screenshot("/admin/setup/configuration/page.png");
    I.pressKey("Escape");
    I.waitToHide(".configuration-value-tooltip.show", 5);
    I.toastrClose();

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
