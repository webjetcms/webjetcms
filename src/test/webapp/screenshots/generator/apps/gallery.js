Feature('apps.gallery');

Before(({ login }) => {
    login('admin');
});

Scenario('zoznam fotografii', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();
    I.click("test-vela-foto", "#SomStromcek");

    Document.screenshot("/redactor/apps/gallery/admin-dt.png");

    I.click("div.tree-col button.buttons-edit");
    Document.screenshot("/redactor/apps/gallery/admin-edit-group.png");

    DTE.cancel();

    Document.screenshotElement("#galleryTable_wrapper div.dt-header-row", "/redactor/apps/gallery/admin-toolbar-photo.png");

    I.click("dsc04068.jpeg");
    DTE.waitForEditor("galleryTable");
    Document.screenshot("/redactor/apps/gallery/admin-edit-photo.png");

    //editor
    Document.screenshotAppEditor(45926, "/redactor/apps/gallery/editor-dialog.png", function(Document, I, DT, DTE) {
        //prepni sa na prvu kartu
        I.click("#tabLink1");
    }, 1280, 800);

    //zobrazenie galerie
    I.amOnPage("/apps/galeria/?NO_WJTOOLBAR");
    Document.screenshot("/redactor/apps/gallery/photoswipe.png");
});
