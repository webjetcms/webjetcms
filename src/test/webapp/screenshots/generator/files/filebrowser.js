Feature('admin.filebrowser');

Before(({ I, login }) => {
    login('admin');
});

Scenario('filebrowser - toolbar + navbar screens', ({ I, DTE, Document, i18n }) => {
    I.amAcceptingPopups();

    I.amOnPage("/admin/v9/files/index/#elf_iwcm_1_L2ltYWdlcy9nYWxsZXJ5");
    I.executeScript(() => { if (elFinderInstance.viewType !== "icons") elFinderInstance.exec("view"); });
    I.waitForElement("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L2FwcHM_E");

    I.moveCursorTo(".header-title");
    Document.screenshot("/redactor/files/fbrowser/page.png");

    Document.screenshotElement("div.elfinder-navbar", "/redactor/files/fbrowser/navbar.png");

    I.clickCss(".elfinder-toolbar .elfinder-button-icon-sort");
    I.waitForElement("div.elfinder-button-menu.elfinder-button-sort-menu");
    Document.screenshotElement("div.elfinder-button-menu.elfinder-button-sort-menu", "/redactor/files/fbrowser/sort_menu.png");
    I.clickCss(".elfinder-toolbar .elfinder-button-icon-sort");

    I.resizeWindow(1280, 660);
    I.moveCursorTo(".header-title");
    Document.screenshotElement("div.elfinder-cwd-wrapper", "/redactor/files/fbrowser/page_sorted_A.png");
    I.clickCss("span.elfinder-button-icon-view-list");
    I.waitForVisible(".elfinder-cwd-view-list");
    Document.screenshotElement("div.elfinder-cwd-wrapper.elfinder-cwd-wrapper-list", "/redactor/files/fbrowser/page_sorted_B.png");
    I.clickCss('.elfinder-toolbar .elfinder-button-icon-view');

    I.resizeWindow(1280, 760);
    I.click( locate("a.nav-link").withText(i18n.get("Tools")) );
    I.moveCursorTo(".header-title");
    Document.screenshotElement(".elfinder-toolbar", "/redactor/files/fbrowser/toolbar_tools.png");

    I.clickCss("div#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L3Rlc3QtdmVsYS1mb3Rv");
    I.clickCss('.elfinder-toolbar .elfinder-button-icon-quicklook');

    I.waitForElement("div.ui-widget.elfinder-quicklook");
    I.waitForVisible(".elfinder-quicklook-info");
    Document.screenshotElement("div.ui-widget.elfinder-quicklook", "/redactor/files/fbrowser/quicklook_folder.png");
    I.click( locate("div.elfinder-quicklook").find("span.elfinder-icon-close") );
    I.clickCss("div#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L2NocnlzYW50aGVtdW0uanBn");
    I.waitForInvisible("div.ui-widget.elfinder-quicklook");

    I.clickCss('.elfinder-toolbar .elfinder-button-icon-quicklook');

    I.waitForElement( locate('div.elfinder-quicklook-preview > img') )
    Document.screenshotElement("div.ui-widget.elfinder-quicklook", "/redactor/files/fbrowser/quicklook_image.png");
    I.click( locate("div.elfinder-quicklook").find("span.elfinder-icon-close") );

    I.clickCss('.elfinder-toolbar .elfinder-button-icon-info');

    I.waitForElement("div.ui-dialog.elfinder-dialog-info");
    Document.screenshotElement("div.ui-dialog.elfinder-dialog-info", "/redactor/files/fbrowser/info_file.png");
    I.click( locate("div.ui-dialog.elfinder-dialog-info").find(".ui-dialog-titlebar-close"));

    I.clickCss("div#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L3Rlc3QtdmVsYS1mb3Rv");

    I.clickCss('.elfinder-toolbar .elfinder-button-icon-info');

    I.waitForElement("div.ui-dialog.elfinder-dialog-info");
    I.waitForElement(".elfinder-folder-size [role=status][title]", 30);
    I.moveCursorTo(".header-title");
    Document.screenshotElement("div.ui-dialog.elfinder-dialog-info", "/redactor/files/fbrowser/info_folder.png");
    I.click( locate("div.ui-dialog.elfinder-dialog-info").find(".ui-dialog-titlebar-close"));

    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2FwcHMvYWRtaW4_E');
    I.refreshPage();
    const textFile = locate('.elfinder-cwd-file').withDescendant(locate('.elfinder-cwd-filename').withTextEquals('editor-component.html'));
    I.waitForVisible(textFile, 15);
    I.click(textFile);
    I.click(locate('a.nav-link').withText(i18n.get('Tools')));

    I.clickCss('.elfinder-toolbar .elfinder-button-icon-quicklook');

    I.waitForElement("div.ui-widget.elfinder-quicklook");
    Document.screenshotElement("div.ui-widget.elfinder-quicklook", "/redactor/files/fbrowser/quicklook_file.png");
    I.click( locate("div.elfinder-quicklook").find("span.elfinder-icon-close") );

    I.click( locate("div.elfinder-button").withChild("span.elfinder-button-icon-wjeditswitch") );
    I.waitForElement("div.ui-dialog.elfinder-dialog-active");
    Document.screenshotElement("div.ui-dialog.elfinder-dialog-active", "/redactor/files/fbrowser/file-edit/edit_file.png");

    I.amOnPage("/components/sync/export_setup.jsp#");
    I.resizeWindow(800, 600);
    Document.screenshot("/redactor/files/fbrowser/import-export.png");

    I.resizeWindow(1280, 760);
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/&id=164#dt-open-editor=true&dt-filter-imageName=chrysanthemum.jpg&dt-filter-imagePath=^/images/gallery$");
    DTE.waitForEditor("galleryTable");
    I.clickCss("#pills-dt-galleryTable-metadata-tab");
    I.waitForVisible("#DTE_Field_imageName");
    Document.screenshot("/redactor/files/fbrowser/file-edit/edit_image_gallery.png");

    I.amOnPage("/admin/v9/files/index/#elf_iwcm_1_L2ltYWdlcy96by1zdmV0YS1maW5hbmNpaQ_E_E");
    I.waitForElement(`.elfinder-cwd-filename[title="foto-1.jpg"]`);
    I.rightClick(`.elfinder-cwd-filename[title="foto-1.jpg"]`);
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjeditswitch');

    I.waitForNumberOfTabs(2, 15);
    I.switchToNextTab();
    I.clickCss('#pills-dt-galleryTable-photoeditor-tab');
    I.waitForVisible("#galleryTable_modal");

    Document.screenshotElement("#galleryTable_modal", "/redactor/files/fbrowser/file-edit/edit_image_tui.png");
});

