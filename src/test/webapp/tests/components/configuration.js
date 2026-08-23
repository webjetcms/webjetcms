Feature('components.configuration');

var randomNumber, name, value;
var datatableName = "configurationDatatable";

async function getConfigurationIdColumnLayout(I) {
    return await I.executeScript(() => {
        const table = configurationDatatable;
        table.columns.adjust();

        const rowIndexes = table.rows({ search: "applied" }).indexes().toArray();
        if (rowIndexes.length !== 1) throw new Error(`Expected one filtered configuration row, got ${rowIndexes.length}`);

        const row = table.row(rowIndexes[0]);
        const idCell = row.node().querySelector("td.dt-select-td");
        const idContent = idCell.querySelector(":scope > .datatable-column-width");
        return {
            id: Number(row.data().id),
            width: idCell.getBoundingClientRect().width,
            idContentDisplay: getComputedStyle(idContent).display
        };
    });
}

Before(({ I, login, DT }) => {

    login('admin');
    I.amOnPage("/admin/v9/settings/configuration/");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        name = "name-autotest-" + randomNumber;
        value = "value-autotest-" + randomNumber+"<script>alert('TEST');</script> &#39; poKUS frame-ancestors 'self'";
    }
    DT.addContext('config','#configurationDatatable_wrapper');
});

Scenario('zoznam konfiguracnych premennych', ({ I }) => {

    I.see("Predvolená hodnota (default value)");
    I.seeElement("#configurationDatatable.dt-hide-id");
    I.seeElement("#configurationDatatable tbody td.dt-select-td");
    I.dontSeeElement("#configurationDatatable.dt-hide-id tbody td.dt-select-td > .datatable-column-width");
});

Scenario('temporary setting hides encryption and scheduled change', ({ I, DTE }) => {
    I.click("button.buttons-create");
    DTE.waitForEditor(datatableName);

    I.waitForVisible("div.DTE_Field_Name_encrypt", 10);
    I.waitForVisible("div.DTE_Field_Name_datePrepared", 10);

    DTE.clickSwitch("temporary_0");
    I.waitForInvisible("div.DTE_Field_Name_encrypt", 10);
    I.waitForInvisible("div.DTE_Field_Name_datePrepared", 10);

    DTE.clickSwitch("temporary_0");
    I.waitForVisible("div.DTE_Field_Name_encrypt", 10);
    I.waitForVisible("div.DTE_Field_Name_datePrepared", 10);

    DTE.cancel();
});

Scenario('pridanie konfiguracnej premennej @baseTest', ({ I, DT, DTE }) => {

    I.click("button.buttons-create");
    I.click("Pridať");
    DTE.waitForEditor(datatableName);

    I.see("Povinné pole", "div.DTE_Field_Name_name");
    I.fillField("#DTE_Field_name", name);
    I.fillField("#DTE_Field_value", value);
    DTE.save();

    I.see("Predvolená hodnota (default value)");

    //over ze je v zozname (bez reloadu)
    I.fillField("input.dt-filter-name", randomNumber);
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.see(name);

    //po pridani noveho zaznamu a hned jeho editacii padlo REST volanie - nebolo nastavene ID zaznamu
    I.click(name);
    DTE.waitForEditor(datatableName);
    DTE.save();
    I.dontSee("JSON parse error");
});

