Feature('apps.app-weather');

Before(({ login }) => {
    login('admin');
});

Scenario('app weather', ({ I, DT, DTE, Document }) => {

    I.amOnPage("/apps/pocasie/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-weather/app-weather.png");
    
    I.amOnPage("/admin/v9/webpages/web-pages-list");

    I.jstreeClick("Aplikácie");
    I.wait(1);
    I.jstreeClick("Počasie");
    I.click('//a[contains(.,"Počasie") and @href="javascript:;"]');
    DTE.waitForEditor();

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.wait(2);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.waitForElement("iframe.wj_component", 10);

    I.click("iframe.wj_component");
    I.wait(2);

    I.switchTo();
  
    I.waitForElement("iframe.cke_dialog_ui_iframe", 10);
    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("iframe#editorComponent");

    I.wait(1);

    Document.screenshot("/redactor/apps/app-weather/editor.png");
    
});
