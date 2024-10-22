Feature('apps.enumeration');

var randomNumber;
var enumTypeNameA;
var enumTypeNameB;
var stringName = "testStringRow";
var numberName = "testNumberRow";
var booleanName = "testBooleanRow";
var stringTestValue = "testTestNow";
var numberTestValue = 369;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
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
    I.wait(1);

    var string1 = "string1_"+randomNumber;

    await DataTables.baseTest({
        dataTable: 'enumerationDataDataTable',
        perms: 'cmp_enumerations',
        testingData: {
            "string1": string1
        },
        createSteps: function(I, options) {
            I.fillField("#DTE_Field_string1", string1);
        },
        afterCreateSteps: function(I, options, requiredFields) {
            requiredFields.push("string1");
            options.testingData[0] = string1;
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

Scenario('logout', ({I}) => {
    I.logout();
});

Scenario('test datatables paging', ({I, DT}) => {
    //types
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
    DT.waitForLoader();
    I.see("2", ".dt-footer-row ul.pagination li a");
    I.see("Okresne Mestá", "#enumerationTypeDataTable tbody tr td");

    I.click({css: "ul.pagination li:nth-child(3) a"});
    DT.waitForLoader();
    I.dontSee("Okresne Mestá", "#enumerationTypeDataTable tbody tr td");

    //data
    I.amOnPage("/apps/enumeration/admin/#2");
    I.see("5", ".dt-footer-row ul.pagination li a");
    I.see("Bánovce nad Bebravou", "#enumerationDataDataTable tbody tr td");
    I.dontSee("Poprad", "#enumerationDataDataTable tbody tr td");

    I.click({css: "ul.pagination li:nth-child(6) a"});
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
    DT.filter("typeName", enumTypeNameB);

    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //Filter enumTypeNameA
    DT.filter("typeName", enumTypeNameA);

    //Delete enumTypeNameA
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
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
    I.clickCss("#DTE_Field_string1");
    I.fillField("#DTE_Field_string1", stringTestValue + bonusStrChar);

    I.clickCss("#DTE_Field_decimal1");
    I.fillField("#DTE_Field_decimal1", numberTestValue);

    I.checkOption("#DTE_Field_boolean1");

    if(childEnumType !== null && childEnumType !== "") {
        //Select enum type child
        I.see("Prepojenie na číselník");
        I.click("#DTE_Field_editorFields-childEnumTypeName");
        I.waitForElement("ul.ui-menu");
        I.fillField("#DTE_Field_editorFields-childEnumTypeName", childEnumType);
        I.click( locate("ul.ui-menu").find( locate("li.ui-menu-item > div").withText(childEnumType) ) );
    } else if(parentEnumData !== null && parentEnumData !== "") {
        //Select enum data parent
        I.see("Rodič");
        I.click("#DTE_Field_editorFields-parentEnumDataName");
        I.waitForElement("ul.ui-menu");
        I.fillField("#DTE_Field_editorFields-parentEnumDataName", parentEnumData);
        I.click( locate("ul.ui-menu").find( locate("li.ui-menu-item > div").withText(parentEnumData) ) );
    }

    DTE.save();
    DTE.waitForLoader('enumerationDataDataTable');
}

function selectEnumTypeLink(I, value) {
    I.click( locate( "div.DTE_Field_Name_editorFields\\.childEnumTypeId > div > div > div.dropdown > button.dropdown-toggle") );
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
    DT.filter("typeName", typeName);
    if(shouldSee === true) I.see(typeName);
    else I.dontSee(typeName);
}

function openEnumType(I, DT, DTE, typeName) {
    DT.filter("typeName", typeName);
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
    I.clickCss("#pills-enumerationType-tab");
    I.fillField('body > div.bs-container.dropdown.bootstrap-select > div > div.bs-searchbox > input', typeName);
    I.see(typeName);
    I.click(typeName);
    DTE.waitForLoader();
}