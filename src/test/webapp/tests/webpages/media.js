Feature('webpages.media');

var editorContainer = "#datatableFieldDTE_Field_editorFields-media_modal";
var randomText;

Before(({ I, login, DT }) => {
    login('admin');
    if (typeof randomText=="undefined") {
        randomText = I.getRandomText();
    }
    DT.addContext("media", "#datatableFieldDTE_Field_editorFields-media_wrapper");
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
            I.say("otvor Media vsetkych stranok");
            I.click( locate("#nav-iwcm_1_"), null, { position: { x: 70, y: 15 } } );
        }

        I.waitForElement("#nav-iwcm_1_.ui-state-active", 10);
        I.wait(1);

        I.say("Obrazky");
        I.click( locate("#nav-iwcm_1_L2ltYWdlcw_E_E"), null, { position: { x: 20, y: 5 } } );
        I.waitForInvisible("#WJLoaderDiv", 10);
        I.wait(1);

        I.say("bannery");
        I.click( locate("#nav-iwcm_1_L2ltYWdlcy9iYW5uZXJ5"), null, { position: { x: 20, y: 5 } } );
        I.waitForInvisible("#WJLoaderDiv", 10);
        I.wait(1);

        I.say("banner-investicie.jpg");
        I.click( locate("#iwcm_1_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pbnZlc3RpY2llLmpwZw_E_E"), null, { position: { x: 20, y: 5 } } );
        I.wait(1);

    I.switchTo();
    I.click({css: "#modalIframe div.modal-footer button"});
    I.wait(1);

    //aby sa nam presunul fokus
    DTE.fillField("mediaSortOrder", "20");

    //over, ze sa nam prenieslo meno obrazka do title
    I.seeInField("#DTE_Field_mediaTitleSk", "banner investicie");

    //vrat povodny nazov, aby nam bezal test korektne dalej
    DTE.fillField("mediaTitleSk", mediaTitle);
}