Scenario('docasna hodnota sa zobrazi, ale do editora sa nacita databazova @baseTest', async ({ I, DT, DTE, a11y }) => {
    const temporaryValue = "temporary-value-autotest-" + randomNumber;
    const databaseValueSelector = "#configurationDatatable tbody [data-conf-value='database-inactive']";

    DT.filterEquals("name", name);
    const initialIdLayout = await getConfigurationIdColumnLayout(I);
    I.click(name);
    DTE.waitForEditor(datatableName);

    I.fillField("#DTE_Field_value", temporaryValue);
    DTE.clickSwitch("temporary_0");
    DTE.save();

    I.waitForText(temporaryValue, 10, "#configurationDatatable");
    I.waitForVisible(databaseValueSelector, 10);
    I.see(value, "#configurationDatatable");
    I.dontSee("DB (neaktívna):", "#configurationDatatable");
    I.dontSeeElement("#configurationDatatable .configuration-value__database-label");
    I.dontSeeElement("#configurationDatatable .configuration-value .visually-hidden");
    I.see(temporaryValue, "#configurationDatatable [data-conf-value='current']");
    I.see(value, databaseValueSelector);
    I.seeElement(databaseValueSelector + "[data-bs-toggle='tooltip'][tabindex='0']");

    I.moveCursorTo(databaseValueSelector);
    I.waitForElement(databaseValueSelector + "[aria-describedby]", 5);
    let tooltipId = await I.grabAttributeFrom(databaseValueSelector, "aria-describedby");
    I.assertTrue(Boolean(tooltipId), "Database value tooltip must describe its trigger");
    I.waitForVisible("#" + tooltipId + ".tooltip.show[role='tooltip']", 5);
    I.see("Hodnota uložená v databáze, momentálne neaktívna", "#" + tooltipId + ".tooltip.show[role='tooltip']");

    I.moveCursorTo("#" + tooltipId);
    // Verify that the tooltip stays open beyond its 300 ms hide grace period.
    I.wait(0.5);
    I.seeElement("#" + tooltipId + ".tooltip.show[role='tooltip']");
    I.pressKey("Escape");
    I.waitToHide("#" + tooltipId, 5);
    I.waitForInvisible(databaseValueSelector + "[aria-describedby]", 5);

    I.moveCursorTo("#configurationDatatable tbody td.dt-select-td");
    I.moveCursorTo(databaseValueSelector);
    I.waitForElement(databaseValueSelector + "[aria-describedby]", 5);
    tooltipId = await I.grabAttributeFrom(databaseValueSelector, "aria-describedby");
    I.waitForVisible("#" + tooltipId + ".tooltip.show[role='tooltip']", 5);
    await I.executeScript(() => configurationDatatable.draw(false));
    I.waitToHide("#" + tooltipId, 5);
    I.waitForInvisible(databaseValueSelector + "[aria-describedby]", 5);

    I.focus(databaseValueSelector);
    I.waitForElement(databaseValueSelector + "[aria-describedby]", 5);
    tooltipId = await I.grabAttributeFrom(databaseValueSelector, "aria-describedby");
    I.assertTrue(Boolean(tooltipId), "Focused database value must expose its tooltip through aria-describedby");
    I.waitForVisible("#" + tooltipId + ".tooltip.show[role='tooltip']", 5);
    I.see("Hodnota uložená v databáze, momentálne neaktívna", "#" + tooltipId + ".tooltip.show[role='tooltip']");
    I.pressKey("Escape");
    I.waitToHide("#" + tooltipId, 5);
    I.waitForInvisible(databaseValueSelector + "[aria-describedby]", 5);
    I.blur(databaseValueSelector);

    const adjustedIdLayout = await getConfigurationIdColumnLayout(I);
    I.assertTrue(Number.isSafeInteger(adjustedIdLayout.id), "Configuration ID must be a safe JavaScript integer");
    I.assertEqual(adjustedIdLayout.id, initialIdLayout.id);
    I.assertEqual(adjustedIdLayout.idContentDisplay, "none");
    I.assertTrue(adjustedIdLayout.width <= initialIdLayout.width + 1, "ID column must stay compact after recalculating column widths");

    await a11y.check(databaseValueSelector);
    I.click("#configurationDatatable tbody td.dt-select-td");
    I.waitForVisible("#configurationDatatable tbody tr.selected", 5);
    await a11y.check(databaseValueSelector);

    I.click(name);
    DTE.waitForEditor(datatableName);
    I.seeInField("#DTE_Field_value", value);
    DTE.cancel();

    I.see(temporaryValue, "#configurationDatatable");
    I.see(value, "#configurationDatatable");
});

Scenario('vyhladanie konfiguracnej premennej @baseTest', ({ I, DT }) => {

    //hladanie podla mena
    I.fillField("input.dt-filter-name", randomNumber);
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.see(name);

    //hladanie podla hodnoty
    I.fillField("input.dt-filter-value", "&#39; pokus");
    I.pressKey('Enter', "input.dt-filter-value");
    DT.waitForLoader();
    I.see(value);
});

Scenario("upravenie konfiguracnej premennej @baseTest", ({ I, DTE }) => {

    I.fillField("input.dt-filter-name", name);
    I.pressKey('Enter', "input.dt-filter-name");

    I.click("th.dt-format-selector");
    I.click("th.dt-format-selector");
    I.click(name);
    DTE.waitForEditor(datatableName);

    I.seeInField("#DTE_Field_value", value);

    value = value + ".changed";

    I.fillField("#DTE_Field_value", value);
    DTE.save();

    I.see(value);

    I.click(name);
    DTE.waitForEditor(datatableName);

    I.seeInField("#DTE_Field_value", value);
});

//chyba v nastaveni id
//detto pri editacii existujuceho, ked sa ulozilo a dalo sa znova editovat padlo
Scenario("overenie nastavenia ID-edit @baseTest", ({ I, DT, DTE }) => {

    DT.filterContains("name", name)
    I.click(name);
    DTE.waitForEditor(datatableName);
    DTE.save();

    I.wait(1);

    I.click(name);
    DTE.waitForEditor(datatableName);
    DTE.save();
    I.dontSee("JSON parse error");
});

Scenario("zmazanie konfiguracnej premennej @baseTest", ({ I, DT }) => {

    I.fillField("input.dt-filter-value", value);
    I.pressKey('Enter', "input.dt-filter-name");

    I.click("th.dt-format-selector");
    I.click("th.dt-format-selector");
    I.click("td.dt-select-td");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();

    I.fillField("input.dt-filter-value", value);
    I.pressKey('Enter', "input.dt-filter-value");
    I.dontSee(name);
});

