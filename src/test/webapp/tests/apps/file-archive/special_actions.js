Feature('apps.file-archive.special_actions');

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

Scenario('Special file upload - Upload a new version', async ({ I, DTE, DT }) => {
    const versionUploadFileName = 'archive_file_test.pdf';
    const versionUploadSecondFileName = 'archive_file_test_second.pdf';
    const versionUploadThirdFileName = 'archive_file_test_third.pdf';
    const versionUploadFifthFileName = 'archive_file_test_fifth.pdf';

    // 1. Pridanie nového súboru do archívu
    I.say('Phase 1 - Upload the first file version');
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(virtualFileName, versionUploadFileName);
    DTE.save('fileArchiveDataTable');

    // 2. Nahratie novej verzie súboru
    I.say('Phase 2 - Upload a new version of the file');
    DT.filterEquals('virtualFileName', virtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, 'Nahrať novú verziu', versionUploadSecondFileName);
    DTE.save('fileArchiveDataTable');

    // 3. Overenie ci sa spravne editovalo
    I.say('Phase 3 - Verify file version update in table');
    DT.filterEquals('virtualFileName', virtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualFileName, "", "files/archiv/", versionUploadFileName]);
    SL.checkStatus(1, 3, ['star', 'map-pin']);
    //I.clickCss('button.buttons-select-all');
    I.clickCss('#fileArchiveDataTable_wrapper .buttons-edit');
    DTE.waitForEditor('fileArchiveDataTable');
    I.clickCss('#pills-dt-fileArchiveDataTable-listOfVersions-tab');
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-listOfVersions", 1, ["", virtualFileName, "files/archiv/", SL.getVersionName(versionUploadFileName, 1), 'Áno', '0', '2']);
    // 4. Overenie, že sa súbor pridal a pôvodný premenoval a funguje
    I.say('Phase 4 - Verify new file version is accessible');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(versionUploadFileName, 'archive_file_test_second.png');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(SL.getVersionName(versionUploadFileName, 1));

    // 5. Nahratie opäť novej verzie súboru
    I.say('Phase 5 - Upload another new version of the file');
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', virtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, 'Nahrať novú verziu', versionUploadThirdFileName);
    DTE.save('fileArchiveDataTable');

    // 6. Overenie ci sa spravne editovalo
    I.say('Phase 6 - Verify file version update again');
    DT.filterEquals('virtualFileName', virtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualFileName, "", "files/archiv/", versionUploadFileName]);
    SL.checkStatus(1, 3, ['star', 'map-pin']);
    //I.clickCss('button.buttons-select-all');
    I.clickCss('#fileArchiveDataTable_wrapper .buttons-edit');
    DTE.waitForEditor('fileArchiveDataTable');
    I.clickCss('#pills-dt-fileArchiveDataTable-listOfVersions-tab');
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-listOfVersions", 1, ["", virtualFileName, "files/archiv/", SL.getVersionName(versionUploadFileName, 1), 'Áno', '0', '3']);
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-listOfVersions", 2, ["", virtualFileName, "files/archiv/", SL.getVersionName(versionUploadFileName, 2), 'Áno', '0', '2']);

    // 7. Overenie, že sa súbory pridali a pôvodný premenoval a funguje
    I.say('Phase 7 - Verify all file versions are accessible');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(versionUploadFileName, 'archive_file_test_third.png');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(SL.getVersionName(versionUploadFileName, 2), 'archive_file_test_second.png');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(SL.getVersionName(versionUploadFileName, 1));

    // 8. Overenie, že historické záznamy v tabuľke nemajú checkbox, sú sivé, pri edite nie je SAVE
    I.say('Phase 8 - Verify historical records behavior');
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', virtualFileName);
    DT.filterSelect("editorFields.statusIcons", 'Všetky verzie dokumentu');
    await SL.setAsc();
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualFileName, "", "files/archiv/", versionUploadFileName]);
    DT.checkTableRow("fileArchiveDataTable", 2, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(versionUploadFileName, 1)]);
    DT.checkTableRow("fileArchiveDataTable", 3, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(versionUploadFileName, 2)]);
    SL.checkOrderIds(['-1', '3', '2']);
    I.assertEqual(await SL.getBackgroundColor(1), SL.white);
    I.assertEqual(await SL.getBackgroundColor(2), SL.gray);
    I.assertEqual(await SL.getBackgroundColor(3), SL.gray);

    // 9. Overenie, že pri pokuse o vymazanie historickej verzie dôjde k chybe
    I.say('Phase 9 - Verify error on deleting historical versions');
    DT.filterSelect("editorFields.statusIcons", 'Historická verzia dokumentu');
    I.click(locate('a').withText(virtualFileName).first());
    DTE.waitForEditor('fileArchiveDataTable');
    I.dontSeeElement(locate('button').withText('Uložiť'));
    DTE.cancel('fileArchiveDataTable');

    DT.filterContains('fileName', 'archive_file_test_v_');
    I.clickCss('button.buttons-select-all');
    I.clickCss('.buttons-remove');
    DTE.waitForEditor('fileArchiveDataTable');
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see('Táto akcia nie je povolená pre historickú verziu dokumentu.');
    I.click("Zrušiť", "div.DTE_Action_Remove");

    // 10.cronjob
    await SL.setCronjob('*/10', '*');

    // 11. Nahratie opäť novej verzie súboru v buducnosti
    I.say('Phase 11 - Upload another new version of the file in the future');
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', virtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, 'Nahrať novú verziu', versionUploadFifthFileName);
    DTE.clickSwitch('editorFields-saveLater_0');
    const futureTimestamp = SL.getFutureTimestamp(500);
    DTE.fillField('editorFields-dateUploadLater', futureTimestamp);
    DTE.fillField("editorFields-emails", "tester@balat.sk");
    DTE.save('fileArchiveDataTable');

    // 12. Over v čakajúcich súboroch
    I.say("Phase 12 - Verify that the table contains the entry as red");
    DT.filterEquals('virtualFileName', virtualFileName);
    SL.checkStatus(1, 3, ['star', 'map-pin', 'calendar-plus']);
    I.click(DT.btn.archive_edit_button);
    DTE.waitForEditor('fileArchiveDataTable');
    I.clickCss("#pills-dt-fileArchiveDataTable-waitingFiles-tab");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-waitingFiles", 1, ["", virtualFileName, "files/archiv/files/archiv_insert_later/files/archiv/", versionUploadFileName, futureTimestamp]);

    // 13. Editovanie v cakajucich suboroch
    I.say("Phase 13 - editing waiting file");
    I.clickCss('#datatableFieldDTE_Field_editorFields-waitingFiles_wrapper button.buttons-select-all');
    I.click(DT.btn.waiting_edit_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-waitingFiles")
    I.fillField("#datatableFieldDTE_Field_editorFields-waitingFiles_modal #DTE_Field_virtualFileName", virtualFileName + ".chan-ge");
    DTE.save("datatableFieldDTE_Field_editorFields-waitingFiles");
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-waitingFiles", 1, ["", virtualFileName + ".chan-ge", "files/archiv/files/archiv_insert_later/files/archiv/", versionUploadFileName, futureTimestamp]);

    // 14. Zmazanie cakajuceho suboru
    I.say("Phase 14 - delete waiting file");
    I.click(DT.btn.waiting_delete_button);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-waitingFiles");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10);
    DTE.save("fileArchiveDataTable");
    SL.checkStatus(1, 3, ['star', 'map-pin'], ['calendar-plus']);

    // 15. Nahratie opäť novej verzie súboru v buducnosti
    I.say('Phase 15 - Upload another new version of the file in the future');
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', virtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, 'Nahrať novú verziu', versionUploadFifthFileName);
    DTE.clickSwitch('editorFields-saveLater_0');
    DTE.fillField('editorFields-dateUploadLater', SL.getFutureTimestamp(60));
    DTE.fillField("editorFields-emails", "tester@balat.sk");
    DTE.save('fileArchiveDataTable');

    I.say("Wait 60 seconds so file have time to be published");
    I.wait(60);

    // 16. Over, že po dátume a čase sa reálne nahradí
    I.say("Phase16 - Verify that it will be uploaded");
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', virtualFileName);
    SL.checkStatus(1, 3, ['star', 'map-pin'], ['calendar-plus']);
    I.clickCss('button.buttons-select-all');
    I.click(DT.btn.archive_edit_button);
    DTE.waitForEditor('fileArchiveDataTable');
    I.clickCss("#pills-dt-fileArchiveDataTable-waitingFiles-tab");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 10);
});

