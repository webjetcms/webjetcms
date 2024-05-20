Feature('webpages.media');

var editorContainer = "#datatableFieldDTE_Field_editorFields-media_modal";
var randomText;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomText=="undefined") {
        randomText = I.getRandomText();
    }
});

function mediaEditSteps(I, DTE, options, isFromWebpage=false) {
    //over, ze ked zmazem hodnoty a nastavim linku, tak sa nastavi aj textova hodnota
    let mediaTitle = options.testingData[0];

    DTE.fillField("mediaTitleSk", "");
    I.wait(1);
    I.forceClickCss("div.DTE_Field_Name_mediaLink button.btn-outline-secondary");
    I.wait(1);
    I.waitForElement("#modalIframeIframeElement", 20);
    I.wait(3);
    I.switchTo("#modalIframeIframeElement");

        I.waitForElement("div.elfinder-cwd-wrapper", 10);
        I.waitForInvisible("#WJLoaderDiv", 10);
        I.wait(1);

        if (isFromWebpage) {
            //otvor Media vsetkych stranok
            I.click("#nav-iwcm_1_");
        }

        //Obrazky
        I.clickCss("#nav-iwcm_1_L2ltYWdlcw_E_E");
        I.waitForInvisible("#WJLoaderDiv", 10);
        I.wait(1);

        //bannery
        I.clickCss("#nav-iwcm_1_L2ltYWdlcy9iYW5uZXJ5");
        I.waitForInvisible("#WJLoaderDiv", 10);
        I.wait(1);

        //banner-investicie.jpg
        I.clickCss("#iwcm_1_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pbnZlc3RpY2llLmpwZw_E_E");
        I.wait(1);

    I.switchTo();
    I.click({css: "#modalIframe div.modal-footer button"});
    I.wait(1);

    //aby sa nam presunul fokus
    I.forceClickCss('#DTE_Field_editorFields-groups_1');

    //over, ze sa nam prenieslo meno obrazka do title
    I.seeInField("#DTE_Field_mediaTitleSk", "banner investicie");

    //vrat povodny nazov, aby nam bezal test korektne dalej
    DTE.fillField("mediaTitleSk", mediaTitle);
}

Scenario('media-zakladne testy', async ({I, DTE, DataTables}) => {
    I.amOnPage("/admin/v9/webpages/media/");
    await DataTables.baseTest({
        dataTable: 'mediaTable',
        perms: 'editor_edit_media_all',
        createSteps: function(I, options) {
            //DTE.fillField("mediaSortOrder", 50);
            I.forceClickCss('#DTE_Field_editorFields-groups_0');

            I.clickCss("#editorAppDTE_Field_editorFields-docDetails button.btn-vue-jstree-item-edit");
            I.click(locate('.jstree-node.jstree-closed').withText('test').find('.jstree-icon.jstree-ocl'));
            I.click('test_final');
        },
        editSteps: function(I, options) {
            mediaEditSteps(I, DTE, options);
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        }
    });
});

