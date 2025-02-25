const assert = require('assert');

Feature('apps.gallery');

var randomNumber;
var autoName;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        autoName = 'autotest-' + randomNumber;
    }
});


Scenario('zoznam fotografii', ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.click("test", "#SomStromcek");
    I.see("koala.jpg");
});

Scenario('zoznam fotografii-pamatanie velkosti', ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.jstreeClick("test-vela-foto");

    I.clickCss("button.btn-gallery-size-l");
    I.see("Záznamy 1 až 4 z", "div.dt-footer-row");

    //reloadni stranku
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.jstreeClick("test-vela-foto");
    DT.waitForLoader();
    I.see("Záznamy 1 až 4 z", "div.dt-footer-row");
    I.seeElement("button.btn-gallery-size-l.active");

    //prepni na default
    I.clickCss("button.btn-gallery-size-s");
    I.see("Záznamy 1 až 16 z", "div.dt-footer-row");
});

Scenario('oblast zaujmu', async ({ I, DT, DTE }) => {
    const assert = require('assert');
    const generateRandomNum = num => Math.floor(Math.random() * num);
    let area = { w: null, h: null, x: null, y: null };
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.click("test", "#SomStromcek");
    DT.waitForLoader();
    I.seeAndClick("koala.jpg");
    I.clickCss("#pills-dt-galleryTable-areaOfInterest-tab");

    I.waitForElement("div.vue-preview__wrapper");

    I.see("Šírka:");
    within('.coordinates', () => {
        I.fillAreaField(area, generateRandomNum);
        // naschval sa spusta dva krat aby otestovalo menenie poli
        I.fillAreaField(area, generateRandomNum);
    });
    DTE.save();

    // vue-advanced-cropper padal ked bol inicializovany, okno sa zatvorilo a zmenila sa velkost
    I.resizeWindow(1280, 850);
    I.seeAndClick("koala.jpg");
    DTE.waitForEditor("galleryTable");

    I.clickCss('#pills-dt-galleryTable-areaOfInterest-tab');
    I.wait(2);
    let inputValueW = await I.grabValueFrom('#w');
    let inputValueH = await I.grabValueFrom('#h');
    let inputValueX = await I.grabValueFrom('#x');
    let inputValueY = await I.grabValueFrom('#y');
    assert.equal(+inputValueH, +area.h);
    assert.equal(+inputValueW, +area.w);
    assert.equal(+inputValueX, +area.x);
    assert.equal(+inputValueY, +area.y);

    //otvor iny obrazok a znova over
    DTE.cancel();
    I.seeAndClick("lighthouse.jpg");
    DTE.waitForEditor("galleryTable");
    I.clickCss('#pills-dt-galleryTable-areaOfInterest-tab');
    I.wait(2);
    DTE.cancel();

    I.seeAndClick("koala.jpg");
    DTE.waitForEditor("galleryTable");

    I.clickCss('#pills-dt-galleryTable-areaOfInterest-tab');
    I.wait(2);
    inputValueW = await I.grabValueFrom('#w');
    inputValueH = await I.grabValueFrom('#h');
    inputValueX = await I.grabValueFrom('#x');
    inputValueY = await I.grabValueFrom('#y');
    assert.equal(+inputValueH, +area.h);
    assert.equal(+inputValueW, +area.w);
    assert.equal(+inputValueX, +area.x);
    assert.equal(+inputValueY, +area.y);

    //obnov stranku a over znova
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.waitForText("test", 10, "#SomStromcek");
    I.wait(2);
    I.click("test", "#SomStromcek");
    DT.waitForLoader();
    I.seeAndClick("koala.jpg");
    I.clickCss('#pills-dt-galleryTable-areaOfInterest-tab');
    I.wait(1);
    inputValueW = await I.grabValueFrom('#w');
    inputValueH = await I.grabValueFrom('#h');
    inputValueX = await I.grabValueFrom('#x');
    inputValueY = await I.grabValueFrom('#y');
    assert.equal(+inputValueH, +area.h);
    assert.equal(+inputValueW, +area.w);
    assert.equal(+inputValueX, +area.x);
    assert.equal(+inputValueY, +area.y);
});

Scenario('galeria v stranke', ({ I }) => {
    I.amOnPage("/apps/galeria/");
    I.dontSee("loading=\"lazy\"");
    I.see("Tesla Supercharger Bratislava");
    I.forceClick("Tesla Supercharger Bratislava");
    I.waitForElement("div.pswp--open");
    I.see("v Bratislave");
    I.see("Auparku", "span.photoswipeLongDesc a")
    I.see("autora fotky", "small p a");
});

