Feature('wj8-folder-mirroring');

var auto_folder_sk, auto_folder_en, auto_subfolder1_sk, auto_subfolder2_sk, test_folder, elfinder_folder, randomNumber;
var domainName = 'mirroring.tau27.iway.sk';

Before(({ I, login }) => {
     login('admin');
     if (typeof randomNumber == "undefined") {
          randomNumber = I.getRandomTextShort();
          I.say("randomNumber=" + randomNumber);
          auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
          auto_folder_en = 'en-mir-autotest-' + randomNumber;
          auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
          auto_subfolder2_sk = 'sk-mir-subfolder2-' + randomNumber;
          test_folder = 'TEST-autotest-' + randomNumber;
          elfinder_folder = 'test_mirroring_elfinder-' + randomNumber;
     }
     I.resizeWindow(1600, 1200);
});

function wj8CreateMirrorSubfolder(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
     var auto_subfolder2_sk = 'sk-mir-subfolder2-' + randomNumber;
     // vytvorenie 1. podpriecinka do sk adresara
     I.say('Vytvaram prvy podpriecinok pre sk adresar');
     I.waitForVisible('#treeScrollDiv', 10);
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForLoader(".WJLoaderDiv");
     I.waitForVisible(locate('a.jstree-anchor').withText(auto_folder_sk), 5);
     I.click('.wjIconBig.wjIconBig-dirAdd');
     I.wait(1);
     I.switchToNextTab();
     I.waitForText('Vlastnosti adresára', 5);
     I.waitForValue('#parentGroup', auto_folder_sk, 5);
     I.fillField('#groupName', auto_subfolder1_sk);
     I.click('td>input#btnOK');
     I.wait(1);
     I.switchToNextTab();
     // pridanie druheho podadresara do sk adresara cez kontextove okno
     I.say('Vytvaram druhy podpriecinok pre sk adresar cez kontextove okno');
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForVisible(locate('a.jstree-anchor').withText(auto_folder_sk), 5);
     I.rightClick(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForVisible('.vakata-context.jstree-contextmenu.jstree-default-contextmenu', 5);
     I.click('Pridať podadresár');
     I.wait(1);
     I.switchToNextTab();
     I.waitForText('Vlastnosti adresára', 5);
     I.waitForValue('#parentGroup', auto_folder_sk, 5);
     I.fillField('#groupName', auto_subfolder2_sk);
     I.click('td>input#btnOK');
     I.wait(1);
     I.switchToNextTab();
     I.waitForVisible('#treeScrollDiv', 10);
     I.waitForElement('a.jstree-anchor', auto_subfolder2_sk, 5);
     // kontrola ci sa podpriecinky vytvorili aj v en adresari - nezverejnene
     I.say('Kontroluje ci sa podpriecinky vytvorili i v en strukture - nezverejnene');
     I.waitForElement(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder1_sk), 5);
     I.waitForElement(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder2_sk), 5);
     // kontrola ci sa v podpriecinkoch v en vytvorili hlavne stranky - nezverejnene
     I.say('Kontroluje ci sa v podpriecinkoch vytvorili i hlavne stranky - nezverejnene');
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder1_sk));
     I.waitForElement(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-danger.jstree-themeicon-custom').withText(auto_subfolder1_sk), 5);
     I.wait(1);
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder2_sk));
     I.waitForElement(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-danger.jstree-themeicon-custom').withText(auto_subfolder2_sk), 5);
}

