Feature('apps.app-date');

Before(({ login }) => {
    login('admin');
});

Scenario('datum', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/datum");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/app-date/app-date.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=77767");
    DTE.waitForEditor();

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    I.wait(2);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.waitForElement("iframe.wj_component", 10);
    I.click("iframe.wj_component");
    I.wait(2);

    I.switchTo();
    I.wait(2);
    I.switchTo(".cke_dialog_ui_iframe");
    Document.screenshot("/redactor/apps/app-date/editor.png");
});
