Feature('webpages.webpage-opener');

let docids = [
        13, //bezna web stranka
        3, //system hlavicky
        9780, //priamo zo system priecinka stranka System - test otvorenia z rootu karty
        5162, //stranka v roote kosa
        5905,  //prepnutie domeny na test23
        8486, //Test stavov - stranka je na druhej strane
        27827, //Kos stranka na 5 strane
        21345, //Kos podadresar asd
        5162, //znova stranka v roote kosa, lebo bol bug, ze ked bol v podadresari, tak nenasiel v roote
        64425, //web stranka v kosi na Xtej stranke
    ];
let index;
const input = '#tree-doc-id';

Before(({ I, login }) => {
    login('admin');
});

Scenario('Otvorenie web stranok zadanim docid do pola', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    I.say('Input a alert element už musia byť vyrenderované');
    I.waitForElement(input);

    for (index = 0; index < docids.length; index++) {
        let docid = docids[index];
        I.say('Otvaram stranku zadanim do pola '+docid);
        I.fillField(input, docid);
        I.pressKey('Enter');

        DTE.waitForEditor();
        I.wait(2);

        DTE.cancel();
    }
});

Scenario('Otvorenie web stranok cez URL', ({ I, DTE }) => {
    for (index = 0; index < docids.length; index++) {
        let docid = docids[index];
        I.say('Otvaram stranku cez URL '+docid);

        I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docid);

        DTE.waitForEditor();
    }
});

Scenario('odhlasenie', ({I}) => {
    //lebo sme pred tym v teste zmenili domenu
    I.logout();
});


Scenario('Overenie otvarania system a trash priecinka novej stranky', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=TRASH&docid=-1");
    DTE.waitForEditor();
    //skontroluj adresar novej stranky a potom aj stromovu strukturu
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#editorAppDTE_Field_editorFields-groupDetails input", "/System/Kôš");
    DTE.cancel();
    I.jstreeWaitForLoader();
    I.see("asd", "#SomStromcek");
    I.see("name-autotest", "#SomStromcek");
    I.dontSee("Hlavičky", "#SomStromcek");
    I.dontSee("Jet portal 4", "#SomStromcek");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=SYSTEM&docid=-1");
    DTE.waitForEditor();
    //skontroluj adresar novej stranky a potom aj stromovu strukturu
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.dontSeeInField("#editorAppDTE_Field_editorFields-groupDetails input", "/System/Kôš");
    I.seeInField("#editorAppDTE_Field_editorFields-groupDetails input", "/System");

    DTE.cancel();
    I.dontSee("asd", "#SomStromcek");
    I.dontSee("name-autotest", "#SomStromcek");
    I.see("Hlavičky", "#SomStromcek");
    I.dontSee("Jet portal 4", "#SomStromcek");
});

Scenario('Overenie otvarania podla groupid a docid', ({ I, DTE }) => {
    //musi otvorit NOVY stranku v zadanom adresari
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=8694&docid=-1");
    DTE.waitForEditor();
    I.wait(5);
    //skontroluj adresar novej stranky a potom aj stromovu strukturu
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#editorAppDTE_Field_editorFields-groupDetails input", "/System/Hlavičky");

    DTE.cancel();
    I.dontSee("asd", "#SomStromcek");
    I.dontSee("name-autotest", "#SomStromcek");
    I.see("Hlavičky", "#SomStromcek");
    I.dontSee("Jet portal 4", "#SomStromcek");

    //ak je zadane docid MUSI ignorovat zadane groupid (co je /System/Kos) kedze stranka je v
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=8694&docid=141");
    DTE.waitForEditor();
    I.wait(5);
    //skontroluj adresar novej stranky a potom aj stromovu strukturu
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.dontSeeInField("#editorAppDTE_Field_editorFields-groupDetails input", "/System");
    I.seeInField("#editorAppDTE_Field_editorFields-groupDetails input", "/Jet portal 4");

    DTE.cancel();

    I.dontSee("asd", "#SomStromcek");
    I.dontSee("Hlavičky", "#SomStromcek");
    I.see("Jet portal 4", "#SomStromcek");
});


Scenario('Zapamatanie priecinka web', ({ I, DT }) => {
    //web stranky si maju zapamatat naposledy otvoreny priecinok
    //vynimka je zadanie ?groupid=0 kedy sa resetuje stav
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    //overenie, ze sa nerozbalilo Jet portal 4
    I.see("Jet portal 4", ".tree-col");
    I.dontSee("Zo sveta financií", ".tree-col");
    I.jstreeNavigate(["Jet portal 4", "Zo sveta financií"]);
    I.see("Jet portal 4", ".tree-col");
    I.see("Zo sveta financií", ".tree-col");

    //
    I.say("obnov stranku a over zobrazenie");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.see("Jet portal 4", ".tree-col");
    I.see("Zo sveta financií", ".tree-col");
    I.see("Trhy sú naďalej vydesené", "#datatableInit_wrapper");

    //
    I.say("check SYSTEM tab");
    I.clickCss("#pills-system-tab");
    DT.waitForLoader();
    I.see("Hlavičky", ".tree-col");
    I.see("Pätičky", ".tree-col");
    I.dontSee("Jet portal 4", ".tree-col");
    I.dontSee("Zo sveta financií", ".tree-col");

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.see("Systém", "div.md-breadcrumb a.nav-link.active")
    I.see("Hlavičky", ".tree-col");
    I.see("Pätičky", ".tree-col");
    I.dontSee("Jet portal 4", ".tree-col");
    I.dontSee("Zo sveta financií", ".tree-col");

    //
    I.say("check TRASH tab");
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();
    I.see("asdf", ".tree-col");
    I.dontSee("Pätičky", ".tree-col");
    I.dontSee("Jet portal 4", ".tree-col");
    I.dontSee("Zo sveta financií", ".tree-col");

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.see("Kôš", "div.md-breadcrumb a.nav-link.active")
    I.see("asdf", ".tree-col");
    I.dontSee("Pätičky", ".tree-col");
    I.dontSee("Jet portal 4", ".tree-col");
    I.dontSee("Zo sveta financií", ".tree-col");

    //
    I.say("Prepni domenu a over zobrazenie");
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("mirroring.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");
    I.wait(2);

    I.waitForText("Slovensky", 10, ".tree-col");
    I.see("English", ".tree-col");
    I.dontSee("Domov", ".tree-col");
    I.see("Slovensky", "#datatableInit_wrapper");

    //
    I.say("Prepni domenu a over zobrazenie rozbalene");
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("rozbalenie.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");

    I.see("Test rozbalenia podadresarov", ".tree-col");
    I.see("Podadresár A", ".tree-col");
    I.dontSee("Podadresár B-1", ".tree-col");
    I.see("Test rozbalenia podadresarov", "#datatableInit_wrapper");
});

