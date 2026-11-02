Feature('components.forms-regexps');

const translations = {
    sk: {
        nameHeader: 'Preložený názov',
        keyHeader: 'Prekladový kľúč',
        lowercaseTitle: 'povolené len malé znaky',
        nameFieldType: 'Meno',
        emailTitle: 'e-mail adresa vo formáte meno@domena.com',
        minLen3Title: 'minimálne 3 znaky'
    },
    cs: {
        nameHeader: 'Přeložený název',
        keyHeader: 'Překladový klíč',
        lowercaseTitle: 'povolena pouze malá písmena',
        nameFieldType: 'Jméno',
        emailTitle: 'e-mailová adresa ve formátu jmeno@domena.com',
        minLen3Title: 'minimálně 3 znaky'
    },
    en: {
        nameHeader: 'Translated name',
        keyHeader: 'Translation key',
        lowercaseTitle: 'only small leters allowed',
        nameFieldType: 'Name',
        emailTitle: 'e-mail address in format name@domain.com',
        minLen3Title: 'at least 3 chars'
    }
};

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
    const { nameHeader, keyHeader, lowercaseTitle } = translations[I.getConfLng()];
    I.amOnPage('/apps/form/admin/regexps/');
    DT.waitForLoader('regExpDataTable');
    I.see(nameHeader, '#regExpDataTable_wrapper .dt-scroll-head');
    I.see(keyHeader, '#regExpDataTable_wrapper .dt-scroll-head');
    DT.filterContains('translatedTitle', lowercaseTitle);
    I.see('alphabetLowercase', '#regExpDataTable tbody');
    I.dontSee('alphabetUppercase', '#regExpDataTable tbody');
    I.click(lowercaseTitle, '#regExpDataTable tbody');
    DTE.waitForEditor('regExpDataTable');
    I.seeInField('#DTE_Field_title', 'checkform.title.alphabetLowercase');
    I.seeInField('#DTE_Field_type', 'alphabetLowercase');
    I.dontSeeElement('#DTE_Field_translatedTitle');
    DTE.save();
    I.see(lowercaseTitle, '#regExpDataTable tbody');
    I.see('checkform.title.alphabetLowercase', '#regExpDataTable tbody');
    I.saveScreenshot('regexp-translated-title-' + I.getConfLng() + '.png');
});

Scenario('Find allowed values by code and preserve multiple selections after saving', async ({ I, DT, DTE }) => {
    const { nameFieldType, emailTitle, lowercaseTitle, minLen3Title } = translations[I.getConfLng()];
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
    DTE.selectOption('fieldType', nameFieldType);
    DTE.fillQuill('label', 'Regex selection autotest');
    await DTE.selectOptionMulti('regexValidationArr', ['alphabetLowercase - ', 'minLen3 - ']);
    const selectedIds = await I.executeScript(() => Array.from(document.querySelector('#DTE_Field_regexValidationArr').selectedOptions, option => option.value));
    I.assertEqual(selectedIds.length, 2);

    I.clickCss('.DTE_Field_Name_regexValidationArr button.dropdown-toggle');
    I.fillField('.bs-container .bs-searchbox input', 'email2');
    I.see('email2 - ' + emailTitle, '.bs-container .dropdown-menu.inner');
    I.dontSee('email - ' + emailTitle, '.bs-container .dropdown-menu.inner');
    I.pressKey('Escape');
    DTE.save();

    I.refreshPage();
    I.waitForText('Regex selection autotest', 15, '#formItemsDataTable');
    I.click('Regex selection autotest', '#formItemsDataTable');
    I.clickCss('#formItemsDataTable_wrapper button.buttons-edit');
    DTE.waitForEditor('formItemsDataTable');
    const savedIds = await I.executeScript(() => Array.from(document.querySelector('#DTE_Field_regexValidationArr').selectedOptions, option => option.value));
    I.assertDeepEqual(savedIds, selectedIds);
    I.see('alphabetLowercase - ' + lowercaseTitle, '.DTE_Field_Name_regexValidationArr button.dropdown-toggle');
    I.see('minLen3 - ' + minLen3Title, '.DTE_Field_Name_regexValidationArr button.dropdown-toggle');
    DTE.cancel();
});

Scenario('Cleanup the regex selection test form', ({ I, DT }) => {
    if (!formName) return;
    I.amOnPage('/apps/form/admin/');
    DT.filterEquals('formName', formName);
    DT.deleteAll('formsDataTable');
});
