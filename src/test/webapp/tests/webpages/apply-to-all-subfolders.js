Feature('webpages.apply-to-all-subfolders');

Before(({ I, login }) => {
     login('admin');
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
});

Scenario('reset table settings', ({I, DT}) => {
  I.relogin("admin");
  I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
  DT.resetTable();
});

// APPLY-TO-ALL-SUBFOLDERS.JS test podpriecinkov
function testSubFolders(I, DT, DTE, randomNumber, nameOfTest, selectValue) {
  var auto_name = 'name-autotest-' + randomNumber;
  var auto_folder_one = 'subone-autotest-' + randomNumber;
  var auto_folder_two = 'subtwo-autotest-' + randomNumber;
  var edit_button = (locate('.tree-col').find('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning'));
  var close_button = '.btn.btn-outline-secondary.btn-close-editor';
  // upravit priecinok name-autotest
  I.say('Testujem ' + nameOfTest.toUpperCase() + ' s hodnotou ' + selectValue.toUpperCase());
  I.click(edit_button);
  DTE.waitForEditor("groups-datatable");

  // podla nazvu nameOfTest vyberie zalozku,selectValue pre vsetky podpriecinky a ulozi zmeny
  I.say('Vyberam hodnotu ' + selectValue.toUpperCase() + ' pre vsetky podpriecinky priecinka name-autotest');
  if (nameOfTest === 'webPage') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('Šablóna pre web stránky', 10);
    DTE.selectOption('tempId', selectValue);
    I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceTemplateToSubgroupsAndPages_0').find('.form-check-label'));
  } else if (nameOfTest === 'language') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('Jazyk', 10);
    DTE.selectOption('lng', selectValue);
    I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceLngToSubFolders_0').find('.form-check-label'));
  } else if (nameOfTest === 'htmlCode') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('HTML kód novej stránky', 10);
    DTE.selectOption('newPageDocIdTemplate', selectValue);
    I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceNewPageDocIdTemplateSubFolders_0').find('.form-check-label'));
  } else if (nameOfTest === 'unloggedUser') {
    I.click('#pills-dt-groups-datatable-menu-tab');
    I.waitForText('Zobrazenie odkazu neprihlásenému používateľovi', 10);
    DTE.selectOption('menuType', selectValue);
    I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceMenuTypeSubfolders_0').find('.form-check-label'));
  } else if (nameOfTest === 'loggedUser') {
    I.click('#pills-dt-groups-datatable-menu-tab');
    I.waitForText('Zobrazenie odkazu prihlásenému používateľovi', 10);
    DTE.selectOption('loggedMenuType', selectValue);
    I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceLoggedMenuTypeSubfolders_0').find('.form-check-label'));
  }
  DTE.save();
  I.wait(5);
  I.amOnPage('/admin/v9/webpages/web-pages-list#/');
  DT.waitForLoader();
  I.waitForText(auto_name, 20);

  // kontrola priecinok name-autotest
  I.say('kontrolujem priecinok name-autotest');
  I.click(auto_name);
  I.click(edit_button);
  DTE.waitForEditor("groups-datatable");
  I.waitForText(auto_name, 5);
  if (nameOfTest === 'webPage') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('Šablóna pre web stránky', 10);
  } else if (nameOfTest === 'language') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('Jazyk', 10);
  } else if (nameOfTest === 'htmlCode') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('HTML kód novej stránky', 10);
  } else if (nameOfTest === 'unloggedUser') {
    I.click('#pills-dt-groups-datatable-menu-tab');
    I.waitForText('Zobrazenie odkazu neprihlásenému používateľovi', 10);
  } else if (nameOfTest === 'loggedUser') {
    I.click('#pills-dt-groups-datatable-menu-tab');
    I.waitForText('Zobrazenie odkazu prihlásenému používateľovi', 10);
  }
  I.waitForElement(locate('.dropdown-toggle.btn.btn-outline-secondary').withText(selectValue), 5);
  I.click(close_button);
  I.waitForText('Zoznam web stránok', 10);

  // kontrola podpriecinok folder_one
  I.say('kontrolujem podpriecinok folder_one');
  I.jstreeClick(auto_folder_one);
  DT.waitForLoader();
  I.click(edit_button);
  DTE.waitForEditor("groups-datatable");
  I.waitForText(auto_folder_one, 5);
  if (nameOfTest === 'webPage') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('Šablóna pre web stránky', 10);
  } else if (nameOfTest === 'language') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('Jazyk', 10);
  } else if (nameOfTest === 'htmlCode') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('HTML kód novej stránky', 10);
  } else if (nameOfTest === 'unloggedUser') {
    I.click('#pills-dt-groups-datatable-menu-tab');
    I.waitForText('Zobrazenie odkazu neprihlásenému používateľovi', 10);
  } else if (nameOfTest === 'loggedUser') {
    I.click('#pills-dt-groups-datatable-menu-tab');
    I.waitForText('Zobrazenie odkazu prihlásenému používateľovi', 10);
  }
  I.waitForElement(locate('.dropdown-toggle.btn.btn-outline-secondary').withText(selectValue), 5);
  I.click(close_button);
  I.waitForText('Zoznam web stránok', 10);

  // kontrola podpriecinok folder2
  I.say('kontrolujem podpriecinok folder_two');
  I.jstreeClick(auto_folder_two);
  DT.waitForLoader();
  I.click(edit_button);
  DTE.waitForEditor("groups-datatable");
  I.waitForText(auto_folder_two, 5);
  if (nameOfTest === 'webPage') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('Šablóna pre web stránky', 10);
  } else if (nameOfTest === 'language') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('Jazyk', 10);
  } else if (nameOfTest === 'htmlCode') {
    I.click('#pills-dt-groups-datatable-template-tab');
    I.waitForText('HTML kód novej stránky', 10);
  } else if (nameOfTest === 'unloggedUser') {
    I.click('#pills-dt-groups-datatable-menu-tab');
    I.waitForText('Zobrazenie odkazu neprihlásenému používateľovi', 10);
  } else if (nameOfTest === 'loggedUser') {
    I.click('#pills-dt-groups-datatable-menu-tab');
    I.waitForText('Zobrazenie odkazu prihlásenému používateľovi', 10);
  }
  I.waitForElement(locate('.dropdown-toggle.btn.btn-outline-secondary').withText(selectValue), 5);
  I.click(close_button);
  I.waitForText('Zoznam web stránok', 10);
};

