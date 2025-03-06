Feature('admin.datatables');

Before(({ I, login }) => {
     login('admin');
});

Scenario('Nastavenie tabulky', async ({ I, Browser, Document }) => {
    if (Browser.isChromium()) {
        I.amOnPage("/admin/v9/apps/insert-script#/");

        //zobrazenie modalneho okna NASTAVENIA - je schovane pod dataTables
        I.clickCss(".buttons-settings");
        I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-page-length');
        //cakanie na dokoncenie animacie
        I.wait(4);
        //toto nie je spolahlive, aj ked je overflow tak to najde
        I.see("Zobrazenie stĺpcov");
        I.see("Počet záznamov");

        //hodnota musi byt 1, inak to bude padat pri overflow hidden (vid _table.scss riadok 498 - //toto nemozeme pouzit kvoli dropdown menu overflow: hidden;)
        await Document.compareScreenshotElement("#insertScriptTable_wrapper", "autotest-insert-script-settings.png", 1280, 270, 5);

        //generate base on server:
        //CODECEPT_URL='http://demotest.webjetcms.sk' CODECEPT_SHOW=false npx codeceptjs run --grep 'Nastavenie tabulky'
    }
});

Scenario('Bug zatvorenia okna po zruseni nested dialogu', ({ I, DTE }) => {
    //Web stranky - 24008 Media - editujem stranku, kliknem na kartu Media, otvorim Media, zatvorim krizikom hore, kliknem na Ulozit vo web stranke, okno sa nezatvori.
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=24008");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-media-tab");
    I.waitForElement("#pills-dt-datatableInit-media");
    I.wait(5);
    I.click("desert", "#datatableFieldDTE_Field_editorFields-media");
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-media");
    I.wait(2);
    I.clickCss("#datatableFieldDTE_Field_editorFields-media_modal div.DTE_Header button.btn-close-editor");
    I.wait(1);

    DTE.save();

    I.dontSeeElement("div.DTED.show");
});

Scenario('Bug oznacenia vsetkych riadkov', ({ I }) => {
    //
    I.say("serverSide=false")
    I.amOnPage("/admin/v9/settings/configuration/");

    I.clickCss("button.buttons-select-all");
    I.see("13 riadkov označených", "div.dt-info");

    //
    I.say("serverSide=true")
    I.amOnPage("/admin/v9/settings/redirect/");

    I.clickCss("button.buttons-select-all");
    I.see("13 riadkov označených", "div.dt-info");
});


Scenario('Zobrazenie tlacidiel maximalizovat a zatvorit', ({ I }) => {
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=1");

    I.seeElement("div.DTE_Header_Content i.ti-arrows-maximize");
    I.dontSeeElement("div.DTE_Header_Content i.ti-arrows-minimize");
    I.seeElement("div.DTE_Header button.btn-close-editor");
    I.dontSeeElement("#datatableInit_modal div.modal-fullscreen");

    I.forceClickCss("div.DTE_Header_Content i.ti-arrows-maximize");

    I.dontSeeElement("div.DTE_Header_Content i.ti-arrows-maximize");
    I.seeElement("div.DTE_Header_Content i.ti-arrows-minimize");
    I.seeElement("div.DTE_Header button.btn-close-editor");
    I.seeElement("#datatableInit_modal div.modal-fullscreen");

    I.clickCss("div.DTE_Header button.btn-close-editor");

    I.dontSeeElement("#datatableInit_modal");
});

Scenario('Maximalizovat - zrusene pre zmazanie zaznamu', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=1");
    I.forceClickCss("div.DTE_Header_Content i.ti-arrows-maximize");
    I.seeElement("#datatableInit_modal div.modal-fullscreen");

    I.clickCss("div.DTE_Header button.btn-close-editor");

    I.click("Generic");
    DTE.waitForEditor();
    I.seeElement("#datatableInit_modal div.modal-fullscreen");

    I.clickCss("div.DTE_Header button.btn-close-editor");

    DT.filterContains("tempName", "Generic");
    I.forceClick(locate('.dt-select-td.sorting_1').at(1));

    I.clickCss("#datatableInit_wrapper button.buttons-remove");
    DTE.waitForEditor();

    //nesmie byt fullscreen
    I.dontSeeElement("#datatableInit_modal div.modal-fullscreen");
    I.clickCss("div.DTE_Header button.btn-close-editor");
});

Scenario('Dynamicke urcenie poctu zaznamov', ({ I }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");

    I.see("Záznamy 1 až 13 z");

    I.resizeWindow(1280, 900);
    I.refreshPage();

    I.see("Záznamy 1 až 16 z");
});