function testGalleryTree(dir, imageName, I) {
    if (dir.startsWith("/admin/v9/")==false) I.amOnPage("/admin/v9/apps/gallery/?dir="+dir);
    I.say("testGalleryTree, dir="+dir+", imageName="+imageName);

    if (dir.endsWith("/")) {
        dir = dir.substring(0, dir.length-1);
    }
    var i = dir.lastIndexOf("/");
    var name = dir.substring(i+1);

    I.waitForElement(locate("a.jstree-anchor.jstree-clicked").withText(name), 10);
    I.seeElement(locate("a.jstree-anchor.jstree-clicked").withText(name));
    I.waitForText(imageName, 10, "table.datatableInit td.dt-row-edit");
}

Scenario('otvorenie galerie s URL parametrom', async({ I }) => {
    I.say("Testujem presmerovanie");
    await I.executeScript(function() {
        window.location.href="/admin/photogallery.do?dir=/images/gallery/user/";
    });
    I.seeInCurrentUrl("/admin/v9/apps/gallery/?dir=/images/gallery/user/");

    I.say("Testujem zobrazenie stromovej struktury a obrazku");
    testGalleryTree("/admin/v9/apps/gallery/?dir=/images/gallery/user/", "demo.jpg", I);
    I.waitForText("demo.jpg", 10, "table.datatableInit td.dt-row-edit");
    I.see("demo.jpg", "table.datatableInit td.dt-row-edit");

    //BUG: on second level it's not working
    testGalleryTree("/images/gallery/test/second-level", "2023-52.jpg", I);

    //BUG: therewas bug with the URL parametr with endsWith /
    testGalleryTree("/images/gallery/test/second-level/", "2023-52.jpg", I);
});

