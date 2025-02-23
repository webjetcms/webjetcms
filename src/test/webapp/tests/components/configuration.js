Feature('components.configuration');

var randomNumber, name, value;
var datatableName = "configurationDatatable";

Before(({ I, login, DT }) => {

    login('admin');
    I.amOnPage("/admin/v9/settings/configuration");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        name = "name-autotest-" + randomNumber;
        value = "value-autotest-" + randomNumber+"<script>alert('TEST');</script> &#39; poKUS frame-ancestors 'self'";
    }
    DT.addContext('config','#configurationDatatable_wrapper');
});

Scenario('zoznam konfiguracnych premennych', ({ I }) => {

    I.see("Predvolená hodnota (default value)");
});

Scenario('pridanie konfiguracnej premennej', ({ I, DT, DTE }) => {

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

Scenario('vyhladanie konfiguracnej premennej', ({ I, DT }) => {

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

Scenario("upravenie konfiguracnej premennej", ({ I, DTE }) => {

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
Scenario("overenie nastavenia ID-edit", ({ I, DT, DTE }) => {

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

Scenario("zmazanie konfiguracnej premennej", ({ I, DT }) => {

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

    I.click("#pills-dt-configurationDatatable-advanced-tab");
    I.see("aaatest - CHANGED - manual 16:10B");
    I.dontSee("aaatest zakladna hodnota-v3");
    I.click("#datatableFieldDTE_Field_confPrepared tr:nth-child(3) td");
    I.click("#datatableFieldDTE_Field_confPrepared_wrapper button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove", 5);
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Chyba: zmazať je možné len záznamy v budúcnosti");
    I.click("div.DTE_Action_Remove div.DTE_Header button.btn-close-editor");

    I.click("#pills-dt-configurationDatatable-history-tab")
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

Scenario("odhlasenie", ({ I }) => {
    I.logout();
});