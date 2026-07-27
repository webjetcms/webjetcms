Feature('admin.filebrowser');

const UNUSED_FILES_TABLE_ID = "datatableFieldDTE_Field_editorFields-unusedFiles";
const UNUSED_FILES_SCAN_TIMEOUT = 300000;

let unusedFilesTestData;

Before(({ I, login }) => {
    login('admin');
    if (typeof unusedFilesTestData == "undefined") {
        unusedFilesTestData = createUnusedFilesTestData(I.getRandomTextShort());
    }
});

Scenario('dir properties', async ({I, DT}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_");

    //Find our file in tree
    I.click( locate("span.elfinder-navbar-dir").withText("Súbory"), null, { position: { x: 20, y: 5 } });
    I.click( locate("span.elfinder-navbar-dir").withText("protected"), null, { position: { x: 20, y: 5 } } );

    //Open dir editor + check dir path
    openDirEditorAndCheck(I, "/files/protected/dir-edit-form-test");

    //
    I.say("Edit entity");

        I.see("Indexovať súbory pre vyhľadávanie");
        I.see("Prístupové práva k neverejným sekciám web sídla");

        I.uncheckOption("#DTE_Field_indexFullText_0");
        I.uncheckOption("#DTE_Field_editorFields-permisions_1");

        //Save
        I.switchTo();
        I.click( locate("div.modal-footer").find("button.btn.btn-primary") );

        // TODO - later when save return some message - do check
    //
    I.say("Test working save");
        openDirEditorAndCheck(I, "/files/protected/dir-edit-form-test");

        I.dontSeeCheckboxIsChecked("#DTE_Field_indexFullText_0");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_1");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_2");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_3");

        //Do change
        I.checkOption("#DTE_Field_indexFullText_0");
        I.checkOption("#DTE_Field_editorFields-permisions_1");
        I.seeCheckboxIsChecked("#DTE_Field_indexFullText_0");
        I.seeCheckboxIsChecked("#DTE_Field_editorFields-permisions_1");

        //Save
        I.switchTo();
        I.click( locate("div.modal-footer").find("button.btn.btn-primary") );

        //TestChange
        openDirEditorAndCheck(I, "/files/protected/dir-edit-form-test");

        I.seeCheckboxIsChecked("#DTE_Field_indexFullText_0");
        I.seeCheckboxIsChecked("#DTE_Field_editorFields-permisions_1");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_2");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_3");

        //Return it to former state and save
        I.uncheckOption("#DTE_Field_indexFullText_0");
        I.uncheckOption("#DTE_Field_editorFields-permisions_1");

        //Save
        I.switchTo();
        I.click( locate("div.modal-footer").find("button.btn.btn-primary") );

    I.say("Test indexing");
        openDirEditorAndCheck(I, "/files/protected/dir-edit-form-test");
        I.clickCss("#pills-dt-datatableInit-index-tab");

        I.waitForElement("#indexMenu");
        I.see("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
        I.click("button#start-index-button");
        I.dontSee("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
        //I.see("Indexujú sa súbory, prosím čakajte (môže to trvať niekoľko minút).");

        I.waitForText("Indexovanie súborov dokončené", 100);

    //
    I.say("Test usage");
        I.clickCss("#pills-dt-datatableInit-usage-tab");
        DT.waitForLoader("#datatableFieldDTE_Field_editorFields-docDetailsList_processing", 200);
        I.see("Použitie", ".nav-link.active");
});

function openDirEditorAndCheck(I, dirPath) {
    //Open dir editor for /files/protected/dir-edit-form-test
    I.say("Opening /files/protected/dir-edit-form-test");
    I.clickCss("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E");
    I.rightClick('#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E');
    I.click( locate('div.elfinder-contextmenu-item').withChild( locate("span").withText("Nastavenie priečinka") ) );
    I.switchTo("#modalIframeIframeElement");
    I.waitForElement( locate("h5.modal-title").withText(dirPath), 10 );
}

function openFileEditorAndCheck(I, fileName) {
    //Open properties of the file
    I.clickCss("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3QvdGVzdGpwZ2F0dGFjaG1lbnRmaWxlLmpwZw_E_E");
    I.rightClick("#iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3QvdGVzdGpwZ2F0dGFjaG1lbnRmaWxlLmpwZw_E_E");
    I.click( locate('div.elfinder-contextmenu-item').withChild( locate("span").withText("Nastavenie súboru") ) );
    I.switchTo("#modalIframeIframeElement");
    I.seeElement( locate("h5.modal-title").withText(fileName) );
}

