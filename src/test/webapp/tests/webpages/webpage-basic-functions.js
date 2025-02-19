Feature('webpages.webpage-basic-functions');

var folder_name, subfolder_one, subfolder_two, auto_webPage, randomNumber,
     note = 'AUTOTEST';

Before(({ I, login }) => {
     login('admin');
     if (typeof folder_name == "undefined") {
          randomNumber = I.getRandomText();
          I.say("randomNumber=" + randomNumber);
          folder_name = 'name-autotest-' + randomNumber;
          subfolder_one = 'subone-autotest-' + randomNumber;
          subfolder_two = 'subtwo-autotest-' + randomNumber;
          auto_webPage = 'webPage-autotest-' + randomNumber;
     }
});

function createNewWebPage(I, randomNumber, DTE, DT) {
     // premenne
     var auto_webPage = 'webPage-autotest-' + randomNumber;
     // vytvorenie webstranky
     I.say('Pridanie novej web stranky ' + auto_webPage);
     I.waitForElement(DT.btn.add_button, 10);
     I.click(DT.btn.add_button);
     I.dtWaitForEditor();
     I.click('#pills-dt-datatableInit-basic-tab');
     I.waitForElement('#DTE_Field_title');
     I.click('#DTE_Field_title');
     I.clearField('#DTE_Field_title');
     I.fillField('#DTE_Field_title', auto_webPage);
     I.click('#DTE_Field_navbar');
     I.clearField('#DTE_Field_navbar');
     I.fillField('#DTE_Field_navbar', auto_webPage);
     // Pridanie textu do obsahu
     I.click('#pills-dt-datatableInit-content-tab');
     I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
     I.click('#trEditor');
     I.type('<!-- This is an autotest -->');
     // Ulozenie
     DTE.save();
}

function checkEditedWebPage(I, randomNumber) {
     // premenne
     var auto_webPage = 'webPage-autotest-' + randomNumber;
     var edit_webpage = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning'));
     var auto_name = 'name-autotest-' + randomNumber;
     // Kontrola editovanej stranky
     I.say('Kontrola editovanej web stranky ' + auto_webPage);
     I.amOnPage('/admin/v9/webpages/web-pages-list/');
     I.waitForVisible('#SomStromcek', 20);
     I.jstreeClick(auto_name);
     I.waitForElement(locate('.even.is-not-public').withText(auto_webPage), 10);
     I.forceClick(locate('.even.is-not-public').withText(auto_webPage).find('.dt-select-td.sorting_1'));
     I.waitForText('1 riadok označený', 10);
     I.click(edit_webpage);
     I.dtWaitForEditor();
     I.waitForText('Obsah', 10);
}

Scenario('Priprav strukturu', ({ I, DTE, DT }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/');

     // vytvorenie hlavneho priecinka a jeho dvoch podpriecinkov
     I.createFolderStructure(randomNumber, false);

     // vytvorenie webstranky
     createNewWebPage(I, randomNumber, DTE, DT);
});

