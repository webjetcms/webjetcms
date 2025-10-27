Feature('files.file-archive');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('Base screens', ({ I, DT, DTE, Document, i18n }) => {
    const invalidPngFileName = 'archive_file_invalid_format.png';
    const validPdfFileName = 'screenshot_file.pdf';

    I.amOnPage('/apps/file-archive/admin/');

    Document.screenshot("/redactor/files/file-archive/datatable.png");

    DT.filterSelect("editorFields.statusIcons", i18n.get('All document versions'));
    Document.screenshot("/redactor/files/file-archive/datatable_allFiles.png");

    I.clickCss('#fileArchiveDataTable_wrapper .buttons-create');
    DTE.waitForEditor('fileArchiveDataTable');
    I.checkOption("#DTE_Field_editorFields-saveLater_0");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/files/file-archive/editor_base.png");

    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/files/file-archive/editor_advanced.png");

    I.clickCss("#pills-dt-fileArchiveDataTable-customFields-tab");
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/files/file-archive/editor_customFields.png");

    I.say("Invalid type file");
    I.clickCss("#pills-dt-fileArchiveDataTable-basic-tab");

    I.uncheckOption("#DTE_Field_editorFields-saveLater_0");

    //Attach file
    I.attachFile('input.dz-hidden-input', 'tests/apps/file-archive/docs/' + invalidPngFileName);
    DTE.save();
    Document.screenshotElement("div.DTE_Field_Type_wjupload", "/redactor/files/file-archive/invalid-file-type.png");

    I.say("Save good file - but duplicity");
    I.click( locate("div.upload-wrapper").find("button.btn-outline-secondary") );
    DTE.fillField('virtualFileName', "testScreenshot-CANT_SAVE");
    I.attachFile('input.dz-hidden-input', 'tests/apps/file-archive/docs/' + validPdfFileName);
    DTE.save();

    I.waitForVisible("div.toast.toast-warning");
    I.moveCursorTo("div.toast-container div.toast");
    Document.screenshotElement("div.toast-container div.toast", "/redactor/files/file-archive/file-duplicity-notif.png");
});

Scenario('Edit and actions screens', ({ I, DT, DTE, Document, i18n }) => {
    const mainBase = "ScreenshotFile_C";

    I.amOnPage('/apps/file-archive/admin/');
    DT.filterEquals('virtualFileName', mainBase);

    I.clickCss("button.buttons-select-all");
    Document.screenshotElement("button.buttons-rollback", "/redactor/files/file-archive/rollback_button.png");
    I.clickCss("button.buttons-rollback");
    I.waitForVisible("#toast-container-webjet");
    I.moveCursorTo("#toast-container-webjet > .toast-info");
    Document.screenshotElement("#toast-container-webjet > div.toast-info", "/redactor/files/file-archive/rollback_dialog.png");
    I.toastrClose();

    I.click(mainBase);
    DTE.waitForEditor('fileArchiveDataTable');
    I.resizeWindow(1280, 850);
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/files/file-archive/editor_edit_base.png");

    I.selectOption('select#DTE_Field_editorFields-uploadType', i18n.get("Replace current document"));
    I.resizeWindow(1280, 450);
    I.scrollTo("#DTE_Field_editorFields-file");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/files/file-archive/action_replace_file.png");

    I.selectOption('select#DTE_Field_editorFields-uploadType', i18n.get("Add to version history"));
    I.resizeWindow(1280, 550);
    I.scrollTo("#DTE_Field_editorFields-saveAfterSelect");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/files/file-archive/action_move_behind.png");

    I.click(locate(".DTE_Field_Name_editorFields\\.saveAfterSelect").find("button"));
    I.waitForVisible("div.dropdown-menu.show");
    Document.screenshotElement("div.dropdown-menu.show", "/redactor/files/file-archive/action_move_behind_options.png");
    I.click(locate(".DTE_Field_Name_editorFields\\.saveAfterSelect").find("button"));

    I.selectOption('select#DTE_Field_editorFields-uploadType', i18n.get("Upload a new version"));
    I.resizeWindow(1280, 500);
    I.scrollTo(".DTE_Field_Name_editorFields\\.saveLater");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/files/file-archive/action_new_version.png");

    I.clickCss("#DTE_Field_editorFields-saveLater_0");
    I.resizeWindow(1280, 650);
    I.scrollTo("#DTE_Field_editorFields-emails");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/files/file-archive/action_new_version_later.png");

    I.resizeWindow(1280, 500);
    I.clickCss("#pills-dt-fileArchiveDataTable-listOfVersions-tab");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/files/file-archive/file_archiv_tab_verisons.png");

    I.clickCss("#pills-dt-fileArchiveDataTable-waitingFiles-tab");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/files/file-archive/file_archiv_tab_waitingFiles.png");

    I.clickCss("#pills-dt-fileArchiveDataTable-listOfPattern-tab");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/files/file-archive/file_archiv_tab_patterns.png");
});

