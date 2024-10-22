Feature('webpages.folderRights-and-loginWebPage');

var folder_name, subfolder_one, subfolder_two;
var add_button = (locate('.tree-col').find('.btn.btn-sm.buttons-create.btn-success.buttons-divider'));
var edit_button = (locate('.tree-col').find('.btn.btn-sm.buttons-selected.buttons-edit.btn-warning'));

Before(({ I, login }) => {
     login('admin');
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     if (typeof folder_name=="undefined") {
          var randomNumber = I.getRandomText();
          folder_name = 'name-autotest-fralw' + randomNumber;
          subfolder_one = 'folder-autotest-' + randomNumber;
          subfolder_two = 'folder2-autotest-' + randomNumber;
     }
});

Scenario('Prava na adresar', ({ I, DTE }) => {
     I.waitForText('Zoznam web stránok', 10);

     // 1. vytvorenie noveho priecinka name-autotest
     I.say('1. Pridanie noveho priecinka name-autotest');
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', folder_name);
     I.groupSetRootParent();
     DTE.save();
     I.waitForText(folder_name, 20);
     I.wait(1);

     // 2. pridaj prava na adresar name-autotest
     I.say('2. Pridanie prav na adresar name-autotest');
     I.jstreeClick(folder_name);
     I.click(edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click('#pills-dt-groups-datatable-access-tab');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_0').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_1').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_2').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_3').find('.form-check-label'));
     DTE.save();
     I.waitForText(folder_name, 20);
     I.waitForElement(locate('.jstree-anchor.jstree-clicked').withDescendant('.jstree-text-icon.ti.ti-lock-filled').withText(folder_name), 10);
     I.wait(1);

     // 3. zrusenie ikony zamku priecinka name-autotest
     I.say('3. Zrusenie prav na priecinok name-autotest');
     // zrusenie pridanych prav pre priecinok name-autotest
     I.click(edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click('#pills-dt-groups-datatable-access-tab');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_0').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_1').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_2').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_3').find('.form-check-label'));
     DTE.save();
     I.waitForText(folder_name, 20);
     I.seeElement(locate('.jstree-anchor.jstree-clicked').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder-filled.jstree-themeicon-custom').withText(folder_name));
     I.dontSeeElement(locate('.jstree-anchor.jstree-clicked').withDescendant('.jstree-text-icon.ti.ti-lock-filled').withText(folder_name));
     I.wait(1);

     // 4. vytvorenie podadresarov priecinka name-autotest
     I.say('4. Vytvorenie podadresarov priecinka name-autotest');
     // folder-autotest
     I.jstreeClick(folder_name);
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', subfolder_one);
     DTE.save();
     I.waitForText(subfolder_one, 20);
     I.wait(1);
     // folder2-autotest
     I.jstreeClick(folder_name);
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', subfolder_two);
     DTE.save();
     I.waitForText(subfolder_two, 20);
     I.wait(1);

     // 5. pridanie prav pre priecinok name-autotest a aplikovanie na vsetky jeho podpriecinky
     I.say('5. Pridanie prav pre priecinok name-autotest a aplikovanie na vsetky jeho podpriecinky');
     I.jstreeClick(folder_name);
     I.click(edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click('#pills-dt-groups-datatable-access-tab');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_0').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-passwordProtectedSubFolders_0').find('.form-check-label')); // aplikovat na vsetky
     DTE.save();
     I.waitForText(folder_name, 20);
     I.seeElement(locate('.jstree-anchor.jstree-clicked').withDescendant('.jstree-text-icon.ti.ti-lock-filled').withText(folder_name));
     I.seeElement(locate('.jstree-node.jstree-leaf').withDescendant('.jstree-text-icon.ti.ti-lock-filled').withText(subfolder_one));
     I.seeElement(locate('.jstree-node.jstree-leaf').withDescendant('.jstree-text-icon.ti.ti-lock-filled').withText(subfolder_two));
     I.wait(1);

     // 6. zrusenie prav pre priecinok name-autotest a aplikovanie zmien na vsetky jeho podpriecinky
     I.say('6. Zrusenie prav pre priecinok name-autotest a aplikovanie zmien na vsetky jeho podpriecinky');
     I.jstreeClick(folder_name);
     I.click(edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click('#pills-dt-groups-datatable-access-tab');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-permisions_0').find('.form-check-label'));
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-passwordProtectedSubFolders_0').find('.form-check-label')); // aplikovat na vsetky
     DTE.save();
     I.waitForText(folder_name, 20);
     I.wait(2);
     I.jstreeClick(folder_name); //if refreshed click on it again
     I.seeElement(locate('.jstree-anchor.jstree-clicked').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder-filled.jstree-themeicon-custom').withText(folder_name));
     I.seeElement(locate('.jstree-node.jstree-leaf').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder-filled.jstree-themeicon-custom').withText(subfolder_one));
     I.seeElement(locate('.jstree-node.jstree-leaf').withDescendant('.jstree-icon.jstree-themeicon.ti.ti-folder-filled.jstree-themeicon-custom').withText(subfolder_two));
     I.dontSeeElement(locate('.jstree-anchor.jstree-clicked').withDescendant('.jstree-text-icon.ti.ti-lock-filled').withText(folder_name));
     I.dontSeeElement(locate('.jstree-node.jstree-leaf').withDescendant('.jstree-text-icon.ti.ti-lock-filled').withText(subfolder_one));
     I.dontSeeElement(locate('.jstree-node.jstree-leaf').withDescendant('.jstree-text-icon.ti.ti-lock-filled').withText(subfolder_two));
     I.wait(1);

     // 7. Vymazanie priecinka name-autotest
     I.say('7. Zmazanie priecinka name-autotest');
     I.jstreeClick(folder_name);
     I.clickCss('div.tree-col .btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
     I.waitForText('Zmazať', 5);
     I.click('Zmazať');
     I.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).');
     I.wait(1);
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.waitForText('Zoznam web stránok', 20);
     I.dontSee(folder_name, '.jstree-anchor');
});

Scenario('Nastavenie prihlasovacej stranky', ({ I, DTE }) => {

     //regenerate new folder name if folder from previous scenario was not deleted
     randomNumber = I.getRandomText();
     folder_name = 'name-autotest-' + randomNumber;
     subfolder_one = 'folder-autotest-' + randomNumber;
     subfolder_two = 'folder2-autotest-' + randomNumber;

     // 1. vytvorenie noveho priecinka name-autotest
     I.say('1. Pridanie noveho priecinka name-autotest');
     I.click(add_button);
     DTE.waitForEditor("groups-datatable");
     I.fillField('#DTE_Field_groupName', folder_name);
     I.groupSetRootParent();
     DTE.save();
     I.waitForText(folder_name, 20);
     I.wait(1);

     // 2. pridanie login stranky - nesmie to byt adresar
     I.say('2. Pridanie login stranky - nesmie to byt adresar');
     I.jstreeClick(folder_name);
     I.click(edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click('#pills-dt-groups-datatable-access-tab');
     I.click(locate('#editorAppDTE_Field_editorFields-logonPage').find('.btn.btn-outline-secondary.btn-vue-jstree-item-edit'));
     // nesmie to byt adresar
     I.click(locate('.jstree-node.jstree-closed').withText(folder_name).find('a.jstree-anchor'));
     I.waitForText('Vyberte web stránku', 5);
     I.click('.toast-close-button');
     I.wait(1);
     // vyber web stranku z adresara name-autotest
     I.click(locate('.jstree-node.jstree-closed').withText(folder_name).find('.jstree-icon.jstree-ocl'));
     I.click(locate('.jstree-node.is-default-page.jstree-leaf').withText(folder_name).find('.jstree-anchor'));
     DTE.save();
     I.waitForText(folder_name, 20);
     I.wait(1);

     // 3. stranka s formularom na prihlasenie je z priecinka name-autotest
     I.say('3. Stranka s formularom na prihlasenie je z priecinka name-autotest');
     I.jstreeClick(folder_name);
     I.click(edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click('#pills-dt-groups-datatable-access-tab');
     I.seeInField('#DTE_Field_editorFields-logonPage', folder_name);
     DTE.save();
     I.refreshPage();
     I.waitForText(folder_name, 20);

     // 4. pouzitie zakladnych nastaveni na stranku s formularom na prihlasenie
     I.say('4. Pouzitie zakladnych nastaveni');
     I.jstreeClick(folder_name);
     I.click(edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click('#pills-dt-groups-datatable-access-tab');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-useDefaultLogonPage_0').find('.form-check-label'));
     DTE.save();
     I.waitForText(folder_name, 20);
     I.wait(1);
     // kontrola vykonanych zmien
     I.click(edit_button);
     DTE.waitForEditor("groups-datatable");
     I.click('#pills-dt-groups-datatable-access-tab');
     I.seeInField('#DTE_Field_editorFields-logonPage', '"docId": 0');
     I.seeInField('#DTE_Field_editorFields-logonPage', '"logonPageDocId": 0');
     I.seeInField('#DTE_Field_editorFields-logonPage', '"fullPath": "/Nenastavené"');
     DTE.save();
     I.wait(1);
     I.refreshPage();
     I.waitForText(folder_name, 20);

     // 5. Vymazanie priecinka name-autotest
     I.say('5. Zmazanie priecinka name-autotest');
     I.jstreeClick(folder_name);
     I.clickCss('div.tree-col .btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider');
     I.waitForText('Zmazať', 5);
     I.click('Zmazať');
     I.dontSee('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).');
     I.wait(1);
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');
     I.waitForText('Zoznam web stránok', 20);
     I.dontSee(folder_name, '.jstree-anchor');
});