function wj8MoveSubfolder(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
     var auto_subfolder2_sk = 'sk-mir-subfolder2-' + randomNumber;
     // presun subfolder2 do subfoldra1 v sk adresari
     I.say('Presuvam druhy subfolder do prveho subfoldra v sk strukture');
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon').withText(auto_subfolder2_sk));
     I.waitForElement(locate('.jstree-anchor.jstree-clicked').withChild('.jstree-icon.jstree-themeicon').withText(auto_subfolder2_sk), 5);
     I.wait(2);
     I.click('.wjIconBig.wjIconBig-dirEdit');
     I.wait(1);
     I.switchToNextTab();
     I.waitForText('Vlastnosti adresára', 5);
     I.waitForValue('#parentGroup', auto_folder_sk, 5);
     I.click('Zmeň');
     I.switchToNextTab();
     I.click(locate('.webfx-tree-item.folder').withText(auto_folder_sk).find({ css: 'img:first-child' }));
     I.waitForVisible(locate('.webfx-tree-item.folder').withText(auto_subfolder1_sk), 5);
     I.click(locate('.webfx-tree-item.folder').withText(auto_subfolder1_sk).find('a'));
     I.switchToNextTab();
     I.switchToNextTab();
     I.waitForText('Vlastnosti adresára', 5);
     I.click('td>input#btnOK');
     I.wait(1);
     I.switchToNextTab();
     I.waitForVisible('#treeScrollDiv', 10);
     // skontroluj ci sa subfolder2 presunul do subfolder1 v sk adresari aj spolu s hlavnou strankou
     I.say('Kontrolujem ci sa subfolder dva presunul do prveho subfoldra v sk strukture');
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon').withText(auto_subfolder1_sk));
     I.waitForElement(locate('.jstree-node.jstree-open').withDescendant('.jstree-children>li.jstree-node.jstree-closed').withText(auto_subfolder2_sk), 5);
     I.click(locate('.jstree-node.jstree-open').withDescendant('.jstree-children>li.jstree-node.jstree-closed').withText(auto_subfolder2_sk));
     I.waitForElement(locate('.jstree-node.jstree-open').withDescendant('.jstree-children>li.jstree-node.jstree-leaf.jstree-last').withText(auto_subfolder2_sk), 5);
     I.wait(1);
     // skontroluj ci sa priecinok presunul aj v en adresari aj s hlavnou strankou
     I.say('Kontrolujem ci sa subfolder dva presunul do prveho subfoldra v en strukture aj s hlavnou strankou subfoldra');
     I.click(locate('.jstree-anchor').withText(auto_folder_en));
     I.waitForElement(locate('.jstree-node.jstree-open').withDescendant('.jstree-anchor>i.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder1_sk), 5);
     I.click(locate('.jstree-anchor').withDescendant('i.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder1_sk));
     I.waitForElement(locate('.jstree-node.jstree-open').withDescendant('.jstree-anchor>i.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder2_sk), 5);
     I.click(locate('.jstree-anchor').withDescendant('i.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder2_sk));
     I.waitForElement(locate('ul.jstree-children>li.jstree-node.jstree-open').withDescendant('.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder2_sk), 5);
}

function wj8DeleteSubfolder(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var auto_subfolder2_sk = 'sk-mir-subfolder2-' + randomNumber;
     // vymazanie podpriecinka v sk strukture
     I.say('Vymazavam druhy podpriecinok v sk strukture');
     I.waitForElement(locate('.jstree-anchor').withText(auto_folder_sk), 5);
     I.waitForElement(locate('.jstree-anchor').withText(auto_folder_en), 5);
     I.click(locate('.jstree-anchor').withDescendant('.jstree-icon.jstree-themeicon').withText(auto_subfolder2_sk));
     I.rightClick(locate('.jstree-anchor').withDescendant('.jstree-icon.jstree-themeicon').withText(auto_subfolder2_sk));
     I.waitForElement('.vakata-context.jstree-contextmenu.jstree-default-contextmenu', 5);
     I.click('.glyphicon.glyphicon-remove'); // zobrazi modalne okno
     I.wait(10);
     I.dontSeeElement(locate('.jstree-node.jstree-open').withDescendant('.jstree-anchor.jstree-icon.jstree-themeicon').withText(auto_subfolder2_sk));
     // skontrolujen ci subfolder vymazalo aj v en strukture
     I.say('Kontroluje ci sa druhy podpriecinok vymazal i v en strukture');
     I.dontSeeElement(locate('.jstree-anchor').withDescendant('.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder2_sk));
     I.wait(1);
}

function wj8elFinder(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var elfinder_folder = 'test-mir-elfinder-sk-' + randomNumber;
     // pridaj priecinok do hlavnej stranky podpriecinka subfolder1 v sk strukture
     I.say('Pridavam priecinok cez elFinder do prveho podpriecinkav sk strukture');
     I.amOnPage('/admin/elFinder/#elf_iwcm_doc_group_volume_');
     I.waitForText('Prieskumník', 10);
     I.waitForVisible('#nav-iwcm_doc_group_volume_');
     I.waitForElement(locate('.elfinder-navbar-wrapper').withText(auto_folder_sk));
     I.waitForElement(locate('.elfinder-navbar-wrapper').withText(auto_folder_en));
     I.wait(1);
     I.click(locate('span.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_sk));
     I.waitForElement('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.rightClick('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.waitForVisible('.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle');
     I.click(locate('.elfinder-contextmenu-item').withText('Nový priečinok'));
     I.wait(1);
     I.type(elfinder_folder);
     I.wait(1);
     I.pressKey('Enter');
     I.waitForText(elfinder_folder, 10);
     // over ci sa tento priecinok pridal aj do en struktury
     I.say('Over ci sa tento priecinok pridal aj do en struktury');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_en));
     I.waitForElement(locate('.elfinder-cwd-file').withText(elfinder_folder));
     I.wait(1);
     // Skopiruj tento novy priecinok v sk adresari
     I.say('Skopiruj tento novy priecinok v sk adresari');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_sk));
     I.waitForElement('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.click(locate('div.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText(elfinder_folder));
     I.wait(1);
     I.rightClick('.elfinder-cwd-file-wrapper.ui-corner-all.ui-draggable-handle.ui-state-hover');
     I.waitForVisible('.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle');
     I.click(locate('.elfinder-contextmenu-item').withText('Kopírovať'));
     I.waitForElement('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.rightClick('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.waitForVisible('.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle');
     I.click(locate('.elfinder-contextmenu-item').withText('Vložiť'));
     I.waitForLoader(".WJLoaderDiv");
     I.wait(3);
     I.click('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-filename').withText(elfinder_folder), 2);
     I.wait(1);
     // over ci sa 2 rovnake elfinder adresare aj v en strukture
     I.say('Over ci sa 2 rovnake elfinder adresare aj v en strukture');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_en));
     I.wait(1);
     I.waitForVisible('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-filename').withText(elfinder_folder), 2);
     I.wait(1);
     // duplikuj priecinok v sk adresari
     I.say('Duplikuj priecinok v sk adresari');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_sk));
     I.waitForElement('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.click(locate('.elfinder-cwd-filename').withText(elfinder_folder));
     I.rightClick('.elfinder-cwd-file-wrapper.ui-corner-all.ui-draggable-handle.ui-state-hover');
     I.waitForVisible('.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle');
     I.click(locate('.elfinder-contextmenu-item').withText('Duplikovať'));
     I.waitForLoader(".WJLoaderDiv");
     I.wait(1);
     I.click('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-filename').withText(elfinder_folder), 3);
     I.wait(1);
     // over ci sa priecinok duplikoval aj v en strukture
     I.say('Over ci sa priecinok duplikoval aj v en strukture');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_en));
     I.wait(1);
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-filename').withText(elfinder_folder), 3);
     I.wait(1);
     // vymaz priecinok zo sk adresara - teraz su tu 3xadresare
     I.say('Vymaz priecinok zo sk adresara');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_sk));
     I.waitForElement('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.click('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.wait(1);
     I.click(locate('.elfinder-cwd-filename').withText(elfinder_folder).at(2));
     I.rightClick('.elfinder-cwd-file-wrapper.ui-corner-all.ui-draggable-handle.ui-state-hover');
     I.waitForVisible('.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle');
     I.click(locate('.elfinder-contextmenu-item').withText('Vymazať'));
     I.waitForVisible('.ui-dialog.ui-widget.ui-widget-content.ui-corner-all.ui-draggable.std42-dialog.touch-punch.elfinder-dialog.elfinder-dialog-confirm.elfinder-dialog-active.ui-front');
     I.click(locate('button').withText('Vymazať'));
     I.wait(3);
     I.waitForLoader(".WJLoaderDiv");
     I.wait(2);
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-state-disabled.ui-droppable-disabled.ui-selectee').withText(elfinder_folder), 0);
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText(elfinder_folder), 2);
     I.wait(1);
     // skontroluj ci sa priecinok vymazal aj v en adresari
     I.say('Skontroluj ci sa priecinok vymazal aj v en adresari');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_en));
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-state-disabled.ui-droppable-disabled.ui-selectee').withText(elfinder_folder), 0);
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText(elfinder_folder), 2);
     I.wait(1);
}