Scenario('Upload file field for DOC', ({ I, DTE, Document, i18n }) => {
    const validPdfFileName = 'archive_file_test.pdf';

    I.amOnPage('/apps/file-archive/admin/');
    I.clickCss('#fileArchiveDataTable_wrapper .buttons-create');
    DTE.waitForEditor('fileArchiveDataTable');

    Document.screenshotElement("div.DTE_Field_Type_wjupload", "/developer/datatables-editor/field-uploadFile.png");

    //Attach file
    I.attachFile('input.dz-hidden-input', 'tests/apps/file-archive/docs/' + validPdfFileName);

    I.say("Screen while loading");
    Document.screenshotElement("div.DTE_Field_Type_wjupload", "/developer/datatables-editor/field-uploadFile-loading.png");

    I.say("Screen file is loaded");
    I.waitForElement(`//button[normalize-space(text())='${i18n.get("Add")}' and not(@disabled)]`, 10);
    Document.screenshotElement("div.DTE_Field_Type_wjupload", "/developer/datatables-editor/field-uploadFile-loaded.png");
});

Scenario('Managers screens', ({ I, Document }) => {
    I.amOnPage('/apps/file-archive/admin/category/');
    Document.screenshot("/redactor/files/file-archive/category-manager.png");
    I.amOnPage('/apps/file-archive/admin/product/');
    Document.screenshot("/redactor/files/file-archive/product-manager.png");
});

Scenario('Export', ({ I, Document, i18n }) => {
    I.say("EXPORT - only main files");
        I.resizeWindow(1280, 700);
        I.amOnPage("/components/file_archiv/export_archiv.jsp");
        I.waitForVisible("#exportArchiveFileForm");
        Document.screenshot("/redactor/files/file-archive/export_base.png");

        I.clickCss("input#btnOk");
        I.waitForText(i18n.get("Please wait, EXPORT of documents is in progress and may take several minutes."), 30);
        Document.screenshot("/redactor/files/file-archive/export_main_only.png");

        I.say("Remove file");
        I.click( locate("#dialogCentralRow").find("input.button100") );
        I.waitForText(i18n.get("was successfully deleted"), 30);
        Document.screenshot("/redactor/files/file-archive/export_removed.png");

    I.say("Export main + awaiting files");
        I.resizeWindow(1280, 750);
        I.amOnPage("/components/file_archiv/export_archiv.jsp");
        I.waitForVisible("#exportArchiveFileForm");
        I.click( locate("#exportArchiveFileForm").find("input[name='includeAwaitingFiles']") );

        I.clickCss("input#btnOk");
        I.waitForText(i18n.get("Please wait, EXPORT of documents is in progress and may take several minutes."), 30);
        Document.screenshot("/redactor/files/file-archive/export_with_awaiting.png");

        I.say("Remove file");
        I.click( locate("#dialogCentralRow").find("input.button100") );
        I.waitForText(i18n.get("was successfully deleted"), 30);

    I.say("Export main + history files");
        I.resizeWindow(1280, 770);
        I.amOnPage("/components/file_archiv/export_archiv.jsp");
        I.waitForVisible("#exportArchiveFileForm");
        I.click( locate("#exportArchiveFileForm").find("input[name='includeHistoryFiles']") );

        I.clickCss("input#btnOk");
        I.waitForText(i18n.get("Please wait, EXPORT of documents is in progress and may take several minutes."), 30);
        Document.screenshot("/redactor/files/file-archive/export_with_history.png");

        I.say("Remove file");
        I.click( locate("#dialogCentralRow").find("input.button100") );
        I.waitForText(i18n.get("was successfully deleted"), 30);

    I.say("EXPORT - all files");
        I.resizeWindow(1280, 790);
        I.amOnPage("/components/file_archiv/export_archiv.jsp");
        I.waitForVisible("#exportArchiveFileForm");
        I.click( locate("#exportArchiveFileForm").find("input[name='includeHistoryFiles']") );
        I.click( locate("#exportArchiveFileForm").find("input[name='includeAwaitingFiles']") );

        I.clickCss("input#btnOk");
        I.waitForText(i18n.get("Please wait, EXPORT of documents is in progress and may take several minutes."), 30);
        Document.screenshot("/redactor/files/file-archive/export_all.png");

        I.say("Remove file");
        I.click( locate("#dialogCentralRow").find("input.button100") );
        I.waitForText(i18n.get("was successfully deleted"), 30);
});