Scenario('multidomain zobrazenie', async({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.jstreeClick("test", true);
    I.see("koala.jpg");
    I.dontSee("dsc04082.jpeg");

    // prepnutie na domenu mirroring.tau27.iway.sk
    I.say("Prepinam domenu");
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("mirroring.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");

    I.wait(2);
    DT.waitForLoader();

    I.jstreeClick("test", true);
    I.dontSee("koala.jpg");
    I.see("dsc04082.jpeg");
});

Scenario('odhlasenie', ({ I }) => {
    I.logout();
});

Scenario('novy priecinok', async({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.click("test", "#SomStromcek");

    //pri vytvoreni noveho priecinka musi byt pole pre zadanie nazvu prazdne
    I.clickCss("div.tree-col button.buttons-create");
    DTE.waitForEditor("galleryDimensionDatatable");
    I.dontSeeInField("#DTE_Field_name", "test");
    I.seeInField("#DTE_Field_path", "/images/gallery/test");
    let name = await I.grabValueFrom("#DTE_Field_name");
    I.assertEqual(name, "");
});

Scenario('bug-remember column order', ({ I, DT, Browser }) => {

    I.resizeWindow(1680, 760);

    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();

    I.waitForElement("button.btn-gallery-size-table", 5);
    I.wait(1);
    I.clickCss("button.btn-gallery-size-table");
    DT.resetTable("galleryTable");
    DT.waitForLoader();

    var position = 4;
    if (Browser.isFirefox()) {
        I.dragAndDrop(
            "#galleryTable_wrapper div.dt-scroll-headInner th.dt-th-descriptionShortCz",
            "#galleryTable_wrapper div.dt-scroll-headInner th.dt-th-imageName",
            {
                force: true,
                sourcePosition: {x: 10, y: 10},
                targetPosition: { x: 10, y: 10 }
            }
        );
        position = 5;
    } else {
        I.dragAndDrop(
            "#galleryTable_wrapper div.dt-scroll-headInner th.dt-th-descriptionShortCz",
            "#galleryTable_wrapper div.dt-scroll-headInner th.dt-th-imagePath",
            {
                force: true,
                sourcePosition: {x: 10, y: 10},
                targetPosition: { x: 10, y: 10 }
            }
        );
    }
    I.see("Názov cz", "#galleryTable_wrapper div.dt-scroll-headInner table thead tr th:nth-child("+position+")");

    //
    I.say("Reload and check if the column order is preserved");
    I.amOnPage("/admin/v9/apps/gallery/");
    I.see("Názov cz", "#galleryTable_wrapper div.dt-scroll-headInner table thead tr th:nth-child("+position+")");

    //
    I.say("check column settings-there was bug with duplicate buttons on header and footer");

    var container = "#galleryTable_wrapper";
    I.clickCss(container+" button.buttons-settings");
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.see("Priečinok", "div.colvisbtn_wrapper button.buttons-columnVisibility span.column-title");

});

Scenario('bug-remember column order-reset', ({ I, DT }) => {
    I.wjSetDefaultWindowSize();

    I.amOnPage("/admin/v9/apps/gallery/");

    I.clickCss("button.btn-gallery-size-s");
    DT.resetTable("galleryTable");
});

Scenario('editor check dir select', ({ I, Document, DTE, Browser }) => {
    I.closeOtherTabs();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=45926");
    DTE.waitForEditor();
    I.wait(5);
    Document.editorComponentOpen();
    if (Browser.isFirefox()) I.wait(1);

    I.waitForElement("#pills-dt-component-datatable-basic-tab");
    DTE.seeInField("dir", "/images/gallery/test-vela-foto");

    I.clickCss('#editorAppDTE_Field_dir button');
    I.waitForInvisible('Loading', 10);

    I.click('user', '.jstree-container-ul.jstree-children');
    I.wait(1);
    I.openNewTab();
    I.closeCurrentTab();

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");
    DTE.seeInField("dir", "/images/gallery/user");
    I.switchTo();
    I.click(locate("div.cke_dialog_container td.cke_dialog_footer a span").withText("Zrušiť"));
    DTE.cancel();
});

Scenario('close tabs', ({ I }) => {
    I.closeOtherTabs();
});

function editImage(name, save, I, DTE) {
    I.say("Edit image "+name)
    I.waitForText(name, 10, "#galleryTable");
    I.click(locate("td.dt-row-edit a").withText(name));
    DTE.waitForEditor("galleryTable");
    I.clickCss("#pills-dt-galleryTable-photoeditor-tab");
    I.waitForElement("li.tie-btn-crop.tui-image-editor-item", 10);

    if (save) {
        I.forceClickCss("li.tie-btn-flip.tui-image-editor-item");
        I.wait(0.5);
        I.forceClickCss("div.tui-image-editor-button.flipX");
        I.wait(0.5);
        I.forceClickCss("div.tui-image-editor-button.flipX");
        I.wait(0.5);
        DTE.save("galleryTable");
        waitForUploadMessage(I, name);
    } else {
        DTE.cancel("galleryTable");
        I.waitForElement("#toast-container-upload", 10);
        I.wait(3);
        I.dontSeeElement(locate("#toast-container-upload div.toast-message span").withText(name));
    }
}

function waitForUploadMessage(I, name) {
    I.waitForElement("#toast-container-upload", 10);
    I.waitForElement(locate("#toast-container-upload div.toast-message span").withText(name), 10);
    I.waitForElement("#toast-container-upload i.ti-spin", 10);
    I.waitForInvisible("#toast-container-upload i.ti-spin", 10);
    I.waitForVisible(locate("#toast-container-upload div.toast-message").withText(name).find("i.ti-circle-check"), 10);
    I.wait(0.5);
    I.waitForVisible(locate("#toast-container-upload div.toast-message").withText(name).find("i.ti-circle-check"), 10);
}

Scenario('Gallery - image editor test', async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test/editor");

    //There can be different view of the gallery
    I.clickCss("button.btn-gallery-size-s");
    DT.waitForLoader();

    I.say(`Duplicating image turtle.png`)
    I.click(locate("tr").withDescendant(locate("td.dt-row-edit a").withText("turtle.png")));
    I.click(DT.btn.gallery_duplicate_button);
    DTE.waitForLoader();
    //verify correct name with multiple extensions, turtle is png
    I.fillField('#DTE_Field_descriptionShortSk', autoName+".jpg");

    I.say("Check that field imageName is disabled during duplication");
    I.clickCss("#pills-dt-galleryTable-metadata-tab");
    I.waitForVisible("#DTE_Field_imageName[disabled]", 10);

    DTE.save();

    //fix filename according to backend changes
    let realFileName = (autoName+".jpg.png").replace(".jpg.png", "-jpg.png");

    I.say(`Edit image ${realFileName}`);
    I.waitForText(realFileName, 10, "#galleryTable");

    I.say("Bug test, check that s_ version of image is not here");
    I.dontSee("s_" + realFileName, "#galleryTable");

    I.click(locate("td.dt-row-edit a").withText(realFileName));
    DTE.waitForEditor("galleryTable");
    I.clickCss("#pills-dt-galleryTable-photoeditor-tab");
    I.waitForElement("li.tie-btn-crop.tui-image-editor-item", 10);

    I.say(`Rotating image ${realFileName}` )
    I.forceClickCss("li.tie-btn-rotate.tui-image-editor-item.normal");
    I.clickCss(".tui-image-editor-button.clockwise");
    I.clickCss(".tui-image-editor-button.clockwise");
    I.clickCss(".tui-image-editor-button.clockwise");
    I.forceClickCss("li.tie-btn-rotate.tui-image-editor-item.normal");
    await Document.compareScreenshotElement('div.tui-image-editor-canvas-container', "autotest-turtle0.png", null, null, 20);

    DTE.save("galleryTable");
    I.say('waiting for changes to be applied to the file system');
    waitForUploadMessage(I, realFileName);

    I.waitForElement("#toast-container-upload", 10);

    I.amOnPage(`/images/gallery/test/editor/${realFileName}`);
    await Document.compareScreenshotElement('body > img', "autotest-image_rotated.png", null, null, 20);
});

Scenario('Gallery - upload image test', ({I,DT, DTE}) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=17321');
    DTE.waitForEditor();

    I.say('Creating new folder');
    I.waitForElement('.cke_button__image', 10);
    I.clickCss('.cke_button__image');
    I.switchTo("#wjImageIframeElement");
    I.waitForElement('#nav-iwcm_fs_ap_volume_ > span.elfinder-perms', 10);
    I.clickCss('#nav-iwcm_fs_ap_volume_ > span.elfinder-perms', null, { position: { x: 3, y: 3 } });
    I.wait(2)
    I.forceClick('#nav-iwcm_fs_ap_volume_L2ltYWdlcy9nYWxsZXJ5L2FwcHM_E');
    I.waitForVisible('.elfinder-button-icon.elfinder-button-icon-mkdir', 10);
    I.wait(5)
    I.click('.elfinder-button-icon.elfinder-button-icon-mkdir');
    I.type(autoName);
    I.clickCss('#nav-iwcm_fs_ap_volume_L2ltYWdlcy9nYWxsZXJ5L2FwcHM_E');
    I.wait(2);
    I.doubleClick(autoName);
    I.clickCss('.elfinder-button-icon.elfinder-button-icon-upload');
    I.say('Uploading new file');
    I.attachFile('input[type=file]', 'tests/apps/gallery.png');
    waitForUpload(I);
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_cancel');

    I.amOnPage(`/admin/v9/apps/gallery/?dir=/images/gallery/apps/${autoName}`);
    I.say('Checking if folder icon is empty');
    I.dontSeeElement(`//a[contains(., "${autoName}")]/i[contains(@class, "ti-folder-filled")]`);
    I.seeElement(`//a[contains(., "${autoName}")]/i[contains(@class, "ti-folder")]`);
    I.click(DT.btn.tree_edit_button);
    DT.waitForLoader();
    I.say('Checking size of the uploaded image')
    I.clickCss('#pills-dt-galleryDimensionDatatable-sizes-tab');
    I.seeInField('#DTE_Field_imageWidth', 200);
    I.seeInField('#DTE_Field_imageHeight', 200);
    I.seeInField('#DTE_Field_normalWidth', 400);
    I.seeInField('#DTE_Field_normalHeight', 400);
    DTE.cancel();
});