Scenario('filebrowser - search screens', ({ I, Document }) => {
    I.resizeWindow(1280, 760);
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_1_L2ltYWdlcy9hcHBzL2F0cmlidXR5LXN0cmFua3k_E");
    const search = '.elfinder-button-search input[name="wjsearch"]';
    I.waitForVisible(search, 15);
    I.fillField('.elfinder-tree-search input', 'bannery');
    I.pressKey('Enter');
    I.waitForVisible('.elfinder-tree-search[aria-busy="false"]', 45);
    I.moveCursorTo(".header-title");
    Document.screenshotElement('.elfinder-navbar', '/redactor/files/fbrowser/folder_search.png');
    I.clickCss('.elfinder-tree-search .btn-clear');

    I.clickCss(search);
    I.waitForVisible('.elfinder-button-search-menu', 10);
    Document.screenshotElement('.elfinder-button-search-menu', '/redactor/files/fbrowser/search.png');
    I.fillField(search, 'monitor.jpg');
    I.pressKey('Enter');
    I.waitForVisible('.elfinder-search-empty', 15);
    I.moveCursorTo(".header-title");
    Document.screenshot('/redactor/files/fbrowser/search_normal.png');
    I.clickCss('.elfinder-search-empty [data-action="subfolders"]');
    I.waitForText('monitor.jpg', 15, '.elfinder-cwd');
    I.moveCursorTo(".header-title");
    Document.screenshot('/redactor/files/fbrowser/search_recursive.png');
});

