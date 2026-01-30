Feature('apps.multistep-forms');

Before(({ I, DT, login }) => {
    login('admin');

    DT.addContext("formSteps", "#formStepsDataTable_wrapper");
    DT.addContext("formItems", "#formItemsDataTable_wrapper");
});

Scenario('Base screenshots', ({ I, DT, DTE, Document }) => {
    I.resizeWindow(1300, 530);

    I.amOnPage("/apps/form/admin/");
    Document.screenshot("/redactor/apps/multistep-form/forms-list.png");

    I.amOnPage("/apps/form/admin/detail/?formName=Multistepform_rowView");
    Document.screenshot("/redactor/apps/multistep-form/form-detail.png", null, null, "div.md-tabs > ul.nav > li:nth-child(4) > a");

    I.amOnPage("/apps/form/admin/form-content/?formName=Multistepform_1");
    Document.screenshot("/redactor/apps/multistep-form/default-form.png");

    I.resizeWindow(1500, 720)
    I.amOnPage("/apps/form/admin/form-content/?formName=Multistepform_docs");
    I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("Krok jedna") );
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
});

Scenario('App screenshots', ({ I, Apps, Document }) => {
    Apps.openAppEditor(156109);
    Document.screenshot("/redactor/apps/multistep-form/app-editor.png");
});