Scenario('Gallery - uploaded image size test', async ({ I }) => {
    const imageUrls = [
        { url: '/images/gallery/apps/' + autoName + '/gallery.png', expected: { height: 400, width: 400 } },
        { url: '/images/gallery/apps/' + autoName + '/s_gallery.png', expected: { height: 200, width: 200 } },
        { url: '/images/gallery/apps/' + autoName + '/o_gallery.png', expected: { height: 455, width: 640 } }
    ];

    for (const imageUrl of imageUrls) {
        I.amOnPage(imageUrl.url);

        const rect = await I.grabElementBoundingRect('img');

        I.assertEqual(rect.height, imageUrl.expected.height, `Image ${imageUrl.url} does not have correct height.`);
        I.assertEqual(rect.width, imageUrl.expected.width, `Image ${imageUrl.url} does not have correct width.`);
    }
});

Scenario('Gallery - uploaded image delete', async ({I, DTE, DT}) => {
    I.say('Deleting copy');
    const copyNameSelector = locate("tr").withDescendant(locate("td.dt-row-edit a").withText('gallery.png'));
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test/editor");
    DTE.waitForLoader();
    I.jstreeClick('apps');
    I.jstreeClick(autoName);
    const numVisible = await I.grabNumberOfVisibleElements(copyNameSelector);
    if (numVisible) {
        I.click(DT.btn.tree_delete_button);
        DTE.waitForModal('galleryDimensionDatatable_modal');
        DTE.save();
    }
});

Scenario('editor check instance image change', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test-vela-foto");

    //
    editImage("dsc04077.jpeg", true, I, DTE);

    //
    editImage("dsc04080.jpeg", false, I, DTE);

    //
    editImage("dsc04082.jpeg", true, I, DTE);

    //
    I.say("Change folder");
    I.jstreeClick("user");
    I.waitForText("stevensegal.jpg", 10, "#galleryTable");
    editImage("stevensegal.jpg", true, I, DTE);
    //there was bug that the file name was not changed and saved as previous file
    I.dontSee("dsc04082", "#galleryTable");

    //
    I.say("recheck");
    I.dontSeeElement(locate("#toast-container-upload div.toast-message span").withText("dsc04080.jpeg"));
});

Scenario('BUG set gallery dimmension by not saved/white parent #56393-10', ({ I, DT, DTE }) => {
    //There was a bug when the parent was not saved (white) and you would like to create new gallery dimension default values was not set correctly
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/apps/blog/");
    //apps is saved - has 200x200 dimension
    //blog is not saved, it's white

    I.waitForElement(locate("a.jstree-anchor.jstree-clicked").withText("blog"));
    I.seeElement(locate("a.jstree-anchor.jstree-clicked").withText("blog").find("i.jstree-icon.ti.ti-folder"));

    I.click(".tree-col button.buttons-create");
    DTE.waitForEditor("galleryDimensionDatatable");

    I.seeInField("#DTE_Field_path", "/images/gallery/apps/blog");
    I.clickCss("#pills-dt-galleryDimensionDatatable-sizes-tab");
    I.seeInField("#DTE_Field_imageWidth", "200");
    I.seeInField("#DTE_Field_imageHeight", "200");

    I.seeInField("#DTE_Field_normalWidth", "400");
    I.seeInField("#DTE_Field_normalHeight", "400");

    DTE.cancel();
});

