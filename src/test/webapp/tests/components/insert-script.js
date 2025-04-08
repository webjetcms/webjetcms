Feature('components.insert-script');

Before(({ I, login }) => {
     login('admin');
     I.amOnPage("/admin/v9/apps/insert-script#/");
});

Scenario('insert script-zakladne testy @baseTest', async ({ I, DataTables}) => {
     I.waitForText('Skripty', 5);
     await DataTables.baseTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'],
          perms: 'cmp_insert_script'
     });
});

Scenario('pridanie platnosti od-do do upravovaneho zaznamu @baseTest', async ({ I, DataTables, DT }) => {
     await DataTables.baseTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'],
          perms: 'cmp_insert_script',

          //pridanie platnosti od-do 1.1.2021 00:00:00 do od-do 1.1.2022 00:00:00 do edit zaznamu
          editSteps: function (I, options, DT, DTE) {
               I.say('pridanie platnosti zaznamu');
               // zalozka obmedzenia
               I.clickCss('#pills-dt-insertScriptTable-scriptPerms-tab');
               // nastavenie datumu od
               I.clickCss('#DTE_Field_validFrom');
               I.selectOption('.dt-datetime-month', 'Január');
               I.selectOption('.dt-datetime-year', '2021');
               I.click(locate('.dt-datetime-button.dt-datetime-day').withText('1'));
               I.click(locate('.dt-datetime-button.dt-datetime-day').withText('00'));
               I.click(locate('.dt-datetime-button.dt-datetime-day').withAttr({ "data-unit": 'minutes' }).withText('00'));
               I.click(locate('.dt-datetime-button.dt-datetime-day').withAttr({ "data-unit": 'seconds' }).withText('00'));
               I.pressKey('Enter');
               // nastavenie datumu do
               I.waitForElement('#DTE_Field_validTo', 5);
               I.clickCss('#DTE_Field_validTo');
               I.selectOption('.dt-datetime-month', 'Január');
               I.selectOption('.dt-datetime-year', '2022');
               I.click(locate('.dt-datetime-button.dt-datetime-day').withText('1'));
               I.click(locate('.dt-datetime-button.dt-datetime-day').withText('00'));
               I.click(locate('.dt-datetime-button.dt-datetime-day').withAttr({ "data-unit": 'minutes' }).withText('00'));
               I.click(locate('.dt-datetime-button.dt-datetime-day').withAttr({ "data-unit": 'seconds' }).withText('00'));
               I.pressKey('Enter');

               I.clickCss('#pills-dt-insertScriptTable-main-tab');
          },

          // pridana platnost do upraveneho zaznamu
          editSearchSteps: function (I, options, DT, DTE) {
               I.say('pridana platnost do edit zaznamu');
               // vo vyhladanom zazname vidim validovanie od-do
               I.fillField('.datetimepicker.min.form-control.form-control-sm.dt-filter-from-validFrom', '01.01.2021 00:00:00');
               I.fillField('.datetimepicker.max.form-control.form-control-sm.dt-filter-to-validTo', '01.01.2022 00:00:00');
               I.pressKey('Enter', "input.dt-filter-availableGrooupsList");
               DT.waitForLoader();
               I.see("01.01.2021 00:00:00", "div.dt-scroll-body");
               I.see("01.01.2022 00:00:00", "div.dt-scroll-body");
               I.see(`${options.testingData[0]}-chan.ge`, "div.dt-scroll-body");
          },
     });
});