Scenario('media-zakladne testy @baseTest', async ({I, DTE, DataTables}) => {
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

Scenario('media tabulka v stranke @baseTest', async ({I, DataTables, DT, DTE, Browser}) => {
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
        skipDuplication: true,
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
    I.click(DT.btn.add_button);
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
    DT.filterContains("title", newPageName);
    I.clickCss("#datatableInit_wrapper .buttons-select-all");
    I.clickCss(".buttons-remove", '#datatableInit_wrapper .dt-buttons');
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();
    DT.filterContains("title", "");
    I.wait(1);
    I.dontSee(newPageName);
});

Scenario('kontrola menu poloziek', ({I}) => {
    //TODO: JAKARTAI.amOnPage("/admin/v9/webpages/media");
    //I.see("Média", "div.menu-wrapper");

    I.amOnPage("/admin/v9/webpages/media/#");
    I.see("Média", "div.menu-wrapper");

    I.amOnPage("/admin/v9/webpages/media/");
    I.see("Média", "div.menu-wrapper");

    I.amOnPage("/admin/v9/webpages/media/#/");
    I.see("Média", "div.menu-wrapper");
});

Scenario('Test media - all permissions media', ({ I, DT, DTE}) => {
    I.say('Logging in as tester2');
    I.relogin('tester2');

    I.say('Verifying access to media section');
    I.amOnPage('/admin/v9/webpages/media/');
    I.waitForText('Na túto aplikáciu/funkciu nemáte prístupové práva', 10);

    I.say('Opening editor for document 120034');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + 120034);

    I.say('Add media and check');
    I.waitForElement("#pills-dt-datatableInit-media-tab", 10);
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.click(DT.btn.media_add_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");
    DTE.fillField("mediaTitleSk", `autotest-${randomText}`);
    I.clickCss('.DTE_Field_Name_mediaThumbLink .btn[aria-label="Vybrať"]');
    I.waitForElement("#modalIframeIframeElement", 10);
    I.switchTo("#modalIframeIframeElement");
    I.fillField("#file", "/images/gallery/watermark/subfolder1/nature-5411408.jpg");
    I.switchTo();
    I.click(locate("#modalIframe button").withText("Potvrdiť"));
    I.click(locate('#datatableFieldDTE_Field_editorFields-media_modal div.DTE_Form_Buttons button').withText('Pridať'));
    DT.checkTableRow("datatableInit_modal", 1, ["", "", `autotest-${randomText}`, "", "/images/gallery/watermark/subfolder1/nature-5411408.jpg"]);

    I.say('Edit media and check');
    I.clickCss("#datatableFieldDTE_Field_editorFields-media_wrapper button.buttons-select-all");
    I.click(DT.btn.media_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");
    DTE.fillField("mediaTitleSk", `autotest-${randomText}-chan.ge`);
    I.click(locate('#datatableFieldDTE_Field_editorFields-media_modal div.DTE_Form_Buttons button').withText('Uložiť'));
    DT.checkTableRow("datatableInit_modal", 1, ["", "", `autotest-${randomText}-chan.ge`, "", "/images/gallery/watermark/subfolder1/nature-5411408.jpg"]);
});

Scenario('Delete media files', ({ I, DT, DTE }) => {
    I.say('Logging in as tester2');
    I.relogin('tester2');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + 120034);
    I.waitForElement("#pills-dt-datatableInit-media-tab", 10);
    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.say('Delete media and check');
    I.clickCss("#datatableFieldDTE_Field_editorFields-media_wrapper button.buttons-select-all");
    I.click(DT.btn.media_delete_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10);
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

//SIVAN - jstreeNavigate nefunguje lebo zamknute priecinky obsahuje pred nazvom medzeru
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
    DT.filterContains("editorFields.docDetails", "/Jet portal 4/Kontakt/Kontakt");

    I.see(xss1, "#mediaTable_wrapper");
    I.see(xss2, "#mediaTable_wrapper");

    I.say("Overenie v audite");

    I.amOnPage("/admin/v9/apps/audit-search/");
    DT.filterSelect("logType", "MEDIA");
    DT.filterContains("description", "^CREATE:");
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

    DT.filterContains("mediaLink", "/images/gallery/test/koala.jpg");

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
    DT.filterContains("mediaLink", "/images/gallery/test/koala.jpg");
    DT.filterContains("editorFields.docDetails", "24008");

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

    I.click(locate("#datatableFieldDTE_Field_editorFields-media_wrapper div.dt-footer-row ul.pagination li.dt-paging-button.page-item button").withText("2"));
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
    DT.filterContains("mediaTitleSk", searchText);

    I.see(webjet9);
    I.dontSee(test23);
    I.dontSee(mirroring);

    //test23 domain test
    Document.switchDomain("test23.tau27.iway.sk");
    DT.filterContains("mediaTitleSk", searchText);

    I.see(test23);
    I.dontSee(webjet9);
    I.dontSee(mirroring);

    //mirroring domain test
    Document.switchDomain("mirroring.tau27.iway.sk");
    DT.filterContains("mediaTitleSk", searchText);

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

    DT.filterContains("editorFields.docDetails", "aplikacie");
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
    DT.filterContains("mediaTitleSk", mediaTitle);
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
    DT.filterContains("mediaTitleSk", mediaTitleMaster);
    I.click(mediaTitleMaster, container);
    DTE.waitForEditor();
    DTE.fillField("mediaTitleSk", mediaTitleMaster+"-chan.ge");
    DTE.save("datatableFieldDTE_Field_editorFields-media");

    //
    I.say("save slave webpage");
    DTE.save();

    await verifyMediaInWebpage([mediaTitleMaster, mediaTitleSlave], "/apps/media/multigroup-media.html", I);
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

Scenario('testovanie app - media app', async ({ I, DTE, Apps, Document }) => {
    Apps.insertApp('Médiá', '#components-media-title', null, false);

    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');
    I.clickCss("button.btn-vue-jstree-item-edit");
    I.click(locate('.jstree-node.jstree-closed').withText('Aplikácie').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jstree-node.jstree-closed').withText('Média').find('.jstree-icon.jstree-ocl'));
    I.clickCss('#docId-24008_anchor');
    I.waitForElement('input[value="/Aplikácie/Média/Média"]', 10);
    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const defaultParams = {
        groups: "-1",
        docid: "24008"
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.seeElement(locate('a').withText('desert'));
    I.seeElement(locate('a').withText('hydrangeas'));
    I.seeElement(locate('a').withText('koala-media-all'));
    I.seeElement(locate('a').withText('about'));
    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        groups: "2",
        docid: "24008"
    };

    DTE.clickSwitch("groups_1");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.dontSeeElement(locate('a').withText('desert'));
    I.dontSeeElement(locate('a').withText('hydrangeas'));
    I.dontSeeElement(locate('a').withText('koala-media-all'));
    I.seeElement(locate('a').withText('about'));
});