Scenario('Zapamatanie priecinka web-zrusenie', ({ I, DT }) => {
    //ak si otvorim nejaky priecinok, potom zavolam s groupid=0 a potom znova bez parametra nesmie si to pamatat (groupid=0 zmaze zapamatanie)
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=1");
    I.jstreeClick("Zo sveta financií");

    //
    I.say("Over zapamatanie");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.see("Zo sveta financií", "#datatableInit_wrapper");

    //
    I.say("Nacitaj s groupid=0")
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    DT.waitForLoader();
    I.see("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");

    //
    I.say("Nacitavam bez parametra, musi byt v roote, nie v Zo sveta financii");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.see("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    I.dontSee("Zo sveta financií", "#datatableInit_wrapper");
});

Scenario('Opener pre ine ako id, otocene sortovanie', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.resetTable();
    //prepni sorting
    I.clickCss("th.dt-th-tempId.sorting_asc");
    DT.waitForLoader();

    I.amOnPage("/admin/v9/templates/temps-list/?tempId=1");
    DTE.waitForEditor();

    I.amOnPage("/admin/v9/templates/temps-list/?tempId=32");
    DTE.waitForEditor();
    DTE.cancel();

    DT.resetTable();
});

Scenario('Filter by ID', ({ I, DT, DTE }) => {
    //
    I.say("Filter webpages");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    DT.filter("id", "8486");
    I.see("Voliteľné polia", "#datatableInit tbody");
    I.dontSee("Test pracovnej verzie stránky", "#datatableInit tbody");
    I.dontSee("Hlavná stránka adresára", "#datatableInit tbody");
    I.see("Záznamy 1 až 1 z 1", "#datatableInit_info");

    //
    I.say("Filter audit");
    I.amOnPage("/admin/v9/apps/audit-search/");
    DT.filter("id", "58993");
    I.see("CRON", "#datatableInit tbody");
    I.see("Cron task executed: sk.iway.iwcm.", "#datatableInit tbody");
    I.see("Záznamy 1 až 1 z 1", "#datatableInit_info");

    I.amOnPage("/admin/v9/apps/audit-search/?id=58993");
    DTE.waitForEditor();

    //
    I.say("Filter records with ID column as dictionaryId");
    I.amOnPage("/apps/tooltip/admin/");
    I.see("Tooltip test", "#tooltipDataTable tbody");
    I.see("Druhy tooltip", "#tooltipDataTable tbody");
    DT.filter("dictionaryId", "4602");
    I.see("Tooltip test", "#tooltipDataTable tbody");
    I.dontSee("Druhy tooltip", "#tooltipDataTable tbody");
    I.see("Záznamy 1 až 1 z 1", "#tooltipDataTable_info");

    I.amOnPage("/apps/tooltip/admin/?id=4602");
    DTE.waitForEditor("tooltipDataTable");

    //
    I.say("Filter serverSide=false table");
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filter("tempId", "33");
    I.see("Subpage EN", "#datatableInit tbody");
    I.dontSee("Subpage DE", "#datatableInit tbody");
    I.see("Záznamy 1 až 1 z 1", "#datatableInit_info");

    I.amOnPage("/admin/v9/templates/temps-list/?tempId=4");
    DTE.waitForEditor();
});

Scenario('BUG open group from another domain', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=1");
    DT.waitForLoader();
    I.fillField("#tree-folder-id", "9811");
    I.pressKey('Enter');
    I.waitForElement("#SomStromcek li[id='9811']", 20);
    I.seeInCurrentUrl("/admin/v9/webpages/web-pages-list/?groupid=9811");
});

Scenario('logout', ({I}) => {
    I.logout();
});

Scenario("BUG filter clientSide table by ID equals value", ({ I, DT }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.waitForLoader();
    I.waitForText("Generic", 5, "#datatableInit tbody");
    I.see("Homepage", "#datatableInit tbody");

    DT.filter("tempId", "1");

    I.waitForText("Generic", 5, "#datatableInit tbody");
    I.dontSee("Homepage", "#datatableInit tbody");
    I.waitForText("Záznamy 1 až 1 z 1", 5, "#datatableInit_info");
});