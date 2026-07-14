Feature('apps.file-archive.multiupload');

const fs = require("fs");
const os = require("os");
const path = require("path");
const SL = require("./SL.js");

const ARCHIVE_FOLDER = "/files/archiv/multiupload/";
const ARCHIVE_FOLDER_NAME = "multiupload";
const DROPZONE_INPUT = "input.dz-hidden-input.dz-hidden-input-dt-upload";
const ELFINDER_MULTUPLOAD = "/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL2FyY2hpdi9tdWx0aXVwbG9hZA_E_E";
const DOCS_DIR = path.join(__dirname, "docs");

let uploadPrefix;
let tmpDir;
let standaloneFiles;
let ckeditorFile;
let duplicateActionFiles;

Before(({ I, login }) => {
    login('admin');

    if (typeof uploadPrefix == "undefined") {
        uploadPrefix = SL.randomName("multiupload");
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "wj-file-archive-multiupload-"));
        standaloneFiles = [
            createUploadFile("archive_file_test_fourth.pdf", uploadPrefix + "-standalone-a.pdf"),
            createUploadFile("archive_file_test_second.pdf", uploadPrefix + "-standalone-b.pdf")
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

    uploadFilesToDropzone(I, duplicateActionFiles.map(file => file.initial));
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
 */
function uploadFilesToDropzone(I, files, expectedStatus = "success", frameSelector = null) {
    I.waitForElement(DROPZONE_INPUT, 20);
    I.usePlaywrightTo("upload files to file archive dropzone", async ({ page }) => {
        const input = frameSelector == null
            ? page.locator(DROPZONE_INPUT)
            : page.frameLocator(frameSelector).locator(DROPZONE_INPUT);
        await input.setInputFiles(files.map(file => file.filePath));
    });

    I.waitForVisible("#upload-wrapper", 25);
    for (const file of files) {
        I.waitForElement(locate("#toast-container-upload div.toast").withText(file.fileName), 20);
        I.waitForElement(locate("#toast-container-upload div.toast[data-upload-status='" + expectedStatus + "']").withText(file.fileName), 60);
    }
}

/**
 * Clicks one duplicate-resolution action in the latest toast for the given file.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {{fileName: string}} file - upload file descriptor
 * @param {string} buttonClass - toast action button class
 */
function clickDuplicateUploadAction(I, file, buttonClass) {
    I.usePlaywrightTo("resolve duplicate upload action", async ({ page }) => {
        const toast = page.locator("#toast-container-upload div.toast", { hasText: file.fileName }).last();
        await toast.waitFor({ state: "visible", timeout: 20000 });
        await toast.locator("." + buttonClass).click();
        await page.waitForFunction(({ fileName }) => {
            const toasts = Array.from(document.querySelectorAll("#toast-container-upload div.toast"))
                .filter(toast => toast.textContent.includes(fileName));
            const latestToast = toasts[toasts.length - 1];
            return latestToast != null && latestToast.getAttribute("data-upload-status") === "success";
        }, { fileName: file.fileName }, { timeout: 60000 });
    });
}

/**
 * Deletes all file archive rows whose virtualFileName contains the given prefix.
 * Navigates to the archive folder, filters by prefix, and bulk-deletes matching records.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {object} DT - DataTable helper
 * @param {string} prefix - the virtualFileName prefix to filter by
 */
async function deleteArchiveRowsByPrefix(I, DT, prefix) {
    SL.openFileArchive(ARCHIVE_FOLDER + "cleanup.pdf");
    DT.filterContains("virtualFileName", prefix.replace(/[-_]+/g, " "));

    const recordCount = await DT.getRecordCount("fileArchiveDataTable");
    if (recordCount > 0) {
        DT.deleteAll("fileArchiveDataTable");
        DT.waitForLoader("fileArchiveDataTable");
        I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10);
    }
}
