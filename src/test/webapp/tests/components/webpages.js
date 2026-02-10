Feature('components.webpages');
const assert = require('assert');
var randomNumber;
var container = "div.tree-col";
var containerPage = "#datatableInit_wrapper";

var settings_button = locate('div.dt-header-row.clearfix.wp-header-tree button.btn.btn-sm.btn-outline-secondary.buttons-jstree-settings.buttons-right');
var refresh_button = locate('div.dt-header-row.clearfix.wp-header-tree button.btn.btn-sm.btn-outline-secondary.buttons-refresh.buttons-right');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('zoznam stranok', ({ I, DT}) => {
    I.jstreeReset();
    I.waitForText("Newsletter", 20);
    I.click("Newsletter", container);
    I.see("Testovaci newsletter");

    //over prava
    DT.checkPerms("menuWebpages", "/admin/v9/webpages/web-pages-list/?groupid=0", "datatableInit");
});

Scenario('logout', async ({I}) => {
    //if previous test fail logout
    I.logout();
});

Scenario('nový adresár', ({ I, DTE }) => {
    I.waitForElement("button.buttons-create", 10, container);
    I.clickCss("button.buttons-create", container);
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
    I.clickCss("button.buttons-edit", container);
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
    I.clickCss("button.buttons-edit", container);
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
        I.clickCss("button.buttons-remove");
    });

    I.click("Zmazať");

    DTE.waitForLoader();

    I.dontSee("test-adresar-2-" + randomNumber, container);
});

Scenario('planovanie a historia', ({ I, DT, DTE }) => {
    I.jstreeNavigate(["Test stavov", "Test publikovania"]);
    I.clickCss("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");

    I.clickCss("#pills-dt-groups-datatable-publishing-tab");
    DT.waitForLoader();
    I.see("Test publikovania-2030", "#datatableFieldDTE_Field_editorFields-groupSchedulerPlannedChanges_wrapper");
    I.dontSee("Test publikovania-20180", "#datatableFieldDTE_Field_editorFields-groupSchedulerPlannedChanges_wrapper");

    I.clickCss("#pills-dt-groups-datatable-history-tab");
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
    I.clickCss("#pills-system-tab");
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
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
    I.wait(1);
    I.waitForText("asdf", 20, "#SomStromcek");
    I.dontSee("Jet portal 4", "#SomStromcek");
    I.dontSee("Newsletter", "#SomStromcek");
    I.dontSee("System", "#SomStromcek");
    I.dontSee("Hlavičky", "#SomStromcek");
    I.dontSee("Pätičky", "#SomStromcek");
    I.dontSee("Menu", "#SomStromcek");
    //over aj zobrazenie web stranok
    I.see("Test zmazania stránky", "#datatableInit_wrapper");

    //prepnit sa nazad na Priecinky zalozku
    I.clickCss("#pills-folders-tab");
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
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("domena1.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");

    //domena zmenena, over zobrazenie
    I.dontSee("Jet portal 4", container);
    I.dontSee("English", container);
    I.dontSee("Newsletter", container);
    I.see("domena1.tau27.iway.sk", container);
});

Scenario('Zmena domeny-logout', ({ I }) => {
    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});

Scenario('Overenie zobrazenia sablon podla adresarov', ({ I, DTE }) => {
    I.click("Jet portal 4", container);
    I.clickCss("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-template-tab");
    I.waitForText('Šablóna pre web stránky', 10);
    I.clickCss("div.DTE_Field_Name_tempId div.dropdown");
    I.see("Homepage", "div.dropdown-menu.show");
    I.dontSee("Newsletter", "div.dropdown-menu.show");
    I.clickCss("div.DTE_Field_Name_tempId div.dropdown");

    I.click({css: "#groups-datatable_modal div.DTE_Footer.modal-footer button.btn-close-editor"});

    I.click("Newsletter", container);
    I.clickCss("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.clickCss("#pills-dt-groups-datatable-template-tab");
    I.waitForText('Šablóna pre web stránky', 10);
    I.clickCss("div.DTE_Field_Name_tempId div.dropdown");
    I.see("Homepage", "div.dropdown-menu.show");
    I.see("Newsletter", "div.dropdown-menu.show");
    //aby sa zatvoril select box
    I.click("Newsletter", "div.dropdown-menu.show");

    I.click({css: "#groups-datatable_modal div.DTE_Footer.modal-footer button.btn-close-editor"});
});

Scenario('Overenie zalozky Naposledy Upravene', ({ I, DT }) => {
    DT.waitForLoader();
    I.see("Naposledy upravené", "#pills-pages");
    I.click("Naposledy upravené", "#pills-pages");
    DT.waitForLoader();
    //nemame prilis ine co otestovat
    I.see("Záznamy 1 až", "#datatableInit_wrapper");
});

Scenario('Overenie zalozky Na schvalenie', ({ I, DT }) => {
    DT.waitForLoader();
    DT.resetTable();
    I.waitForText("Neschválené", 10, "#pills-pages");
    I.click("Neschválené", "#pills-pages");
    DT.waitForLoader();
    DT.resetTable();
    //nemame prilis ine co otestovat
    DT.filterContains("title", "Čakajúce na schválenie-zmena titulku")
    I.waitForText("Čakajúce na schválenie-zmena titulku", 10, "#datatableInit_wrapper");

    //prepni domenu a over, ze tam nie je nic ine
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("test23.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");

    I.wait(1);

    I.dontSee("Neschválené", "#pills-pages");
});

Scenario('logout 2', ({ I }) => {
    //aby ked padol predchadzajuci test nepadli vsetky dalsie v inej domene
    I.logout();
});

Scenario('Overenie nova web stranka', ({ I, DT, DTE }) => {
    let oldValue = "Nová web stránka";
    let newValue = "autotest-title";

    I.jstreeNavigate(["Test stavov"]);
    DT.filterContains("title", oldValue);
    I.click(oldValue);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_title", oldValue);
    I.seeInField("#DTE_Field_navbar", oldValue);
    I.seeInField("#DTE_Field_virtualPath", "nova-web-stranka.html");

    //vypln title na nieco ine a sprav blur
    I.appendField('#DTE_Field_title', newValue);
    //nevedel som inak spravit blur
    I.clickCss("#pills-dt-datatableInit-template-tab");
    I.clickCss("#pills-dt-datatableInit-basic-tab");

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
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("rozbalenie.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");

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

    DT.resetTable();

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

    //
    I.say("Test filter of fullTextIndex of files");
    DT.filterContains("title", ".png");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10, "#datatableInit_wrapper div.dt-scroll-body");
    DT.clearFilter("title");

    //pridaj stlpec šablon medzi videne
    I.click({ css: 'button[data-dtbtn=settings]' });
    I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-colvis');

    I.click(locate('button').withAttr({ title: 'Zobrazenie stĺpcov' }));
    I.click(locate('span').withText('Šablóna'));
    I.forceClick("button.btn.colvis-postfix.btn-primary.dt-close-modal");
    DT.waitForLoader();

    //over ze sa natiahla spravne sablona sub foldera
    I.see("Microsite - blue");

    //rozklikni select
    I.clickCss("div.dt-scroll-headInner div.dt-filter-tempId button");
    I.see("Microsite - blue", "ul.dropdown-menu.inner.show");
    I.wait(1);

    I.clickCss("#pills-changes-tab");
    DT.waitForLoader();

    I.clickCss("#pills-folders-tab");

    //over zobrazenie sablony v tabulke
    I.see("Produktová stránka - B verzia");
    I.see("Microsite - blue");

    //
    I.say("BUG: read again to verify it will also load recursive pages");
    I.clickCss("#pills-folders-tab");
    DT.waitForLoader();

    //over zobrazenie sablony v tabulke
    I.see("Produktová stránka - B verzia");
    I.see("Microsite - blue");
});

