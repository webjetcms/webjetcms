Feature('webpages.jstreesettings');

var randomNumber;

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        I.say("randomNumber="+randomNumber);
    }
});

function setSettings(I, checkedNames=null, treeWidth=4, galeria=false) {
    I.clickCss("button.buttons-jstree-settings");
    I.waitForElement("#jstreeSettingsModal");
    I.wait(1);

    if (galeria == false) {
        I.uncheckOption("#jstree-settings-showid");
        I.wait(1);
        I.uncheckOption("#jstree-settings-showorder");
        I.wait(1);
        I.uncheckOption("#jstree-settings-showpages");
        I.wait(1);
        I.uncheckOption("#jstree-settings-showfolders-dt");
        I.wait(1);

    }
    I.click({ css: "#jstreeSettingsModal .DTE_Field_treeWidth button.btn-outline-secondary" });
    I.click(locate('div.dropdown-menu.show .dropdown-item').withText(treeWidth+":"+(12-treeWidth)));

    if (galeria == false) {
        if (checkedNames != null) {
            checkedNames.forEach((value)=> {
                I.say("Checking option "+value);
                I.checkOption("#jstree-settings-"+value);
                I.wait(2);
            });
        }
    }

    //set first value in select
    let name = "treeSortType";
    I.click({ css: "#jstreeSettingsModal div.DTE_Field_" + name + " button.dropdown-toggle" });
    let firstOption = "div.dropdown-menu.show ul li:first-child .dropdown-item";
    I.waitForElement(locate(firstOption), 5);
    I.waitForEnabled(locate(firstOption), 5);
    I.click(firstOption);
    I.wait(0.3);

    I.checkOption("#jstree-settings-treeSortOrderAsc");

    I.clickCss("#jstree-settings-submit");
}

Scenario('zobrazenie ID a poradia @singlethread', ({ I }) => {
    //cisty stav
    I.say("cisty stav");
    setSettings(I);
    I.dontSee("#67 Test stavov", "#SomStromcek");
    I.dontSee("#67 Test stavov (30)", "#SomStromcek");
    I.dontSee("Test stavov (30)", "#SomStromcek");

    //zapni zobrazenie ID
    I.say("zapni zobrazenie ID");
    setSettings(I, ["showid"]);
    I.see("#67 Test stavov", "#SomStromcek");
    I.dontSee("#67 Test stavov (30)", "#SomStromcek");
    I.dontSee("Test stavov (30)", "#SomStromcek");

    //zapni zobrazenie order
    I.say("zapni zobrazenie order");
    setSettings(I, ["showorder"]);
    I.dontSee("#67 Test stavov", "#SomStromcek");
    I.dontSee("#67 Test stavov (30)", "#SomStromcek");
    I.see("Test stavov (30)", "#SomStromcek");

    //zapni ID aj order
    I.say("zapni ID aj order");
    setSettings(I, ["showid", "showorder"]);
    I.see("#67 Test stavov", "#SomStromcek");
    I.see("#67 Test stavov (30)", "#SomStromcek");
    I.see("Test stavov (30)", "#SomStromcek");
});

Scenario('zobrazenie web stranok @singlethread', ({ I }) => {
    //cisty stav
    I.say("cisty stav");
    setSettings(I);
    I.dontSee("#67 Test stavov", "#SomStromcek");
    I.dontSee("#67 Test stavov (30)", "#SomStromcek");
    I.dontSee("Test stavov (30)", "#SomStromcek");
    I.jstreeClick("Test stavov");
    I.dontSee("Nezobrazená v menu", "#SomStromcek");
    I.dontSee("Nevyhľadateľná", "#SomStromcek");
    I.see("Test BR<br>v nazve", "#SomStromcek");

    //zobrazenie stranok
    I.say("zobrazenie stranok");
    setSettings(I, ["showpages"]);
    I.dontSee("#67 Test stavov", "#SomStromcek");
    I.dontSee("#67 Test stavov (30)", "#SomStromcek");
    I.dontSee("Test stavov (30)", "#SomStromcek");
    I.jstreeClick("Test stavov");
    I.see("Nezobrazená v menu", "#SomStromcek");
    I.see("Nevyhľadateľná", "#SomStromcek");
    I.see("Test BR<br>v nazve", "#SomStromcek");
});