Scenario('Import', async ({ I, Document, i18n }) => {
    const importFile = "tests/apps/file-archive/docs/file_archiv_screenshots.zip";

    I.say("Import");
    I.resizeWindow(1280, 550);
    I.amOnPage("/components/file_archiv/import_archiv.jsp");
    I.waitForVisible("#saveFileForm");
    Document.screenshot("/redactor/files/file-archive/import_base.png");

    I.say("Insert file");
    I.attachFile("#xmlFile", importFile);

    await I.executeScript((row) => {
        $("#loadingMsg").show();
    });
    Document.screenshot("/redactor/files/file-archive/import_start.png");

    I.clickCss("#saveFileForm");
    I.waitForElement("table#fab", 30);

    I.say("Unchecke all options")
    await I.executeScript((row) => {
        $("table#fab").find("input[type='checkbox']").prop("checked", false);
    });

    I.say("Select only 3 of them and start import");
    I.click("table#fab tbody tr:nth-child(1) input");
    I.click("table#fab tbody tr:nth-child(2) input");
    I.click("table#fab tbody tr:nth-child(3) input");
    Document.screenshot("/redactor/files/file-archive/import_replace.png");
    I.clickCss("#btnOk");
    I.waitForText(i18n.get("Import completed"), 10);

    Document.screenshot("/redactor/files/file-archive/import_replace_done.png");
});

Scenario('File archive editor app', async ({ I, DTE,  Document, i18n }) => {
    let confLng = I.getConfLng();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=77668");
    DTE.waitForEditor();

    I.wait(6);
    I.click("a.cke_button__components");
    I.waitForElement("div.cke_editor_data_dialog");
    I.wait(2);
    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");

    I.fillField("#search", i18n.get("Document manager"));
    I.waitForVisible("#components-file_archiv-name");
    I.clickCss("#components-file_archiv-name");

    I.waitForElement(locate(".app-name > h2").withText(i18n.get("Document manager")), 10);

    Document.screenshot("/redactor/files/file-archive/file-archiv-app-insert.png");

    I.clickCss(".app-buy");
    I.waitForVisible("#panel-body-dt-component-datatable-basic");

    doTabScreen(I, Document, "file-archiv-app-tab-base.png", true);

    I.clickCss("#DTE_Field_showOnlySelected_0");
    doTabScreen(I, Document, "file-archiv-app-tab-base_2.png", true);

    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    I.clickCss("#DTE_Field_showPatterns_0");
    I.clickCss("#DTE_Field_archiv_0");
    doTabScreen(I, Document, "file-archiv-app-tab-advanced.png", true);

    I.clickCss("#pills-dt-component-datatable-filesToShow-tab");
    doTabScreen(I, Document, "file-archiv-app-tab-selected.png", false);

    I.amOnPage("/apps/archiv-suborov/?NO_WJTOOLBAR=true");
    I.waitForVisible(".file-archive-app");
    Document.screenshot("/redactor/files/file-archive/app-base.png");

    await I.executeScript(() => {
        $(".collapsible").click();
    });

    Document.screenshot("/redactor/files/file-archive/app-expanded.png");

    I.clickCss("tr.collapsible td a");
    I.switchToNextTab();
    Document.screenshot("/redactor/files/file-archive/app-download.png");
});

function doTabScreen(I, Document, imageName, switchBack) {
    I.switchTo();
    I.switchTo();
    Document.screenshotElement("div.cke_dialog_body", "/redactor/files/file-archive/" + imageName);

    if(switchBack == true) {
        I.switchTo("iframe.cke_dialog_ui_iframe");
        I.switchTo("#editorComponent");
    }
}