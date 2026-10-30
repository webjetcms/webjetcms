// Configuration changes and cleanup must run together without parallel archive tests.
Feature('apps.file-archive.file_archive_app').tag('@singlethread');

const SL = require("./SL.js");

const HISTORY_VALIDITY_CONFIG = 'fileArchivCheckHistoryValid';
const PATTERN_VERSION_CONFIG = 'fileArchivAllowPatternVersion';
const HISTORY_TABLE = 'datatableFieldDTE_Field_editorFields-listOfVersions';
const WAITING_TABLE = 'datatableFieldDTE_Field_editorFields-waitingFiles';
const HISTORY_NAME_PREFIX = 'autotest-history-valid-';
const WAITING_VERSION_STATUS = 'Má novú verziu dokumentu, ktorá čaká na nahratie (v budúcnosti)';
const FILE_ARCHIVE_APP_URL = '/apps/manazer-dokumentov/archiv-suborov-test-behaviour.html';
let historyName;
let historyCurrentUrl;
let historyUrls;
let historyInvariantUrls;
let pendingHistoryUrl;

let randomNumber;
let NO_ARCHIVE_DOCUMENTS = "Žiadne archívne verzie alebo vzory pre tento dokument.";
let ARCHIVE_DOCUMENTS = "Archívne verzie";

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
    }
});

Scenario('Import', async () => {
    await SL.importFile("tests/apps/file-archive/docs/mvc_app_import.zip");
}).tag('@history-validity');

