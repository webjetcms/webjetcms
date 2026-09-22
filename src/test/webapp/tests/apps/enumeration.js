Feature('apps.enumeration');

var randomNumber;
var enumTypeNameA;
var enumTypeNameB;
var stringName = "testStringRow";
var numberName = "testNumberRow";
var booleanName = "testBooleanRow";
var stringTestValue = "testTestNow";
var numberTestValue = 369;
var stringFieldTypeEnumName;
var stringFieldOriginalName;
var stringFieldRenamedName;
var stringFieldOptionLabel;
var stringFieldOptionValue;
var stringFieldOptionLabel2;
var stringFieldOptionValue2;
var stringFieldTooltip;

const stringFieldsTableId = "datatableFieldDTE_Field_editorFields-stringFieldTypes";
const stringFieldsWrapper = "#" + stringFieldsTableId + "_wrapper";
const stringFieldsModal = "#" + stringFieldsTableId + "_modal";

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        stringFieldTypeEnumName = "EnumerationStringField-autotest-" + randomNumber;
        stringFieldOriginalName = "City-autotest-" + randomNumber;
        stringFieldRenamedName = "Country-autotest-" + randomNumber;
        stringFieldOptionLabel = "Slovakia-autotest-" + randomNumber;
        stringFieldOptionValue = "sk-autotest-" + randomNumber;
        stringFieldOptionLabel2 = "Czechia-autotest-" + randomNumber;
        stringFieldOptionValue2 = "cz-autotest-" + randomNumber;
        stringFieldTooltip = "Enumeration-tooltip-autotest-" + randomNumber;
    }
});

Scenario('Enumeration type tree CRUD', async ({I, DT, DTE}) => {
    I.amOnPage("/apps/enumeration/admin/");
    const name = "autotest-enumeration-tree-" + randomNumber;
    createEnumType(I, DTE, name, "Value", "Number", "Enabled");
    I.waitForElement(typeNode(name) + ".jstree-clicked", 10);
    I.seeElement(".tree-col .btn-import-dialog");
    I.seeElement(".tree-col .btn-export-dialog");
    for (const [panel, table] of [[".tree-col", "enumerationTypeDataTable"], ["#enumerationDataDataTable_wrapper", "enumerationDataDataTable"]]) {
        I.clickCss(panel + " .btn-export-dialog");
        DTE.waitForModal("datatableExportModal");
        I.assertEqual(await I.executeScript(() => window.datatableExportModal.tableId), table);
        I.seeElement("#pills-export-advanced-tab");
        I.clickCss("#datatableExportModal [data-bs-dismiss=modal]");
        I.waitForInvisible("#datatableExportModal", 10);
        I.clickCss(panel + " .btn-import-dialog");
        DTE.waitForModal("datatableImportModal");
        I.assertEqual(await I.executeScript(() => window.datatableImportModal.tableId), table);
        I.clickCss("#datatableImportModal [data-bs-dismiss=modal]");
        I.waitForInvisible("#datatableImportModal", 10);
    }
    const typeId = await I.executeScript(() => $("#SomStromcek").jstree(true).get_selected(true)[0].a_attr["data-type-id"]);
    I.clickCss("#enumerationDataDataTable_wrapper .buttons-create");
    DTE.waitForEditor("enumerationDataDataTable");
    I.assertEqual(await I.executeScript(() => String(enumerationDataDataTable.EDITOR.field("typeId").val())), typeId);
    DTE.cancel("enumerationDataDataTable");

    openEnumType(I, DT, DTE, name);
    I.fillField("#DTE_Field_typeName", name + "-edited");
    DTE.save("enumerationTypeDataTable");
    I.waitForElement(typeNode(name + "-edited") + ".jstree-clicked", 10);
    I.clickCss(".tree-col .btn-duplicate");
    DTE.waitForEditor("enumerationTypeDataTable");
    I.fillField("#DTE_Field_typeName", name + "-copy");
    DTE.save("enumerationTypeDataTable");
    I.waitForElement(typeNode(name + "-copy") + ".jstree-clicked", 10);
    deleteEnumType(I, DTE, name + "-copy");
    deleteEnumType(I, DTE, name + "-edited");
});

