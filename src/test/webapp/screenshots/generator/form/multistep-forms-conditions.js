Feature('apps.forms.multistep-forms-conditions');

const formName = "multistep_condition_test";
const editor = "#pills-dt-editor-formItemsDataTable-Content";

Before(({ I, DT, login }) => {
    login('admin');

    DT.addContext("formSteps", "#formStepsDataTable_wrapper");
    DT.addContext("formItems", "#formItemsDataTable_wrapper");
});

Scenario('Before test preparation', async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/form/admin/form-steps/?formName=" + formName);
    DT.waitForLoader();

    I.clickCss("#formItemsDataTable td.sorting_1");
    I.click(DT.btn.formItems_edit_button);
    DTE.waitForEditor("formItemsDataTable");

    I.clickCss("#pills-dt-formItemsDataTable-visibilityConditions-tab");
    I.waitForVisible(editor);

    Document.screenshotElement("div.modal.show div.DTE_Action_Edit.modal-content", "/redactor/apps/multistep-form/tab-visibilityConditions.png");

    I.clickCss(editor + " button.buttons-create");
    DTE.waitForEditor("datatableFieldDTE_Field_visibilityConditions");

    Document.screenshotElement("#datatableFieldDTE_Field_visibilityConditions_modal div.DTE_Action_Create", "/redactor/apps/multistep-form/tab-visibilityConditions-create.png");

    I.clickCss("#datatableFieldDTE_Field_visibilityConditions_modal button.btn-close-editor");

    DTE.cancel();

    I.click( locate("tr").withChild( locate("td div.datatable-column-width").withText("Checkgroup")  )  )
    I.click(DT.btn.formItems_edit_button);
    DTE.waitForEditor("formItemsDataTable");
    DTE.save();

    I.waitForVisible("#toast-container-webjet");
    I.moveCursorTo("#toast-container-webjet");
    Document.screenshotElement("#toast-container-webjet div.toast-warning", "/redactor/apps/multistep-form/save-condition-error.png");
});