Feature('apps.gallery.gallery-bugs');

var randomNumber;
var autoName;

Before(({ I, DT, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        autoName = 'autotest-' + randomNumber;
    }
});


Scenario('Test filter bug', async ({ I, DT, DTE, Apps}) =>  {
    // tetsing on new page (not saved) there this bug has occured

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=39007");
    DT.waitForLoader();

    Apps.insertApp('Fotogaléria', '#components-gallery-title' , null, false);

    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.say("Check i see field dir and tab componentIframe");
    I.seeElement("div.DTE_Field_Name_dir");
    I.seeElement("a#pills-dt-component-datatable-componentIframe-tab");

    I.say("Check that appHideFields is not visible");
    I.clickCss("a#pills-dt-component-datatable-commonSettings-tab");
    I.dontSeeElement("div.DTE_Field_Name_appHideFields");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    I.say("Now set appHideFields")
    Apps.switchEditor('html');
    const body = "!INCLUDE(/components/gallery/gallery.jsp, style=photoSwipe, dir=&quot;/images/gallery&quot;, recursive=false, itemsOnPage=10, orderBy=title, orderDirection=asc, thumbsShortDescription=true, shortDescription=true, longDescription=true, author=true, perexGroup=, appHideFields=dir+tab_componentIframe)!";
    await DTE.fillCkeditor(body);
    Apps.switchEditor('standard');

    I.say("Open app and check that field and tab are hidden");
    Apps.openAppEditor();

    I.dontSeeElement("div.DTE_Field_Name_dir");
    I.dontSeeElement("a#pills-dt-component-datatable-componentIframe-tab");

    I.say('Check that appHideFields is still hidden after saving');
    I.clickCss("a#pills-dt-component-datatable-commonSettings-tab");
    I.dontSeeElement("div.DTE_Field_Name_appHideFields");
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

    var position = 3;
    I.dragAndDrop(
        "#galleryTable_wrapper div.dt-scroll-headInner th.dt-th-descriptionShortCz",
        "#galleryTable_wrapper div.dt-scroll-headInner th.dt-th-imageName",
        {
            force: true,
            sourcePosition: {x: 10, y: 10},
            targetPosition: { x: 10, y: 10 }
        }
    );
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

Scenario('BUG - buttons-create disabled #56393-17 @singlethread', async ({ I, DTE, DT }) => {
    I.relogin("jtester");
    I.amOnPage("/admin/v9/apps/gallery/");
    I.waitForElement(".tree-col .dt-buttons button.buttons-create.disabled");

    I.jstreeClick("test");
    I.waitForElement(".tree-col .dt-buttons button.buttons-create:not(.disabled)");

    I.click(".tree-col button.buttons-create");
    DTE.waitForEditor("galleryDimensionDatatable");
    I.seeInField("#DTE_Field_path", "/images/gallery/test");
    DTE.cancel();

    //
    I.say("Verify server side disable create buttons");
    I.jstreeClick('gallery');
    await I.executeScript(() => {
        document.querySelector('.tree-col .buttons-create').classList.remove('disabled');
        document.querySelector('.tree-col .buttons-create').removeAttribute('disabled');
    });

    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor('galleryDimensionDatatable');
    DTE.fillField('name', autoName);
    DTE.save('galleryDimensionDatatable');
    I.waitForText('Tento priečinok nie je možné upravovať.');
    I.see('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).');
});

Scenario('Revert if last scenario fails', async ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/gallery/");
    const autotest = locate('.jstree-anchor').withText('autotest-');
    while (await I.grabNumberOfVisibleElements(autotest)){
        I.click(autotest);
        I.click(DT.btn.tree_delete_button);
        await I.waitForElement("div.DTE_Action_Remove", 5);
        I.click("Zmazať", "div.DTE_Action_Remove");
    }
});

Scenario('logout', ({ I }) => {
    I.logout();
});