Scenario('Deleted enumeration type visibility and recovery', async ({I, DT, DTE}) => {
    I.amOnPage("/apps/enumeration/admin/");
    DT.waitForLoader();
    const name = "autotest-enumeration-recovery-" + randomNumber;

    I.clickCss(".tree-col .buttons-jstree-settings");
    DTE.waitForModal("jstreeSettingsModal");
    I.uncheckOption("#jstree-settings-showhidden");
    I.clickCss("#jstree-settings-submit");
    DTE.waitForModalClose("jstreeSettingsModal");
    DT.waitForLoader("enumerationTypeDataTable");

    createEnumType(I, DTE, name, "Value", "Number", "Enabled");
    I.waitForElement(typeNode(name) + ".jstree-clicked", 10);
    const typeId = await I.grabAttributeFrom(typeNode(name), "data-type-id");
    deleteEnumType(I, DTE, name);
    I.dontSeeElement(typeNode(name));

    I.clickCss(".tree-col .buttons-jstree-settings");
    DTE.waitForModal("jstreeSettingsModal");
    I.checkOption("#jstree-settings-showhidden");
    I.clickCss("#jstree-settings-submit");
    DTE.waitForModalClose("jstreeSettingsModal");
    I.waitForElement(typeNode(name) + '[data-hidden="true"]', 10);
    filterEnumDataByType(I, DTE, name);
    I.seeElement(".tree-col .buttons-remove:disabled");
    I.seeElement(".tree-col .btn-duplicate:disabled");

    openEnumType(I, DT, DTE, name);
    I.seeCheckboxIsChecked("#DTE_Field_hidden_0");
    I.uncheckOption("#DTE_Field_hidden_0");
    DTE.save("enumerationTypeDataTable");
    I.waitForElement(typeNode(name) + '[data-hidden="false"].jstree-clicked', 10);

    I.clickCss(".tree-col .buttons-jstree-settings");
    DTE.waitForModal("jstreeSettingsModal");
    I.uncheckOption("#jstree-settings-showhidden");
    I.clickCss("#jstree-settings-submit");
    DTE.waitForModalClose("jstreeSettingsModal");
    I.amOnPage("/apps/enumeration/admin/#" + typeId);
    I.waitForElement(typeNode(name) + '[data-hidden="false"].jstree-clicked', 10);
    I.waitForEnabled(".tree-col .buttons-remove", 10);
    I.waitForEnabled(".tree-col .btn-duplicate", 10);
    openEnumType(I, DT, DTE, name);
    I.dontSeeCheckboxIsChecked("#DTE_Field_hidden_0");
    DTE.cancel("enumerationTypeDataTable");

    deleteEnumType(I, DTE, name);
});

