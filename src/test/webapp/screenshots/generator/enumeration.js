Feature('apps.enumeration');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

var randomNumber;
var enumTypeNameA;
var enumTypeNameB;
var string1 = "Nazov_Text_Stlpca_1";
var string2 = "Nazov_Text_Stlpca_2";

Scenario('Enum screenshots', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
    enumTypeNameA = "Test_A_" + randomNumber;
    enumTypeNameB = "Test_B_" + randomNumber;
    let confLng = I.getConfLng();

    //Data table
    Document.screenshot("/redactor/apps/enumeration/dataTable_enumType.png");

    I.click("button.buttons-create");
    DTE.waitForEditor('enumerationTypeDataTable');
    I.fillField('#DTE_Field_typeName', enumTypeNameA);

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/enumeration/editor_enumType.png");

    I.click("#pills-dt-enumerationTypeDataTable-strings-tab");
    I.fillField('#DTE_Field_string1Name', string1);
    I.fillField('#DTE_Field_string2Name', string2);

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/enumeration/editor_stringTab.png");

    DTE.save();

    I.click("button.buttons-create");
    DTE.waitForEditor('enumerationTypeDataTable');
    I.fillField('#DTE_Field_typeName', enumTypeNameB);
    DTE.save();

    openEnumType(I, DTE, enumTypeNameA);

    I.click('//*[@id="panel-body-dt-enumerationTypeDataTable-basic"]/div[3]/div[1]/div[1]/div/button');

    Document.screenshot("/redactor/apps/enumeration/editor_select_1.png")

    I.click('body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input');
    I.fillField('body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input', enumTypeNameB);
    I.click(enumTypeNameB);
    DTE.save();

    openEnumType(I, DTE, enumTypeNameB);

    I.click('//*[@id="panel-body-dt-enumerationTypeDataTable-basic"]/div[3]/div[1]/div[1]/div/button');
    I.click('body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input');
    I.fillField('body > div.bs-container.dropdown.bootstrap-select.form-select > div > div.bs-searchbox > input', enumTypeNameA);
    I.click(enumTypeNameA);
    DTE.save();

    Document.screenshot("/redactor/apps/enumeration/editor_select_2.png");

    DTE.cancel();

    //Delete enumTypeNameB
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    
    if("sk" === confLng) {
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    } else if("en" === confLng) { 
        I.click("Delete", "div.DTE_Action_Remove");
        I.see("No matching records found");
    }

    I.amOnPage("/apps/enumeration/admin/enumeration-type/");

    openEnumType(I, DTE, enumTypeNameA);

    Document.screenshot("/redactor/apps/enumeration/editor_select_3.png");

    I.amOnPage("/apps/enumeration/admin/");

    filterEnumDataByType(I, DTE, enumTypeNameA);

    I.click("button.buttons-create");
    DTE.waitForEditor('enumerationDataDataTable');

    Document.screenshotElement("div.DTE_Action_Create", "/redactor/apps/enumeration/editor_enumData.png");

    I.click("#DTE_Field_string1");
    I.fillField("#DTE_Field_string1", "Test Hodnota AA");

    I.click("#DTE_Field_string2");
    I.fillField("#DTE_Field_string2", "Test Hodnota BB");

    DTE.save();

    //Data table
    Document.screenshot("/redactor/apps/enumeration/dataTable_enumData.png");

    I.amOnPage("/apps/enumeration/admin/enumeration-type/");

    I.fillField("input.dt-filter-typeName", enumTypeNameA);
    I.pressKey('Enter', "input.dt-filter-typeName");
    DT.waitForLoader();

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    
    if("sk" === confLng) {
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.see("Nenašli sa žiadne vyhovujúce záznamy");
    } else if("en" === confLng) { 
        I.click("Delete", "div.DTE_Action_Remove");
        I.see("No matching records found");
    }
});

function openEnumType(I, DTE, enumTypeName) {
    I.fillField("input.dt-filter-typeName", enumTypeName);
    I.pressKey('Enter', "input.dt-filter-typeName");
    DTE.waitForLoader();
    I.click(enumTypeName);
    DTE.waitForEditor('enumerationTypeDataTable');
}

function filterEnumDataByType(I, DTE, typeName) {
    I.click("#pills-enumerationType-tab");
    I.fillField('body > div.bs-container.dropdown.bootstrap-select > div > div.bs-searchbox > input', typeName);
    I.see(typeName);
    I.click(typeName);
    DTE.waitForLoader();
}