Scenario('filebrowser - workspace screens', ({ I, Document }) => {
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_1_");

    I.waitForElement("#iwcm_1_L2ZpbGVz");
    I.doubleClick( locate("#iwcm_1_L2ZpbGVz" ) );
    I.waitForElement("#iwcm_1_L2ZpbGVzL2p1cmtvLmpwZw_E_E"); //files/jurko.jpg

    I.rightClick( locate("div.elfinder-cwd-wrapper > div.elfinder-cwd") );
    I.waitForVisible("div.elfinder-contextmenu.elfinder-contextmenu-ltr");
    I.moveCursorTo(".header-title");
    Document.screenshotElement("div.elfinder-contextmenu.elfinder-contextmenu-ltr", "/redactor/files/fbrowser/rc_workspace.png");

    I.rightClick( locate("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZA_E_E") );  //files/protected
    I.moveCursorTo(".header-title");
    Document.screenshotElement("div.elfinder-contextmenu.elfinder-contextmenu-ltr", "/redactor/files/fbrowser/rc_folder.png");

    I.rightClick( locate("#iwcm_1_L2ZpbGVzL2p1cmtvLmpwZw_E_E") );
    I.moveCursorTo(".header-title");
    Document.screenshotElement("div.elfinder-contextmenu.elfinder-contextmenu-ltr", "/redactor/files/fbrowser/rc_file.png");
});

