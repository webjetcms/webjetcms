Feature('webpages.folders-mirroring-wj9');

var auto_folder_sk, auto_folder_en, auto_folder_de, auto_subfolder1_sk, auto_subfolder2_sk, test_folder, elfinder_folder, randomNumber;
var skID, enID, deID;

Before(({ I, login }) => {
     login('admin');
     if (typeof randomNumber == "undefined") {
          randomNumber = I.getRandomTextShort();
          //randomNumber = "070139"
          I.say("randomNumber=" + randomNumber);
          auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
          auto_folder_en = 'en-mir-autotest-' + randomNumber;
          auto_folder_de = 'de-mir-autotest-' + randomNumber;
          auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
          auto_subfolder2_sk = 'sk-mir-subfolder2-' + randomNumber;
          test_folder = 'TEST-autotest-' + randomNumber;
          elfinder_folder = 'test_mirroring_elfinder-' + randomNumber;
     }
     I.resizeWindow(1600, 1200);
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=9811');
     //I.wait(1);
     //I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     // prepnutie na domenu mirroring.tau27.iway.sk
     /*I.click("div.js-domain-toggler div.bootstrap-select button");
     I.wait(1);
     I.click(locate('.dropdown-item').withText("mirroring.tau27.iway.sk"));
     I.waitForElement("#toast-container-webjet", 10);
     I.click(".toastr-buttons button.btn-primary");*/
});

