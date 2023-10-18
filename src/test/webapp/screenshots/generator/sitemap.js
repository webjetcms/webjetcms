Feature('sitemap');

Before(({ I, login }) => {
    login('admin');
});

Scenario('sitemap editor', async ({ I, DTE, Document }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=24217");
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.click("iframe.wj_component");

    Document.screenshot("/redactor/apps/sitemap/editor-dialog.png");
    I.switchTo();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=24217");
    DTE.waitForEditor();
    DTE.cancel();

    I.click(".tree-col .buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.click("#pills-dt-groups-datatable-menu-tab");
    Document.screenshot("/redactor/apps/sitemap/groups-dialog.png");

    I.amOnPage("/apps/mapa-stranok/");
    Document.screenshotElement("div.sitemaptest", "/redactor/apps/sitemap/sitemap.png");
});
