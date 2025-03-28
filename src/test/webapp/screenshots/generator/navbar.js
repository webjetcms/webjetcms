Feature('navbar');

Before(({ I, login }) => {
    login('admin');
});

Scenario('navbar editor', async ({ I, DTE, Document }) => {

    let docid = "24329";
    if("en" === I.getConfLng()) { docid = "81950"; }

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docid);
    DTE.waitForEditor();
    I.wait(5);

    Document.screenshot("/redactor/apps/navbar/editor-dialog.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docid);
    DTE.waitForEditor();
    DTE.cancel();
    I.click(".tree-col .buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-menu-tab");
    Document.screenshot("/redactor/apps/navbar/groups-dialog.png");

    I.amOnPage("/apps/navbar/subfolder1/the-page-appears-navbar.html");
    Document.screenshotElement("div.navbartest", "/redactor/apps/navbar/navbar.png");
});
