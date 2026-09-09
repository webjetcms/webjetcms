Feature('apps.file-archive.multiupload');

const fs = require("fs");
const os = require("os");
const path = require("path");
const SL = require("./SL.js");

const ARCHIVE_FOLDER = "/files/archiv/multiupload/";
const ARCHIVE_LATER_FOLDER = "/files/archiv/files/archiv_insert_later/files/archiv/multiupload/";
const ARCHIVE_FOLDER_NAME = "multiupload";
const DROPZONE_INPUT = "input.dz-hidden-input.dz-hidden-input-dt-upload";
const ELFINDER_MULTUPLOAD = "/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL2FyY2hpdi9tdWx0aXVwbG9hZA_E_E";
const DOCS_DIR = path.join(__dirname, "docs");

let uploadPrefix;
let tmpDir;
let standaloneFiles;
let advancedMainFile;
let bulkActionFiles;
let bulkScheduledFiles;
let ckeditorFile;
let duplicateActionFiles;
let duplicateMetadataFiles;
let rejectedDestinationFile;

Before(({ I, login }) => {
    login('admin');

    if (typeof uploadPrefix == "undefined") {
        uploadPrefix = SL.randomName("multiupload");
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "wj-file-archive-multiupload-"));
        standaloneFiles = [
            createUploadFile("archive_file_test_fourth.pdf", uploadPrefix + "-standalone-a.pdf"),
            createUploadFile("archive_file_test_second.pdf", uploadPrefix + "-standalone-b.pdf")
        ];
        advancedMainFile = createUploadFile(
            "archive_file_test.pdf",
            SL.randomName("multiupload-main").replace(/\s+/g, "-") + ".pdf"
        );
        fs.appendFileSync(advancedMainFile.filePath, "\n% " + uploadPrefix + " advanced metadata test\n");
        bulkActionFiles = [
            createUploadFile("archive_file_test.pdf", uploadPrefix + "-publication-date-a.pdf"),
            createUploadFile("archive_file_test_second.pdf", uploadPrefix + "-publication-date-b.pdf")
        ];
        fs.copyFileSync(advancedMainFile.filePath, bulkActionFiles[0].filePath);
        bulkScheduledFiles = [
            createUploadFile("archive_file_test_fourth.pdf", uploadPrefix + "-scheduled-a.pdf"),
            createUploadFile("archive_file_test_second.pdf", uploadPrefix + "-scheduled-b.pdf")
        ];
        ckeditorFile = createUploadFile("archive_replace.pdf", uploadPrefix + "-ckeditor.pdf");
        duplicateActionFiles = [
            {
                buttonClass: "btn-toast-skip",
                initial: createUploadFile("archive_file_test.pdf", uploadPrefix + "-duplicate-skip.pdf", "duplicate-initial"),
                duplicate: createUploadFile("archive_file_test_second.pdf", uploadPrefix + "-duplicate-skip.pdf", "duplicate-repeat"),
                expectedMainContent: "archive_file_test.png"
            },
            {
                buttonClass: "btn-toast-overwrite",
                initial: createUploadFile("archive_file_test.pdf", uploadPrefix + "-duplicate-overwrite.pdf", "duplicate-initial"),
                duplicate: createUploadFile("archive_file_test_second.pdf", uploadPrefix + "-duplicate-overwrite.pdf", "duplicate-repeat"),
                expectedMainContent: "archive_file_test_second.png"
            },
            {
                buttonClass: "btn-toast-keepboth",
                initial: createUploadFile("archive_file_test.pdf", uploadPrefix + "-duplicate-keepboth.pdf", "duplicate-initial"),
                duplicate: createUploadFile("archive_file_test_third.pdf", uploadPrefix + "-duplicate-keepboth.pdf", "duplicate-repeat"),
                expectedMainContent: "archive_file_test_third.png",
                expectedHistoryContent: "archive_file_test.png"
            }
        ];
        duplicateMetadataFiles = [
            {
                buttonClass: "btn-toast-overwrite",
                initial: createUploadFile("archive_file_test.pdf", uploadPrefix + "-metadata-overwrite.pdf", "metadata-overwrite-initial"),
                duplicate: createUploadFile("archive_file_test_second.pdf", uploadPrefix + "-metadata-overwrite.pdf", "metadata-overwrite-repeat")
            },
            {
                buttonClass: "btn-toast-keepboth",
                initial: createUploadFile("archive_file_test.pdf", uploadPrefix + "-metadata-keepboth.pdf", "metadata-keepboth-initial"),
                duplicate: createUploadFile("archive_file_test_third.pdf", uploadPrefix + "-metadata-keepboth.pdf", "metadata-keepboth-repeat")
            }
        ];
        rejectedDestinationFile = {
            initial: createUploadFile("archive_file_test.pdf", uploadPrefix + "-destination-rejection.pdf", "destination-rejection-initial"),
            duplicate: createUploadFile("archive_file_test_second.pdf", uploadPrefix + "-destination-rejection.pdf", "destination-rejection-repeat")
        };
    }
});