Scenario('Kontrola subFolder dat - reset nastaveni stlpcov', ({ I, DT }) => {
    DT.resetTable();
});

Scenario('Overenie vyhladavania podla boolean a password_protected', ({ I, DT }) => {
    I.jstreeNavigate(["Newsletter"]);

    var container = "#datatableInit_wrapper";

    I.clickCss(container+" button.buttons-settings");
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");

    I.wait(1);

    I.clickCss(container+" button.buttons-settings");
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.click("Prehľadávať");
    I.click("Povoliť prístup len pre skupinu používateľov");
    I.click("Priradiť stránku k hromadnému emailu");
    I.forceClick("button.btn.colvis-postfix.btn-primary.dt-close-modal");
    I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");

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
    DT.filterContains("editorFields.emails", "Vianočné pozdravy");
    I.dontSee("Produktová stránka - B verzia", container);
    I.see("Testovaci newsletter", container);

    I.clickCss(container+" button.buttons-settings");
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
});

Scenario('Overenie vyhladania po nacitani', ({ I, DT }) => {
    //wj mal bug, ze hned po nacitani ked som zadal vyhladavanie neprenieslo korektne groupId
    DT.filterContains("authorName", "tester");
    DT.waitForLoader();
    I.see("Nenašli sa žiadne vyhovujúce záznamy", "#datatableInit_wrapper");
});