Scenario('file properties', async ({I, DT}) => {
    I.amOnPage("/admin/elFinder/#elf_iwcm_1_L2ZpbGVzL3Byb3RlY3RlZC9kaXItZWRpdC1mb3JtLXRlc3Q_E");

    openFileEditorAndCheck(I, "testjpgattachmentfile.jpg");

    //
    I.say("Test indexing");
        I.clickCss("#pills-dt-datatableInit-index-tab");

        I.waitForElement("#indexMenu");
        I.see("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
        I.click("button#start-index-button");
        I.dontSee("Spustiť akciu indexovania súborov. Táto akcia môže trvať niekoľko minút.");
        I.see("Indexujú sa súbory, prosím čakajte (môže to trvať niekoľko minút).");

        I.waitForText("Indexovanie súborov dokončené", 100);

    //
    I.say("Test usage");
        I.clickCss("#pills-dt-datatableInit-usage-tab");
        DT.waitForLoader("#datatableFieldDTE_Field_docDetailsList_processing", 200);
        I.see("Použitie", ".nav-link.active");
});

Scenario('unused files - prepare folders and files @singlethread @screenshot', async ({ I }) => {
    I.say("Create an isolated folder structure for the unused files tests");
    openFilesFolder(I, "/files");
    createElfinderFolder(I, unusedFilesTestData.baseName);

    openFilesFolder(I, unusedFilesTestData.basePath);
    createElfinderFolder(I, unusedFilesTestData.logicFolderName);

    openFilesFolder(I, unusedFilesTestData.basePath);
    uploadGeneratedFiles(I, [unusedFilesTestData.multiUserFileName]);

    openFilesFolder(I, unusedFilesTestData.logicFolderPath);
    createElfinderFolder(I, unusedFilesTestData.logicChildName);

    openFilesFolder(I, unusedFilesTestData.logicFolderPath);
    uploadGeneratedFiles(I, [
        unusedFilesTestData.selectedFileName,
        unusedFilesTestData.deleteAllFileName
    ]);

    openFilesFolder(I, unusedFilesTestData.logicChildPath);
    uploadGeneratedFiles(I, [unusedFilesTestData.childFileName]);
});

Scenario('unused files - scan and delete @singlethread @screenshot', async ({ I, DT, Document }) => {
    openUnusedFilesTab(
        I,
        unusedFilesTestData.basePath,
        unusedFilesTestData.logicFolderName,
        unusedFilesTestData.logicFolderPath
    );

    I.say("The table is visible before the first scan and subfolders are disabled by default");
    I.waitForElement("#" + UNUSED_FILES_TABLE_ID, 20);
    I.seeElement("#" + UNUSED_FILES_TABLE_ID + "_wrapper button.buttons-unused-files-analyze");
    I.dontSeeCheckboxIsChecked("#unused-files-include-subfolders");
    DT.waitForLoader("#" + UNUSED_FILES_TABLE_ID + "_processing", 30);
    screenshotUnusedFilesDialog(I, Document, "folder_settings_unused_files.png");

    I.say("A non-recursive scan only returns files from the selected folder");
    startUnusedFilesScan(I);
    waitForUnusedFilesScanInUi(I, DT);
    I.waitForText("Nájdených nepoužívaných súborov: 2", 20, "#unused-files-status-message");
    I.see(unusedFilesTestData.selectedFileName, "#" + UNUSED_FILES_TABLE_ID + "_wrapper");
    I.see(unusedFilesTestData.deleteAllFileName, "#" + UNUSED_FILES_TABLE_ID + "_wrapper");
    I.dontSee(unusedFilesTestData.childFileName, "#" + UNUSED_FILES_TABLE_ID + "_wrapper");

    I.say("A recursive scan also returns a file from the child folder");
    I.checkOption("#unused-files-include-subfolders");
    startUnusedFilesScan(I);
    waitForUnusedFilesScanInUi(I, DT);
    I.waitForText("Nájdených nepoužívaných súborov: 3", 20, "#unused-files-status-message");
    I.see(unusedFilesTestData.childFileName, "#" + UNUSED_FILES_TABLE_ID + "_wrapper");
    I.see(unusedFilesTestData.logicChildPath + "/" + unusedFilesTestData.childFileName,
        "#" + UNUSED_FILES_TABLE_ID + "_wrapper");
    screenshotUnusedFilesDialog(I, Document, "folder_settings_unused_files_result.png");

    I.say("The latest scan is restored when the same administrator reopens the folder");
    I.switchTo();
    openUnusedFilesTab(
        I,
        unusedFilesTestData.basePath,
        unusedFilesTestData.logicFolderName,
        unusedFilesTestData.logicFolderPath
    );
    waitForUnusedFilesScanInUi(I, DT);
    I.seeCheckboxIsChecked("#unused-files-include-subfolders");
    I.waitForText("Nájdených nepoužívaných súborov: 3", 20, "#unused-files-status-message");
    I.see(unusedFilesTestData.selectedFileName, "#" + UNUSED_FILES_TABLE_ID + "_wrapper");
    I.see(unusedFilesTestData.deleteAllFileName, "#" + UNUSED_FILES_TABLE_ID + "_wrapper");
    I.see(unusedFilesTestData.childFileName, "#" + UNUSED_FILES_TABLE_ID + "_wrapper");

    I.say("Delete one selected result through the standard DataTable remove action");
    const selectedRow = locate("#" + UNUSED_FILES_TABLE_ID + " tbody tr")
        .withText(unusedFilesTestData.selectedFileName);
    I.click(selectedRow.find("td.dt-select-td"));
    I.waitForElement("#" + UNUSED_FILES_TABLE_ID + "_wrapper button.buttons-remove:not(.disabled)", 10);
    I.clickCss("#" + UNUSED_FILES_TABLE_ID + "_wrapper button.buttons-remove");
    I.waitForVisible("div.DTE_Action_Remove", 10);
    I.click("Zmazať", "div.DTE_Action_Remove:visible");
    I.waitForInvisible(selectedRow, 30);
    I.waitForText("Nájdených nepoužívaných súborov: 2", 20, "#unused-files-status-message");

    I.say("Delete all remaining scan results through the same DataTable remove action");
    I.clickCss("#" + UNUSED_FILES_TABLE_ID + "_wrapper button.buttons-unused-files-delete-all");
    I.waitForVisible("div.DTE_Action_Remove", 10);
    I.click("Zmazať", "div.DTE_Action_Remove:visible");
    I.waitForText("Nenašli sa žiadne nepoužívané súbory.", 30, "#unused-files-status-message");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 30,
        "#" + UNUSED_FILES_TABLE_ID + "_wrapper");
});

