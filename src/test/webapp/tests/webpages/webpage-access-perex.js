Feature('webpages.webpage-access-perex');

var folder_name, subfolder_one, subfolder_two, auto_webPage, randomNumber,
     add_webpage = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-create.btn-success.buttons-divider')),
     edit_webpage = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning')),
     delete_webpage = (locate('#datatableInit_wrapper').find('.btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider')),
     anotation = 'autotest',
     place = 'Bratislava';

Before(({ I, login }) => {
     login('admin');
     if (typeof folder_name == "undefined") {
          randomNumber = I.getRandomText();
          I.say("randomNumber=" + randomNumber);
          folder_name = 'name-autotest-' + randomNumber;
          subfolder_one = 'subfolder_one-autotest-' + randomNumber;
          subfolder_two = 'subfolder_two-autotest-' + randomNumber;
          auto_webPage = 'webPage-autotest-' + randomNumber;
     }
});

// vyplni a skontroluje vybrany datum v editore v poradí: nazov fieldu, den, mesiac, rok, hodina, minúta, sekunda - hodnoty je mozne menit
// mnozina primarnych udajov vstupnych dat: den {1-31}, mesiac {1-12}, hodina {00-23}, minuta {00-09, 10, 20, 30 , 40, 50}, sekunda {00-09, 10, 20, 30 , 40, 50}
function dtFillDate(I, fieldName, day, month, year, hour, minutes, seconds) {
     I.clickCss('#DTE_Field_' + fieldName);
     I.waitForVisible('.dt-datetime.horizontal', 15);
     I.selectOption('.dt-datetime-month', (month - 1).toString());
     I.waitForText(month, 15);
     I.selectOption(locate('.dt-datetime-label').find('select.dt-datetime-year'), year);
     I.waitForText(year, 15);
     I.click(locate('.dt-datetime-calendar').find('button.dt-datetime-button.dt-datetime-day').withText(day));
     I.waitForText(day, 15);
     I.click(locate('.dt-datetime-hours').find('.dt-datetime-button.dt-datetime-day').withText(hour));
     I.waitForText(hour, 15);
     I.click(locate('.dt-datetime-minutes').find('.dt-datetime-button.dt-datetime-day').withAttr({ "data-unit": 'minutes' }).withText(minutes));
     I.waitForText(minutes, 15);
     I.click(locate('.dt-datetime-seconds').find('.dt-datetime-button.dt-datetime-day').withAttr({ "data-unit": 'seconds' }).withText(seconds));
     I.waitForText(seconds, 15);
     I.pressKey('Enter');
     I.waitForVisible('#DTE_Field_' + fieldName, 15);
     if (day < 10 && month < 10) {
          I.waitForValue('#DTE_Field_' + fieldName, '0' + day + '.' + '0' + month + '.' + year + ' ' + hour + ':' + minutes + ':' + seconds, 10);
     } else if (day < 10 && month > 9) {
          I.waitForValue('#DTE_Field_' + fieldName, '0' + day + '.' + month + '.' + year + ' ' + hour + ':' + minutes + ':' + seconds, 10);
     } else if (day > 9 && month < 10) {
          I.waitForValue('#DTE_Field_' + fieldName, + day + '.' + '0' + month + '.' + year + ' ' + hour + ':' + minutes + ':' + seconds, 10);
     } else {
          I.waitForValue('#DTE_Field_' + fieldName, + day + '.' + month + '.' + year + ' ' + hour + ':' + minutes + ':' + seconds, 10);
     }
}

Scenario('Priprav strukturu', ({ I }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/');

     // vytvorenie hlavneho priecinka a jeho dvoch podpriecinkov
     I.createFolderStructure(randomNumber, false);

     // vytvorenie webstranky
     I.createNewWebPage(randomNumber);
});

