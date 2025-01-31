Feature('apps.enumeration');

Before(({ I, login }) => {
    login('admin');
});

var enumPrefix = "Číselnik_";
var enumeration_A = "Číselnik_A";
var enumeration_B = "Číselnik_B";
var enumeration_C = "Číselnik_C";

Scenario('Enum Types screenshots', ({I, DT, DTE, Document, i18n}) => {
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
    Document.screenshot("/redactor/apps/enumeration/dataTable_enumType.png");

    DT.filterContains("typeName", enumeration_A);
    I.click(enumeration_A);
    DTE.waitForEditor('enumerationTypeDataTable');
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/enumeration/editor_enumType.png");
    I.clickCss("#pills-dt-enumerationTypeDataTable-strings-tab");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/enumeration/editor_stringTab.png");
    I.clickCss("#pills-dt-enumerationTypeDataTable-booleans-tab");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/enumeration/editor_booleanTab.png");
    DTE.cancel();

    DT.filterContains("typeName", enumeration_B);
    I.click(enumeration_B);
    DTE.waitForEditor('enumerationTypeDataTable');
    I.click( locate( "div.DTE_Field_Name_editorFields\\.childEnumTypeId > div > div > div.dropdown > button.dropdown-toggle") );
    I.waitForVisible("div.dropdown-menu.show");
    I.fillField(locate("div.dropdown-menu.show").find("input"), enumPrefix);
    Document.screenshot("/redactor/apps/enumeration/editor_select_1.png");
    I.click( locate("div.dropdown-menu").find( locate("a.dropdown-item").withText(enumeration_A) ) );
    DTE.save();
    i18n.waitForText("Selected enumeration link Číselnik_A not possible because this enum is already linked to the current enum.");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/enumeration/editor_select_2.png");
    DTE.cancel();

    DT.filterContains("typeName", enumeration_C);
    I.click(enumeration_C);
    DTE.waitForEditor('enumerationTypeDataTable');
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/enumeration/editor_select_3.png");
});

Scenario('Enum Datas screenshots', ({I, DTE, Document}) => {
    I.amOnPage("/apps/enumeration/admin/#2921");
    Document.screenshot("/redactor/apps/enumeration/dataTable_enumData.png");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor('enumerationDataDataTable');
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Create", "/redactor/apps/enumeration/editor_enumData.png");
});