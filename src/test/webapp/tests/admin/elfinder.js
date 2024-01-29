Feature('admin.elFinder');

Before(({ I, login }) => {
    login('admin');
});

function getFileUsage(I) {

    I.switchTo();

    I.selectOption('#elfinder-modal > div > div > div.modal-footer > select', 'Použitie');

    I.clickCss("#elfinder-modal > div > div > div.modal-footer > button.btn.btn-primary.submit");

    I.switchTo("#elfinderIframe");

    I.waitForText("Použitie súboru:", 300);

    I.see("Použitie súboru:");

    I.see("Test file usage");

    I.switchTo();
}

Scenario('dir usage', async ({I}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_2_L2ltYWdlcw_E_E");

    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5');

    I.clickCss('#finder > div.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle > div:nth-child(16)');

    I.switchTo("#elfinderIframe");

    I.see("Indexovať súbory pre vyhľadávanie");
    I.see("Obchodní partneri");


    getFileUsage(I);
});

Scenario('file usage', async ({I}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_2_L2ltYWdlcy9iYW5uZXJ5");

    I.rightClick('#iwcm_2_L2ltYWdlcy9iYW5uZXJ5L2Jhbm5lci1pd2F5ZGF5LnBuZw_E_E');

    I.clickCss('#finder > div.touch-punch.ui-helper-reset.ui-front.ui-widget.ui-state-default.ui-corner-all.elfinder-contextmenu.elfinder-contextmenu-ltr.ui-draggable.ui-draggable-handle > div:nth-child(20)');

    I.switchTo("#elfinderIframe");

    I.see("Názov súboru");
    I.see("Zmeniť názov vo všetkých súboroch");

    getFileUsage(I);
});

Scenario('search files', ({I}) => {

    //all media/images/apps/
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_L2ltYWdlcy9hcHBz");
    //atributy-stranky
    I.waitForText("atributy-stranky", 10, "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");

    //
    I.say("Searching in current directory");
    I.fillField("div.elfinder-button-search input", "monitor");
    I.waitForElement("div.elfinder-button-search div.elfinder-button-menu", 10);
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
    I.waitForElement("div.elfinder-button-search div.elfinder-button-menu", 10);
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

});

Scenario('search webpages', ({I}) => {

    //webpages/Aplikacie/
    I.amOnPage("/admin/elFinder/#elf_iwcm_doc_group_volume_L2dyb3VwOjE1MjU3");

    //atributy-stranky
    I.waitForText("Atribúty stránky", 10, "div.elfinder-cwd-wrapper");
    I.seeElement("#iwcm_doc_group_volume_L2dyb3VwOjQ5MzM0");

    //
    I.say("Searching in current directory");
    I.fillField("div.elfinder-button-search input", "galer");
    I.waitForElement("div.elfinder-button-search div.elfinder-button-menu", 10);
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
    I.waitForElement("div.elfinder-button-search div.elfinder-button-menu", 10);
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
});

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
    I.click(locate('.ui-corner-all.elfinder-navbar-dir.elfinder-navbar-root.elfinder-tree-dir.elfinder-ro.elfinder-navbar-collapsed.ui-droppable.elfinder-subtree-loaded').withText('Médiá všetkých stránok'));
    I.waitForText('Foto galéria', 10, ".elfinder-cwd-file");
    I.wait(1);
    I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('Foto galéria'));
    I.waitForVisible(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'), 20);
    I.doubleClick(locate('.elfinder-cwd-file.directory.ui-corner-all.ui-droppable.native-droppable.ui-selectee').withText('test'));
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

    I.clickCss('#trEditor');
    if (Browser.isFirefox()) I.wait(2);
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    I.clickCss('.cke_button.cke_button__image.cke_button_off');
    I.waitForText('Vlastnosti obrázka', 20);
    I.switchTo('#wjImageIframeElement');
    I.waitForLoader(".WJLoaderDiv");

    I.waitForElement("#nav-iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS90cmh5LXN1LW5hZGFsZWotdnlkZXNlbmU_E.ui-state-active", 10);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    DTE.cancel();
});

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

    I.waitForElement("#iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS91dG9yb2stMDIvc2F2ZS13YXJuaW5nLnBuZw_E_E", 10); //utorok/save-varning.png

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

    I.waitForElement("#iwcm_fs_ap_volume_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaS9wb25kZWxvay9tYXhyZXNkZWZhdWx0LmpwZw_E_E", 10); //pondelok/maxresdefault.jpg

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");

    DTE.cancel();
});

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
});