Feature('webpages.webpage-history');

var folder_name, subfolder_one, subfolder_two, auto_webPage, randomNumber, now,
     edit_history_webpage = (locate('#datatableFieldDTE_Field_editorFields-history_wrapper').find('.btn.btn-sm.btn-warning.buttons-history-edit')),
     view_history_webpage = (locate('#datatableFieldDTE_Field_editorFields-history_wrapper').find('.btn.btn-sm.btn-outline-secondary.buttons-history-preview')),
     compare_history_webpage = (locate('#datatableFieldDTE_Field_editorFields-history_wrapper').find('.btn.btn-sm.btn-outline-secondary.buttons-divider.buttons-history-compare'));

Before(({ I, login }) => {
     login('admin');
     if (typeof folder_name == "undefined") {
          randomNumber = I.getRandomText();
          I.say("randomNumber=" + randomNumber);
          folder_name = 'name-autotest-' + randomNumber;
          subfolder_one = 'subone-autotest-' + randomNumber;
          subfolder_two = 'subtwo-autotest-' + randomNumber;
          auto_webPage = 'webPage-autotest-' + randomNumber;
          now = new Date();
     }
});

// Nahlad - stara verzia a nova verzia
function insight(I, version) {
     I.forceClick(locate('#datatableFieldDTE_Field_editorFields-history>tbody>tr.even').find('.dt-select-td.cell-not-editable'));

     if (version === 'old') {
          I.waitForText('1 riadok označený', 10, '.select-item');
     } else if (version === 'new') {
          I.forceClick(locate('#datatableFieldDTE_Field_editorFields-history>tbody>tr.odd').find('.dt-select-td.cell-not-editable'));
     }

     I.click(view_history_webpage);
     I.switchToNextTab();
     I.waitForText('Demo stránka', 10, 'p');
     I.seeElement(locate('h1').withText(auto_webPage));
     I.seeElement(locate('p').withText('<!-- This is an autotest -->'));
     if (version === 'old') {
          I.dontSeeElement(locate('p').withText('<h1>Testujem historiu webstranky</h1>'));
     } else if (version === 'new') {
          I.seeElement(locate('p').withText('<h1>Testujem historiu webstranky</h1>'));
     }

     I.closeCurrentTab();
}

// Verzie - novsia na lavej strane a starsia na pravej strane
function versionSides(I, side) {

     if (side === 'left') {
          // novsia verzia
          I.switchTo('#left');
     } else if (side === 'right') {
          // starsia verzia
          I.switchTo('#right');
     }

     I.waitForText('Demo stránka', 10, 'p');
     I.seeElement(locate('h1').withText(auto_webPage));
     I.seeElement(locate('p').withText('<!-- This is an autotest -->'));
     if (side === 'left') {
          I.seeElement(locate('p').withText('<h1>Testujem historiu webstranky</h1>'));
     } else if (side === 'right') {
          I.dontSeeElement(locate('p').withText('<h1>Testujem historiu webstranky</h1>'));
     }

     I.switchTo();
}

// aktualne publikovana stranka a verzia z historie
function versions(I, variant) {
     if (variant === 'actual') {
          I.switchTo("frame[name='leftTop']");
          I.waitForText('Aktuálne publikovaná verzia', 10);
     } else if (variant === 'historical') {
          I.switchTo("frame[name='rightTop']");
          I.waitForElement(locate('.first.openFirst').withText('Verzia z histórie'));
     }

     I.waitForElement(locate('#tabMenu1').withText('Tester Playwright'), 10);
}

// Kontrola tabu Obsah
function contentTab(I) {
     I.say('Skontroluj zalozku Obsah');
     I.waitForElement('.cke_wysiwyg_frame.cke_reset', 15);
     I.waitForElement('#toast-container-webjet', 15); // poznamka redaktora tu ostala z najnovsej verzie (neuklada sa historicky)
     I.switchTo('.cke_wysiwyg_frame.cke_reset');
     I.waitForElement(locate('#WebJETEditorBody').withDescendant('p').withText('<!-- This is an autotest -->'), 10);
     I.wait(0.5);
     I.dontSeeElement(locate('#WebJETEditorBody').withDescendant('p').withText('<h1>Testujem historiu webstranky</h1>'));
     I.switchTo();
}

