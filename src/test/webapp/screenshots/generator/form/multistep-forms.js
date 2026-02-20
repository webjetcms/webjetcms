Feature('apps.multistep-forms');

Before(({ I, DT, login }) => {
    login('admin');

    DT.addContext("forms", "#formsDataTable_wrapper");
    DT.addContext("formSteps", "#formStepsDataTable_wrapper");
    DT.addContext("formItems", "#formItemsDataTable_wrapper");
});

Scenario('Base screenshots', ({ I, DT, DTE, Document }) => {
    I.resizeWindow(1280, 530);

    I.amOnPage("/apps/form/admin/");
    Document.screenshot("/redactor/apps/multistep-form/forms-list.png");

    I.resizeWindow(1280, 760);
    I.click(DT.btn.forms_add_button);
    DTE.waitForEditor("formsDataTable");
    DTE.fillField("formSettings-recipients", "");
    Document.screenshot("/redactor/apps/multistep-form/form-create-dialog.png");
    DTE.cancel();

    I.amOnPage("/apps/form/admin/detail/?formName=Registracia-na-online-kurz");
    Document.screenshot("/redactor/apps/multistep-form/form-detail.png", null, null, "div.md-tabs > ul.nav > li:nth-child(2) > a");

    I.amOnPage("/apps/form/admin/form-steps/?formName=Multistepform_1");
    Document.screenshot("/redactor/apps/multistep-form/default-form.png");

    I.resizeWindow(1360, 720)
    I.amOnPage("/apps/form/admin/form-steps/?formName=Registracia-na-online-kurz");
    I.waitForVisible("div.stepPreviewWrapper");
    Document.screenshot("/redactor/apps/multistep-form/real-form.png");

    I.click(DT.btn.formSteps_edit_button);
    DTE.waitForEditor("formStepsDataTable");
    Document.screenshot("/redactor/apps/multistep-form/form-step-editor.png");
    DTE.cancel();

    I.click( locate("table#formItemsDataTable > tbody > tr > td").withText("Meno") );
    I.click(DT.btn.formItems_add_button);
    DTE.waitForEditor("formItemsDataTable");
    Document.screenshot("/redactor/apps/multistep-form/form-item-editor.png");
    DTE.cancel();

    I.amOnPage("/apps/form/admin/form-steps/?formName=Multistepform_rowView");
    I.waitForVisible("div.stepPreviewWrapper");
    Document.screenshot("/redactor/apps/multistep-form/row-view.png");

    I.amOnPage("/apps/form/admin/");
    DT.filterEquals("formName", "Multistepform_emailVerification");
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-edit");
    DTE.waitForEditor("formsDataTable");
    I.clickCss("#pills-dt-formsDataTable-settings-advanced-tab");
    Document.screenshotElement("div.modal.show div.DTE_Action_Edit.modal-content", "/redactor/apps/form/form-step-email-verification-advanced.png");
    DTE.cancel();

    I.amOnPage("/apps/form/admin/form-steps/?formName=Multistepform_emailVerification");
    I.waitForVisible("div.stepPreviewWrapper");
    Document.screenshot("/redactor/apps/form/form-step-email-verification-1.png");
    I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("Krok 2") );
    Document.screenshot("/redactor/apps/form/form-step-email-verification-2.png");
});

Scenario('App screenshots', ({ I, Apps, Document }) => {
    Apps.openAppEditor(156109);
    Document.screenshot("/redactor/apps/multistep-form/app-editor.png");
});