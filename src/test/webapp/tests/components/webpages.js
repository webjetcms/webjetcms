Feature('components.webpages');
var randomNumber;
var container = "div.tree-col";
var containerPage = "#datatableInit_wrapper";

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('zoznam stranok', ({ I, DT}) => {
    I.waitForText("Newsletter", 20);
    I.click("Newsletter", container);
    I.see("Testovaci newsletter");

    //over prava
    DT.checkPerms("menuWebpages,cmp_news", "/admin/v9/webpages/web-pages-list/?groupid=0", "datatableInit");
});

Scenario('logout', async ({I}) => {
    //if previous test fail logout
    I.logout();
});

Scenario('nový adresár', ({ I, DTE }) => {
    I.waitForElement("button.buttons-create", 10, container);
    I.click("button.buttons-create", container);
    DTE.waitForEditor("groups-datatable");
    I.groupSetRootParent();
    I.click("Pridať");

    I.see("Povinné pole", "div.DTE_Field_Name_groupName");
    I.see("Povinné pole", "div.DTE_Field_Name_navbarName");
    I.fillField("#DTE_Field_groupName", "test-adresar-" + randomNumber);

    //netreba - vyplna sa automaticky on blur I.fillField("#DTE_Field_navbarName", "test-adresar-" + randomNumber);
    I.click("Pridať");

    //I.dontSee("Povinné pole");
    I.waitForText("test-adresar-" + randomNumber, 10, container);
});

Scenario('editácia adresáru', ({ I, DTE }) => {
    I.waitForText("test-adresar-" + randomNumber, 20, container);

    I.click("test-adresar-" + randomNumber, container);
    I.click("button.buttons-edit", container);
    I.clearField("#DTE_Field_groupName");
    I.fillField("#DTE_Field_groupName", "test-adresar-2-" + randomNumber);
    DTE.save();

    I.waitForText("test-adresar-2-" + randomNumber, 20, container);
});


