Feature('apps.video');

Before(({ login }) => {
    login('admin');
});

Scenario('video', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/video/");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/video/video.png");
    
    I.amOnPage("/admin/v9/webpages/web-pages-list");

    I.jstreeClick("Aplikácie");
    I.wait(1);
    I.jstreeClick("Video");
    I.click('//a[contains(.,"Video") and @href="javascript:;"]');
    DTE.waitForEditor();

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.wait(2);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.waitForElement("iframe.wj_component", 10);

    I.click("iframe.wj_component");

    I.wait(1);
    Document.screenshot("/redactor/apps/video/editor-parameters.png");
    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");
    I.click(".choose.Button.button.green");
    Document.screenshot("/redactor/apps/video/editor-source.png");
    
    
    
});
