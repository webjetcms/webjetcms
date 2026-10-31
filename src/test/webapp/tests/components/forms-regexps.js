Feature('components.forms-regexps');

let formName;

Before(({ I, login }) => {
    login('admin');
});

Scenario('from-regexps zakladne testy @baseTest', async ({I, DataTables}) => {
    I.amOnPage("/apps/form/admin/regexps/");
    await DataTables.baseTest({
        dataTable: 'regExpDataTable',
        perms: 'cmp_form',
        requiredFields: ['title', 'type', 'regExp'],
        testingData: {
            type: "autotest-type"
        },
        afterCreateSteps(I, options, requiredFields, DT) {
            DT.filterEquals('title', options.testingData[requiredFields.indexOf('title')]);
            I.see(options.testingData[requiredFields.indexOf('title')], '#regExpDataTable tbody td:nth-child(2)');
            DT.clearFilter('title');
        },
        skipSwitchDomain: true
    });
});

Scenario('Order by code and filter by translation key', async ({I, DT}) => {
    I.amOnPage("/apps/form/admin/regexps/");
    DT.waitForLoader('regExpDataTable');
    const codes = (await I.grabTextFromAll('#regExpDataTable tbody td:nth-child(3)')).map(code => code.toLowerCase());
    I.assertAbove(codes.length, 1);
    I.assertDeepEqual(codes, [...codes].sort());
    DT.filterContains("title", "phone");
    I.see("checkform.title.mobilePhone");
    I.see("checkform.title.fixedPhone");
    I.see("checkform.title.allPhone");

    I.dontSee("checkform.title.loginChars");
    I.dontSee("checkform.title.safeChars");
});

Scenario('Filter by translated name and preserve the translation key', ({ I, DT, DTE }) => {
    const translations = {
        sk: ['Preložený názov', 'Prekladový kľúč', 'povolené len malé znaky'],
        cs: ['Přeložený název', 'Překladový klíč', 'povolena pouze malá písmena'],
        en: ['Translated name', 'Translation key', 'only small leters allowed']
    };
    const [nameHeader, keyHeader, title] = translations[I.getConfLng()];
    I.amOnPage('/apps/form/admin/regexps/');
    DT.waitForLoader('regExpDataTable');
    I.see(nameHeader, '#regExpDataTable_wrapper .dt-scroll-head');
    I.see(keyHeader, '#regExpDataTable_wrapper .dt-scroll-head');
    DT.filterContains('translatedTitle', title);
    I.see('alphabetLowercase', '#regExpDataTable tbody');
    I.dontSee('alphabetUppercase', '#regExpDataTable tbody');
    I.click(title, '#regExpDataTable tbody');
    DTE.waitForEditor('regExpDataTable');
    I.seeInField('#DTE_Field_title', 'checkform.title.alphabetLowercase');
    I.seeInField('#DTE_Field_type', 'alphabetLowercase');
    I.dontSeeElement('#DTE_Field_translatedTitle');
    DTE.save();
    I.see(title, '#regExpDataTable tbody');
    I.see('checkform.title.alphabetLowercase', '#regExpDataTable tbody');
    I.saveScreenshot('regexp-translated-title-' + I.getConfLng() + '.png');
});

Scenario('Find allowed values by code and preserve multiple selections after saving', async ({ I, DT, DTE }) => {
    formName = 'regexp_selection_autotest_' + I.getRandomText();
    I.amOnPage('/apps/form/admin/');
    DT.waitForLoader('formsDataTable');
    I.clickCss('#formsDataTable_wrapper button.buttons-create');
    DTE.waitForEditor('formsDataTable');
    DTE.fillField('formName', formName);
    DTE.save();
    // The item table loads again after the default step is selected.
    I.waitForFunction(() => {
        const step = document.querySelector('#formStepsDataTable tbody tr.selected');
        return step && new URL(formItemsDataTable.getAjaxUrl(), location.origin).searchParams.get('stepId') === step.id;
    }, [], 15);
    DT.waitForLoader('formItemsDataTable');
    I.clickCss('#formItemsDataTable_wrapper button.buttons-create');
    DTE.waitForEditor('formItemsDataTable');
    const codes = await I.executeScript(() => Array.from(document.querySelector('#DTE_Field_regexValidationArr').options, option => option.textContent.split(' - ')[0].toLowerCase()));
    I.assertAbove(codes.length, 1);
    I.assertDeepEqual(codes, [...codes].sort());
    DTE.selectOption('fieldType', 'Meno');
    DTE.fillQuill('label', 'Regex selection autotest');
    await DTE.selectOptionMulti('regexValidationArr', ['alphabetLowercase - ', 'minLen3 - ']);
    const selectedIds = await I.executeScript(() => Array.from(document.querySelector('#DTE_Field_regexValidationArr').selectedOptions, option => option.value));
    I.assertEqual(selectedIds.length, 2);

    I.clickCss('.DTE_Field_Name_regexValidationArr button.dropdown-toggle');
    I.fillField('.bs-container .bs-searchbox input', 'email2');
    I.see('email2 - e-mail adresa vo formáte meno@domena.com', '.bs-container .dropdown-menu.inner');
    I.dontSee('email - e-mail adresa vo formáte meno@domena.com', '.bs-container .dropdown-menu.inner');
    I.pressKey('Escape');
    DTE.save();

    I.refreshPage();
    I.waitForText('Regex selection autotest', 15, '#formItemsDataTable');
    I.click('Regex selection autotest', '#formItemsDataTable');
    I.clickCss('#formItemsDataTable_wrapper button.buttons-edit');
    DTE.waitForEditor('formItemsDataTable');
    const savedIds = await I.executeScript(() => Array.from(document.querySelector('#DTE_Field_regexValidationArr').selectedOptions, option => option.value));
    I.assertDeepEqual(savedIds, selectedIds);
    I.see('alphabetLowercase - povolené len malé znaky', '.DTE_Field_Name_regexValidationArr button.dropdown-toggle');
    I.see('minLen3 - minimálne 3 znaky', '.DTE_Field_Name_regexValidationArr button.dropdown-toggle');
    DTE.cancel();
});

Scenario('Cleanup the regex selection test form', ({ I, DT }) => {
    if (!formName) return;
    I.amOnPage('/apps/form/admin/');
    DT.filterEquals('formName', formName);
    DT.deleteAll('formsDataTable');
});
