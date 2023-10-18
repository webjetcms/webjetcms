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

Scenario('Enum type zakladne testy', async ({I, DataTables}) => {
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
    });
});

Scenario('Okresne mesta zakladne testy', async ({I, DT, DataTables}) => {
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
    });
});

Scenario('Enum type and data tests', async ({I, DTE, DT}) => {
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
    enumTypeNameA = "EnumerationAutoTestA_" + randomNumber;
    enumTypeNameB = "EnumerationAutoTestB_" + randomNumber;

    I.say("Phase 1");

    //Create an check enum type A
    createEnumType(I, DTE, enumTypeNameA, stringName+"A", numberName+"A", booleanName+"A");
    checkEnumType(I, DTE, enumTypeNameA, true);

    //Create an check enum type B
    createEnumType(I, DTE, enumTypeNameB, stringName+"B", numberName+"B", booleanName+"B");
    checkEnumType(I, DTE, enumTypeNameB, true);

    //First check that we dont see ChildEnumerationType and ParentEnumerationData
    I.amOnPage("/apps/enumeration/admin/");
    filterEnumDataByType(I, DTE, enumTypeNameA);

    I.click("button.buttons-create");
    DTE.waitForEditor('enumerationDataDataTable');
    I.dontSee("Prepojenie na číselník");
    I.dontSee("Rodič");

    I.say("Phase 2");

    //Do some checks
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");

        //Toggle logic
        openEnumType(I, DTE, enumTypeNameA);
        checkToggleLogic(I);

        //Check loop child select error
        selectChildEnumType(I, enumTypeNameB);
            //Toggle child enum for enumData
            I.click("#DTE_Field_allowChildEnumerationType_0");
        DTE.save();

        openEnumType(I, DTE, enumTypeNameB);
        selectChildEnumType(I, enumTypeNameA);
        DTE.save();
        I.see("Zvolené prepojenie na číselník " + enumTypeNameA + " nie je možné, pretože tento číselník je už prepojený na aktuálny číselník.")
        DTE.cancel();

            //Toggle parent option for enumData
            I.click(enumTypeNameB);
            DTE.waitForEditor('enumerationTypeDataTable');
            I.click("#DTE_Field_allowParentEnumerationData_0");
            DTE.save();

    I.say("Phase 3");

    //NOW DO ENUM DATA TESTS
    I.amOnPage("/apps/enumeration/admin/");
    filterEnumDataByType(I, DTE, enumTypeNameA);

        createEnumData(I, DTE, "A", enumTypeNameB, null, null);

        //CHECK - Must see column names
        I.see(stringName+"A");
        I.see(numberName+"A");
        I.see(booleanName+"A");

        //CHECK - must see values
        I.see(stringTestValue);
        I.see(numberTestValue + "");
        //Checkbox does not working for now

        //Check child enum type was saved
        I.click("td.dt-select-td.sorting_1");
        I.click("button.buttons-edit");
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

        //CHECK - must see values
        I.see(stringTestValue + "1");
        I.see(stringTestValue + "2");
        I.see(numberTestValue + "");
        //Checkbox does not working for now

        //Delete parent enumeration data
        I.click("td.dt-select-td.sorting_1");
        I.click("button.buttons-remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        DT.waitForLoader('enumerationDataDataTable');
        I.see(stringTestValue+"2");

        //Check deleted parent
        I.click("td.dt-select-td.sorting_1");
        I.click("button.buttons-edit");
        DTE.waitForEditor('enumerationDataDataTable');
        I.see("(!deleted)_" + stringTestValue + "1");
        DTE.cancel();

    I.say("Phase 5");

    I.amOnPage("/apps/enumeration/admin/enumeration-type/");

    //Filter enumTypeNameBS
    I.fillField("input.dt-filter-typeName", enumTypeNameB);
    I.pressKey('Enter', "input.dt-filter-typeName");
    DTE.waitForLoader();

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //Filter enumTypeNameA
    I.fillField("input.dt-filter-typeName", enumTypeNameA);
    I.pressKey('Enter', "input.dt-filter-typeName");
    DTE.waitForLoader();

    //Delete enumTypeNameA
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

function createEnumData(I, DTE, variant, childEnumType, parentEnumData, bonusStrChar) {
    I.click("button.buttons-create");
    DTE.waitForEditor('enumerationDataDataTable');

    //Must see fields
    I.see(stringName + variant.toUpperCase());
    I.see(numberName + variant.toUpperCase());
    I.see(booleanName + variant.toUpperCase());

    //Set enum data value
    I.click("#DTE_Field_string1");
    I.fillField("#DTE_Field_string1", stringTestValue + bonusStrChar);

    I.click("#DTE_Field_decimal1");
    I.fillField("#DTE_Field_decimal1", numberTestValue);

    I.checkOption("#DTE_Field_boolean1");

    if(childEnumType !== null && childEnumType !== "") {
        //Select enum type child
        I.see("Prepojenie na číselník");
        I.click('//*[@id="enumerationDataDataTable_modal"]/div/div/div[3]/div/form/div/div[27]/div[1]/div[1]/div/button');
        I.fillField('body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input', childEnumType);
        I.click(childEnumType);
    } else if(parentEnumData !== null && parentEnumData !== "") {
        //Select enum data parent
        I.see("Rodič");
        I.click('//*[@id="enumerationDataDataTable_modal"]/div/div/div[3]/div/form/div/div[28]/div[1]/div[1]/div/button');
        I.fillField('body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input', parentEnumData);
        I.click(parentEnumData);
    }

    DTE.save();
    DTE.waitForLoader('enumerationDataDataTable');
}

function selectChildEnumType(I, childName) {
    I.click('//*[@id="panel-body-dt-enumerationTypeDataTable-basic"]/div[3]/div[1]/div[1]/div/button');
    I.click('body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input');
    I.fillField('body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input', childName);
    I.click(childName);
}

function createEnumType(I, DTE, typeName, stringName, numberName, booleanName) {
    I.click("button.buttons-create");
    DTE.waitForEditor('enumerationTypeDataTable');
    DTE.save();

    //See error
    I.see("Povinné pole. Zadajte aspoň jeden znak.");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).");

    I.fillField('#DTE_Field_typeName', typeName);
    I.click("#pills-dt-enumerationTypeDataTable-strings-tab");

    I.fillField('#DTE_Field_string1Name', stringName);
    I.click("#pills-dt-enumerationTypeDataTable-numbers-tab");

    I.fillField("#DTE_Field_decimal1Name", numberName);
    I.click("#pills-dt-enumerationTypeDataTable-booleans-tab");

    I.fillField('#DTE_Field_boolean1Name', booleanName);
    DTE.save();
}

function checkEnumType(I, DTE, typeName, shouldSee) {
    I.fillField("input.dt-filter-typeName", typeName);
    I.pressKey('Enter', "input.dt-filter-typeName");
    DTE.waitForLoader();

    if(shouldSee === true) I.see(typeName);
    else I.dontSee(typeName);
}

function openEnumType(I, DTE, typeName) {
    I.fillField("input.dt-filter-typeName", typeName);
    I.pressKey('Enter', "input.dt-filter-typeName");
    DTE.waitForLoader();
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
    I.click("#pills-enumerationType-tab");
    I.fillField('body > div.bs-container.dropdown.bootstrap-select > div > div.bs-searchbox > input', typeName);
    I.see(typeName);
    I.click(typeName);
    DTE.waitForLoader();
}