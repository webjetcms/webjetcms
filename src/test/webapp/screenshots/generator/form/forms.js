Feature('form.forms');

Before(({ I, login }) => {
    login('admin');
});

Scenario('forms', ({ I , DT, DTE, Document }) => {
    I.amOnPage("/apps/form/admin/#/detail/Dotaznik-spokojnosti-externy");
    DT.waitForLoader();

    Document.screenshot("/redactor/apps/form/detail.png");

    switch (I.getConfLng()) {
        case 'sk':
        case 'cs':
            I.click("08.07.2019 14:31:40");
            break;
        case 'en':
            I.click("07/08/2019 14:31:40");
            break;
        default:
            throw new Error(`Unsupported language code: ${I.getConfLng()}`);
    }


    Document.screenshot("/redactor/apps/form/detail-editnote.png");

    DTE.cancel();

    I.click("button.btn-export-dialog");
    I.wait(2);
    I.clickCss("#pills-export-advanced-tab");
    Document.screenshotElement("div.modal.show div.DTE_Action_Edit.modal-content", "/redactor/apps/form/export-advanced.png");
});

Scenario('forms email confirmation', ({ I , DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=93982");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.waitForElement('#WebJETEditorBody > form', 10);
    I.doubleClick('#WebJETEditorBody > form');
    I.switchTo();
    I.clickCss('#cke_wjFormAttributes_151');
    Document.screenshotElement('.cke_dialog_body', '/redactor/apps/form/advanced-settings.png');
    Document.screenshotElement('#attributesContent > table > tbody > tr:has(label[for="attribute-doubleOptIn"])', '/redactor/apps/form/checkbox-confirmation.png');
    Document.screenshotElement('#attributesContent > table > tbody > tr:has(input[id="attribute_recipients_id"])', '/redactor/apps/form/input-recipient.png');
    Document.screenshotElement('#attributesContent > table > tbody > tr:has(input[name="attribute_formmail_sendUserInfoDocId"])', '/redactor/apps/form/input-docid.png');

    I.amOnPage('/apps/form/admin/#/detail/Formular-doubleoptin');
    DT.waitForLoader();
    Document.screenshotElement('#forms-list-app .dt-scroll', '/redactor/apps/form/forms-list.png', 1280, 350);
    I.wjSetDefaultWindowSize();
});