Scenario("BUG - filter by URL and imageName", async ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/#dt-filter-imageName=chrysanthemum.jpg");
    DT.waitForLoader();
    I.waitForText("chrysanthemum.jpg", 10, "table.datatableInit td.dt-row-edit");
    I.wait(2);
    const numVisible = await I.grabNumberOfVisibleElements(locate("#galleryTable td.dt-row-edit").withText("chrysanthemum.jpg"));
    I.assertEqual(numVisible, 1);
});

const editorTestImage = 'editorTestImage';
const editorTestImageType = '.jpeg';
Scenario('Editor test crop', async ({ I, DT, DTE, Document }) => {
    const testFileNameCrop = autoName + 'crop';

    duplicateImage(I, DT, DTE, testFileNameCrop);

    const ratios = ['9:16', '2:1', '3:4', '5:3']; //1:1 failed tests because screenshoted image has 428x428px instead of 427x427px
    Document.setConfigValue('imageEditorRatio', ratios.join(', '));

    I.say(`Editing image ${testFileNameCrop}`);
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test/editor");
    I.waitForVisible(locate("td.dt-row-edit a").withText(testFileNameCrop + editorTestImageType), 10);

    I.say(`Opening editor for ${testFileNameCrop}`);
    openEditor(I, DTE, testFileNameCrop, editorTestImageType);
    I.clickCss('.tie-btn-crop.tui-image-editor-item.normal');
    I.waitForElement('.tui-image-editor-button.preset label', 10);

    I.say('Verifying available DEFAULT crop ratios');
        I.see('Vlastné', '.tui-image-editor-button.preset label');
        I.see('Štvorec', '.tui-image-editor-button.preset label');
    I.say('Verifying available GENERATED crop ratios');
    for (const ratio of ratios) {
        I.see(ratio, '.tui-image-editor-button.preset label');
    }

    I.say('Selecting a random crop ratio');
    const randomIndex = Math.floor(Math.random() * ratios.length);
    const randomRatio = ratios[randomIndex];
    I.say("selected ratio: "+randomRatio);
    I.waitForElement('.tui-image-editor-button.preset label', 10);
    I.clickCss(`.tui-image-editor-button.preset.preset-${randomRatio.replace(':', '-')}`);
    I.wait(1);
    I.clickCss('.tui-image-editor-menu-crop .tie-crop-button.action > div.tui-image-editor-button.apply.active');
    I.wait(1);

    I.say('Verifying cropped image dimensions match the selected ratio');
    const width = await I.grabAttributeFrom('.upper-canvas', 'width');
    const height = await I.grabAttributeFrom('.upper-canvas', 'height');
    isMatchingRatio(I, width, height, randomRatio);
    DTE.save();

    //wait for upload
    I.waitForVisible('#upload-wrapper', 25);
    I.waitForVisible('#upload-wrapper .ti.ti-circle-check.float-end', 25);

    I.say('Verifying the file system for the cropped image');
    I.wait(3);
    I.amOnPage(`/images/gallery/test/editor/${testFileNameCrop}${editorTestImageType}?v=${Date.now()}`);
    const fileBaseName = `${editorTestImage}-${randomRatio.replace(':', ';')}.png`;
    await Document.compareScreenshotElement('body > img', fileBaseName, null, null, 20);
});

Scenario('Editor test resize lockAspectRation FALSE', async ({ I, DT, DTE, Document }) => {
    const testFileNameResize = autoName + 'resize-false';

    duplicateImage(I, DT, DTE, testFileNameResize);

    openEditor(I, DTE, testFileNameResize, editorTestImageType);

    I.clickCss('.tie-btn-resize.tui-image-editor-item.normal');
    I.seeCheckboxIsChecked('.tie-lock-aspect-ratio');

    await testDimension(I, DTE, Document, testFileNameResize, false);
});

