Feature('webpages.webpage-content');

var folder_name, randomNumber;

Before(({ I, login }) => {
     login('admin');
     if (typeof folder_name == "undefined") {
          randomNumber = I.getRandomText();
          I.say("randomNumber="+randomNumber);
          folder_name = 'name-autotest-' + randomNumber;
     }
});

Scenario('Priprav strukturu', ({ I }) => {

     I.openNewTab();
     I.closeOtherTabs();
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');

     // vytvorenie hlavneho priecinka a jeho dvoch podpriecinkov
     I.createFolderStructure(randomNumber);

     // vytvorenie webstranky
     I.createNewWebPage(randomNumber);
});

Scenario('Obsah web stranky - zakladne ikony', ({ I, DT, DTE, Browser, Document }) => {
     Document.setConfigValue('editorFontAwesomeCssPath', '');
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.jstreeNavigate([folder_name]);

     // editovanie webstranky
     I.editCreatedWebPage(randomNumber);

     // KONTROLA NASTAVENYCH PEREX DATUMOV
     I.say('Kontrolujem nastavene perex datumy');
     I.clickCss('#pills-dt-datatableInit-perex-tab');
     I.seeInField('#DTE_Field_publishStartDate', '01.01.2022 00:00:00');
     I.seeInField('#DTE_Field_publishEndDate', '03.01.2022 00:00:00');
     I.seeInField('#DTE_Field_eventDateDate', '02.01.2022 00:00:00'); // TODO sice sa nastavi hodnota 00:00:00 no v editore je 00:05:00

     //--------------- OBSAH WEBSTRANKY----------------------
     // PRIDANIE TABULKY
     I.say('Pridavam tabulku do obsahu');
     I.clickCss('#pills-dt-datatableInit-content-tab');
     I.dontSeeElement(locate('.cke_path_item').withText('table'));
     I.waitForElement('#trEditor', 10);
     I.clickCss('#trEditor');
     if (Browser.isFirefox()) I.wait(2);
     I.clickCss('#trEditor');
     I.pressKey('ArrowDown');

     I.clickCss('.cke_button.cke_button__table.cke_button_.cke_button_off');
     I.waitForElement('.cke_button.cke_button__table.cke_button_.cke_button_on', 10);
     // volba velkosti tabulky
     for (let i = 0; i <= 3; i++) {
          I.pressKey('ArrowDown');
     }
     for (let j = 0; j <= 2; j++) {
          I.pressKey('ArrowRight');
     }
     I.pressKey('Enter');
     if (Browser.isFirefox()) I.wait(1);
     // vidim tabulku v editore
     I.clickCss(".cke_path a.cke_path_item:nth-child(2)"); //klikni na body p v navbare dole v ckeditore
     if (Browser.isFirefox()) I.wait(3);
     I.pressKey('ArrowUp');
     if (Browser.isFirefox()) {
          //ff ma nejak inak kurzor a je potrebne este ist hore
          I.pressKey('ArrowUp');
     }
     I.waitForElement(locate('.cke_path_item').withText('table'), 10);
     I.wait(1);

     // PRIDANIE OBRAZKA
     I.say('Pridavam obrazok do obsahu');
     I.pressKey('ArrowDown');
     if (Browser.isFirefox()) {
          //ff ma nejak inak kurzor a je potrebne ist dole
          I.pressKey('ArrowDown');
     }
     I.clickCss('.cke_button.cke_button__image.cke_button_off');
     I.waitForText('Vlastnosti obrázka', 20);
     I.switchTo('#wjImageIframeElement');
     I.waitForLoader(".WJLoaderDiv");

     I.waitForElement(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), 20);
     I.wait(1);
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), null, { position: { x: 20, y: 5 } });
     I.waitForText('Foto galéria', 10, ".elfinder-cwd-file");
     I.wait(1);
     I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('Foto galéria'));
     I.waitForVisible(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'), 20);
     I.wait(1);
     I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'));
     I.wait(1);
     I.waitForVisible(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee'), 10);
     I.doubleClick(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee').last());
     I.switchTo();
     //I.clickCss("div.cke_editor_data_dialog td.cke_dialog_footer a.cke_dialog_ui_button_ok");
     I.click('p');
     I.waitForElement(locate('.cke_path_item').withText('img'), 10);
     I.wait(1);

     // VRATIT SPAT KROK - odstrani pridany obrazok
     I.say('Pouzitie kroku spat');
     I.clickCss('.cke_button_icon.cke_button__undo_icon');
     I.dontSeeElement(locate('.cke_path_item').withText('img'));

     // PRIDANIE ODKAZU
     I.say('Pridavam odkaz');
     I.clickCss('.cke_button_icon.cke_button__link_icon');
     I.waitForText('Informácie o odkaze', 10);
     I.switchTo('#wjLinkIframe');
     I.waitForVisible('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable.elfinder-subtree-loaded', 20);

     I.waitForLoader(".WJLoaderDiv");

     I.forceClick(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable.elfinder-subtree-loaded').withText('Webové stránky'), null, { position: { x: 70, y: 15 } });
     I.waitForElement(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('name-autotest-'+randomNumber), 20);
     I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('name-autotest-'+randomNumber));
     I.waitForElement('.elfinder-cwd-file.ui-corner-all.ui-selectee', 10);
     I.waitForElement(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee').withText('name-autotest-'+randomNumber), 20);
     I.wait(0.3);
     I.doubleClick(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee').withText('name-autotest-'+randomNumber));
     I.wait(0.3);
     I.switchTo();
     //I.clickCss("div.cke_editor_data_dialog td.cke_dialog_footer a.cke_dialog_ui_button_ok");
     I.waitForElement(locate('.cke_path_item').withText('a'), 10);
     I.wait(1);

     // ODSTRANIT ODKAZ - IKONA
     I.say('Odstranujem odkaz z obsahu cez ikonu');
     I.clickCss('.cke_button.cke_button__unlink.cke_button_off');
     I.dontSeeElement(locate('.cke_path_item').withText('a'));
     I.pressKey('ArrowRight');
     I.pressKey('Enter');
     I.wait(1);

     //SIVAN - nie je tlacidlo uz odstranene Jeeff?? Prekladovz kluc k tomu "editor.specialChar" nie je nikde pouzity

     // VLOZIT SPECIALNY ZNAK
     // I.say('Pridavam specialny znak do obsahu');
     // I.clickCss('.cke_button.cke_button__specialchar.cke_button_off');
     // I.waitForText('Výber špeciálneho znaku', 20);
     // I.waitForText('¾', 20);
     // I.click(locate('.cke_specialchar').withText('¾'));
     // I.switchTo('.cke_wysiwyg_frame.cke_reset');
     // I.waitForElement(locate('p').withText('¾'), 10);
     // I.switchTo();
     // I.pressKey('Enter');
     // I.wait(1);

     // HLADAT A NAHRADIT
     I.say('Hladam a nahradzam slovo');
     I.clickCss('.cke_button.cke_button__find.cke_button_off');
     I.waitForText('Vyhľadať a nahradiť', 10);
     I.waitForText('Čo hľadať', 10);
     I.clickCss('.cke_dialog_ui_input_text')
     I.type('This');
     I.click('Nahradiť');
     I.waitForText('Čo hľadať', 10);
     I.click(locate('.cke_dialog_ui_input_text').last());
     I.type('Toto');
     I.doubleClick(locate('.cke_dialog_ui_vbox.cke_dialog_page_contents').find('.cke_dialog_ui_button').withText('Nahradiť'));
     I.dontSee('Hľadaný text nebol nájdený');
     I.click(locate('.cke_dialog_footer').find('.cke_dialog_ui_button.cke_dialog_ui_button_cancel').withText('Zatvoriť'));
     I.switchTo('.cke_wysiwyg_frame.cke_reset');
     I.pressKey('ArrowRight');
     I.waitForText('Toto is an autotest', 10);
     I.switchTo();
     I.wait(1);

     // ULOZIT ZMENY
     I.say('Ukladam vykonane zmeny vo web stranke');
     DTE.save();
     I.wait(1);

     // KONTROLA OBSAHU WEBSTRANKY
     I.checkEditedWebPage(randomNumber);
     I.switchTo('.cke_wysiwyg_frame.cke_reset');
     I.waitForText('<!-- Toto is an autotest -->', 10);
     I.waitForElement('table', 5);
     I.waitForText('name-autotest-', 5);
     //I.waitForText('¾', 5);
     I.switchTo();
     DTE.cancel();
});

Scenario('Set config value editorFontAwesomeCssPath to default ', ({ Document }) => {
     const defaultEditorFontAwesomeCssPath = '/templates/aceintegration/jet/assets/fontawesome/css/fontawesome.css\n/templates/aceintegration/jet/assets/fontawesome/css/solid.css';
     Document.setConfigValue('editorFontAwesomeCssPath', defaultEditorFontAwesomeCssPath);
});

Scenario('TODO - bugs', ({ I, DT, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.jstreeNavigate([folder_name]);

     // -----------------TODO BUGS -----------------------
     // Editovanie stranky
     I.editCreatedWebPage(randomNumber);
     I.wait(1);

     // ******** ZALOZKA ZAKLADNE *************
     // Zaradenie v stromovej strukture - Nevidno field pre zmenu nadradeneho priecinka
     I.waitForVisible('#editorAppDTE_Field_editorFields-groupDetails', 5); // TODO

     // ********** ZALOZKA OBSAH **************
     I.clickCss('#pills-dt-datatableInit-content-tab');
     I.waitForVisible('#pills-dt-datatableInit-content', 20);

     // BLOKY - zalozka VASE -> diakritika v dynamicky a staticky blok, v dropdowne .filterOptions su options bez diakritiky OSTATNE a INA STRANKA s DocID
     I.clickCss('#trEditor');
     I.clickCss('.cke_button_icon.cke_button__htmlbox_icon');
     I.wait(3);
     I.waitForElement('.cke_dialog_ui_iframe', 5);
     I.wait(3);
     I.switchTo('.cke_dialog_ui_iframe');
     I.wait(1);
     I.switchTo('#editorComponent');
     I.wait(1);
     I.waitForElement('#componentsWindowTableMainDiv', 5);
     I.waitForText('Dynamický blok', 5);
     I.waitForText('Statický blok', 5);
     I.waitForElement(locate('.filterOptions').withText('Ostatné'), 5);
     I.waitForElement(locate('.filterOptions').withText('Iná stránka s DocID'), 5);
     I.switchTo();
     I.waitForElement(locate('.cke_dialog_ui_button.cke_dialog_ui_button_ok').withText('OK'));
     I.wait(1);
     I.forceClick(locate('.cke_dialog_ui_button.cke_dialog_ui_button_ok').withText('OK'));
     I.waitForElement('#trEditor', 10);

     // HLADAT - diakritika na tlacidle Zatvorit
     I.clickCss('#trEditor');
     I.clickCss('.cke_button.cke_button__find.cke_button_off');
     I.waitForText('Vyhľadať a nahradiť', 10);
     I.waitForElement('.cke_dialog_footer', 10);
     I.waitForElement(locate('.cke_dialog_ui_button.cke_dialog_ui_button_cancel').withText('Zatvoriť'), 10); // TODO
     I.click(locate('.cke_dialog_ui_button.cke_dialog_ui_button_cancel').withText('Zatvoriť')); // TODO
     I.waitForElement('#trEditor', 10);
     DTE.save();

     // Kontrola ulozenia udajov -
     I.checkEditedWebPage(randomNumber); // TODO
     DTE.cancel();
     I.wait(1);
});

Scenario('Zmaz strukturu', ({ I }) => {
     //ak zostalo nieco zaseknute na editacii reloadni aby sa dalo dobre zmazat
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.jstreeNavigate([folder_name]);

     // vymazanie webstranky
     I.deleteCreatedWebPage(randomNumber);

     // vymazanie vytvoreneho priecinka name-autotest
     I.deleteFolderStructure(randomNumber);
});

Scenario('Zadanie neexistujúceho docid', ({I}) => {
     const input = '#tree-doc-id';
     const alert = '#toast-container-webjet';
     const errorMessage = 'Zadaná stránka neexistuje v aktuálnom zobrazení adresárovej štruktúry, alebo k nej nemáte prístupové práva.';
     const url = '/admin/v9/webpages/web-pages-list/?groupid=0'

     I.amOnPage(url);

     I.say('Input a alert element už musia byť vyrenderované');
     I.waitForElement(input);

     I.say('Čakám na load jstree');
     I.wait(1);

     I.say('Zadám do inputu ID neexistujúceho dokumentu.');
     I.fillField(input, '5');
     I.pressKey('Enter');
     I.say('Testuje zobrazenie hlášky. Musí sa zobraziť.');
     I.waitForVisible(alert);
     I.waitForText(errorMessage, 10);
     I.toastrClose();

     I.say('Zmaž hodnotu v inpute');
     I.fillField(input, '');
     I.pressKey('Enter');
     I.say('Testuje zobrazenie hlášky. Nesmie sa zobraziť.');
     I.waitForInvisible(alert, 30);

     I.say('Zadám do inputu ID dalšieho neexistujúceho dokumentu.');
     I.fillField(input, '1234567');
     I.say('Testuje zobrazenie hlášky. Nesmie sa zobraziť.');
     I.waitForInvisible(alert, 30);

     I.say('Stlačím enter');
     I.pressKey('Enter');
     I.say('Testuje zobrazenie hlášky. Musí sa zobraziť.');
     I.waitForVisible(alert);
     I.waitForText(errorMessage, 10);

     I.say('Zadám do url ID neexistujúceho dokumentu.');
     I.amOnPage(url + '?docid=5');

     I.say('Čakám na load jstree');
     I.wait(1);

     I.say('Testuje zobrazenie hlášky. Nesmie sa zobraziť.');
     I.waitForInvisible(alert, 30);
});

Scenario('Zadanie existujúceho docid', ({I}) => {
     const input = '#tree-doc-id';
     //const alert = '.tree-doc-id-alert';
     const modal = '#datatableInit_modal';
     const closeModalBtn = '.btn.btn-outline-secondary.btn-close-editor';
     const url = '/admin/v9/webpages/web-pages-list/?groupid=0'

     I.amOnPage(url);

     I.say('Input a alert element už musia byť vyrenderované');
     I.waitForElement(input);

     I.say('Čakám na load jstree');
     I.wait(1);

     I.say('Zadám do inputu ID existujúceho dokumentu.');
     I.fillField(input, '4');
     I.pressKey('Enter');

     I.say('Počkám kým sa zobrazí modal s dokumentom.');
     I.waitForVisible(modal, 10);
     I.wait(3);

     I.say('Zatvorím modal');
     I.clickCss(closeModalBtn);
     I.waitForInvisible(modal, 10);

     I.say('Zadám do url ID existujúceho dokumentu.');
     I.amOnPage(url + '&docid=50');

     I.say('Čakám na load jstree');
     I.wait(1);

     I.say('Počkám kým sa zobrazí modal s dokumentom.');
     I.waitForVisible(modal, 10);
     I.wait(3);

     I.say('Zatvorím modal');
     I.clickCss(closeModalBtn);
     I.waitForInvisible(modal, 10);
});

Scenario('Notify', ({ I, DT, DTE }) => {
     //
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     DT.waitForLoader();

     I.jstreeNavigate(["English", "News"]);

     //Vyfiltruj si zaznam
     DT.filterContains("title", "Test");

     //Edituj stranku
     I.click("Test", "#datatableInit_wrapper td.dt-row-edit");
     DTE.waitForEditor();

     //over zobrazenie info o historickych zaznamoch
     I.see("Existuje rozpracovaná alebo neschválená verzia tejto stránky");
     I.clickCss(".toast-close-button");
     I.dontSee("Existuje rozpracovaná alebo neschválená verzia tejto stránky");

     //Prejsi na tab zakladane
     I.clickCss("#pills-dt-datatableInit-basic-tab");

     //Zmen honotu navbaru
     I.fillField('#DTE_Field_navbar', "Test-edit");

     // Ulozenie
     I.click(locate('#datatableInit_modal').find('button.btn.btn-primary'));
     DTE.waitForLoader();

     I.see("Stránka podlieha schvaľovaniu");

     I.clickCss(".toast-close-button");

     I.dontSee("Stránka podlieha schvaľovaniu");
});

Scenario('Notify edit button', ({ I, DT, DTE }) => {
     //overi zobrazenie a kliknutie na button Editovať poslednú verziu pri notifikacii o rozpracovanej verzii

     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     DT.waitForLoader();

     I.jstreeNavigate(["English", "News"]);

     //Vyfiltruj si zaznam
     DT.filterContains("title", "Test");

     //Edituj stranku
     I.click("Test");
     DTE.waitForEditor();

     I.see("Existuje rozpracovaná alebo neschválená verzia tejto stránky");

     //Prejsi na tab zakladane
     I.clickCss("#pills-dt-datatableInit-basic-tab");
     //Over honotu navbaru
     I.dontSeeInField('#DTE_Field_navbar', "Test-edit");
     I.seeInField('#DTE_Field_navbar', "Test");

     I.click("Editovať poslednú verziu");

     I.wait(1);
     DTE.waitForEditor();

     I.dontSee("Existuje rozpracovaná alebo neschválená verzia tejto stránky");

     //Prejsi na tab zakladane
     I.clickCss("#pills-dt-datatableInit-basic-tab");

     //Over honotu navbaru
     I.seeInField('#DTE_Field_navbar', "Test-edit");
});

Scenario('overit notifikacie pri ulozeni pracovnej verzie stranky', ({ I, DTE }) => {
     //uloz pracovnu verziu
     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22916");
     DTE.waitForEditor();
     I.wait(5);
     I.clickCss("#pills-dt-datatableInit-basic-tab");
     DTE.fillField("virtualPath", "/test-stavov/test-pracovnej-verzie-stranky.html");
     I.clickCss("#webpagesSaveCheckbox");
     DTE.save();

     I.see("Stránka bola uložená ako pracovná verzia, pre návštevníkov web sídla sa nebude zobrazovať.");
     I.clickCss(".toast-close-button");

     //over zobrazenie info o rozpracovanej verzii
     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22916");
     DTE.waitForEditor();

     I.see("Existuje rozpracovaná alebo neschválená verzia tejto stránky");
     I.clickCss(".toast-close-button");
     I.dontSee("Existuje rozpracovaná alebo neschválená verzia tejto stránky");
});

Scenario('overit zobrazenie notifikacie, ak vytvorim stranku s URL, ktora uz existuje A zmena linky v stranke', ({ I, DTE, Browser }) => {
     //nastav stranke 22916 URL adresu /test-stavov/test-pracovnej-verzie-stranky.html
     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22916");
     DTE.waitForEditor();
     I.wait(5);
     I.clickCss("#pills-dt-datatableInit-basic-tab");
     DTE.fillField("virtualPath", "/test-stavov/test-pracovnej-verzie-stranky.html");
     DTE.save();

     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22954");
     DTE.waitForEditor();
     I.wait(5);

     I.say("Nastavujem text");
     I.clickCss('#trEditor');
     I.pressKey(["CommandOrControl", "A"]);
     I.pressKey("Delete");
     I.wait(1);
     I.type("Test existujucej URL adresy");

     I.say('overit zobrazenie notifikacie, ked nastane zmena URL a premenuju sa niektore linky');
     I.clickCss('.cke_button_icon.cke_button__link_icon');
     I.waitForText('Informácie o odkaze', 10);
     I.switchTo('#wjLinkIframe');
     I.waitForLoader(".WJLoaderDiv");
     if (Browser.isFirefox()) I.wait(3);
     I.fillField("#txtUrl", "/test-stavov/test-existujucej-url-adresy.html");
     I.switchTo();
     I.click("OK");

     I.clickCss("#pills-dt-datatableInit-basic-tab");
     //najskor nastav prazdnu hodnotu, aby sa zresetovalo pocitadlo na konci URL a neskoncilo nam to po X testoch
     DTE.fillField("virtualPath", "/test-stavov/test-existujucej-url-adresy.html");

     DTE.save();
     DTE.waitForLoader();


     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22954");
     DTE.waitForEditor();
     I.wait(5);

     I.clickCss("#pills-dt-datatableInit-basic-tab");
     //nastav existujuce URL zo stranky 22916
     DTE.fillField("virtualPath", "/test-stavov/test-existujucej-url-adresy-5.html");

     DTE.save();

     I.waitForElement(".toast-close-button", 20);
     I.see("Nastala zmena v názve stránky, linky boli aktualizované v nasledovných stránkach");
     I.see("22954-Test existujucej URL");

     //nastav URL existujucej stranky
     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22954");
     DTE.waitForEditor();
     I.wait(5);

     I.clickCss("#pills-dt-datatableInit-basic-tab");
     //nastav existujuce URL zo stranky 22916
     DTE.fillField("virtualPath", "/test-stavov/test-pracovnej-verzie-stranky.html");

     DTE.save();

     I.waitForElement(".toast-close-button", 20);
     I.see("Nastala zmena v názve stránky, linky boli aktualizované v nasledovných stránkach");
     I.see("22954-Test existujucej URL");

     I.see("Zadaná URL adresa je už použitá na stránke: 22916");
     I.clickCss(".toast-close-button");

     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22954");
     DTE.waitForEditor();

     I.clickCss("#pills-dt-datatableInit-basic-tab");
     //over pridanie -1 na koniec URL
     I.seeInField("#DTE_Field_virtualPath", "/test-stavov/test-pracovnej-verzie-stranky-2.html");

});

Scenario('Zmena linky btn', async ({ I, DT, DTE }) => {
     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");
     DT.waitForLoader();

     DTE.waitForEditor();
     I.wait(4);

     I.switchTo(".cke_wysiwyg_frame.cke_reset");
     I.click("Sollicitudin");

     I.switchTo();
     I.waitForVisible("div.cke_dialog_body");
     I.see("Adresa stránky kliknutia", "table.cke_dialog");
     I.seeInField(locate("input.cke_dialog_ui_input_text").inside(locate("td.cke_dialog_ui_hbox_first").withText("Adresa stránky kliknutia")), "#")

     I.forceClick({css: "div.cke_dialog_body i.wj-action-icon"});

     I.waitForVisible("#modalIframeIframeElement");
     I.wait(5);
     I.switchTo("#modalIframeIframeElement");
     I.waitForElement("#nav-iwcm_1_");

     const numbOfEl = await I.grabNumberOfVisibleElements("#nav-iwcm_1_L2ltYWdlcw_E_E");

     if(numbOfEl < 1) {
          I.click( locate("#nav-iwcm_1_"), null, { position: { x: 20, y: 5 } });
     }

     I.waitForElement("#nav-iwcm_1_L2ltYWdlcw_E_E");

     I.click( locate("#nav-iwcm_1_L2ltYWdlcw_E_E"), null, { position: { x: 20, y: 5 } });
     I.waitForElement("#nav-iwcm_1_L2ltYWdlcy9iYW5uZXJ5"); ///images/bannery
     I.click( locate("#nav-iwcm_1_L2ltYWdlcy9iYW5uZXJ5"), null, { position: { x: 20, y: 5 } });
     I.waitForElement("#iwcm_1_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pbnZlc3RpY2llLmpwZw_E_E"); ///images/bannery/banner-investicie.jpg
     I.click( locate("#iwcm_1_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pbnZlc3RpY2llLmpwZw_E_E"), null, { position: { x: 20, y: 5 } });

     I.switchTo();
     I.clickCss("#modalIframe div.modal-footer button.btn-primary");

     I.seeInField(locate("input.cke_dialog_ui_input_text").inside(locate("td.cke_dialog_ui_hbox_first").withText("Adresa stránky kliknutia")), "/images/bannery/banner-investicie.jpg")

});

Scenario('Nastavenie editorAutoFillPublishStart @singlethread', ({ I, DT, DTE, Document }) => {
     var date = I.formatDate((new Date()).getTime());

     //
     I.say("Vypinam nastavenie editorAutoFillPublishStart");
     Document.setConfigValue("editorAutoFillPublishStart", "false");

     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=40273");
     DT.waitForLoader();
     DTE.waitForEditor();
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     I.dontSeeInField("#DTE_Field_publishStartDate", date);

     DTE.cancel();

     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     I.waitForElement("#pills-dt-datatableInit-perex-tab", 10);
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     I.dontSeeInField("#DTE_Field_publishStartDate", date);

     //
     I.say("Zapinam nastavenie editorAutoFillPublishStart");
     Document.setConfigValue("editorAutoFillPublishStart", "true");

     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=40273");
     DT.waitForLoader();
     DTE.waitForEditor();
     I.waitForElement("#pills-dt-datatableInit-perex-tab", 10);
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     I.seeInField("#DTE_Field_publishStartDate", date);

     DTE.cancel();

     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     I.waitForElement("#pills-dt-datatableInit-perex-tab", 10);
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     I.seeInField("#DTE_Field_publishStartDate", date);
});

async function testLink(link, fixedLink, I, DT, DTE) {

     if (fixedLink==null) fixedLink = link;

     await DTE.fillCkeditor("<p>Test</p>");

     //
     I.say('Adding link to the text');
     I.clickCss('#trEditor');
     I.pressKey(["CommandOrControl", "A"]);

     I.clickCss('.cke_button_icon.cke_button__link_icon');
     I.waitForText('Informácie o odkaze', 10);
     I.wait(1);
     I.switchTo();
     I.switchTo('#wjLinkIframe');

     I.waitForLoader(".WJLoaderDiv");
     I.waitForElement("#txtUrl", 10);
     I.fillField("#txtUrl", link);
     I.switchTo();
     I.click(locate('.cke_dialog_ui_button').withText('OK'));

     var htmlCode = await I.executeScript(function () {
          return window.ckEditorInstance.getData();
     });

     I.say("Html code="+htmlCode);

     I.assertContain(htmlCode, fixedLink);
     I.assertNotContain(htmlCode, "http://"+fixedLink);

     //
     I.say("Reopen window and check the link");
     I.clickCss('.cke_button_icon.cke_button__link_icon');
     I.waitForText('Informácie o odkaze', 10);

     I.switchTo('#wjLinkIframe');

     I.waitForLoader(".WJLoaderDiv");
     I.switchTo();
     I.switchTo('#wjLinkIframe');
     I.waitForElement("#txtUrl", 10);
     I.seeInField("#txtUrl", fixedLink);
     I.dontSeeInField("#txtUrl", "http://"+fixedLink);

     //if URL startsWith / wait for ckeditor select by hash
     if (fixedLink.indexOf("/")===0) {
          I.wait(5);
          I.seeInField("#txtUrl", fixedLink);
          I.dontSeeInField("#txtUrl", "http://"+fixedLink);

          if (fixedLink.indexOf("/?email=")===0) {
               //click on another page and verify URL contains email parameter
               //I.forceClick("Banner neprihlásený", "div.elfinder-cwd-filename");
               I.clickCss("#iwcm_doc_group_volume_L2RvYzo1Mw_E_E");
               I.waitForElement("#iwcm_doc_group_volume_L2RvYzo1Mw_E_E.ui-selected", 10);
               I.wait(1);
               I.seeInField("#txtUrl", "/banner-neprihlaseny.html?email=!RECIPIENT_EMAIL!");
          }
     }

     I.switchTo();
     I.click(locate('.cke_dialog_ui_button').withText('Zrušiť'));

     //
     I.say("Verify link from HTML code");
     await DTE.fillCkeditor('<p><a href="'+fixedLink+'">Test</a></p>');

     I.waitForElement(locate('.cke_path_item').withText('a'), 10);
     I.click(locate('.cke_path_item').withText('a'));

     I.clickCss('.cke_button_icon.cke_button__link_icon');
     I.waitForText('Informácie o odkaze', 10);

     I.switchTo('#wjLinkIframe');

     I.waitForLoader(".WJLoaderDiv");
     I.switchTo();
     I.switchTo('#wjLinkIframe');
     I.waitForElement("#txtUrl", 10);
     I.seeInField("#txtUrl", fixedLink);
     I.dontSeeInField("#txtUrl", "http://"+fixedLink);
     I.switchTo();
     I.amAcceptingPopups();
     I.click(locate('.cke_dialog_ui_button').withText('Zrušiť'));

     return htmlCode;
}

Scenario('Various link types', async ({ I, DT, DTE }) => {
     I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
     DT.waitForLoader();
     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
     I.clickCss("#pills-dt-datatableInit-content-tab");

     I.say("Testing phone links");
     await testLink("tel:0903123666", null, I, DT, DTE);
     await testLink("tel:+421903123666", null, I, DT, DTE);
     await testLink("tel:+421-903-123-666", null, I, DT, DTE);
     await testLink("tel:09/53788888", null, I, DT, DTE);
     await testLink("tel:09-5378-8888", null, I, DT, DTE);

     //
     I.say("test link with URL parameter");
     await testLink("/?email=!RECIPIENT_EMAIL!", null, I, DT, DTE);

     //
     I.say("Testing links with : in URL parameter");
     await testLink("https://eur-lex.europa.eu/legal-content/CS/TXT/HTML/?uri=OJ:L_202401991", null, I, DT, DTE);
     await testLink("eur-lex.europa.eu/legal-content/CS/TXT/HTML/?uri=OJ:L_202401991", "http://eur-lex.europa.eu/legal-content/CS/TXT/HTML/?uri=OJ:L_202401991", I, DT, DTE);

     //
     I.say("Testing web links");
     await testLink("www.interway.sk", "http://www.interway.sk", I, DT, DTE);
     await testLink("http://docs.webjetcms.sk", null, I, DT, DTE);
     await testLink("https://docs.webjetcms.sk", null, I, DT, DTE);
     await testLink("docs.webjetcms.sk", "http://docs.webjetcms.sk", I, DT, DTE);

     //
     I.say("Testing local links");
     await testLink("/apps/formular/", null, I, DT, DTE);
     await testLink("/apps/formular/potvdenie-double-optin.html", null, I, DT, DTE);
     await testLink("/files/jurko.jpg", null, I, DT, DTE);
     await testLink("/files/do-not-exist.pdf", null, I, DT, DTE);

     //
     I.say("Testing email links");
     await testLink("info@webjetcms.sk", "mailto:info@webjetcms.sk", I, DT, DTE);
     await testLink("mailto:info@webjetcms.sk", null, I, DT, DTE);

     //
     I.say("Testing tiktok links");
     await testLink("https://www.tiktok.com/@webjetcms", null, I, DT, DTE);
     await testLink("www.tiktok.com/@webjetcms", "http://www.tiktok.com/@webjetcms", I, DT, DTE);
});

Scenario('Test link elfinder - ALT field', async ({ I, DT, DTE }) => {
     I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
     DT.waitForLoader();
     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
     I.click("#pills-dt-datatableInit-content-tab");

     //
     I.say("Testing link chosen by elfinder and check metadata");
     await testLinkElfinder(DTE, I);
});


async function testLinkElfinder(DTE, I) {
     await DTE.fillCkeditor("<p>Test</p>");

     I.say('Adding link to the text');
     I.clickCss('#trEditor');
     I.pressKey(["CommandOrControl", "A"]);

     I.clickCss('.cke_button_icon.cke_button__link_icon');
     I.waitForText('Informácie o odkaze', 10);
     I.switchTo();
     I.waitForElement('#wjLinkIframe');
     I.switchTo('#wjLinkIframe');

     I.waitForLoader(".WJLoaderDiv");
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), null, { position: { x: 20, y: 5 } });
     I.waitForText('Súbory', 10, ".elfinder-cwd-file");
     I.doubleClick(".elfinder-cwd-filename[title='Súbory']");
     I.click(".elfinder-cwd-filename[title='jurko.jpg']");
     await I.waitForElement(".elfinder-stat-selected[title^='jurko.jpg']", 10);
     I.switchTo();
     I.clickCss(".cke_dialog_tab[title='Rozšírené']");
     I.waitForValue('#cke_270_textInput', 'Súbor JPG, veľkosť 465,15 kB', 10);

     I.click(locate('.cke_dialog_ui_button').withText('OK'));
}