Scenario('pridanie adresara a webstranky do edit zaznamu @baseTest', async ({ I, DataTables, DT }) => {
     await DataTables.baseTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'],
          perms: 'cmp_insert_script',

          //pridanie adresara a webstranky do edit zaznamu
          editSteps: function (I, options, DT, DTE) {
               // pridanie adresara
               I.say('pridanie adresara');
               I.clickCss('#pills-dt-insertScriptTable-scriptPerms-tab');
               I.click('.btn.btn-outline-secondary.btn-vue-jstree-add');
               // vyber test23
               I.click(locate('.jstree-node.jstree-closed').withText('Test stavov').find('.jstree-icon.jstree-ocl'));
               I.click(locate('.jstree-anchor').withText('Zobrazený v menu'));

               // pridanie web stranky
               I.waitForElement('.btn.btn-outline-secondary.btn-vue-jstree-add');
               I.click(locate('.btn.btn-outline-secondary.btn-vue-jstree-add').withText('Pridať web stránku'));
               // vyber test23
               I.click(locate('.jstree-node.jstree-closed').withText('Test stavov').find('.jstree-icon.jstree-ocl'));
               I.click("Presmerovaná extrená linka", 'section.custom-modal div.jstree');

               I.clickCss('#pills-dt-insertScriptTable-main-tab');
          },

          // pridany adresar a web stranka
          editSearchSteps: function (I, options, DT, DTE) {
               I.say('pridany adresar a web stranka v zazname');
               // cakam na zobrazenie adresarov zaznamu vo vyhladanom zazname
               I.fillField('.form-control.form-control-sm.filter-input.dt-filter-groupIds', '/Test stavov/Zobrazený v menu');
               I.fillField('.form-control.form-control-sm.filter-input.dt-filter-docIds', '/Test stavov/Presmerovaná extrená linka');
               I.pressKey('Enter', "input.dt-filter-availableGrooupsList");
               DT.waitForLoader();
               I.see("/Test stavov/Zobrazený v menu", "div.dt-scroll-body");
               I.see("/Test stavov/Presmerovaná extrená linka", "div.dt-scroll-body");
               I.see(`${options.testingData[0]}-chan.ge`, "div.dt-scroll-body");
          },
     });
});

Scenario('pridanie sriptu do edit zaznamu @baseTest', async ({ I, DataTables, DT }) => {
     await DataTables.baseTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'],
          perms: 'cmp_insert_script',

          // pridanie scriptu do edit zaznamu
          editSteps: function (I, options, DT, DTE) {
               // pridanie scriptu
               I.say('pridanie scriptu do edit zaznamu');
               I.clickCss('#pills-dt-insertScriptTable-scriptBody-tab');
               I.fillField('#DTE_Field_scriptBody', '<!-- auto test script -->');

               I.clickCss('#pills-dt-insertScriptTable-main-tab');
          },

          // pridany script do zaznamu
          editSearchSteps: function (I, options, DT, DTE) {
               I.say('script pridany');
               // cakam, kym sa zobrazi vo vyhladavani script zaznamu
               I.fillField('.form-control.form-control-sm.filter-input.dt-filter-scriptBody', '<!-- auto test script -->');
               I.pressKey('Enter', "input.dt-filter-availableGrooupsList");
               DT.waitForLoader();
               I.see("<!-- auto test script -->", "div.dt-scroll-body"); // TODO filter nevyhlada zaznam podla scriptu
               I.see(`${options.testingData[0]}-chan.ge`, "div.dt-scroll-body");
          },
     });
});

Scenario('zmena hodnoty cookie @baseTest', async ({ I, DataTables, DT, DTE }) => {
     await DataTables.baseTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'],
          perms: 'cmp_insert_script',

          // zmena cookie - TODO doplniť diakritiku pre options
          editSteps: function (I, options, DT, DTE) {
               I.say('zmena cookie v edit zazname');
               // preferencne cookie
               I.click('div.DTE_Field_Name_cookieClass .dropdown.bootstrap-select.form-control');
               I.click(locate('.dropdown-item').withText('Preferenčné'));
               DTE.save();
               I.waitForText('Preferenčné', 10);
               I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
               // marketingove cookie
               I.click('div.DTE_Field_Name_cookieClass .dropdown.bootstrap-select.form-control');
               I.click(locate('.dropdown-item').withText('Marketingové'));
               DTE.save();
               I.waitForText('Marketingové', 10);
               I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
               // statisticke cookie
               I.click('div.DTE_Field_Name_cookieClass .dropdown.bootstrap-select.form-control');
               I.click(locate('.dropdown-item').withText('Štatistické'));
               DTE.save();
               I.waitForText('Štatistické', 10);
               I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
               // neklasifikovane cookie
               I.click('div.DTE_Field_Name_cookieClass .dropdown.bootstrap-select.form-control');
               I.click(locate('.dropdown-item').withText('Neklasifikované'));
               DTE.save();
               I.waitForText('Neklasifikované', 10);
               I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');

               I.clickCss('#pills-dt-insertScriptTable-main-tab');
          },

          // cookie PREFERENCNE
          editSearchSteps: function (I, options, DT, DTE) {
               I.say('zmenena cookie v edit zazname');
               // cakam na neklasifikovane cookie vo vyhladanom zazname
               DT.filterSelect('cookieClass', 'Neklasifikované');
               I.see("Neklasifikované", "div.dt-scroll-body");
               I.see(`${options.testingData[0]}-chan.ge`, "div.dt-scroll-body");
          },
     });
});

