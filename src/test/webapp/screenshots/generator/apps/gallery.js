Feature('apps.gallery');

Before(({ login }) => {
    login('admin');
});

Scenario('zoznam fotografii', ({ I, DT, DTE, Document }) => {

    I.amOnPage('/admin/v9/apps/gallery');
    DT.waitForLoader();
    I.clickCss('td.dt-row-edit');

	let tabs = ['description', 'metadata', 'editor', 'area_of_interest'];
    tabs.forEach((tab, index) => {
        I.clickCss(`#pills-dt-editor-galleryTable > li:nth-child(${index + 1})`);
        Document.screenshotElement('.DTE.DTE_Action_Edit.modal-content', `/redactor/apps/gallery/${tab}-preview.png`);
    });

    I.clickCss('button.btn.btn-outline-secondary.btn-close-editor');

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
    Document.screenshotAppEditor(45926, "/redactor/apps/gallery/editor-dialog.png", function(Document, I, DT, DTE, Apps) {
        //prepni sa na prvu kartu
        Apps.switchToTabByIndex(0);
    }, 1280, 800);

    //zobrazenie galerie
    I.amOnPage("/apps/galeria/?NO_WJTOOLBAR");
    Document.screenshot("/redactor/apps/gallery/photoswipe.png");
});

Scenario('thumb servlet', ({ I, DT, DTE, Document }) =>  {

    I.amOnPage('/admin/v9/apps/gallery/?dir=/images/gallery/test-vela-foto');
    I.click('dsc04068.jpeg');
    DTE.waitForEditor('galleryTable');
    I.clickCss("#pills-dt-galleryTable-areaOfInterest-tab");
    I.fillField("#zoom", "65");
    I.wait(2);
    I.fillField("#x", "276");
    I.wait(2);
    I.fillField("#y", "123");
    I.wait(2);
    I.fillField("#w", "438");
    I.wait(2);
    I.fillField("#h", "590");
    I.wait(5);
    Document.screenshot('/frontend/thumb-servlet/editor-original-image.png');
    DTE.save();

    I.amOnPage('/images/gallery/test-vela-foto/dsc04068.jpeg');
    Document.screenshotElement('img', '/frontend/thumb-servlet/original-image.png');

    I.amOnPage('/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?w=200&h=200');
    Document.screenshotElement('img', '/frontend/thumb-servlet/thumb-image.png');

    I.amOnPage('/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?w=200&ip=1');
    Document.screenshotElement('img', '/frontend/thumb-servlet/ip-1.png');

    I.amOnPage('/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?h=200&ip=2');
    Document.screenshotElement('img', '/frontend/thumb-servlet/ip-2.png');

    I.amOnPage('/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?w=300&h=200&ip=3');
    Document.screenshotElement('img', '/frontend/thumb-servlet/ip-3.png');

    I.amOnPage('/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?w=300&h=200&ip=4&c=ffff00');
    Document.screenshotElement('img', '/frontend/thumb-servlet/ip-4.png');

    I.amOnPage('/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?w=200&h=200&ip=5');
    Document.screenshotElement('img', '/frontend/thumb-servlet/ip-5.png');

    I.amOnPage('/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?w=200&h=200&ip=6');
    Document.screenshotElement('img', '/frontend/thumb-servlet/ip-6.png');

    I.amOnPage('/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?w=300&h=200&ip=4&noip=true&c=ffff00');
    Document.screenshotElement('img', '/frontend/thumb-servlet/noip-4.png');

});