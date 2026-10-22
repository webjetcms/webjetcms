Feature('settings.tabs-settings @singlethread');

const settingsUrl = '/admin/v9/settings/tabs-settings/';
const table = 'tabsSettingsDataTable';
const docClass = 'sk.iway.iwcm.doc.DocDetails';
const pagesUrl = '/admin/v9/webpages/web-pages-list/?groupid=67';
let marker;

Before(({ I, login }) => {
    login('admin');
    if (!marker) marker = 'autotest-tabs-settings-' + I.getRandomText();
});

Scenario('Validate, create and edit a tab', ({ I, DT, DTE }) => {
    I.amOnPage(settingsUrl);
    DT.waitForLoader(table);
    I.clickCss('#' + table + '_wrapper button.buttons-create');
    DTE.waitForEditor(table);
    DTE.save(table);
    I.seeElement('.DTE_Field_Name_className.is-invalid');
    I.seeElement('.DTE_Field_Name_tabId.is-invalid');

    DTE.fillField('className', docClass);
    DTE.fillField('tabId', marker);
    DTE.fillField('title', marker);
    DTE.fillField('sortAfter', marker);
    DTE.save(table);
    I.seeElement('.DTE_Field_Name_sortAfter.is-invalid');
    DTE.fillField('sortAfter', marker + '-missing');
    DTE.save(table);
    I.seeElement('.DTE_Field_Name_sortAfter.is-invalid');

    DTE.fillField('sortAfter', 'FIRST');
    DTE.fillField('cssClass', 'autotest-tab');
    DTE.fillField('content', '<p>' + marker + '-content</p>');
    I.clickCss('.DTE_Field_Name_selected input[value="true"] + label');
    I.clickCss('.DTE_Field_Name_hideOnCreate input[value="true"] + label');
    DTE.save(table, true);

    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=163800');
    DTE.waitForEditor();
    I.seeElement('#pills-dt-editor-datatableInit li:first-child.autotest-tab a.active');
    I.see(marker, '#pills-dt-datatableInit-' + marker + '-tab');
    I.see(marker + '-content', '#datatableInit_modal');
    DTE.cancel();
    I.clickCss('#datatableInit_wrapper button.buttons-create');
    DTE.waitForEditor();
    I.dontSeeElement('#pills-dt-datatableInit-' + marker + '-tab');
    I.seeElement('#pills-dt-editor-datatableInit a.active');
    DTE.cancel();

    I.amOnPage(settingsUrl);
    DT.waitForLoader(table);
    I.clickCss('#' + table + '_wrapper button.buttons-create');
    DTE.waitForEditor(table);
    DTE.fillField('className', docClass);
    DTE.fillField('tabId', marker);
    DTE.save(table);
    I.seeElement('.DTE_Field_Name_tabId.is-invalid');
    DTE.cancel(table);

    DT.filterEquals('tabId', marker);
    I.clickCss('#' + table + ' tbody td.dt-row-edit a');
    DTE.waitForEditor(table);
    I.seeInField('#DTE_Field_content', '<p>' + marker + '-content</p>');
    DTE.fillField('title', marker + '-edited');
    DTE.fillField('sortAfter', 'basic');
    I.clickCss('#' + table + '_modal .DTE_Header');
    I.clickCss('.DTE_Field_Name_selected input[value="null"] + label');
    I.clickCss('.DTE_Field_Name_hideOnCreate input[value="null"] + label');
    DTE.save(table, true);

    I.amOnPage(pagesUrl);
    DT.waitForLoader();
    I.clickCss('#datatableInit_wrapper button.buttons-create');
    DTE.waitForEditor();
    I.seeElement('#pills-dt-editor-datatableInit li:has(> #pills-dt-datatableInit-basic-tab) + li > #pills-dt-datatableInit-' + marker + '-tab');
    I.clickCss('#pills-dt-datatableInit-' + marker + '-tab');
    I.see(marker + '-edited', '#pills-dt-datatableInit-' + marker + '-tab');
    I.see(marker + '-content', '#datatableInit_modal');
    DTE.cancel();
});

Scenario('Cleanup tab settings and verify the tab disappears', async ({ I, DT, DTE }) => {
    await deleteSettings(I, DT, settingsUrl, table, 'tabId', marker);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=163800');
    DTE.waitForEditor();
    I.dontSeeElement('#pills-dt-datatableInit-' + marker + '-tab');
    DTE.cancel();
});

