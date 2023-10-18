Feature('webpages.group-internal');

var auto_name, auto_folder_internal, auto_folder_public, sub_folder_public;
var add_button = (locate('.col-md-4.tree-col').find('.btn.btn-sm.buttons-create.btn-success.buttons-divider'));

Before(({ I, login }) => {
     login('admin');
     I.amOnPage('/admin/v9/webpages/web-pages-list#/');

     if (typeof auto_name=="undefined") {
          var randomNumber = I.getRandomText();
          auto_name = 'name-autotest-' + randomNumber;
          auto_folder_internal = 'internal_folder-autotest-' + randomNumber;
          auto_folder_public = 'public_folder-autotest-' + randomNumber;
          sub_folder_public = 'sub_folder_public-autotest-' + randomNumber;
     }
});

Scenario('Interny adresar', ({ I, DT, DTE }) => {
     I.waitForText('Zoznam web stránok', 5);

     // 1. vytvorenie priecinka name-autotest
     I.say('1. Pridanie noveho priecinka name-autotest');
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', auto_name);
     I.groupSetRootParent();
     DTE.save();
     DT.waitForLoader();
     I.waitForText(auto_name, 10);
     I.wait(1);

     // 2. nastavenie moznosti neverejne/nedostupne pre priecinok name-autotest a jeho overenie v stromovej strukture
     I.say('2. Priecinok je neverejny/nedostupny a jeho overenie v stromovej strukture');
     I.click(auto_name);
     I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     DTE.waitForEditor("groups-datatable");
     DTE.selectOption('internal', 'Nedostupné/Neverejné (interné pre administrátorov)'); // zmena dostupnosti priecinku pre verejnost
     I.waitForText('Nedostupné/Neverejné (interné pre administrátorov)', 5);
     DTE.save();
     I.waitForText(auto_name, 10);
     I.seeTextEquals(auto_name, locate('.jstree-node.is-internal.jstree-leaf').withText(auto_name)); // vidim ze priecinok ma classu .is-internal
     I.wait(1);

     // PODADRESARE
     // 3. pridanie neverejneho/nedostupneho podadresara do priecinka name-autotest
     I.say('3. Pridanie neverejneho/nedostupneho podadresara do priecinka name-autotest');
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.waitForText(auto_name, 5);
     I.fillField('#DTE_Field_groupName', auto_folder_internal);
     DTE.save();
     DT.waitForLoader();
     I.waitForText(auto_folder_internal, 10);
     I.seeElement(locate('.jstree-node.is-internal.jstree-leaf').withText(auto_folder_internal));
     I.wait(1);

     // 4. pridanie verejneho/dostupneho podadresara do priecinka name-autotest
     I.say('4. Pridanie verejneho/dostupneho podadresara do priecinka name-autotest');
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.waitForText(auto_name, 5);
     I.fillField('#DTE_Field_groupName', auto_folder_public);
     DTE.selectOption('internal', 'Dostupné/Verejné');
     I.waitForText('Dostupné/Verejné', 5);
     DTE.save();
     DT.waitForLoader();
     I.waitForText(auto_folder_public, 10);
     I.seeElement(locate('.jstree-node.jstree-leaf').withText(auto_folder_public)); // nema classu .is-internal
     I.wait(1);

     // TODO - aplikovat na vsetky podpriecinky nefunguje - switch sa po ulozeni zjavne neaplikuje do nastaveni - znovu po otvoreni uprav priecinka switch nie je ulozeny
     // 5. interny adresar - aplikovanie na vsetky podadresare - INTERNY ADRESAR
     I.say('5. Interny adresar - aplikovanie na vsetky podadresare');
     I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     I.waitForText('Aplikovať na všetky podpriečinky', 5);
     //nefunguje spolahlivo - I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceInternalToSubgroups_0')); // aplikovat na vsetky podpriecinky
     I.click("Aplikovať na všetky podpriečinky", "div.DTE_Field_Name_editorFields\\.forceInternalToSubgroups")
     DTE.save();
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.waitForText(auto_name, 15);
     DT.waitForLoader();
     I.seeElement(locate('.jstree-node.is-internal.jstree-closed').withText(auto_name));
     I.click(locate('.jstree-anchor').withText(auto_name));
     I.see(auto_folder_internal, '.jstree-node.is-internal.jstree-leaf');
     I.seeTextEquals(auto_folder_public, locate('.jstree-node.is-internal.jstree-leaf').withText(auto_folder_public));
     I.wait(1);

     // 6. zmena interneho adresara na verejny adresar a aplikovanie zmien na vsetky jeho podadresare - VEREJNY ADRESAR
     I.say('Zmena interneho adresara na verejny a aplikovanie zmien na podadresare');
     I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     DTE.waitForEditor("groups-datatable");
     I.selectOption('#DTE_Field_internal', 'Dostupné/Verejné');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceInternalToSubgroups_0'));
     DTE.save();
     I.waitForText(auto_name, 5);
     I.seeElement(locate('.jstree-anchor.jstree-clicked').withText(auto_name));
     I.see(auto_folder_internal, '.jstree-node.jstree-leaf');
     I.see(auto_folder_public, '.jstree-node.jstree-leaf');
     I.wait(1);

     // TODO - po zmene subfoldera a jeho podpriecinkov priecinka name-autotest - nemenit matersky folder
     // 7. vytvorenie verejneho a interneho podadresara v podadresari public_folder-autotest + zmena na interne + kontrola zmien
     I.say('7. Vytvorenie verejneho a interneho podadresara v podadresari public_folder-autotest');
     I.click(locate('.jstree-anchor').withText(auto_folder_public));
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', sub_folder_public);
     DTE.save();
     I.waitForText(sub_folder_public, 10);
     I.wait(1);
     // zmena na interne
     I.click('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning');
     I.selectOption('#DTE_Field_internal', 'Nedostupné/Neverejné (interné pre administrátorov)');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceInternalToSubgroups_0'));
     DTE.save();
     I.wait(1);
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.waitForText(auto_name, 15);
     I.wait(1);
     // kontrola zmeny
     I.click(locate('.jstree-anchor').withText(auto_name));
     I.see(auto_folder_public, '.jstree-node.is-internal.jstree-closed'); // public_folder-autotest = zmenene na internal
     I.click(locate('.jstree-node.is-internal.jstree-closed').withText(auto_folder_public).find('.jstree-icon.jstree-ocl'));
     I.see(sub_folder_public, '.jstree-node.is-internal.jstree-leaf'); // sub_folder_public-autotest = zmenene na internal
     I.see(auto_name, '.jstree-anchor.jstree-clicked'); // name-autotest = nezmenene = public
     I.see(auto_folder_internal, '.jstree-node.jstree-leaf'); // internal_folder-autotest = nezmenene = public
     I.wait(1);

     // 8. zmazanie priecinka name-autotest
     I.say('8. Zmazanie priecinka name-autotest');
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     DT.waitForLoader();
     I.waitForText(auto_name, 15);
     I.wait(1);
     I.click(locate('.jstree-anchor').withText(auto_name));
     I.click('div.tree-col .btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
     I.waitForText('Zmazať', 10);
     I.click('Zmazať');
     I.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).');
     DTE.waitForLoader();
     I.waitForText('Zoznam web stránok', 5);
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.dontSee(auto_name, '.jstree-anchor');
});