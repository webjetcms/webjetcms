Feature('webpages.group-parent');

var root1_name, subfolder_name, root2_name;
var add_button = (locate('.col-md-4.tree-col').find('.btn.btn-sm.buttons-create.btn-success.buttons-divider'));

Before(({ I, login }) => {
     login('admin');
     I.amOnPage('/admin/v9/webpages/web-pages-list#/');
     if (typeof root1_name=="undefined") {
          var randomText = I.getRandomText();
          root1_name = 'root1-autotest-' + randomText;
          subfolder_name = 'sub1-autotest-' + randomText;
          root2_name = 'root2-autotest-' + randomText;
     }
});

Scenario('1. pridanie noveho priecinka name-autotest', ({ I, DTE }) => {
     I.waitForText('Zoznam web stránok', 5);

     // 1. pridanie noveho priecinka name-autotest
     I.say('1. Pridanie noveho priecinka '+root1_name);
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', root1_name);
     I.groupSetRootParent();
     DTE.save();
     I.waitForText(root1_name, 5);
});

Scenario('2. otvorenie noveho priecinka a vytvorenie jeho podpriecinka', ({ I, DTE }) => {
     // 2. otvorenie noveho priecinka a vytvorenie jeho podpriecinka
     I.say('2. Vytvorenie podpriecinka');
     I.click(root1_name);
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', subfolder_name);
     I.waitForValue('#editorAppDTE_Field_editorFields-parentGroupDetails .input-group input', root1_name, 10);
     DTE.save();
     I.waitForText('Zoznam web stránok', 5);
});

Scenario('3. overenie vytvoreneho podpriecinka', ({ I }) => {
     // 3. overenie vytvoreneho podpriecinka
     I.say('3. Overenie vytvoreneho priecinka');
     I.waitForText(root1_name, 10, '.jstree-anchor');
     I.click(root1_name);
     I.waitForText(subfolder_name, 10, '.jstree-children');
});

Scenario('4. vytvorenie noveho priecinka - ulozeneho do korenoveho adresara', ({ I, DTE }) => {
     // 4. vytvorenie noveho priecinka - ulozeneho do korenoveho adresara
     I.say('4. Vytvorenie noveho priecinka do korenoveho adresara');
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.waitForElement('.form-control', 5);
     I.fillField('#DTE_Field_groupName', root2_name);
     I.groupSetRootParent();
     I.click('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'); // zmena na korenovy adresar
     I.waitForText('Koreňový priečinok', 5);
     I.click('.jstree-icon.jstree-themeicon.fas.fa-home.jstree-themeicon-custom');
     I.waitForValue('#editorAppDTE_Field_editorFields-parentGroupDetails .input-group input', '/', 10);
     DTE.save();
     // priecinok je ulozeny v korenovom priecinku
     I.waitForText(root2_name, '.jstree-node.jstree-leaf',);
});

