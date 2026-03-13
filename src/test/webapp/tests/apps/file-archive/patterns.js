Feature('apps.file-archive.patterns');

const SL = require("./SL.js");

let patternVirtualFileName, validPdfVirtualFileName;

Before(({ login }) => {
    login('admin');
    if (typeof patternVirtualFileName == "undefined") {
        patternVirtualFileName = SL.randomName("pattern");
        validPdfVirtualFileName = SL.randomName("validfile");
    }
});

Scenario('Pattern basic tests', ({ I, DTE, DT}) => {
    const patternFileName = 'archive_file_test.pdf';
    const validPdfFileName = 'archive_file_test_second.pdf';

    I.amOnPage(SL.fileArchive);

    // 1. Pridanie nového súboru do archívu
    I.say("Phase 1 - Add new file to archive and use first one as a pattern");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(validPdfVirtualFileName, validPdfFileName);
    DTE.save('fileArchiveDataTable');

    // 2. Pridanie dalsieho súboru do archívu a oznacenie prveho ako patternu
    I.say("Phase 2 - Add new file to archive");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(patternVirtualFileName, patternFileName);

    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.clickCss("#DTE_Field_referenceToMain");
    I.clickCss(`//li[contains(@class, "ui-menu-item")]//div[text()="files/archiv/${validPdfFileName}"]`);
    DTE.save('fileArchiveDataTable');

    // 3. Overenie ze vzor nema vzor
    I.say("Phase3 - Pattern does not have pattern tab");
    DT.filterEquals('virtualFileName', patternVirtualFileName);
    SL.checkStatus(1, 4, ['star', 'map-pin', 'texture']);
    I.clickCss('button.buttons-select-all');
    SL.editFile(patternVirtualFileName);
    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.dontSeeElement("#pills-dt-fileArchiveDataTable-listOfPattern-tab");
    DTE.cancel('fileArchiveDataTable');

    // 4. Overenie v datatabulke v editore
    I.say("Phase4 - Check in editor datatable");
    DT.filterEquals('virtualFileName', validPdfVirtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile();
    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.clickCss("#pills-dt-fileArchiveDataTable-listOfPattern-tab");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-patterns", 1, ["", patternVirtualFileName, "files/archiv/"]);
    DTE.save('fileArchiveDataTable');
});

Scenario('Pattern path change test', ({ I, DTE, DT }) => {
    const folderName = SL.randomName("folder");
    const newFileName = 'archive_file_test_third.pdf';

    // 1. Zmena cesty patternu
    I.say("Phase 1 - Change pattern file path");
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', patternVirtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, "Nahrať novú verziu", newFileName);
    I.fillField("#editorAppDTE_Field_editorFields-dir .input-group input", "/files/archiv/" + folderName);
    DTE.save('fileArchiveDataTable');

    // 2. Overenie ze sa to zmenilo
    I.say("Phase 2 - Verify path was changed");
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', validPdfVirtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile();
    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.clickCss("#pills-dt-fileArchiveDataTable-listOfPattern-tab");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-patterns", 1, ["", patternVirtualFileName, "/files/archiv/" + folderName]);
    DTE.save('fileArchiveDataTable');
});

Scenario('Veritfy pattern is also deleted when main file is deleted', ({ I, DT }) => {
    // 1. Vymazanie main suboru
    I.say("Phase 1 - Delete main file");
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', validPdfVirtualFileName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    DT.deleteAll('fileArchiveDataTable');

    // 2. Overenie vymazania vzoru
    I.say("Phase 2 - Verify if pattern has been removed");
    DT.filterEquals('virtualFileName', patternVirtualFileName);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Delete archiv entity and autotest folders', async ({I, DT}) => {
    I.amOnPage(SL.fileArchive);
    DT.filterContains('virtualFileName', 'autotest-');
    if (await I.grabNumberOfVisibleElements(".dt-empty") === 0) {
        DT.deleteAll('fileArchiveDataTable');
        I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 10);
    }

    const folderSelector = ".elfinder-cwd-filename[title^='autotest--']";
    await SL.removeFileByElfinder(folderSelector);
});