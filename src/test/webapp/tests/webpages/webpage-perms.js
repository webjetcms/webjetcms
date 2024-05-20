Feature('webpages.webpage-perms');

var folder_name, auto_webPage, randomNumber;

Before(({ I }) => {
     if (typeof folder_name == "undefined") {
          randomNumber = I.getRandomText();
          I.say("randomNumber="+randomNumber);
          folder_name = 'name-autotest-' + randomNumber;
          auto_webPage = 'webPage-autotest-' + randomNumber;
     }
});


Scenario('uvodne overenie prav', ({ I }) => {
    I.relogin("admin");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=1");

    //over zobrazenie vsetkych tlacidiel pre adresar
    I.seeElement("div.tree-col button.buttons-create");
    I.seeElement("div.tree-col button.buttons-edit");
    I.seeElement("div.tree-col button.buttons-remove");

    //over zobrazenie vsetkych tlacidiel pre web stranky
    I.seeElement("#datatableInit_wrapper button.buttons-create");
    I.seeElement("#datatableInit_wrapper button.buttons-edit");
    I.seeElement("#datatableInit_wrapper button.btn-duplicate");
    I.seeElement("#datatableInit_wrapper button.buttons-remove");

    //over zobrazenie kariet System a Kos
    I.see("Priečinky", "div.tree-col div.md-breadcrumb");
    I.see("Systém", "div.tree-col div.md-breadcrumb");
    I.see("Kôš", "div.tree-col div.md-breadcrumb");
});

Scenario('stranky-overenie prav na tlacidla', ({ I, DT, DTE }) => {
    I.relogin("admin");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=1&removePerm=addPage,pageSave,deletePage,pageSaveAs");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-create");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-edit");
    I.dontSeeElement("#datatableInit_wrapper button.btn-duplicate");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-remove");

    //over v dialogu
    I.click("Jet portal 4 - testovacia stranka", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.dontSeeElement("#datatableInit_modal div.DTE_Form_Buttons button.btn-primary");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?removePerm=addPage,pageSave,deletePage,pageSaveAs,forceShowButton&groupid=67");
    DT.waitForLoader();
    //skus pridat
    I.click("#datatableInit_wrapper button.buttons-create");
    I.click("#pills-dt-datatableInit-basic-tab");
    I.fillField("Názov web stránky", auto_webPage);
    DTE.save();
    I.see("Pridať web stránku - nemáte právo na pridanie web stránky");
    DTE.cancel();

    //skus editovat
    I.jstreeNavigate(["Test stavov", "Nedá sa zmazať"]);
    I.click("Nedá sa zmazať", "#datatableInit");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("Názov web stránky", "Nedá sa zmazať");
    DTE.save();
    I.see("Nemáte právo na editáciu web stránky");
    DTE.cancel();

    //skus zmazat
    DT.filter("title", "Nedá sa zmazať");
    I.click("table.datatableInit button.buttons-select-all");
    I.click("#datatableInit_wrapper button.buttons-remove");
    DTE.waitForEditor();
    I.see("Naozaj chcete zmazať položku?");
    DTE.save();
    I.see("nemáte právo na zmazanie web stránky");
});