Scenario('Editor test resize lockAspectRation TRUE', async ({ I, DT, DTE, Document }) => {
    const testFileNameResize = autoName + 'resize-true';

    duplicateImage(I, DT, DTE, testFileNameResize);

    openEditor(I, DTE, testFileNameResize, editorTestImageType);

    I.clickCss('.tie-btn-resize.tui-image-editor-item.normal');
    I.seeCheckboxIsChecked('.tie-lock-aspect-ratio');

    await testDimension(I, DTE, Document, testFileNameResize, true);

    I.say('Changing image editor size templates');
    const changedSizes = ['160x160', '320x240', '1024x768'];
    Document.setConfigValue('imageEditorSizeTemplates', changedSizes.join(';') + ';');

    I.say('Opening editor again after changing size templates');
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test/editor");
    openEditor(I, DTE, testFileNameResize, editorTestImageType);
    I.clickCss('.tie-btn-resize.tui-image-editor-item.normal');
    I.uncheckOption('.tie-lock-aspect-ratio');
    I.fillField('.tie-width-range-value', '11');
    I.wait(2);

    I.say('Verifying that the preset dimension is none after changing width manually');
    const selectedOption = await I.grabValueFrom('#presetDimensionSelect');
    I.assertEqual(selectedOption, 'none');

    I.say('Verifying that size templates are updated');
    const changedOptions = (await I.grabTextFromAll('#presetDimensionSelect option')).filter(option => option !== 'Vyberte');
    arraysAreEqual(I, changedSizes, changedOptions);
});

Scenario('Default Gallery Style Setting Test - prettyPhoto', async ({Apps, Document, DTE, I }) => {
    Document.setConfigValue('galleryDefaultStyle', 'prettyPhoto');
    Apps.insertApp('Fotogaléria', '#components-gallery-title');
    await Apps.assertParams({style: 'prettyPhoto'});

    Apps.insertApp('Fotogaléria', '#components-gallery-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');
    DTE.waitForModal('component-datatable_modal');
    DTE.selectOption('style', 'Photo Swipe - galéria pre mobilné zariadenia');
    clickOkButton(I);
    await Apps.assertParams({style: 'photoSwipe'});
});

Scenario('Revert and test default option - photoSwipe', async ({Apps, Document, I }) => {
    Document.setConfigValue('galleryDefaultStyle', 'photoSwipe');
    Apps.insertApp('Fotogaléria', '#components-gallery-title');
    await Apps.assertParams({style: 'photoSwipe'});
});

Scenario('Clear testing page for next Scenario', ({ DTE, Apps }) => {
    Apps.clearPageContent(112886);
    DTE.save();
});

Scenario('Input updates gallery path parameter', async ({ I, Apps, DTE }) => {
    const paths = ['/images/gallery/test-vela-foto', '/images/gallery/test/editor'];
    Apps.insertApp('Fotogaléria', '#components-gallery-title', 112886, false);
    I.waitForElement(".cke_dialog_ui_iframe");
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');
    DTE.waitForModal('component-datatable_modal');

    await updateAndVerifyGalleryPath(I, DTE, paths[0]);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=112886');
    Apps.openAppEditor();
    await updateAndVerifyGalleryPath(I, DTE, paths[1]);
});

async function updateAndVerifyGalleryPath(I, DTE, path) {
    I.say("updateAndVerifyGalleryPath");
    I.waitForVisible("#editorAppDTE_Field_dir input");

    //Dont know why, but without waiting, input is not filled somethimes even if it is visible
    I.wait(1);

    I.fillField('#editorAppDTE_Field_dir input', path);
    clickOkButton(I);
    DTE.save();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=112886');
    DTE.waitForEditor();
    I.switchTo(".cke_wysiwyg_frame.cke_reset");
    I.openNewTab();
    I.amOnPage('/test-stavov/test-input-updates-gallery-path-parameter.html');
    I.waitForElement(`img[src*="${path}/s"]`, 30);
    I.switchToPreviousTab();
    I.closeOtherTabs();
}

async function clickOkButton(I) {
    I.switchTo();
    I.click('.cke_dialog_ui_button_ok');
}

function duplicateImage(I, DT, DTE, newImageName) {
    I.openNewTab();
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test/editor");
    I.closeOtherTabs();

    I.say('Navigating to the gallery editor page');
    I.clickCss("button.btn-gallery-size-table");
    DT.waitForLoader();
    DT.filterEquals("imageName", editorTestImage+editorTestImageType);
    I.clickCss("button.btn-gallery-size-s");
    DT.waitForLoader();

    I.say('Duplicating image ' + editorTestImage+editorTestImageType);
    I.click(locate("tr").withDescendant(locate("td.dt-row-edit a").withText(editorTestImage+editorTestImageType)));

    DT.clearFilter("imageName");

    I.click(DT.btn.gallery_duplicate_button);
    DTE.waitForLoader();
    I.fillField('#DTE_Field_descriptionShortSk', newImageName);
    DTE.save();

    I.say("Wait foe new image to show");
    I.waitForVisible(locate("td.dt-row-edit a").withText(newImageName + editorTestImageType), 10);
}

