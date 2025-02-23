Feature('admin.elFinder');

Before(({ I, login }) => {
    login('admin');
});

function getFileUsage(I, DT, loader) {

    I.clickCss("#pills-dt-datatableInit-usage-tab");

    I.waitForText("Použitie súboru", 300);

    DT.waitForLoader(loader);

    I.see("Použitie súboru");

    I.see("Test file usage");

    I.switchTo();
}

Scenario('dir usage', async ({I, DT}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_2_L2ltYWdlcw_E_E");

    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5');

    I.click(locate('#finder .elfinder-contextmenu-item').withText("Nastavenie priečinka"));
    I.waitForVisible("#modalIframe", 10);

    I.switchTo("#modalIframeIframeElement");

    I.see("Indexovať súbory pre vyhľadávanie");
    I.see("Obchodní partneri");

    getFileUsage(I, DT, "#datatableFieldDTE_Field_editorFields-docDetailsList_processing");
});

Scenario('file usage', async ({I, DT}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_2_L2ltYWdlcy9iYW5uZXJ5");

    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E');

    I.click(locate('#finder .elfinder-contextmenu-item').withText("Nastavenie súboru"));
    I.waitForVisible("#modalIframe", 10);

    //I.switchTo("#modalIframe");
    I.switchTo("#modalIframeIframeElement");

    I.see("Názov súboru");
    I.see("Zmeniť názov vo všetkých súboroch");

    getFileUsage(I, DT, "#datatableFieldDTE_Field_docDetailsList_processing");
})

