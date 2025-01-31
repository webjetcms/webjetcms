Feature('webpages.webpage-select-options');

var folder_name, randomNumber;

Before(({ I, login }) => {
     login('admin');
     if (typeof folder_name == "undefined") {
          randomNumber = I.getRandomText();
          I.say("randomNumber=" + randomNumber);
          folder_name = 'name-autotest-' + randomNumber;
     }
});

Scenario('Priprav strukturu', ({ I }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/');

     // vytvorenie hlavneho priecinka a jeho dvoch podpriecinkov
     I.createFolderStructure(randomNumber, false);

     // vytvorenie webstranky
     I.createNewWebPage(randomNumber);
});

Scenario('Webstranky - vyber z moznosti', ({ I, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/');
     I.jstreeNavigate([folder_name]);

     // editovanie webstranky
     I.editCreatedWebPage(randomNumber);

     // Aktivovat - Zobrazenie web stranky a vyhladanie cez vyhladavace
     I.say('Aktivovanie zobrazenia web stranky a vyhladavanie cez vyhladavace');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_available_0').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_searchable_0').find('.form-check-label'));

     // ------------------------ ZALOZKA SABLONA ------------------
     I.say('Select options na zalozke Sablona');
     I.click('#pills-dt-datatableInit-template-tab');
     I.waitForText('Šablóna web stránky', 10);

     // Sablona web stranky
     I.dtEditorSelectOption('tempId', 'Homepage');
     I.waitForElement(locate('.filter-option-inner-inner').withText('Homepage'), 10);

     // Hlavicka
     I.dtEditorSelectOption('headerDocId', 'Prázdne');
     I.waitForElement(locate('.filter-option-inner-inner').withText('Prázdne'), 10);

     // Paticka
     I.dtEditorSelectOption('footerDocId', 'Prázdne');
     I.waitForElement(locate('.filter-option-inner-inner').withText('Prázdne'), 10);

     // Hlavne menu
     I.dtEditorSelectOption('menuDocId', 'System');
     I.waitForElement(locate('.filter-option-inner-inner').withText('System'), 10);

     // Bocne menu
     I.dtEditorSelectOption('rightMenuDocId', 'System');
     I.waitForElement(locate('.filter-option-inner-inner').withText('System'), 10);

     // HTML kod do hlavicky
     I.fillField('#DTE_Field_htmlHead', '<title>This is an autotest</title>');

     // ------------------------ ZALOZKA NAVIGACIA ------------------
     I.say('Select options na zalozke Navigacia');
     I.click('#pills-dt-datatableInit-menu-tab');
     I.waitForText('Poradie usporiadania', 10);

     // Zobrazenie odkazu neprihlasenemu uzivatelovi - Menu
     I.dtEditorSelectOption('showInMenu', 'Nezobraziť');
     I.waitForElement(locate('.filter-option-inner-inner').withText('Nezobraziť'), 10);
     DTE.save();

     // ------------------------ KONTROLA ULOZENIA UDAJOV ------------------
     // Zalozka SABLONA
     I.checkEditedWebPage(randomNumber);
     I.click('#pills-dt-datatableInit-template-tab');
     I.waitForText('Šablóna web stránky', 10);
     I.waitForElement(locate('.filter-option-inner-inner').withText('Homepage'), 10); // Sablona web stranky
     I.waitForElement(locate('.filter-option-inner-inner').withText('Prázdne'), 10); // Hlavicka
     I.waitForElement(locate('.filter-option-inner-inner').withText('Prázdne'), 10); // Paticka
     I.waitForElement(locate('.filter-option-inner-inner').withText('System'), 10); // Hlavne menu
     I.waitForElement(locate('.filter-option-inner-inner').withText('System'), 10); // Bocne menu
     I.seeInField('#DTE_Field_htmlHead', '<title>This is an autotest</title>');

     // Zalozka NAVIGACIA
     I.click('#pills-dt-datatableInit-menu-tab');
     I.waitForText('Poradie usporiadania', 10);
     I.waitForElement(locate('.filter-option-inner-inner').withText('Nezobraziť'), 10); // Zobrazenie odkazu neprihlasenemu uzivatelovi - Menu
     I.dtEditorCancel();
});

Scenario('Zmaz strukturu', ({ I }) => {
     //ak zostalo nieco zaseknute na editacii reloadni aby sa dalo dobre zmazat
     I.amOnPage('/admin/v9/webpages/web-pages-list/');
     I.jstreeNavigate([folder_name]);

     // vymazanie vytvoreneho priecinka name-autotest
     I.deleteFolderStructure(randomNumber);
});