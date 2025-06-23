Feature('apps.related-pages');

Before(({ login }) => {
    login('admin');
});

Scenario('app related pages', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/pribuzne-stranky/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/related-pages/related-pages.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/");

    I.jstreeClick("Aplikácie");
    I.wait(1);
    I.jstreeClick("Príbuzné stránky");
    I.click('//a[contains(.,"Príbuzné stránky") and @href="javascript:;"]');
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

    Document.screenshot("/redactor/apps/related-pages/editor.png");

});