Scenario('Editacia bunky po presune stlpca', ({ I, DT, Browser }) => {

    I.amOnPage("/admin/v9/settings/redirect/");
    DT.resetTable();
    DT.filterContains("newUrl", "/test-stavov/virtualpath/podla-title.html");

    if (Browser.isFirefox()) {
        I.dragAndDrop("div.dt-scroll-headInner th.dt-th-redirectCode", "div.dt-scroll-headInner th.dt-th-oldUrl", { force: true });
    } else {
        I.dragAndDrop("div.dt-scroll-headInner th.dt-th-redirectCode", "div.dt-scroll-headInner th.dt-th-newUrl", { force: true });
    }

    I.click({css: "div.buttons-select-cel"});

    I.forceClickCss("#datatableInit tbody tr:nth-child(1) td:nth-child(3)");

    I.seeInField("div.DTE.DTE_Bubble div.DTE_Bubble_Table div.DTE_Field_InputControl input", "301");

    I.dontSee("Stará URL", "div.DTE.DTE_Bubble");
    I.dontSee("Doména", "div.DTE.DTE_Bubble");

    //over zapamatanie poradia
    I.say("over, ze poradie si to po reloade stranky pamata");
    I.amOnPage("/admin/v9/settings/redirect/");

    I.see("Presmerovací kód", "div.dt-scroll-headInner table thead tr:first-child th:nth-child(3)")

    //
    I.say("Over korektne filtrovanie po presune stlpca");
    I.dtFilter("newUrl", "/sk12345-");
    I.see("/sk12345-imptest2b/", "#datatableInit");
    I.see("Záznamy 1 až 7 z 7");

    I.dtFilter("oldUrl", "imptest3");
    I.see("Záznamy 1 až 3 z 3");

    I.fillField("input.dt-filter-from-redirectCode", "302");
    I.clickCss("button.dt-filtrujem-redirectCode");
    DT.waitForLoader();
    I.see("Záznamy 1 až 3 z 3");
});

Scenario('Presun stlpca, editacia bunky-reset', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.resetTable();
});

Scenario('Zobrazenie nazvu v hlavicke', ({ I, DT, DTE }) => {

    I.openNewTab();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=221");
    I.closeOtherTabs();
    DT.waitForLoader();
    I.waitForText("page-2021-02-23-134924-937", 10, "#datatableInit_wrapper");

    I.clickCss("#datatableInit tbody tr:nth-child(1) td.dt-row-edit a");
    DTE.waitForEditor();
    I.see("Zobrazený v menu", "#datatableInit_modal div.DTE_Header_Content h5.modal-title");
    I.wait(3);
    DTE.cancel();
    I.wait(1);

    I.say("Oznac 2 riadky a over zobrazenie nadpisu");

    I.forceClickCss("#datatableInit tbody tr:nth-child(1) td.dt-select-td");
    I.forceClickCss("#datatableInit tbody tr:nth-child(2) td.dt-select-td");

    I.wait(1);

    I.clickCss("#datatableInit_wrapper button.buttons-edit");
    DTE.waitForEditor();
    I.see("Upraviť", "#datatableInit_modal div.DTE_Header_Content h5.modal-title");
    I.wait(1);
    DTE.cancel();

    I.wait(1);

    I.say("Overenie pri mazani");

    I.clickCss("#datatableInit_wrapper button.buttons-remove");
    I.see("Zobrazený v menu", "#datatableInit_modal div.DTE_Form_Info div.col-sm-7");
    I.see("page-2021-02-23-134924-937", "#datatableInit_modal div.DTE_Form_Info div.col-sm-7");
    DTE.cancel();

});

Scenario('drag drop okna editora', async ({ I, DTE, Browser }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=92");
    DTE.waitForEditor();
    let value = await I.grabCssPropertyFrom('div.DTED.show div.modal-content', 'left');
    I.assertEqual(value, "0px");
    I.wait(0.5);
    I.dragSlider("div.DTED.show div.modal-header", -70);
    I.wait(0.5);
    value = await I.grabCssPropertyFrom('div.DTED.show div.modal-content', 'left');
    if (Browser.isFirefox()) {
        I.assertEqual(value, "-13px");
    } else {
        I.assertEqual(value, "-69px");
    }

    I.say("Maximalizujem a overim, ze je vystredene");
    I.forceClickCss("div.DTED.show div.DTE_Header_Content i.ti-arrows-maximize");
    value = await I.grabCssPropertyFrom('div.DTED.show div.modal-content', 'left');
    I.assertEqual(value, "0px");

    I.say("Minimalizujem a overim, ze je vystredene");
    I.forceClickCss("div.DTED.show div.DTE_Header_Content i.ti-arrows-minimize");
    value = await I.grabCssPropertyFrom('div.DTED.show div.modal-content', 'left');
    I.assertEqual(value, "0px");
});

