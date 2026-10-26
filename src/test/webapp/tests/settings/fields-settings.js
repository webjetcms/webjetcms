Feature('settings.fields-settings @singlethread');

const settingsUrl = '/admin/v9/settings/fields-settings/';
const table = 'fieldsSettingsDataTable';
const docClass = 'sk.iway.iwcm.doc.DocDetails';
let marker;

Before(({ I, login }) => {
    login('admin');
    if (!marker) marker = 'autotest-fields-settings-' + I.getRandomText();
});

Scenario('Validate and create field settings', ({ I, DT, DTE }) => {
    I.amOnPage(settingsUrl);
    DT.waitForLoader(table);
    I.clickCss('#' + table + '_wrapper button.buttons-create');
    DTE.waitForEditor(table);
    DTE.save(table);
    I.seeElement('.DTE_Field_Name_className.is-invalid');
    I.seeElement('.DTE_Field_Name_fieldName.is-invalid');

    DTE.fillField('className', docClass);
    DTE.fillField('title', marker);
    DTE.fillField('fieldName', marker);
    DTE.save(table);
    I.seeElement('.DTE_Field_Name_fieldName.is-invalid');

    DTE.fillField('fieldName', 'title');
    DTE.fillField('tab', marker);
    DTE.fillField('sortAfter', 'title');
    DTE.save(table);
    I.seeElement('.DTE_Field_Name_tab.is-invalid');
    I.seeElement('.DTE_Field_Name_sortAfter.is-invalid');

    DTE.fillField('tab', 'basic');
    DTE.fillField('sortAfter', 'navbar');
    I.clickCss('#' + table + '_modal .DTE_Header');
    I.clickCss('.DTE_Field_Name_disabled input[value="true"] + label');
    DTE.save(table, true);
    DT.filterContains('title', marker);
    I.see(docClass, '#' + table + ' tbody');

    openDocument(I, DTE);
    I.see(marker, 'label[for="DTE_Field_title"]');
    I.seeElement('#DTE_Field_title:disabled');
    I.seeElement(locate('.DTE_Field_Name_title').after('.DTE_Field_Name_navbar'));
    DTE.cancel();
});

Scenario('Reject duplicate field settings and edit editor visibility', ({ I, DT, DTE }) => {
    I.amOnPage(settingsUrl);
    DT.waitForLoader(table);
    I.clickCss('#' + table + '_wrapper button.buttons-create');
    DTE.waitForEditor(table);
    DTE.fillField('className', docClass);
    DTE.fillField('fieldName', 'title');
    DTE.fillField('title', marker);
    DTE.save(table);
    I.seeElement('.DTE_Field_Name_fieldName.is-invalid');
    DTE.cancel(table);

    openSetting(I, DT, DTE);
    I.seeInField('#DTE_Field_tab', 'basic');
    I.seeInField('#DTE_Field_sortAfter', 'navbar');
    I.seeCheckboxIsChecked('.DTE_Field_Name_disabled input[value="true"]');
    I.clickCss('.DTE_Field_Name_disabled input[value="null"] + label');
    I.clickCss('.DTE_Field_Name_hiddenEditor input[value="true"] + label');
    DTE.save(table, true);

    openDocument(I, DTE);
    I.dontSeeElement('#DTE_Field_title');
    DTE.cancel();

    openSetting(I, DT, DTE);
    I.seeCheckboxIsChecked('.DTE_Field_Name_disabled input[value="null"]');
    I.clickCss('.DTE_Field_Name_hiddenEditor input[value="null"] + label');
    DTE.save(table, true);

    openDocument(I, DTE);
    I.seeElement('#DTE_Field_title:enabled');
    I.see(marker, 'label[for="DTE_Field_title"]');
    DTE.cancel();
});

Scenario('Cleanup field settings and verify original editor', async ({ I, DT, DTE }) => {
    I.amOnPage(settingsUrl);
    DT.waitForLoader(table);
    DT.filterContains('title', marker);
    if (await I.grabNumberOfVisibleElements('#' + table + ' tbody td.dt-row-edit') > 0) {
        DT.deleteAll(table);
        DT.waitForLoader(table);
    }
    I.dontSee(marker, '#' + table + ' tbody');

    openDocument(I, DTE);
    I.seeElement('#DTE_Field_title:enabled');
    I.dontSee(marker, 'label[for="DTE_Field_title"]');
    DTE.cancel();
});

Scenario('Field settings require Editor configuration permission', ({ DT }) => {
    DT.checkPerms('dataTablesConfig', settingsUrl, table);
});

function openDocument(I, DTE) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=163800');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
}

function openSetting(I, DT, DTE) {
    I.amOnPage(settingsUrl);
    DT.waitForLoader(table);
    DT.filterContains('title', marker);
    I.clickCss('#' + table + ' tbody td.dt-row-edit a');
    DTE.waitForEditor(table);
    I.clickCss('#' + table + '_modal .DTE_Header');
}