Scenario('Set basic and advanced metadata for all files in a bulk upload', ({ I, DT, DTE }) => {
    const publicationTimestamp = new Date();
    publicationTimestamp.setDate(publicationTimestamp.getDate() + 1);
    publicationTimestamp.setSeconds(0, 0);
    const publicationDateTime = I.formatDateTime(publicationTimestamp.getTime());
    publicationTimestamp.setDate(publicationTimestamp.getDate() + 1);
    const expirationDateTime = I.formatDateTime(publicationTimestamp.getTime());
    const product = uploadPrefix + " product";
    const category = uploadPrefix + " category";
    const productCode = uploadPrefix + " code";
    const priority = "42";
    const referenceToMain = ARCHIVE_FOLDER.substring(1) + advancedMainFile.fileName;
    const note = uploadPrefix + " note";

    I.amOnPage(SL.fileArchive);
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);

    uploadFilesToDropzone(I, [advancedMainFile]);
    DT.waitForLoader("fileArchiveDataTable");

    uploadFilesToDropzone(I, bulkActionFiles, "success", null, {
        validFrom: publicationDateTime,
        validTo: expirationDateTime,
        product: product,
        category: category,
        productCode: productCode,
        showFile: false,
        indexFile: false,
        priority: priority,
        referenceToMain: referenceToMain,
        note: note,
        uploadRedundantFile: true
    });
    DT.waitForLoader("fileArchiveDataTable");

    for (const file of bulkActionFiles) {
        DT.filterContains("virtualFileName", file.virtualName);
        I.click(file.virtualName);
        DTE.waitForEditor("fileArchiveDataTable");
        I.seeInField("#DTE_Field_validFrom", publicationDateTime);
        I.seeInField("#DTE_Field_validTo", expirationDateTime);
        I.click("#pills-dt-fileArchiveDataTable-advanced-tab");
        I.seeInField("#DTE_Field_product", product);
        I.seeInField("#DTE_Field_category", category);
        I.seeInField("#DTE_Field_productCode", productCode);
        I.dontSeeCheckboxIsChecked("#DTE_Field_showFile_0");
        I.dontSeeCheckboxIsChecked("#DTE_Field_indexFile_0");
        I.seeInField("#DTE_Field_priority", priority);
        I.seeInField("#DTE_Field_referenceToMain", referenceToMain);
        I.seeInField("#DTE_Field_note", note);
        DTE.cancel("fileArchiveDataTable");
    }
});

