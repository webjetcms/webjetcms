Feature('apps.emoticon');

Before(({ login }) => {
    login('admin');
});

Scenario('Emotikon', ({ I, DT, DTE, Document, i18n }) => {
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/contact/contact.png");
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.wait(5);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.clickCss('.cke_button.cke_button__components.cke_button_off');
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');
    I.waitForElement('#search', 10);
    I.fillField('#search', i18n.get('Emoticons'));
    I.wait(4);
    I.dontSeeElement('div.promoApp');

    I.clickCss('#components-emoticon-title');
    i18n.click("Add to page");
    Document.screenshot("/redactor/apps/emoticon/editor.png");
    I.fillField('#imgPath', 'biggrin.gif');
    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button.cke_dialog_ui_button_ok')
    Document.screenshot("/redactor/apps/emoticon/emoticon.png");
});