//chyba nastala ked sa kliklo na editovat adresar, zrusit, editovat stranku, zrusit, editovat stranku, ulozit
//zostali zobrazene oba backdropy a schoval sa len jeden (zly handling backdropov vo WJ)
Scenario('overenie zobrazenia backdropov medzi 2 editormi', ({ I, DT, DTE }) => {
    //zobraz do editora adresar
    I.waitForText("test-adresar-2-" + randomNumber, 20, container);
    DT.waitForLoader();
    I.click("test-adresar-2-" + randomNumber, container);
    I.click("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    //zrus editaciu adresara
    I.click({css: "#groups-datatable_modal div.DTE_Footer.modal-footer button.btn-close-editor"});

    DT.waitForLoader();
    I.dontSeeElement('.modal-backdrop');

    //zobraz editor web stranky (je to web stranka v root adresari, takze jej sa meno nezmenilo na -2-)
    I.click("test-adresar-" + randomNumber, containerPage);
    DTE.waitForEditor();
    //zrus editaciu web stranky
    I.click({css: "#datatableInit_modal div.DTE_Footer.modal-footer button.btn-close-editor"});

    I.wait(1);
    I.dontSeeElement('.modal-backdrop');

    //znova otvor okno editora a klikni na ulozit
    DT.waitForLoader();
    I.click("test-adresar-" + randomNumber, containerPage);
    DTE.waitForEditor();
    I.click('Uložiť', {css: "#datatableInit_modal div.DTE_Footer.modal-footer"});
    DTE.waitForLoader();

    //znova over kliknutie na adresar, ak sa zle schoval backdrop, toto padne
    I.wait(3);
    I.dontSeeElement('.modal-backdrop');
});

Scenario('zmazanie adresaru', ({ I, DTE }) => {
    I.waitForText("test-adresar-2-" + randomNumber, 20, container);

    within(container, () => {
        I.click("test-adresar-2-" + randomNumber);
        I.click("button.buttons-remove");
    });

    I.click("Zmazať");

    DTE.waitForLoader();

    I.dontSee("test-adresar-2-" + randomNumber, container);
});

Scenario('planovanie a historia', ({ I, DT, DTE }) => {
    I.jstreeNavigate(["Test stavov", "Test publikovania"]);
    I.click("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");

    I.click("#pills-dt-groups-datatable-publishing-tab");
    DT.waitForLoader();
    I.see("Test publikovania-2030", "#datatableFieldDTE_Field_editorFields-groupSchedulerPlannedChanges_wrapper");
    I.dontSee("Test publikovania-20180", "#datatableFieldDTE_Field_editorFields-groupSchedulerPlannedChanges_wrapper");

    I.click("#pills-dt-groups-datatable-history-tab");
    DT.waitForLoader();
    I.dontSee("Test publikovania-2030", "#datatableFieldDTE_Field_editorFields-groupSchedulerChangeHistory_wrapper");
    I.see("Test publikovania-20180", "#datatableFieldDTE_Field_editorFields-groupSchedulerChangeHistory_wrapper");

});


Scenario('poznamka redaktora', ({ I }) => {
    //stranka 299 test23/testing2 ma poznamku redaktora, over jej zobrazenie
    I.jstreeClick("Test stavov");
    I.jstreeClick("Tento nie je interný");

    I.dontSeeElement("#toast-container-webjet");

    I.click("Tento nie je interný", containerPage);
    I.see("Toto je poznámka redaktora", "#toast-container-webjet");
    I.wait(17);
    I.dontSeeElement("#toast-container-webjet");
});

Scenario('System adresar', ({ I, DT }) => {
    //v zalozke Priecinky nesmie byt vidiet adresar System
    I.dontSee("System", "#SomStromcek");

    //prepni sa na System zalozku
    I.click("#pills-system-tab");
    DT.waitForLoader();
    I.dontSee("Jet portal 4", "#SomStromcek");
    I.dontSee("Newsletter", "#SomStromcek");
    I.see("Hlavičky", "#SomStromcek");
    I.see("Pätičky", "#SomStromcek");
    I.see("Menu", "#SomStromcek");
    //over aj zobrazenie web stranok
    I.see("System", "#datatableInit_wrapper");
    //I.see("WebJET 9 info", "#datatableInit_wrapper");

    //prepni sa na Kos zalozku
    I.click("#pills-trash-tab");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
    I.dontSee("Jet portal 4", "#SomStromcek");
    I.dontSee("Newsletter", "#SomStromcek");
    I.dontSee("System", "#SomStromcek");
    I.dontSee("Hlavičky", "#SomStromcek");
    I.dontSee("Pätičky", "#SomStromcek");
    I.dontSee("Menu", "#SomStromcek");
    I.see("test-adresar-", "#SomStromcek");
    //over aj zobrazenie web stranok
    I.see("Test zmazania stránky", "#datatableInit_wrapper");

    //prepnit sa nazad na Priecinky zalozku
    I.click("#pills-folders-tab");
    DT.waitForLoader();
    I.see("Jet portal 4", "#SomStromcek");
    I.see("Newsletter", "#SomStromcek");
    I.dontSee("System", "#SomStromcek");
    //over aj zobrazenie web stranok
    I.see("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
});

Scenario('Zmena domeny', ({ I }) => {
    //over, ze mame zobrazenu standardnu domenu
    var container = "#SomStromcek";
    I.see("Jet portal 4", container);
    I.see("English", container);
    I.see("Newsletter", container);
    I.dontSee("domena1.tau27.iway.sk", container);

    //domenovy selektor
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("domena1.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");

    //domena zmenena, over zobrazenie
    I.dontSee("Jet portal 4", container);
    I.dontSee("English", container);
    I.dontSee("Newsletter", container);
    I.see("domena1.tau27.iway.sk", container);

    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});

Scenario('Import/export stranok', ({ I, DT }) => {
    //pause();
    //I.seeElement("#datatableInit_wrapper div.dt-buttons button.buttons-import-export.disabled");
    I.waitForText("Test stavov", 10, "#SomStromcek");
    I.click("Test stavov", "#SomStromcek");
    DT.waitForLoader();
    I.dontSeeElement("#datatableInit_wrapper div.dt-buttons button.buttons-import-export.disabled");
    I.seeElement("#datatableInit_wrapper div.dt-buttons button.buttons-import-export");
    I.click("#datatableInit_wrapper div.dt-buttons button.buttons-import-export");
    I.wait(4);
    I.switchToNextTab();
    I.see("Importovať web stránky zo ZIP archívu (xml)");
    I.closeCurrentTab();
});

Scenario('Overenie zobrazenia sablon podla adresarov', ({ I, DTE }) => {
    I.click("Jet portal 4", container);
    I.click("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.click("#pills-dt-groups-datatable-template-tab");
    I.waitForText('Šablóna pre web stránky', 10);
    I.click("div.DTE_Field_Name_tempId div.dropdown");
    I.see("Homepage", "div.dropdown-menu.show");
    I.dontSee("Newsletter", "div.dropdown-menu.show");
    I.click("div.DTE_Field_Name_tempId div.dropdown");

    I.click({css: "#groups-datatable_modal div.DTE_Footer.modal-footer button.btn-close-editor"});

    I.click("Newsletter", container);
    I.click("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.click("#pills-dt-groups-datatable-template-tab");
    I.waitForText('Šablóna pre web stránky', 10);
    I.click("div.DTE_Field_Name_tempId div.dropdown");
    I.see("Homepage", "div.dropdown-menu.show");
    I.see("Newsletter", "div.dropdown-menu.show");
    //aby sa zatvoril select box
    I.click("Newsletter", "div.dropdown-menu.show");

    I.click({css: "#groups-datatable_modal div.DTE_Footer.modal-footer button.btn-close-editor"});
});

Scenario('Overenie zalozky Naposledy Upravene', ({ I, DT }) => {
    I.see("Naposledy upravené", "#pills-pages");
    I.click("Naposledy upravené", "#pills-pages");
    DT.waitForLoader();
    //nemame prilis ine co otestovat
    I.see("Záznamy 1 až", "#datatableInit_wrapper");
});

Scenario('Overenie zalozky Na schvalenie', ({ I, DT }) => {
    I.see("Čakajúce na schválenie", "#pills-pages");
    I.click("Čakajúce na schválenie", "#pills-pages");
    DT.waitForLoader();
    //nemame prilis ine co otestovat
    I.see("Čakajúce na schválenie-zmena titulku", "#datatableInit_wrapper");

    //prepni domenu a over, ze tam nie je nic ine
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("test23.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");

    I.wait(1);

    I.dontSee("Čakajúce na schválenie", "#pills-pages");
});

Scenario('logout 2', ({ I }) => {
    //aby ked padol predchadzajuci test nepadli vsetky dalsie v inej domene
    I.logout();
});

Scenario('Overenie nova web stranka', ({ I, DT, DTE }) => {
    let oldValue = "Nová web stránka";
    let newValue = "autotest-title";

    I.jstreeNavigate(["Test stavov"]);
    DT.filter("title", oldValue);
    I.click(oldValue);
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_title", oldValue);
    I.seeInField("#DTE_Field_navbar", oldValue);
    I.seeInField("#DTE_Field_virtualPath", "nova-web-stranka.html");

    //vypln title na nieco ine a sprav blur
    I.appendField('#DTE_Field_title', newValue);
    //nevedel som inak spravit blur
    I.click("#pills-dt-datatableInit-template-tab");
    I.click("#pills-dt-datatableInit-basic-tab");

    I.seeInField("#DTE_Field_title", newValue);
    I.seeInField("#DTE_Field_navbar", newValue);
    I.dontSeeInField("#DTE_Field_title", oldValue);
    I.dontSeeInField("#DTE_Field_navbar", oldValue);
    I.dontSeeInField("#DTE_Field_virtualPath", "nova-web-stranka.html", );
});

Scenario('Automaticke rozbalenie domeny', ({ I }) => {
    //over, ze mame zobrazenu standardnu domenu
    var container = "#SomStromcek";

    //domenovy selektor
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("rozbalenie.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");

    //domena zmenena, over zobrazenie
    I.dontSee("Jet portal 4", container);
    I.dontSee("English", container);
    I.dontSee("Newsletter", container);
    I.see("Test rozbalenia podadresarov", container);
    I.see("Podadresár A", container);
    I.see("Podadresár B", container);
    I.see("Podadresár C", container);
    I.dontSee("Podadresár B-1", container);
});

Scenario('Automaticke rozbalenie domeny-odhlasenie', ({ I }) => {
    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});

Scenario('Kontrola subFolder dat', ({ I, DT }) => {

    //Skontrolujem ze main pages su tam a sub pages nie
    I.click("Jet portal 4", container);
    DT.waitForLoader();
    I.see("Jet portal 4 - testovacia stranka");
    I.dontSee("Produktová stránka - B verzia");

    I.click("English", container);
    DT.waitForLoader();
    I.see("Jet portal 4 - testovacia stranka");
    I.dontSee("McGregor sales force");

    //nacitaj sub folders
    I.forceClick({ css: '#dtRecursiveSwitch' });
    DT.waitForLoader();

    //over ze sa natiahli sub folders pre group-u "English"
    I.see("McGregor sales force");

    //over ze stav tlacidla sa zachoval po zmene group v jsTree
    I.click("Jet portal 4", container);
    DT.waitForLoader();

    I.see("Produktová stránka - B verzia");

    //pridaj stlpec šablon medzi videne
    I.click({ css: 'button[data-dtbtn=settings]' });
    I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-colvis');

    I.click(locate('button').withAttr({ title: 'Zobrazenie stĺpcov' }));
    I.click(locate('span').withText('Šablóna web stránky'));
    I.click("button.btn.colvis-postfix.btn-primary.dt-close-modal");
    DT.waitForLoader();

    //over ze sa natiahla spravne sablona sub foldera
    I.see("Microsite - blue");

    //rozklikni select
    I.click("div.dataTables_scrollHeadInner div.dt-filter-tempId button");
    I.see("Microsite - blue", "ul.dropdown-menu.inner.show");
    I.wait(1);

    I.click("#pills-changes-tab");
    I.wait(1);
    I.click("#pills-pages-tab");

    //over zobrazenie sablony v tabulke
    I.see("Produktová stránka - B verzia");
    I.see("Microsite - blue");
});

Scenario('Kontrola subFolder dat - reset nastaveni stlpcov', ({ I, DT }) => {

    DT.waitForLoader();
    I.click({ css: 'button[data-dtbtn=settings]' });
    I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-colvis');

    I.click(locate('button').withAttr({ title: 'Zobrazenie stĺpcov' }));
    I.click("button.buttons-colvisRestore");
    DT.waitForLoader();
});

Scenario('Overenie vyhladavania podla boolean a password_protected', ({ I, DT }) => {
    I.jstreeNavigate(["Newsletter"]);

    var container = "#datatableInit_wrapper";

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    I.wait(1);

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.click("Prehľadávať");
    I.click("Povoliť prístup len pre skupinu používateľov");
    I.click("Priradiť stránku k hromadnému emailu");
    I.click("button.btn.btn-primary.dt-close-modal");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    //prehladavat
    I.see("Newsletter", container);
    I.see("Testovaci newsletter", container);
    //prepni prehladavat na ano
    DT.filterSelect("searchable", "Áno");
    DT.waitForLoader();
    I.see("Newsletter", container);
    I.dontSee("Testovaci newsletter", container);
    //prepni na nie
    DT.filterSelect("searchable", "Nie");
    DT.waitForLoader();
    I.dontSee("Produktová stránka - B verzia", container);
    I.see("Testovaci newsletter", container);

    //vyhladavanie podla prav
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Newsletter"]);

    I.see("Produktová stránka - B verzia", container);
    I.see("Testovaci newsletter", container);
    DT.filter("editorFields.emails", "Vianočné pozdravy");
    I.dontSee("Produktová stránka - B verzia", container);
    I.see("Testovaci newsletter", container);

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
});

Scenario('Overenie vyhladania po nacitani', ({ I, DT }) => {
    //wj mal bug, ze hned po nacitani ked som zadal vyhladavanie neprenieslo korektne groupId
    DT.filter("authorName", "tester");
    DT.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy", "#datatableInit_wrapper");
});

//tempFieldADocId / tempFieldBDocId / tempFieldCDocId  /tempFieldDDocId
Scenario('Otestuj nove stlpce tempFieldDocId', ({ I, DT, DTE }) => {
    var name = "Test_volnych_poli_sablony";

    //Select list in tree
    I.jstreeClick("Test stavov");
    DT.filter("title", name);

    //Edit entity
    I.click(name);
    DTE.waitForEditor();

    I.click('#pills-dt-datatableInit-basic-tab');
    I.fillField("#DTE_Field_title", name + "_" +randomNumber);

    //Change to tab template
    I.click('#pills-dt-datatableInit-template-tab');

    //Set select tempFieldADocId
    I.click(".DTE_Field_Name_tempFieldADocId");
    I.click(locate('span').withText('Hlavičky/Default header'));
    I.wait(1);
    //Set select tempFieldADocId
    I.click(".DTE_Field_Name_tempFieldBDocId");
    I.click(locate('span').withText('System'));
    I.wait(1);
    //Set select tempFieldADocId
    I.click(".DTE_Field_Name_tempFieldCDocId");
    I.click(locate('span').withText('Hlavičky/Default header'));
    I.wait(1);
    //Set select tempFieldADocId
    I.click(".DTE_Field_Name_tempFieldDDocId");
    I.click(locate('span').withText('Hlavičky/Default footer'));
    I.wait(1);

    //Save change entity
    DTE.save();
    I.wait(2);

    //Edit this entity again
    I.click(name + "_" +randomNumber);
    DTE.waitForEditor();

    I.click('#pills-dt-datatableInit-basic-tab');
    I.fillField("#DTE_Field_title", name);

    //Change to tab template
    I.click('#pills-dt-datatableInit-template-tab');

    //CHECK if set values in selects are correct
    //Select tempFieldADocId
    I.seeElement(locate('button').withAttr({title: 'Hlavičky/Default header'}).inside(locate('.DTE_Field_Name_tempFieldADocId')))
    //Select tempFieldBDocId
    I.seeElement(locate('button').withAttr({title: 'System'}).inside(locate('.DTE_Field_Name_tempFieldBDocId')))
    //Select tempFieldCDocId
    I.seeElement(locate('button').withAttr({title: 'Hlavičky/Default header'}).inside(locate('.DTE_Field_Name_tempFieldCDocId')))
    //Select tempFieldDDocId
    I.seeElement(locate('button').withAttr({title: 'Hlavičky/Default footer'}).inside(locate('.DTE_Field_Name_tempFieldDDocId')))

    //Save change entity
    DTE.save();
    I.wait(2);

    //Edit this entity again
    I.click(name);
    DTE.waitForEditor();

    //Go to history tab
    I.click('#pills-dt-datatableInit-history-tab');
    I.wait(2);

    I.fillField("#datatableFieldDTE_Field_editorFields-history_wrapper input.dt-filter-title", name + "_" +randomNumber);
    I.pressKey('Enter', "input.dt-filter-key");
    I.wait(1);

    I.click("#datatableFieldDTE_Field_editorFields-history_wrapper button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(".buttons-history-edit");

    //Check set values from history

    //Change to tab template
    I.click('#pills-dt-datatableInit-template-tab');

    //CHECK if set values in selects are correct
    //Select tempFieldADocId
    I.seeElement(locate('button').withAttr({title: 'Hlavičky/Default header'}).inside(locate('.DTE_Field_Name_tempFieldADocId')))
    //Select tempFieldBDocId
    I.seeElement(locate('button').withAttr({title: 'System'}).inside(locate('.DTE_Field_Name_tempFieldBDocId')))
    //Select tempFieldCDocId
    I.seeElement(locate('button').withAttr({title: 'Hlavičky/Default header'}).inside(locate('.DTE_Field_Name_tempFieldCDocId')))
    //Select tempFieldDDocId
    I.seeElement(locate('button').withAttr({title: 'Hlavičky/Default footer'}).inside(locate('.DTE_Field_Name_tempFieldDDocId')))

    //tato stranka ma ako volne pole A sablony nastavenu navigaciu a vlozenu do tela stranky, over zobrazenie
    I.amOnPage("/test-stavov/test_volnych_poli_sablony.html");
    I.see("Úvod", "div.template-object-a");
});

Scenario('Otestuj nove stlpce show_in', ({ I, DT, DTE }) => {
    var name = "Test_show_in";

    //Select list in tree
    I.jstreeClick("Test stavov");
    DT.filter("title", name);

    //Edit entity
    I.click(name);
    DTE.waitForEditor();

    I.click('#pills-dt-datatableInit-basic-tab');
    I.fillField("#DTE_Field_title", name + "_" +randomNumber);

    //Change to tab template
    I.click('#pills-dt-datatableInit-menu-tab');

    //Set select showInMenu
    I.click(".DTE_Field_Name_showInMenu");
    I.click(locate('span').withText('Zobraziť'));
    I.wait(2);
    //Set select showInNavbar
    I.click(".DTE_Field_Name_showInNavbar");
    I.click(locate('span').withText('Nezobraziť'));
    I.wait(2);
    //Set select showInSitemap
    I.click(".DTE_Field_Name_showInSitemap");
    I.click(locate('span').withText('Rovnako ako menu'));
    I.wait(2);
    //Set select loggedShowInMenu
    I.click(".DTE_Field_Name_loggedShowInMenu");
    I.click(locate('span').withText('Nezobraziť'));
    I.wait(2);
    //Set select loggedShowInNavbar
    I.click(".DTE_Field_Name_loggedShowInNavbar");
    I.click(locate('span').withText('Rovnako ako pre neprihláseného'));
    I.wait(2);
    //Set select loggedShowInSitemap
    I.click(".DTE_Field_Name_loggedShowInSitemap");
    I.click(locate('span').withText('Zobraziť'));
    I.wait(2);

    //Save change entity
    DTE.save();

    //Edit this entity again - vrat nazad meno stranky
    I.click(name + "_" +randomNumber);
    DTE.waitForEditor();

    I.click('#pills-dt-datatableInit-basic-tab');
    I.fillField("#DTE_Field_title", name);

    //Change to tab template
    I.click('#pills-dt-datatableInit-menu-tab');

    //CHECK if set values in selects are correct
    I.seeElement(locate('button').withAttr({title: 'Zobraziť'}).inside(locate('.DTE_Field_Name_showInMenu')))
    I.seeElement(locate('button').withAttr({title: 'Nezobraziť'}).inside(locate('.DTE_Field_Name_showInNavbar')))
    I.seeElement(locate('button').withAttr({title: 'Rovnako ako menu'}).inside(locate('.DTE_Field_Name_showInSitemap')))
    I.seeElement(locate('button').withAttr({title: 'Nezobraziť'}).inside(locate('.DTE_Field_Name_loggedShowInMenu')))
    I.seeElement(locate('button').withAttr({title: 'Rovnako ako pre neprihláseného'}).inside(locate('.DTE_Field_Name_loggedShowInNavbar')))
    I.seeElement(locate('button').withAttr({title: 'Zobraziť'}).inside(locate('.DTE_Field_Name_loggedShowInSitemap')))

    //Save change entity
    DTE.save();

    //Edit this entity again
    I.click(name);
    DTE.waitForEditor();

    //Go to history tab
    I.click('#pills-dt-datatableInit-history-tab');
    I.waitForVisible("#pills-dt-datatableInit-history", 20);
    I.wait(2);

    I.fillField("#datatableFieldDTE_Field_editorFields-history_wrapper input.dt-filter-title", name + "_" +randomNumber);
    I.pressKey('Enter', "input.dt-filter-key");
    I.wait(1);

    I.click("#datatableFieldDTE_Field_editorFields-history_wrapper button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.click(".buttons-history-edit");
    I.wait(3);

    //Change to tab template
    I.click('#pills-dt-datatableInit-menu-tab');
    I.waitForVisible("#pills-dt-datatableInit-menu", 10);

    //Check set values from history

    //CHECK if set values in selects are correct
    I.seeElement(locate('button').withAttr({title: 'Zobraziť'}).inside(locate('.DTE_Field_Name_showInMenu')))
    I.seeElement(locate('button').withAttr({title: 'Nezobraziť'}).inside(locate('.DTE_Field_Name_showInNavbar')))
    I.seeElement(locate('button').withAttr({title: 'Rovnako ako menu'}).inside(locate('.DTE_Field_Name_showInSitemap')))
    I.seeElement(locate('button').withAttr({title: 'Nezobraziť'}).inside(locate('.DTE_Field_Name_loggedShowInMenu')))
    I.seeElement(locate('button').withAttr({title: 'Rovnako ako pre neprihláseného'}).inside(locate('.DTE_Field_Name_loggedShowInNavbar')))
    I.seeElement(locate('button').withAttr({title: 'Zobraziť'}).inside(locate('.DTE_Field_Name_loggedShowInSitemap')))
});

Scenario('Overenie zobrazenia pola pre zadanie domeny', ({ I, DT, DTE }) => {
    I.jstreeNavigate(['Jet portal 4', 'Úvodná stránka']);
    I.click("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.dontSeeElement("div.DTE_Field_Name_domainName");
    I.dontSeeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");

    //overenie zobrazenia, ked zmenim parent na root
    I.groupSetRootParent();
    I.seeElement("div.DTE_Field_Name_domainName");
    I.seeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");

    DTE.cancel();

    I.click('Jet portal 4', container);
    DT.waitForLoader();
    I.click("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.seeElement("div.DTE_Field_Name_domainName");
    I.seeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");

    //zmen parent na /English
    I.click('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'); // zmena na korenovy adresar
    I.waitForElement("div.jsTree-wrapper");
    I.wait(1);
    I.waitForText('English', 5);
    I.wait(1);
    I.click('English', 'div.jsTree-wrapper');
    I.wait(1);

    I.dontSeeElement("div.DTE_Field_Name_domainName");
    I.dontSeeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");

    //test otvorenim priamo editora
    I.amOnPage("/admin/v9/webpages/web-pages-list?groupid=23");
    DT.waitForLoader();
    I.click("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.dontSeeElement("div.DTE_Field_Name_domainName");
    I.dontSeeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");

    I.amOnPage("/admin/v9/webpages/web-pages-list?groupid=1");
    DT.waitForLoader();
    I.click("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.seeElement("div.DTE_Field_Name_domainName");
    I.seeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");
});

Scenario('Overenie zobrazenia tooltipov', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=11");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");

    I.dontSee("Správny titulok vystihuje podstatu stránky");
    I.moveCursorTo("div.DTE_Field_Name_title button.btn-tooltip");
    I.see("Správny titulok vystihuje podstatu stránky", "div.tooltip.show");

    //over konverziu Markdown na HTML kod
    I.click("#pills-dt-datatableInit-template-tab");
    I.scrollTo('div.DTE_Field_Name_htmlHead');
    I.dontSee("Voliteľne môžete zadať HTML kód, ktorý sa priamo vloží do HTML kódu web stránky.");
    I.moveCursorTo("div.DTE_Field_Name_htmlHead button.btn-tooltip");
    I.see("html_head", "div.tooltip.show i");
});

Scenario('nejde otvorit stranku z Naposledy upravene', ({ I, DT, DTE }) => {
    //neslo otvorit stranku z Naposledy upravene kvoli groupId=999997 co je NULL
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    DT.waitForLoader();
    I.click("#pills-changes-tab");
    DT.waitForLoader();
    I.click("table.datatableInit tbody tr:nth-child(4) td.dt-row-edit a");
    DT.waitForLoader();
    I.dontSee("Nastala neočakávaná chyba");
    DTE.waitForEditor();
    I.seeElement("#datatableInit_modal");
});

Scenario('NPE chyby po prechode na repozitar', ({ I, DTE }) => {
    //padalo otvorenie z historie kvoli NPE hodnotam
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=10");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-history-tab");
    I.wait(5);
    I.fillField("#datatableFieldDTE_Field_editorFields-history_wrapper .dt-filter-to-historySaveDate", "22.11.2018 12:14:57");
    I.click("#datatableFieldDTE_Field_editorFields-history_wrapper .dt-filtrujem-historySaveDate");
    I.wait(5);
    //overenie filtrovania podla datumov, to bol tiez bug kvoli poradiu stlpcov
    I.dontSee("04.01.2022", "#datatableFieldDTE_Field_editorFields-history_wrapper");

    I.forceClick(locate("#datatableFieldDTE_Field_editorFields-history td.dt-select-td").withText("362"));
    I.click("#datatableFieldDTE_Field_editorFields-history_wrapper button.buttons-history-edit");
    I.amAcceptingPopups();
    I.wait(5);
    //over, ze sa preplo na zalozku Obsah
    I.see("Obsah", "#pills-dt-editor-datatableInit a.nav-link.active");
});

Scenario('Stavove ikony', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");

    DT.resetTable();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");

    DT.filterSelect("editorFields.statusIcons", "Zobrazený v menu");
    I.see("Hlavná stránka adresára", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee("Nezobrazená v menu", "#datatableInit_wrapper .dataTables_scrollBody");

    DT.filterSelect("editorFields.statusIcons", "Nezobrazený v menu");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dataTables_scrollBody");
    I.see("Nezobrazená v menu", "#datatableInit_wrapper .dataTables_scrollBody");

    DT.filterSelect("editorFields.statusIcons", "Dostupné len pre prihláseného návštevníka");
    I.see("Zaheslovaná", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dataTables_scrollBody");

    DT.filterSelect("editorFields.statusIcons", "Stránka s vypnutým zobrazením");
    I.see("Stránka s vypnutým zobrazením", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dataTables_scrollBody");

    DT.filterSelect("editorFields.statusIcons", "Stránka je presmerovaná");
    I.see("Presmerovaná extrená linka", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dataTables_scrollBody");

    DT.filterSelect("editorFields.statusIcons", "Stránka sa nedá vyhľadať");
    I.see("Nevyhľadateľná", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dataTables_scrollBody");

});

Scenario('Stavove ikony - default_doc', ({ I, DT }) => {
    //hlavna stranka adresara
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=23");
    DT.waitForLoader();
    I.see("Úvodná stránka", "#datatableInit_wrapper .dataTables_scrollBody");
    I.see("Osobný bankár", "#datatableInit_wrapper .dataTables_scrollBody");

    DT.filterSelect("editorFields.statusIcons", "Hlavná stránka");
    I.see("Úvodná stránka", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee("Osobný bankár", "#datatableInit_wrapper .dataTables_scrollBody");

    //zapni aj rekurziu
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=23");
    DT.waitForLoader();
    I.wait(2);
    I.forceClick("#dtRecursiveSwitch");
    DT.waitForLoader();
    I.see("Úvodná stránka", "#datatableInit_wrapper .dataTables_scrollBody");
    I.see("Osobný bankár", "#datatableInit_wrapper .dataTables_scrollBody");
    I.see("Test podadresar", "#datatableInit_wrapper .dataTables_scrollBody")
    I.see("Nesmie sa dať presunúť", "#datatableInit_wrapper .dataTables_scrollBody")

    DT.filterSelect("editorFields.statusIcons", "Hlavná stránka");

    I.see("Úvodná stránka", "#datatableInit_wrapper .dataTables_scrollBody");
    I.dontSee("Osobný bankár", "#datatableInit_wrapper .dataTables_scrollBody");
    I.see("Test podadresar", "#datatableInit_wrapper .dataTables_scrollBody")
    I.see("Nesmie sa dať presunúť", "#datatableInit_wrapper .dataTables_scrollBody")
});

function testSaveStandard(I, DTE) {
    DTE.waitForEditor();

    I.wait(3);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    //over texty, aby ked sa posaha ulozenie to na buduce padlo
    I.dontSee("Etiam orci"); //toto je len v PB verzii
    I.see("vulputate purus");
    I.see("rutrum varius sollicitudin");
    I.see("Ut efficitur venenatis erat a facilisis.")

    I.pressKey(['CommandOrControl', 's']);
    I.switchTo();
    DTE.waitForLoader();
    I.waitForElement("div.toast.toast-success", 20);
    I.see("bol uložený", "div.toast.toast-success");
}

function testSavePB(I, DTE) {
    DTE.waitForEditor();

    I.wait(6);

    I.switchTo('#DTE_Field_data-pageBuilderIframe');

    //over texty, aby ked sa posaha ulozenie to na buduce padlo
    I.see("Etiam orci");
    I.see("rutrum varius sollicitudin");
    I.see("Ut efficitur venenatis erat a facilisis.")

    I.pressKey(['CommandOrControl', 's']);
    I.switchTo();
    I.waitForElement("div.toast.toast-success", 20);
    I.see("bol uložený", "div.toast.toast-success");
}

Scenario('Ukladanie cez CTRL+S', ({ I, DTE }) => {

    I.say("Ukladanie v editore");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");

    testSaveStandard(I, DTE);

    //zopakuj ked zatvoris okno, ci sa bindne event
    DTE.cancel();
    I.click("Produktová stránka - B verzia", "#datatableInit_wrapper");

    testSaveStandard(I, DTE);

    //over ukladanie v PB editore
    DTE.cancel();
    I.click("Produktová stránka - PageBuilder", "#datatableInit_wrapper");

    testSavePB(I, DTE);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");

    testSavePB(I, DTE);
});

Scenario('Nacitanie CSS do listy', ({ I, DTE, Browser }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=265");
    DTE.waitForEditor();

    I.wait(4);
    if (Browser.isFirefox()) I.wait(3);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.click("#WebJETEditorBody");
    I.switchTo();

    I.click(".cke_combo__styles .cke_combo_button");

    I.switchTo(".cke_panel_frame");
    I.see("baretest1");
    I.see("baretest2");
    I.see("more-info1");

    I.switchTo();
});

Scenario('BUG - editacia viacerych zaznamov naraz @singlethread', async ({ I, DT, DTE }) => {
    //pri editacii viacerych zaznamov naraz sa nespravil fetch a do data sa ulozilo data not loaded
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=221");
    DT.waitForLoader();

    var text = "webpage-content-"+randomNumber;

    //nastav strankam rovnaky text
    I.click("page-2021-02-23-134924-937");
    DT.waitForLoader();
    await DTE.fillCkeditor("<p>"+text+"</p>");
    DTE.save();

    I.click("page-2022-03-25-213117-79");
    DT.waitForLoader();
    await DTE.fillCkeditor("<p>"+text+"</p>");
    DTE.save();

    //reloadni a over nacitanie
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=221");
    DT.waitForLoader();

    I.forceClick(locate("#datatableInit td.dt-select-td").withText("2664"));
    I.forceClick(locate("#datatableInit td.dt-select-td").withText("29195"));

    I.click("button.buttons-edit", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.wait(4);

    I.dontSee("Upraviť rozdielné hodnoty");

    text = text + "-edited";

    await DTE.fillCkeditor("<p>"+text+"</p>");

    DTE.save();

    //over, ci sa to korektne ulozilo
    I.amOnPage("/novy-adresar-01/zobrazeny-menu/page-2021-02-23-134924-937.html");
    I.see(text);
    I.amOnPage("/test-stavov/zobrazeny-menu/page-2022-03-25-213117-79.html");
    I.see(text);

});


Scenario('BUG - vyhladavanie podla perex skupin', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=32");
    var container = "#datatableInit_wrapper";

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    I.click(container+" button.buttons-settings");
    I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-colvis');
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.click("Značky", container+" div.colvisbtn_wrapper");
    I.click("button.btn.colvis-postfix.btn-primary.dt-close-modal");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    I.see("News", container);
    I.see("McGregor sales force", container);
    I.see("Trhy sú naďalej vydesené", container);
    I.see(" Loyalty club: Vacation in Nha Trang", container);
    I.see("Konsolidácia naprieč trhmi", container);

    DT.filter("perexGroups", "podnika");

    I.dontSee("News", container);
    I.see("McGregor sales force", container);
    I.see("Trhy sú naďalej vydesené", container);
    I.dontSee(" Loyalty club: Vacation in Nha Trang", container);
    I.dontSee("Konsolidácia naprieč trhmi", container);

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
});

Scenario('BUG - prepinanie kariet a zobrazenie stranok', ({ I, DT }) => {
    //ked sa preplo na kartu System, nazad na Priecinky a potom System tak sa nerefreshol zoznam web stranok, zostali stare
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=1");
    DT.waitForLoader();
    I.see("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");

    I.click("#pills-system-tab");
    DT.waitForLoader();
    I.dontSee("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    I.see("System", "#datatableInit_wrapper");

    I.click("#pills-folders-tab");
    DT.waitForLoader();
    I.see("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    I.dontSee("System", "#datatableInit_wrapper");

    I.click("#pills-system-tab");
    DT.waitForLoader();
    I.dontSee("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    I.see("System", "#datatableInit_wrapper");
});

Scenario('BUG #54953-6 - pri prvom nacitani sa citalo len 10 zaznamov, musi sa refreshnut', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    I.wait(1);
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    //over, ze mame viac ako 10 riadkov
    I.see("Záznamy 1 až 11 z", "#datatableInit_wrapper .dataTables_info");
    I.see("11 stránka v Adresári", "#datatableInit_wrapper");
});

//TODO: test ukladania stranky v multi adresari, overenie, ze sa po editacii slave stranky ulozi aj master

Scenario("Check editor for user with perms only for one webpage", ({ I, DTE }) => {

    I.relogin("jtester");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=31");
    DTE.waitForEditor();

    //
    I.say("Check options loading");
    I.dontSeeElement("div.toast-message");
    I.dontSee("Pole Menu (loggedMenuType) neobsahuje možnosť s ID -1");

    //
    I.say("Check editor values");
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_title", "Home");
    I.click("#pills-dt-datatableInit-template-tab");
    I.see("Homepage", "div.DTE_Field_Name_tempId div.filter-option-inner-inner");

    DTE.cancel();
});

Scenario('odhlasenie', ({ I }) => {
    I.logout();
});