Scenario('adresare-overenie prav na tlacidla', ({ I, DT, DTE }) => {
    I.relogin("admin");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?removePerm=editDir,addSubdir,deleteDir&groupid=67");
    DT.waitForLoader();

    I.dontSeeElement("div.tree-col button.buttons-create");
    I.dontSeeElement("div.tree-col button.buttons-edit");
    I.dontSeeElement("div.tree-col button.buttons-remove");

    //over v dialogu
    I.executeScript(function() {
        try {
            groupsDatatable.wjEdit("67");
        }
        catch (e) {}
    });

    DTE.waitForEditor("groups-datatable");
    I.dontSeeElement("#groups-datatable_modal div.DTE_Form_Buttons button.btn-primary");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?removePerm=editDir,addSubdir,deleteDir,forceShowButton&groupid=67");
    //forcni zobrazenie tlacidiel, su len skryte
    I.executeScript(function() {
        try {
            $("div.tree-col button").removeClass("noperms-editDir");
            $("div.tree-col button").removeClass("noperms-addSubdir");
            $("div.tree-col button").removeClass("noperms-deleteDir");
        }
        catch (e) {}
    });
    //skus pridat
    I.click("div.tree-col button.buttons-create");
    DTE.waitForEditor("groups-datatable");
    I.fillField("Názov priečinku", folder_name);
    DTE.save();
    I.see("K tomuto adresáru nemáte prístupové práva");
    DTE.cancel();

    //skus editovat
    I.jstreeNavigate(["Test stavov", "Nedá sa zmazať"]);
    I.click("div.tree-col button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.seeInField("Názov priečinku", "Nedá sa zmazať");
    DTE.save();
    I.see("K tomuto adresáru nemáte prístupové práva");
    DTE.cancel();

    //skus zmazat
    I.jstreeNavigate(["Test stavov", "Nedá sa zmazať"]);
    I.click("div.tree-col button.buttons-remove");
    DTE.waitForEditor("groups-datatable");
    I.see("Naozaj chcete zmazať položku?")
    DTE.save();
    I.see("K tomuto adresáru nemáte prístupové práva");
});

Scenario('overenie prav na strukturu', ({ I, DT, DTE }) => {
    I.relogin("tester2");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    //over zobrazenie vsetkych tlacidiel pre adresar, inicialne musia byt disabled
    I.seeElement("div.tree-col button.buttons-create.disabled");
    I.seeElement("div.tree-col button.buttons-edit.disabled");
    I.seeElement("div.tree-col button.buttons-remove.disabled");

    //over zobrazenie a moznost editacie Test podadresar
    I.say("over zobrazenie a moznost editacie Test podadresar");
    I.jstreeNavigate(["Test stavov", "Nedá sa zmazať"]);
    I.click("Nedá sa zmazať", "#datatableInit");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("Názov web stránky", "Nedá sa zmazať");
    DTE.save();
    I.dontSee("Nemáte právo na editáciu web stránky");

    //over, ze sa neda editovat adresar na ktory nemam prava
    I.say("over, ze sa neda editovat adresar na ktory nemam prava");
    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka"]);
    I.executeScript(function() {
        try {
            $("#groups-datatable_wrapper").show();
            $("#datatableInit_wrapper").hide();
        }
        catch (e) {}
    });
    I.wait(1);
    I.scrollTo("#groups-datatable_wrapper");
    I.click("Úvodná stránka", "#groups-datatable_wrapper td.dt-row-edit a");
    DT.waitForLoader();
    I.see("K tomuto adresáru nemáte prístupové práva")
    I.wait(2);
    I.toastrClose();
    I.wait(2);
    I.dontSee("K tomuto adresáru nemáte prístupové práva")

    I.executeScript(function() {
        try {
            $("#groups-datatable_wrapper").hide();
            $("#datatableInit_wrapper").show();
        }
        catch (e) {}
    });

    //over, ze sa neda presunut adresar
    /* - not possible anymore because of structure not showing root folder anymore
    I.say("over, ze sa neda presunut adresar");
    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka", "Test podadresar", "Nesmie sa dať presunúť"]);
    DT.waitForLoader();
    I.click("div.tree-col button.buttons-edit");
    DTE.waitForEditor("groups-datatable");
    I.seeInField("Názov priečinku", "Nesmie sa dať presunúť");
    I.groupSetRootParent();
    DTE.save();
    I.see("K tomuto adresáru nemáte prístupové práva");
    //skus iny ako root adresar
    I.click('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'); // zmena na korenovy adresar
    I.waitForElement("div.jsTree-wrapper");
    I.wait(1);
    I.waitForText('Jet portal 4', 5);
    I.wait(1);
    I.click('Jet portal 4', 'div.jsTree-wrapper');
    I.wait(1);
    I.seeInField('#editorAppDTE_Field_editorFields-parentGroupDetails input.form-control', '/Jet portal 4');
    DTE.save();
    I.see("Na tento priečinok nemáte prístupvé práva. Skúste o úroveň nižšie.");
    DTE.cancel();*/

    //over, ze sa neda zmazat
    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka"]);
    I.executeScript(function() {
        try {
            $('.tree-col .dt-header-row .buttons-remove').removeClass("disabled");
        }
        catch (e) {}
    });
    I.click('.tree-col .dt-header-row .buttons-remove');
    DTE.waitForEditor("groups-datatable");
    I.seeElement("#groups-datatable_modal");
    DTE.save(); //zmazat
    I.wait(3);
    I.seeElement("#groups-datatable_modal"); //zmazanie sa nepodarilo, dialog zostal otvoreny
    DTE.cancel();

    //over vytvorenie noveho adresara
    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka", "Test podadresar", "Nesmie sa dať presunúť"]);
    I.click('.tree-col .dt-header-row .buttons-create');
    DTE.waitForEditor("groups-datatable");
    I.seeElement("#groups-datatable_modal");
    I.fillField("Názov priečinku", folder_name);
    DTE.save();
    I.dontSee("K tomuto adresáru nemáte prístupové práva");
    I.dontSeeElement("#groups-datatable_modal");

    //skus zmazanie
    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka", "Test podadresar", "Nesmie sa dať presunúť", folder_name]);
    I.click('.tree-col .dt-header-row .buttons-remove');
    DTE.waitForEditor("groups-datatable");
    I.seeElement("#groups-datatable_modal");
    DTE.save(); //zmazat
    I.wait(3);
    I.dontSeeElement("#groups-datatable_modal");
});

Scenario('overenie prav na strukturu - web stranky', ({ I, DTE }) => {
    I.relogin("tester2");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    I.seeElement("#datatableInit_wrapper .dt-header-row button.buttons-create.disabled");
    I.seeElement("#datatableInit_wrapper .dt-header-row button.buttons-edit.disabled");
    I.seeElement("#datatableInit_wrapper .dt-header-row button.buttons-remove.disabled");

    //nesmu byt ziadne stranky nacitane
    I.see("Záznamy 0 až 0 z 0", "#datatableInit_wrapper");

    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka"]);
    I.seeElement("#datatableInit_wrapper .dt-header-row button.buttons-create.disabled");
    I.seeElement("#datatableInit_wrapper .dt-header-row button.buttons-edit.disabled");
    I.seeElement("#datatableInit_wrapper .dt-header-row button.buttons-remove.disabled");
    I.see("Záznamy 0 až 0 z 0", "#datatableInit_wrapper");

    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka", "Test podadresar"]);
    I.dontSeeElement("#datatableInit_wrapper .dt-header-row button.buttons-create.disabled");
    I.dontSee("Záznamy 0 až 0 z 0", "#datatableInit_wrapper");

    //skus ulozit stranku, na ktoru mame prava
    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka", "Test podadresar", "Nesmie sa dať presunúť"]);
    I.click("Nesmie sa dať presunúť", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("Názov web stránky", "Nesmie sa dať presunúť");
    DTE.save();
    I.dontSee("Nemáte právo na editáciu web stránky");
    I.dontSeeElement("#datatableInit_modal");

    //stranke nastav adresar, na ktory nemame prava
    /* not possible anymore because of structure not showing root folder anymore
    I.click("Nesmie sa dať presunúť", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("Názov web stránky", "Nesmie sa dať presunúť");
    I.click('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'); // zmena na korenovy adresar
    I.waitForElement("div.jsTree-wrapper");
    I.wait(1);
    I.waitForText('Jet portal 4', 5);
    I.wait(1);
    I.click('Jet portal 4', 'div.jsTree-wrapper');
    I.wait(1);
    I.seeInField('#editorAppDTE_Field_editorFields-groupDetails input.form-control', '/Jet portal 4');
    DTE.save();
    I.see("Nemáte právo na editáciu web stránky");
    DTE.cancel();
    */
});

Scenario('zobrazenie samostatnych web stranok z prav', ({ I }) => {
    //tester 2 ma prava na DVE web stranky v English->Home
    I.relogin("tester2");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    I.jstreeNavigate(["English"]);
    I.see("Záznamy 0 až 0 z 0");
    I.jstreeNavigate(["English", "Home"]);

    I.see("Záznamy 1 až 2 z 2");

    I.see("Home", "#datatableInit_wrapper");
    I.see("Osobný bankár", "#datatableInit_wrapper");

    I.dontSee("Banner prihlásený", "#datatableInit_wrapper");
    I.dontSee("Banner neprihlásený", "#datatableInit_wrapper");

});

Scenario('tester2 nema pravo na System a Kos', ({ I }) => {
    I.relogin("tester2");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");

    I.see("Priečinky", "div.tree-col div.md-breadcrumb");
    I.dontSee("Systém", "div.tree-col div.md-breadcrumb");
    I.dontSee("Kôš", "div.tree-col div.md-breadcrumb");

    I.click("Priečinky", "div.tree-col div.md-breadcrumb");
    I.dontSee("Prístup na adresár zamietnutý");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('logoff', ({ I }) => {
    I.logout();
});