// Kontrola tabu Zakladne
function basicTab(I) {
     I.say('Skontroluj zalozku Zakladne');
     I.clickCss('#pills-dt-datatableInit-basic-tab');
     I.dontSeeCheckboxIsChecked('#DTE_Field_available_0');
     I.dontSeeCheckboxIsChecked('#DTE_Field_searchable_0');
     I.seeInField('#DTE_Field_editorFields-redactorNote', 'webpages history autotest'); // poznamka redaktora tu ostala z najnovsej verzie (neuklada sa historicky)
}

Scenario('Priprav strukturu', ({ I }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');

     // vytvorenie hlavneho priecinka a jeho dvoch podpriecinkov
     I.createFolderStructure(randomNumber, false);

     // vytvorenie webstranky
     I.createNewWebPage(randomNumber);
});

Scenario('Historia webstranok', ({ I, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.jstreeNavigate([folder_name]);

     // ---------------------- 1. EDITOVANIE WEBSTRANKY ----------------------------------------------------------------------------------------
     I.editCreatedWebPage(randomNumber);

     // Aktivovat - Zobrazenie web stranky a vyhladanie cez vyhladavace a pridanie poznamky redaktora
     I.say('1.Aktivovat - Zobrazenie web stranky a vyhladanie cez vyhladavace a pridanie poznamky redaktora');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_available_0').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_searchable_0').find('.form-check-label'));
     I.fillField('#DTE_Field_editorFields-redactorNote', 'webpages history autotest');

     // Pridanie textu do obsahu a ulozenie udajov
     I.say('Pridanie textu do obsahu a ulozenie udajov');
     I.clickCss('#pills-dt-datatableInit-content-tab');
     I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
     I.clickCss('#trEditor');
     I.pressKey('Enter');
     I.type('<h1>Testujem historiu webstranky</h1>');
     DTE.save();

     // ----------------------- 2. KONTROLA ULOZENIA NOVYCH UDAJOV ------------------------------------------------------------------------------------
     I.say('2. Kontrola ulozenia novych udajov');
     // Kontrola ulozenia udajov v zalozke Zakladne
     I.say('Zalozka Zakladne');
     I.checkEditedWebPage(randomNumber);
     I.clickCss('#pills-dt-datatableInit-basic-tab');
     I.seeCheckboxIsChecked('#DTE_Field_available_0');
     I.seeCheckboxIsChecked('#DTE_Field_searchable_0');
     I.seeInField('#DTE_Field_editorFields-redactorNote', 'webpages history autotest');

     // Kontrola v zalozke Obsah
     I.say('Zalozka Obsah');
     I.clickCss('#pills-dt-datatableInit-content-tab');
     I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
     I.switchTo('.cke_wysiwyg_frame.cke_reset');
     I.seeElement(locate('#WebJETEditorBody').withDescendant('p').withText('<h1>Testujem historiu webstranky</h1>'));
     I.switchTo();

     // ----------------------------------- 3. KONTROLA HISTORIE --------------------------------------------------------------------------------------
     // 1.KONTROLA HISTORIE NA ZALOZKE HISTORIA
     I.say('3.Kontrola historie na zalozke Historia');
     I.toastrClose();
     I.click("#pills-dt-datatableInit-history-tab", null, { position: { x: 0, y: 0 } }); //because after toastr close cursor stays in close button tooltip
     I.waitForVisible('#datatableFieldDTE_Field_editorFields-history_wrapper');

     // V tabulke vidim 2 zaznamy s dnesnym datumom
     I.say('V tabulke historia vidim 2 zaznamy s dnesnym datumom');

     I.waitForElement(locate('#datatableFieldDTE_Field_editorFields-history>tbody>tr').withText(I.formatDate(now.getTime())), 20);

     I.seeNumberOfVisibleElements(locate('#datatableFieldDTE_Field_editorFields-history>tbody>tr').withText(I.formatDate(now.getTime())), 2);

     // 2.ZOBRAZ NAHLAD STARSEJ A NOVSEJ WEBSTRANKY
     I.say('Zobrazujem nahlad starsej stranky');
     insight(I, 'old');

     I.say('Zobrazujem nahlad novsej stranky');
     insight(I, 'new');

     // 3.POROVNANIE NOVSEJ A STARSEJ STRANKY
     I.say('Porovnanie novsej a starsej stranky');
     I.forceClick(locate('#datatableFieldDTE_Field_editorFields-history>tbody>tr.odd').find('.dt-select-td.cell-not-editable'));
     I.wait(0.5);
     I.forceClick(locate('#datatableFieldDTE_Field_editorFields-history>tbody>tr.even').find('.dt-select-td.cell-not-editable'));
     I.click(compare_history_webpage);
     I.switchToNextTab();

     // Novsia stranka na lavej strane
     I.say('Novsia stranka na lavej strane');
     versionSides(I, 'left');

     // Starsia stranka na pravej strane
     I.say('Starsia stranka na pravej strane');
     versionSides(I, 'right');

     // Aktualne publikovana verzia
     I.say('Aktualne publikovana verzia');
     versions(I, 'actual');

     I.seeElement(locate('#tabMenu1').withText(I.formatDate(now.getTime())));

     I.switchTo();

     // Verzia z historie
     I.say('Verzia z historie');
     versions(I, 'historical');

     I.seeElement(locate('#tabMenu1').withText(I.formatDate(now.getTime())));

     I.switchTo();

     // Zvyraznit rozdiely
     I.say('Zvyrazni rozdiely');
     I.switchTo("frame[name='leftTop']");
     I.checkOption('#showChangesCb');
     I.switchTo();
     I.switchTo('#left');
     I.seeElement(locate('p').withText('<!-- This is an autotest -->'));
     I.waitForElement(locate('#added-diff-0').withText('<h1>Testujem historiu webstranky</h1>'));
     I.switchTo();
     I.switchTo('#right');
     I.seeElement(locate('p').withText('<!-- This is an autotest -->'));
     I.dontSeeElement(locate('p').withText('<h1>Testujem historiu webstranky</h1>'));
     I.dontSeeElement(locate('#added-diff-0').withText('<h1>Testujem historiu webstranky</h1>'));
     I.switchTo();
     I.closeCurrentTab();

     // 4.ZOBRAZ STARY ZAZNAM Z HISTORIE CEZ EDITACIU A ULOZ HO AKO AKTUALNY
     I.say('Zobraz stary zaznam z historie cez editaciu a uloz ho ako aktualny');
     I.waitForText('1 riadok označený', 10, '.select-item');
     I.click(edit_history_webpage);

     // Skontroluj zalozku Obsah
     contentTab(I);

     // Skontroluj zalozku Zakladne
     basicTab(I);
     DTE.save();

     // 5.KONTROLA CI SA ULOZILA NAJSTARSIA VERZIA STRANKY AKO AKTUALNA
     I.say('Kontrola ci sa ulozila najstarsia verzia stranky ako aktualna');
     I.checkEditedWebPage(randomNumber);

     // Skontroluj zalozku Obsah
     contentTab(I);

     // Skontroluj zalozku Zakladne
     basicTab(I);
});

