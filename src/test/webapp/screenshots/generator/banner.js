Feature('banner');

Before(({ I, login }) => {
    login('admin');
});

Scenario('banner admin', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/banner/admin/");

    Document.screenshot("/redactor/apps/banner/datatable.png");

    DT.filter("name", "WebJET");

    I.click("WebJET CMS");
    DTE.waitForEditor("bannerDataTable");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/banner/editor.png");

    I.click("#pills-dt-bannerDataTable-restrictions-tab");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/banner/editor-restrictions.png");
});

Scenario('banner editor', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=21343");
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.click("iframe.wj_component");
    I.wait(5);

    Document.screenshot("/redactor/apps/banner/editor-dialog.png");
    I.switchTo();

    I.amOnPage("/apps/bannerovy-system/");
    I.wait(5);
    Document.screenshotElement("div.banner-image", "/redactor/apps/banner/banner-image.png");
    Document.screenshotElement("div.banner-html", "/redactor/apps/banner/banner-html.png");
    Document.screenshotElement("div.banner-content", "/redactor/apps/banner/banner-content.png");
});