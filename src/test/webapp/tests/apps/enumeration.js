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

Scenario('Enum type zakladne testy @baseTest', async ({I, DataTables}) => {
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
    await DataTables.baseTest({
        dataTable: 'enumerationTypeDataTable',
        perms: 'cmp_enumerations',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
        skipSwitchDomain: true
    });
});

Scenario('Okresne mesta zakladne testy @baseTest', async ({I, DT, DataTables}) => {
    I.amOnPage("/apps/enumeration/admin/");
    DT.waitForLoader();

    var fieldA = "fieldA_autotest_"+randomNumber;

    await DataTables.baseTest({
        dataTable: 'enumerationDataDataTable',
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

Scenario('test datatables paging', ({I, DT}) => {
    //types
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
    DT.waitForLoader();
    I.see("2", ".dt-footer-row ul.pagination li button");
    I.see("Okresne Mestá", "#enumerationTypeDataTable tbody tr td");

    I.click({css: "ul.pagination li:nth-child(3) button"});
    DT.waitForLoader();
    I.dontSee("Okresne Mestá", "#enumerationTypeDataTable tbody tr td");

    //data
    I.amOnPage("/apps/enumeration/admin/#2");
    I.see("5", ".dt-footer-row ul.pagination li button");
    I.see("Bánovce nad Bebravou", "#enumerationDataDataTable tbody tr td");
    I.dontSee("Poprad", "#enumerationDataDataTable tbody tr td");

    I.click({css: "ul.pagination li:nth-child(6) button"});
    DT.waitForLoader();
    I.dontSee("Bánovce nad Bebravou", "#enumerationDataDataTable tbody tr td");
    I.see("Senec", "#enumerationDataDataTable tbody tr td");
    I.see("Senica", "#enumerationDataDataTable tbody tr td");
});

Scenario('Enum type and data tests', ({I, DTE, DT}) => {
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
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

    I.clickCss("button.buttons-create");
    DTE.waitForEditor('enumerationDataDataTable');
    I.dontSee("Prepojenie na číselník");
    I.dontSee("Rodič");

    I.say("*** Phase 2 ***");

    I.say("Do some checks");
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");

        I.say("Check - Toggle logic");
        openEnumType(I, DT, DTE, enumTypeNameA);
        checkToggleLogic(I);

        I.say("Check - loop child select error")
        selectEnumTypeLink(I, enumTypeNameB);
            //Toggle child enum for enumData
            I.clickCss("#DTE_Field_allowChildEnumerationType_0");
        DTE.save();

        openEnumType(I, DT, DTE, enumTypeNameB);
        selectEnumTypeLink(I, enumTypeNameA);
        DTE.save();
        I.see("Zvolené prepojenie na číselník " + enumTypeNameA + " nie je možné, pretože tento číselník je už prepojený na aktuálny číselník.")
        DTE.cancel();

            //Toggle parent option for enumData
            I.click(enumTypeNameB);
            DTE.waitForEditor('enumerationTypeDataTable');
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
        I.clickCss("td.dt-select-td.sorting_1");
        I.clickCss("button.buttons-edit");
        DTE.waitForEditor('enumerationDataDataTable');
        I.see(enumTypeNameB);
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
        I.clickCss("td.dt-select-td.sorting_1");
        I.clickCss("button.buttons-remove");
        DTE.waitForEditor('enumerationDataDataTable');
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.waitForLoader('enumerationDataDataTable');
        I.see(stringTestValue+"2");

        //Check deleted parent
        I.say("Check deleted parent");
        I.clickCss("td.dt-select-td.sorting_1");
        I.clickCss("button.buttons-edit");
        DTE.waitForEditor('enumerationDataDataTable');
        I.seeInField("#DTE_Field_editorFields-parentEnumDataName", "(!deleted)_" + stringTestValue + "1");
        DTE.cancel();

    I.say("Phase 5");

    I.amOnPage("/apps/enumeration/admin/enumeration-type/");

    //Filter enumTypeNameB
    DT.filterContains("typeName", enumTypeNameB);

    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //Filter enumTypeNameA
    DT.filterContains("typeName", enumTypeNameA);

    //Delete enumTypeNameA
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Enumeration string field type setup', async ({I, DTE, DT}) => {
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");

    I.clickCss("button.buttons-create");
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

    I.clickCss("button.buttons-create");
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
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
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
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
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

    DT.filterEquals("typeName", stringFieldTypeEnumName);
    await deleteAllIfPresent(I, DT, "enumerationTypeDataTable");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
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
    I.click("button.btn-import-dialog");
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

    let rows = await I.getTotalRows();
    if(rows > 0) {
        I.clickCss("button.dt-filter-id");
        I.clickCss("button.buttons-remove");
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    }
});

function createEnumData(I, DTE, variant, childEnumType, parentEnumData, bonusStrChar) {
    I.clickCss("button.buttons-create");
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
    I.clickCss("button.buttons-create");
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
    DT.filterContains("typeName", typeName);
    if(shouldSee === true) I.see(typeName);
    else I.dontSee(typeName);
}

function openEnumType(I, DT, DTE, typeName) {
    DT.filterContains("typeName", typeName);
    I.click(typeName);
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

function filterEnumDataByType(I, DTE, typeName) {
    I.clickCss(".buttons-select");
    I.fillField("body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input", typeName);
    I.clickCss("a[role=option] > span");
    DTE.waitForLoader();
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

async function deleteAllIfPresent(I, DT, tableId) {
    const rowCount = await I.executeScript((id) => {
        return window.$("#" + id).DataTable().page.info().recordsDisplay;
    }, tableId);

    if(rowCount > 0) {
        DT.deleteAll(tableId);
    }
}
