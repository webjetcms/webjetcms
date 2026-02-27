Feature('apps.file-archive.edit_file');

const SL = require("./SL.js");

let randomNumber;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
    }
});

Scenario('Add new file and edit', async ({ I, DT, DTE }) => {
    const editablePdfFileName = 'archive_file_test.pdf';
    const editablePdfVirtualFileName = SL.randomName("editablefile");
    const renamedPdfFileName = 'new_file_name.pdf'

    // 1. Pridanie nového súboru do archívu
    I.say("Phase1 - Adding a new file to the archive");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(editablePdfVirtualFileName, editablePdfFileName);
    DTE.save('fileArchiveDataTable');

    // 2. Editovanie pridaneho suboru
    I.say("Phase2 - Editing the added file");
    DT.filterEquals('virtualFileName', editablePdfVirtualFileName);
    I.clickCss('button.buttons-select-all');
    const validFrom = SL.getFutureTimestamp(0);
    const validTo = SL.getFutureTimestamp(240);
    SL.editFile(editablePdfVirtualFileName+'-chan.ge', null, validFrom, validTo, null);
    DTE.save('fileArchiveDataTable');

    // 3. Overenie ci sa spravne editovalo
    I.say("Phase3 - Verifying if the edit was successful");
    DT.filterEquals('virtualFileName', editablePdfVirtualFileName+'-chan.ge');
    DT.checkTableRow("fileArchiveDataTable", 1, ["", "", editablePdfVirtualFileName+'-chan.ge', "", "files/archiv/", editablePdfFileName, "", validFrom, validTo]);
    SL.checkStatus(1, 4, ['star', 'map-pin']);

    // 4. Premenovanie súboru
    I.say("Phase4 - Renaming the file");
    SL.editFile(null, renamedPdfFileName);
    DTE.save('fileArchiveDataTable');
    I.waitForElement("div.toast-message", 10);
    I.see("Dokument bol úspešne premenovaný.", "div.toast-message");

    // 5. Overenie, že sa súbor premenoval a funguje a obsah sa nezmenil
    I.say("Phase5 - Verifying that the file was renamed, is functioning, and its content remains unchanged");
    I.amOnPage(SL.elfinder);
    I.seeElement(`.elfinder-cwd-filename[title='${renamedPdfFileName}']`);
    I.dontSeeElement(`.elfinder-cwd-filename[title='${editablePdfFileName}']`);
    await SL.checkFileContent(renamedPdfFileName);
});

Scenario('Delete archiv entity (and file using elfinder if neccesary)', async ({I}) => {
    SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
});