Scenario('Zakladne funkcie webstranky - zalozka Zakladne', ({ I, DT, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/');
     I.jstreeNavigate([folder_name]);

     // Kontrola ci stranka je verejna a je ju mozno vyhladavat cez vyhladavace
     I.waitForElement(locate('.even').withText(auto_webPage), 15);
     I.dontSeeElement(locate('.even.is-not-public').withText(auto_webPage));
     I.dontSeeElement(locate('.even.is-not-public').find('.fas.fa-eye-slash'));

     // editovanie webstranky
     I.editCreatedWebPage(randomNumber);

     // ------------------------ ZALOZKA ZAKLADNE ------------------
     // Presmerovanie
     I.say('Presmerovanie web stranky');
     I.click(locate('.DTE_Field.form-group.row.DTE_Field_Type_elfinder.DTE_Field_Name_externalLink').find('.btn.btn-outline-secondary'));
     I.waitForText('URL adresa, na ktorú chcete web stránku presmerovať', 10);
     I.waitForVisible('#modalIframeIframeElement', 15);
     I.waitForLoader(".WJLoaderDiv");
     I.switchTo('#modalIframeIframeElement');
     I.waitForElement(locate('#nav-iwcm_doc_group_volume_').withText('Webové stránky'), 15);
     I.wait(1);
     I.waitForElement(locate(".ui-state-active.elfinder-subtree-loaded").withText("Obrázky"), 15);
     I.wait(1);
     I.click(locate('#nav-iwcm_doc_group_volume_'));
     I.wait(1);
     I.click(locate('.elfinder-navbar-wrapper').withText(folder_name).last());
     I.wait(1);
     I.waitForVisible('.elfinder-cwd-file.ui-corner-all.ui-selectee', 10);
     I.click(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee').withText(folder_name).last());
     I.wait(1);
     I.waitForElement('.elfinder-cwd-file.ui-corner-all.ui-selectee.ui-state-hover.ui-selected', 10);
     I.switchTo();
     I.click(locate('.btn.btn-primary').withText('Potvrdiť'));
     I.dtWaitForLoader();
     I.seeInField('[name="externalLink"]', '/' + folder_name + '/');

     // Pridat kopiu web stranky
     I.say('Pridanie kopie web stranky');
     I.click(locate('.btn.btn-outline-secondary.btn-vue-jstree-add').withText('Pridať kópiu web stránky'));
     I.waitForVisible('#jsTree', 10);
     I.click(locate('.jstree-anchor').withText(folder_name).inside('#jsTree'));
     I.dtWaitForLoader();
     I.waitForValue('.dt-tree-container>.form-group>div>div.input-group>input.form-control', '/' + folder_name, 5);

     // Deaktivovat - nepovolene zobrazenie web stranky a vyhladanie cez vyhladavace
     I.say('Aktivovanie zobrazenia web stranky a vyhladavanie cez vyhladavace');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_available_0').find('.form-check-label'));
     I.wait(0.5);
     I.dontSeeCheckboxIsChecked('#DTE_Field_available_0');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_searchable_0').find('.form-check-label'));
     I.wait(0.5);
     I.dontSeeCheckboxIsChecked('#DTE_Field_searchable_0');

     // Poznamka redaktora
     I.say('Pridanie poznamky redaktora');
     I.fillField('#DTE_Field_editorFields-redactorNote', note);
     I.wait(0.5);
     I.seeInField('#DTE_Field_editorFields-redactorNote', note);
     DTE.save();
     I.waitForText('Záznamy 1 až 3 z 3');

     // ------------------------------------ KONTROLA ULOZENIA UDAJOV ---------------------------------------------------------------
     checkEditedWebPage(I, randomNumber);
     I.waitForElement('.toast-message', note, 15);
     I.click('#pills-dt-datatableInit-basic-tab');
     I.waitForValue('.dt-tree-container>.form-group>div>div.input-group>input.form-control', '/' + folder_name, 5);
     I.seeInField('#DTE_Field_virtualPath', '/' + folder_name, 5);
     I.seeInField('#DTE_Field_editorFields-redactorNote', note);
     I.dtEditorCancel();
     I.click(DT.btn.tree_refresh_button);
     DT.waitForLoader();

     // Kontrola web stranky - nie je verejna, bola pridana kopia webstranky
     I.say('Kontrola ci stranka je verejna a bola vytvorena kopia');
     I.waitForElement(locate('.even.is-not-public').withText(auto_webPage), 15);
     I.seeNumberOfVisibleElements(locate('tr.is-not-public').withText(auto_webPage), 2);
     I.waitForText('Záznamy 1 až 3 z 3', 5);

     // vymazanie jednej webstranky
     I.say('Vymazanie jednej webstranky');
     I.deleteCreatedWebPage(randomNumber, false);
     I.seeNumberOfVisibleElements(locate('.even.is-not-public').withText(auto_webPage), 1);
});

Scenario('Zmaz strukturu', ({ I }) => {
     //ak zostalo nieco zaseknute na editacii reloadni aby sa dalo dobre zmazat
     I.amOnPage('/admin/v9/webpages/web-pages-list/');
     I.jstreeNavigate([folder_name]);

     // vymazanie webstranky webPage-autotest
     I.deleteCreatedWebPage(randomNumber);

     // vymazanie vytvoreneho priecinka name-autotest
     I.deleteFolderStructure(randomNumber);
});