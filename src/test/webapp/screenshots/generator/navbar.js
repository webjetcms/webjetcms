Feature('navbar');

Before(({ I, login }) => {
    login('admin');
});

Scenario('navbar editor', async ({ I, DTE, Document }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=24329");
    DTE.waitForEditor();
    I.wait(5);

    Document.screenshot("/redactor/apps/navbar/editor-dialog.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=24329");
    DTE.waitForEditor();
    DTE.cancel();
    I.click(".tree-col .buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.click("#pills-dt-groups-datatable-menu-tab");
    Document.screenshot("/redactor/apps/navbar/groups-dialog.png");

    I.amOnPage("/apps/navbar/podadresar-1/stranka-zobrazi-navigacnej-liste.html");
    Document.screenshotElement("div.navbartest", "/redactor/apps/navbar/navbar.png");
});