Scenario('media tabulka v stranke', async ({I, DataTables, DT, DTE, Browser}) => {
    if (Browser.isFirefox()) {
        //FF nevie tento test spravit, padne mu to vo funkcii mediaEditSteps na nezmysloch (necaka na dokoncenie akcie)
        //nic nepomaha, asi nejaky bug v Playwright
        I.say("Firefox, skipping test");
        return;
    }

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    DT.waitForLoader();

    I.say("Otvaram test stavov");
    I.jstreeClick("Test stavov");
    //malo to bug, ze ked sa najskor zobrazila jedna stranka a potom druha, tak to ukladalo k tej prvo zobrazenej
    I.jstreeClick("Tento nie je interný");
    I.click("Tento nie je interný", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    DTE.cancel();

    I.say("Otvaram zobrazeny v menu");
    I.jstreeClick("Zobrazený v menu");
    I.click("Zobrazený v menu", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    //over zobrazenie, ze vidis len media pre tuto web stranku
    var container = "#datatableFieldDTE_Field_editorFields-media";
    I.see("www.sme.sk", container);
    I.dontSee("general_video.mp4", container);

    //spusti basetest - datatableInnerTable sa nastavuje ako window objekt vo field-type-datatable.js
    await DataTables.baseTest({
        dataTable: 'datatableInnerTable_editorFields_media',
        container: 'div.DTE_Field_Type_datatable',
        containerModal: '#datatableFieldDTE_Field_editorFields-media_modal',
        perms: '-',
        skipRefresh: true,
        editSteps: function(I, options) {
            mediaEditSteps(I, DTE, options, true);
        },
        skipSwitchDomain: true
    });
});

Scenario('media tabulka v NOVEJ stranke @singlethread', async ({I, DT, DTE}) => {

    const random = I.getRandomText();
    const newMediaName = "media-" + random;
    const newPageName = "page-" + random;

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeClick("Test stavov");
    I.jstreeClick("Zobrazený v menu");

    //open editor to add new webPage
    I.clickCss("#datatableInit_wrapper button.buttons-create");
    DTE.waitForEditor();

    //go to basic tab add fill web page title
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    DTE.fillField("title", newPageName);

    //go to media tab check that there aren't any data
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    //click to add media button and fill media name
    I.clickCss("#datatableFieldDTE_Field_editorFields-media_wrapper button.buttons-create");
    DTE.waitForEditor();
    DTE.fillField("mediaTitleSk", newMediaName);

    //click to save media button
    I.clickCss("#datatableFieldDTE_Field_editorFields-media_modal button.btn-primary");
    DTE.waitForLoader();

    //click to save webPage button
    I.clickCss("#datatableInit_modal button.btn-primary");
    DTE.waitForLoader();

    I.refreshPage();
    I.jstreeClick("Test stavov");
    I.jstreeClick("Zobrazený v menu");
    DT.waitForLoader();

    //open our new webPage, go to media tab and check if there is our new added media, than close editor
    I.click(newPageName);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab")
    I.see(newMediaName);
    I.click("Zrušiť", {css: "div.DTE_Form_Buttons"});

    //filter out our new web page, select this page and detele
    DT.filter("title", newPageName);
    I.clickCss("#datatableInit_wrapper .buttons-select-all");
    I.clickCss(".buttons-remove", '#datatableInit_wrapper .dt-buttons');
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();
    DT.filter("title", "");
    I.wait(1);
    I.dontSee(newPageName);
});

Scenario('kontrola menu poloziek', ({I}) => {
    I.amOnPage("/admin/v9/webpages/media");
    I.see("Média", "div.menu-wrapper");

    I.amOnPage("/admin/v9/webpages/media#");
    I.see("Média", "div.menu-wrapper");

    I.amOnPage("/admin/v9/webpages/media/");
    I.see("Média", "div.menu-wrapper");

    I.amOnPage("/admin/v9/webpages/media/#/");
    I.see("Média", "div.menu-wrapper");
});

function testMediaGroups(I, DTE, editorContainer, shouldSee=true) {
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.wait(1);
    I.clickCss("button.buttons-create", "div.datatableFieldType");
    I.waitForElement(editorContainer);
    I.see("Media Skupina 1", editorContainer);
    I.dontSee("Iba JetPortal4", editorContainer);
    if (shouldSee) I.see("English a Newsletter skupina", editorContainer);
    else I.dontSee("English a Newsletter skupina", editorContainer);
    I.clickCss("button.btn-close-editor", editorContainer);
    I.clickCss("div.DTE_Footer button.btn-close-editor", "#datatableInit_modal");
}

Scenario('overenie filtrovania media skupin podla adresara', ({I, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.say("Before navigate");
    I.jstreeNavigate(["Jet portal 4", "Kontakt"]);
    I.say("After navigate");
    I.click("Kontakt", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.wait(1);
    I.clickCss("button.buttons-create", "div.datatableFieldType");
    I.waitForElement(editorContainer);
    I.see("Media Skupina 1", editorContainer);
    I.see("Iba JetPortal4", editorContainer); //this is important, should be only on this page
    I.dontSee("English a Newsletter skupina", editorContainer);
    I.clickCss("button.btn-close-editor", editorContainer);
    I.clickCss("div.DTE_Footer button.btn-close-editor", "#datatableInit_modal");

    I.jstreeNavigate(["English", "Contact"]);
    I.click("Contact", "#datatableInit_wrapper");
    testMediaGroups(I, DTE, editorContainer);

    I.jstreeNavigate(["test", "Podadresar 1", "test_url_inherit"]);
    I.click("test_url_inherit", "#datatableInit_wrapper");
    testMediaGroups(I, DTE, editorContainer);

    I.jstreeNavigate(["test", "Podadresar 1"]);
    I.click("Podadresar 1", "#datatableInit_wrapper");
    testMediaGroups(I, DTE, editorContainer, false);
});

Scenario('XSS zranitelnost', ({I, DT, DTE}) => {
    var xss1 = "<img src=x onerror=alert(2)>";
    var xss2 =  "x\" onerror=alert(document.domain)>\"";

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Jet portal 4", "Kontakt"]);
    I.click("Kontakt", "#datatableInit_wrapper");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.wait(1);
    I.clickCss("button.buttons-create", "div.datatableFieldType");
    I.waitForElement(editorContainer, 10);
    I.waitForText("Pridať", 5)
    I.wait(1);

    DTE.fillField("mediaTitleSk", xss1);
    I.fillField({css: "div.DTE_Field_Name_mediaThumbLink input.form-control"}, xss2);

    I.clickCss("div.DTE_nested_modal.show div.DTE_Footer button.btn-primary");

    DTE.waitForLoader();

    I.see(xss1, "#datatableFieldDTE_Field_editorFields-media");
    I.see(xss2, "#datatableFieldDTE_Field_editorFields-media");

    I.say("Overenie v media tabulke");

    I.amOnPage("/admin/v9/webpages/media/");
    DT.filter("editorFields.docDetails", "/Jet portal 4/Kontakt/Kontakt");

    I.see(xss1, "#mediaTable_wrapper");
    I.see(xss2, "#mediaTable_wrapper");

    I.say("Overenie v audite");

    I.amOnPage("/admin/v9/apps/audit-search/");
    DT.filterSelect("logType", "MEDIA");
    DT.filter("description", "^CREATE:");
    DT.waitForLoader();

    I.see(xss1, "#datatableInit_wrapper");
    I.see(xss2, "#datatableInit_wrapper");
});

Scenario('Zobrazenie medii na stranke - vsetky media', ({I}) => {
    I.amOnPage("/apps/media/");
    I.see("Kontakt");

    I.see("Výpis Media skupina 1");
    I.see("desert");
    I.see("hydrangeas");

    I.see("Výpis Iná skupina 2");
    I.see("about");
});

Scenario('Overenie editacie - cez web stranku', ({I, DTE, DT}) => {
    var title = "koala-media-webpage-"+randomText;

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=24008");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.wait(3);

    DT.filter("mediaLink", "/images/gallery/test/koala.jpg");

    I.see("Záznamy 1 až 1 z 1");

    I.clickCss("button.buttons-select-all", "#datatableFieldDTE_Field_editorFields-media_wrapper");
    I.clickCss("button.buttons-edit", "#datatableFieldDTE_Field_editorFields-media_wrapper");

    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");

    DTE.fillField("mediaTitleSk", title);

    I.clickCss("#datatableFieldDTE_Field_editorFields-media_modal div.DTE_Footer.modal-footer button.btn.btn-primary");
    DTE.waitForLoader();
    I.wait(3);

    I.say("Overime, ze media sa nezobrazia kym neulozim stranku");

    I.openNewTab();
    I.amOnPage("/apps/media/");

    I.see("Kontakt");
    I.see("desert");
    I.dontSee(title);

    I.closeCurrentTab();

    DTE.save();

    I.say("Overime, ze media sa zobrazili po neulozeni stranky");

    I.amOnPage("/apps/media/");

    I.see("Kontakt");
    I.see("desert");
    I.see(title);
});

Scenario('Zatvorenie tabu ak padne test', ({I}) => {
    I.switchTo();
    I.closeOtherTabs();
});

Scenario('Overenie editacie cez vsetky media a zobrazenie zmeny hned', ({I, DT, DTE}) => {
    var title = "koala-media-all-"+randomText;

    //ak padne predchadzajuci test nastav datum stranky na aktualny
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=24008");
    DTE.waitForEditor();
    DTE.save();

    I.amOnPage("/admin/v9/webpages/media/");
    DT.filter("mediaLink", "/images/gallery/test/koala.jpg");
    DT.filter("editorFields.docDetails", "24008");

    I.see("Záznamy 1 až 1 z 1");

    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-edit");

    DTE.waitForEditor("mediaTable");

    DTE.fillField("mediaTitleSk", title);
    DTE.save();

    I.say("Overime, ze media sa zobrazili aj bez potreby ulozenia stranky (zachoval sa datum zmeny)");

    I.amOnPage("/apps/media/");

    I.see("Kontakt");
    I.see("desert");
    I.see(title);
});

Scenario('BUG - strankovanie v mediach', ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=259");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-media-tab");
    DT.waitForLoader();
    I.see("Záznamy 1 až 10 z", "#datatableFieldDTE_Field_editorFields-media_wrapper");
    I.see("fasdfasd-final", "#datatableFieldDTE_Field_editorFields-media_wrapper");
    I.see("www.sme.sk", "#datatableFieldDTE_Field_editorFields-media_wrapper");

    I.click(locate("#datatableFieldDTE_Field_editorFields-media_wrapper li.paginate_button.page-item a").withText("2"));
    DT.waitForLoader();
    I.see("Záznamy 11 až", "#datatableFieldDTE_Field_editorFields-media_wrapper");
    I.see("mediaTitleSk-autotest-2022-06-05-230357-94", "#datatableFieldDTE_Field_editorFields-media_wrapper");
    I.dontSee("fasdfasd-final", "#datatableFieldDTE_Field_editorFields-media_wrapper");
    I.dontSee("www.sme.sk", "#datatableFieldDTE_Field_editorFields-media_wrapper");
});

Scenario('media domain filter test', async ({I, Document, DT}) => {
    I.amOnPage("/admin/v9/webpages/media/");

    var searchText = "Domain filter test";

    var webjet9 = searchText + " webjet9";
    var test23 = searchText + " test23";
    var mirroring = searchText + " mirroring";

    //webjet9 domain test
    //Document.switchDomain("demotest.webjetcms.sk");
    DT.filter("mediaTitleSk", searchText);

    I.see(webjet9);
    I.dontSee(test23);
    I.dontSee(mirroring);

    //test23 domain test
    Document.switchDomain("test23.tau27.iway.sk");
    DT.filter("mediaTitleSk", searchText);

    I.see(test23);
    I.dontSee(webjet9);
    I.dontSee(mirroring);

    //mirroring domain test
    Document.switchDomain("mirroring.tau27.iway.sk");
    DT.filter("mediaTitleSk", searchText);

    I.see(mirroring);
    I.dontSee(webjet9);
    I.dontSee(test23);
});

Scenario('logoff', ({I}) => {
    I.logout();
});

Scenario('media required docId tree selector', async ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/media/");

    var entityName = "docTreeSelector_" + randomText;

    I.clickCss("button.buttons-create");
    DTE.waitForEditor('mediaTable');

    DTE.save();
    I.see("Povinné pole. Vyberte stránku.");

    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('.jstree-node.jstree-closed').withText('test').find('.jstree-icon.jstree-ocl'));
    I.click('test_final');
    DTE.save();

    I.see("Povinné pole. Zadajte aspoň jeden znak.");
    I.clickCss("#DTE_Field_mediaTitleSk");
    DTE.fillField("mediaTitleSk", entityName)
    DTE.save();

    I.fillField("input.dt-filter-mediaTitleSk", entityName);
    I.pressKey('Enter', "input.dt-filter-mediaTitleSk");
    I.see(entityName);
    I.see("/test/test_final");

    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(entityName);
});

