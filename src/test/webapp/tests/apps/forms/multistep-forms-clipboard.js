Feature('apps.forms.multistep-forms-clipboard');

const items = '#formItemsDataTable';
const modal = '#formItemsDataTable_modal';
const copyButton = '#formItemsDataTable_wrapper .buttons-copy-field';
const pasteButton = '#formItemsDataTable_wrapper .buttons-paste-field';
const label = 'Clipboard options autotest';
const hint = '<p><strong>Clipboard hint autotest</strong></p><p>Second paragraph</p><ul><li>First instruction</li><li><a href="/help">Help</a></li></ul>';
// Created when missing and reused; only pasted fields in the destination's first step are removed.
const sourceForm = 'clipboard_source_autotest';
const targetForm = 'clipboard_target_autotest';

Before(({ login, DT }) => {
    login('admin');
    DT.addContext('formItems', '#formItemsDataTable_wrapper');
});

function openForm(I, DT, name) {
    I.amOnPage('/apps/form/admin/form-steps/?formName=' + name);
    I.waitForFunction(() => {
        const step = document.querySelector('#formStepsDataTable tbody tr.selected');
        return step && new URL(formItemsDataTable.getAjaxUrl(), location.origin).searchParams.get('stepId') === step.id;
    }, [], 15);
    DT.waitForLoader('formItemsDataTable');
}

/** Edits the multiline hint through its HTML source dialog. */
function fillHint(I, html) {
    I.clickCss(modal + ' #DTE_Field_hint button:has(.ti-code)');
    I.waitForVisible('.ql-html-textArea', 5);
    I.fillField('.ql-html-textArea', html);
    I.clickCss('button.ql-html-buttonOk');
    I.waitForInvisible('.ql-html-textArea', 5);
}

/** Clears test copies while preserving the permanent form and its steps. */
async function clearDestination(I, DT) {
    openForm(I, DT, targetForm);
    if (await I.executeScript(() => formItemsDataTable.rows().count()) === 0) return;
    // Bulk selection is needed because the form editor normally selects a single field.
    I.executeScript(() => {
        formItemsDataTable.select.style('multi');
        formItemsDataTable.rows().select();
    });
    I.click(DT.btn.formItems_delete_button);
    I.waitForVisible('div.DTE_Action_Remove', 10);
    I.click('Zmazať', 'div.DTE_Action_Remove');
    DT.waitForLoader('formItemsDataTable');
    I.waitForFunction(() => formItemsDataTable.rows().count() === 0, [], 10);
    I.executeScript(() => { formItemsDataTable.select.style('single'); });
}