Scenario('Set all scheduled upload parameters for every file in a bulk upload', ({ I, DT, DTE }) => {
    const uploadTimestamp = new Date();
    uploadTimestamp.setDate(uploadTimestamp.getDate() + 1);
    uploadTimestamp.setSeconds(0, 0);
    const dateUploadLater = I.formatDateTime(uploadTimestamp.getTime());
    const validityTimestamp = new Date(uploadTimestamp);
    validityTimestamp.setDate(validityTimestamp.getDate() + 1);
    const validFrom = I.formatDateTime(validityTimestamp.getTime());
    validityTimestamp.setDate(validityTimestamp.getDate() + 1);
    const validTo = I.formatDateTime(validityTimestamp.getTime());
    const emails = "tester@balat.sk, tester2@balat.sk";

    I.amOnPage(SL.fileArchive);
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);

    uploadFilesToDropzone(I, bulkScheduledFiles, "success", null, {
        validFrom: validFrom,
        validTo: validTo,
        saveLater: true,
        dateUploadLater: dateUploadLater,
        emails: emails
    });

    SL.openFileArchive(ARCHIVE_LATER_FOLDER + "scheduled.pdf");
    for (const file of bulkScheduledFiles) {
        DT.filterContains("virtualFileName", file.virtualName);
        SL.checkStatus(1, 4, ['calendar-time']);
        I.click(file.virtualName);
        DTE.waitForEditor("fileArchiveDataTable");
        I.seeInField("#DTE_Field_validFrom", validFrom);
        I.seeInField("#DTE_Field_validTo", validTo);
        I.seeInField("#DTE_Field_dateUploadLater", dateUploadLater);
        I.seeInField("#DTE_Field_emails", emails);
        DTE.cancel("fileArchiveDataTable");
    }
});

Scenario('Upload multiple files directly to file archive folder', async ({ I, DT }) => {
    I.amOnPage(SL.fileArchive);
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);

    uploadFilesToDropzone(I, standaloneFiles);
    DT.waitForLoader("fileArchiveDataTable");

    for (const file of standaloneFiles) {
        DT.filterContains("virtualFileName", file.virtualName);
        I.see(file.virtualName, "#fileArchiveDataTable");
        I.see(file.fileName, "#fileArchiveDataTable");
        I.see("files/archiv/multiupload", "#fileArchiveDataTable");
        SL.checkStatus(1, 4, ['star', 'map-pin']);
    }

    I.amOnPage(ELFINDER_MULTUPLOAD);
    for (const file of standaloneFiles) {
        await SL.checkFileContent(file.fileName, null, false);
    }

});

Scenario('Resolve duplicate multiupload files with all action buttons @screenshot', async ({ I, DT, Document }) => {
    I.amOnPage(SL.fileArchive);
    I.resizeWindow(1400, 850);
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);

    uploadFilesToDropzone(I, duplicateActionFiles.map(file => file.initial), "success", null, {
        uploadRedundantFile: true
    });
    Document.screenshotElement("#upload-wrapper", "/redactor/files/file-archive/drag-drop-upload-dialog.png");
    DT.waitForLoader("fileArchiveDataTable");

    for (const file of duplicateActionFiles) {
        DT.filterContains("virtualFileName", file.initial.virtualName);
        I.see(file.initial.virtualName, "#fileArchiveDataTable");
        I.see(file.initial.fileName, "#fileArchiveDataTable");
    }

    I.amOnPage(SL.fileArchive);
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);

    uploadFilesToDropzone(I, duplicateActionFiles.map(file => file.duplicate), "exist");
    Document.screenshotElement("#upload-wrapper", "/redactor/files/file-archive/drag-drop-upload-duplicity-dialog.png");

    for (const file of duplicateActionFiles) {
        clickDuplicateUploadAction(I, file.duplicate, file.buttonClass);
    }
    DT.waitForLoader("fileArchiveDataTable");

    for (const file of duplicateActionFiles) {
        I.amOnPage(ELFINDER_MULTUPLOAD);
        await SL.checkFileContent(file.initial.fileName, file.expectedMainContent);
    }

    I.amOnPage(ELFINDER_MULTUPLOAD);
    I.dontSeeElement(".elfinder-cwd-filename[title='" + SL.getVersionName(duplicateActionFiles[0].initial.fileName, 1) + "']");
    I.dontSeeElement(".elfinder-cwd-filename[title='" + SL.getVersionName(duplicateActionFiles[1].initial.fileName, 1) + "']");
    await SL.checkFileContent(SL.getVersionName(duplicateActionFiles[2].initial.fileName, 1), duplicateActionFiles[2].expectedHistoryContent);
});

