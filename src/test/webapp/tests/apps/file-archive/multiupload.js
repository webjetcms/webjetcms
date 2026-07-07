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
    }
});

Scenario('Upload multiple files directly to file archive folder', async ({ I, DT }) => {
    I.amOnPage(SL.fileArchive);
    DT.waitForLoader("fileArchiveDataTable");
    selectMultiuploadFolder(I, DT);

    uploadFilesToDropzone(I, standaloneFiles);
    DT.waitForLoader("fileArchiveDataTable");

    for (const file of standaloneFiles) {
        DT.filterEquals("virtualFileName", file.virtualName);
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

    DT.filterEquals("virtualFileName", ckeditorFile.virtualName);
    I.click(locate("#fileArchiveDataTable tbody .dt-row-edit a").withText(ckeditorFile.virtualName));

    const expectedUrl = ARCHIVE_FOLDER + ckeditorFile.fileName;
    I.seeInField("#txtUrl", expectedUrl);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");

    const htmlCode = await I.executeScript(() => window.ckEditorInstance.getData());
    I.assertContain(htmlCode, expectedUrl);
});

Scenario('Delete multiupload file archive entities', async ({ I, DT, Document }) => {
    await deleteArchiveRowsByPrefix(I, DT, uploadPrefix);

    const wasRemovedByElfinder = await SL.removeFileByElfinder(".elfinder-cwd-filename[title^='" + uploadPrefix + "']", ELFINDER_MULTUPLOAD);
    if (wasRemovedByElfinder) {
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elFinder.");
    }

    fs.rmSync(tmpDir, { recursive: true, force: true });
    Document.resetPageBuilderMode();
});

/**
 * Creates a temporary copy of a test document for upload.
 * @param {string} sourceName - name of the source file in the docs directory
 * @param {string} targetName - desired name for the copy in the temp directory
 * @returns {{fileName: string, filePath: string, virtualName: string}} upload file descriptor
 */
function createUploadFile(sourceName, targetName) {
    const sourcePath = path.join(DOCS_DIR, sourceName);
    const targetPath = path.join(tmpDir, targetName);
    fs.copyFileSync(sourcePath, targetPath);

    return {
        fileName: targetName,
        filePath: targetPath,
        virtualName: targetName.replace(/\.[^.]+$/, "")
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
 * Deletes all file archive rows whose virtualFileName contains the given prefix.
 * Navigates to the archive folder, filters by prefix, and bulk-deletes matching records.
 * @param {CodeceptJS.I} I - CodeceptJS actor
 * @param {object} DT - DataTable helper
 * @param {string} prefix - the virtualFileName prefix to filter by
 */
async function deleteArchiveRowsByPrefix(I, DT, prefix) {
    SL.openFileArchive(ARCHIVE_FOLDER + "cleanup.pdf");
    DT.filterContains("virtualFileName", prefix);

    const recordCount = await DT.getRecordCount("fileArchiveDataTable");
    if (recordCount > 0) {
        DT.deleteAll("fileArchiveDataTable");
        DT.waitForLoader("fileArchiveDataTable");
        I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10);
    }
}