//tempFieldADocId / tempFieldBDocId / tempFieldCDocId  /tempFieldDDocId
Scenario('Otestuj nove stlpce tempFieldDocId', ({ I, DT, DTE }) => {
    var name = "Test_volnych_poli_sablony";

    //Select list in tree
    I.jstreeClick("Test stavov");
    DT.filterContains("title", name);

    //Edit entity
    I.click(name);
    DTE.waitForEditor();

    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.fillField("#DTE_Field_title", name + "_" +randomNumber);

    //Change to tab template
    I.clickCss('#pills-dt-datatableInit-template-tab');

    //Set select tempFieldADocId
    I.clickCss(".DTE_Field_Name_tempFieldADocId");
    I.click(locate('span').withText('Hlavičky/Default header'));
    I.wait(1);
    //Set select tempFieldADocId
    I.clickCss(".DTE_Field_Name_tempFieldBDocId");
    I.click(locate('span').withText('System'));
    I.wait(1);
    //Set select tempFieldADocId
    I.clickCss(".DTE_Field_Name_tempFieldCDocId");
    I.click(locate('span').withText('Hlavičky/Default header'));
    I.wait(1);
    //Set select tempFieldADocId
    I.clickCss(".DTE_Field_Name_tempFieldDDocId");
    I.click(locate('span').withText('Hlavičky/Default footer'));
    I.wait(1);

    //Save change entity
    DTE.save();
    I.wait(2);

    //Edit this entity again
    I.click(name + "_" +randomNumber);
    DTE.waitForEditor();

    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.fillField("#DTE_Field_title", name);

    //Change to tab template
    I.clickCss('#pills-dt-datatableInit-template-tab');

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
    I.clickCss('#pills-dt-datatableInit-history-tab');
    I.wait(1);
    I.waitForInvisible("#datatableFieldDTE_Field_editorFields-history_processing", 200);

    I.fillField("#datatableFieldDTE_Field_editorFields-history_wrapper input.dt-filter-title", name + "_" +randomNumber);
    I.pressKey('Enter', "input.dt-filter-key");
    //its client side paging
    I.wait(1);
    I.waitForInvisible("#datatableFieldDTE_Field_editorFields-history_processing", 200);

    I.clickCss("#datatableFieldDTE_Field_editorFields-history_wrapper button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.clickCss(".buttons-history-edit");

    //Check set values from history

    //Change to tab template
    I.waitForElement("#pills-dt-datatableInit-content-tab.nav-link.active", 10);
    I.clickCss('#pills-dt-datatableInit-template-tab');

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

Scenario('Check REQUEST object filtration', async ({ I }) => {
    I.logout();
    //after PlayWrigt update it will be easier
    /*I.setPlaywrightRequestHeaders({
        'X-Sent-By': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) <strong>test</strong>',
    });*/
    var url = '/test-stavov/test_volnych_poli_sablony.html';
    I.amOnPage(url);
    I.see("<p>Toto je system priecinok</p>", "div.template-object-b-filtered");
    I.seeInSource("<div class=\"template-object-b-filtered\">&lt;p&gt;Toto je system priecinok&lt;/p&gt;");
    I.see("Mozilla/5.0", "div.user-agent");

    var userAgentHeader = 'Mozilla/5.0 (iPhone" onMouseMove html onMouseMove="javascript:javascript:alert(\'b\')"></html onMouseMove><script>alert(\'a\')</script>" iPhone OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B176 Safari/7534.48.3';
    var userAgent = 'iPhone" onMouseMove html onMouseMove="javascript:javascript:alert(\'b\')"></html';
    var userAgentEscaped = userAgent.replace(/"/gi, "&quot;").replace(/'/gi, "&#39;").replace(/>/gi, "&gt;").replace(/</gi, "&lt;");
    var response = await I.sendGetRequest(url, {
        'User-Agent': userAgentHeader,
        'x-auth-token': ''
    });
    //console.log("response", response);
    var data = response.data;
    I.assertNotContain(data, userAgent, "Contains unescaped user agent!");
    I.assertNotContain(data, userAgent.toLowerCase(), "Contains unescaped user agent lowercase!");
    I.assertContain(data, userAgentEscaped, "Does not contain escaped user agent!");
    //BrowserDetector doesn't know this, so it should be parsed as some generic OS
    I.assertNotContain(data, userAgentEscaped.toLowerCase(), "Does contain escaped user agent lowercase in html data attributes!");
    I.assertContain(data, 'data-browser-name="mobile safari" data-browser-version="5" data-device-type="phone" data-device-os="ios 5"', "Does not contain correctly parsed User-Agent!");
});

Scenario('Otestuj nove stlpce show_in', ({ I, DT, DTE }) => {
    var name = "Test_show_in";

    //Select list in tree
    I.jstreeClick("Test stavov");
    DT.filterContains("title", name);

    //Edit entity
    I.click(name);
    DTE.waitForEditor();

    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.fillField("#DTE_Field_title", name + "_" +randomNumber);

    //Change to tab template
    I.clickCss('#pills-dt-datatableInit-menu-tab');

    //Set select showInMenu
    I.clickCss(".DTE_Field_Name_showInMenu");
    I.click(locate('span').withText('Zobraziť'));
    I.wait(2);
    //Set select showInNavbar
    I.clickCss(".DTE_Field_Name_showInNavbar");
    I.click(locate('span').withText('Nezobraziť'));
    I.wait(2);
    //Set select showInSitemap
    I.clickCss(".DTE_Field_Name_showInSitemap");
    I.click(locate('span').withText('Rovnako ako menu'));
    I.wait(2);
    //Set select loggedShowInMenu
    I.clickCss(".DTE_Field_Name_loggedShowInMenu");
    I.click(locate('span').withText('Nezobraziť'));
    I.wait(2);
    //Set select loggedShowInNavbar
    I.clickCss(".DTE_Field_Name_loggedShowInNavbar");
    I.click(locate('span').withText('Rovnako ako pre neprihláseného'));
    I.wait(2);
    //Set select loggedShowInSitemap
    I.clickCss(".DTE_Field_Name_loggedShowInSitemap");
    I.click(locate('span').withText('Zobraziť'));
    I.wait(2);

    //Save change entity
    DTE.save();

    //Edit this entity again - vrat nazad meno stranky
    I.click(name + "_" +randomNumber);
    DTE.waitForEditor();

    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.fillField("#DTE_Field_title", name);

    //Change to tab template
    I.clickCss('#pills-dt-datatableInit-menu-tab');

    //CHECK if set values in selects are correct
    I.seeElement(locate('button').withAttr({title: 'Zobraziť'}).inside(locate('.DTE_Field_Name_showInMenu')))
    I.seeElement(locate('button').withAttr({title: 'Nezobraziť'}).inside(locate('.DTE_Field_Name_showInNavbar')))
    I.seeElement(locate('button').withAttr({title: 'Rovnako ako menu'}).inside(locate('.DTE_Field_Name_showInSitemap')))
    I.seeElement(locate('button').withAttr({title: 'Nezobraziť'}).inside(locate('.DTE_Field_Name_loggedShowInMenu')))
    I.seeElement(locate('button').withAttr({title: 'Rovnako ako pre neprihláseného'}).inside(locate('.DTE_Field_Name_loggedShowInNavbar')))
    I.seeElement(locate('button').withAttr({title: 'Zobraziť'}).inside(locate('.DTE_Field_Name_loggedShowInSitemap')))

    //Save change entity
    DTE.save();
    DT.waitForLoader();
    I.jstreeWaitForLoader();

    //Edit this entity again
    I.click(name);
    DTE.waitForEditor();

    //Go to history tab
    I.clickCss('#pills-dt-datatableInit-history-tab');
    I.waitForVisible("#pills-dt-datatableInit-history", 20);
    I.wait(2);

    I.fillField("#datatableFieldDTE_Field_editorFields-history_wrapper th.dt-th-title input.dt-filter-title", name + "_" +randomNumber);
    DT.waitForLoader("#datatableFieldDTE_Field_editorFields-history_processing");
    I.forceClick("#datatableFieldDTE_Field_editorFields-history_wrapper th.dt-th-title button.dt-filtrujem-title");
    I.wait(1);

    I.clickCss("#datatableFieldDTE_Field_editorFields-history_wrapper button.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id");
    I.clickCss(".buttons-history-edit");
    I.wait(3);

    //Change to tab template
    I.clickCss('#pills-dt-datatableInit-menu-tab');
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
    I.clickCss("button.buttons-edit", container);
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
    I.clickCss("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.seeElement("div.DTE_Field_Name_domainName");
    I.seeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");

    //zmen parent na /English
    I.clickCss('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'); // zmena na korenovy adresar
    I.waitForElement("div.jsTree-wrapper");
    I.wait(1);
    I.waitForText('English', 5);
    I.wait(1);
    I.click('English', 'div.jsTree-wrapper');
    I.wait(1);

    I.dontSeeElement("div.DTE_Field_Name_domainName");
    I.dontSeeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");

    //test otvorenim priamo editora
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=23");
    DT.waitForLoader();
    I.clickCss("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.dontSeeElement("div.DTE_Field_Name_domainName");
    I.dontSeeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=1");
    DT.waitForLoader();
    I.clickCss("button.buttons-edit", container);
    DTE.waitForEditor("groups-datatable");
    I.seeElement("div.DTE_Field_Name_domainName");
    I.seeElement("div.DTE_Field_Name_editorFields\\.forceDomainNameChange");
});

Scenario('Overenie zobrazenia tooltipov', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=11");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");

    I.dontSee("Správny titulok vystihuje podstatu stránky");
    I.moveCursorTo("div.DTE_Field_Name_title button.btn-tooltip");
    I.see("Správny titulok vystihuje podstatu stránky", "div.tooltip.show");

    //over konverziu Markdown na HTML kod
    I.clickCss("#pills-dt-datatableInit-template-tab");
    I.scrollTo('div.DTE_Field_Name_htmlHead');
    I.dontSee("Voliteľne môžete zadať HTML kód, ktorý sa priamo vloží do HTML kódu web stránky.");
    I.moveCursorTo("div.DTE_Field_Name_htmlHead button.btn-tooltip");
    I.see("html_head", "div.tooltip.show i");
});

Scenario('nejde otvorit stranku z Naposledy upravene', ({ I, DT, DTE }) => {
    //neslo otvorit stranku z Naposledy upravene kvoli groupId=999997 co je NULL
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    DT.waitForLoader();
    I.clickCss("#pills-changes-tab");
    DT.waitForLoader();
    I.clickCss("table.datatableInit tbody tr:nth-child(4) td.dt-row-edit a");
    DT.waitForLoader();
    I.dontSee("Nastala neočakávaná chyba");
    DTE.waitForEditor();
    I.seeElement("#datatableInit_modal");
});

Scenario('NPE chyby po prechode na repozitar', ({ I, DTE }) => {
    //padalo otvorenie z historie kvoli NPE hodnotam
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=10");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-history-tab");
    I.wait(5);
    I.fillField("#datatableFieldDTE_Field_editorFields-history_wrapper .dt-filter-to-historySaveDate", "22.11.2018 12:14:57");
    I.clickCss("#datatableFieldDTE_Field_editorFields-history_wrapper .dt-filtrujem-historySaveDate");
    I.wait(5);
    //overenie filtrovania podla datumov, to bol tiez bug kvoli poradiu stlpcov
    I.dontSee("04.01.2022", "#datatableFieldDTE_Field_editorFields-history_wrapper");

    I.forceClick(locate("#datatableFieldDTE_Field_editorFields-history td.dt-select-td").withText("362"));
    I.clickCss("#datatableFieldDTE_Field_editorFields-history_wrapper button.buttons-history-edit");
    I.amAcceptingPopups();
    I.wait(5);
    //over, ze sa preplo na zalozku Obsah
    I.see("Obsah", "#pills-dt-editor-datatableInit a.nav-link.active");
});

Scenario('Stavove ikony', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");

    DT.resetTable();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    DT.waitForLoader();

    DT.filterSelect("editorFields.statusIcons", "Zobrazený v menu");
    I.see("Hlavná stránka adresára", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee("Nezobrazená v menu", "#datatableInit_wrapper .dt-scroll-body");

    DT.filterSelect("editorFields.statusIcons", "Nezobrazený v menu");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dt-scroll-body");
    I.see("Nezobrazená v menu", "#datatableInit_wrapper .dt-scroll-body");

    DT.filterSelect("editorFields.statusIcons", "Dostupné len pre prihláseného návštevníka");
    I.see("Zaheslovaná", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dt-scroll-body");

    DT.filterSelect("editorFields.statusIcons", "Stránka s vypnutým zobrazením");
    I.see("Stránka s vypnutým zobrazením", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dt-scroll-body");

    DT.filterSelect("editorFields.statusIcons", "Stránka je presmerovaná");
    I.see("Presmerovaná extrená linka", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dt-scroll-body");

    DT.filterSelect("editorFields.statusIcons", "Stránka sa nedá vyhľadať");
    I.see("Nevyhľadateľná", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee("Hlavná stránka adresára", "#datatableInit_wrapper .dt-scroll-body");

});

Scenario('Stavove ikony - default_doc', ({ I, DT }) => {
    //hlavna stranka adresara
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=23");
    DT.waitForLoader();
    I.see("Úvodná stránka", "#datatableInit_wrapper .dt-scroll-body");
    I.see("Osobný bankár", "#datatableInit_wrapper .dt-scroll-body");

    DT.filterSelect("editorFields.statusIcons", "Hlavná stránka");
    I.see("Úvodná stránka", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee("Osobný bankár", "#datatableInit_wrapper .dt-scroll-body");

    //zapni aj rekurziu
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=23");
    DT.waitForLoader();
    I.wait(2);
    I.forceClick("#dtRecursiveSwitch");
    DT.waitForLoader();
    I.see("Úvodná stránka", "#datatableInit_wrapper .dt-scroll-body");
    I.see("Osobný bankár", "#datatableInit_wrapper .dt-scroll-body");
    I.see("Test podadresar", "#datatableInit_wrapper .dt-scroll-body")
    I.see("Nesmie sa dať presunúť", "#datatableInit_wrapper .dt-scroll-body")

    DT.filterSelect("editorFields.statusIcons", "Hlavná stránka");

    I.see("Úvodná stránka", "#datatableInit_wrapper .dt-scroll-body");
    I.dontSee("Osobný bankár", "#datatableInit_wrapper .dt-scroll-body");
    I.see("Test podadresar", "#datatableInit_wrapper .dt-scroll-body")
    I.see("Nesmie sa dať presunúť", "#datatableInit_wrapper .dt-scroll-body")
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
    I.clickCss("#WebJETEditorBody");
    I.switchTo();

    I.clickCss(".cke_combo__styles .cke_combo_button");

    I.switchTo(".cke_panel_frame");
    I.see("baretest1");
    I.see("Bare TEST 02 bold");
    I.see("Bare TEST 03");
    I.see("Bare TEST 04");
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
    I.wait(1);

    I.forceClick(locate("#datatableInit td.dt-select-td").withText("2664"));
    I.forceClick(locate("#datatableInit td.dt-select-td").withText("29195"));

    I.clickCss("#datatableInit_wrapper div.dt-buttons button.buttons-edit");
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

    I.clickCss(container+" button.buttons-settings");
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");

    I.clickCss(container+" button.buttons-settings");
    I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-colvis');
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.click("Značky", container+" div.colvisbtn_wrapper");
    I.forceClick("button.btn.colvis-postfix.btn-primary.dt-close-modal");
    I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");

    I.see("News", container);
    I.see("McGregor sales force", container);
    I.see("Trhy sú naďalej vydesené", container);
    I.see(" Loyalty club: Vacation in Nha Trang", container);
    I.see("Konsolidácia naprieč trhmi", container);

    DT.filterContains("perexGroups", "podnika");

    I.dontSee("News", container);
    I.see("McGregor sales force", container);
    I.see("Trhy sú naďalej vydesené", container);
    I.dontSee(" Loyalty club: Vacation in Nha Trang", container);
    I.dontSee("Konsolidácia naprieč trhmi", container);

    I.clickCss(container+" button.buttons-settings");
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
});

Scenario('BUG - prepinanie kariet a zobrazenie stranok', ({ I, DT }) => {
    //ked sa preplo na kartu System, nazad na Priecinky a potom System tak sa nerefreshol zoznam web stranok, zostali stare
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=1");
    DT.waitForLoader();
    I.see("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    I.dontSeeElement(locate("#SomStromcek a.jstree-anchor").withText("files"));

    I.clickCss("#pills-system-tab");
    DT.waitForLoader();
    I.dontSee("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    I.see("System", "#datatableInit_wrapper");
    I.seeElement(locate("#SomStromcek a.jstree-anchor").withText("files"));

    I.clickCss("#pills-folders-tab");
    DT.waitForLoader();
    I.see("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    I.dontSee("System", "#datatableInit_wrapper");
    I.dontSeeElement(locate("#SomStromcek a.jstree-anchor").withText("files"));

    I.clickCss("#pills-system-tab");
    DT.waitForLoader();
    I.dontSee("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    I.see("System", "#datatableInit_wrapper");
    I.seeElement(locate("#SomStromcek a.jstree-anchor").withText("files"));
});

Scenario('BUG #54953-6 - pri prvom nacitani sa citalo len 10 zaznamov, musi sa refreshnut', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    I.wait(1);
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    //over, ze mame viac ako 10 riadkov
    I.see("Záznamy 1 až 12 z", "#datatableInit_wrapper .dt-info");
    I.see("11 stránka v Adresári", "#datatableInit_wrapper");
});

//TODO: test ukladania stranky v multi adresari, overenie, ze sa po editacii slave stranky ulozi aj master

Scenario("Check editor for user with perms only for one webpage", ({ I, DT, DTE }) => {

    I.relogin("jtester");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.jstreeWaitForLoader();

    I.see("English", "#SomStromcek a.jstree-anchor");
    I.see("Home", "#SomStromcek a.jstree-anchor");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=31");
    DTE.waitForEditor();

    //
    I.say("Check options loading");
    I.dontSeeElement("div.toast-message");
    I.dontSee("Pole Menu (loggedMenuType) neobsahuje možnosť s ID -1");

    //
    I.say("Check editor values");
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_title", "Home");
    I.clickCss("#pills-dt-datatableInit-template-tab");
    I.see("Homepage", "div.DTE_Field_Name_tempId div.filter-option-inner-inner");

    DTE.cancel();

    //
    I.say("Verify domain name selector");
    I.see(I.getDefaultDomainName(), ".js-domain-toggler .filter-option-inner-inner");
});

Scenario("logout jtester", ({ I }) => {
    I.logout();
});

Scenario("Group doc name change based on another", async ({ I, DT, DTE }) => {
    let parentGroup = "pg-autotest-" + randomNumber;
    let childGroup = "cg-autotest-" + randomNumber;

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=84");
    DT.waitForLoader();

    I.say("Create parent group");
        I.click( locate("div.tree-col").find("button.buttons-create") );
        DTE.waitForEditor("groups-datatable");
        I.fillField("#DTE_Field_groupName", parentGroup);
        DTE.save();

    I.say("Edit parent group DOC");
        editDoc(I, DT, DTE, parentGroup);
        changeDocName(I, DTE, parentGroup + "_1");

    I.say("Select changed parent group (NEW name)");
        I.jstreeClick(parentGroup + "_1");
        const parentGroupId = await I.grabValueFrom("#tree-doc-id");

    I.say("Create child group");
        I.click( locate("div.tree-col").find("button.buttons-create") );
        DTE.waitForEditor("groups-datatable");
        I.fillField("#DTE_Field_groupName", childGroup);
        DTE.save();

        I.jstreeClick(childGroup);
        editGroup(I, DT, DTE, childGroup);
        DTE.cancel();
        const childGroupId = await I.grabValueFrom("#tree-doc-id");

    I.say("Set parent group DOC as main for child group");
        editGroup(I, DT, DTE, childGroup);
        changeGroupMainDoc(I, ["test", parentGroup + "_1"], parentGroupId)
        DTE.save();

    I.say("NOW test, that change of DOC does NOT change group name");
    I.say("Because 1 page is main page to more groups");
        editDoc(I, DT, DTE, parentGroup + "_1");
        changeDocName(I, DTE, parentGroup + "_2");

        I.say("Check gropups names");
        I.jstreeClick(parentGroup + "_1");
        I.jstreeClick(childGroup);

        I.say("Change group name and check that DOC name does not change");
        editGroup(I, DT, DTE, parentGroup + "_1");

        I.wait(1);
        I.clickCss("#DTE_Field_groupName");
        I.fillField("#DTE_Field_groupName", parentGroup + "_3");
        DTE.save();
        //Checks
        I.jstreeClick(parentGroup + "_3");
        within("#datatableInit_wrapper", () => { I.see( parentGroup + "_2") });


    I.say("NOW set parentGroup main DOC as childGroup DOC");
    I.say("In this case, there is no multiple groups with same DOC as main BUT main DOC is in another group that group itself");
        I.say("Change parent group DOC to child group DOC");
        editGroup(I, DT, DTE, parentGroup + "_3");
        changeGroupMainDoc(I, ["test", parentGroup + "_3", childGroup], childGroupId);
        DTE.save();

        I.say("Switch to child group and check that DOC name is NOT changed");
        I.jstreeClick(childGroup);
        within("#datatableInit_wrapper", () => { I.see( childGroup ) });

        I.say("Change child group DOC name");
        editDoc(I, DT, DTE, childGroup);
        changeDocName(I, DTE, childGroup + "_1");

        I.say("DO parent group name change");
        I.jstreeClick(parentGroup + "_3");
        editGroup(I, DT, DTE, parentGroup + "_3");
        I.wait(1);
        I.clickCss("#DTE_Field_groupName");
        I.fillField("#DTE_Field_groupName", parentGroup + "_4");
        DTE.save();

        I.jstreeClick(parentGroup + "_4");
        within("#datatableInit_wrapper", () => { I.see( parentGroup + "_2") });

        I.jstreeClick(childGroup);
        within("#datatableInit_wrapper", () => { I.see( childGroup ) });

    I.say("DELETE THIS GROUPS");
        I.jstreeClick(parentGroup + "_4");
        I.click( locate("div.tree-col").find("button.buttons-remove") );
        I.waitForElement("div.DTE_Action_Remove");
        I.click("Zmazať", "div.DTE_Action_Remove");
});

function editDoc (I, DT, DTE, groupName) {
    DT.waitForLoader();
    I.jstreeClick(groupName);
    DT.waitForLoader();
    I.click( locate("#datatableInit_wrapper").find("button.buttons-select-all") );
    I.click( locate("#datatableInit_wrapper").find("button.buttons-edit") );
    DTE.waitForEditor();
}

function editGroup (I, DT, DTE, groupName) {
    DT.waitForLoader();
    I.jstreeClick(groupName);
    DT.waitForLoader();
    I.click( locate("div.tree-col").find("button.buttons-edit") );
    DTE.waitForEditor("groups-datatable");
}

function changeDocName(I, DTE, newDocName) {
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.fillField("#DTE_Field_title", newDocName);
    DTE.save();
}

function changeGroupMainDoc(I, arr, id) {
    within("#groups-datatable_modal > div.modal-dialog", () => {
        I.waitForElement( locate("#editorAppDTE_Field_editorFields-defaultDocDetails").find("button.btn-vue-jstree-item-edit") );
        I.click( locate("#editorAppDTE_Field_editorFields-defaultDocDetails").find("button.btn-vue-jstree-item-edit") );
    });

    I.waitForElement("#jsTree");
    for(let i = 0; i < arr.length; i++) {
        I.click(locate('#jsTree .jstree-node.jstree-closed').withText(arr[i]).find('.jstree-icon.jstree-ocl'));
        I.wait(1);
        I.waitForInvisible("#jsTree li.jstree-loading");
    }
    I.clickCss("#docId-" + id + "_anchor");
}

Scenario('odhlasenie', ({ I }) => {
    I.logout();
});

/**
 * there was bug generating data_asc for empty page with title B,
 * data_asc has &nbsp; so <h1> was not generated because data has allready letter b,
 * then &nbsp; was removed so it was empty
 */
Scenario('BUG - webpage with name B will have error data_asc cannot be null #56393-15', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=104246");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    DTE.save();
    I.dontSee("Could not commit JPA transaction", "div.DTE_Form_Error");
    I.waitForModalClose("datatableInit_modal");
});

Scenario('Jstree filtering', async ({ I }) => {
    const pageName = 'Zo sveta financií';

    //
    I.say('1. I can filter the folder by name.');
    I.jstreeFilter('sveta');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText(pageName));
    I.seeElement(locate('.jstree-anchor').withText('Jet portal 4'));

    //
    I.say('2. Change search type functionality')
    I.amOnPage('/admin/v9/webpages/web-pages-list/');

    I.jstreeFilter('zo', 'Začína na');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText(pageName));
    I.seeElement(locate('.jstree-anchor').withText('Jet portal 4'));
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('Zobrazený v menu'));
    I.seeElement(locate('.jstree-anchor').withText('Test stavov'));

    I.jstreeFilter('financií', 'Končí na');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText(pageName));
    I.seeElement(locate('.jstree-anchor').withText('Jet portal 4'));

    I.jstreeFilter(pageName, 'Rovná sa');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText(pageName));
    I.seeElement(locate('.jstree-anchor').withText('Jet portal 4'));

    //
    I.say("2.a Check filtering accent insensitive");
    I.jstreeFilter('galeria', 'Obsahuje');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText("Galéria"));
    I.seeElement(locate('.jstree-anchor').withText('Aplikácie'));
    I.dontSeeElement(locate('.jstree-anchor').withText('Jet portal 4'));

    I.jstreeFilter('sveta', 'Obsahuje');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText(pageName));
    I.seeElement(locate('.jstree-anchor').withText('Jet portal 4'));

    //
    I.say('3. All filtering is cleared');
    I.jstreeClick("Zo sveta financií");
    I.jstreeFilter('');
    I.seeElement(locate('.jstree-node.jstree-open .jstree-node a.jstree-clicked').withText("Zo sveta financií"));

    I.jstreeFilter('sveta');
    I.seeElement(locate('.jstree-anchor').withText('Jet portal 4'));
    I.clickCss('#tree-folder-search-clear-button');
    I.waitForInvisible("div.dt-processing", 40);
    I.dontSeeInField('#tree-folder-search-input', pageName);
    I.seeElement(locate('.jstree-node.jstree-open').withText("Jet portal 4"));

    I.say('4. When switching between Folders/System/Recycle Bin, their data MUST be separated correctly.');

    I.clickCss('#pills-folders-tab');
    I.jstreeFilter(pageName);
    I.seeElement(locate('.jstree-anchor.jstree-search').withText(pageName));
    I.jstreeFilter('Pätičky');
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText('Pätičky'));

    I.clickCss('#pills-system-tab');
    I.jstreeFilter(pageName);
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText(pageName));
    I.jstreeFilter('Pätičky');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('Pätičky'));

    I.clickCss('#pills-trash-tab');
    I.jstreeFilter(pageName);
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText(pageName));
    I.jstreeFilter('Pätičky');
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText('Pätičky'));
    I.jstreeFilter("Aktualita_");
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('Aktualita_A'));
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('Aktualita_B'));

    I.say("5. When filtering in the Folders tab and switching to the Trash tab, the filter must be cleared and the input should be empty.");
    I.jstreeFilter(pageName);
    I.clickCss('#pills-trash-tab');
    I.dontSeeInField('#tree-folder-search-input', pageName);
    I.dontSeeElement('.jstree-node.jstree-open');

    I.say('6. new Loader');
    I.seeCssPropertiesOnElements('.webjetAnimatedLoader', { display: 'none' });

    I.say('7. Check that when filtering in the System/Recycle Bin cards, they dont go out of hierarchy');
    I.clickCss('#pills-system-tab');

    I.jstreeFilter('Pätičky');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('Pätičky'));
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText('System'));

    I.jstreeFilter('test-move-dir');
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('test-move-dir'));
    I.say("Cant see parent folder because it's in another tab, specail case")
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText('Jet portal 4'));

    I.clickCss('#pills-trash-tab');
    I.jstreeFilter('test');
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText('Kôš'));
    I.dontSeeElement(locate('.jstree-anchor.jstree-search').withText('System'));
    I.seeElement(locate('.jstree-anchor.jstree-search').withText('subone-autotest'));
});
Scenario('Set default ordering setting before', ({ I }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    setTreeSorting(I, 'Názov', true);
    setCheckBox(I, '#jstree-settings-showorder', false);
    I.relogin('tester4');
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    setCheckBox(I, '#jstree-settings-showid', false);
});