Scenario('Zmaz strukturu', ({ I }) => {
     //ak zostalo nieco zaseknute na editacii reloadni aby sa dalo dobre zmazat
     I.amOnPage('/admin/v9/webpages/web-pages-list/');
     I.jstreeNavigate([folder_name]);

     // vymazanie vytvoreneho priecinka name-autotest
     I.deleteFolderStructure(randomNumber);
});

Scenario('history from multigroup mapping', ({ I, DT, DTE }) => {
     //open slave page and load history version
     I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=143');
     DTE.waitForEditor();
     I.switchTo(".cke_wysiwyg_frame");
     I.see("Toto je testovacia stranka... 01");
     I.see("Druhy odstavec");

     I.switchTo();
     I.clickCss("#pills-dt-datatableInit-history-tab");
     DT.waitForLoader("#datatableFieldDTE_Field_editorFields-history_processing");
     I.fillField("#datatableFieldDTE_Field_editorFields-history_wrapper input.filter-input-id", "687");
     I.clickCss("#datatableFieldDTE_Field_editorFields-history_wrapper button.dt-filtrujem-id");
     DT.waitForLoader("#datatableFieldDTE_Field_editorFields-history_processing");

     I.clickCss("#datatableFieldDTE_Field_editorFields-history_wrapper button.buttons-select-all");
     I.clickCss("#datatableFieldDTE_Field_editorFields-history_wrapper button.buttons-history-edit");

     I.waitForElement("#pills-dt-datatableInit-content-tab.nav-link.active", 20);
     I.switchTo(".cke_wysiwyg_frame");
     I.see("Toto je testovacia stranka");
     I.dontSee("Druhy odstavec");
});

Scenario('open webpage history from URL', ({ I, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=143&historyid=687');
     DTE.waitForEditor();

     I.switchTo(".cke_wysiwyg_frame");
     I.see("Toto je testovacia stranka");
     I.dontSee("Druhy odstavec");

     I.switchTo();
     DTE.cancel();
     I.dontSeeInCurrentUrl("history=687");
});