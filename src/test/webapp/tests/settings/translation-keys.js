Feature('settings.translation-keys');

var randomNumber, newEntityName, value1, value2, value3;
let testingData = null;

Before(({ I, login }) =>{
    login('admin');
    I.amOnPage("/admin/v9/settings/translation-keys/");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        value1 = "value-sk";
        value2 = "value-cz";
        value3 = "value-en";
    }
});

Scenario('zakladne testy @baseTest', async ({ I, DataTables }) =>{
    I.see("Kľúč");
    let options = await DataTables.baseTest({
        dataTable: 'translationKeysTable',
        perms: 'edit_text',
        createSteps: function(I, options) {
            I.fillField('#DTE_Field_fieldA', value1);
            I.fillField('#DTE_Field_fieldB', value2);
            I.fillField('#DTE_Field_fieldC', value3);
        },
        skipSwitchDomain: true
    });
    testingData = options.testingData;
});

function cellEdit(text, field, I, DT) {
    I.dontSee(text+"-celledit");
    I.forceClick(locate("td.dt-style-text-wrap div.datatable-column-width").withText(text));
    I.waitForElement("div.DTE.DTE_Bubble", 10);
    I.wait(1);
    I.fillField("#DTE_Field_field"+field, text+"-celledit");
    I.wait(1);
    I.click("div.DTE.DTE_Bubble button.btn.btn-primary");
    I.wait(1);
    I.waitForInvisible("div.DTE.DTE_Bubble", 100);
    DT.waitForLoader();
    I.see(text+"-celledit");
}

Scenario('celledit', ({ I, DT, DTE }) =>{
    if (testingData != null) {
        let key = testingData[0].replace("-chan.ge", "");
        DT.filter("key", key);
        I.forceClick("div.buttons-select-cel");
        I.wait(2);

        cellEdit("value-sk", "A", I, DT);
        cellEdit("value-cz", "B", I, DT);
        cellEdit("value-en", "C", I, DT);

        I.amOnPage("/admin/v9/settings/translation-keys/");
        DT.filter("key", key);
        I.see("value-sk-celledit");
        I.see("value-cz-celledit");
        I.see("value-en-celledit");

        I.click("td.dt-select-td.sorting_1");
        I.click("button.buttons-edit");
        DTE.waitForEditor();

        I.seeInField("#DTE_Field_fieldA", "value-sk-celledit");
        I.seeInField("#DTE_Field_fieldB", "value-cz-celledit");
        I.seeInField("#DTE_Field_fieldC", "value-en-celledit");

        DTE.cancel();
    }
});