Scenario('unused files - separate scan per user @singlethread', ({ I, DT }) => {
    I.say("The first administrator scans the folder and sees the unused file");
    scanBaseFolderForUnusedFiles(I, DT, "admin");

    I.say("A second administrator runs an independent scan and sees the same unused file");
    scanBaseFolderForUnusedFiles(I, DT, "tester3");

    I.logout();
});

Scenario('unused files - cleanup @singlethread @screenshot', async ({ I }) => {
    I.say("Delete the complete unused files test structure");
    openFilesFolder(I, "/files");
    await deleteElfinderItemIfExists(I, unusedFilesTestData.baseName);
});

function createUnusedFilesTestData(suffix) {
    const baseName = "unused-files-autotest-" + suffix;
    const basePath = "/files/" + baseName;
    const logicFolderName = "logic-autotest-" + suffix;
    const logicFolderPath = basePath + "/" + logicFolderName;
    const logicChildName = "logic-child-autotest-" + suffix;

    return {
        baseName,
        basePath,
        logicFolderName,
        logicFolderPath,
        logicChildName,
        logicChildPath: logicFolderPath + "/" + logicChildName,
        selectedFileName: "selected-unused-autotest-" + suffix + ".txt",
        deleteAllFileName: "delete-all-unused-autotest-" + suffix + ".txt",
        childFileName: "child-unused-autotest-" + suffix + ".txt",
        multiUserFileName: "multi-user-unused-autotest-" + suffix + ".txt"
    };
}

function openFilesFolder(I, folderPath) {
    I.amOnPage("/admin/v9/templates/temps-groups-list/");
    I.amOnPage("/admin/v9/files/index/#" + getElfinderId(folderPath));
    I.waitForElement("#finder .elfinder-cwd-wrapper", 30);
    I.waitForVisible(".elfinder-button-icon.elfinder-button-icon-mkdir:visible", 30);
}

function getElfinderId(folderPath) {
    const encodedPath = Buffer.from(folderPath)
        .toString("base64")
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=/g, "_E");
    return "elf_iwcm_2_" + encodedPath;
}

function getElfinderItemSelector(itemName) {
    return '.elfinder-cwd-filename[title="' + itemName + '"]';
}