Scenario('Preserve untouched metadata when resolving duplicate bulk uploads', ({ I, DT, DTE }) => {
    const priority = "73";

    I.amOnPage(SL.fileArchive);
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);

    uploadFilesToDropzone(I, duplicateMetadataFiles.map(file => file.initial), "success", null, {
        showFile: false,
        indexFile: false,
        priority: priority
    });
    DT.waitForLoader("fileArchiveDataTable");

    uploadFilesToDropzone(I, duplicateMetadataFiles.map(file => file.duplicate), "exist");
    for (const file of duplicateMetadataFiles) {
        clickDuplicateUploadAction(I, file.duplicate, file.buttonClass);
    }
    DT.waitForLoader("fileArchiveDataTable");

    for (const file of duplicateMetadataFiles) {
        DT.filterContains("virtualFileName", file.initial.virtualName);
        I.click(file.initial.virtualName);
        DTE.waitForEditor("fileArchiveDataTable");
        I.click("#pills-dt-fileArchiveDataTable-advanced-tab");
        I.dontSeeCheckboxIsChecked("#DTE_Field_showFile_0");
        I.dontSeeCheckboxIsChecked("#DTE_Field_indexFile_0");
        I.seeInField("#DTE_Field_priority", priority);
        DTE.cancel("fileArchiveDataTable");
    }
});

Scenario('Reject changing the physical destination for a new version', async ({ I, DT }) => {
    const category = uploadPrefix + "-different-category";

    I.amOnPage(SL.fileArchive);
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);

    uploadFilesToDropzone(I, [rejectedDestinationFile.initial]);
    DT.waitForLoader("fileArchiveDataTable");
    uploadFilesToDropzone(I, [rejectedDestinationFile.duplicate], "exist", null, { category: category });
    clickDuplicateUploadAction(I, rejectedDestinationFile.duplicate, "btn-toast-keepboth", "error");

    I.see(
        "Pri nahradení dokumentu alebo nahratí novej verzie nie je možné zmeniť cieľový adresár ani kategóriu určujúcu adresár.",
        "#toast-container-upload .toast[data-upload-status='error'] .toast-error-message"
    );

    I.amOnPage(ELFINDER_MULTUPLOAD);
    await SL.checkFileContent(rejectedDestinationFile.initial.fileName, "archive_file_test.png");
});

Scenario('Upload and select file archive link in CKEditor', async ({ I, DT, DTE, Document }) => {
    Document.setEditorMode("standard");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    DT.waitForLoader();
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
    I.clickCss("#pills-dt-datatableInit-content-tab");
    I.waitForElement("#pills-dt-datatableInit-content-tab.active", 10);
    I.waitForVisible("#trEditor", 20);

    await DTE.fillCkeditor("<p>" + ckeditorFile.virtualName + "</p>");
    I.clickCss("#trEditor");
    I.pressKey(["CommandOrControl", "A"]);

    I.clickCss(".cke_button_icon.cke_button__link_icon");
    I.waitForText("Informácie o odkaze", 10);
    I.waitForElement(locate(".cke_dialog_tab").withText("Manažér dokumentov"), 20);
    I.click(locate(".cke_dialog_tab").withText("Manažér dokumentov"));
    I.waitForElement("#wjLinkFileArchiveIframeElement", 20);

    I.switchTo("#wjLinkFileArchiveIframeElement");
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);
    uploadFilesToDropzone(I, [ckeditorFile], "success", "#wjLinkFileArchiveIframeElement");
    DT.waitForLoader("fileArchiveDataTable");

    DT.filterContains("virtualFileName", ckeditorFile.virtualName);
    I.click(locate("#fileArchiveDataTable tbody .dt-row-edit a").withText(ckeditorFile.virtualName));

    const expectedUrl = ARCHIVE_FOLDER + ckeditorFile.fileName;
    I.seeInField("#txtUrl", expectedUrl);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");

    const htmlCode = await I.executeScript(() => window.ckEditorInstance.getData());
    I.assertContain(htmlCode, expectedUrl);
});