Scenario('media all - filtering', ({I, DT, DTE}) => {

    I.amOnPage("/admin/v9/webpages/media/");
    I.see("mediaTitleSk-autotest");

    DT.filterSelect("editorFields.groups", "Iná skupina 2");
    I.see("Cenník");
    I.see("jeeff media test 2");
    I.see("about");
    I.dontSee("mediaTitleSk-autotest");

    DT.filter("editorFields.docDetails", "aplikacie");
    I.dontSee("Cenník");
    I.dontSee("jeeff media test 2");
    I.see("about");
    I.dontSee("mediaTitleSk-autotest");

});

Scenario('custom fields in webpage', ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=24008");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.wait(3);

    //
    I.say("Checking filled values");
    I.click(locate("#datatableFieldDTE_Field_editorFields-media td.dt-row-edit a").withText("desert"));
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");

    I.clickCss("#pills-dt-datatableFieldDTE_Field_editorFields-media-fields-tab");
    I.seeInField("#datatableFieldDTE_Field_editorFields-media_modal #DTE_Field_fieldB", "06.09.2023");
    I.seeCheckboxIsChecked("#datatableFieldDTE_Field_editorFields-media_modal #DTE_Field_fieldE");

    DTE.cancel("datatableFieldDTE_Field_editorFields-media", true);

    //
    I.say("Checking empty values");
    I.click(locate("#datatableFieldDTE_Field_editorFields-media td.dt-row-edit a").withText("hydrangeas"));
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");

    I.clickCss("#pills-dt-datatableFieldDTE_Field_editorFields-media-fields-tab");
    I.seeInField("#datatableFieldDTE_Field_editorFields-media_modal #DTE_Field_fieldB", "");
    I.dontSeeCheckboxIsChecked("#datatableFieldDTE_Field_editorFields-media_modal #DTE_Field_fieldE");
});