Scenario('Copy a saved field, cancel and save independent repeated copies', async ({ I, DT, DTE }) => {
    I.amOnPage('/apps/form/admin/');
    DT.waitForLoader('formsDataTable');
    const forms = await I.executeScript(() => formsDataTable.rows().data().toArray().map(form => form.formName));
    for (const name of [targetForm, sourceForm]) {
        if (forms.includes(name)) continue;
        I.amOnPage('/apps/form/admin/');
        I.clickCss('#formsDataTable_wrapper button.buttons-create');
        DTE.waitForEditor('formsDataTable');
        DTE.fillField('formName', name);
        DTE.save();
        I.waitForVisible('#formStepsDataTable_wrapper', 10);
    }
    await clearDestination(I, DT);
    openForm(I, DT, sourceForm);
    if (await I.grabNumberOfVisibleElements(locate(items + ' tbody tr').withText(label)) === 0) {
        I.click(DT.btn.formItems_add_button);
        DTE.waitForEditor('formItemsDataTable');
        DTE.selectOption('fieldType', 'Výberový zoznam - select');
        DTE.fillQuill('label', label);
        I.checkOption('#DTE_Field_required_0');
        I.clickCss('#pills-dt-formItemsDataTable-advanced-tab');
        DTE.addFieldOptions('valueAsOptions', 'First autotest:first|Second autotest:second');
        DTE.fillQuill('tooltip', 'Clipboard tooltip autotest');
        DTE.fillField('customError', 'Clipboard validation autotest');
        I.clickCss('#pills-dt-formItemsDataTable-stat-tab');
        DTE.fillField('topCount', '3');
        I.uncheckOption('#DTE_Field_showOtherCount_0');
        I.checkOption('#DTE_Field_showUnanswered_0');
        DTE.save();
        I.waitForText(label, 10, items);
    }
    I.click(locate(items + ' tbody tr').withText(label).find('td:last-child'));
    I.clickCss('#formItemsDataTable_wrapper .buttons-edit');
    DTE.waitForEditor('formItemsDataTable');
    I.seeAttributesOnElements(modal + ' #DTE_Field_hint .ql-editor', { 'aria-multiline': 'true' });
    I.seeElement(modal + ' #DTE_Field_hint .ql-header');
    I.dontSeeElement(modal + ' #DTE_Field_label .ql-header');
    fillHint(I, hint);
    DTE.save();
    openForm(I, DT, sourceForm);
    I.waitForText('Clipboard hint autotest', 10, '.stepPreview .form-hint strong');
    I.seeNumberOfElements('.stepPreview .form-hint > p', 2);
    I.seeNumberOfElements('.stepPreview .form-hint ul > li', 2);
    I.seeAttributesOnElements('.stepPreview .form-hint a', { href: '/help' });
    I.click(locate(items + ' tbody tr').withText(label).find('td:last-child'));
    I.waitForEnabled(copyButton, 10);
    const source = await I.executeScript(() => formItemsDataTable.rows({ selected: true }).data().toArray()[0]);
    I.clickCss(copyButton);
    I.waitForEnabled(pasteButton, 10);

    openForm(I, DT, targetForm);
    I.waitForEnabled(pasteButton, 10);
    I.clickCss(pasteButton);
    DTE.waitForEditor('formItemsDataTable');
    I.waitForText('Podmienky zobrazenia a povinnosti sa nekopírujú.', 10, '#toast-container-webjet');
    I.see(label, modal + ' .DTE_Field_Name_label');
    I.see('Clipboard hint autotest', modal + ' #DTE_Field_hint strong');
    I.seeNumberOfElements(modal + ' #DTE_Field_hint .ql-editor > p', 2);
    I.seeCheckboxIsChecked('#DTE_Field_required_0');
    I.dontSeeElement('#pills-dt-formItemsDataTable-visibilityConditions-tab');
    I.dontSeeElement('#pills-dt-formItemsDataTable-requirementConditions-tab');
    DTE.cancel();
    I.dontSee(label, items);

    for (let index = 0; index < 2; index++) {
        I.waitForEnabled(pasteButton, 10);
        I.clickCss(pasteButton);
        DTE.waitForEditor('formItemsDataTable');
        DTE.save();
        I.waitForFunction(([count]) => formItemsDataTable.rows().count() === count, [index + 1], 10);
    }
    openForm(I, DT, targetForm);
    const records = await I.executeScript(() => formItemsDataTable.rows().data().toArray());
    I.assertEqual(records.length, 2, 'Both copies must survive reopening the form');
    I.assertNotEqual(records[0].id, records[1].id, 'Each copy must have a new database ID');
    I.assertNotEqual(records[0].itemFormId, records[1].itemFormId, 'Each field must have its own identifier');
    I.assertAbove(records[1].sortPriority, records[0].sortPriority, 'Repeated paste must append to the step');
    for (const record of records) {
        I.assertNotEqual(record.id, source.id, 'Paste must create a new record');
        I.assertEqual(record.formName, targetForm, 'Paste must use the destination form');
        I.assertNotEqual(record.stepId, source.stepId, 'Paste must use the destination step');
        for (const field of ['fieldType', 'label', 'hint', 'required', 'value', 'tooltip', 'customError', 'topCount', 'showOtherCount', 'showUnanswered']) {
            I.assertDeepEqual(record[field], source[field], field + ' must survive copy, save and reopen');
        }
    }

    const conditionUrls = [];
    I.clickCss(items + ' tbody tr[id="' + records[1].id + '"] td:last-child');
    I.click(DT.btn.formItems_edit_button);
    DTE.waitForEditor('formItemsDataTable');
    for (const condition of ['visibilityConditions', 'requirementConditions']) {
        const tableId = 'datatableFieldDTE_Field_' + condition;
        I.clickCss('#pills-dt-formItemsDataTable-' + condition + '-tab');
        I.clickCss('#' + tableId + '_wrapper button.buttons-create');
        DTE.waitForEditor(tableId);
        DTE.selectOption('itemFormId', '(Krok 1) ' + label);
        DTE.selectOption('operator', 'je prázdne');
        DTE.save(tableId);
        DT.waitForLoader(tableId);
        const saved = await I.executeScript(condition => {
            const table = condition === 'visibilityConditions' ? visibilityConditionsDataTable : requirementConditionsDataTable;
            return { url: WJ.urlAddPath(table.getAjaxUrl(), '/all'), count: table.rows().count() };
        }, condition);
        I.assertEqual(saved.count, 1, 'The copied field must have a saved ' + condition);
        conditionUrls.push(saved.url);
    }
    DTE.cancel('formItemsDataTable');
    I.clickCss(items + ' tbody tr[id="' + records[0].id + '"] td:last-child');
    I.click(DT.btn.formItems_delete_button);
    I.waitForVisible('div.DTE_Action_Remove', 10);
    I.click('Zmazať', 'div.DTE_Action_Remove');
    I.waitForText('Táto položka formulára sa používa v podmienkach, preto sa NEDÁ odstrániť.', 10, 'div.DTE_Action_Remove');
    DTE.cancel('formItemsDataTable');
    I.seeElement(items + ' tbody tr[id="' + records[0].id + '"]');
    I.toastrClose();

    I.clickCss(items + ' tbody tr[id="' + records[1].id + '"] td:last-child');
    I.click(DT.btn.formItems_delete_button);
    I.waitForVisible('div.DTE_Action_Remove', 10);
    I.click('Zmazať', 'div.DTE_Action_Remove');
    DT.waitForLoader('formItemsDataTable');
    I.waitForInvisible(items + ' tbody tr[id="' + records[1].id + '"]', 10);
    for (const url of conditionUrls) {
        const result = await I.executeScript(async url => {
            const response = await fetch(url, { headers: { 'X-CSRF-Token': window.csrfToken } });
            return { status: response.status, data: await response.json() };
        }, url);
        I.assertEqual(result.status, 200, 'Conditions remain readable after deleting their owning field');
        I.assertDeepEqual(result.data.content, [], 'Deleting a copied field must remove its saved conditions');
    }
});