Scenario('Delete multiupload file archive entities @screenshot', async ({ I, DT, Document }) => {
    await deleteArchiveRowsByPrefix(I, DT, uploadPrefix);
    await deleteArchiveRowsByPrefix(I, DT, advancedMainFile.virtualName);
    await deleteArchiveRowsByPrefix(I, DT, uploadPrefix, ARCHIVE_LATER_FOLDER);

    const wasRemovedByElfinder = await SL.removeFileByElfinder(".elfinder-cwd-filename[title^='" + uploadPrefix + "']", ELFINDER_MULTUPLOAD);
    if (wasRemovedByElfinder) {
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elFinder.");
    }

    fs.rmSync(tmpDir, { recursive: true, force: true });
    Document.resetPageBuilderMode();
});

Scenario('Add folder btn visibility test', ({ I, DT }) => {
    const addFolderPerm = "menuFileArchivManagerCategory";

    I.amOnPage("/apps/file-archive/admin/");
    DT.waitForLoader();
    I.waitForVisible("button#btn-create-folder", 5);

    I.amOnPage("/apps/file-archive/admin/" + "?removePerm=" + addFolderPerm);
    DT.waitForLoader();
    I.dontSeeElement("button#btn-create-folder");
});

/**
 * Creates a temporary copy of a test document for upload.
 * @param {string} sourceName - name of the source file in the docs directory
 * @param {string} targetName - desired name for the copy in the temp directory
 * @param {string|null} variant - optional temp subdirectory for same-name replacement fixtures
 * @returns {{fileName: string, filePath: string, virtualName: string}} upload file descriptor
 */
function createUploadFile(sourceName, targetName, variant = null) {
    const sourcePath = path.join(DOCS_DIR, sourceName);
    const targetDir = variant == null ? tmpDir : path.join(tmpDir, variant);
    fs.mkdirSync(targetDir, { recursive: true });
    const targetPath = path.join(targetDir, targetName);
    fs.copyFileSync(sourcePath, targetPath);

    return {
        fileName: targetName,
        filePath: targetPath,
        virtualName: targetName.replace(/\.[^.]+$/, "").replace(/[-_]+/g, " ")
    };
}

/**
 * Programmatically selects the "multiupload" folder in the file archive jsTree
 * by opening its parent node and selecting it.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {object} DT - DataTable helper
 */
function selectMultiuploadFolder(I, DT) {
    I.waitForElement("#SomStromcek", 20);
    I.executeScript(() => {
        const folder = "/files/archiv/multiupload/";
        const folderWithoutSlash = folder.replace(/\/$/, "");
        const parentFolder = folderWithoutSlash.substring(0, folderWithoutSlash.lastIndexOf("/"));
        const tree = window.$ && window.$("#SomStromcek").jstree(true);
        if (tree == null) return;

        tree.open_node(parentFolder, () => {
            tree.deselect_all(true);
            tree.select_node(folderWithoutSlash);
        });
    });

    I.waitForElement(locate("#SomStromcek a.jstree-clicked").withText(ARCHIVE_FOLDER_NAME), 20);
    DT.waitForLoader("fileArchiveDataTable");
}

/**
 * Uploads files to the file archive dropzone using Playwright's setInputFiles API.
 * Waits for each file's toast notification to reach the expected status.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {Array} files - array of file descriptors from createUploadFile()
 * @param {string} [expectedStatus='success'] - expected upload status in toast
 * @param {string|null} [frameSelector=null] - iframe selector if uploading inside an iframe
 * @param {{validFrom?: string, validTo?: string, saveLater?: boolean, dateUploadLater?: string, emails?: string, product?: string, category?: string, productCode?: string, showFile?: boolean, indexFile?: boolean, priority?: string, referenceToMain?: string, note?: string, uploadRedundantFile?: boolean}|null} [bulkOptions=null] - optional metadata applied before upload
 */