function wj9CreateMirroringSubfolder(I, DTE, DT, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
     var auto_subfolder2_sk = 'sk-mir-subfolder2-' + randomNumber;
     // vytvorenie 1. podadresar v sk strukture
     I.say('Vytvaram prvy podpriecinok pre sk adresar');
     I.jstreeClick(auto_folder_sk);
     I.click(DT.btn.tree_add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', auto_subfolder1_sk);
     I.wait(1);
     DTE.save();
     I.jstreeClick(auto_folder_sk);
     I.waitForText(auto_subfolder1_sk, 10, '.jstree-anchor');
     I.wait(1);
     // vytvorenie 2. podpriecinka
     I.say('Vytvaram druhy podpriecinok pre sk adresar');
     I.jstreeClick(auto_folder_sk);
     I.click(DT.btn.tree_add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', auto_subfolder2_sk);
     DTE.save();
     I.jstreeClick(auto_folder_sk);
     I.waitForText(auto_subfolder2_sk, 10, '.jstree-anchor');
     I.wait(1);
     // skontrolovanie ci sa nepublikovane priecinky vytvorili aj v en strukture
     I.say('Kontrolujem ci sa pridali nepublikovane priecinky i do en struktury');
     I.jstreeClick(auto_folder_en);

     I.waitForElement(locate('li.jstree-node.jstree-leaf').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder.jstree-themeicon-custom').withText(auto_subfolder1_sk).inside( locate(".jstree-node.jstree-open").withText(auto_folder_en)), 10);
     I.seeElement(locate('li.jstree-node.jstree-leaf').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder.jstree-themeicon-custom').withText(auto_subfolder2_sk).inside( locate(".jstree-node.jstree-open").withText(auto_folder_en)));

}

function wj9MoveMainFolder(I, DTE, DT, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var test_folder = 'TEST-autotest-' + randomNumber;
     var domainName = 'mirroring.tau27.iway.sk';
     // vytvorenie testovacieho priecinka
     I.say('Vytvorenie testovacieho priecinka TEST-autotest-');
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.waitForVisible('#SomStromcek');
     I.click(DT.btn.tree_add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', test_folder);
     I.fillField('#DTE_Field_domainName', domainName);
     I.wait(0.5);
     DTE.save();
     // presun sk priecinka do TEST priecinka
     I.say('Presuvam sk folder do TEST priecinka');
     I.jstreeClick(auto_folder_sk);
     I.click(DT.btn.tree_edit_button);
     DTE.waitForLoader();
     I.click(locate('#editorAppDTE_Field_editorFields-parentGroupDetails').find('button.btn.btn-outline-secondary.btn-vue-jstree-item-edit'));
     I.waitForVisible('#jsTree');
     I.click(locate('#jsTree').withChild('ul.jstree-container-ul.jstree-children').find('.jstree-icon.jstree-ocl'));
     I.waitForVisible(locate('#jsTree').withDescendant('a.jstree-anchor').withText(test_folder));
     I.click(locate('#jsTree').find('.jstree-node.jstree-leaf.jstree-last').withText(test_folder).find('a.jstree-anchor'));
     DTE.save();
     // skontrolujem ci sa sk priecinok presunul do TEST priecinka
     I.say('Skontrolujem ci sa sk priecinok presunul do TEST priecinka');
     I.jstreeClick(auto_folder_sk);
     I.jstreeClick(test_folder);
     I.waitForElement(locate('.jstree-node.jstree-last.jstree-open').withDescendant('.jstree-node.jstree-closed.jstree-last').withText(auto_folder_sk));
     I.wait();
     // skontrolujem ci sa en priecinok presunul do TEST priecinka
     I.say('Skontrolujem ci sa en priecinok presunul do TEST priecinka');
     I.seeElement(locate('.jstree-node.jstree-last.jstree-open').withDescendant('.jstree-node.jstree-closed.jstree-last').withText(auto_folder_en)); // TODO - priecinok sa nepremiestnil
     I.dontSeeElement(locate('.jstree-node.jstree-closed').withDescendant('.jstree-anchor').withText(auto_folder_en)); // TODO - priecinok ostal na mieste
}

function wj9DeleteMainFolder(I, DT, DTE, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var test_folder = 'TEST-autotest-' + randomNumber;
     // vymaz hlavny sk adresar z test adresara
     I.say('Vymazam hlavny sk adresar z test adresara');
     //I.jstreeClick(test_folder);
     I.jstreeClick(auto_folder_sk);
     I.wait(1);
     I.click(DT.btn.tree_delete_button);
     DT.waitForLoader();
     I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
     I.waitForText('Naozaj chcete zmazať položku?', 10, '.col-sm-7.offset-sm-4');
     I.click('Zmazať');
     DTE.waitForLoader();
     I.wait(3);
     I.waitForElement('.jstree-anchor', 20);
     DT.waitForLoader();
     // skontroluj ci je vymazany i hlavny en folder
     I.say('Skontroluj ci je vymazany i hlavny en folder');
     /*I.jstreeClick(test_folder);
     within(locate('.jstree-anchor').withText(test_folder), () => {
          I.dontSeeElement(locate('.jstree-anchor').withText(auto_folder_en));
     });*/
     I.dontSeeElement(locate('.jstree-anchor').withText(auto_folder_sk));
     I.dontSeeElement(locate('.jstree-anchor').withText(auto_folder_en));
}

function wj9DeleteTestFolder(I, DT, randomNumber) {
     var test_folder = 'TEST-autotest-' + randomNumber;
     // vymazanie test priecinka
     I.say('Vymazavam testovaci adresar TEST-autotest-');
     I.jstreeClick(test_folder);
     I.wait(1);
     I.click(DT.btn.tree_delete_button);
     DT.waitForLoader();
     I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
     I.waitForText('Naozaj chcete zmazať položku?', 10, '.col-sm-7.offset-sm-4');
     I.click('Zmazať');
     I.waitForElement('.jstree-anchor', 20);
     DT.waitForLoader();
     I.dontSeeElement(locate('.jstree-anchor').withText(test_folder));
}

Scenario('reset table settings', ({I, DT}) => {
     I.relogin("admin");
     I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
     DT.resetTable();
     I.jstreeReset();
});

Scenario('reset nastavenej domeny', ({ I }) => {
     I.logout();
 });

Scenario('Zrkadlenie priecinkov', async ({ I, DTE, Document }) => {
     // vytvorenie mirroring hlavneho priecinka
     I.wj9CreateMirroringFolder(randomNumber, true);

     // nastavenie structureMirroringConfig
     // zober ID priecinkov sk a en
     I.say('Kopirujem ID priecinkov');
     I.jstreeClick(auto_folder_sk);
     I.wait(1);
     skID = await I.grabValueFrom('#tree-folder-id');
     I.say('ID sk priecinka je: ' + skID);

     I.jstreeClick(auto_folder_en);
     I.wait(1);
     enID = await I.grabValueFrom('#tree-folder-id');
     I.say('ID en priecinka je: ' + enID);

     I.jstreeClick(auto_folder_de);
     I.wait(1);
     deID = await I.grabValueFrom('#tree-folder-id');
     I.say('ID de priecinka je: ' + deID);

     // nastavenie konfiguracnej premennej
     I.say('Nastavujem createMirroringStructure');
     Document.setConfigValue("structureMirroringConfig", skID + ',' + enID + ',' + deID + ':mirroring.tau27.iway.sk');

});
Scenario('vytvorenie 2 podpriecinkov do sk adresara', ({ I, DTE, DT }) => {
     // vytvorenie 2 podpriecinkov do sk adresara
     wj9CreateMirroringSubfolder(I, DTE, DT, randomNumber);
});
Scenario('presun druheho podpriecinka do prveho podpriecinka', ({ I, DT, DTE }) => {
     // presun druheho podpriecinka do prveho podpriecinka
     // presun subfolder2 do subfoldra1 v sk adresari
     I.say('Presuvam druhy subfolder do prveho subfoldra v sk strukture');
     I.jstreeClick(auto_folder_sk);
     I.jstreeClick(auto_subfolder2_sk);
     I.click(DT.btn.tree_edit_button);
     DT.waitForLoader();
     I.click(locate('#editorAppDTE_Field_editorFields-parentGroupDetails').find('button.btn.btn-outline-secondary.btn-vue-jstree-item-edit'));
     I.waitForVisible('#jsTree');
     //I.click(locate('#jsTree').withChild('ul.jstree-container-ul.jstree-children').find('.jstree-icon.jstree-ocl'));
     I.waitForVisible(locate('#jsTree').withDescendant('a.jstree-anchor').withText(auto_folder_sk));
     I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText(auto_folder_sk).find('.jstree-icon.jstree-ocl'));
     I.waitForVisible(locate('#jsTree').withDescendant('ul.jstree-children').withText(auto_subfolder2_sk));
     I.click(locate('#jsTree').find('.jstree-node.jstree-leaf').withText(auto_subfolder1_sk).find('a.jstree-anchor'));
     DTE.save();
     // skontrolujem ci sa pricinok subfolder2 presunul do priecinka subfolder1 v ramci sk struktury
     I.say('Skontrolujem ci sa pricinok subfolder2 presunul do priecinka subfolder1 v ramci sk struktury');
     I.jstreeClick(auto_folder_sk);
     I.dontSeeElement(locate('.jstree-anchor').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder-filled.jstree-themeicon-custom').withText(auto_subfolder2_sk));
     I.jstreeClick(auto_subfolder1_sk);
     within((locate('li.jstree-node.jstree-open.jstree-last').withDescendant('.jstree-anchor.jstree-clicked')), () => {
          I.waitForElement(locate('ul.jstree-children').withText(auto_subfolder2_sk));
     });
     I.wait(1);
     // Skontrolujem ci sa pricinok subfolder2 presunul do priecinka subfolder1 v ramci en struktury
     I.say('Skontrolujem ci sa pricinok subfolder2 presunul do priecinka subfolder1 v ramci en struktury');
     I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText(auto_folder_en).find('.jstree-icon.jstree-ocl'));
     I.jstreeWaitForLoader();
     I.click(locate('.jstree-anchor').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder.jstree-themeicon-custom').withText(auto_subfolder1_sk));
     I.jstreeWaitForLoader();
     within((locate('li.jstree-node.jstree-last.jstree-open').withDescendant('.jstree-anchor.jstree-clicked')), () => {
          I.waitForElement(locate('ul.jstree-children').withText(auto_subfolder2_sk));
     });
     //over, ze vidis len 2 elementy a ze sa skutocne presunul
     I.seeNumberOfVisibleElements(locate('li.jstree-node > a').withText(auto_subfolder2_sk), 2)
});
Scenario('vymazanie priecinka subfolder2 z priecinka subfolder 1', ({ I, DT, DTE }) => {
     // vymazanie priecinka subfolder2 z priecinka subfolder 1
     // vymazanie podpriecinka v sk strukture
     I.say('Vymazanie podpriecinka v sk strukture');
     I.jstreeClick(auto_folder_en);
     I.jstreeClick(auto_subfolder1_sk);
     I.jstreeClick(auto_folder_sk);
     I.jstreeClick(auto_subfolder1_sk);
     I.jstreeClick(auto_subfolder2_sk);
     I.wait(1);
     I.click(DT.btn.tree_delete_button);
     DT.waitForLoader();
     I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
     I.waitForText('Naozaj chcete zmazať položku?', 10, '.col-sm-7.offset-sm-4');
     I.click('Zmazať');
     DTE.waitForLoader();
     I.waitForElement('.jstree-anchor', 20);
     DT.waitForLoader();
     // overim ci sa v sk strukture vymazal priecinok subfolder2
     I.say('Overim ci sa v sk strukture vymazal priecinok subfolder2');
     I.jstreeClick(auto_folder_sk);
     within((locate('li.jstree-node.jstree-open').withText(auto_folder_sk)), () => {
          I.seeElement(locate('.jstree-anchor').withText(auto_subfolder1_sk));
          I.dontSeeElement(locate('.jstree-anchor').withText(auto_subfolder2_sk));
     });
     I.wait(1);
     // overim ci sa v en strukture vymazal priecinok subfolder2
     I.say('Overim ci sa v en strukture vymazal priecinok subfolder2');
     I.jstreeClick(auto_folder_en);
     within((locate('li.jstree-node.jstree-open').withText(auto_folder_en)), () => {
          I.seeElement(locate('.jstree-anchor').withText(auto_subfolder1_sk));
          I.dontSeeElement(locate('.jstree-anchor').withText(auto_subfolder2_sk)); // TODO - priecinok sa v en strukture nevymazal
     });
});
Scenario('presun hlavneho priecinka sk do ineho priecinka TEST', ({ I, DTE, DT }) => {
     // presun hlavneho priecinka sk do ineho priecinka TEST
     //wj9MoveMainFolder(I, DTE, DT, randomNumber);
});

Scenario('vypnutie synchronizacie na DE a overenie ze sa nezmeni', ({ I, DT, DTE, Document }) => {
     I.say('Nastavujem createMirroringStructure');
     Document.setConfigValue("structureMirroringConfig", skID + ',' + enID + ':mirroring.tau27.iway.sk');

     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=9811');
     DT.waitForLoader();
     I.jstreeWaitForLoader();

     //
     I.say("Change sort priority value");
     I.jstreeClick(auto_folder_sk);
     I.click(auto_folder_sk, "#datatableInit_wrapper");
     DTE.waitForEditor();
     I.clickCss("#pills-dt-datatableInit-menu-tab");
     DTE.fillField("sortPriority", "11");
     DTE.save();

     //
     I.say("Check sort priority value");
     I.jstreeClick(auto_folder_en);
     I.click(auto_folder_en, "#datatableInit_wrapper");
     DTE.waitForEditor();
     I.clickCss("#pills-dt-datatableInit-menu-tab");
     I.seeInField("#DTE_Field_sortPriority", "11");
     DTE.cancel();

     //
     I.say("Check sort priority not changed in DE version");
     I.jstreeClick(auto_folder_de);
     I.click(auto_folder_de, "#datatableInit_wrapper");
     DTE.waitForEditor();
     I.clickCss("#pills-dt-datatableInit-menu-tab");
     I.seeInField("#DTE_Field_sortPriority", "10");
     DTE.cancel();

     Document.setConfigValue("structureMirroringConfig", skID + ',' + enID + ',' + deID + ':mirroring.tau27.iway.sk');
});

Scenario('vymazanie hlavneho sk priecinka z test priecinka', ({ I, DT, DTE }) => {
     // vymazanie hlavneho sk priecinka z test priecinka
     wj9DeleteMainFolder(I, DT, DTE, randomNumber);
});
Scenario('vymazanie testovacieho priecinka TEST', ({ I, DT }) => {
     // vymazanie testovacieho priecinka TEST
     //wj9DeleteTestFolder(I, DT, randomNumber);
});

Scenario('odhlasenie', ({ I }) => {
     I.logout();
});