Scenario('drag drop @singlethread', ({ I, DT, DTE }) => {
    //zobrazenie stranok
    I.say("zobrazenie stranok");
    setSettings(I, ["showpages"]);

    I.createFolderStructure(randomNumber);
    I.createNewWebPage(randomNumber);
    var webpageTitle = "webPage-autotest-"+randomNumber;
    var folderTitle = "subone-autotest-"+randomNumber;

    //aby sa nam zatvorili ostatne priecinky
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeClick("name-autotest-"+randomNumber);

    I.say("Presun stranku do podadresara");
    I.see(webpageTitle, "#SomStromcek");
    I.checkOption("#treeAllowDragDrop");
    I.dragAndDrop(locate('a.jstree-anchor').withText(webpageTitle), locate('a.jstree-anchor').withText(folderTitle), { force: true });
    DT.waitForLoader();
    I.jstreeWaitForLoader();

    I.dontSee(webpageTitle, "#SomStromcek");
    I.jstreeClick(folderTitle);
    I.see(webpageTitle, "#SomStromcek");

    I.say("Otvor stranku a ober zobrazenie adresara");
    I.click(webpageTitle, "#datatableInit");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#editorAppDTE_Field_editorFields-groupDetails input.form-control", "/"+folderTitle);

    DTE.cancel();
});

Scenario("delete folder structure @singlethread", ({ I }) => {
    I.deleteFolderStructure(randomNumber);
});

