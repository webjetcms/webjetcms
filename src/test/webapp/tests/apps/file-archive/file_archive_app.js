Feature('apps.file-archive.file_archive_app');

const SL = require("./SL.js");

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
});

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
});

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

Scenario('File archive app - open files test', async ({ I }) => {
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

    SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
});