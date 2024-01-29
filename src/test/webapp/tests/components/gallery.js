Feature('components.gallery');

Before(({ login }) => {
    login('admin');
});

Scenario('zoznam fotografii', ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();
    I.click("test", "#SomStromcek");
    I.see("koala.jpg");
});

Scenario('zoznam fotografii-pamatanie velkosti', ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();
    I.jstreeClick("test-vela-foto");

    I.click("button.btn-gallery-size-l");
    I.see("Záznamy 1 až 4 z", "div.dt-footer-row");

    //reloadni stranku
    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();
    I.jstreeClick("test-vela-foto");
    DT.waitForLoader();
    I.see("Záznamy 1 až 4 z", "div.dt-footer-row");
    I.seeElement("button.btn-gallery-size-l.active");

    //prepni na default
    I.click("button.btn-gallery-size-s");
    I.see("Záznamy 1 až 12 z", "div.dt-footer-row");
});

Scenario('oblast zaujmu', async ({ I, DT, DTE }) => {
    const assert = require('assert');
    const generateRandomNum = num => Math.floor(Math.random() * num);
    let area = { w: null, h: null, x: null, y: null };
    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();
    I.click("test", "#SomStromcek");
    DT.waitForLoader();
    I.seeAndClick("koala.jpg");
    I.seeAndClick("Oblasť záujmu");

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

    I.seeAndClick("Oblasť záujmu");
    I.wait(2);
    let inputValueW = await I.grabAttributeFrom('#w', 'value');
    let inputValueH = await I.grabAttributeFrom('#h', 'value');
    let inputValueX = await I.grabAttributeFrom('#x', 'value');
    let inputValueY = await I.grabAttributeFrom('#y', 'value');
    assert.equal(+inputValueH, +area.h);
    assert.equal(+inputValueW, +area.w);
    assert.equal(+inputValueX, +area.x);
    assert.equal(+inputValueY, +area.y);

    //otvor iny obrazok a znova over
    DTE.cancel();
    I.seeAndClick("lighthouse.jpg");
    DTE.waitForEditor("galleryTable");
    I.seeAndClick("Oblasť záujmu");
    I.wait(2);
    DTE.cancel();

    I.seeAndClick("koala.jpg");
    DTE.waitForEditor("galleryTable");

    I.seeAndClick("Oblasť záujmu");
    I.wait(2);
    inputValueW = await I.grabAttributeFrom('#w', 'value');
    inputValueH = await I.grabAttributeFrom('#h', 'value');
    inputValueX = await I.grabAttributeFrom('#x', 'value');
    inputValueY = await I.grabAttributeFrom('#y', 'value');
    assert.equal(+inputValueH, +area.h);
    assert.equal(+inputValueW, +area.w);
    assert.equal(+inputValueX, +area.x);
    assert.equal(+inputValueY, +area.y);

    //obnov stranku a over znova
    I.refreshPage();
    DT.waitForLoader();
    I.waitForText("test", 10, "#SomStromcek");
    I.wait(2);
    I.click("test", "#SomStromcek");
    DT.waitForLoader();
    I.seeAndClick("koala.jpg");
    I.seeAndClick("Oblasť záujmu");
    I.wait(1);
    inputValueW = await I.grabAttributeFrom('#w', 'value');
    inputValueH = await I.grabAttributeFrom('#h', 'value');
    inputValueX = await I.grabAttributeFrom('#x', 'value');
    inputValueY = await I.grabAttributeFrom('#y', 'value');
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

Scenario('otvorenie galerie s URL parametrom', async({ I }) => {
    I.say("Testujem presmerovanie");
    await I.executeScript(function() {
        window.location.href="/admin/photogallery.do?dir=/images/gallery/user/";
    });
    I.seeInCurrentUrl("/admin/v9/apps/gallery/?dir=/images/gallery/user/");

    I.say("Testujem zobrazenie stromovej struktury a obrazku");
    I.waitForElement(locate("a.jstree-anchor.jstree-clicked").withText("user"), 10);
    I.seeElement(locate("a.jstree-anchor.jstree-clicked").withText("user"));
    I.waitForText("demo.jpg", 10, "table.datatableInit td.dt-row-edit");
    I.see("demo.jpg", "table.datatableInit td.dt-row-edit");
});

Scenario('multidomain zobrazenie', async({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();
    I.jstreeClick("test");
    I.see("koala.jpg");
    I.dontSee("dsc04082.jpeg");

    // prepnutie na domenu mirroring.tau27.iway.sk
    I.say("Prepinam domenu");
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("mirroring.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");

    I.wait(2);
    DT.waitForLoader();

    I.jstreeClick("test");
    I.dontSee("koala.jpg");
    I.see("dsc04082.jpeg");
});

Scenario('odhlasenie', ({ I }) => {
    I.logout();
});

Scenario('novy priecinok', async({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/apps/gallery");
    DT.waitForLoader();
    I.click("test", "#SomStromcek");

    //pri vytvoreni noveho priecinka musi byt pole pre zadanie nazvu prazdne
    I.click("div.tree-col button.buttons-create");
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
    I.click("button.btn-gallery-size-table");
    DT.resetTable("galleryTable");
    DT.waitForLoader();

    var position = 4;
    if (Browser.isFirefox()) {
        I.dragAndDrop("#galleryTable_wrapper div.dataTables_scrollHeadInner th.dt-th-descriptionShortCz", "#galleryTable_wrapper div.dataTables_scrollHeadInner th.dt-th-imageName", {sourcePosition: {x: 10, y: 10}, targetPosition: { x: 10, y: 10 }});
        position = 5;
    } else {
        I.dragAndDrop("#galleryTable_wrapper div.dataTables_scrollHeadInner th.dt-th-descriptionShortCz", "#galleryTable_wrapper div.dataTables_scrollHeadInner th.dt-th-imagePath");
    }
    I.see("Názov cz", "#galleryTable_wrapper div.dataTables_scrollHeadInner table thead tr th:nth-child("+position+")");

    //
    I.say("Reload and check if the column order is preserved");
    I.amOnPage("/admin/v9/apps/gallery/");
    I.see("Názov cz", "#galleryTable_wrapper div.dataTables_scrollHeadInner table thead tr th:nth-child("+position+")");

    //
    I.say("check column settings-there was bug with duplicate buttons on header and footer");

    var container = "#galleryTable_wrapper";
    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.see("Priečinok", "div.colvisbtn_wrapper button.buttons-columnVisibility span.column-title");

});

Scenario('bug-remember column order-reset', ({ I, DT }) => {
    I.wjSetDefaultWindowSize();

    I.amOnPage("/admin/v9/apps/gallery/");

    I.click("button.btn-gallery-size-s");
    DT.resetTable("galleryTable");
});

Scenario('editor check dir select', ({ I, Document, DTE, Browser }) => {

    I.closeOtherTabs();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=45926");
    DTE.waitForEditor();
    I.wait(5);
    Document.editorComponentOpen();
    if (Browser.isFirefox()) I.wait(1);
    I.waitForElement("#Tabs li.last.openLast");
    I.wait(0.5);
    I.click("#Tabs li.first a");
    I.wait(0.5);
    I.seeInField("#dir", "/images/gallery/test-vela-foto");
    I.forceClick("span.input-group-addon.btn.green");

    //it's open in new window
    I.switchToNextTab();
    I.see("/images/", "div.webfx-tree-item");
    I.click(locate("div.webfx-tree-item.folder").withText("gallery").find({css: "img:first-child"}));
    I.waitForText("test-vela-foto", 10, "div.webfx-tree-item.folder a");
    if (Browser.isFirefox()) I.wait(1);
    I.click(locate("div.webfx-tree-item.folder a").withText("user"));
    I.wait(1);
    I.openNewTab();
    I.closeCurrentTab();

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    I.seeInField("#dir", "/images/gallery/user");

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
    I.click("#pills-dt-galleryTable-photoeditor-tab");
    I.waitForElement("li.tie-btn-crop.tui-image-editor-item", 10);

    if (save) {
        I.forceClickCss("li.tie-btn-flip.tui-image-editor-item");
        I.wait(0.5);
        I.forceClickCss("div.tui-image-editor-button.flipX");
        I.wait(0.5);
        I.forceClickCss("div.tui-image-editor-button.flipX");
        I.wait(0.5);
        DTE.save("galleryTable");
        I.waitForElement("#toast-container-upload", 10);
        I.waitForElement(locate("#toast-container-upload div.toast-message span").withText(name), 10);
        I.waitForElement("#toast-container-upload i.fa-spin", 10);
        I.waitForInvisible("#toast-container-upload i.fa-spin", 10);
        I.waitForVisible(locate("#toast-container-upload div.toast-message").withText(name).find("i.fa-check-circle"), 10);
        I.wait(0.5);
        I.waitForVisible(locate("#toast-container-upload div.toast-message").withText(name).find("i.fa-check-circle"), 10);
    } else {
        DTE.cancel("galleryTable");
        I.waitForElement("#toast-container-upload", 10);
        I.wait(3);
        I.dontSeeElement(locate("#toast-container-upload div.toast-message span").withText(name));
    }
}

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