Scenario("oznacovanie vyfiltrovanych riadkov", async ({ I }) => {
    const assert = require('assert');
    I.see("Názov konfigurácie");
    I.fillField("input.dt-filter-name", "admin");
    I.pressKey('Enter', 'input.dt-filter-name');
    I.click('.dt-filter-id');
    const info = await I.grabHTMLFrom('.select-item:first-child');
    const infoData = info.split(" ").shift();
    within('tbody', async () => {
        const numOfElements = await I.grabNumberOfVisibleElements('tr');
        assert.equal(+numOfElements, +infoData);
    });
});

Scenario("planovanie a historia", async ({ I, DTE }) => {
    const historyName = "aaatest";
    I.click("button.toast-close-button")
    I.fillField("input.dt-filter-value", historyName);
    I.pressKey('Enter', "input.dt-filter-name");

    I.click(historyName);
    DTE.waitForEditor(datatableName);

    I.clickCss("#pills-dt-configurationDatatable-advanced-tab");
    I.see("aaatest - CHANGED - manual 16:10B");
    I.dontSee("aaatest zakladna hodnota-v3");
    I.clickCss("#datatableFieldDTE_Field_confPrepared tr:nth-child(3) td");
    I.clickCss("#datatableFieldDTE_Field_confPrepared_wrapper button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove", 5);
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Chyba: zmazať je možné len záznamy v budúcnosti");
    I.click("div.DTE_Action_Remove div.DTE_Header button.btn-close-editor");

    I.clickCss("#pills-dt-configurationDatatable-history-tab")
    I.see("aaatest zakladna hodnota-v3");
    I.dontSee("aaatest - CHANGED - manual 16:10B");
});

Scenario("overenie prav editacie vsetkych premennych", ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/settings/configuration/?removePerm=conf.show_all_variables");
    I.dontSee("structureMirroringConfig");

    DT.filterContains("name", "structureMirroringConfig");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.click(DT.btn.config_add_button);
    DTE.waitForEditor("configurationDatatable");

    I.dontSee("História", "#pills-dt-editor-configurationDatatable");

    I.fillField("#DTE_Field_name", "structureMirroringConfig");
    I.fillField("#DTE_Field_value", "34364,34365:mirroring.tau27.iway.sk");
    DTE.save();
    I.seeElement("#configurationDatatable_modal");
    I.see("Na editáciu tejto konfiguračnej premennej nemáte právo");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty.");

    //skus vyplnit planovanu zmenu
    I.fillField("#DTE_Field_datePrepared", "09.09.2037 08:40:15");
    I.pressKey("Tab");
    DTE.save();
    I.see("Na editáciu tejto konfiguračnej premennej nemáte právo");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty.");

    DTE.cancel();
});

Scenario("logout", ({ I }) => {
    I.logout();
});

const testConfiguration = "smsSendMaxlength";
Scenario("check setting oldValue after delete", async ({ I, DT, DTE }) => {
    const oldValue = 140;
    const newValue = 299792;

    I.amOnPage("/admin/v9/settings/configuration/");

    I.say("Check actual and default value");
        I.click(DT.btn.config_add_button);
        DTE.waitForEditor("configurationDatatable");

        DTE.fillField("name", testConfiguration);
        I.waitForVisible( locate("div.ui-menu-item-wrapper").withText(testConfiguration) );
        I.click( locate("div.ui-menu-item-wrapper").withText(testConfiguration) );

        I.seeInField("#DTE_Field_value", oldValue);
        I.seeInField("#DTE_Field_oldValue", oldValue);

    I.say("Change value and check that value stay changed. But original value of not changed.");
        DTE.fillField("value", newValue);
        DTE.save();

        DT.filterEquals("name", testConfiguration);
        I.click(testConfiguration);
        DTE.waitForEditor("configurationDatatable");

        I.seeInField("#DTE_Field_value", newValue);
        I.seeInField("#DTE_Field_oldValue", oldValue);
        DTE.cancel();

    I.say("Delete value from DB.");
        I.clickCss("td.dt-select-td");
        I.clickCss("button.buttons-remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.dontSee(testConfiguration);

    I.say("Check, that after delete values are back.");
        I.clickCss("button.buttons-create");
        DTE.waitForEditor("configurationDatatable");

        DTE.fillField("name", testConfiguration);
        I.waitForVisible( locate("div.ui-menu-item-wrapper").withText(testConfiguration) );
        I.click( locate("div.ui-menu-item-wrapper").withText(testConfiguration) );

        I.seeInField("#DTE_Field_value", oldValue);
        I.seeInField("#DTE_Field_oldValue", oldValue);
});

Scenario("Post delete", async ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/configuration/");
    DT.filterEquals("name", testConfiguration);
    const rowCount = await I.grabNumberOfVisibleElements('#configurationDatatable > tbody > tr > td.dt-row-edit');
    if(rowCount > 0) {
        I.clickCss("td.dt-select-td");
        I.clickCss("button.buttons-remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.waitForLoader();
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
});

Scenario("odhlasenie", ({ I }) => {
    I.logout();
});