function uploadFilesToDropzone(I, files, expectedStatus = "success", frameSelector = null, bulkOptions = null) {
    I.waitForElement(DROPZONE_INPUT, 20);
    I.usePlaywrightTo("upload files to file archive dropzone", async ({ page }) => {
        const input = frameSelector == null
            ? page.locator(DROPZONE_INPUT)
            : page.frameLocator(frameSelector).locator(DROPZONE_INPUT);
        await input.setInputFiles(files.map(file => file.filePath));
    });

    I.waitForVisible("#fileArchiveDataTable_modal", 20);
    I.waitForVisible("#pills-dt-fileArchiveDataTable-basic-tab.active", 20);
    if (bulkOptions != null) {
        if (bulkOptions.validFrom) setBulkUploadFieldValue(I, "#DTE_Field_validFrom", bulkOptions.validFrom);
        if (bulkOptions.validTo) setBulkUploadFieldValue(I, "#DTE_Field_validTo", bulkOptions.validTo);
        if (bulkOptions.saveLater === true) {
            I.checkOption("#DTE_Field_editorFields-saveLater_0");
            I.seeCheckboxIsChecked("#DTE_Field_editorFields-saveLater_0");
            I.waitForVisible("#DTE_Field_editorFields-dateUploadLater", 10);
            setBulkUploadFieldValue(I, "#DTE_Field_editorFields-dateUploadLater", bulkOptions.dateUploadLater);
            setBulkUploadFieldValue(I, "#DTE_Field_editorFields-emails", bulkOptions.emails);
        }
        const hasAdvancedOptions = bulkOptions.product || bulkOptions.category || bulkOptions.productCode
            || bulkOptions.showFile !== undefined || bulkOptions.indexFile !== undefined || bulkOptions.priority
            || bulkOptions.referenceToMain || bulkOptions.note || bulkOptions.uploadRedundantFile !== undefined;
        if (hasAdvancedOptions) {
            I.click("#pills-dt-fileArchiveDataTable-advanced-tab");
            I.waitForElement("#pills-dt-fileArchiveDataTable-advanced.active", 10);
            if (bulkOptions.product) setBulkUploadAutocompleteValue(I, "#DTE_Field_product", bulkOptions.product);
            if (bulkOptions.category) setBulkUploadAutocompleteValue(I, "#DTE_Field_category", bulkOptions.category);
            if (bulkOptions.productCode) setBulkUploadFieldValue(I, "#DTE_Field_productCode", bulkOptions.productCode);
            if (bulkOptions.showFile !== undefined) setBulkUploadCheckbox(I, "#DTE_Field_showFile_0", bulkOptions.showFile);
            if (bulkOptions.indexFile !== undefined) setBulkUploadCheckbox(I, "#DTE_Field_indexFile_0", bulkOptions.indexFile);
            if (bulkOptions.priority) setBulkUploadFieldValue(I, "#DTE_Field_priority", bulkOptions.priority);
            if (bulkOptions.referenceToMain) setBulkUploadAutocompleteValue(I, "#DTE_Field_referenceToMain", bulkOptions.referenceToMain);
            if (bulkOptions.note) setBulkUploadFieldValue(I, "#DTE_Field_note", bulkOptions.note);
            if (bulkOptions.uploadRedundantFile !== undefined) {
                setBulkUploadCheckbox(I, "#DTE_Field_editorFields-uploadRedundantFile_0", bulkOptions.uploadRedundantFile);
            }
        }
    }
    I.click("#fileArchiveDataTable_modal .DTE_Form_Buttons button.btn-primary");
    I.waitForInvisible("#fileArchiveDataTable_modal", 10);

    I.waitForVisible("#upload-wrapper", 25);
    for (const file of files) {
        I.waitForElement(locate("#toast-container-upload div.toast").withText(file.fileName), 20);
        I.waitForElement(locate("#toast-container-upload div.toast[data-upload-status='" + expectedStatus + "']").withText(file.fileName), 60);
    }
}

