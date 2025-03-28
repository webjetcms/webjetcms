Feature('webpages.pages-mirroring-wj9');

var auto_folder_sk, auto_folder_en, auto_subfolder1_sk, auto_subfolder2_sk, randomNumber;

Before(({ I, login }) => {
     login('admin');
     if (typeof randomNumber == "undefined") {
          randomNumber = I.getRandomTextShort();
          I.say("randomNumber=" + randomNumber);
          auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
          auto_folder_en = 'en-mir-autotest-' + randomNumber;
          auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
          auto_subfolder2_sk = 'sk-mir-subfolder2-' + randomNumber;
     }
     I.resizeWindow(1600, 1200);
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
});

function wj9CreateMirroringWebpage(I, DT, DTE, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var auto_webpage1_sk = 'sk-webpage1-autotest-' + randomNumber;
     var auto_webpage2_sk = 'sk-webpage2-autotest-' + randomNumber;
     // vytvorenie 1. webstranky v sk adresari
     I.say('Vytvaram 1. webstranku v sk adresari');
     I.jstreeClick(auto_folder_sk);
     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     //I.clickCss("#pills-dt-datatableInit-content-tab");
     //I.waitForVisible('#cke_1_toolbox');
     I.clickCss('#pills-dt-datatableInit-basic-tab');
     I.waitForElement(locate('.col-sm-4.col-form-label').withText('Názov web stránky'));
     I.fillField('#DTE_Field_title', auto_webpage1_sk);
     DTE.save();
     // vytvorenie 2. webstranky v sk adresari
     I.say('Vytvaram 2. webstranku v sk adresari');
     I.jstreeClick(auto_folder_sk);
     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     //I.clickCss("#pills-dt-datatableInit-content-tab");
     //I.waitForVisible('#cke_1_toolbox');
     I.clickCss('#pills-dt-datatableInit-basic-tab');
     I.waitForElement(locate('.col-sm-4.col-form-label').withText('Názov web stránky'));
     I.fillField('#DTE_Field_title', auto_webpage2_sk);
     DTE.save();
     // skontrolujem ci sa vytvorili webstranky i v en adresari - nepublikovane
     I.say('Skontrolujem ci sa vytvorili webstranky i v en adresari - nepublikovane');
     I.jstreeClick(auto_folder_en);
     I.wait(1);
     within({ css: 'table#datatableInit>tbody' }, () => {
          I.seeElement(locate('tr.even.is-not-public').withText(auto_webpage1_sk));
          I.seeElement(locate('tr.odd.is-not-public').withText(auto_webpage2_sk));
     });
}

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

     I.seeElement(locate('li.jstree-node.jstree-leaf').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder.jstree-themeicon-custom').withText(auto_subfolder1_sk).inside( locate(".jstree-node.jstree-open").withText(auto_folder_en) ));
     I.seeElement(locate('li.jstree-node.jstree-leaf').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder.jstree-themeicon-custom').withText(auto_subfolder2_sk).inside( locate(".jstree-node.jstree-open").withText(auto_folder_en) ));
}

function wj9DeleteSubpage(I, DT, DTE, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var auto_webpage1_sk = 'sk-webpage1-autotest-' + randomNumber;
     var auto_webpage2_sk = 'sk-webpage2-autotest-' + randomNumber;
     var delete_webpage_button = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider'));
     // vymazanie podstranky sk-webpage2
     I.say('Vymazavam druhu podstranku v sk strukture');
     I.jstreeClick(auto_folder_sk);
     I.waitForElement(locate('tbody>tr.odd').withText(auto_webpage2_sk));
     I.click(locate('tbody>tr.odd').withText(auto_webpage2_sk).find('td.dt-select-td.cell-not-editable'));
     I.waitForElement(locate('tbody>tr.odd.selected').withText(auto_webpage2_sk));
     I.click(delete_webpage_button);
     DT.waitForLoader();
     I.waitForVisible('.DTE.modal-content.DTE_Action_Remove');
     I.waitForText('Naozaj chcete zmazať položku?', 10, '.col-sm-7.offset-sm-4');
     I.click('Zmazať');
     DTE.waitForLoader();
     I.waitForElement('.jstree-anchor', 20);
     DT.waitForLoader();
     // overim ci sa stranka vymazala zo sk adresara
     I.say('Overujem ci sa stranka vymazala z sk adresara');
     I.wait(1);
     I.dontSeeElement(locate('tbody>tr.odd').withText(auto_webpage2_sk));
     // overim ci sa stranka vymazala aj v en adresari
     I.say('Overujem ci sa stranka vymazala aj v en adresari');
     I.wait(1);
     I.jstreeClick(auto_folder_en);
     I.waitForElement(locate('tbody>tr.even.is-not-public').withText(auto_webpage1_sk));
     I.dontSeeElement(locate('tbody>tr.odd.is-not-public').withText(auto_webpage2_sk));
}

function wj9ChangeMainPage(I, DTE, DT, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var auto_webpage1_sk = 'sk-webpage1-autotest-' + randomNumber;
     // zmena hlavnej stranky v sk adresari
     I.say('Menim hlavnu stranku v sk adresari');
     I.jstreeClick(auto_folder_sk);
     I.click(DT.btn.tree_edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click(locate('#editorAppDTE_Field_editorFields-defaultDocDetails').find('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'));
     I.waitForVisible('#jsTree');
     I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText(auto_folder_sk).find('.jstree-icon.jstree-ocl'));
     I.waitForVisible(locate('#jsTree').withDescendant('ul.jstree-children').withText(auto_webpage1_sk));
     I.click(locate('#jsTree').find('.jstree-node.jstree-leaf').withText(auto_webpage1_sk).find('a.jstree-anchor'));
     DTE.save();
     // over ci sa v hlavnom sk adresari zmenila hlavna stranka na webpage1
     I.say('Over ci sa v hlavnom sk adresari zmenila hlavna stranka na webpage1');
     I.wait(1);
     I.jstreeClick(auto_folder_sk);
     I.seeElement(locate('tbody>tr.even.is-default-page').withText(auto_webpage1_sk));
     // over ci sa v en adresari zmenila hlavna stranka na sk-webpage1
     I.say('Over ci sa v hlavnom en adresari zmenila hlavna stranka na webpage1');
     I.jstreeClick(auto_folder_en);
     within({ css: 'table#datatableInit>tbody' }, () => {
          I.dontSeeElement(locate('tr.odd.is-default-page').withText(auto_folder_en));
          I.seeElement(locate('tr.even.is-default-page.is-not-public').withText(auto_webpage1_sk));
     });
}

function wj9MoveSubpage(I, DT, DTE, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var auto_webpage1_sk = 'sk-webpage1-autotest-' + randomNumber;
     var auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
     // presuvam stranku webpage1 do adresara
     I.say('Presuvam webpage1 do podpriecinka sk-mir-subfolder1');
     I.jstreeClick(auto_folder_sk);
     I.waitForElement(locate('tbody>tr.even.is-default-page').withText(auto_webpage1_sk));
     I.click(locate('tbody>tr.even.is-default-page').withText(auto_webpage1_sk).find('td.dt-select-td.cell-not-editable'));
     I.waitForElement(locate('tbody>tr.even.is-default-page.selected').withText(auto_webpage1_sk));
     I.click(DT.btn.edit_button);
     I.waitForVisible('#datatableInit_modal');
     I.clickCss('#pills-dt-datatableInit-basic-tab');
     I.waitForElement(locate('.col-sm-4.col-form-label').withText('Nadradený priečinok'));
     I.click(locate('#editorAppDTE_Field_editorFields-groupDetails').find('button.btn.btn-outline-secondary.btn-vue-jstree-item-edit'));
     I.waitForVisible('#jsTree');
     I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText(auto_folder_sk).find('.jstree-icon.jstree-ocl'));
     I.waitForVisible(locate('#jsTree').withDescendant('ul.jstree-children').withText(auto_subfolder1_sk));
     I.click(locate('#jsTree').find('.jstree-node.jstree-leaf').withText(auto_subfolder1_sk).find('a.jstree-anchor'));
     DTE.save();
     // overim ci sa stranka webpage1 presunula do priecinka sk-mir-subfolder1
     I.say('Overim ci sa stranka webpage1 presunula do priecinka sk-mir-subfolder1');
     I.dontSeeElement(locate('tbody>tr.even.is-default-page').withText(auto_webpage1_sk));
     I.jstreeClick(auto_subfolder1_sk);
     I.seeElement(locate('tbody>tr.odd').withText(auto_webpage1_sk));
     I.seeElement(locate('tbody>tr.even.is-default-page').withText(auto_subfolder1_sk));
     // overim ci sa stranka webpage1 premiestnila i do en struktury
     I.say('Overim ci sa stranka webpage1 premiestnila i do en podpriecinka subfolder1');
     I.jstreeClick(auto_folder_en);
     I.wait(1);
     DT.waitForLoader();

     I.dontSeeElement(locate('tbody>tr.even.is-default-page.is-not-public').withText(auto_webpage1_sk));
     I.wait(5);

     //I.click(locate('.jstree-node').withText(auto_subfolder1_sk).inside( locate(".jstree-node.jstree-open").withText(auto_folder_en) ) );
     //I.click(locate('li.jstree-node a.jstree-anchor').withText(auto_subfolder1_sk));
     //I.click( locate('li.jstree-node.jstree-leaf').withDescendant('.jstree-icon.jstree-themeicon.far.fa-folder.jstree-themeicon-custom').withText(auto_subfolder1_sk) );
     I.click( locate('a').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder').withText(auto_subfolder1_sk) );

     DT.waitForLoader();
     within({ css: 'table#datatableInit>tbody' }, () => {
          I.seeElement(locate('tr.odd.is-not-public').withText(auto_webpage1_sk));
          I.seeElement(locate('tr.even.is-default-page.is-not-public').withText(auto_subfolder1_sk));
     });

     //kedze sme si nie isty, ci to nekliklo do SK adresara spravme reload a este raz
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.say('Verifikujem presun');
     I.jstreeClick(auto_folder_en);
     I.wait(1);
     DT.waitForLoader();

     I.dontSeeElement(locate('tbody>tr.even.is-default-page.is-not-public').withText(auto_webpage1_sk));
     I.wait(5);

     //I.click(locate('.jstree-node').withText(auto_subfolder1_sk).inside( locate(".jstree-node.jstree-open").withText(auto_folder_en) ) );
     //I.jstreeClick(auto_subfolder1_sk);
     I.click( locate('a').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder').withText(auto_subfolder1_sk) );

     DT.waitForLoader();
     within({ css: 'table#datatableInit>tbody' }, () => {
          I.seeElement(locate('tr.odd.is-not-public').withText(auto_webpage1_sk));
          I.seeElement(locate('tr.even.is-default-page.is-not-public').withText(auto_subfolder1_sk));
     });
}

function wj9DeleteMainFolder(I, DT, DTE, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     // vymaz hlavny sk adresar
     I.say('Vymazam hlavny sk adresar');
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
     I.wait(1);
     // skontroluj ci je vymazany hlavny sk folder
     I.say('Skontroluj ci je vymazany hlavny sk folder');
     I.dontSeeElement(locate('.jstree-anchor').withText(auto_folder_sk));
     I.wait(1);
     // skontroluj ci je vymazany i hlavny en folder
     I.say('Skontroluj ci je vymazany i hlavny en folder');
     I.dontSeeElement(locate('.jstree-anchor').withText(auto_folder_en));
}

Scenario('reset nastavenej domeny', ({ I, DT }) => {
     DT.resetTable();
     I.logout();
 });

Scenario('Zrkadlenie webstranok wj9', async ({ I, DT, DTE }) => {
     // vytvorenie mirroring hlavneho priecinka
     I.wj9CreateMirroringFolder(randomNumber, false);

     // nastavenie structureMirroringConfig
     // zober ID priecinkov sk a en
     I.say('Kopirujem ID priecinkov');
     I.jstreeClick(auto_folder_sk);
     I.wait(1);
     const skID = await I.grabValueFrom('#tree-folder-id');
     I.say('ID sk priecinka je: ' + skID);
     I.jstreeClick(auto_folder_en);
     I.wait(1);
     const enID = await I.grabValueFrom('#tree-folder-id');
     I.say('ID en priecinka je: ' + enID);
     // nastavenie konfiguracnej premennej
     I.say('Nastavujem createMirroringStructure');
     I.amOnPage('/admin/v9/settings/configuration/');
     I.fillField('.form-control.form-control-sm.filter-input.dt-filter-name', 'structureMirroringConfig');
     I.wait(1);
     I.clickCss('.filtrujem.btn.btn-sm.btn-outline-secondary.dt-filtrujem-name')
     I.waitForElement(locate('#configurationDatatable').withText('structureMirroringConfig'));
     I.click(locate('#configurationDatatable').withDescendant('.datatable-column-width').withText('structureMirroringConfig').find('.ti.ti-pencil'));
     DTE.waitForEditor("configurationDatatable");
     I.waitForValue('#DTE_Field_description', 'Nastavenie zrkadlenia jazykovych verzii stranok.', 10);
     I.wait(1);
     I.clearField('#DTE_Field_value');
     I.fillField('#DTE_Field_value', skID + ',' + enID + ':mirroring.tau27.iway.sk');
     I.wait(1);
     DTE.save();
     I.wait(1);
     I.waitForElement(locate('#configurationDatatable').withDescendant('tr.odd').withText(skID + ',' + enID + ':mirroring.tau27.iway.sk'), 10);
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.wait(1);
});
Scenario('vytvorenie 2 webstranok v sk adresari', ({ I, DT, DTE }) => {
     // vytvorenie 2 webstranok v sk adresari
     wj9CreateMirroringWebpage(I, DT, DTE, randomNumber);
});
Scenario('vytvorenie 2 podpriecinkov do sk adresara', ({ I, DTE, DT }) => {
     // vytvorenie 2 podpriecinkov do sk adresara
     wj9CreateMirroringSubfolder(I, DTE, DT, randomNumber);
});
Scenario('vymazanie druhej podstranky sk-webpage2', ({ I, DT, DTE }) => {
     // vymazanie druhej podstranky sk-webpage2
     wj9DeleteSubpage(I, DT, DTE, randomNumber);
});
Scenario('zmena hlavnej stranky sk hlavneho adresara', ({ I, DTE, DT }) => {
     // zmena hlavnej stranky sk hlavneho adresara
     wj9ChangeMainPage(I, DTE, DT, randomNumber);
});
Scenario('presun stranky webpage1 do podpriecinka sk-mir-subfolder1', ({ I, DT, DTE }) => {
     // presun stranky webpage1 do podpriecinka sk-mir-subfolder1
     wj9MoveSubpage(I, DT, DTE, randomNumber);
});
Scenario('vymazanie hlavneho sk priecinka z test priecinka', ({ I, DT, DTE }) => {
     // vymazanie hlavneho sk priecinka z test priecinka
     wj9DeleteMainFolder(I, DT, DTE, randomNumber);
});

Scenario('odhlasenie', ({ I }) => {
     I.logout();
});