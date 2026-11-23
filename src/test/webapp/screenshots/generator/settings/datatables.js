Feature('settings.datatables @singlethread');

const docClass = 'sk.iway.iwcm.doc.DocDetails';
const fieldsTable = 'fieldsSettingsDataTable';
const tabsTable = 'tabsSettingsDataTable';
const screenshots = '/frontend/datatables/';
let tabId;

Before(({ I, login }) => {
    login('admin');
    if (!tabId) tabId = 'autotest-details-' + I.getRandomText();
});

Scenario('Create Editor screenshot settings', ({ I, DT, DTE, i18n }) => {
    I.amOnPage('/admin/v9/settings/tabs-settings/');
    DT.waitForLoader(tabsTable);
    I.clickCss('#' + tabsTable + '_wrapper button.buttons-create');
    DTE.waitForEditor(tabsTable);
    DTE.fillField('className', docClass);
    DTE.fillField('tabId', tabId);
    DTE.fillField('title', i18n.get('Additional data'));
    DTE.fillField('sortAfter', 'basic');
    DTE.fillField('content', '<p>' + i18n.get('Additional information about the web page.') + '</p>');
    DTE.save(tabsTable, true);

    I.amOnPage('/admin/v9/settings/fields-settings/');
    DT.waitForLoader(fieldsTable);
    // Never replace a pre-existing field setting in the development database.
    DT.filterEquals('className', docClass);
    DT.filterEquals('fieldName', 'fieldT');
    I.dontSeeElement('#' + fieldsTable + ' tbody td.dt-row-edit');
    I.clickCss('#' + fieldsTable + '_wrapper button.buttons-create');
    DTE.waitForEditor(fieldsTable);
    DTE.fillField('className', docClass);
    DTE.fillField('fieldName', 'fieldT');
    DTE.fillField('tab', tabId);
    DTE.fillField('title', i18n.get('Additional information'));
    I.clickCss('#' + fieldsTable + '_modal .DTE_Header');
    I.clickCss('.DTE_Field_Name_disabled input[value="true"] + label');
    DTE.save(fieldsTable, true);
});

Scenario('Field settings screenshots', ({ I, DT, DTE, Document, i18n }) => {
    I.amOnPage('/admin/v9/settings/fields-settings/');
    DT.waitForLoader(fieldsTable);
    DT.filterEquals('tab', tabId);
    I.see(i18n.get('Additional information'), '#' + fieldsTable + ' tbody');
    Document.screenshot(screenshots + 'fields-settings-datatable.png', 1600, 650);

    I.clickCss('#' + fieldsTable + ' tbody td.dt-row-edit a');
    DTE.waitForEditor(fieldsTable);
    I.seeInField('#DTE_Field_fieldName', 'fieldT');
    I.seeCheckboxIsChecked('.DTE_Field_Name_disabled input[value="true"]');
    I.clickCss('#' + fieldsTable + '_modal .DTE_Header');
    I.waitToHide('ul.dt-autocomplete-select');
    Document.screenshotElement('#' + fieldsTable + '_modal .DTE_Action_Edit', screenshots + 'fields-settings-editor.png', 1280, 1000);
    DTE.cancel(fieldsTable);
});

Scenario('Tab settings screenshots', ({ I, DT, DTE, Document, i18n }) => {
    I.amOnPage('/admin/v9/settings/tabs-settings/');
    DT.waitForLoader(tabsTable);
    DT.filterEquals('tabId', tabId);
    I.see(i18n.get('Additional data'), '#' + tabsTable + ' tbody');
    Document.screenshot(screenshots + 'tabs-settings-datatable.png', 1600, 650);

    I.clickCss('#' + tabsTable + ' tbody td.dt-row-edit a');
    DTE.waitForEditor(tabsTable);
    I.seeInField('#DTE_Field_sortAfter', 'basic');
    I.clickCss('#' + tabsTable + '_modal .DTE_Header');
    I.waitToHide('ul.dt-autocomplete-select');
    Document.screenshotElement('#' + tabsTable + '_modal .DTE_Action_Edit', screenshots + 'tabs-settings-editor.png', 1280, 900);
    DTE.cancel(tabsTable);
});

Scenario('Custom fields screenshots', ({ I, DT, DTE, Document, i18n }) => {
    I.amOnPage('/admin/v9/settings/custom-fields/');
    DT.waitForLoader('customFieldsDataTable');
    Document.screenshot(screenshots + 'custom-fields-settings-datatable.png', 1600, 650);

    I.clickCss('#customFieldsDataTable_wrapper button.buttons-create');
    DTE.waitForEditor('customFieldsDataTable');
    DTE.fillField('className', docClass);
    DTE.selectOption('alphabet', 'C');
    DTE.selectOption('type', i18n.get('Select box'));
    DTE.fillField('label', i18n.get('Operating system'));
    I.checkOption('#DTE_Field_required_0');
    I.waitForVisible('input.options-value-1');
    I.fillField('input.options-value-1', 'macOS');
    I.fillField('input.options-value-2', 'macos');
    I.clickCss('button.options-add-btn');
    Document.screenshotElement('#customFieldsDataTable_modal .DTE_Action_Create', screenshots + 'custom-fields-settings-editor.png', 1280, 900);

    I.clickCss('#pills-dt-customFieldsDataTable-bonus-tab');
    DTE.fillField('bonusClassName', 'sk.iway.iwcm.doc.TemplateDetails');
    DTE.fillField('bonusEntityId', '6');
    I.clickCss('#customFieldsDataTable_modal .DTE_Header');
    Document.screenshotElement('#customFieldsDataTable_modal .DTE_Action_Create', screenshots + 'custom-fields-settings-editor-bonus.png', 1280, 600);
    DTE.cancel('customFieldsDataTable');
});

Scenario('Cleanup Editor field screenshot settings', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/settings/fields-settings/');
    DT.waitForLoader(fieldsTable);
    DT.filterEquals('tab', tabId);
    if (await I.grabNumberOfVisibleElements('#' + fieldsTable + ' tbody td.dt-row-edit') > 0) {
        DT.deleteAll(fieldsTable);
        DT.waitForLoader(fieldsTable);
    }
    I.dontSeeElement('#' + fieldsTable + ' tbody td.dt-row-edit');
});

Scenario('Cleanup Editor tab screenshot settings', async ({ I, DT }) => {
    I.amOnPage('/admin/v9/settings/tabs-settings/');
    DT.waitForLoader(tabsTable);
    DT.filterEquals('tabId', tabId);
    if (await I.grabNumberOfVisibleElements('#' + tabsTable + ' tbody td.dt-row-edit') > 0) {
        DT.deleteAll(tabsTable);
        DT.waitForLoader(tabsTable);
    }
    I.dontSeeElement('#' + tabsTable + ' tbody td.dt-row-edit');
});