Scenario('Pristup a perex webstranky', async ({ I, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/');
     I.jstreeNavigate([folder_name]);

     // editovanie webstranky
     I.editCreatedWebPage(randomNumber);

     // Aktivovat - Zobrazenie web stranky a vyhladanie cez vyhladavace
     I.say('Aktivovanie zobrazenia web stranky a vyhladavanie cez vyhladavace');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_available_0').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_searchable_0').find('.form-check-label'));

     // ------------------------ ZALOZKA PRISTUP ------------------
     I.clickCss('#pills-dt-datatableInit-access-tab');
     I.waitForText('Obmedzenie prístupu', 5);
     I.waitForVisible('#panel-body-dt-datatableInit-access', 10);

     // Obmedzenie pristupu
     I.say('Obmedzenie pristupu');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_0').find('.form-check-label')); // bankari
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_1').find('.form-check-label')); // newsletter
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_2').find('.form-check-label')); // obchodni partneri
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_3').find('.form-check-label')); // redaktori

     // ------------------------ ZALOZKA PEREX ------------------
     I.say('Zalozka perex');
     I.clickCss('#pills-dt-datatableInit-perex-tab');
     I.waitForText('Dátum začiatku', 10);

     // Datum zaciatku
     I.say('Datum zaciatku');
     dtFillDate(I, 'publishStartDate', '10', '6', '2031', '00', '00', '10');
     const dateOfStart = await I.grabValueFrom('[id="DTE_Field_publishStartDate"]'); // zober datum zaciatku

     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-publishAfterStart_0').find('.form-check-label'));

     // Datum konca
     I.say('Datum konca');
     dtFillDate(I, 'publishEndDate', '11', '12', '2032', '14', '50', '20');
     const dateOfEnd = await I.grabValueFrom('[id="DTE_Field_publishEndDate"]'); // zober datum konca

     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-disableAfterEnd_0').find('.form-check-label'));

     // Datum konania
     I.say('Datum konania');
     dtFillDate(I, 'eventDateDate', '1', '1', '2032', '22', '30', '30');
     const dateOfAction = await I.grabValueFrom('[id="DTE_Field_eventDateDate"]'); // zober datum konania

     // Anotacia
     I.say('Anotacia');
     I.fillField('#DTE_Field_htmlData', anotation);

     // Miesto konania
     I.say('Miesto konania');
     I.fillField('#DTE_Field_perexPlace', place);

     // Obrazok
     I.say('Pridaj obrazok do perexu');
     I.click(locate('.DTE_Field.form-group.row.DTE_Field_Type_elfinder.DTE_Field_Name_perexImage.image').withText('Obrázok').find('button.btn.btn-outline-secondary'));
     I.waitForElement('.modal-header h5', 10);
     I.waitForElement("#modalIframeIframeElement", 10);
     I.switchTo('#modalIframeIframeElement');
     I.waitForLoader(".WJLoaderDiv");
     I.waitForElement(locate('.elfinder-navbar-wrapper').withText('Obrázky'), 15);
     I.wait(4);
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), null, { position: { x: 20, y: 5 } });
     I.waitForText('Foto galéria', 20, ".elfinder-cwd-file");
     I.waitForElement(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('Foto galéria'), 15);
     I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('Foto galéria'));
     I.wait(1);
     I.waitForVisible(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'), 20);
     I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'));
     I.wait(1);
     I.waitForVisible(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee'), 10);
     I.wait(1);
     var selector = '.elfinder-cwd-file.ui-corner-all.ui-selectee:not(.directory)'
     I.waitForVisible(locate(selector));
     I.wait(1);
     I.forceClick(locate(selector));
     I.wait(1);

     const fileName = await I.grabAttributeFrom(selector+' div.elfinder-cwd-filename', 'title'); // zober nazov oznaceneho obrazka

     I.say("Mam fileName="+fileName);

     I.switchTo();
     I.wait(0.2);
     I.click(locate('#modalIframe button.btn.btn-primary').withText('Potvrdiť'));
     I.waitForValue('div.DTE_Field_Name_perexImage .form-control', '/images/gallery/test/' + fileName, 5);

     // Znacky
     I.say('Pridaj znacky do perexu');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_perexGroups_0').find('.form-check-label')); // dalsia perex skupina
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_perexGroups_1').find('.form-check-label')); // investicia
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_perexGroups_2').find('.form-check-label')); // podnikanie
     DTE.save();
     I.waitForText("Stránka bola uložená, bude automaticky publikovaná do verejnej časti web stránky 10.06.2031 0:00:10", 10, "div.toast-message");
     I.clickCss("button.toast-close-button");
     I.waitForVisible('#SomStromcek');

     // ------------------------ KONTROLA ULOZENIA UDAJOV ------------------
     I.wait(3);

     I.checkEditedWebPage(randomNumber);
     I.waitForText("Existuje rozpracovaná alebo neschválená verzia tejto stránky. Skontrolujte kartu História.", 10, "div.toast-message");
     I.wait(2);
     I.click("Editovať poslednú verziu", "a.btn.btn.btn-primary");
     I.wait(3);

     // Skontroluje ci sa ulozili vlastnosti webstranky - Zobrazovat a Prehladavat
     I.say('Skontroluje ci sa ulozili vlastnosti webstranky - Zobrazovat a Prehladavat');
     I.clickCss('#pills-dt-datatableInit-basic-tab');
     I.waitForVisible('#DTE_Field_available_0', 10);
     I.seeCheckboxIsChecked('#DTE_Field_available_0');
     I.seeCheckboxIsChecked('#DTE_Field_searchable_0');

     // Skontrolujem ulozene udaje v Pristupe - aktivne prve 4 toggle buttony
     I.say('Skontrolujem ulozene udaje v Pristupe - aktivne prve 4 toggle buttony');
     I.clickCss('#pills-dt-datatableInit-access-tab');
     I.waitForVisible('#panel-body-dt-datatableInit-access', 10);
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-permisions_0'); // bankari
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-permisions_1'); // newsletter
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-permisions_2'); // obchodni partneri
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-permisions_3'); // redaktori

     // Skontrolujem ulozene udaje v Perexe
     I.say('Skontrolujem ulozene udaje v Perexe');
     I.clickCss('#pills-dt-datatableInit-perex-tab');
     I.waitForVisible('#pills-dt-datatableInit-perex', 10);
     I.waitForValue('#DTE_Field_publishStartDate', dateOfStart, 10); // Datum  - TODO neulozia sa nastavene sekundy
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-publishAfterStart_0'); // Zverejnit stranku po tomto datume - TODO checkbox nie je aktivny
     I.waitForValue('#DTE_Field_publishEndDate', dateOfEnd, 10); // Datum konca - TODO neulozia sa nastavene sekundy
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-disableAfterEnd_0'); // Odverejnit stranku po tomto datume - TODO checkbox nie je aktivny
     I.waitForValue('#DTE_Field_eventDateDate', dateOfAction, 10); // Datum konania - TODO neulozia sa nastavene sekundy
     I.seeInField('#DTE_Field_htmlData', anotation); // Anotacia
     I.seeInField('#DTE_Field_perexPlace', place); // Miesto konania
     I.waitForValue('.form-control', '/images/gallery/test/' + fileName, 10); // Obrazok
     I.seeCheckboxIsChecked('#DTE_Field_perexGroups_0'); // dalsia perex skupina
     I.seeCheckboxIsChecked('#DTE_Field_perexGroups_1'); // investicia
     I.seeCheckboxIsChecked('#DTE_Field_perexGroups_2'); // podnikanie

     DTE.save();

     // Skontroluj ci ma webstranka priznak "needitovatelna"
     I.waitForElement(locate('#datatableInit').withDescendant('tbody>tr.even').withText(auto_webPage).find('.cell-not-editable'), 15);
});

Scenario('Zmaz strukturu', ({ I }) => {
     //ak zostalo nieco zaseknute na editacii reloadni aby sa dalo dobre zmazat
     I.amOnPage('/admin/v9/webpages/web-pages-list/');
     I.jstreeNavigate([folder_name]);

     // vymazanie webstranky
     I.deleteCreatedWebPage(randomNumber);

     // vymazanie vytvoreneho priecinka name-autotest
     I.deleteFolderStructure(randomNumber);
});