Scenario('search files', ({I}) => {

    //all media/images/apps/
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_L2ltYWdlcy9hcHBz");
    //atributy-stranky
    I.waitForText("atributy-stranky", 10, "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");

    //
    I.say("Searching in current directory");
    I.fillField("div.elfinder-button-search input", "monitor");
    I.waitForElement("div.elfinder-button-search-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwd]");
    I.waitForLoader(".WJLoaderDiv");
    //apps-monitor.jpg - there ist &zeroWidthSpace; in the name
    I.see("apps-monitor", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2FwcHMtbW9uaXRvci5qcGc_E");
    //atributy-stranky
    I.dontSee("atributy-stranky", "div.elfinder-cwd-wrapper");
    I.dontSeeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");

    //
    I.say("Searching in current directory and subdirectories");
    I.fillField("div.elfinder-button-search input", "monitor");
    I.waitForElement("div.elfinder-button-search-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwdRecursive]");
    I.waitForLoader(".WJLoaderDiv");
    //apps-monitor.jpg
    I.see("apps-monitor", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2FwcHMtbW9uaXRvci5qcGc_E");
    //monitor
    I.see("monitor", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3kvbW9uaXRvcnkvYXBwbGUtNWsvbW9uaXRvci5qcGc_E");

    //atributy-stranky
    I.dontSee("atributy-stranky", "div.elfinder-cwd-wrapper");
    I.dontSeeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");

})

Scenario('search webpages', ({I}) => {

    //webpages/Aplikacie/
    I.amOnPage("/admin/elFinder/#elf_iwcm_doc_group_volume_L2dyb3VwOjE1MjU3");

    //atributy-stranky
    I.waitForText("Atribúty stránky", 10, "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2dyb3VwOjQ5MzM0");

    //
    I.say("Searching in current directory");
    I.fillField("div.elfinder-button-search input", "galer");
    I.waitForElement("div.elfinder-button-search-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwd]");
    I.waitForLoader(".WJLoaderDiv");
    //Galery
    I.see("Galery", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2RvYzo1OTA3MQ_E_E");
    //Atribúty stránky
    I.dontSee("Atribúty stránky", "div.elfinder-cwd-wrapper");
    I.dontSeeElement("#iwcm_doc_group_volume_L2dyb3VwOjQ5MzM0");

    //
    I.say("Searching in current directory and subdirectories");
    I.fillField("div.elfinder-button-search input", "galer");
    I.waitForElement("div.elfinder-button-search-menu", 10);
    I.wait(0.5);
    I.clickCss("label[for=elfinder-finderSearchFromCwdRecursive]");
    I.waitForLoader(".WJLoaderDiv");
    //Galery
    I.see("Galery", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2RvYzo1OTA3MQ_E_E");
    //Galeria
    I.see("Galéria", "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2RvYzo0NTkyNg_E_E");
    //Atribúty stránky
    I.dontSee("Atribúty stránky", "div.elfinder-cwd-wrapper");
    I.dontSeeElement("#iwcm_doc_group_volume_L2dyb3VwOjQ5MzM0");
})

Scenario('remember last elfinder dir in webpages', ({I, DTE, Browser}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=16");
    DTE.waitForEditor();
    I.wait(3);

    I.clickCss('#trEditor');
    if (Browser.isFirefox()) I.wait(2);
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    //
    I.say("Opening image dialog and navigating to test folder");

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), 20);
    I.wait(1);
    I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'), null, { position: { x: 20, y: 5 } });
    I.waitForText('Foto galéria', 10, ".elfinder-cwd-file");
    I.waitForVisible(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('Foto galéria'), 10);
    I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('Foto galéria'));
    I.waitForVisible(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'), 20);
    I.wait(1);
    I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'));
    I.waitForVisible("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L3Rlc3QvZGVzZXJ0LmpwZw_E_E", 10); //test/desert.jpg
    I.waitForVisible(locate('.elfinder-cwd-file.ui-corner-all.ui-selectee'), 10);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    //
    I.say("Checking if test folder is selected after reopening image dialog");

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement(locate("div.elfinder-navbar-wrapper span.elfinder-navbar-dir.ui-state-active").withText("test"), 10);
    I.seeElement("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L3Rlc3Qva29hbGEuanBn"); //koala.jpg

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    DTE.cancel();

    //
    I.say("Opening another page, image dialog should be in Media tejto stranky folder");

    I.click("Trhy sú naďalej vydesené");
    DTE.waitForEditor();

    I.click(locate('#trEditor'), null, { position: { x: 50, y: 250 } });
    if (Browser.isFirefox()) I.wait(2);
    I.click(locate('#trEditor'), null, { position: { x: 50, y: 250 } });
    I.pressKey('ArrowDown');

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement("#nav-iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS90cmh5LXN1LW5hZGFsZWotdnlkZXNlbmU_E.ui-state-active", 10);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    DTE.cancel();
})

Scenario('folder for new page by title', ({I, DTE, Browser}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=24");

    I.clickCss("#datatableInit_wrapper div.dt-buttons button.buttons-create");
    DTE.waitForEditor();
    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
    DTE.fillField("title", "Utorok 02");
    I.clickCss("#pills-dt-datatableInit-content-tab");

    //
    I.say("Open image dialog and check if it is in the right folder");

    I.clickCss('#trEditor');
    if (Browser.isFirefox()) I.wait(2);
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement("#iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS91dG9yb2stMDIvc2F2ZS13YXJuaW5nLnBuZw_E_E", 10); ///images/zo-sveta-financii/utorok-02/save-warning.png

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    //
    I.say("Change title and check it's reloaded");

    I.clickCss("#pills-dt-datatableInit-basic-tab");
    DTE.fillField("title", "Pondelok");
    DTE.fillField("navbar", "Pondelok");
    I.clickCss("#pills-dt-datatableInit-content-tab");

    I.clickCss('#trEditor');
    if (Browser.isFirefox()) I.wait(2);
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement("#iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS9wb25kZWxvay9tYXhyZXNkZWZhdWx0LmpwZw_E_E", 10); ///images/zo-sveta-financii/pondelok/maxresdefault.jpg

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    DTE.cancel();
})

Scenario('CVE-2022-26960', async ({I}) => {
    I.amOnPage("/admin/v9/");
    ///files/protected/bankari/test-forward.txt
    await I.executeScript(function(){
        window.location.href="/admin/elfinder-connector/?cmd=file&volumes=files&target=iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9iYW5rYXJpL3Rlc3QtZm9yd2FyZC50eHQ_E";
    });
    I.waitForText("This is test file for fileforward after logon", 10);

    I.amOnPage("/admin/v9/apps/audit-search/");
    await I.executeScript(function(){
        // ../../../../../../../../../../../../../../www/tomcat_au27/conf/server.xml
        window.location.href="/admin/elfinder-connector/?cmd=file&volumes=files&target=iwcm_1_Li4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vLi4vd3d3L3RvbWNhdF9hdTI3L2NvbmYvc2VydmVyLnhtbA==";
    });
    I.dontSee("This is test file for fileforward after logon");
    I.dontSee("Server");

    I.amOnPage("/admin/v9/");
    ///files/../protected/bankari/test-forward.txt
    await I.executeScript(function(){
        window.location.href="/admin/elfinder-connector/?cmd=file&volumes=files&target=iwcm_1_L2ZpbGVzLy4uL3Byb3RlY3RlZC9iYW5rYXJpL3Rlc3QtZm9yd2FyZC50eHQ=";
    });
    I.dontSee("This is test file for fileforward after logon");
})

Scenario('Explorer - file diacritics test', async ({ I }) => {
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVz');

    I.say('Uploading file with diacritics in name');
    I.click('.elfinder-navbar-wrapper span[id^="nav-iwcm_2_"][title*="files"] .elfinder-navbar-icon');
    I.click('.elfinder-button-icon.elfinder-button-icon-upload');
    I.attachFile('input[type=file]', 'tests/admin/ľščťú žýáíéô.png');
    waitForUpload(I);

    I.say('Checking if file was automatically renamed to the name without diacritics');
    checkCorrectTitle(I);

    I.say('Try to rename a file to name with diacritics');
    searchFile(I, 'lsctu-zyaieo.png');
    within('.elfinder-cwd-wrapper', () => {
        I.waitForText('lsctu-zyaieo.png', 10);
        I.rightClick('lsctu-zyaieo.png');
    });
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rename');

    I.fillField({ css: '.elfinder-cwd-filename[title="lsctu-zyaieo.png"] textarea' }, 'ľščťú žýáíéô');
    I.pressKey('Enter');
    I.clickCss('.elfinder-button-search > .ui-icon-close');
    I.say('Checking if file was automatically renamed to the name without diacritics');
    checkCorrectTitle(I);
    await deleteTestFiles(I);

    I.amOnPage("/admin/v9/");
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVz');
    I.click("#nav-iwcm_2_L2ZpbGVz"); //files
    createFolder(I, 'ľščťú žýáíéô');

    I.say('Try to rename a folder to the name with diacritics');
    I.rightClick(locate(".elfinder-cwd-file").withText("lsctu-zyaieo"));

    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rename');

    I.fillField({ css: '.elfinder-cwd-filename[title="lsctu-zyaieo"] textarea' }, 'ľščťú žýáíéô');
    I.pressKey('Enter');
    I.clickCss('.elfinder-button-search > .ui-icon-close');
    checkCorrectTitle(I);
})

Scenario('Explorer - file diacritics delete', async ({ I }) => {
    await deleteTestFiles(I);
})

Scenario('Elfinder - Move Confirm with config value true', async ({ I, Document }) => {
    Document.setConfigValue('elfinderMoveConfirm', 'true');
    await deleteTestFiles(I);
    await checkMoveConfirm(I, true);
})

Scenario('Elfinder - Move Confirm with config value false', async ({ I, Document }) => {
    Document.setConfigValue('elfinderMoveConfirm', 'false');
    await deleteTestFiles(I);
    await checkMoveConfirm(I, false);
})

Scenario('revert changes - Move Confirm with config value false', async ({ I, Document }) => {
    await deleteTestFiles(I);
    Document.setConfigValue('elfinderMoveConfirm', 'true');
})

function searchFile(I, fileName) {
    I.clickCss('.elfinder-button-search > input[type="text"]');
    I.type(fileName);
    I.clickCss('label[for = elfinder-finderSearchFromCwd]');
}

async function checkMoveConfirm(I, elfinderMoveConfirm) {
    //to be sure link is working with hash
    I.amOnPage('/admin/v9/');
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E');
    I.clickCss('#nav-iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E');

    I.say('Testing with drag and drop.');
    createFolder(I, 'outer');
    createFolder(I, 'inner');

    I.dragAndDrop(getFileSelector('inner'), getFileSelector('outer'), { force: true });

    handleConfirmationDialog(elfinderMoveConfirm, I);

    I.seeElement(getFileSelector('outer'));
    I.waitForInvisible(getFileSelector('inner'));
    I.dontSeeElement(getFileSelector('inner'));

    await deleteFile(I, 'outer');

    I.say('Testing with context menu');

    I.clickCss('#nav-iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E');
    createFolder(I, 'outer');
    createFolder(I, 'inner');

    I.rightClick(getFileSelector('inner'));
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-button-icon.elfinder-button-icon-cut.elfinder-contextmenu-icon');

    I.rightClick(getFileSelector('outer'));
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-button-icon.elfinder-button-icon-paste.elfinder-contextmenu-icon');

    handleConfirmationDialog(elfinderMoveConfirm, I);

    I.seeElement(getFileSelector('outer'));
    I.waitForInvisible(getFileSelector('inner'));
    I.dontSeeElement(getFileSelector('inner'));
}

function handleConfirmationDialog(elfinderMoveConfirm, I) {
    if (elfinderMoveConfirm) {
        I.waitForVisible('div.ui-dialog.ui-widget.elfinder-dialog-confirm', 10);
        I.see('Naozaj chcete presunúť položky do');
        I.clickCss('button.elfinder-confirm-accept');
    }
    else {
        I.dontSeeElement('div.ui-dialog.ui-widget.elfinder-dialog-confirm');
        I.dontSee('Naozaj chcete presunúť položky do');
        I.waitForVisible('.ui-front.toast-success.elfinder-frontmost', 10);
        I.wait(2);
    }
}

function createFolder(I, folderName) {
    I.say('Creating a folder');
    I.click('.elfinder-button-icon.elfinder-button-icon-mkdir');
    I.fillField({ css: '.elfinder-cwd-filename[title="Nový priečinok"] textarea' }, folderName);
    I.pressKey('Enter');
    I.waitForVisible('.elfinder-toast', 10);
    I.wait(2);
}

function waitForUpload(I) {
    I.clickIfVisible('.elfinder-confirm-accept');
    I.waitForVisible('.elfinder-notify-chunkmerge', 10);
    I.waitForInvisible('.elfinder-notify-upload', 10);
    I.waitForInvisible('.elfinder-notify-chunkmerge', 10);
}

function checkCorrectTitle(I) {
    I.waitForText('lsctu-zyaieo', 10, '.elfinder-cwd-filename');
    I.dontSee('ľščťú-žýáíéô', '.elfinder-cwd-filename');
    I.dontSee('ľščťú žýáíéô', '.elfinder-cwd-filename');
    I.dontSee('lsctu zyaieo', '.elfinder-cwd-filename');
}

async function deleteTestFiles(I) {
    I.say('Deleting uploaded files');
    I.amOnPage("/admin/v9/");
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVz');
    let fileNames = ['lsctu-zyaieo.png', 'ľščťú-žýáíéô.png', 'ľščťú žýáíéô.png', 'lsctu zyaieo.png',
        'lsctu-zyaieo', 'ľščťú-žýáíéô', 'ľščťú žýáíéô', 'lsctu zyaieo'];
    for (const fileName of fileNames) {
        await deleteFile(I, fileName);
    }

    //otherwise hash change will not work
    I.amOnPage("/admin/v9/");
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E");
    fileNames = ['outer', 'inner'];
    for (const fileName of fileNames) {
        await deleteFile(I, fileName);
    }
}

function getFileSelector(fileName) {
    return `.elfinder-cwd-filename[title="${fileName}"]`;
}

async function deleteFile(I, fileName) {
    const numVisible = await I.grabNumberOfVisibleElements(`.elfinder-cwd-filename[title="${fileName}"]`);
    if (numVisible) {
        I.rightClick(fileName, ".elfinder-cwd-wrapper");
        I.waitForVisible('.elfinder-contextmenu', 10);
        I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rm');
        I.clickCss('.elfinder-confirm-accept');
        I.waitForInvisible(fileName);
    }
}