/**
 * Sets a bulk-upload field and dispatches the same events used by its UI widget.
 * Direct assignment avoids asynchronous input-widget updates while keeping the test focused on request processing.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {string} selector - input selector
 * @param {string} value - field value
 */
function setBulkUploadFieldValue(I, selector, value) {
    I.executeScript(({ selector, value }) => {
        const input = document.querySelector(selector);
        input.value = value;
        input.dispatchEvent(new Event("input", { bubbles: true }));
        input.dispatchEvent(new Event("change", { bubbles: true }));
    }, { selector, value });
    I.seeInField(selector, value);
}

/**
 * Sets an autocomplete-backed bulk-upload field through the event emitted by jQuery UI selection.
 * The widget updates the value programmatically, so it does not emit a native input or change event.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {string} selector - input selector
 * @param {string} value - selected autocomplete value
 */
function setBulkUploadAutocompleteValue(I, selector, value) {
    I.executeScript(({ selector, value }) => {
        const input = window.$(selector);
        input.val(value);
        input.trigger("autocompleteselect", { item: { value: value } });
    }, { selector, value });
    I.seeInField(selector, value);
}

/**
 * Sets a checkbox in the bulk-upload DataTable editor.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {string} selector - checkbox selector
 * @param {boolean} checked - desired checkbox state
 */
function setBulkUploadCheckbox(I, selector, checked) {
    if (checked) {
        I.checkOption(selector);
        I.seeCheckboxIsChecked(selector);
    } else {
        I.uncheckOption(selector);
        I.dontSeeCheckboxIsChecked(selector);
    }
}

/**
 * Clicks one duplicate-resolution action in the latest toast for the given file.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {{fileName: string}} file - upload file descriptor
 * @param {string} buttonClass - toast action button class
 * @param {string} [expectedStatus='success'] - expected status after processing the action
 */
function clickDuplicateUploadAction(I, file, buttonClass, expectedStatus = "success") {
    I.usePlaywrightTo("resolve duplicate upload action", async ({ page }) => {
        const toast = page.locator("#toast-container-upload div.toast", { hasText: file.fileName }).last();
        await toast.waitFor({ state: "visible", timeout: 20000 });
        await toast.locator("." + buttonClass).click();
        await page.waitForFunction(({ fileName, expectedStatus }) => {
            const toasts = Array.from(document.querySelectorAll("#toast-container-upload div.toast"))
                .filter(toast => toast.textContent.includes(fileName));
            const latestToast = toasts[toasts.length - 1];
            return latestToast != null && latestToast.getAttribute("data-upload-status") === expectedStatus;
        }, { fileName: file.fileName, expectedStatus: expectedStatus }, { timeout: 60000 });
    });
}

/**
 * Deletes all file archive rows whose virtualFileName contains the given prefix.
 * Navigates to the archive folder, filters by prefix, and bulk-deletes matching records.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {object} DT - DataTable helper
 * @param {string} prefix - the virtualFileName prefix to filter by
 * @param {string} [archiveFolder=ARCHIVE_FOLDER] - archive folder containing the records
 */
async function deleteArchiveRowsByPrefix(I, DT, prefix, archiveFolder = ARCHIVE_FOLDER) {
    SL.openFileArchive(archiveFolder + "cleanup.pdf");
    DT.filterContains("virtualFileName", prefix.replace(/[-_]+/g, " "));

    const recordCount = await DT.getRecordCount("fileArchiveDataTable");
    if (recordCount > 0) {
        DT.deleteAll("fileArchiveDataTable");
        DT.waitForLoader("fileArchiveDataTable");
        I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10);
    }
}
