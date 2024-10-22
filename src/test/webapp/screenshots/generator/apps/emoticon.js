Feature('apps.emoticon');
var doc_add_button = (locate("#datatableInit_wrapper > div.dt-header-row.clearfix.wp-header > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success"));

Before(({ login }) => {
    login('admin');
});

Scenario('Emotikon', ({ I, DT, DTE, Document }) => {
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/contact/contact.png");
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.click(doc_add_button);
    DTE.waitForEditor();
    I.wait(5);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.clickCss('.cke_button.cke_button__components.cke_button_off');
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');
    I.waitForElement('#search', 10);
    I.fillField('#search', 'Emotikony');
    I.wait(4);
    I.dontSeeElement('div.promoApp');

    I.clickCss('#components-emoticon-title'); 
    I.click("Vložiť do stránky"); 
    Document.screenshot("/redactor/apps/emoticon/editor.png");
    I.fillField('#imgPath', 'biggrin.gif');
    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button.cke_dialog_ui_button_ok')
    Document.screenshot("/redactor/apps/emoticon/emoticon.png");
});