Scenario('Revert cronjob', async () => {
    await SL.setCronjob('0', '*/5');
});

Scenario('Delete archive entity',  ({I}) => {
    SL.deleteTestFiles();
});

Scenario('Import', async ({ I, DT, DTE}) => {
    await SL.importFile("tests/apps/file-archive/docs/special_action_import.zip");
});

Scenario('Special file upload - Add to version history', async ({ I, DT, DTE }) => {
    const addToHistoryFileName = 'archive_file_test.pdf';
    const addToHistoryFourthVersionFileName = 'archive_file_test_fourth.pdf';

    // 1. Vloženie medzi historické záznamy
    I.say('Phase 1 - Add file to version history');
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', virtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, 'Pridať do histórie verzií', addToHistoryFourthVersionFileName);
    I.clickCss("button[data-id='DTE_Field_editorFields-saveAfterSelect']");
    I.seeElement(locate('span').withText('files/archiv/archive_file_test_v_1.pdf  (poradie : 5)'));
    I.seeElement(locate('span').withText('files/archiv/archive_file_test_v_2.pdf  (poradie : 4)'));
    I.seeElement(locate('span').withText('files/archiv/archive_file_test_v_3.pdf  (poradie : 3)'));
    I.click(locate('span').withText('files/archiv/archive_file_test_v_2.pdf  (poradie : 4)'));
    DTE.save('fileArchiveDataTable');

    // 2. Overenie v datatabuľke
    I.say('Phase 2 - Verify in the data table');
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', virtualFileName);
    DT.filterSelect("editorFields.statusIcons", 'Všetky verzie dokumentu');
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualFileName, "", "files/archiv/", addToHistoryFileName]);
    DT.checkTableRow("fileArchiveDataTable", 2, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(addToHistoryFileName, 1)]);
    DT.checkTableRow("fileArchiveDataTable", 3, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(addToHistoryFileName, 2)]);
    DT.checkTableRow("fileArchiveDataTable", 4, ["", virtualFileName, "", "files/archiv/", SL.getVersionName(addToHistoryFileName, 3)]);
    DT.checkTableRow("fileArchiveDataTable", 5, ["", virtualFileName, "", "files/archiv/", addToHistoryFourthVersionFileName]);
    SL.checkStatus(1, 3, ['star', 'map-pin']);
    SL.checkStatus(2, 3, ['star-off', 'map-pin']);
    SL.checkStatus(3, 3, ['star-off', 'map-pin']);
    SL.checkStatus(4, 3, ['star-off', 'map-pin']);
    SL.checkStatus(5, 3, ['star-off', 'map-pin']);

    // 3. Overenie v Zozname verzií
    I.say('Phase 3 - Verify in List of Versions');
    DT.filterSelect("editorFields.statusIcons", 'Aktuálna verzia dokumentu');
    I.clickCss('button.buttons-select-all');
    I.clickCss('#fileArchiveDataTable_wrapper .buttons-edit');
    DTE.waitForEditor('fileArchiveDataTable');
    I.clickCss('#pills-dt-fileArchiveDataTable-listOfVersions-tab');
    I.clickCss('#fileArchiveDataTable_modal .dt-th-id > .dt-column-order');
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-listOfVersions", 1, ["", virtualFileName, "files/archiv/", SL.getVersionName(addToHistoryFileName, 1), 'Áno', '0', '4']);
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-listOfVersions", 2, ["", virtualFileName, "files/archiv/", SL.getVersionName(addToHistoryFileName, 2), 'Áno', '0', '2']);
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-listOfVersions", 3, ["", virtualFileName, "files/archiv/", SL.getVersionName(addToHistoryFileName, 3), 'Áno', '0', '3']);
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-listOfVersions", 4, ["", virtualFileName, "files/archiv/", addToHistoryFourthVersionFileName, 'Áno', '0', '3']);

    // 4. Overenie, že sa súbor pridal a ostatne subory ostali nezmenene
    I.say('Phase 4 - Verify that files are correctly updated');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(SL.getVersionName(addToHistoryFileName, 3), 'archive_file_test_third.png');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(SL.getVersionName(addToHistoryFileName, 2), 'archive_file_test_second.png');
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(SL.getVersionName(addToHistoryFileName, 1));
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(addToHistoryFourthVersionFileName, 'archive_file_test_fourth.png');
});

