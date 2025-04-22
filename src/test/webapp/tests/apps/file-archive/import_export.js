Feature('apps.file-archive.import_export');

const SL = require("./SL.js");

let randomNumber;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
    }
});

Scenario('Upload a file, export that file, delete and try to import again', async ({ I, DTE, DT }) => {
    const exportImportPdfFileName = 'archive_export_import.pdf';
    const replacePdfFileName = 'archive_replace.pdf';
    const originalPdfFileName= 'blank/archive_replace.pdf';
    const replacePdfVirtualFileName = SL.randomName("replacefile");
    const exportImportPdfVirtualFileName = SL.randomName("exportimportfile");
    const exportedZipFileName = "autotest-export-" + randomNumber + ".zip";
    const futureUploadFileName = 'archive_file_test_fifth.pdf';
    const futureUploadVirtualFileName = SL.randomName('future');

    // 1. Pridanie nového súboru do archívu
    I.say("Phase1 - Adding new files to archive");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(exportImportPdfVirtualFileName, exportImportPdfFileName);
    DTE.save('fileArchiveDataTable');

    SL.uploadFile(replacePdfVirtualFileName, replacePdfFileName);
    DTE.save('fileArchiveDataTable');

    I.say("Adding new file to archive in future");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(futureUploadVirtualFileName, futureUploadFileName, null , null, null, SL.getFutureTimestamp(1000), "webjetfuture@fexpost.com");
    DTE.save('fileArchiveDataTable');

    // 2. Vyexportovanie
    I.say("Phase2 - Exporting the file");
    I.amOnPage("/components/file_archiv/export_archiv.jsp");
    I.checkOption("input[name=includeAwaitingFiles]")
    I.clickCss("#btnOk");

    I.waitForText("Dokumenty k exportu", 30);
    I.see('Hlavný dokument : files/archiv/archive_file_chosen.pdf');
    I.see('Hlavný dokument : files/archiv/archive_export_import.pdf');
    I.see('Hlavný dokument : files/archiv/archive_replace.pdf');
    I.see('Naplanovaný hlavný dokument : files/archiv/files/archiv_insert_later/files/archiv/archive_file_test_fifth.pdf');
    I.handleDownloads("downloads/" + exportedZipFileName);
    I.clickCss("//div[@id='dialogCentralRow']//p//a[contains(@href, '.zip')]");

    // 3. Vymazanie súboru a nahradenie
    I.say("Phase3 - Deleting the file");
    SL.deleteTestFiles("exportimportfile");
    SL.deleteTestFiles("future");

    I.say("Phase3 - Replacing the file");
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', replacePdfVirtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(replacePdfVirtualFileName, null, null, null, "Nahradiť aktuálny dokument", originalPdfFileName);
    DTE.save('fileArchiveDataTable');

    // 4. Importovanie
    I.say("Phase4 - Importing the file");
    await SL.importFile("../../../build/test/downloads/" + exportedZipFileName, true);

    // 5. Verifikovanie
    I.say("Phase5 - Verification");
    I.amOnPage(SL.fileArchive);
    DT.waitForLoader();

    DT.filterContains('virtualFileName', exportImportPdfVirtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", exportImportPdfVirtualFileName, "", "files/archiv/", exportImportPdfFileName]);

    DT.filterContains('virtualFileName', replacePdfVirtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", replacePdfVirtualFileName, "", "files/archiv/", replacePdfFileName]);

    DT.filterContains('virtualFileName', futureUploadVirtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", futureUploadVirtualFileName, "", "files/archiv/files/archiv_insert_later/files/archiv/", futureUploadFileName]);

    I.say("Phase6 - Verify in elfinder");
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(replacePdfFileName, "archive_replace_test.png", true);
});

Scenario('Delete archiv entity (and file using elfinder if neccesary)', async ({ I }) => {
    const importFileSelector = ".elfinder-cwd-filename[title^='file_archiv_export_aceintegration']";
    await SL.removeFileByElfinder(importFileSelector);

    SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_export_import']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
});