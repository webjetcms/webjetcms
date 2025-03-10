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
    I.click(DT.btn.add_button);
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
    I.click(DT.btn.tree_edit_button);
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
    I.click(DT.btn.tree_edit_button);
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

Scenario("BUG custom-fields-webpage-prefix", ({I}) => {
    I.amOnPage("/novy-adresar-01/volitelne-polia/?NO_WJTOOLBAR=true");
    I.waitForText("text - A", 5, "p.noprefix-a");
    I.waitForText("Temp Group Prefixed TEST", 5, "p.noprefix-test");
});

function checkCustomFieldsHeader(I, isCustom) {
    if (isCustom) {
        I.see("text - A", "#datatableInit_wrapper .datatableInit thead th.dt-th-fieldA");
        I.see("select - B", "#datatableInit_wrapper .datatableInit thead th.dt-th-fieldB");
    } else {
        I.see("Pole A", "#datatableInit_wrapper .datatableInit thead th.dt-th-fieldA");
        I.see("Pole B", "#datatableInit_wrapper .datatableInit thead th.dt-th-fieldB");
    }
}

Scenario('custom-fields-list @singlethread', async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    DT.resetTable();
    await DT.showColumn("Pole A");
    await DT.showColumn("Pole B");

    checkCustomFieldsHeader(I, false);
    I.jstreeClick("Voliteľné polia");
    checkCustomFieldsHeader(I, true);

    //
    I.say("Change sort order");
    I.clickCss("#datatableInit_wrapper .datatableInit thead .dt-th-id");
    checkCustomFieldsHeader(I, true);
    I.clickCss("#datatableInit_wrapper .datatableInit thead .dt-th-id");
    checkCustomFieldsHeader(I, true);

    //
    I.say("open webpage, yellow has different custom fields, it should not change header");
    I.click("Yellow Subpage", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-fields-tab");
    I.see("temp-6 - A", "label[for=DTE_Field_fieldA]");
    I.see("temp6-select - B", "label[for=DTE_Field_fieldB]");
    DTE.cancel();
    checkCustomFieldsHeader(I, true);

    //
    I.jstreeClick("Zobrazený v menu");
    I.clickCss("#datatableInit_wrapper .datatableInit thead .dt-th-id");
    checkCustomFieldsHeader(I, false);
    I.clickCss("#datatableInit_wrapper .datatableInit thead .dt-th-id");
    checkCustomFieldsHeader(I, false);

    //
    DT.resetTable();
});

Scenario('Optional fields - yellow template test', async ({ I, DT, DTE }) => {
    //
    I.say("Test on Volitelne polia folder");
    const fields_volitelne = ["text - A", "select - B", "autocomplete - D"];
    await checkOptionalFields(I, DTE, DT, fields_volitelne, '/admin/v9/webpages/web-pages-list/?groupid=7625', true);

    //
    I.say("Test on yellow folder - as jstree click");
    const fields_yellow = ["temp-6 - A", "temp6-select - B", "Pole D"];
    I.jstreeClick("Yellow Folder");
    await checkOptionalFields(I, DTE, DT, fields_yellow, null, false);

    //
    I.say("Test on yellow folder - as loaded");
    await checkOptionalFields(I, DTE, DT, fields_yellow, '/admin/v9/webpages/web-pages-list/?groupid=81154', false);
});

async function checkOptionalFields(I, DTE, DT, fields, pageUrl, showColumns) {
    if (pageUrl != null) I.amOnPage(pageUrl);
    const columnLabelSelector = "#datatableInit_wrapper tr > th > span.dt-column-title[role='button']";

    if (showColumns) {
        for (const field of fields) {
            await DT.showColumn(field);
        }
    }

    // over v stlpcoch
    fields.forEach(field => I.see(field, columnLabelSelector));

    // over v editore
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
    I.clickCss('#pills-dt-datatableInit-fields-tab');
    fields.forEach(field => I.seeElement(locate('#panel-body-dt-datatableInit-fields label').withText(field)));

    DTE.cancel();
};

Scenario('custom-fields-list-reset @singlethread', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    DT.resetTable();
});