Scenario('zobrazenie web stranok-rozbalenie @singlethread', ({ I }) => {
    //single lebo tam moze medzi tym iny test vytvorit priecinok
    //overenie, ze aj defaultne sa nacitaju stranky (ked je rozbalena struktura)

    //domenovy selektor
    I.clickCss("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("rozbalenie.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.clickCss(".toastr-buttons button.btn-primary");

    //cisty stav
    I.say("cisty stav");
    setSettings(I);
    I.dontSeeElement("#docId-9768_anchor");
    I.see("Podadresár A", "#SomStromcek");
    I.see("Test rozbalenia podadresarov", "#datatableInit");
    I.dontSee("Jet portal 4 - testovacia stranka", "#datatableInit")

    //zobrazenie stranok
    I.say("zobrazenie stranok");
    setSettings(I, ["showpages"]);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.waitForElement("#docId-9768_anchor");
    I.see("Podadresár A", "#SomStromcek");
    I.see("Test rozbalenia podadresarov", "#datatableInit");
    I.dontSee("Jet portal 4 - testovacia stranka", "#datatableInit")
});

Scenario('Automaticke rozbalenie domeny-odhlasenie @singlethread', ({ I }) => {
    //odhlas sa, aby domena nezostala zapamatana pre dalsie testy
    I.logout();
});

Scenario('jstree zobrazenie v datatabulke @singlethread', ({ I, DT }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    //reset
    setSettings(I);

    I.dontSee("Priečinky", "div.col-md-8");

    //zobrazenie priecinkov
    setSettings(I, ["showfolders-dt"]);

    I.see("Priečinky", "div.col-md-8");

    I.say("Zobrazi sa root a jeho podpriecinky");
    I.clickCss("#pills-folders-dt-tab");
    I.see("Jet portal 4", "#groups-datatable");
    I.see("Úvodná stránka", "#groups-datatable");
    I.see("Investičný vklad", "#groups-datatable");

    I.say("Zobrazi sa zvoleny priecinok a jeho podpriecinky");
    I.jstreeClick("Test stavov");
    I.dontSee("Jet portal 4", "#groups-datatable");
    I.dontSee("Investičný vklad", "#groups-datatable");
    I.see("Test stavov", "#groups-datatable");
    I.see("Zaheslovaný", "#groups-datatable");
    I.see("Tento nie je interný", "#groups-datatable");

    I.say("Multi oznacenie priecinkov - zobrazia sa len zvolene");
    I.click("English", "#SomStromcek", { modifiers: ['Meta'] } );
    DT.waitForLoader();
    I.see("Záznamy 1 až 2 z 2");
    I.see("English", "#groups-datatable");
    I.see("Test stavov", "#groups-datatable");
    I.dontSee("Zaheslovaný", "#groups-datatable");
    I.dontSee("Tento nie je interný", "#groups-datatable");
});

Scenario('nastavenie sirky stlpcov @singlethread', ({ I }) => {
    setSettings(I);

    I.seeElement(".tree-col.col-md-4");
    I.seeElement(".datatable-col.col-md-8");

    setSettings(I, null, 6);

    I.dontSeeElement(".tree-col.col-md-4");
    I.dontSeeElement(".datatable-col.col-md-8");

    I.seeElement(".tree-col.col-md-6");
    I.seeElement(".datatable-col.col-md-6");

    //reload
    I.say("Overujem zapamatanie stavu")
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    I.waitForElement(".tree-col.col-md-6", 5);
    I.dontSeeElement(".tree-col.col-md-4");
    I.dontSeeElement(".datatable-col.col-md-8");

    I.seeElement(".tree-col.col-md-6");
    I.seeElement(".datatable-col.col-md-6");

    //galeria
    I.say("Kontrolujem v galerii");
    I.amOnPage("/admin/v9/apps/gallery/");

    setSettings(I, null, 4, true);

    I.seeElement(".tree-col.col-md-4");
    I.seeElement(".datatable-col.col-md-8");

    setSettings(I, null, 6, true);

    I.waitForElement(".tree-col.col-md-6", 5);
    I.dontSeeElement(".tree-col.col-md-4");
    I.dontSeeElement(".datatable-col.col-md-8");

    I.seeElement(".tree-col.col-md-6");
    I.seeElement(".datatable-col.col-md-6");

    //reload
    I.say("Overujem zapamatanie stavu galeria")
    I.amOnPage("/admin/v9/apps/gallery/");

    I.waitForElement(".tree-col.col-md-6", 5);
    I.dontSeeElement(".tree-col.col-md-4");
    I.dontSeeElement(".datatable-col.col-md-8");

    I.seeElement(".tree-col.col-md-6");
    I.seeElement(".datatable-col.col-md-6");
});

Scenario('reset @singlethread', ({ I }) => {
    setSettings(I);
    I.amOnPage("/admin/v9/apps/gallery/");
    setSettings(I, null, 4, true);
});

Scenario('Slash in group name @singlethread', ({ I, DT, DTE }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Test stavov"]);

    var title = "Lomka/v nazve";
    I.see(title, "#SomStromcek");
    I.click(title, "#SomStromcek");

    DT.waitForLoader();
    I.clickCss("div.tree-col div.dt-buttons button.buttons-edit");
    DTE.waitForEditor("groups-datatable");

    I.seeInField("#DTE_Field_groupName", title);
    I.seeInField("#DTE_Field_navbarName", title);
    I.seeInField("#DTE_Field_urlDirName", "lomka-nazve");

    //
    I.say("regenerating urlDirName, will check on next run");
    I.fillField("#DTE_Field_urlDirName", "");
    DTE.save();

    I.see(title, "#SomStromcek");

    //
    I.say("Checking webpage");
    I.see(title, "div.datatable-col");
    I.click(title, "div.datatable-col");

    DTE.waitForEditor();

    I.see(title, "#datatableInit_modal h5.modal-title");
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("#DTE_Field_title", title);
    I.seeInField("#DTE_Field_navbar", title);
    I.seeInField("#DTE_Field_virtualPath", "/test-stavov/lomka-nazve/");

    DTE.save();

    I.amOnPage("/test-stavov/lomka-nazve/");

    //there was no other way to do this except by this script, codecept always returns / as unencoded value
    //$(function() { $("#unescapedTitle").text("!REQUEST(doc_title)!"); });
    I.waitForText("Lomka&#47;v nazve", 10, "#unescapedTitle");
});

Scenario('reset 2 @singlethread', ({ I }) => {
    I.jstreeReset();
    I.logout();
});

Scenario('reset 2-nosingle', ({ I }) => {
    I.jstreeReset();
    I.logout();
});