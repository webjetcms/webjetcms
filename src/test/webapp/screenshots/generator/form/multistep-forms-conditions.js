Feature('apps.forms.multistep-forms-conditions');

const formName = "multistep_condition_test";
const editor = "#pills-dt-editor-formItemsDataTable-Content";

Before(({ I, DT, login }) => {
    login('admin');

    DT.addContext("formSteps", "#formStepsDataTable_wrapper");
    DT.addContext("formItems", "#formItemsDataTable_wrapper");
});

Scenario('Relations tab screenshot', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/form/admin/form-steps/?formName=" + formName);
    I.waitForFunction(() => {
        const step = document.querySelector('#formStepsDataTable tbody tr.selected');
        return step && new URL(formItemsDataTable.getAjaxUrl(), location.origin).searchParams.get('stepId') === step.id;
    }, [], 15);
    DT.waitForLoader("formItemsDataTable");

    I.waitForText("Checkgroup", 10, "#formItemsDataTable");
    I.click(locate("#formItemsDataTable tbody td").withText("Checkgroup"));
    I.click(DT.btn.formItems_edit_button);
    DTE.waitForEditor("formItemsDataTable");

    I.clickCss("#pills-dt-formItemsDataTable-relations-tab");
    I.waitForVisible("#formItemVisibilityUsage button.form-item-relations-name");
    I.waitForVisible("#formItemRequirementUsageEmpty");
    Document.screenshotElement("#formItemsDataTable_modal .modal-content", "/redactor/apps/multistep-form/tab-relations.png", 1280, 720);
    DTE.cancel("formItemsDataTable");
});

Scenario('Before test preparation', async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/form/admin/form-steps/?formName=" + formName);
    DT.waitForLoader();

    I.clickCss("#formItemsDataTable td.sorting_1");
    I.click(DT.btn.formItems_edit_button);
    DTE.waitForEditor("formItemsDataTable");

    I.clickCss("#pills-dt-formItemsDataTable-visibilityConditions-tab");
    I.waitForVisible(editor);

    Document.screenshot("/redactor/apps/multistep-form/tab-visibilityConditions.png");

    I.clickCss(editor + " button.buttons-create");
    DTE.waitForEditor("datatableFieldDTE_Field_visibilityConditions");

    I.wait(3);
    await I.executeScript(() => {
        $("#datatableFieldDTE_Field_visibilityConditions_modal div.DTE.modal-content").css("top", "110px");
    });

    Document.screenshot("/redactor/apps/multistep-form/tab-visibilityConditions-create.png");

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