Scenario('pamatanie usporiadania', ({ I }) => {
    I.amOnPage("/admin/v9/settings/redirect/");
    I.click(locate("div.dt-scroll-headInner table thead tr:first-child th").withText("Dátum vytvorenia"));
    I.see("/sk12345/", "#datatableInit tbody tr:first-child td");
    I.see("26.06.2020 10:52:32", "#datatableInit tbody tr:first-child td");
});

Scenario('pamatanie usporiadania-reset', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.resetTable();
});

Scenario('pamatanie velkost stranky', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.waitForLoader();

    //
    I.say("nastav zobrazenie na 50 zaznamov na stranu");
    I.click({ css: 'button[data-dtbtn=settings]' });
    I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-page-length');
    I.clickCss('.btn.buttons-collection.dropdown-toggle.buttons-page-length');
    I.waitForVisible("div.dt-button-collection");

    I.click("50");
    I.click("Uložiť");

    //
    I.say("reloadni a over, ze mame 50 zaznamov");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.waitForLoader();
    I.see("Záznamy 1 až 50 z", "div.dt-footer-row");

    //
    I.say("nastav nazad na auto");
    I.click({ css: 'button[data-dtbtn=settings]' });
    I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-page-length');
    I.clickCss('.btn.buttons-collection.dropdown-toggle.buttons-page-length');
    I.waitForVisible("div.dt-button-collection");

    I.click("Automaticky (13)");
    I.click("Uložiť");
    DT.waitForLoader();
    I.see("Záznamy 1 až 13 z", "div.dt-footer-row");

    //
    I.say("reloadni v auto rezime");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.waitForLoader();
    I.see("Záznamy 1 až 13 z", "div.dt-footer-row");

    //
    I.say("zvacsi okno a over, ze auto rezim funguje");
    I.resizeWindow(1280, 1000);
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.waitForLoader();
    I.see("Záznamy 1 až 19 z", "div.dt-footer-row");
});

Scenario('pamatanie velkost stranky-reset', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.resetTable();
});

function checkTemplateIdValue(I, checkToastMessage=true) {
    if (checkToastMessage) {
        I.waitForText("Pole Šablóna web stránky (tempId) neobsahuje možnosť s ID 5.", 10, "div.toast-message");
        I.toastrClose();
    }
    I.clickCss("#pills-dt-datatableInit-template-tab");
    I.see("id: 5", "div.DTE_Field_Name_tempId div.filter-option-inner-inner");
}

Scenario('select - missing ID', ({ I, DT, DTE }) => {
    //add missing ID to select fields in editor #54953-16
    let docId = 63979;
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
    DTE.waitForEditor();

    checkTemplateIdValue(I, true);

    //
    I.say("Reopen editor, ther should not to be toast notify again");
    DTE.cancel();
    I.click("Test missing tempId", "#datatableInit");
    DTE.waitForEditor();
    I.dontSeeElement("div.toast-message");
    checkTemplateIdValue(I, false);
    DTE.cancel();

    //
    I.say("Go to another page to reload options");
    I.click("1", "#datatableInit_wrapper div.dt-paging");
    I.dontSeeElement("div.toast-message");

    //
    I.say("Open page then open page with missing ID");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=92");
    DTE.waitForEditor();
    DTE.cancel();

    I.fillField("#tree-doc-id", docId);
    I.pressKey('Enter');

    DTE.waitForEditor();
    checkTemplateIdValue(I, true);
});

Scenario("conditional tabs", ({ I, DT, DTE }) => {
    //
    I.say("hide-on-create");
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.waitForLoader();
    I.click(DT.btn.add_button);

    DTE.waitForEditor();
    I.seeElement(locate("#pills-dt-editor-datatableInit li a").withText("Základné"));
    I.dontSeeElement("#pills-dt-editor-datatableInit li.hide-on-create");
    I.dontSeeElement(locate("#pills-dt-editor-datatableInit li a").withText("Priečinky"));

    //
    DT.addContext("campaings", "#campaingsDataTable_wrapper");
    I.say("hide-on-duplicate");
    I.amOnPage("/apps/dmail/admin/");
    DT.filterContains("subject", "testOfUnsucribed");
    I.click(".buttons-select-all");
    I.click(DT.btn.campaings_duplicate_button);
    DTE.waitForEditor("campaingsDataTable");
    I.seeElement(locate("#pills-dt-editor-campaingsDataTable li a").withText("Základné"));
    I.dontSeeElement("#pills-dt-editor-campaingsDataTable li.hide-on-duplicate");
    I.dontSeeElement(locate("#pills-dt-editor-campaingsDataTable li a").withText("Otvorenia"));

});