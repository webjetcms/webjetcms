Feature('apps.file-archive.configurations');

const SL = require("./SL.js");


Before(({ login }) => {
    login('admin');
});

Scenario("Clean fexpost mailbox", async ({ I, TempMail }) => {
    I.say("Cleaning fexpost mailbox");
    await TempMail.destroyInbox("webjetarchive2");
    I.say("Mailbox cleaned");
});

Scenario('Set variable fileArchivAllowExt and verify behaviour', ({ I, DTE, DT, Document}) => {
    const validPdfFileName = 'archive_format.rtf';
    const validPdfVirtualFileName = SL.randomName("validfile");

    const invalidPdfFileName = 'archive_file_test.pdf';
    const invalidPdfVirtualFileName = SL.randomName("invalidfile");

    // 1. Nastavenie konfiguracnej premennej
    I.say("Phase 1 - Set fileArchivAllowExt only for rft format");
    Document.setConfigValue("fileArchivAllowExt", ".rtf");

    // 2. Overenie povolených formátov
    I.say("Phase 2 - Check valid format");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(validPdfVirtualFileName, validPdfFileName);
    DTE.save('fileArchiveDataTable');
    DT.filterEquals('virtualFileName', validPdfVirtualFileName);
    DT.checkTableRow("fileArchiveDataTable", 1, ["", validPdfVirtualFileName, "", "files/archiv/", validPdfFileName]);

    // 3. Overenie nepovelených formátov
    I.say("Phase 3 - Check forbidden format");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(invalidPdfVirtualFileName, invalidPdfFileName);
    DTE.save('fileArchiveDataTable');
    I.waitForText('Chyba: niektoré polia neobsahujú správne hodnoty. Skontrolujte všetky polia na chybové hodnoty (aj v jednotlivých kartách).', 10);
    I.see('Povolené prípony sú: .rtf použili ste súbor s príponou: .pdf');
    DTE.cancel('fileArchiveDataTable');
});

Scenario('Set variable fileArchivAllowExt to default', ({ Document }) => {
    Document.setConfigValue("fileArchivAllowExt", ".pdf,.xlsx,.xls,.doc,.docx,.asc,.xml,.csv")
});

Scenario('Set variable fileArchivCanEdit and verify behaviour', ({ I, DTE, DT, Document}) => {
    const newFileName = 'archive_file_test.pdf';

    Document.setConfigValue("fileArchivCanEdit", "false");

    I.amOnPage(SL.fileArchive);
    DT.filterContains('virtualFileName', 'autotest-validfile-');
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, 'Nahrať novú verziu', newFileName);
    DTE.save('fileArchiveDataTable');
    I.waitForText('Vkladanie dokumentov pri úprave nie je povolené.', 10);
});

Scenario('Set variable fileArchivCanEdit to default', ({ Document }) => {
    Document.setConfigValue("fileArchivCanEdit", "true");

});

Scenario('Set variable fileArchivFromMail and verify behaviour', async ({ I, Document, DTE, TempMail }) => {
    const scheduledDocFileName = 'archive_file_test.pdf';
    const scheduledDocVirtualFileName = SL.randomName("scheduledfile");
    const email = SL.randomName("email") + "@interway.sk";
    Document.setConfigValue("fileArchivFromMail", email);
    await SL.setCronjob('*/10', '*');

    I.amOnPage(SL.fileArchive);
    SL.uploadFile(scheduledDocVirtualFileName, scheduledDocFileName, null , null, null, SL.getFutureTimestamp(30), "webjetarchive2"+TempMail.getTempMailDomain());
    DTE.save('fileArchiveDataTable');
    I.wait(30);

    await TempMail.login("webjetarchive2");
    TempMail.openLatestEmail();
    I.see(`<${email}>`);
});

Scenario('Set variable and Cronjob fileArchivFromMail to default', async ({ I, Document }) => {
    Document.setConfigValue("fileArchivFromMail", "");
    await SL.setCronjob('0', '*/5');
});


Scenario('Delete archiv entities', async ({I}) => {
    SL.deleteTestFiles();
});

Scenario('Set variable fileArchivAllowPatternVersion and verify behaviour', ({ I, DT, DTE, Document }) => {
    // Nastavenie fileArchivAllowPatternVersion na false
    I.say("Phase 1 - Setting fileArchivAllowPatternVersion to false");
    Document.setConfigValue("fileArchivAllowPatternVersion", "false");

    const patternFileName = 'archive_file_test.pdf';
    const patternVirtualFileName = SL.randomName("pattern");
    const patternSecondFileName = 'archive_file_test_third.pdf';
    const validPdfFileName = 'archive_file_test_second.pdf';
    const validPdfVirtualFileName = SL.randomName("validfile");
    I.amOnPage(SL.fileArchive);

    // 2. Pridanie nového súboru do archívu
    I.say("Phase 2 - Add new file to archive");
    SL.uploadFile(validPdfVirtualFileName, validPdfFileName);
    DTE.save('fileArchiveDataTable');

    // 3. Pridanie ďalšieho súboru do archívu a označenie prvého ako vzoru
    I.say("Phase 3 - Add new file to archive and mark first one as pattern");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(patternVirtualFileName, patternFileName);

    // 4. Otvorenie rozšírenej karty a výber referencie na hlavný súbor
    I.say("Phase 4 - Opening advanced tab and selecting reference to main file");
    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.clickCss("#DTE_Field_referenceToMain");
    I.clickCss(`//li[contains(@class, "ui-menu-item")]//div[text()="files/archiv/${validPdfFileName}"]`);
    DTE.save('fileArchiveDataTable');

    // 5. Filtrovanie podľa názvu vzorového súboru a pokus o nahratie novej verzie
    I.say("Phase 5 - Filtering by pattern virtual file name and attempting to upload new version");
    DT.filterEquals('virtualFileName', patternVirtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, "Nahrať novú verziu", patternSecondFileName);
    DTE.save('fileArchiveDataTable');
    I.waitForText('Vzor nemôže mať archív', 10);

    // 6. Povolenie nahrávania novej verzie vzorového súboru a zopakovanie testu
    I.say("Phase 6 - Setting fileArchivAllowPatternVersion to true and retrying file upload");
    Document.setConfigValue("fileArchivAllowPatternVersion", "true");
    I.amOnPage(SL.fileArchive);
    DT.filterEquals('virtualFileName', patternVirtualFileName);
    I.clickCss('button.buttons-select-all');
    SL.editFile(null, null, null, null, "Nahrať novú verziu", patternSecondFileName);
    DTE.save('fileArchiveDataTable');
    I.dontSee('Vzor nemôže mať archív');
});

Scenario('Set variable and Cronjob fileArchivAllowPatternVersion to default', ({ Document }) => {
    Document.setConfigValue("fileArchivAllowPatternVersion", "true");
});

Scenario('Delete archiv entity (and file using elfinder if neccesary)', async ({I, TempMail}) => {
    SL.deleteTestFiles();
    await TempMail.destroyInbox("webjetarchive2");
});