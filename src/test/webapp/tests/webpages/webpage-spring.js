Feature('webpages.webpage-spring');

var datatableName = "webpagesDatatable";

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
});

Scenario('Zoznamu web stranok + overenie stranky s docId 4', ({ I, DT }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    //Rozklikni Jet portal 4
    I.click(locate('.jstree-node.jstree-closed').withText('Jet portal 4').find('.jstree-icon.jstree-ocl'));

    //Uvodna stranka
    I.click("Úvodná stránka");

    //Over ze su tam stranky
    I.see("Úvodná stránka");
    I.see("Osobný bankár");
    I.see("Banner prihlásený");
    I.see("Banner neprihlásený");

    //Vyfiltruj a over nacitanie editora pre stranku s docId 4 ("Úvodná stránka")
    I.fillField({css: "input.dt-filter-title"}, "Úvodná stránka");
    I.pressKey('Enter', "input.dt-filter-name");

    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-edit");
    DT.waitForLoader();
});

//Not sure how check this
// Scenario('Serverove strankovanie', ({ I }) => {
//     I.click('.dt-filter-id');
//     let info = "kokos";
//     info = I.grabValueFrom('#datatableInit_info');
//     const numOfElements = I.grabNumberOfVisibleElements('tr');
//     I.say(numOfElements);
//     I.say(info);
// });

Scenario('Vyhladavanie v poliach @singlethread', ({ I, DT }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    //Zobraz všetky stĺpce
    var container = "#datatableInit_wrapper";

    I.clickCss(container+" button.buttons-settings");
    I.wait(1);
    I.clickCss(container+" button.buttons-colvis");
    I.waitForElement(locate("button").withText("Zobraziť všetky"));
    I.click(locate("button").withText("Zobraziť všetky"));
    I.wait(5);
    I.forceClick({css: "button.btn.colvis-postfix.btn-primary.dt-close-modal"});

    DT.waitForLoader();

    //Kôs adresár lebo má veľa web stránok kde môžem testovať filtre
    I.clickCss("#pills-trash-tab");
    I.wait(5);
    DT.waitForLoader();

    //WEB STRÁNKY - tab
    I.fillField({css: "input.dt-filter-title"}, "page");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-from-dateCreated"}, "28.10.2021");
    I.fillField({css: "input.dt-filter-to-dateCreated"}, "29.10.2021");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-navbar"}, "page");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-virtualPath"}, "name-autotest-2021-10-28-115943-684");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();

    I.see("name-autotest-2021-10-28-115943-684")

    //NAPOSLEDY UPRAVENE - tab
    I.click("Naposledy upravené", "#pills-pages");

    I.fillField({css: "input.dt-filter-title"}, "page");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-from-dateCreated"}, "21.03.2022");
    I.fillField({css: "input.dt-filter-to-dateCreated"}, "29.03.2022");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-virtualPath"}, "page-2022-03-27-213028-11.html");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-from-publishStartDate "}, "23.2.2021");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-navbar"}, "web");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();

    I.see("page-2022-03-27-213028-11");

    //CAKAJUCE NA SCHVALENIE - tab
    I.click("Čakajúce na schválenie", "#pills-pages");

    I.fillField({css: "input.dt-filter-title"}, "Čakajúce");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-from-dateCreated"}, "19.04.2021");
    I.fillField({css: "input.dt-filter-to-dateCreated"}, "20.04.2021");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-virtualPath"}, "novy-adresar-01/cakajuce-schvalenie");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-from-publishStartDate "}, "19.04.2021");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();
    I.fillField({css: "input.dt-filter-navbar"}, "Čakajúce na schválenie-zmena titulku");
    I.pressKey('Enter', "input.dt-filter-name");
    DT.waitForLoader();

    I.see("Čakajúce na schválenie-zmena titulku");

    //resetni nastavenia
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    I.click({ css: 'button[data-dtbtn=settings]' });
    I.waitForVisible('.btn.buttons-collection.dropdown-toggle.buttons-colvis');

    I.click(locate('button').withAttr({ title: 'Zobrazenie stĺpcov' }));
    I.wait(2);
    I.forceClick({css: "#datatableInit_wrapper button.buttons-colvisRestore"});
    I.wait(5);
    DT.waitForLoader();
});

Scenario('Odhlasenie after page crash', ({ I }) => {
    I.logout();
});

