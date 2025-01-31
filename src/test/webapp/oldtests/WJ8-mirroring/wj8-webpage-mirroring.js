Feature('wj8-webpage-mirroring');

var auto_folder_sk, auto_folder_en, auto_subfolder1_sk, auto_subfolder2_sk, elfinder_file, randomNumber;
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
          elfinder_file = 'test-mir-elfinderFile-sk-' + randomNumber;
     }
     I.resizeWindow(1600, 1200);
});

function wj8CreateMirroringWebpage(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_webpage1_sk = 'sk-webpage1-autotest-' + randomNumber;
     var auto_webpage2_sk = 'sk-webpage2-autotest-' + randomNumber;
     // vytvorenie 1. webstranky v sk adresari cez hornu listu
     I.say('Vytvaram 1. webstranku v sk adresari cez hornu listu');
     I.waitForVisible('#treeScrollDiv', 10);
     I.wait(3);
     I.waitForVisible(locate('.jstree-anchor').withText(auto_folder_sk), 5);
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForLoader(".WJLoaderDiv");
     I.waitForVisible(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_folder_sk), 5);
     I.click('.wjIconBig.wjIconBig-uploadDoc');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(2);
     I.waitForVisible('#tabLink1', 15);
     I.wait(2);
     I.clearField('#docTitleWrapper>input');
     I.fillField('#docTitleWrapper>input', auto_webpage1_sk);
     I.wait(1);
     I.click('#btn-publish');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(1);
     I.waitForVisible('#treeScrollDiv', 10);
     I.waitForElement(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage1_sk), 10);
     // skontrolujem ci sa vytvorila i v en adresari - nepublikovana
     I.say('Kontrolujem ci sa webstranka1 vytvorila i v en strukture ako nepublikovana');
     I.waitForElement(locate('.jstree-node.jstree-open').withDescendant('.jstree-anchor>i.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-danger.jstree-themeicon-custom').withText(auto_webpage1_sk), 5);
     // vytvorenie 2 webstranky v sk adresari cez kontextove okno
     I.say('Vytvaram 2. webstranku v sk adresari cez kontextove menu');
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.rightClick(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForVisible('.vakata-context.jstree-contextmenu.jstree-default-contextmenu', 5);
     I.click('.glyphicon.glyphicon-file');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(2);
     I.waitForVisible('#tabLink1', 10);
     I.wait(2);
     I.clearField('#docTitleWrapper>input');
     I.fillField('#docTitleWrapper>input', auto_webpage2_sk);
     I.wait(1);
     I.click('#btn-publish');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(1);
     I.waitForVisible('#treeScrollDiv', 10);
     I.waitForElement(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage2_sk), 5);
     // skontrolujem ci sa vytvorila i v en adresari - nepublikovana
     I.say('Kontrolujem ci sa webstranka2 vytvorila i v en strukture ako nepublikovana');
     I.waitForElement(locate('.jstree-node.jstree-open').withDescendant('.jstree-anchor>i.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-danger.jstree-themeicon-custom').withText(auto_webpage2_sk), 5);
}

function wj8CreateMirrorSubfolder(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
     var auto_subfolder2_sk = 'sk-mir-subfolder2-' + randomNumber;
     // vytvorenie 1. podpriecinka do sk adresara
     I.say('Vytvaram prvy podpriecinok pre sk adresar');
     I.waitForVisible('#treeScrollDiv', 10);
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
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

function wj8DeleteSubpage(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_webpage2_sk = 'sk-webpage2-autotest-' + randomNumber;
     // vymazanie podstranky sk-webpage2
     I.say('Vymazavam druhu podstranku v sk strukture');
     I.waitForVisible('#treeScrollDiv', 10);
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForVisible(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage2_sk), 10);
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage2_sk));
     I.rightClick(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage2_sk));
     I.waitForVisible('.vakata-context.jstree-contextmenu.jstree-default-contextmenu', 5);
     I.click('.glyphicon.glyphicon-remove');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(2);
     // overim ci sa stranka vymazala zo sk adresara
     I.say('Overujem ci sa stranka vymazala z sk adresara');
     I.waitForInvisible(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage2_sk), 10);
     I.dontSeeElement(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage2_sk));
     I.wait(1);
     // overim ci sa stranka vymazala aj v en adresari
     I.say('Overujem ci sa stranka vymazala aj v en adresari');
     I.dontSeeElement(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-danger.jstree-themeicon-custom').withText(auto_webpage2_sk)); // TODO - v en adresari nevymaze webstranku2
}