Scenario('Move custom fields to a new tab and save their values', async ({ I, DT, DTE }) => {
    const tabId = marker + '-fields';
    const pane = '#pills-dt-datatableInit-' + tabId;

    I.amOnPage(pagesUrl);
    DT.waitForLoader();
    I.clickCss('#datatableInit_wrapper button.buttons-create');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    DTE.fillField('title', marker);
    DTE.fillField('navbar', marker);
    I.uncheckOption('#DTE_Field_available_0');
    DTE.save('datatableInit', true);
    DT.filterEquals('title', marker);
    I.see(marker, '#datatableInit tbody');
    const docId = String(await I.executeScript(() => window.webpagesDatatable.row(0).data().id));

    I.amOnPage('/admin/v9/settings/custom-fields/');
    DT.waitForLoader('customFieldsDataTable');
    for (const alphabet of ['A', 'B']) {
        I.clickCss('#customFieldsDataTable_wrapper button.buttons-create');
        DTE.waitForEditor('customFieldsDataTable');
        DTE.fillField('className', docClass);
        DTE.selectOption('alphabet', alphabet);
        DTE.selectOption('type', 'Text');
        DTE.fillField('entityId', docId);
        DTE.fillField('label', marker + '-' + alphabet);
        DTE.fillField('tooltip', marker);
        I.checkOption('#DTE_Field_required_0');
        DTE.save('customFieldsDataTable', true);
    }

    I.amOnPage(settingsUrl);
    DT.waitForLoader(table);
    I.clickCss('#' + table + '_wrapper button.buttons-create');
    DTE.waitForEditor(table);
    DTE.fillField('className', docClass);
    DTE.fillField('tabId', tabId);
    DTE.fillField('title', tabId);
    DTE.fillField('sortAfter', 'basic');
    DTE.save(table, true);

    I.amOnPage('/admin/v9/settings/fields-settings/');
    DT.waitForLoader('fieldsSettingsDataTable');
    for (const alphabet of ['A', 'B']) {
        I.clickCss('#fieldsSettingsDataTable_wrapper button.buttons-create');
        DTE.waitForEditor('fieldsSettingsDataTable');
        DTE.fillField('className', docClass);
        DTE.fillField('fieldName', 'field' + alphabet);
        DTE.fillField('tab', tabId);
        DTE.save('fieldsSettingsDataTable', true);
    }

    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + docId);
    DTE.waitForEditor();
    I.clickCss(pane + '-tab');
    for (const alphabet of ['A', 'B']) {
        I.see(marker + '-' + alphabet, pane + ' label[for="DTE_Field_field' + alphabet + '"]');
        I.seeElement(pane + ' .DTE_Field_Name_field' + alphabet + '.required');
        I.dontSeeElementInDOM('#pills-dt-datatableInit-fields #DTE_Field_field' + alphabet);
        I.fillField(pane + ' #DTE_Field_field' + alphabet, marker + '-value-' + alphabet);
    }
    DTE.save('datatableInit', true);

    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + docId);
    DTE.waitForEditor();
    I.clickCss(pane + '-tab');
    for (const alphabet of ['A', 'B']) {
        I.seeInField(pane + ' #DTE_Field_field' + alphabet, marker + '-value-' + alphabet);
    }
    DTE.cancel();
});

Scenario('Cleanup combined custom field and tab settings', async ({ I, DT }) => {
    await deleteSettings(I, DT, '/admin/v9/settings/fields-settings/', 'fieldsSettingsDataTable', 'tab', marker + '-fields');
    await deleteSettings(I, DT, settingsUrl, table, 'tabId', marker + '-fields');
    await deleteSettings(I, DT, '/admin/v9/settings/custom-fields/', 'customFieldsDataTable', 'tooltip', marker);
    await deleteSettings(I, DT, pagesUrl, 'datatableInit', 'title', marker);
});

Scenario('Tab settings require Editor configuration permission', ({ DT }) => {
    DT.checkPerms('dataTablesConfig', settingsUrl, table);
});

/** Deletes only rows belonging to this test run, including after a failed setup. */
async function deleteSettings(I, DT, url, tableId, field, value) {
    I.amOnPage(url);
    DT.waitForLoader(tableId);
    DT.filterEquals(field, value);
    if (await I.grabNumberOfVisibleElements('#' + tableId + ' tbody td.dt-row-edit') > 0) {
        DT.deleteAll(tableId);
        DT.waitForLoader(tableId);
    }
    I.dontSeeElement('#' + tableId + ' tbody td.dt-row-edit');
}