Scenario('Overenie funkcionalit stranok na frontende', ({ I }) => {

    I.amOnPage("/zo-sveta-financii");

    //--------Stranka zo sveta financii-----------------
    I.see("Zo sveta financií"); //Nadpis

    //Konsolidacia napriec trhmi - clanok
    I.click("Konsolidácia naprieč trhmi");
    I.see("KONSOLIDÁCIA NAPRIEČ TRHMI"); //Nadpis
    I.see("DLHOPISOVÉ TRHY");
    I.see("KOMODITNÉ TRHY");
    I.see("DEVÍZOVÉ TRHY");

    I.click("Zo sveta financií");

    //Graf týždňa: Svetové akciové indexy majú vyšší podiel klesajúcich aktív - clanok
    I.click("Graf týždňa: Svetové akciové indexy majú vyšší podiel klesajúcich aktív");
    I.see("GRAF TÝŽDŇA: SVETOVÉ AKCIOVÉ INDEXY MAJÚ VYŠŠÍ PODIEL KLESAJÚCICH AKTÍV"); //Nadpis

    I.click("Zo sveta financií");

    //Trhy sú naďalej vydesené - clanok
    I.click("Trhy sú naďalej vydesené");
    I.see("TRHY SÚ NAĎALEJ VYDESENÉ"); //Nadpis

    I.click("Zo sveta financií");

    //Otestuj top menu
    I.click("Úvod");
    I.click("Zo sveta financií");
    I.click("Kontakt");
    I.see("KONTAKT");
    I.click("Zo sveta financií");

    //McGregorov obchodný úder - clanok
    I.wait(5);
    I.click("McGregorov obchodný úder");
    I.see("MCGREGOROV OBCHODNÝ ÚDER"); //Nadpis

    //----------Uvod-----------------
    I.click("Úvod");

    I.waitForText("Matej Pavlík", 10); //nasi bankari
    I.see("Matej Pavlík"); //nasi bankari

    //Test generovaneho menu
    I.click("Konsolidácia naprieč trhmi");
    I.wait(2);
    I.click("Úvod");
    I.wait(2);
    I.forceClick("Graf týždňa: Svetové akciové indexy majú vyšší podiel klesajúcich aktív");
    I.wait(2);
    I.click("Úvod");

    //----------Kontakt-----------------
    I.click("Kontakt");

    I.see("KONTAKT"); //Nadpis

    I.see("Meno a priezvisko");

    //----------Produktová stránka-----------------
    I.click("Produktová stránka");

    I.see("Etiam sit amet");
});

/**
 * Existuje bug, ked kliknem na Naposledy upravene, zadam vyhladavanie, potom Web stranky a
 * prepnem zobrazenie z pod adresarov setne sa groupId na -1
 */
Scenario('Overenie chyby nastavenia zaporneho groupId', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    I.clickCss("#pills-changes-tab");
    DT.waitForLoader();
    DT.filter("title", "page-2021-02-23-134924-937");

    I.see("page-2021-02-23-134924-937", "table.datatableInit tbody");

    I.clickCss("#pills-pages-tab");

    I.dontSee("page-2021-02-23-134924-937", "table.datatableInit tbody");

    I.forceClick({css: '#dtRecursiveSwitch'});
    DT.waitForLoader();

    I.dontSeeElement("div.toast-title");
    I.dontSee("page-2021-02-23-134924-937", "table.datatableInit tbody");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Pole typu select-editable', ({ I, DT, DTE }) => {
    var randomNumber = I.getRandomTextShort();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");

    DT.waitForLoader();
    DT.filter("title", "vnorenu editaciu");
    DT.waitForLoader();

    I.click("Test zmeny sablony cez vnorenu editaciu");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-template-tab");
    I.wait(2);

    I.clickCss("div.DTE_Field_Name_tempId button.btn-edit");

    I.waitForElement("#modalIframe");
    I.wait(4);
    I.switchTo("#modalIframeIframeElement");
    DTE.waitForEditor();

    var newTempName = "Test sablona select-editable-"+randomNumber;

    I.seeInField("#DTE_Field_tempName", "Test sablona select-editable");
    DTE.fillField("tempName", newTempName);

    I.switchTo();
    I.wait(2);
    I.clickCss("#modalIframe div.modal-footer button.btn-primary");
    I.wait(5);

    I.see(newTempName, "div.DTE_Field_Name_tempId div.filter-option div.filter-option-inner-inner");

    //BUG: ked dam editovat sablonu a potom chcem zmenit hodnotu pola, neda sa (pokazeny reload dat)
    DTE.selectOption("tempId", "Generic");
    I.see("Generic", "div.DTE_Field_Name_tempId div.filter-option div.filter-option-inner-inner");

    DTE.selectOption("tempId", newTempName);
    I.see(newTempName, "div.DTE_Field_Name_tempId div.filter-option div.filter-option-inner-inner");
});