Scenario('1. Sablona pre web stranky - webPage', ({ I, DT, DTE }) => {
     var randomNumber = I.getRandomText();
     // vytvorenie hlavneho priecinka a jeho dvoch podpriecinkov
     I.createFolderStructure(randomNumber);

     // otestovanie sablony pre web stranky cez atribut "webPage" - vyber z moznosti -> Article, Generic, Homepage, Microsite - blue, Microsite - green, Microsite - yellow, Newsletter, Newsletter EN, Subpage, Subpage DE, Subpage EN
     testSubFolders(I, DT, DTE, randomNumber, 'webPage', 'Generic');

     // vymazanie vytvoreneho priecinka name-autotest
     I.deleteFolderStructure(randomNumber);
});

Scenario('2. Jazyk - language', ({ I, DT, DTE }) => {
     var randomNumber = I.getRandomText();
     I.createFolderStructure(randomNumber);

     // otestovanie jazyka stranky cez atribut "language" - vyber z moznosti -> Prevziať zo šablóny, Slovenský, Český, Anglický, Nemecký, Poľský, Maďarský, Chorvátsky, Ruský, Španielsky
     testSubFolders(I, DT, DTE, randomNumber, 'language', 'Španielsky'); // odstrániť dĺžen v slove Španielský

     I.deleteFolderStructure(randomNumber);
});

Scenario('3. HTML kód novej stránky - htmlCode', ({ I, DT, DTE }) => {
     var randomNumber = I.getRandomText();
     I.createFolderStructure(randomNumber);

     // otestovanie HTML kodu novej stranky cez atribut "htmlCode" - vyber z moznosti -> Prázdna stránka, Normálna stránka, Stránka s nadpisom a 2 stĺpcami
     testSubFolders(I, DT, DTE, randomNumber, 'htmlCode', 'Stránka s nadpisom a 2 stĺpcami'); // pridat diakritiku do Stranka s nadpisom a 2 stlpcami

     I.deleteFolderStructure(randomNumber);
});

Scenario('4. Menu zobrazenie odkazu neprihlasenemu uzivatelovi - unloggedUser', ({ I, DT, DTE }) => {
     var randomNumber = I.getRandomText();
     I.createFolderStructure(randomNumber);

     // otestovanie Menu stranky neprihlasenemu používateľovi cez atribut "unloggedUser" - vyber z moznosti -> Štandardné, Nezobrazovať, Bez podadresárov, Všetko (adresáre aj stránky)
     testSubFolders(I, DT, DTE, randomNumber, 'unloggedUser', 'Zobraziť bez podpriečinkov');

     I.deleteFolderStructure(randomNumber);
});

Scenario('5. Menu zobrazenie odkazu prihlasenemu uzivatelovi - loggedUser', ({ I, DT, DTE }) => {
     var randomNumber = I.getRandomText();
     I.createFolderStructure(randomNumber);

     // otestovanie Menu stranky prihlasenemu používateľovi cez atribut "loggedUser" - vyber z moznosti -> Rovnako ako pre neprihláseného, Štandardné, Nezobrazovať, Bez podadresárov, Všetko (adresáre aj stránky)
     testSubFolders(I, DT, DTE, randomNumber, 'loggedUser', 'Zobraziť');

     I.deleteFolderStructure(randomNumber);
});