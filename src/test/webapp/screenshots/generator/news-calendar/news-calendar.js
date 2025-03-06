Feature('apps.news-calendar');

Before(({ login }) => {
    login('admin');
});

Scenario('news calendar', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/kalendar-noviniek/?datum=29.10.2018");
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/news-calendar/news-calendar.png");

    Document.screenshotAppEditor(120026 , "/redactor/apps/news-calendar/editor.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=120026");
    DTE.waitForEditor();
    I.waitForElement(".cke_wysiwyg_frame.cke_reset");
    I.switchTo(".cke_wysiwyg_frame.cke_reset");
    I.waitForElement("iframe.wj_component", 10);
    I.switchTo(locate("iframe.wj_component").at(2));
    I.click("a.inlineComponentButton.cke_button");
    I.wait(5);
    I.switchTo();
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');
    I.clickCss("#tabLink1");
    I.switchTo();
    Document.screenshotElement("#datatableInit_modal > div > div", "/redactor/apps/news-calendar/news-editor.png");
});