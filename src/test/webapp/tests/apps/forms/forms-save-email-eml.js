const { recorder } = require('codeceptjs');

Feature('apps.forms.forms-save-email-eml');

let originalConfiguration = {};
let testDirectory;

Before(({ login }) => {
    login('admin');
});

Scenario('Save classic and multistep form messages as EML @singlethread', async ({ I, DT, DTE }) => {
    const suffix = I.getRandomText();
    testDirectory = '/files/protected/emails-autotest-' + suffix;
    const classicName = 'Classic EML autotest ' + suffix;
    const multistepName = 'Multistep EML autotest ' + suffix;
    const email = 'autotest.' + suffix + '@balat.sk';
    let testError;
    let restoreError;

    try {
        await setTemporaryConfiguration(I, DT, DTE, 'spamProtection', 'false', originalConfiguration);
        await setTemporaryConfiguration(I, DT, DTE, 'sendMailSaveEmailPath', testDirectory, originalConfiguration);
        await setTemporaryConfiguration(I, DT, DTE, 'sendMailSaveEmail', 'true', originalConfiguration);
        await setTemporaryConfiguration(I, DT, DTE, 'useSMTPServer', 'true', originalConfiguration);

        I.amOnPage('/apps/formular-lahko/');
        I.waitForVisible('form.formsimple', 10);
        I.fillField('Meno a priezvisko', classicName);
        I.clearField('E-mailová adresa');
        I.fillField('E-mailová adresa', email);
        I.seeInField('E-mailová adresa', email);
        I.fillField('Vaša otázka', classicName);
        I.click('Súhlas s podmienkami');
        I.click('Odoslať');
        await I.waitForText('Formulár bol úspešne odoslaný', 15);

        const classicEmailCount = await countEmlFiles(I, 1);

        I.amOnPage('/apps/multistep-formular/rowviewversion.html');
        I.waitForVisible('#f1-meno-1', 10);
        I.fillField('#f1-meno-1', 'Multistep autotest');
        I.fillField('#f1-priezvisko-1', multistepName);
        I.clearField('#f1-email-1');
        I.fillField('#f1-email-1', email);
        I.seeInField('#f1-email-1', email);
        I.fillField('#f1-adresa-1', 'Autotest address ' + suffix);
        I.clickCss("button[type='submit']");
        await I.waitForText('Formulár bol úspešne odoslaný', 15);

        const totalEmailCount = await countEmlFiles(I, classicEmailCount + 1);
        await I.assertAbove(totalEmailCount, classicEmailCount, 'The multistep form must create another EML file');
    } catch (error) {
        testError = error;
    } finally {
        await recorder.catchWithoutStop(error => { if (!testError) testError = error; });
        let emailPathRestored = false;
        for (const name of ['useSMTPServer', 'sendMailSaveEmail', 'sendMailSaveEmailPath', 'spamProtection']) {
            if (!Object.prototype.hasOwnProperty.call(originalConfiguration, name)) continue;
            try {
                await setTemporaryConfiguration(I, DT, DTE, name, originalConfiguration[name]);
                if (name === 'sendMailSaveEmailPath') emailPathRestored = true;
            } catch (error) {
                if (!restoreError) restoreError = error;
                await recorder.catchWithoutStop(() => {});
            }
        }
        if (emailPathRestored) {
            try {
                await deleteTestEmailDirectory(I);
            } catch (error) {
                if (!restoreError) restoreError = error;
                await recorder.catchWithoutStop(() => {});
            }
        }
    }
    if (testError && restoreError) throw new AggregateError([testError, restoreError], 'EML test and cleanup failed');
    if (testError) throw testError;
    if (restoreError) throw restoreError;
});

async function setTemporaryConfiguration(I, DT, DTE, name, value, originalValues) {
    I.amOnPage('/admin/v9/settings/configuration/');
    const allNode = "#SomStromcek li[data-configuration-view='all'] > a.jstree-anchor";
    I.waitForElement(allNode, 20);
    I.clickCss(allNode);
    I.waitForFunction(() => new URL(configurationDatatable.getAjaxUrl(), location.origin).searchParams.get('view') === 'all', 20);
    DT.waitForLoader();
    DT.filterEquals('name', name);
    I.waitForText(name, 10, '#configurationDatatable');

    const currentValue = await I.executeScript(() => {
        const rows = configurationDatatable.rows({ search: 'applied' }).data().toArray();
        if (rows.length !== 1) throw new Error('Expected one configuration row');
        return rows[0].displayValue ?? rows[0].value;
    });
    if (originalValues) originalValues[name] = currentValue;

    I.click(name, '#configurationDatatable');
    DTE.waitForEditor('configurationDatatable');
    DTE.fillField('value', value);
    DTE.clickSwitch('temporary_0');
    DTE.save();
    await I.waitForFunction(([expected]) => {
        const rows = configurationDatatable.rows({ search: 'applied' }).data().toArray();
        return rows.length === 1 && (rows[0].displayValue ?? rows[0].value) === expected;
    }, [value], 15);
}

async function countEmlFiles(I, minimumCount) {
    I.amOnPage('/admin/elFinder/#elf_iwcm_1_' + encodeElfinderPath(testDirectory));
    I.waitForVisible('#finder .elfinder-cwd-wrapper', 20);
    const emlSelector = '.elfinder-cwd-filename[title$=".eml"]';
    await I.waitForFunction(([selector, expected]) => document.querySelectorAll(selector).length >= expected,
        [emlSelector, minimumCount], 20);
    return I.grabNumberOfVisibleElements(emlSelector);
}

async function deleteTestEmailDirectory(I) {
    if (!/^\/files\/protected\/emails-autotest-[0-9-]+$/.test(testDirectory || '')) return;

    const folderName = testDirectory.substring(testDirectory.lastIndexOf('/') + 1);
    const selector = '.elfinder-cwd-filename[title="' + folderName + '"]';
    I.amOnPage('/admin/elFinder/#elf_iwcm_1_' + encodeElfinderPath('/files/protected'));
    I.waitForVisible('#finder .elfinder-cwd-wrapper', 20);
    I.waitForVisible('.elfinder-button-icon-mkdir:visible', 20);
    if (await I.grabNumberOfVisibleElements(selector) === 0) return;

    I.rightClick(selector);
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rm');
    I.waitForVisible('.elfinder-confirm-accept', 10);
    I.clickCss('.elfinder-confirm-accept');
    await I.waitForInvisible(selector, 120);
}

function encodeElfinderPath(path) {
    return Buffer.from(path)
        .toString('base64')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '_E');
}