Scenario('Delete archiv entity (and file using elfinder if neccesary)', async ({I}) => {
    SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
    I.dontSeeElement(`.elfinder-cwd-filename[title^='archive_file_test']`);
});

Scenario('Special file upload - Replace current file', async ({ I, DTE, DT }) => {
    const fileName = 'archive_file_test.pdf';
    const virtualName = SL.randomName("replace");
    const secondVersionFileName = 'archive_file_test_second.pdf';

    // 1. Pridanie nového súboru do archívu,
    I.say('Phase 1 - Upload new file to the archive');
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(virtualName, fileName);
    DTE.save('fileArchiveDataTable');

    // 2. Nahratie novej verzie súboru
    I.say('Phase 2 - Upload new version of the file');
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', virtualName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, 'Nahradiť aktuálny dokument', secondVersionFileName);
    DTE.save('fileArchiveDataTable');

    // 3. Overenie ci sa spravne editovalo
    I.say('Phase 3 - Verify file version in the data table');
    DT.filterEquals('virtualFileName', virtualName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", virtualName, "", "files/archiv/", fileName]);
    SL.checkStatus(1, 3, ['star', 'map-pin']);

    // 4. Overenie v prieskumnikovi
    I.say('Phase 4 - Verify file content in the file explorer');
    I.amOnPage(SL.elfinder);
    I.dontSeeElement(`.elfinder-cwd-filename[title='${secondVersionFileName}']`);
    await SL.checkFileContent(fileName, 'archive_file_test_second.png');
});

Scenario('Delete archiv entity 2 and file using elfinder if neccesary', async ({I}) => {
    const importFileSelector = ".elfinder-cwd-filename[title^='file_archiv_export_aceintegration']";
    await SL.removeFileByElfinder(importFileSelector);
    SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
});