Scenario('filebrowser - folder properties screens', ({ I, DTE, Document, i18n }) => {
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_1_L2ZpbGVz");
    I.waitForVisible( locate("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZA_E_E") );
    I.rightClick( locate("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZA_E_E") );

    I.click( locate("div.elfinder-contextmenu.elfinder-contextmenu-ltr").find( locate("div.elfinder-contextmenu-item").withChild( locate("span").withText(i18n.get("Folder properties")) ) ) );
    I.waitForVisible("iframe#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.waitForVisible("#pills-dt-datatableInit-basic-tab");
    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/folder-settings/folder_settings_basic.png");

    I.switchTo();
    I.switchTo("#modalIframeIframeElement");
    I.clickCss("#pills-dt-datatableInit-index-tab");
    I.waitForVisible("button#start-index-button", 10);
    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/folder-settings/folder_settings_index.png");

    I.switchTo("#modalIframeIframeElement");
    I.clickCss("button#start-index-button");

    i18n.waitForText("File indexing completed");

    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/folder-settings/folder_settings_index_empty.png");

    I.switchTo("#modalIframeIframeElement");
    I.clickCss("#pills-dt-datatableInit-usage-tab");
    I.waitForInvisible("#datatableFieldDTE_Field_editorFields-docDetailsList_processing");
    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/folder-settings/folder_settings_usage.png");
    I.click( locate("#modalIframe").find("button.btn-close") );

    I.rightClick( locate("#iwcm_1_L2ZpbGVzL3Rlc3Qtc2l2YW4_E") );

    I.click( locate("div.elfinder-contextmenu.elfinder-contextmenu-ltr").find( locate("div.elfinder-contextmenu-item").withChild( locate("span").withText(i18n.get("Folder properties")) ) ) );
    I.waitForVisible("iframe#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.waitForElement("#pills-dt-datatableInit-index-tab", 10);
    I.clickCss("#pills-dt-datatableInit-index-tab");
    I.waitForVisible("button#start-index-button");
    I.clickCss("button#start-index-button");

    i18n.waitForText("File indexing completed");

    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/folder-settings/folder_settings_index_not-empty.png");
    I.click( locate("#modalIframe").find("button.btn-close") );
    I.waitForInvisible("iframe#modalIframeIframeElement");

    I.click( locate("span#nav-iwcm_1_L2ltYWdlcy9nYWxsZXJ5"), null, { position: { x: 20, y: 5 } });
    I.waitForElement("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L2FwcHM_E");
    I.rightClick( locate("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L2FwcHM_E") );

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=59071");
    DTE.waitForEditor();
    I.wait(5);
    Document.screenshot("/redactor/files/fbrowser/folder-settings/folder_link_A.png");

    I.amOnPage("/apps/galery.html");
    Document.screenshot("/redactor/files/fbrowser/folder-settings/folder_link_B.png");
});

Scenario('filebrowser - file properties screens', ({ I, DTE, Document, i18n }) => {
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_1_L2ZpbGVz");
    I.waitForVisible( locate("#iwcm_1_L2ZpbGVzL2p1cmtvLmpwZw_E_E") );
    I.rightClick( locate("#iwcm_1_L2ZpbGVzL2p1cmtvLmpwZw_E_E") );

    var text = i18n.get("File properties");

    I.click( locate("div.elfinder-contextmenu.elfinder-contextmenu-ltr").find( locate("div.elfinder-contextmenu-item").withChild( locate("span").withText(text) ) ) );
    I.waitForVisible("iframe#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.waitForVisible("#pills-dt-datatableInit-basic-tab");
    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/file-settings/file_settings_basic.png");

    I.switchTo("#modalIframeIframeElement");
    I.clickCss("#pills-dt-datatableInit-index-tab");
    I.waitForVisible("button#start-index-button");
    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/file-settings/file_settings_index.png");

    I.switchTo("#modalIframeIframeElement");
    I.clickCss("button#start-index-button");

    i18n.waitForText("File indexing completed");

    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/file-settings/file_settings_index_done.png");
    I.click( locate("#modalIframe").find("button.btn-close") );

    I.click( locate("span#nav-iwcm_1_L2ltYWdlcy9nYWxsZXJ5"), null, { position: { x: 20, y: 5 } });
    I.waitForVisible("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L2NocnlzYW50aGVtdW0uanBn");
    I.rightClick( locate("#iwcm_1_L2ltYWdlcy9nYWxsZXJ5L2NocnlzYW50aGVtdW0uanBn") );
    I.click( locate("div.elfinder-contextmenu.elfinder-contextmenu-ltr").find( locate("div.elfinder-contextmenu-item").withChild( locate("span").withText(text) ) ) );
    I.waitForVisible("iframe#modalIframeIframeElement");

    I.switchTo("#modalIframeIframeElement");
    I.waitForElement("#pills-dt-datatableInit-usage-tab", 10);
    I.clickCss("#pills-dt-datatableInit-usage-tab");
    I.waitForInvisible("#datatableFieldDTE_Field_docDetailsList_processing");
    I.switchTo();
    Document.screenshotElement("#modalIframe > div.modal-dialog", "/redactor/files/fbrowser/file-settings/file_settings_usage.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=60028");
    DTE.waitForEditor();
    I.wait(2);
    Document.screenshot("/redactor/files/fbrowser/file-settings/file_link_A.png");

    I.amOnPage("/apps/contentblock/");
    Document.screenshot("/redactor/files/fbrowser/file-settings/file_link_B.png");
});

Scenario('Elfinder Move Confirm', ({ I, Document, i18n }) => {
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E');
    I.clickCss('#nav-iwcm_2_L2ZpbGVzL3Rlc3QtbW92ZS1kaXI_E');
    createFolder(I, i18n,'outer');
    createFolder(I, i18n,'inner');

    const outerSelector = '.elfinder-cwd-filename[title="outer"]';
    const innerSelector = '.elfinder-cwd-filename[title="inner"]';
    I.dragAndDrop(innerSelector, outerSelector, { force: true });

    I.waitForVisible('div.ui-dialog.ui-widget.elfinder-dialog-confirm', 10);

    Document.screenshot('/redactor/files/fbrowser/move-confirm.png', 1200, 500);
    I.clickCss('button.elfinder-confirm-accept');
    deleteFile(I,'outer');
});

function createFolder(I, i18n, folderName) {
    I.say('Creating a folder');
    I.click('.elfinder-button-icon.elfinder-button-icon-mkdir');
    I.fillField({ css: `.elfinder-cwd-filename[title="${i18n.get('NewFolder')}"] textarea` }, folderName);
    I.pressKey('Enter');
    I.waitForVisible('.elfinder-toast', 10);
    I.waitForInvisible('.elfinder-toast', 10);
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