Scenario('Veci na prerobenie', ({ I }) => {
     // 1.oznacenie zaznamov - kontrola zmenenej anglictiny na slovencinu
     // kontrola 2 vybranych zaznamov
     //I.click('Zrušiť', '.btn.btn-outline-secondary.btn-close-editor'); // zatvori modalne okno
     I.forceClick('.dt-select-td.sorting_1');
     I.forceClick(locate('.dt-select-td.sorting_1').at(2));
     I.dontSee('2 rows selected'); // nezobrazi sa anglictina
     I.see('2 riadky označené');

     I.forceClick('.buttons-select-all.btn.btn-sm.btn-outline-secondary'); // odskrtne vsetky checkboxy
     I.dontSeeElement('.odd.selected'); // skontroluje ze ziadny checkbox nie je zaskrtnuty
     I.dontSee('2 riadky označené');

     // kontrola vsetkych vybranych zaznamov
     I.forceClick('.buttons-select-all.btn.btn-sm.btn-outline-secondary.dt-filter-id');
     I.waitForElement('.odd');
     I.dontSee('rows selected');
     I.see('riadkov označených');

     I.forceClick('.buttons-select-all.btn.btn-sm.btn-outline-secondary');
});

Scenario('insert script-import', async ({ I, DataTables }) => {
     I.waitForText('Skripty', 5);
     await DataTables.importTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'],
          file: 'tests/components/insert-script.xlsx',
          updateBy: 'Názov / popis skriptu - name',
          rows: [
               {
                    name: "Test import"
               }
          ]
     });
});


function testAlwaysIncluded(I, page) {
     I.seeInSource("//Skript bez obmedzeni demo domena");
     I.dontSeeInSource("//Skript pre SK domena mirroring");
     I.dontSeeInSource("//Skript bez obmedzeni domena test");
     //BUG: script was inserted with contains name, we must test as equals
     I.dontSeeInSource("//This script should not be included in head position");

     //datumovo obmedzene
     I.seeInSource("//Platny skript s casovo obmedzenou platnostou");
     I.dontSeeInSource("//Exspirovany skript v minulosti");
     I.dontSeeInSource("//Neplatny skript s casovo obmedzenou platnostou");
     I.seeInSource("//Platny planovany skript");

     if ("kontakt"==page) I.seeInSource("//Skript v priecinku kontakt");
     else I.dontSeeInSource("//Skript v priecinku kontakt");

     if ("test"==page) I.seeInSource("//Skript v priecinku test a stranke /en/home/");
     else I.dontSeeInSource("//Skript v priecinku test a stranke /en/home/");
}

Scenario('vkladanie skriptov', ({ I, DTE }) => {
     I.amOnPage("/?NO_WJTOOLBAR=true");
     testAlwaysIncluded(I, "home");

     I.amOnPage("/uvodna-stranka-thymeleaf.html?NO_WJTOOLBAR=true")
     testAlwaysIncluded(I, "home");


     I.amOnPage("/kontakt/");
     testAlwaysIncluded(I, "kontakt");

     I.amOnPage("/tseer/formular.html");
     testAlwaysIncluded(I, "test");

     I.amOnPage("/en/");
     testAlwaysIncluded(I, "test");

     //prepni domenu na test23
     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=5905");
     DTE.waitForEditor();

     I.amOnPage("/teeeeestststst/test-23-domena.html");
     I.dontSeeInSource("//Skript bez obmedzeni demo domena");
     I.dontSeeInSource("//Skript pre SK domena mirroring");
     I.seeInSource("//Skript bez obmedzeni domena test");

     I.amOnPage("/test23-newsletter/");
     I.dontSeeInSource("//Skript bez obmedzeni demo domena");
     I.dontSeeInSource("//Skript pre SK domena mirroring");
     I.seeInSource("//Skript bez obmedzeni domena test");

     //prepni na mirroring
     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=11036");
     DTE.waitForEditor();

     I.amOnPage("/slovensky/");
     I.dontSeeInSource("//Skript bez obmedzeni demo domena");
     I.seeInSource("//Skript pre SK domena mirroring");
     I.dontSeeInSource("//Skript bez obmedzeni domena test");

     I.amOnPage("/english/");
     I.dontSeeInSource("//Skript bez obmedzeni demo domena");
     I.dontSeeInSource("//Skript pre SK domena mirroring");
     I.dontSeeInSource("//Skript bez obmedzeni domena test");
});

Scenario('odhlasenie', ({ I }) => {
     I.logout();
});