/*
Toto nie je implementovane, ked sa presunie root nic sa neudeje (lebo sa presunie mimo nastavenych adresarov)
function wj8MoveMainFolder(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var test_folder = 'TEST-autotest-' + randomNumber;
     // vytvor testovaci priecinok TEST
     I.say('Vytvaran testovaci priecinok TEST-autotest-');
     I.waitForLoader(".WJLoaderDiv");
     I.click('.tree-settings>span>i.fa.fa-cog');
     I.waitForVisible('.modal-content', 10);
     I.click('#newRootGroup');
     I.waitForElement("input.jstree-rename-input");
     I.type(test_folder);
     I.pressKey('Enter');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(5);
     I.waitForVisible('#groupslist-jstree', 10);
     // presun hlavny sk adresar do tohoto testovacieho adresara
     I.say('Presuvam hlavny sk adresar do testovacieho adresara');
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.click('.wjIconBig.wjIconBig-dirEdit');
     I.switchToNextTab();
     I.waitForText('Vlastnosti adresára', 5);
     I.click('Zmeň');
     I.switchToNextTab();
     I.waitForText(test_folder, 10);
     I.click(locate('div.webfx-tree-item.folder>a').withText(test_folder));
     I.switchToNextTab();
     I.switchToNextTab();
     I.click('td>input#btnOK');
     I.wait(1);
     I.switchToNextTab();
     I.waitForVisible('#treeScrollDiv', 10);
     I.wait(1);
     // skontroluj ci sa hlavny sk adresar presunul do testovacieho adresara
     I.say('Kontrolujem ci sa hlavny sk adresar presunul do testovacieho adresara');
     I.click(locate('.jstree-anchor').withText(test_folder));
     I.waitForElement(locate('.jstree-node.jstree-open.jstree-last').withDescendant('i.jstree-icon.jstree-themeicon').withText(auto_folder_sk), 5);
     // skontroluj ci sa sem presunul aj en adresar
     I.say('Kontrolujem ci sa hlavny en adresar tiez presunul do testovacieho adresara');
     I.waitForElement(locate('.jstree-node.jstree-open.jstree-last').withDescendant('i.jstree-icon.jstree-themeicon').withText(auto_folder_en), 5); // TODO nepresunie hlavny priecinok en do skusobneho test adresara
}
*/