Scenario('jstree - sorting method', async ({ I }) => {
    I.say("1. The tree correctly sorts when changing the order type and direction.");
    I.amOnPage('/admin/v9/webpages/web-pages-list/');

    setTreeSorting(I, 'Názov', true);
    var items = await I.grabTextFromAll('#SomStromcek > ul > li.jstree-closed');
    assertIfSortedByName(items, true);

    setTreeSorting(I, 'Názov', false);
    items = await I.grabTextFromAll('#SomStromcek > ul > li.jstree-closed');
    assertIfSortedByName(items, false);

    I.say("2. Order selection remains saved after refreshing the page");
    setTreeSorting(I, 'Poradie usporiadania', false);

    checkTreeSorting(I, 'Poradie usporiadania', false, '#jstree-settings-showorder');
    items = await I.grabTextFromAll('#SomStromcek > ul > li.jstree-closed');
    assertIfSortedByPriority(items, false);

    I.say("3. Create a new user, and verify on him that when he switches between him and the tester, order selection remains saved");

    I.relogin('tester4');
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    setTreeSorting(I, 'Dátum vytvorenia', true);

    I.relogin('admin');
    checkTreeSorting(I, 'Poradie usporiadania', false, '#jstree-settings-showorder');
    items = await I.grabTextFromAll('#SomStromcek > ul > li.jstree-closed');
    assertIfSortedByPriority(items, false);
    setCheckBox(I, '#jstree-settings-showorder', false);

    I.relogin('tester4');
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    checkTreeSorting(I, 'Dátum vytvorenia', true, '#jstree-settings-showid');
    items = await I.grabTextFromAll('#SomStromcek > ul > li.jstree-closed');
    assertIfSortedByCreationDate(items, true);
    setCheckBox(I, '#jstree-settings-showid', false);
});

