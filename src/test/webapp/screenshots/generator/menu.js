Feature('menu');

Before(({ I, login }) => {
    login('admin');
});

Scenario('menu editor', async ({ I, DTE, Document }) => {

    Document.screenshotAppEditor(25209, "/redactor/apps/menu/editor-dialog.png")

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25209");
    DTE.waitForEditor();
    DTE.cancel();

    I.click(".tree-col .buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-menu-tab");
    Document.screenshot("/redactor/apps/menu/groups-dialog.png");

    I.amOnPage("/apps/menu/?NO_WJTOOLBAR=true");
    Document.screenshotElement("header.ly-header", "/redactor/apps/menu/top-menu.png");
    Document.screenshotElement("#menutest", "/redactor/apps/menu/left-menu.png");
});