function createNewMedia(mediaTitle, I, DTE) {
    I.say("Create new media "+mediaTitle);
    //click to add media button and fill media name
    I.clickCss("#datatableFieldDTE_Field_editorFields-media_wrapper button.buttons-create");
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");
    DTE.fillField("mediaTitleSk", mediaTitle);
    I.click("autoTestInsert", "#datatableFieldDTE_Field_editorFields-media_modal");

    //click to save media button
    DTE.save("datatableFieldDTE_Field_editorFields-media");
}

function deleteNewMedia(mediaTitle, I, DT, DTE) {
    DT.filter("mediaTitleSk", mediaTitle);
    I.see("Záznamy 1 až 1 z 1", "#datatableFieldDTE_Field_editorFields-media_wrapper");
    I.clickCss("button.buttons-select-all", "#datatableFieldDTE_Field_editorFields-media_wrapper");
    I.clickCss("button.buttons-remove", '#datatableFieldDTE_Field_editorFields-media_wrapper .dt-buttons');
    I.click("Zmazať", "#datatableFieldDTE_Field_editorFields-media_modal div.DTE_Action_Remove");
    DT.waitForLoader();
}

async function verifyMediaInWebpage(mediaTitles, webpageUrl, I, see=true) {
    I.amOnPage(webpageUrl+"?NO_WJTOOLBAR=true");
    for (var i=0; i<mediaTitles.length; i++) {
        var value = mediaTitles[i];
        I.say("Verify media "+value+" in webpage "+webpageUrl+" see="+see);
        if (see) {
            I.see(value, "article.ly-content div.container ul li");
            var count = await I.grabNumberOfVisibleElements(locate("article.ly-content div.container ul li").withText(value));
            I.assertEqual(count, 1);
        } else {
            I.dontSee(value, "article.ly-content div.container ul li");
        }
    };
}