function wj8DeleteMainFolder(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     // vymaz hlavny sk adresar
     I.say('Vymazavam hlavny sk adresar');
     I.waitForVisible(locate('.jstree-anchor').withText(auto_folder_sk), 5);
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.rightClick(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForVisible('.vakata-context.jstree-contextmenu.jstree-default-contextmenu', 5);
     I.click('.glyphicon.glyphicon-remove');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(3);

     //reloadni sa na adresar 1 aby sa nezobrazil otvoreny Kos
     I.amOnPage('/admin/webpages/?groupid=1');
     I.waitForVisible('#treeScrollDiv', 10);
     I.dontSeeElement(locate('.jstree-anchor').withText(auto_folder_sk));
     // skontroluj ci je vymazany i hlavny en folder
     I.say('Kontrolujem ci sa automaticky vymazal i en adresar');
     I.dontSeeElement(locate('.jstree-anchor').withText(auto_folder_en));
     I.wait(1);
     I.waitForVisible('#treeScrollDiv', 10);
}

/*
Toto nie je implementovane, ked sa presunie root nic sa neudeje (lebo sa presunie mimo nastavenych adresarov)
function wj8DeleteTestFolder(I, randomNumber) {
     var test_folder = 'TEST-autotest-' + randomNumber;
     // vymazanie test priecinka
     I.say('Vymazavam testovaci adresar TEST-autotest-');
     I.waitForVisible('#treeScrollDiv', 10);
     I.click(locate('.jstree-anchor').withText(test_folder));
     I.rightClick(locate('.jstree-anchor').withText(test_folder));
     I.waitForElement('.vakata-context.jstree-contextmenu.jstree-default-contextmenu', 5);
     I.click('.glyphicon.glyphicon-remove');
     I.wait(3);
     I.waitForVisible('#treeScrollDiv', 10);
     I.dontSeeElement(locate('.jstree-anchor').withText(test_folder));
}
*/

Scenario('Vytvorenie struktury', async ({ I }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');

     // vytvorenie mirroring hlavneho priecinka sk a en
     I.wj8CreateMirroringStructure(randomNumber);

     // skopirovanie groupID
     I.waitForVisible('#treeScrollDiv', 10);
     I.click(locate('a.jstree-anchor').withText(auto_folder_sk));
     I.wait(2);
     const skId = await I.grabValueFrom('#groupIdInputBox');
     I.say('skID: ' + skId);
     I.wait(2);
     I.click(locate('a.jstree-anchor').withText(auto_folder_en));
     I.wait(2);
     const enId = await I.grabValueFrom('#groupIdInputBox');
     I.say('enID: ' + enId);
     I.wait(1);

     // nastavenie konfiguracnej premennej structureMirroringConfig
     I.amOnPage('/admin/conf_editor.jsp');
     I.waitForVisible('.tab-content', 10);
     I.click(locate('tr>td.not-allow-hide.row-controller-and-settings.text-left').withText('structureMirroringConfig'));
     I.switchToNextTab();
     I.waitForText('Pridať novú premennú', 5);
     I.fillField('#confValue', skId + ',' + enId + ':' + 'mirroring.tau27.iway.sk');
     I.click('td>input#btnOK');
     I.wait(1);
     I.switchToNextTab();
     I.waitForElement(locate('tbody>tr>td').withText(skId + ',' + enId + ':' + 'mirroring.tau27.iway.sk'), 10);
     I.wait(1);
     I.amOnPage('/admin/webpages/');

});

Scenario('Vytvorenie podpriecinkov', ({ I }) => {

     // vytvorenie dvoch podpriecinkov subfolder1 a subfolder2 cez sk priecinok
     wj8CreateMirrorSubfolder(I, randomNumber);
     I.wait(1);

});

Scenario('Presun podpriecinka', ({ I }) => {

     //potrebujeme aby nam zostal EN adresar zatvoreny, preto takto cez reload
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon').withText(auto_folder_sk));
     I.wait(5);
     I.amOnPage('/admin/webpages/');
     I.waitForVisible(locate('a.jstree-anchor').withText(auto_folder_sk), 5);
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon').withText(auto_folder_sk));
     I.wait(5);

     // presun podpriecinka subfolder2 do ineho podpriecinka subfolder1
     wj8MoveSubfolder(I, randomNumber);
     I.wait(1);

     // vymazanie podpriecinka subfolder2
     wj8DeleteSubfolder(I, randomNumber);
     I.wait(1);

});

Scenario('elfinder test', ({ I }) => {

     // vytvorenie priecinkov v elfinderi v sk strukture
     wj8elFinder(I, randomNumber);
     I.wait(1);

     // presun hlavneho priecinka sk do ineho priecinka TEST
     //I.amOnPage('/admin/webpages/');
     //Toto nie je implementovane, ked sa presunie root nic sa neudeje (lebo sa presunie mimo nastavenych adresarov)
     //wj8MoveMainFolder(I, randomNumber);
     //I.wait(1);

});

Scenario('Vymazanie', ({ I }) => {

     // vymazanie hlavneho sk priecinka z test priecinka
     //Toto nie je implementovane, ked sa presunie root nic sa neudeje (lebo sa presunie mimo nastavenych adresarov)
     //I.click(locate('.jstree-anchor').withText(test_folder));
     I.amOnPage('/admin/webpages/?groupid=1');
     wj8DeleteMainFolder(I, randomNumber);
     I.wait(1);

     // vymazanie testovacieho priecinka TEST
     //Toto nie je implementovane, ked sa presunie root nic sa neudeje (lebo sa presunie mimo nastavenych adresarov)
     //wj8DeleteTestFolder(I, randomNumber);
     I.wait(1);
});

Scenario('odhlasenie', ({ I }) => {
     I.amOnPage('/logoff.do?forward=/admin/');
 });