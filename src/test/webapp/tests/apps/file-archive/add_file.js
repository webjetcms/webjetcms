Feature('apps.file-archive.add_file');

const SL = require("./SL.js");

Before(({ login }) => {
    login('admin');
});

Scenario("Clean fexpost mailbox", async ({ I, TempMail }) => {
    I.say("Cleaning fexpost mailbox");
    await TempMail.login("webjetarchive");
    await TempMail.destroyInbox();
    I.say("Mailbox cleaned");
});

Scenario('Add new file to archive and validate upload', async ({ I, DT, DTE }) => {
    const validPdfFileName = 'archive_file_test.pdf';
    const validPdfVirtualFileName = SL.randomName("validfile");
    const duplicatePdfVirtualFileName = SL.randomName("duplicatepdf");
    const invalidPngFileName = 'archive_file_invalid_format.png';
    const invalidPngVirtualFileName = SL.randomName("invalidpdf");

    // 1. Pridanie nového súboru do archívu
    I.say("Phase 1 - Add new file to archive");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(validPdfVirtualFileName, validPdfFileName);

    // 2. Overenie, že sa daju zvoliť iba adresáre od ARCHIV dole
    I.say("Phase 2 - Check that only directories from ARCHIVE down can be selected");
    I.clickCss('#editorAppDTE_Field_editorFields-dir .btn-vue-jstree-item-edit');
    DTE.waitForModal('custom-modal-id');
    await SL.checkTreeStructure();
    I.click('archiv');
    DTE.waitForModalClose('custom-modal-id');

    // 3. Overenie, že nevidim taby ktore tam nemaju byt
    I.say("Phase 3 - Check that I don't see tabs that shouldn't be there");
    I.dontSeeElement(locate('.nav-link').withText('Zoznam verzií'));
    I.dontSeeElement(locate('.nav-link').withText('Čakajúce súbory'));
    DTE.save('fileArchiveDataTable');

    // 4. Overenie, že sa súbor nahrá a je na správnom mieste
    I.say("Phase 4 - Check that the file is uploaded and in the right place");
    DT.filterEquals('virtualFileName', validPdfVirtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", validPdfVirtualFileName, "", "files/archiv/", validPdfFileName]);
    SL.checkStatus(1, 3, ['star', 'map-pin']);

    // 5. Overenie, že súbor má správny typ a obsah
    I.say("Phase 5 - Check that the file has the correct type and content");
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(validPdfFileName);

    // 6. Overenie povolených formátov
    I.say("Phase 6 - Check allowed formats");
    I.amOnPage(SL.fileArchive);
    I.closeOtherTabs();
    SL.uploadFile(invalidPngVirtualFileName, invalidPngFileName);
    DTE.save('fileArchiveDataTable');
    I.waitForText('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).', 10);
    I.see('Povolené prípony sú: .pdf,.xlsx,.xls,.doc,.docx,.asc,.xml,.csv použili ste súbor s príponou: .png');
    DTE.cancel('fileArchiveDataTable');

    // 7. Pokus o nahranie súboru s rovnakým obsahom, ale iným názvom
    I.say("Phase 7 - Attempt to upload a file with the same content but a different name");
    I.amOnPage(SL.fileArchive);
    I.closeOtherTabs();
    SL.uploadFile(duplicatePdfVirtualFileName, validPdfFileName);
    DTE.save('fileArchiveDataTable');
    I.see('Manažér dokumentov zistil, že dokument archive_file_test.pdf ktorý ste nahrali, už v archivu existuje. Ak si želáte uložiť dokument aj tak, zvoľte možnosť "Uložiť dokument aj keď už existuje" v karte "Pokročilé".')

    // 8. Overenie notifikácie s linkami na existujúce súbory
    I.say("Phase 8 - Check notification with links to existing files");
    I.waitForElement("div.toast-message", 10);
    I.clickCss("#toast-container-webjet > div > div.toast-message > a");
    I.wait(2);
    I.switchToNextTab();
    if ("false" !== process.env.CODECEPT_SHOW) I.seeInCurrentUrl(`files/archiv/${validPdfFileName}`);
    I.switchToPreviousTab();
    I.closeOtherTabs();

    // 9. Povolenie možnosti "Uložiť súbor aj keď už existuje"
    I.say("Phase 9 - Enable the option 'Save file even if it already exists'");
    I.clickCss('#pills-dt-fileArchiveDataTable-advanced-tab');
    DTE.clickSwitch('editorFields-uploadRedundantFile_0');
    I.clickCss('#pills-dt-fileArchiveDataTable-basic-tab');
    DTE.save('fileArchiveDataTable');

    // 10. Overenie, že sa rovnaký súbor uložil
    I.say("Phase 10 - Check that the same file was saved");
    DT.filterContains('fileName', validPdfFileName.split('.')[0]);
    I.see('Záznamy 1 až 2 z 2');
    I.clickCss('.dt-th-id > .dt-column-order');
    DT.checkTableRow("fileArchiveDataTable", 2, ["", duplicatePdfVirtualFileName, "", "files/archiv/", SL.getVersionName(validPdfFileName,1)]);
    SL.checkStatus(2, 3, ['star', 'map-pin']);
});

Scenario('Checking icon status and filtering with them works', async ({ I, DT }) => {
    I.amOnPage(SL.fileArchive);
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-editorFields\\.statusIcons button.btn-outline-secondary" });
    const statusOptions = (await I.grabTextFromAll('div.dropdown-menu.show .dropdown-item'))
        .map(option => option.trim())
        .filter(option => option !== "");
    I.click({ css: "div.dt-scroll-headInner div.dt-filter-editorFields\\.statusIcons button.btn-outline-secondary" });

    const statusIcons = {
        'Aktuálna verzia dokumentu':'star',
        'Historická verzia dokumentu':'star-off',
        'Zobrazuje dokument':'map-pin',
        'Nezobrazuje dokument':'map-pin-off',
        'Vzor':'texture',
        'Dokument čaká na nahratie (v budúcnosti)':'calendar-time',
        'Má novú verziu dokumentu, ktorá čaká na nahratie (v budúcnosti)':'calendar-plus'
    }
    for (const statusOption of statusOptions) {
        DT.filterSelect("editorFields.statusIcons", statusOption);
        if (statusOption === 'Všetky verzie dokumentu'){
            continue;
        } else if (await DT.getRecordCount('fileArchiveDataTable') > 0) {
            I.say("")
            SL.checkStatus(1, 3, [statusIcons[statusOption]]);
        }
    }
});

Scenario('Delete archiv entity (and file using elfinder if neccesary)', async ({I}) => {
    SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
});

Scenario('Add new file in the future and validate', async ({ I, DT, DTE, TempMail }) => {
    const scheduledDocFileName = 'archive_file_test.docx';
    const scheduledDocVirtualFileName = SL.randomName("scheduledfile");
    //cronjob
    await SL.setCronjob('*', '*/10');

    // 1. Pridanie nového súboru do archívu
    I.say("Phase1 - Adding new file to archive");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(scheduledDocVirtualFileName, scheduledDocFileName, null , null, null, SL.getFutureTimestamp(60), "webjetarchive"+TempMail.getTempMailDomain());
    DTE.save('fileArchiveDataTable');

    // 2. Over že v tabuľke bude ako červený
    I.say("Phase2 - Verify that the table contains the entry as red");
    DT.filterEquals('virtualFileName', scheduledDocVirtualFileName);
    I.wait(2);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", scheduledDocVirtualFileName, "", "files/archiv/files/archiv_insert_later/files/archiv/", scheduledDocFileName]);
    SL.checkStatus(1, 3, ['star', 'map-pin', 'calendar-time']);
    let lineColor = await SL.getFontColor(1,1);
    I.assertEqual(lineColor, SL.red);

    // 3. Overenie v elfinderi
    I.say("Phase3 - Verify in elfinder");
    I.amOnPage(SL.elfinderLater);
    await SL.checkFileContent(scheduledDocFileName, null, false);

    await SL.setCronjob('*/10', '*');
    I.say("Wait 60 seconds so file have time to be published");
    I.wait(60);

    // 4. Over, že po dátume a čase sa reálne sprístupní
    I.say("Phase4 - Verify that it becomes accessible");
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', scheduledDocVirtualFileName);
    lineColor = await SL.getFontColor(1,1);
    I.assertEqual(lineColor, SL.black);

    // 5. Overenie ze sa subor presunul v elfinderi
    I.say("Phase5 - Verification of move in elFinder");
    I.amOnPage(SL.elfinder);
    await SL.checkFileContent(scheduledDocFileName, null, false);

    // 6. Overenie v emaili
    I.say("Phase6 - Verification in email")
    await TempMail.login("webjetarchive");
    TempMail.openLatestEmail();

    I.say("Check mail content");
    I.see("Súbor bol úspešne nahraný - " + scheduledDocVirtualFileName);
    I.see("Nasledujúci súbor bol úspešne nahraný:");
    I.see("Názov: " + scheduledDocVirtualFileName);
    I.see("Meno súboru: " + scheduledDocFileName);

    TempMail.deleteCurrentEmail();
});

Scenario('Add file in different location', ({ I, DTE, DT }) => {
    const validPdfFileName = 'archive_file_test_second.pdf';
    const validPdfVirtualFileName = SL.randomName("locationfile");
    const folderName = SL.randomName("folder");
    // 1. Pridanie nového súboru do archívu
    I.say("Phase 1 - Add new file to archive");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(validPdfVirtualFileName, validPdfFileName);
    I.fillField("#editorAppDTE_Field_editorFields-dir .input-group input", "/files/archiv/" + folderName);
    DTE.save('fileArchiveDataTable');

    // 2. Overenie, že sa súbor nahrá a je na správnom mieste
    I.say("Phase 2 - Check that the file is uploaded and in the right place");
    DT.filterEquals('virtualFileName', validPdfVirtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", validPdfVirtualFileName, "", "files/archiv/" + folderName, validPdfFileName]);
    SL.checkStatus(1, 3, ['star', 'map-pin']);

    I.amOnPage(SL.elfinder);
    I.doubleClick(`.elfinder-cwd-filename[title^="${folderName}"]`);
    SL.checkFileContent(validPdfFileName, null, false);
});

Scenario('Delete archiv entity 2 (and file using elfinder if neccesary)', async ({I}) => {
    SL.deleteTestFiles();

    const folderSelector = ".elfinder-cwd-filename[title^='autotest--']";
    await SL.removeFileByElfinder(folderSelector, SL.elfinderLater);

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector, SL.elfinderLater);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
});

Scenario('Revert cronjob setting', async () => {
    await SL.setCronjob('0', '*/5');
});

