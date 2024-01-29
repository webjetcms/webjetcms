Feature('webpages.customfields');

Before(({ I, login }) => {
    login('admin');
});

Scenario('custom-fields-webpages', async ({ I, DT, DTE }) => {

    I.say("Overujem zobrazenie volitelnych poli vo web stranke.");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=8487");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-fields-tab");
    DT.waitForLoader();

    I.see("text - A", "#datatableInit_modal div.DTE_Field_Name_fieldA");
    I.see("select - B", "#datatableInit_modal div.DTE_Field_Name_fieldB");
    I.dontSeeElement("#datatableInit_modal div.DTE_Field_Name_fieldC");
    I.see("autocomplete - D", "#datatableInit_modal div.DTE_Field_Name_fieldD");
    I.dontSee("Pole A", "#datatableInit_modal div.DTE_Field_Name_fieldA");
    I.dontSee("Pole B", "#datatableInit_modal div.DTE_Field_Name_fieldB");
    I.dontSee("Pole D", "#datatableInit_modal div.DTE_Field_Name_fieldD");
    //
    I.say("Checking UUID preserved value")
    I.seeInField("#datatableInit_modal div.DTE_Field_Name_fieldM input.field-type-uuid", "7ddaab7f-3d08-b5ab-1e97-1528963fb99a")
    I.say("Checking new UUID value");
    DTE.cancel();
    I.click("#datatableInit_wrapper button.buttons-create");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-fields-tab");
    DT.waitForLoader();
    var uuid = await I.grabValueFrom("#datatableInit_modal div.DTE_Field_Name_fieldM input.field-type-uuid");
    //check uuid has 36 chars
    I.assertEqual(36, uuid.length);

    I.say("Overujem zobrazenie standardnych poli.");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Test stavov", "Zobrazený v menu"]);
    I.click("Zobrazený v menu", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-fields-tab");
    DT.waitForLoader();

    I.see("Pole A", "#datatableInit_modal div.DTE_Field_Name_fieldA");
    I.see("Pole B", "#datatableInit_modal div.DTE_Field_Name_fieldB");
    I.seeElement("#datatableInit_modal div.DTE_Field_Name_fieldC");
    I.see("Pole D", "#datatableInit_modal div.DTE_Field_Name_fieldD");
    I.dontSee("text - A", "#datatableInit_modal div.DTE_Field_Name_fieldA");
    I.dontSee("select - B", "#datatableInit_modal div.DTE_Field_Name_fieldB");
    I.dontSee("autocomplete - D", "#datatableInit_modal div.DTE_Field_Name_fieldD");
});

Scenario('custom-fields-groups', ({ I, DT, DTE }) => {

    I.say("Overujem zobrazenie volitelnych poli v adresari.");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Test stavov", "Voliteľné polia"]);
    I.click("div.tree-col button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.click("#pills-dt-groups-datatable-fields-tab");
    DT.waitForLoader();

    I.see("group text - A", "#groups-datatable_modal div.DTE_Field_Name_fieldA");
    I.see("group select - B", "#groups-datatable_modal div.DTE_Field_Name_fieldB");
    I.dontSeeElement("#groups-datatable_modal div.DTE_Field_Name_fieldC");
    I.see("group autocomplete - D", "#groups-datatable_modal div.DTE_Field_Name_fieldD");
    I.dontSee("Pole A", "#groups-datatable_modal div.DTE_Field_Name_fieldA");
    I.dontSee("Pole B", "#groups-datatable_modal div.DTE_Field_Name_fieldB");
    I.dontSee("Pole D", "#groups-datatable_modal div.DTE_Field_Name_fieldD");

    I.say("Overujem zobrazenie standardnych poli.");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Test stavov", "Zobrazený v menu"]);
    I.click("div.tree-col button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.click("#pills-dt-groups-datatable-fields-tab");
    DT.waitForLoader();

    I.dontSee("group text - A", "#groups-datatable_modal div.DTE_Field_Name_fieldA");
    I.dontSee("group select - B", "#groups-datatable_modal div.DTE_Field_Name_fieldB");
    I.seeElement("#groups-datatable_modal div.DTE_Field_Name_fieldC");
    I.dontSee("group autocomplete - D", "#groups-datatable_modal div.DTE_Field_Name_fieldD");
    I.see("Pole A", "#groups-datatable_modal div.DTE_Field_Name_fieldA");
    I.see("Pole B", "#groups-datatable_modal div.DTE_Field_Name_fieldB");
    I.see("Pole D", "#groups-datatable_modal div.DTE_Field_Name_fieldD");
});