Scenario('Set default ordering setting after', ({ I }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.jstreeReset();
    I.relogin('tester4');
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.jstreeReset();
});

Scenario('Inserting various links through the dialog', ({ I, DTE }) => {
    const links = [
        '/en/',
        'http://www.interway.sk',
        'https://www.webjetcms.com',
        'https://eur-lex.europa.eu/legal-content/CS/TXT/HTML/?uri=OJ:L_202401991'
    ];

    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=112027');
    DTE.waitForEditor();

    for (const input of links) {
        I.clickCss('#cke_44');
        I.switchTo('#wjLinkIframe');
        I.wait(1); //necessary static waiting
        I.waitForElement('#txtUrl', 10);
        I.fillField('#txtUrl', input);
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok');

        I.clickCss('#datatableInit_modal button.btn.btn-warning.btn-preview');
        I.switchToNextTab();
        I.see(input);
        I.seeInSource(input);
        I.seeInSource(`href="${input}"`);
        I.switchToPreviousTab();
        I.closeOtherTabs();
        I.switchTo();
    }
});


function setCheckBox(I, optionToCheck, value) {
    I.click(settings_button);
    I.waitForVisible('#jstreeSettingsModal');
    if (value)
        I.checkOption(optionToCheck);
    else
        I.uncheckOption(optionToCheck);
    I.clickCss('#jstree-settings-submit');
    I.waitForInvisible('#jstreeSettingsModal');
}

