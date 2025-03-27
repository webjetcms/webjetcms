Feature('webpages.group-defaultdocid');

var auto_name, auto_folder, auto_webPage, auto_todo;

Before(({ I, login }) => {
     login('admin');
     I.amOnPage('/admin/v9/webpages/web-pages-list/');

     if (typeof auto_name=="undefined") {
          var randomNumber = I.getRandomText();
          auto_name = 'name-autotest-' + randomNumber;
          auto_folder = 'folder-autotest-' + randomNumber;
          auto_webPage = 'webpage-autotest-' + randomNumber;
          auto_todo = 'TODO-autotest-' + randomNumber;
     }

});

Scenario('Hlavna stranka priecinka @singlethread', async ({ I, DT, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.waitForText('Webové stránky', 10);
     DT.resetTable();

     // 1. pridanie noveho priecinka folder-autotest
     I.say('1. Pridanie noveho priecinka folder-autotest');
     I.click(DT.btn.tree_add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', auto_folder);
     I.groupSetRootParent();
     DTE.save();
     I.waitForText(auto_folder, 10);
     I.wait(1);

     // 2. pridanie noveho priecinka name-autotest
     I.say('2. Pridanie noveho priecinka name-autotest');
     I.refreshPage();
     I.waitForText('Webové stránky', 10);
     I.click(DT.btn.tree_add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', auto_name);
     I.groupSetRootParent();
     DTE.save();
     I.waitForText(auto_name, 10);
     I.wait(1);

     // 3. kontrola nastavenia hlavnej stranky priecinka name-autotest = rovnaka ako nazov
     I.say('3. kontrola nastavenia hlavnej stranky priecinka name-autotest');
     I.click(auto_name);
     I.seeElement(locate('.odd.is-default-page').withText(auto_name));

     // 4. pridanie novej stranky
     I.say('4. Priradenie novej stranky priecinku name-autotestu');
     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     I.wait(2);
     // pridanie obsahu
     I.clickCss("#pills-dt-datatableInit-content-tab");
     await DTE.fillCkeditor('<p>This is an autotest</p>');
     // pridanie nazvu web stranky a polozky v menu
     I.click(locate('#pills-dt-datatableInit-basic-tab').withText('Základné'));
     I.fillField('#DTE_Field_title', auto_webPage);
     I.forceClick('#DTE_Field_navbar');
     I.fillField('#DTE_Field_navbar', auto_webPage);
     I.forceClick('Pridať', '#datatableInit_modal');

     DTE.waitForLoader();
     DT.resetTable();

     I.seeElement(locate('.even').withText(auto_webPage), 5);
     I.wait(1);

     // 5. zmena hlavnej stranky z daneho priecinka
     I.say('5. Zmena hlavnej stranky priecinka name-autotest z tohoto priecinka');
     I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     I.waitForText(auto_name, 10);
     I.click(locate('#editorAppDTE_Field_editorFields-defaultDocDetails').find('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'));
     // nejde vybrat priecinok, len webstranku
     I.click(locate('.jstree-node.jstree-closed').withText(auto_name).find('a.jstree-anchor'));
     I.waitForText('Vyberte web stránku', 10);
     I.click('.toast-close-button');
     I.wait(2);
     // vyberie web stranku z priecinka name-autotest
     I.click(locate('.jstree-node.jstree-closed').withText(auto_name).find('.jstree-icon.jstree-ocl'));
     I.click(locate('.jstree-node.jstree-leaf').withText(auto_webPage).find('a.jstree-anchor'));
     DTE.save();
     I.wait(1);
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0'); // musim refreshnut stranku, pretoze v tomto kroku WJ prestane reagovat
     I.waitForText('Webové stránky', 10);
     DT.waitForLoader();
     I.click(auto_name);
     I.seeElement(locate('.even.is-default-page').withText(auto_webPage), 5);
     I.wait(1);

     // 6. zmena hlavnej stranky z ineho priecinka
     I.say('6. Zmena hlavnej stranky name-autotest z priecinka folder-autotest');
     I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     I.waitForText(auto_folder, 10);
     // vyber web stranku z priecinka folder-autotest
     I.click(locate('#editorAppDTE_Field_editorFields-defaultDocDetails').find('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'));
     I.waitForElement("#jsTree");
     I.click(locate('.jstree-node.jstree-closed').withText(auto_folder).find('.jstree-icon.jstree-ocl'));
     I.wait(1);
     I.click(locate('.jstree-node.jstree-leaf.jstree-last').withText(auto_folder).find('a.jstree-anchor'));
     I.waitForText(auto_folder, 10);
     DTE.save();
     I.waitForText('Webové stránky', 10);
     I.dontSeeElement('.is-defalt-page');
     I.wait(1);

     // 7.vymazanie vytvorenych priecinkov // TODO vymazanie priecinkov zatial nefunguje
     I.say('7. Vymazanie vytvorenych priecinkov');
     // zmazanie folder-autotest
     I.refreshPage();
     I.waitForText('Webové stránky', 10);
     I.wait(4);
     I.click(locate('.jstree-anchor').withText(auto_folder));
     I.wait(2);
     I.clickCss('div.tree-col .btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
     I.wait(1);
     I.click('Zmazať', "#groups-datatable_modal");
     DTE.waitForLoader();
     I.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).');
     I.waitForText('Webové stránky', 10);
     DT.waitForLoader();
     I.dontSee(auto_folder);
     I.wait(1);
     // zmazanie name-autotest
     I.click(locate('.jstree-anchor').withText(auto_name));
     DT.waitForLoader();
     I.clickCss('div.tree-col .btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
     I.wait(1);
     I.click('Zmazať', "#groups-datatable_modal");
     I.wait(1);
     DT.waitForLoader();
     I.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).');
     I.waitForText('Webové stránky', 10);
     I.dontSee(auto_name);
});

Scenario('Ulozenie prazdnej stranky', ({ I, DT, DTE }) => {
     // TODO "Target object must not be null" -> pridanie novej web stranky bez obsahu - po potvrdeni vypise hlasku v anglictine 'Target object must not be null'
     I.waitForText('Webové stránky', 10);
     I.jstreeClick("Test stavov");
     I.jstreeClick("Tento nie je interný");
     I.wait(1);
     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     I.waitForText('Obsah', 10);
     I.forceClick('Pridať', '#datatableInit_modal');
     I.wait(2);
     I.dontSee('Target object must not be null');

     //zmaz vytvorenu web stranku
     DT.filterContains("title", "Nová web stránka");
     I.clickCss("div.dt-scroll-headInner button.dt-filter-id");
     I.click(DT.btn.delete_button);
     //polozku/y preto len polozk
     I.waitForText("Naozaj chcete zmazať položk", 10);
     I.wait(1);
     I.clickCss("#datatableInit_modal div.DTE_Form_Buttons button.btn-danger");
     DTE.waitForLoader();
});

Scenario('Chybne texty', ({ I, DTE, DT }) => {
     // Zalozka SABLONA - title pre 'Volne objekty' je v anglictine 'Nothing selected'
     I.waitForText('Webové stránky', 10);
     I.jstreeClick("Test stavov");
     I.jstreeClick("Tento nie je interný");
     I.wait(1);
     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     I.waitForElement(locate('#pills-dt-datatableInit-basic-tab.active'), 10);

     I.clickCss('#pills-dt-datatableInit-template-tab');
     I.dontSee('Nothing selected');

     // Zalozka PEREX - chybajuca diakritika v 'Znacky' => dalsia perex skupina
     I.clickCss('#pills-dt-datatableInit-perex-tab');
     I.waitForElement("label[for=DTE_Field_perexGroups]", 10);
     I.scrollTo("label[for=DTE_Field_perexGroups]");
     I.see('ďalšia perex skupina');
     I.dontSee('dalsia perex skupina');

     // Zalozka VOLITELNE POLIA - polka nazvu labela v anglictine 'Alternatívny title'
     I.clickCss('#pills-dt-datatableInit-fields-tab');
     I.see('SEO titulok');
     I.dontSee('Alternatívny title');
     I.forceClick('Zrušiť', '#datatableInit_modal');

     // v modalnom okne pre upravu web stranky nefunguje tlacidlo otaznika "?" - po kliknuti nan sa nic nestane
     // WJ prestava reagovat po ulozeni hlavnej webstranky z ineho priecinka - po kroku 6. v 1. scenari => cela obrazovka stmavne a na nic sa neda kliknut -> pomaha len refresh stranky
});