Scenario('Edit and clear a hint without changing the label, tooltip or other copies', async ({ I, DT, DTE }) => {
    openForm(I, DT, targetForm);
    // The previous scenario deletes one copy while verifying condition cleanup.
    I.waitForEnabled(pasteButton, 10);
    I.clickCss(pasteButton);
    DTE.waitForEditor('formItemsDataTable');
    DTE.save();
    openForm(I, DT, targetForm);
    const records = await I.executeScript(() => formItemsDataTable.rows().data().toArray());
    I.assertEqual(records.length, 2);
    const field = records[0];
    const row = items + ' tbody tr[id="' + field.id + '"]';
    const updatedHint = '<p><em>Updated hint autotest</em></p><p>Another paragraph</p>';

    for (const value of [updatedHint, '']) {
        I.clickCss(row + ' td:last-child');
        I.clickCss('#formItemsDataTable_wrapper .buttons-edit');
        DTE.waitForEditor('formItemsDataTable');
        fillHint(I, value);
        DTE.save();
        openForm(I, DT, targetForm);
        const saved = await I.executeScript(id => formItemsDataTable.row('#' + id).data(), field.id);
        I.assertEqual(saved.hint, value, 'The updated or empty hint must survive reloading');
        I.assertEqual(saved.label, field.label);
        I.assertEqual(saved.tooltip, field.tooltip);
        I.assertEqual(await I.executeScript(id => formItemsDataTable.row('#' + id).data().hint, records[1].id), records[1].hint,
            'Editing one copy must not change another copy');
    }
    I.dontSee('Updated hint autotest', '.stepPreview');
    I.clickCss(row + ' td:last-child');
    I.clickCss('#formItemsDataTable_wrapper .buttons-edit');
    DTE.waitForEditor('formItemsDataTable');
    I.assertEqual((await I.grabTextFrom(modal + ' #DTE_Field_hint .ql-editor')).trim(), '');
    DTE.cancel();
});

Scenario('Cleanup clipboard and pasted fields', async ({ I, DT }) => {
    await clearDestination(I, DT);
    I.executeScript(() => {
        localStorage.removeItem('webjet.multistepform.clipboard.' + formClipboardUserId + '.' + encodeURIComponent(formClipboardDomain));
    });
});

Scenario('Clipboard requires form permissions', ({ I, DT }) => {
    DT.checkPerms('cmp_form', '/apps/form/admin/form-steps/?formName=' + targetForm, 'formItemsDataTable', () => {
        I.executeScript(() => { formItemsDataTable.ajax.reload(); });
    });
});

Scenario('Restore form permissions', ({ I }) => {
    // A failed permission assertion must not leave subsequent files using a restricted session.
    I.logout();
});