async function testDimension(I, DTE, Document, fileName, isAspectRationLocked = true) {
    I.say("TEST with lock aspect ration locked : " + isAspectRationLocked);

    if(isAspectRationLocked === true) {
        I.checkOption('.tie-lock-aspect-ratio');
    }  else {
        I.uncheckOption('.tie-lock-aspect-ratio');
    }

    I.say('Grabbing available resize options');
    const options = (await I.grabTextFromAll('#presetDimensionSelect option')).filter(option => option !== 'Vyberte');
    let randomIndex = Math.floor(Math.random() * options.length);
    //skip small resolutions
    if (randomIndex<2) randomIndex = 2;
    const randomOption = options[randomIndex];

    I.say(`Selecting random dimension: ${randomOption}`);
    I.selectOption('#presetDimensionSelect', randomOption);

    I.say(`Verifying selected dimensions: ${randomOption}`);

    let [expectedWidth, expectedHeight] = randomOption.split('x').map(Number);
    if(isAspectRationLocked === true) {
        const ratioA = await I.grabValueFrom('.tie-width-range-value');
        const ratioB = await I.grabValueFrom('.tie-height-range-value');
        expectedHeight = Math.round( (expectedWidth * ratioB) / ratioA );
    }

    I.seeInField('.tie-width-range-value', expectedWidth);
    I.seeInField('.tie-height-range-value', expectedHeight);
    I.wait(2);

    I.say('Applying resize changes');
    I.clickCss('.tui-image-editor-button.apply');
    const width = parseInt(await I.grabAttributeFrom('.upper-canvas', 'width'), 10);
    const height = parseInt(await I.grabAttributeFrom('.upper-canvas', 'height'), 10);
    let tolerance = 5;

    I.say('Verifying resized image dimensions');
    I.assertTrue(Math.abs(width - expectedWidth) <= tolerance, `Width is not within the tolerance. Expected: ${expectedWidth}, but got: ${width}`);
    I.assertTrue(Math.abs(height - expectedHeight) <= tolerance, `Height is not within the tolerance. Expected: ${expectedHeight}, but got: ${height}`);
    DTE.save();

    I.say('Waiting before verifying the file system for resized image');
    I.waitForVisible('#upload-wrapper', 25);
    I.waitForVisible('.ti.ti-circle-check.float-end', 25);
    I.amOnPage(`/images/gallery/test/editor/${fileName}${editorTestImageType}`);
    const fileBaseName = `${editorTestImage}-${randomOption}-${isAspectRationLocked}.png`;
    tolerance = 20;
    if (expectedHeight < 100) tolerance = 30;
    await Document.compareScreenshotElement('body > img', fileBaseName, null, null, 20);
}

Scenario('Revert configuration variables', ({ I, Document }) => {
    I.say('Reverting configuration variables');
    Document.setConfigValue('imageEditorRatio', '3:2, 4:3, 5:4, 7:5, 16:9');
    Document.setConfigValue('imageEditorSizeTemplates', '80x80;640x480;800x600;');
});

Scenario('Gallery - image editor delete', async ({I, DT, DTE }) => {
    I.say(`Deleting copies`)
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test/editor");
    DTE.waitForLoader();

    //Select table VIEW
    I.clickCss("button.btn-gallery-size-table");
    DT.waitForLoader();

    DT.filterContains("imagePath", "/images/gallery/test/editor");
    DT.filterContains("imageName", "autotest-");

    let rows = await I.getTotalRows();
    if(rows > 0) {
        I.clickCss("button.dt-filter-id");
        I.click(DT.btn.gallery_delete_button);
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
        I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10);
    }

    I.clickCss("button.btn-gallery-size-s");
});

function openEditor(I, DTE, imageName, imageType) {
    I.say(`Opening editor for image ${imageName}`);
    I.click(locate("td.dt-row-edit a").withText(imageName + imageType));
    DTE.waitForEditor("galleryTable");
    I.clickCss("#pills-dt-galleryTable-photoeditor-tab");
    I.waitForElement("li.tie-btn-crop.tui-image-editor-item", 10);
}

function waitForUpload(I) {
    I.clickIfVisible('.elfinder-confirm-accept');
    I.waitForVisible('.elfinder-notify-chunkmerge', 10);
    I.waitForInvisible('.elfinder-notify-upload', 10);
    I.waitForInvisible('.elfinder-notify-chunkmerge', 10);
};

const isMatchingRatio = (I, width, height, ratio) => {
    const [w, h] = ratio.split(':').map(Number);
    const calculatedRatio = width / height;
    const targetRatio = w / h;
    const tolerance = 0.01;
    I.assertBelow(Math.abs(calculatedRatio - targetRatio), tolerance, `Ratios are different for ${ratio}. Calculated: ${calculatedRatio}, Expected: ${targetRatio}`);
};

const arraysAreEqual = (I, arr1, arr2) => {
    I.say(`Comparing if ${arr1} === ${arr2}`)
    const sortedArr1 = [...arr1].sort();
    const sortedArr2 = [...arr2].sort();
    assert.deepStrictEqual(sortedArr1, sortedArr2, 'Arrays are not equal');
}