Scenario('zoznam prekladovych klucov - zmazanie kluca', ({ I, DT, DTE }) =>{
    if (testingData != null) {
        //vyhladaj aj kluc pred doplnenim change, kedze ten zostava
        let key = testingData[0].replace("-chan.ge", "");
        DT.filter("key", key);

        I.click("td.dt-select-td.sorting_1");
        I.click("button.buttons-remove");
        DTE.waitForEditor();
        I.waitForText("Zmazať", 10, "div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForLoader();
        I.dontSee(key, "div.dataTables_scrollBody table.datatableInit");
    }
});

Scenario('zobrazenie poli', async ({ I, DT, DTE, Document }) => {
    Document.setConfigValue("languages", "sk,cz,en,de,pl,hu,cho,ru,esp");

    I.amOnPage("/admin/v9/settings/translation-keys/");

    //Check headline
    I.see("Prekladové kľúče", "div.md-breadcrumb");

    var entity = "temp-group-2.project.name";
    DT.filter("key", entity);

    I.click(entity);
    DTE.waitForEditor();

    I.see(entity);

    //I see elements
    I.seeElement("#DTE_Field_fieldA");
    I.seeElementInDOM("#DTE_Field_originalValueA");
    I.seeElement("#DTE_Field_fieldB");
    I.seeElementInDOM("#DTE_Field_originalValueB");
    I.seeElement("#DTE_Field_fieldC");
    I.seeElementInDOM("#DTE_Field_originalValueC");
    //etc

    //I see set fields names
    I.see("Slovenský");
    I.see("Slovenský, pôvodná hodnota");
    I.see("Český");
    I.see("Český, pôvodná hodnota");
    I.see("Anglický");
    I.see("Anglický, pôvodná hodnota");
    //etc

    //Check field J, it should be hidden
    I.dontSeeElement("#DTE_Field_fieldJ");
    I.seeElement(".DTE_Field_Name_originalValueA");
    I.dontSeeElement(".DTE_Field_Name_originalValueJ");

    //Close editor
    DTE.cancel();
});

Scenario('vyplnenie poli', async ({ I, DT, DTE }) => {
    newEntityName = "autotest-delete-" + randomNumber;

    I.click("button.buttons-create");
    DTE.waitForEditor();

    //Check that originalValueFields are hidden
    I.dontSeeElement(".DTE_Field_Name_originalValueA");
    I.dontSeeElement(".DTE_Field_Name_originalValueB");
    I.dontSeeElement(".DTE_Field_Name_originalValueC");
    //etc

    //Set values
    I.click('#DTE_Field_fieldA');
    I.fillField('#DTE_Field_fieldA', value1);
    I.click('#DTE_Field_fieldB');
    I.fillField('#DTE_Field_fieldB', value2);
    I.click('#DTE_Field_fieldC');
    I.fillField('#DTE_Field_fieldC', value3);

    //Test ERROR
    DTE.save();
    I.see("Povinné pole. Zadajte aspoň jeden znak.");
    I.click('#DTE_Field_key');
    I.fillField('#DTE_Field_key', newEntityName);

    DTE.save();

    DT.filter("key", newEntityName);

    //Check of save
    I.click(newEntityName);
    DTE.waitForEditor();

    I.see(newEntityName);
    I.see(value1);
    I.see(value2);
    I.see(value3);

    //Close editor
    DTE.cancel();
});

Scenario('overenie filtrovania', async ({ I, DT, DTE }) => {
    DT.filter("key", newEntityName);

    //Check field filter
    DT.filter("fieldA", value1);
    DT.filter("fieldB", value2);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    DTE.waitForEditor();

    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    DT.waitForLoader();

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.dontSee("Kľúč nie je možné zmazať");
 });

 Scenario('overenie zmazania kluca zo suboru', async ({ I, DT, DTE }) => {
    var entity = "admin.fck.last_pages";

    DT.filter("key", entity);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    DTE.waitForEditor();

    I.click("Zmazať", "div.DTE_Action_Remove");

    //We cant remove allready set keys in file
    I.see("Zvolený prekladový kľúč pre jazyky SK,CZ,EN,DE,HU je načítaný zo súboru, a preto ho nie je možné zmazať. Mazať je možné len upravené alebo novo vytvorené kľuče.");
    I.wait(5);

    //now check update plus again delete :D
    I.click(entity);
    DTE.waitForEditor();

    //Change only 2 values (but all this values will be set into DB ass new values)
    I.click('#DTE_Field_fieldA');
    I.fillField('#DTE_Field_fieldA', value1);
    I.click('#DTE_Field_fieldB');
    I.fillField('#DTE_Field_fieldB', value2);

    DTE.save();
    I.wait(2);

    I.click(entity);
    DTE.waitForEditor();
    I.see(value1);
    I.see(value2);

    //Close editor
    DTE.cancel();

    //Now all values can bee delete, so delete without popup msg
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.dontSee("Kľúč nie je možné zmazať");

    //BUT key is still there just with original values from FILE
    I.click(entity);
    DTE.waitForEditor();
    I.dontSee(value1);
    I.dontSee(value2);
 });

 Scenario('overenie zobrazenia so zakladnymi pravami', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/translation-keys");
    DT.filter("key", "components.banner.add_new_banner");
    I.see("Pridať nový banner");

    I.amOnPage("/admin/v9/settings/translation-keys?removePerm=prop.show_all_texts");
    DT.filter("key", "components.banner.add_new_banner");
    I.dontSee("Pridať nový banner");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario("odhlasenie", ({ I }) => {
    I.logout();
});

Scenario("vyhladavanie zacina na/konci na", ({ I, DT }) => {

    I.amOnPage("/admin/v9/settings/translation-keys/");
    DT.filter("key", "qa.r", "Začína na");
    I.waitForText("qa.result.ok", 120);
    I.dontSee("components.qa.");

    DT.filter("key", ".answer", "Končí na");
    I.see("qa.form.answer");
    I.dontSee("inquiry.answer_text_ok_default");

    DT.filter("key", "inquiry.answer", "Rovná sa");
    I.see("inquiry.answer");
    I.see("Záznamy 1 až 1 z 1");
});

Scenario('import nema moznost aktualizovat zaznam podla', async ({ I, DT, DTE }) => {
    I.click("button.btn-import-dialog");
    I.waitForElement("#datatableImportModal");
    I.see("Importovať nové a aktualizovať existujúce", "#import-settings");
    I.see("Importovať iba nové záznamy", "#import-settings");
    I.dontSee("Aktualizovať existujúce záznamy", "#import-settings");
});

Scenario("BUG key with tab prefix/suffix", ({ I, DT, DTE }) => {

    DT.waitForLoader();
    I.click("button.buttons-create");
    DTE.waitForEditor();

    var key = "autotest-"+I.getRandomText();
    var tab = "\t";

    I.fillField("#DTE_Field_key", tab + key + tab);
    I.fillField("#DTE_Field_fieldA", value1);

    DTE.save();

    //
    I.say("Filtering key");
    DT.filter("key", key);

    I.see(key);
    I.see("Záznamy 1 až 1 z 1");

    I.click(key);
    DTE.waitForEditor();
    I.fillField("#DTE_Field_fieldB", value2);
    DTE.save();

    I.see(key);
    I.see("Záznamy 1 až 1 z 1");

    I.forceClick("td.dt-select-td");
    I.click("button.buttons-remove");
    DTE.waitForEditor();
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();

    I.dontSee(key);
    I.see("Záznamy 0 až 0 z 0");

});