function createElfinderFolder(I, folderName) {
    I.usePlaywrightTo("create an elFinder test folder", async ({ page }) => {
        await page.locator(".elfinder-button-icon-mkdir:visible").last().click();
        const input = page.locator(".elfinder-cwd-filename textarea:visible").last();
        await input.waitFor({ state: "visible", timeout: 10000 });
        await input.fill(folderName);
        await input.press("Enter");
    });
    I.waitForElement(getElfinderItemSelector(folderName), 30);
}

function uploadGeneratedFiles(I, fileNames) {
    I.usePlaywrightTo("upload generated unused files fixtures", async ({ page }) => {
        const files = fileNames.map(fileName => ({
            name: fileName,
            mimeType: "text/plain",
            buffer: Buffer.from("Unused files E2E fixture: " + fileName)
        }));
        await page.locator(".elfinder-button-icon-upload:visible").last().click();
        const input = page.locator(
            'input[type="file"][multiple]:not([webkitdirectory])'
        ).last();
        await input.waitFor({ state: "attached", timeout: 10000 });
        await input.setInputFiles(files);
    });
    I.clickIfVisible(".elfinder-confirm-accept");
    I.waitForInvisible(".elfinder-notify-upload", 180);
    I.waitForInvisible(".elfinder-notify-chunkmerge", 180);
    I.waitForElement(getElfinderItemSelector(fileNames[fileNames.length - 1]), 60);
}

function openUnusedFilesTab(I, parentPath, folderName, expectedFolderPath) {
    openFilesFolder(I, parentPath);
    I.waitForElement(getElfinderItemSelector(folderName), 30);
    I.rightClick(getElfinderItemSelector(folderName));
    I.waitForVisible(".elfinder-contextmenu", 10);
    I.click(locate("div.elfinder-contextmenu-item")
        .withChild(locate("span").withText("Nastavenie priečinka")));
    I.waitForVisible("#modalIframe", 10);
    I.switchTo("#modalIframeIframeElement");
    I.waitForElement(locate("h5.modal-title").withText(expectedFolderPath), 20);
    I.clickCss("#pills-dt-datatableInit-unusedFiles-tab");
    I.waitForElement("#unused-files-menu", 20);
}

function startUnusedFilesScan(I) {
    I.clickCss("#" + UNUSED_FILES_TABLE_ID + "_wrapper button.buttons-unused-files-analyze");
    I.waitForElement("#unused-files-status.alert-info", 10);
}

function waitForUnusedFilesScanInUi(I, DT) {
    I.waitForElement("#unused-files-status.alert-success", UNUSED_FILES_SCAN_TIMEOUT / 1000);
    DT.waitForLoader("#" + UNUSED_FILES_TABLE_ID + "_processing", 30);
}

function screenshotUnusedFilesDialog(I, Document, fileName) {
    if (Document.isScreenshotsEnabled() == false) {
        return;
    }

    I.switchTo();
    Document.screenshotElement(
        "#modalIframe > div.modal-dialog",
        "/redactor/files/fbrowser/folder-settings/" + fileName
    );
    I.switchTo("#modalIframeIframeElement");
}

function scanBaseFolderForUnusedFiles(I, DT, user) {
    I.relogin(user);
    openUnusedFilesTab(
        I,
        "/files",
        unusedFilesTestData.baseName,
        unusedFilesTestData.basePath
    );
    I.waitForElement("#" + UNUSED_FILES_TABLE_ID, 20);
    I.dontSeeCheckboxIsChecked("#unused-files-include-subfolders");
    DT.waitForLoader("#" + UNUSED_FILES_TABLE_ID + "_processing", 30);

    startUnusedFilesScan(I);
    waitForUnusedFilesScanInUi(I, DT);

    // A non-recursive scan of the base folder only returns the file placed directly in it
    I.waitForText("Nájdených nepoužívaných súborov: 1", 20, "#unused-files-status-message");
    I.see(unusedFilesTestData.multiUserFileName, "#" + UNUSED_FILES_TABLE_ID + "_wrapper");
}

async function deleteElfinderItemIfExists(I, itemName) {
    const selector = getElfinderItemSelector(itemName);
    const visibleItems = await I.grabNumberOfVisibleElements(selector);
    if (visibleItems == 0) {
        return;
    }

    I.rightClick(selector);
    I.waitForVisible(".elfinder-contextmenu", 10);
    I.clickCss(".elfinder-contextmenu-item .elfinder-button-icon-rm");
    I.waitForVisible(".elfinder-confirm-accept", 10);
    I.clickCss(".elfinder-confirm-accept");
    I.waitForInvisible(selector, 120);
}