Scenario('Okresne mesta zakladne testy @baseTest', async ({I, DT, DataTables}) => {
    I.amOnPage("/apps/enumeration/admin/");
    DT.waitForLoader();

    var fieldA = "fieldA_autotest_"+randomNumber;

    await DataTables.baseTest({
        dataTable: 'enumerationDataDataTable',
        container: '#enumerationDataDataTable_wrapper',
        perms: 'cmp_enumerations',
        testingData: {
            "fieldA": fieldA
        },
        createSteps: function(I, options) {
            I.waitForVisible("#DTE_Field_fieldA", 10);
            I.fillField("#DTE_Field_fieldA", fieldA);
        },
        afterCreateSteps: function(I, options, requiredFields) {
            requiredFields.push("fieldA");
            options.testingData[0] = fieldA;
        },
        editSteps: function(I, options) {

        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
        skipSwitchDomain: true,
    });
});

Scenario('logout', ({I}) => {
    I.logout();
});

Scenario('Enumeration entry paging', async ({I, DT}) => {
    //data
    I.amOnPage("/apps/enumeration/admin/#2");
    I.see("5", "#enumerationDataDataTable_wrapper .dt-footer-row ul.pagination li button");
    I.see("Bánovce nad Bebravou", "#enumerationDataDataTable tbody tr td");
    I.dontSee("Poprad", "#enumerationDataDataTable tbody tr td");

    I.click("5", "#enumerationDataDataTable_wrapper ul.pagination");
    DT.waitForLoader();
    I.dontSee("Bánovce nad Bebravou", "#enumerationDataDataTable tbody tr td");
    I.assertEqual(await I.executeScript(() => enumerationDataDataTable.page.info().page), 4);
    I.click("1", "#enumerationDataDataTable_wrapper ul.pagination");
    DT.waitForLoader("enumerationDataDataTable");
    I.see("Bánovce nad Bebravou", "#enumerationDataDataTable tbody");
});

Scenario('Enum type and data tests', async ({I, DTE, DT}) => {
    I.amOnPage("/apps/enumeration/admin/");
    enumTypeNameA = "EnumerationAutoTestA_" + randomNumber;
    enumTypeNameB = "EnumerationAutoTestB_" + randomNumber;

    I.say("*** Phase 1 ***");

    I.say("Create an check enum type A");
    createEnumType(I, DTE, enumTypeNameA, stringName+"A", numberName+"A", booleanName+"A");
    checkEnumType(I, DT, enumTypeNameA, true);

    I.say("Create an check enum type B");
    createEnumType(I, DTE, enumTypeNameB, stringName+"B", numberName+"B", booleanName+"B");
    checkEnumType(I, DT, enumTypeNameB, true);

    I.say("First check that we dont see ChildEnumerationType and ParentEnumerationData");
    I.amOnPage("/apps/enumeration/admin/");
    filterEnumDataByType(I, DTE, enumTypeNameA);

    I.clickCss("#enumerationDataDataTable_wrapper button.buttons-create");
    DTE.waitForEditor('enumerationDataDataTable');
    I.dontSee("Prepojenie na číselník");
    I.dontSee("Rodič");

    I.say("*** Phase 2 ***");

    I.say("Do some checks");
    I.amOnPage("/apps/enumeration/admin/");

        I.say("Check - Toggle logic");
        openEnumType(I, DT, DTE, enumTypeNameA);
        checkToggleLogic(I);

        I.say("Check - loop child select error")
        selectEnumTypeLink(I, enumTypeNameB);
            //Toggle child enum for enumData
            I.clickCss("#DTE_Field_allowChildEnumerationType_0");
        DTE.save();

        const sharedParent = "EnumerationSharedParent-autotest-" + randomNumber;
        createEnumType(I, DTE, sharedParent, "Value", "Number", "Enabled");
        openEnumType(I, DT, DTE, sharedParent);
        selectEnumTypeLink(I, enumTypeNameB);
        DTE.save();
        filterEnumDataByType(I, DTE, enumTypeNameB);
        const child = await I.executeScript(({name, parentName}) => {
            const tree = $("#SomStromcek").jstree(true);
            const nodes = tree.get_json("#", {flat: true}).filter(node => node.a_attr.title === name);
            const selected = nodes.find(node => tree.get_node(node.parent).a_attr.title === parentName);
            return {count: nodes.length, id: selected.id, typeId: selected.a_attr["data-type-id"]};
        }, {name: enumTypeNameB, parentName: sharedParent});
        I.assertEqual(child.count, 2, "A shared child appears under both parents in search results");
        const childNode = '#SomStromcek a[id="' + child.id + '_anchor"]';
        I.clickCss(childNode);
        I.seeInCurrentUrl("#" + child.typeId);
        I.clickCss("#tree-folder-search-clear-button");
        I.waitForElement(childNode + ".jstree-clicked", 10);
        I.clickCss(".tree-col .buttons-refresh");
        I.waitForElement(childNode + ".jstree-clicked", 10);
        I.waitForEnabled(".tree-col .buttons-edit", 10);
        I.clickCss(".tree-col .buttons-edit");
        DTE.waitForEditor("enumerationTypeDataTable");
        I.seeInField("#DTE_Field_typeName", enumTypeNameB);
        DTE.cancel("enumerationTypeDataTable");
        I.clickCss("#enumerationDataDataTable_wrapper .buttons-create");
        DTE.waitForEditor("enumerationDataDataTable");
        I.assertEqual(await I.executeScript(() => String(enumerationDataDataTable.EDITOR.field("typeId").val())), child.typeId);
        DTE.cancel("enumerationDataDataTable");
        I.amOnPage("/apps/enumeration/admin/#" + child.typeId);
        I.waitForVisible(typeNode(enumTypeNameB) + ".jstree-clicked", 10);
        deleteEnumType(I, DTE, sharedParent);
        filterEnumDataByType(I, DTE, enumTypeNameB);
        I.assertEqual(await I.executeScript(() => {
            const tree = $("#SomStromcek").jstree(true);
            return tree.get_node(tree.get_selected(true)[0].parent).a_attr.title;
        }), enumTypeNameA, "Deleting one parent keeps the child under its remaining parent");

        openEnumType(I, DT, DTE, enumTypeNameB);
        selectEnumTypeLink(I, enumTypeNameA);
        DTE.save();
        I.see("Zvolené prepojenie na číselník " + enumTypeNameA + " nie je možné, pretože tento číselník je už prepojený na aktuálny číselník.")
        DTE.cancel();

            //Toggle parent option for enumData
            openEnumType(I, DT, DTE, enumTypeNameB);
            I.clickCss("#DTE_Field_allowParentEnumerationData_0");
            DTE.save();

    I.say("*** Phase 3 - NOW DO ENUM DATA TESTS ***");

    I.amOnPage("/apps/enumeration/admin/");
    filterEnumDataByType(I, DTE, enumTypeNameA);

        createEnumData(I, DTE, "A", enumTypeNameB, null, null);

        I.say("CHECK - Must see column names");
        I.see(stringName+"A");
        I.see(numberName+"A");
        I.see(booleanName+"A");

        //CHECK - must see values
        I.see(stringTestValue);
        I.see(numberTestValue + "");
        //Checkbox does not working for now

        //Check child enum type was saved
        I.clickCss("#enumerationDataDataTable td.dt-select-td.sorting_1");
        I.clickCss("#enumerationDataDataTable_wrapper button.buttons-edit");
        DTE.waitForEditor('enumerationDataDataTable');
        I.seeInField("#DTE_Field_editorFields-childEnumTypeName", enumTypeNameB);
        DTE.cancel();

        I.say("Phase 4");

        /* NOW test parent select */
        //For this we must create at least 2 enum data under B enum type
        I.amOnPage("/apps/enumeration/admin/");
        filterEnumDataByType(I, DTE, enumTypeNameB);

        createEnumData(I, DTE, "B", null, null, "1");

        createEnumData(I, DTE, "B", null, stringTestValue+"1", "2");

        I.say("CHECK - must see values");
        I.see(stringTestValue + "1");
        I.see(stringTestValue + "2");
        I.see(numberTestValue + "");
        //Checkbox does not working for now

        //Delete parent enumeration data
        I.say("Delete parent enumeration data");
        I.clickCss("#enumerationDataDataTable td.dt-select-td.sorting_1");
        I.clickCss("#enumerationDataDataTable_wrapper button.buttons-remove");
        DTE.waitForEditor('enumerationDataDataTable');
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.waitForLoader('enumerationDataDataTable');
        I.see(stringTestValue+"2");

        //Check deleted parent
        I.say("Check deleted parent");
        I.clickCss("#enumerationDataDataTable td.dt-select-td.sorting_1");
        I.clickCss("#enumerationDataDataTable_wrapper button.buttons-edit");
        DTE.waitForEditor('enumerationDataDataTable');
        I.seeInField("#DTE_Field_editorFields-parentEnumDataName", "(!deleted)_" + stringTestValue + "1");
        DTE.cancel();

    I.say("Phase 5");

    I.amOnPage("/apps/enumeration/admin/");

    deleteEnumType(I, DTE, enumTypeNameB);
    openEnumType(I, DT, DTE, enumTypeNameA);
    I.see("(!deleted)_" + enumTypeNameB, ".DTE_Field_Name_editorFields\\.childEnumTypeId");
    I.seeElementInDOM('#DTE_Field_editorFields-childEnumTypeId option[value="' + child.typeId + '"][disabled]');
    DTE.save("enumerationTypeDataTable");
    openEnumType(I, DT, DTE, enumTypeNameA);
    I.assertEqual(await I.executeScript(() => String(enumerationTypeDataTable.EDITOR.field("editorFields.childEnumTypeId").val())), child.typeId,
        "Saving the surviving parent preserves its link to the deleted child");
    DTE.cancel("enumerationTypeDataTable");
    I.assertEqual(await I.executeScript(async typeId => {
        const response = await fetch("/admin/rest/enumeration/enumeration-data/all?enumerationTypeId=" + typeId, {headers: {"X-CSRF-Token": window.csrfToken}});
        return (await response.json()).content.length;
    }, child.typeId), 0, "Deleting a type also hides its data records");

    const survivingChild = "EnumerationSurvivor-autotest-" + randomNumber;
    createEnumType(I, DTE, survivingChild, "Value", "Number", "Enabled");
    openEnumType(I, DT, DTE, enumTypeNameA);
    selectEnumTypeLink(I, survivingChild);
    DTE.save("enumerationTypeDataTable");
    deleteEnumType(I, DTE, enumTypeNameA);
    filterEnumDataByType(I, DTE, survivingChild);
    I.assertEqual(await I.executeScript(() => $("#SomStromcek").jstree(true).get_selected(true)[0].parent), "#",
        "Deleting the last parent moves its surviving child to the root");
    deleteEnumType(I, DTE, survivingChild);
});

Scenario('Enumeration string field type setup', async ({I, DTE, DT}) => {
    I.amOnPage("/apps/enumeration/admin/");

    I.clickCss(".tree-col button.buttons-create");
    DTE.waitForEditor('enumerationTypeDataTable');
    I.dontSeeElement("#pills-dt-enumerationTypeDataTable-stringFieldTypes-tab");
    I.fillField("#DTE_Field_typeName", stringFieldTypeEnumName);
    I.clickCss("#pills-dt-enumerationTypeDataTable-strings-tab");
    I.fillField("#DTE_Field_string1Name", stringFieldOriginalName);
    DTE.save("enumerationTypeDataTable");

    openEnumType(I, DT, DTE, stringFieldTypeEnumName);
    I.clickCss("#pills-dt-enumerationTypeDataTable-stringFieldTypes-tab");
    I.waitForVisible(stringFieldsWrapper, 10);
    DT.waitForLoader(stringFieldsTableId);
    I.see("Nenašli sa žiadne vyhovujúce záznamy", stringFieldsWrapper);

    I.clickCss(stringFieldsWrapper + " button.buttons-create");
    DTE.waitForEditor(stringFieldsTableId);
    const alphabetOptions = await I.executeScript((selector) => {
        return Array.from(document.querySelectorAll(selector)).map(option => option.value);
    }, stringFieldsModal + " #DTE_Field_alphabet option");
    I.assertTrue(alphabetOptions.includes("A"), "Named string field must be available for configuration");
    I.assertFalse(alphabetOptions.includes("B"), "Unnamed string field must not be available for configuration");
    DTE.selectOption("alphabet", "Reťazec 1 – " + stringFieldOriginalName);
    DTE.selectOption("type", "Výberové pole");
    I.waitForVisible(stringFieldsModal + " div.DTE_Field_Name_optionsSource", 10);
    I.waitForVisible(stringFieldsModal + " div.DTE_Field_Name_selectOptions", 10);
    I.dontSeeElement(stringFieldsModal + " div.DTE_Field_Name_enumeration");

    I.checkOption(stringFieldsModal + " .DTE_Field_Name_optionsSource input[value='enumeration']");
    I.waitForVisible(stringFieldsModal + " div.DTE_Field_Name_enumeration", 10);
    I.waitForInvisible(stringFieldsModal + " div.DTE_Field_Name_selectOptions", 10);

    I.checkOption(stringFieldsModal + " .DTE_Field_Name_optionsSource input[value='static']");
    I.waitForVisible(stringFieldsModal + " div.DTE_Field_Name_selectOptions", 10);
    I.waitForInvisible(stringFieldsModal + " div.DTE_Field_Name_enumeration", 10);
    I.checkOption(stringFieldsModal + " #DTE_Field_required_0");
    I.fillField(stringFieldsModal + " #DTE_Field_tooltip", stringFieldTooltip);
    fillEnumerationStringFieldOptions(I, [
        { label: stringFieldOptionLabel, value: stringFieldOptionValue },
        { label: stringFieldOptionLabel2, value: stringFieldOptionValue2 }
    ]);
    DTE.save(stringFieldsTableId);

    DT.checkTableRow(stringFieldsTableId, 1, [
        "Reťazec 1 – " + stringFieldOriginalName,
        "Výberové pole",
        stringFieldOriginalName,
        stringFieldTooltip
    ]);

    I.clickCss("#" + stringFieldsTableId + " tbody tr:first-child td:first-child");
    I.clickCss(stringFieldsWrapper + " button.buttons-edit");
    DTE.waitForEditor(stringFieldsTableId);
    I.waitForText("Upraviť: Reťazec 1 – " + stringFieldOriginalName, 10, stringFieldsModal + " div.DTE_Header");
    DTE.cancel(stringFieldsTableId, true);
    DTE.cancel("enumerationTypeDataTable", true);
});

Scenario('Enumeration configured string field behavior', ({I, DTE, DT}) => {
    I.amOnPage("/apps/enumeration/admin/");
    filterEnumDataByType(I, DTE, stringFieldTypeEnumName);

    I.clickCss("#enumerationDataDataTable_wrapper button.buttons-create");
    DTE.waitForEditor('enumerationDataDataTable');
    I.seeElement("#DTE_Field_fieldA");
    I.dontSeeElement("#DTE_Field_fieldB");
    I.seeElement(locate("label[for='DTE_Field_fieldA']").withText(stringFieldOriginalName));
    I.seeElementInDOM("select#DTE_Field_fieldA");

    DTE.save("enumerationDataDataTable");
    I.see("Voliteľné pole je nastavené ako povinné.", "div.DTE_Field_Name_fieldA");
    DTE.selectOption("fieldA", stringFieldOptionLabel2);
    DTE.save("enumerationDataDataTable");
    I.waitForText(stringFieldOptionLabel2, 10, "#enumerationDataDataTable tbody");
});

Scenario('Enumeration string field name synchronization', async ({I, DTE, DT}) => {
    I.amOnPage("/apps/enumeration/admin/");
    openEnumType(I, DT, DTE, stringFieldTypeEnumName);

    I.clickCss("#pills-dt-enumerationTypeDataTable-strings-tab");
    I.fillField("#DTE_Field_string1Name", stringFieldRenamedName);
    I.clickCss("#pills-dt-enumerationTypeDataTable-stringFieldTypes-tab");
    I.waitForVisible(stringFieldsWrapper, 10);
    DT.waitForLoader(stringFieldsTableId);

    DT.checkTableRow(stringFieldsTableId, 1, [
        "Reťazec 1 – " + stringFieldOriginalName,
        "Výberové pole",
        stringFieldOriginalName,
        stringFieldTooltip
    ]);
    I.dontSee(stringFieldRenamedName, stringFieldsWrapper);

    const requiredColumnVisible = await I.executeScript((wrapperSelector) => {
        const header = document.querySelector(wrapperSelector + " th.dt-th-required");
        return header != null && window.getComputedStyle(header).display !== "none";
    }, stringFieldsWrapper);
    I.assertFalse(requiredColumnVisible, "Required column must stay hidden in the nested table");

    I.clickCss("#" + stringFieldsTableId + " tbody tr:first-child td:first-child");
    I.clickCss(stringFieldsWrapper + " button.buttons-edit");
    DTE.waitForEditor(stringFieldsTableId);
    I.waitForText("Upraviť: Reťazec 1 – " + stringFieldOriginalName, 10, stringFieldsModal + " div.DTE_Header");
    DTE.cancel(stringFieldsTableId, true);
    DTE.save("enumerationTypeDataTable");

    openEnumType(I, DT, DTE, stringFieldTypeEnumName);
    I.clickCss("#pills-dt-enumerationTypeDataTable-stringFieldTypes-tab");
    I.waitForVisible(stringFieldsWrapper, 10);
    DT.waitForLoader(stringFieldsTableId);
    DT.checkTableRow(stringFieldsTableId, 1, [
        "Reťazec 1 – " + stringFieldRenamedName,
        "Výberové pole",
        stringFieldRenamedName,
        stringFieldTooltip
    ]);
    I.clickCss("#" + stringFieldsTableId + " tbody tr:first-child td:first-child");
    I.clickCss(stringFieldsWrapper + " button.buttons-edit");
    DTE.waitForEditor(stringFieldsTableId);
    I.waitForText("Upraviť: Reťazec 1 – " + stringFieldRenamedName, 10, stringFieldsModal + " div.DTE_Header");
    DTE.cancel(stringFieldsTableId, true);
    DTE.cancel("enumerationTypeDataTable", true);

    I.amOnPage("/apps/enumeration/admin/");
    filterEnumDataByType(I, DTE, stringFieldTypeEnumName);
    I.see(stringFieldRenamedName, "#enumerationDataDataTable_wrapper thead");
    I.clickCss("#enumerationDataDataTable tbody tr:first-child td.dt-select-td");
    I.clickCss("#enumerationDataDataTable_wrapper button.buttons-edit");
    DTE.waitForEditor("enumerationDataDataTable");
    I.seeElement(locate("label[for='DTE_Field_fieldA']").withText(stringFieldRenamedName));
    I.seeInField("#DTE_Field_fieldA", stringFieldOptionValue2);
    DTE.cancel("enumerationDataDataTable", true);
});

Scenario('Enumeration string field type cleanup', async ({I, DTE, DT}) => {
    I.amOnPage("/apps/enumeration/admin/");
    DT.waitForLoader("enumerationTypeDataTable");
    openEnumType(I, DT, DTE, stringFieldTypeEnumName);
    I.clickCss("#pills-dt-enumerationTypeDataTable-stringFieldTypes-tab");
    I.waitForVisible(stringFieldsWrapper, 10);
    DT.waitForLoader(stringFieldsTableId);
    I.clickCss("#" + stringFieldsTableId + " tbody tr:first-child td:first-child");
    I.clickCss(stringFieldsWrapper + " button.buttons-remove");
    I.waitForVisible(stringFieldsModal + " div.DTE_Action_Remove", 10);
    I.click("Zmazať", stringFieldsModal + " div.DTE_Action_Remove");
    I.waitForInvisible("div.DTE_Processing_Indicator", 200);
    I.waitForInvisible(stringFieldsModal, 30);
    DT.waitForLoader(stringFieldsTableId);
    I.see("Nenašli sa žiadne vyhovujúce záznamy", stringFieldsWrapper);
    DTE.cancel("enumerationTypeDataTable", true);

    deleteEnumType(I, DTE, stringFieldTypeEnumName);
});

Scenario('Test special import logic', ({I, DTE, DT}) => {
    /**
     * During import when we choose option UPDATE, we can have data from another enum type.
     * This result in UPDATE of data so original enum type will lost data.
     *
     * SPECIAL LOGIC -> during import UPDATE action CREATE data's that ID's do not belong to DEST enum type.
     */

    const source_enum = "TestExportSrc";
    const dest_enum = "TestImportDest";
    const excel_file = "tests/apps/enumeration-special-insert-data.xlsx";

    I.say('Check that SOURCE enum type has data');
    I.amOnPage("/apps/enumeration/admin/#3075");
    DT.checkTableRow("enumerationDataDataTable", 1, ["44220", "1", "One"]);
    DT.checkTableRow("enumerationDataDataTable", 2, ["44221", "2", "Two"]);
    DT.checkTableRow("enumerationDataDataTable", 3, ["44222", "3", "Three"]);

    I.say('Check that DEST enum type has NO data');
    filterEnumDataByType(I, DTE, dest_enum);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Import data from SOURCE to DEST enum type as UPDATE");
    I.click("#enumerationDataDataTable_wrapper button.btn-import-dialog");
    DTE.waitForModal("datatableImportModal");
    I.checkOption("#dt-settings-import3");

    I.attachFile('#insert-file', excel_file);
    I.waitForEnabled("#submit-import", 5);
    I.clickCss("#submit-import");

    DT.waitForLoader();

    I.say("Wait for data na dod a check");
    I.waitForText("One", 15);
    DT.checkTableRow("enumerationDataDataTable", 1, ["", "1", "One"]);
    DT.checkTableRow("enumerationDataDataTable", 2, ["", "2", "Two"]);
    DT.checkTableRow("enumerationDataDataTable", 3, ["", "3", "Three"]);

    I.say("Now return to SOURCE enum type and check that data ARE STILL THERE");
    filterEnumDataByType(I, DTE, source_enum);
    DT.checkTableRow("enumerationDataDataTable", 1, ["44220", "1", "One"]);
    DT.checkTableRow("enumerationDataDataTable", 2, ["44221", "2", "Two"]);
    DT.checkTableRow("enumerationDataDataTable", 3, ["44222", "3", "Three"]);
});

Scenario('Delete enum data', async ({I}) => {
    I.amOnPage("/apps/enumeration/admin/#3076");

    let rows = await I.executeScript(() => enumerationDataDataTable.page.info().recordsDisplay);
    if(rows > 0) {
        I.clickCss("#enumerationDataDataTable_wrapper button.dt-filter-id");
        I.clickCss("#enumerationDataDataTable_wrapper button.buttons-remove");
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
});

function createEnumData(I, DTE, variant, childEnumType, parentEnumData, bonusStrChar) {
    I.clickCss("#enumerationDataDataTable_wrapper button.buttons-create");
    DTE.waitForEditor('enumerationDataDataTable');

    //Must see fields
    I.see(stringName + variant.toUpperCase());
    I.see(numberName + variant.toUpperCase());
    I.see(booleanName + variant.toUpperCase());

    //Set enum data value
    I.clickCss("#DTE_Field_fieldA");
    I.fillField("#DTE_Field_fieldA", stringTestValue + bonusStrChar);

    I.clickCss("#DTE_Field_decimal1");
    I.fillField("#DTE_Field_decimal1", numberTestValue);

    I.checkOption("#DTE_Field_boolean1");

    if(childEnumType !== null && childEnumType !== "") {
        //Select enum type child
        I.see("Prepojenie na číselník");
        I.clickCss("#DTE_Field_editorFields-childEnumTypeName");
        I.waitForElement("ul.ui-menu");
        I.fillField("#DTE_Field_editorFields-childEnumTypeName", childEnumType);
        I.click( locate("ul.ui-menu").find( locate("li.ui-menu-item > div").withText(childEnumType) ) );
    } else if(parentEnumData !== null && parentEnumData !== "") {
        //Select enum data parent
        I.see("Rodič");
        I.clickCss("#DTE_Field_editorFields-parentEnumDataName");
        I.waitForElement("ul.ui-menu");
        I.fillField("#DTE_Field_editorFields-parentEnumDataName", parentEnumData);
        I.click( locate("ul.ui-menu").find( locate("li.ui-menu-item > div").withText(parentEnumData) ) );
    }

    DTE.save();
    DTE.waitForLoader('enumerationDataDataTable');
}

function selectEnumTypeLink(I, value) {
    I.click( locate( "div.DTE_Field_Name_editorFields\\.childEnumTypeId > div > div > div.dropdown > button.dropdown-toggle") ); //NOSONAR
    I.waitForVisible("div.dropdown-menu.show");
    I.fillField(locate("div.dropdown-menu.show").find("input"), value);
    I.click( locate("div.dropdown-menu").find( locate("a.dropdown-item").withText(value) ) );
}

function createEnumType(I, DTE, typeName, stringName, numberName, booleanName) {
    I.clickCss(".tree-col button.buttons-create");
    DTE.waitForEditor('enumerationTypeDataTable');
    DTE.save();

    //See error
    I.see("Povinné pole. Zadajte aspoň jeden znak.");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    I.fillField('#DTE_Field_typeName', typeName);

    I.clickCss("#pills-dt-enumerationTypeDataTable-strings-tab");
    I.fillField('#DTE_Field_string1Name', stringName);

    I.clickCss("#pills-dt-enumerationTypeDataTable-numbers-tab");
    I.fillField("#DTE_Field_decimal1Name", numberName);

    I.clickCss("#pills-dt-enumerationTypeDataTable-booleans-tab");
    I.fillField('#DTE_Field_boolean1Name', booleanName);

    DTE.save();
}

function checkEnumType(I, DT, typeName, shouldSee) {
    if (shouldSee) I.waitForElement(typeNode(typeName), 10);
    else I.dontSeeElement(typeNode(typeName));
}

function openEnumType(I, DT, DTE, typeName) {
    filterEnumDataByType(I, DTE, typeName);
    I.waitForEnabled(".tree-col .buttons-edit", 10);
    I.clickCss(".tree-col .buttons-edit");
    DTE.waitForEditor('enumerationTypeDataTable');
}

//Check only 1 of them can be true at time
function checkToggleLogic(I) {
    I.dontSeeCheckboxIsChecked("#DTE_Field_allowChildEnumerationType_0");
    I.dontSeeCheckboxIsChecked("#DTE_Field_allowParentEnumerationData_0");

    I.checkOption("#DTE_Field_allowChildEnumerationType_0");

    I.seeCheckboxIsChecked("#DTE_Field_allowChildEnumerationType_0");
    I.dontSeeCheckboxIsChecked("#DTE_Field_allowParentEnumerationData_0");

    I.checkOption("#DTE_Field_allowParentEnumerationData_0");

    I.dontSeeCheckboxIsChecked("#DTE_Field_allowChildEnumerationType_0");
    I.seeCheckboxIsChecked("#DTE_Field_allowParentEnumerationData_0");

    I.uncheckOption("#DTE_Field_allowParentEnumerationData_0");

    I.dontSeeCheckboxIsChecked("#DTE_Field_allowChildEnumerationType_0");
    I.dontSeeCheckboxIsChecked("#DTE_Field_allowParentEnumerationData_0");
}

function typeNode(name) {
    return '#SomStromcek a[title="' + name + '"]';
}

function filterEnumDataByType(I, DTE, typeName) {
    I.waitForVisible("#tree-folder-search-input", 10);
    I.fillField("#tree-folder-search-input", typeName);
    I.clickCss("#tree-folder-search-button");
    I.waitForVisible(typeNode(typeName), 10);
    I.clickCss(typeNode(typeName));
    I.waitForElement(typeNode(typeName) + ".jstree-clicked", 10);
    DTE.waitForLoader("enumerationDataDataTable");
}

function deleteEnumType(I, DTE, name) {
    filterEnumDataByType(I, DTE, name);
    I.waitForEnabled(".tree-col .buttons-remove", 10);
    I.clickCss(".tree-col .buttons-remove");
    DTE.waitForEditor("enumerationTypeDataTable");
    I.click("Zmazať", "#enumerationTypeDataTable_modal div.DTE_Action_Remove");
    I.waitForInvisible("#enumerationTypeDataTable_modal", 10);
    I.waitForInvisible(typeNode(name), 10);
}

function fillEnumerationStringFieldOptions(I, options) {
    I.waitForVisible(stringFieldsModal + " div.DTE_Field_Name_selectOptions", 10);

    options.forEach((option, index) => {
        if(index > 0) {
            I.clickCss(stringFieldsModal + " div.DTE_Field_Name_selectOptions button.options-add-btn");
        }

        const rowSelector = stringFieldsModal + " div.DTE_Field_Name_selectOptions .options-inputs .options-input-row:nth-child(" + (index + 1) + ")";
        I.fillField(rowSelector + " input.options-value-1", option.label);
        I.fillField(rowSelector + " input.options-value-2", option.value);
    });
}