Scenario('BUG - buttons-create disabled #56393-17 @singlethread', async ({ I, DTE }) => {
    I.relogin("jtester");
    I.amOnPage("/admin/v9/apps/gallery/");
    I.waitForElement(".tree-col .dt-buttons button.buttons-create.disabled");

    I.jstreeClick("test");
    I.waitForElement(".tree-col .dt-buttons button.buttons-create:not(.disabled)");

    I.click(".tree-col button.buttons-create");
    DTE.waitForEditor("galleryDimensionDatatable");
    I.seeInField("#DTE_Field_path", "/images/gallery/test");
    DTE.cancel();
});

Scenario('logout', ({ I }) => {
    I.logout();
});

Scenario('Editovanie obrazka - nezobrazovat upload bez zmeny v obrazku', ({ I, DT, DTE }) => {
    var nameOfImage = 'koala.jpg';
    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();

    I.jstreeWaitForLoader();
    const selector = ".//*[@id='SomStromcek']//a[contains(@class, 'jstree-anchor') and text() = 'test']";
    I.click(selector);
    DT.waitForLoader();

    // otvor ten isty obrazok a bez uprav zatvor editor
    I.say('Otvor ten isty obrazok a bez uprav zatvor editor');
    I.seeAndClick(nameOfImage);
    I.waitForVisible('.DTE_Header.modal-header', 15);
    I.waitForVisible('#pills-dt-galleryTable-photoeditor-tab', 5);
    I.clickCss('#pills-dt-galleryTable-photoeditor-tab');
    I.waitForVisible('#photoEditorContainer', 10);
    DTE.save();

    // po ulozeni over ci sa modalne okno s uploadom suboru nezobrazilo
    I.say('Po ulozeni over ci sa modalne okno s uploadom suboru nezobrazilo');
    I.dontSeeElement('#upload-wrapper');
    I.dontSeeElement(locate('#toast-container-upload').withText(nameOfImage));
});

Scenario('Feature - test that field imageNAme is now required', ({I,DT, DTE}) => {
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test/editor");

    //There can be different view of the gallery
    I.clickCss("button.btn-gallery-size-s");
    DT.waitForLoader();

    I.waitForText("turtle.png", 10, "#galleryTable");
    I.click(locate("td.dt-row-edit a").withText('turtle.png'));
    DTE.waitForEditor("galleryTable");

    I.clickCss("#pills-dt-galleryTable-metadata-tab");
    I.waitForVisible("#DTE_Field_imageName", 10);
    I.fillField("#DTE_Field_imageName", "");
    DTE.save();

    I.see("Povinné pole. Zadajte aspoň jeden znak.");
});

Scenario('Gallery - filtering', ({ I }) => {
    I.amOnPage('/admin/v9/apps/gallery/');
    I.waitForElement(locate("a.jstree-anchor").withText("test-vela-foto"), 10);

    I.say('1. I can filter the folder by name.');
    I.jstreeFilter('locnost');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('o-spolocnosti'));
    I.seeElement(locate('.jstree-anchor').withText('gallery'));

    I.jstreeFilter('spolocnosti', 'Začína na');
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText('o-spolocnosti'));
    I.dontSeeElement(locate('.jstree-anchor').withText('gallery'));

    I.jstreeFilter('edi', 'Začína na');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('editor'));
    I.seeElement(locate('.jstree-anchor').withText('gallery'));
    I.seeElement(locate('.jstree-anchor').withText('test'));

    I.jstreeFilter('level', 'Končí na');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('second-level'));
    I.seeElement(locate('.jstree-anchor').withText('gallery'));
    I.seeElement(locate('.jstree-anchor').withText('test'));

    I.jstreeFilter('second-level', 'Rovná sa');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('second-level'));
    I.seeElement(locate('.jstree-anchor').withText('gallery'));
    I.seeElement(locate('.jstree-anchor').withText('test'));

    I.say('2. All filtering is cleared');
    I.jstreeFilter('');
    I.dontSeeInField('#tree-folder-search-input', 'second-level');
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText('second-level'));
    I.seeElement(locate('.jstree-anchor').withText('gallery'));
    I.seeElement(locate('.jstree-anchor').withText('apps'));
    I.seeElement(locate('.jstree-anchor').withText('test'));
    I.seeElement(locate('.jstree-anchor').withText('test-vela-foto'));
    I.seeElement(locate('.jstree-anchor').withText('user'));

    I.jstreeFilter('test-vela-foto');
    I.seeElement(locate('.jstree-anchor').withText('test-vela-foto'));
    I.clickCss('#tree-folder-search-clear-button');
    I.waitForInvisible("div.dt-processing", 40);
    I.dontSeeInField('#tree-folder-search-input', 'test-vela-foto');
    I.seeElement(locate('.jstree-node.jstree-open').withText("test-vela-foto"));
    I.seeElement(locate('.jstree-anchor').withText('gallery'));
    I.seeElement(locate('.jstree-anchor').withText('apps'));
    I.seeElement(locate('.jstree-anchor').withText('test'));
    I.seeElement(locate('.jstree-anchor').withText('test-vela-foto'));
    I.seeElement(locate('.jstree-anchor').withText('user'));
});