function setTreeSorting(I, category, ascending = true) {
    I.click(settings_button);
    I.waitForVisible('#jstreeSettingsModal');
    I.clickCss('button[data-id="jstree-settings-treeSortType"]');
    I.click(category);
    if (ascending)
        I.checkOption('#jstree-settings-treeSortOrderAsc');
    else
        I.uncheckOption('#jstree-settings-treeSortOrderAsc');
    I.clickCss('#jstree-settings-submit');
    I.forceClick(refresh_button);
}

function checkTreeSorting(I, category, ascending, optionToCheck) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.click(settings_button);
    I.waitForVisible('#jstreeSettingsModal');
    I.see(category);
    if (ascending)
        I.seeCheckboxIsChecked('#jstree-settings-treeSortOrderAsc');
    else
        I.dontSeeCheckboxIsChecked('#jstree-settings-treeSortOrderAsc');
    I.checkOption(optionToCheck);
    I.clickCss('#jstree-settings-submit');
    I.waitForInvisible('#jstreeSettingsModal');
    I.forceClick(refresh_button);
}

function assertIfSortedByName(items, ascending = true) {
    const filteredItems = items.filter(item => !item.startsWith(' '));
    const sortedItems = [...filteredItems].sort((a, b) => {
        return ascending ? a.localeCompare(b) : b.localeCompare(a);
    });
    assert.deepStrictEqual(filteredItems, sortedItems, 'Items are not sorted alphabetically');
}

function assertIfSortedByPriority(items, ascending = true) {
    const filteredItems = items.filter(item => !item.startsWith(' '));
    const sortedItems = [...filteredItems].sort((a, b) => {
        const priorityA = parseInt(a.match(/\((\d+)\)/)[1], 10);
        const priorityB = parseInt(b.match(/\((\d+)\)/)[1], 10);
        return ascending ? priorityA - priorityB : priorityB - priorityA;
    });
    assert.deepStrictEqual(filteredItems, sortedItems, 'Items are not sorted by priority');
}

function assertIfSortedByCreationDate(items, ascending = true) {
    const filteredItems = items.filter(item => !item.startsWith(' '));
    const sortedItems = [...filteredItems].sort((a, b) => {
        const dateA = parseInt(a.match(/^#(\d+)/)[1], 10);
        const dateB = parseInt(b.match(/^#(\d+)/)[1], 10);
        return ascending ? dateA - dateB : dateB - dateA;
    });

    assert.deepStrictEqual(filteredItems, sortedItems, 'Items are not sorted by creation date');
}