function wj8MoveSubpage(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_webpage1_sk = 'sk-webpage1-autotest-' + randomNumber;
     var auto_subfolder1_sk = 'sk-mir-subfolder1-' + randomNumber;
     //otvor SK adresar
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     // presuvam stranku webpage1 do adresara
     I.say('Presuvam webpage1 do podpriecinka sk-mir-subfolder1');
     I.waitForVisible(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage1_sk), 10);
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage1_sk));
     I.wait(1);
     I.waitForVisible(locate('.nav.nav-tabs.nav-tabs-corner').withText('Základné'), 15);
     I.click(locate('#tabLink2').withText('Základné'));
     I.waitForVisible('.control-label.block', 10);
     I.click('Zmeň');
     I.switchToNextTab();
     I.waitForElement('.webfx-tree-item', 15);
     I.click(locate('.webfx-tree-item.folder').withText(auto_folder_sk).find({ css: 'img:first-child' }));
     I.waitForVisible(locate('.webfx-tree-item.folder').withText(auto_subfolder1_sk), 5);
     I.click(locate('.webfx-tree-item.folder').withText(auto_subfolder1_sk).find('a'));
     I.wait(1);
     I.switchToNextTab();
     I.waitForVisible(locate('#tabLink2').withText('Základné'), 10);
     I.click(locate('#tabLink2').withText('Základné'));
     I.click('#btn-publish');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(1);
     I.waitForVisible('#treeScrollDiv', 10);
     // overim ci sa stranka webpage1 presunula do priecinka sk-mir-subfolder1
     I.say('Overim ci sa stranka webpage1 presunula do priecinka sk-mir-subfolder1');
     I.waitForInvisible(locate('.jstree-node.jstree-leaf.jstree-last').withDescendant('.jstree-anchor>i.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage1_sk), 10);
     I.dontSeeElement(locate('.jstree-node.jstree-leaf.jstree-last').withDescendant('.jstree-anchor>i.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage1_sk));
     I.click(locate('.jstree-anchor').withText(auto_subfolder1_sk));
     I.waitForVisible('.dt-scroll-body', 15);
     I.waitForElement(locate('tr.odd').withText(auto_webpage1_sk), 15);
     // overim ci sa stranka webpage1 premiestnila i do en struktury
     I.say('Overim ci sa stranka webpage1 premiestnila i do en podpriecinka subfolder1');
     I.click(locate('.jstree-anchor').withText(auto_folder_en));
     I.dontSeeElement(locate('.jstree-node.jstree-leaf.jstree-last').withDescendant('.jstree-anchor>i.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-danger.jstree-themeicon-custom').withText(auto_webpage1_sk));
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.wj-folder-hidden.jstree-themeicon-custom').withText(auto_subfolder1_sk));
     I.waitForVisible('.dt-scroll-body', 15);
     I.waitForElement(locate('tr.odd').withText(auto_webpage1_sk), 5); // TODO - webstranka sa automaticky nepremiestni do en subadresara subfolder1
     I.wait(1);
}

function wj8ChangeMainPage(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var auto_webpage1_sk = 'sk-webpage1-autotest-' + randomNumber;
     // zmena hlavnej stranky v sk adresari
     I.say('Menim hlavnu stranku v sk adresari');
     I.waitForVisible('#treeScrollDiv', 10);
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForVisible(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage1_sk), 10);
     I.click(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage1_sk));
     I.rightClick(locate('.jstree-anchor').withChild('.jstree-icon.jstree-themeicon.fa.fa-file.icon-state-success.jstree-themeicon-custom').withText(auto_webpage1_sk));
     I.waitForVisible('.vakata-context.jstree-contextmenu.jstree-default-contextmenu', 5);
     I.click('.glyphicon.glyphicon-certificate');
     I.click('#btn-publish');
     I.waitForLoader(".WJLoaderDiv");
     I.wait(1);
     I.waitForVisible('#treeScrollDiv', 10);
     // over ci sa v hlavnom sk adresari zmenila hlavna stranka na webpage1
     I.say('Over ci sa v hlavnom sk adresari zmenila hlavna stranka na webpage1');
     I.click(locate('.jstree-anchor').withText(auto_folder_sk));
     I.waitForVisible('.dt-scroll-body', 15);
     I.seeElement(locate('tr.even.is-default-doc').withText(auto_webpage1_sk));
     I.wait(1);
     // over ci sa v en adresari zmenila hlavna stranka na sk-webpage1
     I.say('Over ci sa v hlavnom en adresari zmenila hlavna stranka na webpage1');
     I.click(locate('.jstree-anchor').withText(auto_folder_en));
     I.waitForVisible('.dt-scroll-body', 15);
     I.seeElement(locate('tr.even.is-default-doc').withText(auto_webpage1_sk)); // TODO hlavna stranka v en adresari sa nezmenila
}

function wj8elFinderPage(I, randomNumber) {
     var auto_folder_sk = 'sk-mir-autotest-' + randomNumber;
     var auto_folder_en = 'en-mir-autotest-' + randomNumber;
     var elfinder_file = 'test-mir-elfinderFile-sk-' + randomNumber;
     // pridaj file do hlavnej stranky podpriecinka subfolder1 v sk strukture
     I.say('Pridavam file cez elFinder do prveho podpriecinkav sk strukture');
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
     I.click(locate('.elfinder-contextmenu-item').withText('Nový textový súbor'));
     I.waitForLoader(".WJLoaderDiv");
     I.wait(1);
     I.type(elfinder_file);
     I.wait(1);
     I.pressKey('Enter');
     I.waitForText(elfinder_file, 10);
     // over ci sa tento file pridal aj do en struktury
     I.say('Over ci sa tento file pridal aj do en struktury');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_en));
     I.waitForElement(locate('.elfinder-cwd-filename').withText(elfinder_file));
     I.wait(1);
     // Skopiruj tento novy file v sk adresari
     I.say('Skopiruj tento novy file v sk adresari');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_sk));
     I.waitForElement('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.click(locate('div.elfinder-cwd-file.ui-corner-all.ui-selectee').withText(elfinder_file));
     I.wait(1);
     I.rightClick('.elfinder-cwd-file-wrapper.ui-corner-all.ui-draggable-handle.ui-state-hover');
     I.waitForVisible('.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle');
     I.click(locate('.elfinder-contextmenu-item').withText('Kopírovať'));
     I.waitForLoader(".WJLoaderDiv");
     I.wait(2);
     I.click('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.rightClick('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');

     I.click(locate('.elfinder-contextmenu-item').withText('Vložiť'));
     I.waitForLoader(".WJLoaderDiv");
     I.wait(2);
     I.click('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-filename').withText(elfinder_file), 2);
     I.wait(1);
     // over ci sa 2 rovnake elfinder adresare aj v en strukture
     I.say('Over ci sa 2 rovnake elfinder adresare aj v en strukture');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_en));
     I.wait(1);
     I.waitForVisible('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-filename').withText(elfinder_file), 2);
     I.wait(1);
     // duplikuj file v sk adresari
     I.say('Duplikuj file v sk adresari');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_sk));
     I.waitForElement('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.click(locate('.elfinder-cwd-filename').withText(elfinder_file));
     I.rightClick('.elfinder-cwd-file-wrapper.ui-corner-all.ui-draggable-handle.ui-state-hover');
     I.waitForVisible('.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle');
     I.click(locate('.elfinder-contextmenu-item').withText('Duplikovať'));
     I.waitForLoader(".WJLoaderDiv");
     I.wait(2);

     I.click('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-filename').withText(elfinder_file), 3);
     I.wait(1);
     // over ci sa file duplikoval aj v en strukture
     I.say('Over ci sa file duplikoval aj v en strukture');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_en));
     I.wait(1);
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-filename').withText(elfinder_file), 3);
     I.wait(1);
     // vymaz file zo sk adresara
     I.say('Vymaz file zo sk adresara');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_sk));
     I.waitForElement('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.click('.ui-helper-clearfix.elfinder-cwd.ui-selectable.elfinder-cwd-view-icons');
     I.wait(1);
     I.click(locate('.elfinder-cwd-filename').withText(elfinder_file).at(2));
     I.rightClick('.elfinder-cwd-file-wrapper.ui-corner-all.ui-draggable-handle.ui-state-hover');
     I.waitForVisible('.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle');
     I.click(locate('.elfinder-contextmenu-item').withText('Vymazať'));
     I.waitForVisible('.ui-dialog.ui-widget.ui-widget-content.ui-corner-all.ui-draggable.std42-dialog.touch-punch.elfinder-dialog.elfinder-dialog-confirm.elfinder-dialog-active.ui-front');
     I.click(locate('button').withText('Vymazať'));
     I.waitForLoader(".WJLoaderDiv");
     I.wait(3); //TODO cakaj na loader
     //toto nefungovalo, hlasilo to ze tam je 1 aj ked po pause tam uz nebol I.seeNumberOfVisibleElements(locate('.elfinder-cwd-file.ui-corner-all.ui-state-disabled.ui-selectee').withText(elfinder_file), 0);
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee').withText(elfinder_file), 2);
     I.wait(1);
     // skontroluj ci sa file vymazal aj v en adresari
     I.say('Skontroluj ci sa file vymazal aj v en adresari');
     I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-tree-dir.elfinder-navbar-collapsed.native-droppable.ui-droppable').withText(auto_folder_en));
     //toto nefungovalo, hlasilo to ze tam je 1 aj ked po pause tam uz nebol I.seeNumberOfVisibleElements(locate('..elfinder-cwd-file.ui-corner-all.ui-state-disabled.ui-selectee').withText(elfinder_file), 0);
     I.seeNumberOfVisibleElements(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee').withText(elfinder_file), 2);
     I.wait(1);
}

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

Scenario('Zrkadlenie webstranok', async ({ I }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');

     // vytvorenie mirroring hlavneho priecinka sk a en
     I.wj8CreateMirroringStructure(randomNumber);

     /// skopirovanie groupID
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

Scenario('Vytvorenie struktury stranok', ({ I }) => {

     // vytvorenie 2 webstranok webpage1 a webpage2
     wj8CreateMirroringWebpage(I, randomNumber);
     I.wait(1);

     // vytvorenie podpriecinkov subfolder1 a subfolder2
     wj8CreateMirrorSubfolder(I, randomNumber);
     I.wait(1);

});

Scenario('Vymazanie podadresarov', ({ I }) => {

     // vymazanie druhej podstranky sk-webpage2
     wj8DeleteSubpage(I, randomNumber);
     I.wait(1);

});

Scenario('Zmena hlavnej stranky', ({ I }) => {

     // zmena hlavnej stranky sk hlavneho adresara
     wj8ChangeMainPage(I, randomNumber);
     I.wait(1);

});

Scenario('Presun webpage1 do podpriecinka subfolder1', ({ I }) => {

     // presun stranky webpage1 do podpriecinka sk-mir-subfolder1
     wj8MoveSubpage(I, randomNumber);
     I.wait(1);

});

Scenario('elfinder test', ({ I }) => {
     // elfinder - textova stranka
     wj8elFinderPage(I, randomNumber);

});

Scenario('vymazanie priecinkov', ({ I }) => {
     I.amOnPage('/admin/webpages/?groupid=1');

     // vymazanie hlavneho sk priecinka z test priecinka
     wj8DeleteMainFolder(I, randomNumber);
     I.wait(1);
});

Scenario('odhlasenie', ({ I }) => {
     I.amOnPage('/logoff.do?forward=/admin/');
});