Scenario('File archive app - inserting an app', async ({ I, DTE, Apps, Document }) => {
    const docId = '120040';
    Apps.clearPageContent(docId);
    Apps.save();
    Apps.insertApp('Manažér dokumentov', '#components-file_archiv-name', docId);

    const defaultParams = {
        dir: "/files/archiv/",
        subDirsInclude: 'false',
        productCode: "",
        product: "",
        category: "",
        showOnlySelected: 'false',
        globalIds: "",
        orderMain: "priority",
        ascMain: 'true',
        open: 'false',
        archiv: 'false',
        order: "priority",
        asc: 'true',
        showPatterns: 'false',
        orderPatterns: "priority",
        ascPatterns: 'true'
    };

    I.say("Set default APP params");
    await Apps.assertParams(defaultParams);
    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    ['autotest-app-main-history', 'autotest-app-basic', 'autotest-app-main', 'autotest-app-history'].forEach((file) => {
        I.seeElement(locate(".table tr td span").withText(file));
    });
    I.dontSeeElement(locate(".table tr td span").withText("autotest-app-subfolder"));

    I.switchToPreviousTab();

    Apps.openAppEditor();

    I.checkOption("#DTE_Field_subDirsInclude_0");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    I.say("Set param subDirsInclude to true");
    await Apps.assertParams({subDirsInclude: 'true'});

    I.say('Changed parameters visual testing');
    Apps.save();
    I.switchToNextTab();

    I.seeElement(locate(".table tr td span").withText("autotest-app-subfolder"));

    I.switchToPreviousTab();

    Apps.openAppEditor();
    I.uncheckOption("#DTE_Field_subDirsInclude_0");
    DTE.fillField("productCode", "product code");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')
    I.say("Set param productCode to 'product code'");
    await Apps.assertParams({productCode: 'product code'});
    I.say('Changed parameters visual testing');

    Apps.save();
    I.switchToNextTab();
    I.seeElement(locate(".table tr td span").withText("autotest-app-product_code"));
    I.switchToPreviousTab();

    //

    Apps.openAppEditor();
    DTE.fillField("productCode", "");
    DTE.fillField("product", "product");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.say("Set param product to 'product'");
    await Apps.assertParams({product: 'product'});

    I.say('Changed parameters visual testing');

    Apps.save();
    I.switchToNextTab();
    I.seeElement(locate(".table tr td span").withTextEquals("autotest-app-product"));
    I.switchToPreviousTab();

    Apps.openAppEditor();
    DTE.fillField("product", "");
    DTE.fillField("category", "category");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.say("Set param category to 'category'");
    await Apps.assertParams({category: 'category'});

    I.say('Changed parameters visual testing');
    Apps.save();
    I.switchToNextTab();
    I.seeElement(locate(".table tr td span").withTextEquals("autotest-app-category"));
    I.switchToPreviousTab();

    //
    Apps.openAppEditor();
    I.clearField("#DTE_Field_category");
    I.clickCss("#editorAppDTE_Field_dir");
    I.seeElement('#DTE_Field_globalIds[disabled]');
    I.checkOption("#DTE_Field_showOnlySelected_0");
    I.dontSeeElement('#DTE_Field_globalIds[disabled]');
    ['#editorAppDTE_Field_dir input', '#DTE_Field_subDirsInclude_0', '#DTE_Field_productCode', '#DTE_Field_product', ].forEach((field) => {
        I.seeElement(`${field}[disabled]`);
    })

    DTE.fillField('globalIds', '1496');
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.say("Set param showOnlySelected to true");
    await Apps.assertParams({showOnlySelected: 'true'});

    I.say('Changed parameters visual testing');
    Apps.save();
    I.switchToNextTab();
    I.seeElement(locate(".table tr td span").withText("app-chosen_file"));
    I.switchToPreviousTab();

    //

    Apps.openAppEditor();

    I.uncheckOption("#DTE_Field_showOnlySelected_0");
    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    const sortOptions = ['priority', 'time', 'validFrom', 'validTo', 'virtual_file_name',
        'fileName', 'productCode', 'product', 'category', 'fileSize'];
    for (const field of ['orderMain', 'order', 'orderPatterns']) {
        const options = await I.grabAttributeFromAll(`#DTE_Field_${field} option`, 'value');
        I.assertDeepEqual(options, sortOptions, `Unexpected sorting options for ${field}`);
    }
    DTE.selectOption("orderMain", "Názov dokumentu");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.say("Set params orderMain to 'virtual_file_name' AND ascMain to 'true'");
    await Apps.assertParams({orderMain: 'virtual_file_name', ascMain : 'true'});

    I.say('Changed parameters visual testing');
    Apps.save();
    I.switchToNextTab();
    let names = await I.grabTextFromAll('div.table-responsive > table.table.tabulkaStandard > tbody > tr.collapsible > td:nth-child(1) > span');
    let sortedNamesAsc = [...names].sort((a, b) => a.localeCompare(b, 'sk'));
    I.assertDeepEqual(names, sortedNamesAsc, 'Table is not sorted correctly!');
    I.switchToPreviousTab();
    //

    Apps.openAppEditor();

    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    I.uncheckOption('#DTE_Field_ascMain_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.say("Set params orderMain to 'virtual_file_name' AND ascMain to 'false'");
    await Apps.assertParams({orderMain: 'virtual_file_name', ascMain:'false'});

    I.say('Changed parameters visual testing');
    Apps.save();
    I.switchToNextTab();
    let names2 = await I.grabTextFromAll('//table[@class="table tabulkaStandard"]//tr/td[1]/span');
    names2 = names2.filter(name => name !== NO_ARCHIVE_DOCUMENTS);
    let sortedNamesDesc = [...sortedNamesAsc].reverse();
    I.assertDeepEqual(names2, sortedNamesDesc, 'Table is not sorted correctly!');
    I.switchToPreviousTab();
    //

    Apps.openAppEditor();
    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    I.checkOption('#DTE_Field_open_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.say("Set param open to 'true'");
    await Apps.assertParams({open : 'true'});

    I.say('Changed parameters visual testing');
    Apps.save();
    I.switchToNextTab();
    I.seeElement(locate(".table tr td span").withText(NO_ARCHIVE_DOCUMENTS));
    I.dontSeeElement(locate(".table tr td span").withText(ARCHIVE_DOCUMENTS));
    I.dontSeeElement(locate(".table tr td span").withText("Vzory dokumentu"));
    I.switchToPreviousTab();

    Apps.openAppEditor();
    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    I.checkOption('#DTE_Field_archiv_0');
    I.checkOption('#DTE_Field_showPatterns_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.say("Set param open to 'true'");
    await Apps.assertParams({open : 'true'});

    I.say('Changed parameters visual testing');
    Apps.save();
    I.switchToNextTab();
    I.seeElement(locate(".table tr td span").withText(NO_ARCHIVE_DOCUMENTS));
    I.seeElement(locate(".table tr td span").withText(NO_ARCHIVE_DOCUMENTS));
    I.seeElement(locate(".table tr td span").withText(ARCHIVE_DOCUMENTS));
    I.seeElement(locate(".table tr td span").withText("Vzory dokumentu"));
    I.switchToPreviousTab();
}).tag('@history-validity');

Scenario('File archive app - structure and behaviour test', ({ I }) => {
    //
    I.say('Checking file autotest-app-basic');
    I.amOnPage('/apps/manazer-dokumentov/archiv-suborov-test-behaviour.html');
    I.waitForElement(".table.tabulkaStandard", 10);
    I.click(locate(".table tr td span").withText("autotest-app-basic"));
    I.waitForElement(locate('tr[id^=detail] > td > span').withText(NO_ARCHIVE_DOCUMENTS), 10);
    I.click(locate(".table tr td span").withText("autotest-app-basic"));

    //
    I.say('Checking file autotest-app-main');
    I.click(locate(".table tr td span").withText("autotest-app-main"));
    I.waitForElement(locate('tr[id^=detail] > td tbody tr > th > span').withText("Vzory dokumentu"), 10);
    I.see("autotest-app-pattern",'tr[id^=detail] > td tbody tr td:nth-of-type(1) > span');
    I.click(locate(".table tr td span").withText("autotest-app-main"));

    //
    I.say('Checking file autotest-app-history');
    I.click(locate(".table tr td span").withText("autotest-app-history"));
    I.waitForElement(locate('tr[id^=detail] > td tbody tr > th > span').withText(ARCHIVE_DOCUMENTS), 10);
    I.see("autotest-app-history",'tr[id^=detail] > td tbody tr td:nth-of-type(1) > span');
    I.click(locate(".table tr td span").withText("autotest-app-history"));

    //
    I.say('Checking file autotest-app-main-history');
    I.click(locate(".table tr td span").withText("autotest-app-main-history"));
    I.waitForElement(locate('tr[id^=detail] > td tbody tr > th > span').withText(ARCHIVE_DOCUMENTS), 10);
    I.waitForElement(locate('tr[id^=detail] > td tbody tr > th > span').withText("Vzory dokumentu"), 10);
    I.see("autotest-app-main-history", 'tr[id^=detail] > td tbody tr td:nth-of-type(1) > span');
    I.see("autotest-app-pattern2", 'tr[id^=detail] > td tbody tr td:nth-of-type(1) > span');
    I.click(locate(".table tr td span").withText("autotest-app-main-history"));
});

Scenario('File archive app - open files test', async ({ I, Document }) => {
    I.closeOtherTabs();
    I.amOnPage('/apps/manazer-dokumentov/archiv-suborov-test-behaviour.html');
    I.waitForElement(".table.tabulkaStandard", 10);

    const testCases = [
        { name: 'autotest-app-main-history', fileName: 'archive_export_import.pdf', baseFile: 'archive_replace.png' },
        { name: 'autotest-app-basic', fileName: 'archive_file_test.pdf', baseFile: 'archive_file_test.png' },
        { name: 'autotest-app-main', fileName: 'archive_file_test_fourth.pdf', baseFile: 'archive_file_test_fourth.png' },
        { name: 'autotest-app-history', fileName: 'archive_file_test_second.pdf', baseFile: 'archive_file_test_third.png' },
    ];

    for (const testCase of testCases) {
        I.click(locate('tr').withChild(locate('td span').withTextEquals(testCase.name)).find('td > a'));
        await SL.checkScreenshot(testCase.fileName, testCase.baseFile);
        I.switchToPreviousTab();
        I.closeOtherTabs();
    }
});

Scenario('Delete archiv entity (and file using elfinder if neccesary)', async ({I}) => {
    const importFileSelector = ".elfinder-cwd-filename[title^='file_archiv_export_aceintegration']";
    await SL.removeFileByElfinder(importFileSelector);

    await SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
}).tag('@history-validity');

Scenario('Test google non index header in files',async ({ I, DT, DTE }) => {
    const filePath = "/files/archiv/nonindexheader.pdf";
    I.amOnPage(SL.fileArchive);

    // First setting - do indexation
    I.say("First setting - do indexation");
    setValidationAndIndexation(I, DT, DTE, "", "", true);
    let response1 = await I.sendGetRequest(filePath);
    I.assertEqual(response1.headers["x-robots-tag"], undefined);

    // Second setting - indexation turn off
    I.say("Second setting - indexation turn off");
    setValidationAndIndexation(I, DT, DTE, "", "", false);
    let response2 = await I.sendGetRequest(filePath);
    I.assertEqual(response2.headers["x-robots-tag"], "noindex, nofollow");

    // Third setting - indexation turn on BUT not valid date
    I.say("Third setting - indexation turn on BUT not valid date -> forward to 404 page");
    setValidationAndIndexation(I, DT, DTE, "01.01.2045 00:00:00", "", true);
    I.amOnPage(filePath);
    I.see("Chyba 404 - požadovaná stránka neexistuje");

    // Fourth setting - indexation turn on AND valid date
    I.say("Fourth setting - indexation turn on AND valid date");
    I.amOnPage(SL.fileArchive);
    setValidationAndIndexation(I, DT, DTE, "01.01.2023 00:00:00", "01.01.2045 00:00:00", true);
    let response4 = await I.sendGetRequest(filePath);
    I.assertEqual(response4.headers["x-robots-tag"], undefined);
});

Scenario('Test full text indexation', ({ I, DT, DTE }) => {
    const fileName = "Test non-index header";
    const fileBody = "File to test inserting of non index header";

    I.amOnPage(SL.fileArchive);

    I.say("Set file for indexation");
    setValidationAndIndexation(I, DT, DTE, "", "", true);

    I.say("Check, that indexation file was crated");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=69200");
    DT.waitForLoader();
    DT.filterEquals("title", fileName);
    I.see(fileName);

    I.say("Now try find indexed file by body text");
    I.amOnPage("/apps/vyhladavanie/vyhladavanie-files.html");
    I.waitForElement("#searchWords", 10);
    I.fillField("#searchWords", fileBody);
    I.clickCss("input.smallSearchSubmit");
    I.waitForText(fileBody, 10);

    I.say("Now, turn off indexation");
    I.amOnPage(SL.fileArchive);
    setValidationAndIndexation(I, DT, DTE, "", "", false);

    I.say("Check, that we wont find indexation file");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=69200");
    DT.waitForLoader();
    DT.filterEquals("title", fileName);
    I.dontSee(fileName);

    I.say("Check that we wont find indexed file by body text");
    I.amOnPage("/apps/vyhladavanie/vyhladavanie-files.html");
    I.waitForElement("#searchWords", 10);
    //spam protection
    I.wait(10);
    I.fillField("#searchWords", fileBody);
    I.clickCss("input.smallSearchSubmit");
    I.waitForText("Neboli nájdené žiadne stránky vyhovujúce zadaným kritériám.", 10);
});

function setValidationAndIndexation(I, DT, DTE, validFrom, validTo, indexation) {
    const fileName = "Test non-index header";

    DT.filterEquals("virtualFileName", fileName);
    I.click(fileName);
    DTE.waitForEditor('fileArchiveDataTable');

    I.fillField("#DTE_Field_validFrom", validFrom);
    I.fillField("#DTE_Field_validTo", validTo);

    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    if(indexation == true) {
        I.checkOption("#DTE_Field_indexFile_0");
    } else {
        I.uncheckOption("#DTE_Field_indexFile_0");
    }

    DTE.save();
}

Scenario('File archive app - only selected files', async ({ I, DT, DTE, Apps, Document }) => {
    I.closeOtherTabs();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=62096");

    I.say("Open app in new page");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-content-tab');
    DTE.waitForCkeditor();
    Apps.openAppForInsert('Manažér dokumentov', '#components-file_archiv-name');

    I.checkOption("#DTE_Field_showOnlySelected_0");
    I.fillField("#DTE_Field_globalIds", "1496,1423");

    I.say("Check that files are marked selected in table - the rest is not selected");
    I.clickCss("#pills-dt-component-datatable-filesToShow-tab");
    await checkSelectedFiles(I, ["1496", "1423"]);

    I.say("Check, that I cant see files that are not specific for this table");
    DT.filterContains('virtualFileName', 'ScreenshotFile');
    I.see("ScreenshotFile_C");
    I.dontSee("ScreenshotFile_Pattern"); // Don't see because it's pattern
    I.dontSee("ScreenshotFile_A"); // don't see becuse it's in history version, not main
    I.dontSee("ScreenshotFile_B"); // don't see becuse it's in history version, not main
    I.dontSee("ScreenshotFile_Future"); // don't see because it's Future version, not active

    I.say("Cancel filter and check that files are still selected");
    DT.filterContains('virtualFileName', '');
    await checkSelectedFiles(I, ["1496", "1423"]);

    I.say("try add files from table, then check duplicity");
    selectRowsByGlobalId(I, ["1496", "1423", "1422"]);
    I.clickCss("#datatableFieldDTE_Field_filesToSelect_wrapper button.btn-success");
    I.clickCss("#pills-dt-component-datatable-basic-tab");
    I.seeInField("#DTE_Field_globalIds", "1496,1423,1422");

    I.say("Go back, do check and try removing files");
    I.clickCss("#pills-dt-component-datatable-filesToShow-tab");
    await checkSelectedFiles(I, ["1496", "1423", "1422"]);
    // plus one that was never selected
    selectRowsByGlobalId(I, ["1496", "422"]);
    I.clickCss("#datatableFieldDTE_Field_filesToSelect_wrapper button.btn-danger");
    I.clickCss("#pills-dt-component-datatable-basic-tab");
    I.seeInField("#DTE_Field_globalIds", "1423,1422");

    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    DTE.selectOption("orderMain", "Meno súboru");
    I.checkOption("#DTE_Field_ascMain_0");

    I.say("OK app and check page preview");
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    await Apps.assertParams({orderMain: 'fileName', ascMain: 'true'});

    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    const shownFilesCount = await I.grabNumberOfVisibleElements("table.tabulkaStandard tr.collapsible");
    I.assertEqual(shownFilesCount, 2, "Number of shown files in preview is different than expected");

    const links = 'table.tabulkaStandard tr.collapsible > td > a';
    const ascendingLinks = await I.grabAttributeFromAll(links, 'href');
    const fileNames = ascendingLinks.map(link => link.split('?')[0].split('/').pop().toLowerCase());
    I.assertDeepEqual(fileNames, [...fileNames].sort(), 'Selected files must be sorted by file name');
    I.assertNotEqual(fileNames[0], fileNames[1], 'Sorting needs two different file names');

    I.switchToPreviousTab();
    Apps.openAppEditor();
    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    I.seeInField("#DTE_Field_orderMain", "fileName");
    I.uncheckOption("#DTE_Field_ascMain_0");
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    await Apps.assertParams({orderMain: 'fileName', ascMain: 'false'});
    I.closeOtherTabs();
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForElement(links, 10);
    const descendingLinks = await I.grabAttributeFromAll(links, 'href');
    I.assertDeepEqual(descendingLinks, [...ascendingLinks].reverse(), 'Selected files must respect descending order');
    I.switchToPreviousTab();
    I.closeOtherTabs();
});

Scenario('Clean earlier history validity fixtures', async ({ I, DT, DTE, Document }) => {
    historyName = SL.randomName('history-valid');
    historyUrls = {};
    historyInvariantUrls = {};
    pendingHistoryUrl = null;

    Document.setConfigValue(HISTORY_VALIDITY_CONFIG, 'true', true);
    Document.setConfigValue(PATTERN_VERSION_CONFIG, 'true', true);
    await deleteWaitingHistoryVersions(I, DT, DTE);
    await SL.deleteTestFiles('history-valid-');
}).tag('@history-validity');

Scenario('Create the historical-version validity matrix', async ({ I, DT, DTE }) => {
    SL.openFileArchive('/files/archiv/archive_file_test.pdf');
    SL.uploadFile(historyName, 'archive_file_test.pdf');
    DTE.fillField('validFrom', '01.01.2000 00:00:00');
    I.pressKey('Enter');
    DTE.fillField('validTo', '01.01.2001 00:00:00');
    I.pressKey('Enter');
    I.clickCss('#pills-dt-fileArchiveDataTable-advanced-tab');
    I.checkOption('#DTE_Field_editorFields-uploadRedundantFile_0');
    DTE.save('fileArchiveDataTable', true);

    // Set metadata before archiving, so the test does not need history-edit permissions.
    for (const version of [
        { file: 'archive_file_test_second.pdf', from: '01.01.2099 00:00:00', to: '01.01.2100 00:00:00', visible: true },
        { file: 'archive_file_test_third.pdf', from: '', to: '', visible: false },
        { file: 'archive_file_test_fourth.pdf', from: '', to: '', visible: true },
        { file: 'archive_file_test_fifth.pdf', from: '', to: '', visible: true }
    ]) {
        DT.filterEquals('virtualFileName', historyName);
        await SL.selectAll();
        SL.editFile(null, null, null, null, 'Nahrať novú verziu', version.file);
        DTE.fillField('validFrom', version.from);
        I.pressKey('Enter');
        DTE.fillField('validTo', version.to);
        I.pressKey('Enter');
        I.clickCss('#pills-dt-fileArchiveDataTable-advanced-tab');
        if (version.visible) I.checkOption('#DTE_Field_showFile_0');
        else I.uncheckOption('#DTE_Field_showFile_0');
        I.checkOption('#DTE_Field_editorFields-uploadRedundantFile_0');
        DTE.save('fileArchiveDataTable', true);
    }

    I.click(locate('#fileArchiveDataTable .dt-row-edit a').withText(historyName));
    DTE.waitForEditor('fileArchiveDataTable');
    const fileName = await I.grabValueFrom('#DTE_Field_fileName');
    const filePath = await I.grabValueFrom('#DTE_Field_filePath');
    historyCurrentUrl = '/' + filePath + fileName;
    I.clickCss('#pills-dt-fileArchiveDataTable-listOfVersions-tab');
    DT.waitForLoader(HISTORY_TABLE);
    I.waitForText(historyName, 10, '#' + HISTORY_TABLE);
    const versions = await I.executeScript((tableId) => {
        return $('#' + tableId).DataTable().rows().data().toArray().map(row => ({
            filePath: row.filePath, fileName: row.fileName, showFile: row.showFile,
            validFrom: row.validFrom, validTo: row.validTo
        }));
    }, HISTORY_TABLE);
    I.assertEqual(versions.length, 4);
    const versionUrl = number => {
        const version = versions.find(row => row.fileName === SL.getVersionName(fileName, number));
        I.assertTrue(Boolean(version), 'Historical version ' + number + ' was not found');
        return '/' + version.filePath + version.fileName;
    };
    historyUrls = {
        expired: versionUrl(1),
        future: versionUrl(2),
        hidden: versionUrl(3),
        valid: versionUrl(4)
    };
    I.assertEqual(versions.filter(version => version.showFile === false).length, 1);
    I.assertEqual(versions.filter(version => version.validFrom && version.validTo).length, 2);
    DTE.cancel('fileArchiveDataTable');
    await checkDownload(I, historyCurrentUrl, 200);
}).tag('@history-validity');

Scenario('Create current, pattern and pending-upload invariants', async ({ I, DT, DTE }) => {
    const futureFrom = '01.01.2099 00:00:00';
    const futureTo = '01.01.2100 00:00:00';

    const futureMainName = historyName + '-current-future';
    SL.openFileArchive('/files/archiv/archive_replace.pdf');
    SL.uploadFile(futureMainName, 'archive_replace.pdf', futureFrom, futureTo);
    I.clickCss('#pills-dt-fileArchiveDataTable-advanced-tab');
    I.checkOption('#DTE_Field_editorFields-uploadRedundantFile_0');
    DTE.save('fileArchiveDataTable', true);
    historyInvariantUrls.futureMain = await getCurrentFileUrl(I, DT, DTE, futureMainName);

    const hiddenMainName = historyName + '-current-hidden';
    SL.openFileArchive('/files/archiv/archive_file_test_fourth.pdf');
    SL.uploadFile(hiddenMainName, 'archive_file_test_fourth.pdf');
    I.clickCss('#pills-dt-fileArchiveDataTable-advanced-tab');
    I.uncheckOption('#DTE_Field_showFile_0');
    I.checkOption('#DTE_Field_editorFields-uploadRedundantFile_0');
    DTE.save('fileArchiveDataTable', true);
    historyInvariantUrls.hiddenMain = await getCurrentFileUrl(I, DT, DTE, hiddenMainName);

    const mainPath = historyCurrentUrl.substring(1);
    const versionedPatternName = historyName + '-pattern-versioned';
    await createPatternFixture(
        I, DT, DTE, versionedPatternName, 'archive_export_import.pdf', mainPath,
        '01.01.2000 00:00:00', '01.01.2001 00:00:00');

    for (const sourceFile of ['archive_file_test_third.pdf', 'archive_file_test_fourth.pdf']) {
        SL.openFileArchive('/files/archiv/archive_export_import.pdf');
        DT.filterEquals('virtualFileName', versionedPatternName);
        await SL.selectAll();
        SL.editFile(null, null, null, null, 'Nahrať novú verziu', sourceFile);
        DTE.fillField('validFrom', '');
        I.pressKey('Enter');
        DTE.fillField('validTo', '');
        I.pressKey('Enter');
        I.clickCss('#pills-dt-fileArchiveDataTable-advanced-tab');
        I.checkOption('#DTE_Field_editorFields-uploadRedundantFile_0');
        DTE.save('fileArchiveDataTable', true);
    }

    DT.filterEquals('virtualFileName', versionedPatternName);
    I.click(locate('#fileArchiveDataTable .dt-row-edit a').withText(versionedPatternName));
    DTE.waitForEditor('fileArchiveDataTable');
    const currentPatternFileName = await I.grabValueFrom('#DTE_Field_fileName');
    const currentPatternFilePath = await I.grabValueFrom('#DTE_Field_filePath');
    historyInvariantUrls.validPattern = '/' + currentPatternFilePath + currentPatternFileName;
    I.clickCss('#pills-dt-fileArchiveDataTable-listOfVersions-tab');
    DT.waitForLoader(HISTORY_TABLE);
    I.waitForText(versionedPatternName, 10, '#' + HISTORY_TABLE);
    const patternVersions = await I.executeScript((tableId) => {
        return $('#' + tableId).DataTable().rows().data().toArray().map(row => ({
            filePath: row.filePath, fileName: row.fileName,
            validFrom: row.validFrom, validTo: row.validTo
        }));
    }, HISTORY_TABLE);
    I.assertEqual(patternVersions.length, 2);
    const patternVersionUrl = number => {
        const version = patternVersions.find(row => row.fileName === SL.getVersionName(currentPatternFileName, number));
        I.assertTrue(Boolean(version), 'Historical pattern version ' + number + ' was not found');
        return '/' + version.filePath + version.fileName;
    };
    I.assertEqual(patternVersions.filter(version => version.validFrom && version.validTo).length, 1);
    historyInvariantUrls.expiredPatternHistory = patternVersionUrl(1);
    historyInvariantUrls.validPatternHistory = patternVersionUrl(2);
    DTE.cancel('fileArchiveDataTable');

    historyInvariantUrls.futurePattern = await createPatternFixture(
        I, DT, DTE, historyName + '-pattern-future', 'archive_file_test_second.pdf', mainPath, futureFrom, futureTo);

    SL.openFileArchive('/files/archiv/archive_file_test.pdf');
    DT.filterEquals('virtualFileName', historyName);
    await SL.selectAll();
    SL.editFile(null, null, null, null, 'Nahrať novú verziu', 'archive_file_test_fifth.pdf');
    DTE.clickSwitch('editorFields-saveLater_0');
    DTE.fillField('editorFields-dateUploadLater', '01.01.2100 00:00:00');
    I.clickCss('#pills-dt-fileArchiveDataTable-advanced-tab');
    I.checkOption('#DTE_Field_editorFields-uploadRedundantFile_0');
    DTE.save('fileArchiveDataTable', true);

    DT.filterEquals('virtualFileName', historyName);
    I.click(locate('#fileArchiveDataTable .dt-row-edit a').withText(historyName));
    DTE.waitForEditor('fileArchiveDataTable');
    I.clickCss('#pills-dt-fileArchiveDataTable-waitingFiles-tab');
    DT.waitForLoader(WAITING_TABLE);
    I.waitForText(historyName, 10, '#' + WAITING_TABLE);
    const waitingVersions = await I.executeScript((tableId) => {
        return $('#' + tableId).DataTable().rows().data().toArray().map(row => ({
            filePath: row.filePath, fileName: row.fileName
        }));
    }, WAITING_TABLE);
    I.assertEqual(waitingVersions.length, 1);
    pendingHistoryUrl = '/' + waitingVersions[0].filePath + waitingVersions[0].fileName;
    DTE.cancel('fileArchiveDataTable');
}).tag('@history-validity');

Scenario('Apply history validity configuration immediately and preserve visibility', async ({ I, Document }) => {
    const { expired, future, hidden, valid } = historyUrls;
    const { futureMain, hiddenMain, validPattern, expiredPatternHistory, validPatternHistory, futurePattern } = historyInvariantUrls;
    for (const checkHistory of [true, false, true]) {
        Document.setConfigValue(HISTORY_VALIDITY_CONFIG, String(checkHistory), true);
        await checkDownload(I, expired, checkHistory ? 404 : 200);
        await checkDownload(I, future, checkHistory ? 404 : 200);
        await checkDownload(I, hidden, 404);
        await checkDownload(I, valid, 200);
        await checkDownload(I, historyCurrentUrl, 200);
        await checkDownload(I, futureMain, 404);
        await checkDownload(I, hiddenMain, 404);
        await checkDownload(I, validPattern, 200);
        await checkDownload(I, expiredPatternHistory, checkHistory ? 404 : 200);
        await checkDownload(I, validPatternHistory, 200);
        await checkDownload(I, futurePattern, 404);

        I.amOnPage(FILE_ARCHIVE_APP_URL);
        I.waitForText(historyName, 10, '.file-archive-app');
        checkAppFileVisibility(I, expired, !checkHistory);
        checkAppFileVisibility(I, future, !checkHistory);
        checkAppFileVisibility(I, hidden, false);
        checkAppFileVisibility(I, valid, true);
        checkAppFileVisibility(I, historyCurrentUrl, true);
        checkAppFileVisibility(I, futureMain, false);
        checkAppFileVisibility(I, hiddenMain, false);
        checkAppFileVisibility(I, validPattern, true);
        checkAppFileVisibility(I, expiredPatternHistory, false);
        checkAppFileVisibility(I, validPatternHistory, false);
        checkAppFileVisibility(I, futurePattern, false);
        checkAppFileVisibility(I, pendingHistoryUrl, false);
    }
}).tag('@history-validity');

Scenario('Restore history validity configuration and remove test documents', async ({ I, DT, DTE, Document }) => {
    Document.setConfigValue(HISTORY_VALIDITY_CONFIG, 'true', true);
    Document.setConfigValue(PATTERN_VERSION_CONFIG, 'true', true);
    await deleteWaitingHistoryVersions(I, DT, DTE);
    await SL.deleteTestFiles('history-valid-');
    const deletedUrls = [historyCurrentUrl, pendingHistoryUrl, ...Object.values(historyUrls || {}), ...Object.values(historyInvariantUrls || {})]
        .filter(Boolean);
    for (const url of deletedUrls) await checkDownload(I, url, 404);
}).tag('@history-validity');

/** Removes scheduled replacements before their published parent records are deleted. */
async function deleteWaitingHistoryVersions(I, DT, DTE) {
    SL.openFileArchive('/files/archiv/cleanup.pdf');
    DT.filterStartsWith('virtualFileName', HISTORY_NAME_PREFIX);
    DT.filterSelect('editorFields.statusIcons', WAITING_VERSION_STATUS);
    const recordCount = await DT.getRecordCount('fileArchiveDataTable');

    for (let index = 0; index < recordCount; index++) {
        SL.openFileArchive('/files/archiv/cleanup.pdf');
        DT.filterStartsWith('virtualFileName', HISTORY_NAME_PREFIX);
        DT.filterSelect('editorFields.statusIcons', WAITING_VERSION_STATUS);
        I.click(locate('#fileArchiveDataTable .dt-row-edit a').first());
        DTE.waitForEditor('fileArchiveDataTable');
        I.clickCss('#pills-dt-fileArchiveDataTable-waitingFiles-tab');
        DT.waitForLoader(WAITING_TABLE);

        if (await DT.getRecordCount(WAITING_TABLE) > 0) {
            I.clickCss('#' + WAITING_TABLE + '_wrapper button.buttons-select-all');
            I.clickCss('#' + WAITING_TABLE + '_wrapper button.buttons-remove');
            DTE.waitForEditor(WAITING_TABLE);
            I.click('Zmazať', 'div.DTE_Action_Remove');
            I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 10, '#' + WAITING_TABLE);
        }
        DTE.save('fileArchiveDataTable');
    }
}

/** Checks the HTTP response so a rendered error page cannot pass as a downloaded document. */
async function checkDownload(I, url, status) {
    const response = await I.sendGetRequest(url);
    I.assertEqual(response.status, status, 'Unexpected download status for ' + url);
    if (status === 200) I.assertContain(response.headers['content-type'], 'application/pdf');
}

/** Checks that the page application only renders links available for download. */
function checkAppFileVisibility(I, url, visible) {
    const selector = `.file-archive-app a[href^="${url}?v="]`;
    if (visible) I.seeElementInDOM(selector);
    else I.dontSeeElementInDOM(selector);
}

/** Returns the public URL stored for the current archive record. */
async function getCurrentFileUrl(I, DT, DTE, virtualFileName) {
    SL.openFileArchive('/files/archiv/lookup.pdf');
    DT.filterEquals('virtualFileName', virtualFileName);
    I.click(locate('#fileArchiveDataTable .dt-row-edit a').withText(virtualFileName));
    DTE.waitForEditor('fileArchiveDataTable');
    const fileName = await I.grabValueFrom('#DTE_Field_fileName');
    const filePath = await I.grabValueFrom('#DTE_Field_filePath');
    DTE.cancel('fileArchiveDataTable');
    return (filePath.startsWith('/') ? filePath : '/' + filePath) + fileName;
}

/** Creates a current pattern with the requested dates and returns its public URL. */
async function createPatternFixture(I, DT, DTE, virtualFileName, sourceFile, mainPath, validFrom, validTo) {
    SL.openFileArchive('/files/archiv/' + sourceFile);
    SL.uploadFile(virtualFileName, sourceFile, validFrom, validTo);
    I.clickCss('#pills-dt-fileArchiveDataTable-advanced-tab');
    I.clickCss('#DTE_Field_referenceToMain');
    const mainOption = `//li[contains(@class, "ui-menu-item")]//div[text()="${mainPath}"]`;
    I.waitForElement(mainOption, 10);
    I.click(mainOption);
    I.checkOption('#DTE_Field_editorFields-uploadRedundantFile_0');
    DTE.save('fileArchiveDataTable', true);
    return getCurrentFileUrl(I, DT, DTE, virtualFileName);
}

async function checkSelectedFiles(I, selectedGlobalIds) {
    for (const globalId of selectedGlobalIds) {
        I.seeElement( locate("tr.highlighted-file-row").withChild( locate("td.dt-type-numeric").withTextEquals(globalId) ) );
    }

    const selectedCount = await I.grabNumberOfVisibleElements("tr.highlighted-file-row")
    I.assertEqual(selectedCount, selectedGlobalIds.length, "Number of selected files is different than expected");
}

function selectRowsByGlobalId(I, globalIds) {
    for (const globalId of globalIds) {
        I.click( locate("tr").withChild( locate("td.dt-type-numeric").withTextEquals(globalId) ).find("td.dt-select-td") );
    }
}