Scenario('5a. overenie, ze sa nezobrazi v stromovej strukture aj web stranka', ({ I, DTE }) => {
     // 5. test negativnych/chybnych ciest - na priecinku z 1. kroku
     I.say('5a. overenie, ze sa nezobrazi v stromovej strukture aj web stranka');
     I.jstreeClick(root1_name);
     DTE.waitForLoader();
     I.click('div.tree-col .btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     DTE.waitForEditor("groups-datatable");
     I.waitForText(root1_name, '#DTE_Field_groupName');

     // ked rozkliknem adresar nemal by som v nom vidiet web stranku (cize rovnake meno ako ma adresar)
     I.click('.btn.btn-outline-secondary.btn-vue-jstree-item-edit');
     I.waitForElement(".jsTree-wrapper", 5);
     //I.click(locate('.jsTree-wrapper').find('.jstree-icon.jstree-ocl')); // rozklikne adresarovy strom
     //I.wait(1);
     I.waitForText(root1_name, 5);
     I.click(locate('.jstree-node.jstree-closed').withText(root1_name).find('.jstree-icon.jstree-ocl')); //rozklikne adresar
     I.wait(1);
     I.waitForText(subfolder_name, 5);
     within({css: "#custom-modal-id li.jstree-node.jstree-open ul li.jstree-node.jstree-open ul.jstree-children"}, () => {
          //vidim subfolder
          I.see(subfolder_name);
          //nevidim web stranku s rovnakym menom ako aktualny adresar
          I.dontSee(root1_name);
     });

});

Scenario('5b. test negativnych/chybnych ciest - na priecinku z 1. kroku', ({ I, DTE }) => {
     // 5. test negativnych/chybnych ciest - na priecinku z 1. kroku
     I.say('5b. Test negativnych/chybnych ciest');
     I.jstreeClick(root1_name);
     DTE.waitForLoader();
     I.click('div.tree-col .btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     DTE.waitForEditor("groups-datatable");
     I.waitForText(root1_name, '#DTE_Field_groupName');

     I.say('ako Nadradeny priecinok nie je mozne vybrat sameho seba');
     I.click('.btn.btn-outline-secondary.btn-vue-jstree-item-edit');
     I.waitForElement(".jsTree-wrapper", 5);
     //I.click(locate('.jsTree-wrapper').find('.jstree-icon.jstree-ocl')); // rozklikne adresarovy strom
     I.waitForText(root1_name, 5, '.jsTree-wrapper');
     I.wait(2);
     I.click(locate('.jstree-node.jstree-closed').withText(root1_name).find('a.jstree-anchor')); // vyberie totozny priecinok
     I.wait(1);
     DTE.save();
     I.waitForText('Priečinok nemôže byť súčasne zvolený ako svoj nadradený priečinok', 5);
     I.waitForText('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).', 5);

     I.say('ako Nadradeny priecinok nie je mozne vybrat podpriecinok sameho seba');
     I.click('.btn.btn-outline-secondary.btn-vue-jstree-item-edit');
     I.waitForElement(".jsTree-wrapper", 5);
     //I.click(locate('.jsTree-wrapper').find('.jstree-icon.jstree-ocl')); // rozklikne adresarovy strom
     I.waitForText(root1_name, 5, '.jsTree-wrapper');
     I.click(locate('.jsTree-wrapper').find('.jstree-node.jstree-closed').withText(root1_name).find('.jstree-icon.jstree-ocl')); // zobrazi podadresar
     I.wait(1);
     I.waitForText(subfolder_name, 5, '.jsTree-wrapper');
     I.click(locate('.jsTree-wrapper').find('.jstree-node.jstree-leaf').withText(subfolder_name).find('a.jstree-anchor'));
     I.wait(1);
     I.waitForText('Priečinok nemôže byť súčasne zvolený ako svoj nadradený priečinok', 5);
     DTE.save();
     I.waitForText('Nadradený priečinok nemôže byť vybraný zo svojho potomka', 5);
     I.waitForText('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).', 5);

     I.say('upravenie nadradeneho priecinka na korenovy adresar');
     I.click('.btn.btn-outline-secondary.btn-vue-jstree-item-edit');
     I.waitForText('Koreňový priečinok', 5);
     I.click('.jstree-icon.jstree-themeicon.fas.fa-home.jstree-themeicon-custom');
     I.wait(1);
     I.waitForValue('#editorAppDTE_Field_editorFields-parentGroupDetails .input-group input', '/', 10);
     I.wait(1);
     DTE.save();
     I.waitForText('Zoznam web stránok', 5);
     I.dontSee('Priečinok nemôže byť súčasne zvolený ako svoj nadradený priečinok');
     I.dontSee('Nadradený priečinok nemôže byť vybraný zo svojho potomka');
});

Scenario('6. vymazanie vytvorenych priecinkov', ({ I, DT, DTE }) => {
     // 6. vymazanie vytvorenych priecinkov
     // a) folder-autotest
     I.say('6. Vymazanie vytvorenych priecinkov');
     I.click(locate('.jstree-anchor').withText(root2_name));
     // skontrolujem cez upravit ci mam pristupove prava na priecinok
     I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     I.waitForText('Názov priečinku', 5);
     I.dontSee('Na tento priečinok nemáte prístupové práva. Skúste o úroveň nižšie.');
     I.click('.btn.btn-outline-secondary.btn-close-editor');
     I.waitForText('Zoznam web stránok', 5);
     // vymazanie priecinka folder-autotest
     I.click('div.tree-col .btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
     I.waitForText('Zmazať', 5);
     I.waitForText('Naozaj chcete zmazať položku?', 10);
     I.click('Zmazať');
     DT.waitForLoader();
     I.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).'); // hlasku by nemalo zobrazovat
     I.waitForText('Zoznam web stránok', 5);
     I.dontSeeElement(root2_name, '.jstree-anchor');

     // b) vymazanie podpriecinka value-autotest
     I.click(locate('.jstree-anchor').withText(root1_name)); // rozklikni hlavny priecinok name-autotest
     DT.waitForLoader();
     I.waitForText(subfolder_name, 10);
     I.click(locate('.jstree-anchor').withText(subfolder_name)); // klikni na podpriecinok
     // skontrolujem cez upravit ci mam pristupove prava na priecinok
     I.click('div.tree-col .btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     DTE.waitForEditor("groups-datatable");

     I.dontSee('Naozaj chcete zmazať položku?'); //WJ mal chybu, ked pred tym som klikol na zmazat a dal zrusit, potom zostalo zobrazene aj v editacii

     I.dontSee('Na tento priečinok nemáte prístupové práva. Skúste o úroveň nižšie.'); // TODO upravit slovo pristupve  - hlasku by nemalo zobrazovat
     I.click('.btn.btn-outline-secondary.btn-close-editor');
     I.waitForText('Zoznam web stránok', 5);
     I.wait(1);
     // vymazanie podpriecinka value-autotest
     I.click('div.tree-col .btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
     I.waitForText('Zmazať', 5);
     I.waitForText('Naozaj chcete zmazať položku?', 10); // slovo chete
     I.click('Zmazať');
     DT.waitForLoader();
     I.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).'); // hlasku by nemalo zobrazovat
     I.waitForText('Zoznam web stránok', 5);
     I.dontSeeElement(subfolder_name, '.jstree-anchor');

     // vymazanie priecinka name-autotest
     I.click(locate('.jstree-anchor').withText(root1_name));
     // skontrolujem cez upravit ci mam pristupove prava na priecinok
     I.click('div.tree-col .btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     I.waitForText('Názov priečinku', 5);
     I.dontSee('Na tento priečinok nemáte prístupové práva. Skúste o úroveň nižšie.'); // hlasku by nemalo zobrazovat
     I.click('.btn.btn-outline-secondary.btn-close-editor');
     I.waitForText('Zoznam web stránok', 5);
     // vymazanie priecinka folder-autotest
     I.click('div.tree-col .btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
     I.waitForText('Zmazať', 5);
     I.waitForText('Naozaj chcete zmazať položku?', 10); // Upraviť slovo chete
     I.click('Zmazať');
     DT.waitForLoader();
     I.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).'); // hlasku by nemalo zobrazovat
     I.waitForText('Zoznam web stránok', 5);
     I.dontSeeElement(root1_name, '.jstree-anchor');

     // TODO
     // zalozky System, Kos, Naposledy upravene -> nereaguju
});