Scenario('media tabulka v stranke-multigroup', async ({I, DataTables, DT, DTE, Browser}) => {
    var mediaTitleMaster = "autotest-master-" + randomText;
    var mediaTitleSlave = "autotest-slave-" + randomText;
    var container = "#datatableFieldDTE_Field_editorFields-media";

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=70753");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.see("barepage", container);

    createNewMedia(mediaTitleMaster, I, DTE);
    DTE.save();

    await verifyMediaInWebpage([mediaTitleMaster], "/apps/media/multigroup-media.html", I);
    await verifyMediaInWebpage([mediaTitleMaster], "/apps/media/multigroup/multigroup-media.html", I);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=70754");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.see("barepage", container);

    createNewMedia(mediaTitleSlave, I, DTE);

    //
    I.say("edit master media to verify only one copy");
    DT.filter("mediaTitleSk", mediaTitleMaster);
    I.click(mediaTitleMaster, container);
    DTE.waitForEditor();
    DTE.fillField("mediaTitleSk", mediaTitleMaster+"-chan.ge");
    DTE.save("datatableFieldDTE_Field_editorFields-media");

    //
    I.say("save slave webpage");
    DTE.save();

    await verifyMediaInWebpage([mediaTitleMaster, mediaTitleSlave] , "/apps/media/multigroup-media.html", I);
    await verifyMediaInWebpage([mediaTitleMaster, mediaTitleSlave], "/apps/media/multigroup/multigroup-media.html", I);

    //
    I.say("deleting media");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=70754");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.see("barepage", container);

    deleteNewMedia(mediaTitleMaster, I, DT, DTE);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=70753");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.see("barepage", container);

    deleteNewMedia(mediaTitleSlave, I, DT, DTE);

    await verifyMediaInWebpage([mediaTitleMaster, mediaTitleSlave] , "/apps/media/multigroup-media.html", I, false);
    await verifyMediaInWebpage([mediaTitleMaster, mediaTitleSlave], "/apps/media/multigroup/multigroup-media.html", I, false);
});