Feature('apps.file-archive.rollback');

const SL = require("./SL.js");

let randomNumber, virtualFileName;

Before(({ I, login, DT }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        virtualFileName = "autotest-special-032021";
        I.amOnPage(SL.fileArchive);
        DT.showColumn("Poradie usporiadania", "fileArchiveDataTable");
    }
    DT.addContext("archive","#fileArchiveDataTable_wrapper");
    DT.addContext("waiting","#datatableFieldDTE_Field_editorFields-waitingFiles_wrapper")
    
});

Scenario('Import 2', async ({ I, DT, DTE}) => {
    await SL.importFile("tests/apps/file-archive/docs/rollback_import.zip");
});

Scenario('Rollback', async ({ I, DT}) => {
    const rollbackFileName = 'archive_file_test.pdf';

    // 1. Prejdi na stránku s archívom a vráť súbor do predchádzajúcej verzie
    I.say('Phase 1 - Rollback to latest historical version');
    I.amOnPage(SL.fileArchive);
    SL.rollback(virtualFileName);

    DT.filterSelect("editorFields.statusIcons", 'Všetky verzie dokumentu');
    await SL.setAsc();
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualFileName, "", "files/archiv/", rollbackFileName]);
    DT.checkTableRow("fileArchiveDataTable", 2, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(rollbackFileName, 1)]);
    DT.checkTableRow("fileArchiveDataTable", 3, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(rollbackFileName, 2)]);
    DT.checkTableRow("fileArchiveDataTable", 4, ["", virtualFileName, "", "files/archiv/", 'archive_file_test_fourth.pdf']);
    SL.checkOrderIds(['-1', '4', '2', '3']);

    // 2. Prejdi na elfinder a skontroluj obsah súboru
    I.say('Phase 2 - Check file content after rollback');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(rollbackFileName, 'archive_file_test_third.png');

    // 3. Opätovné vrátenie súboru na predchádzajúcu verziu
    I.say('Phase 3 - Rollback to previous historical version');
    I.amOnPage(SL.fileArchive);
    SL.rollback(virtualFileName);
    DT.filterSelect("editorFields.statusIcons", 'Všetky verzie dokumentu');
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualFileName, "", "files/archiv/", rollbackFileName]);
    DT.checkTableRow("fileArchiveDataTable", 2, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(rollbackFileName, 1)]);
    DT.checkTableRow("fileArchiveDataTable", 3, ["", virtualFileName, "", "files/archiv/", 'archive_file_test_fourth.pdf']);
    SL.checkOrderIds(['-1', '3', '2']);

    // 4. Opätovné skontrolovanie súboru v elfinderi
    I.say('Phase 4 - Check file content after second rollback');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(rollbackFileName, 'archive_file_test_second.png');

    // 5. Opätovné vrátenie súboru na predchádzajúcu verziu
    I.say('Phase 5 - Rollback to previous historical version');
    I.amOnPage(SL.fileArchive);
    SL.rollback(virtualFileName);
    DT.filterSelect("editorFields.statusIcons", 'Všetky verzie dokumentu');
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualFileName, "", "files/archiv/", rollbackFileName]);
    DT.checkTableRow("fileArchiveDataTable", 2, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(rollbackFileName, 1)]);
    SL.checkOrderIds(['-1', '2']);

    // 6. Skontroluj subor
    I.say('Phase 6 - Check file content after rollback');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(rollbackFileName, 'archive_file_test_fourth.png');

    // 7. Rollback bez použitia historických verzií
    I.say('Phase 7 - Rollback with no available historical versions');
    I.amOnPage(SL.fileArchive);
    SL.rollback(virtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualFileName, "", "files/archiv/", rollbackFileName]);
    //Deselect
    I.clickCss('button.buttons-select-all');
    SL.rollback(virtualFileName, false);
    SL.checkOrderIds(['-1']);
});

Scenario('Delete archiv entity (and file using elfinder if neccesary)', async ({I}) => {
    SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
});