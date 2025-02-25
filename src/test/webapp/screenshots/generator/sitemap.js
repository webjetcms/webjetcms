Feature('sitemap');

Before(({ I, login }) => {
    login('admin');
});

Scenario('sitemap editor sitemap', async ({ I, DTE, Document }) => {
    let confLng = I.getConfLng();
    //Default let SK
    let docId;

    switch (confLng) {
        case 'sk':
            docId = 24217;
            break;
        case 'en':
            docId = 81851;
            break;
        case 'cs':
            docId = 100093;
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.click("iframe.wj_component");

    Document.screenshot("/redactor/apps/sitemap/editor-dialog.png");
    I.switchTo();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
    DTE.waitForEditor();
    DTE.cancel();

    I.click(".tree-col .buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-menu-tab");
    Document.screenshot("/redactor/apps/sitemap/groups-dialog.png");

    if(("sk" === confLng)) {
        I.amOnPage("/apps/mapa-stranok/");
    } else if(("en" === confLng)) {
        I.amOnPage("/apps/sitemap/");
    } else if(("cs" === confLng)) {
        I.amOnPage("/apps/mapa-stranek/");
    }
    
    Document.screenshotElement(".sitemap", "/redactor/apps/sitemap/sitemap.png");
});
