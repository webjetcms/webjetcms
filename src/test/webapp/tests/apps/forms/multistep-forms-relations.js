Feature('apps.forms.multistep-forms-relations');

const formUrl = '/apps/form/admin/form-steps/?formName=multistep_condition_test';
const items = '#formItemsDataTable';
const relationsTab = '#pills-dt-formItemsDataTable-relations-tab';
const basicTab = '#pills-dt-formItemsDataTable-basic-tab';
const advancedTab = '#pills-dt-formItemsDataTable-advanced-tab';
const visibilityUsage = '#formItemVisibilityUsage';
const requirementUsage = '#formItemRequirementUsage';
const confirmDialog = '#toast-container-webjet .toast[role="dialog"]';

Before(({ login, DT }) => {
    login('admin');
    DT.addContext('formItems', '#formItemsDataTable_wrapper');
});

function openForm(I, DT) {
    I.amOnPage(formUrl);
    I.waitForFunction(() => {
        const step = document.querySelector('#formStepsDataTable tbody tr.selected');
        return step && new URL(formItemsDataTable.getAjaxUrl(), location.origin).searchParams.get('stepId') === step.id;
    }, [], 15);
    DT.waitForLoader('formItemsDataTable');
}

function openField(I, DT, DTE, label) {
    const row = locate(items + ' tbody tr').withText(label);
    I.waitForElement(row, 10);
    I.clickIfVisible(locate(items + ' tbody tr:not(.selected)').withText(label).find('td:last-child'));
    I.click(DT.btn.formItems_edit_button);
    DTE.waitForEditor('formItemsDataTable');
    I.seeElement(relationsTab);
    I.clickCss(relationsTab);
    I.waitForElement(relationsTab + '[aria-selected="true"]', 10);
}

function openSecondStep(I, DT) {
    I.click(locate('#formStepsDataTable tbody td').withText('Krok 2'));
    I.waitForText('Meno a priezvisko', 10, items);
    DT.waitForLoader('formItemsDataTable');
}

async function checkOpenedTarget(I, DTE, itemFormId, label) {
    I.waitForFunction(([id]) => document.querySelector('#formItemsDataTable_modal #DTE_Field_itemFormId')?.value === id, [itemFormId], 15);
    DTE.waitForEditor('formItemsDataTable');
    I.seeElement(basicTab + '[aria-selected="true"]');
    I.seeInField('#DTE_Field_itemFormId', itemFormId);
    I.see('Krok 2', '#formStepsDataTable tbody tr.selected');
    I.see(label, items + ' tbody tr.selected');
    I.assertEqual(await I.grabNumberOfVisibleElements('.DTED.show'), 1, 'Related fields open in the existing editor rather than a nested modal');
}

async function checkSingleUsage(I, list, name) {
    I.waitForText(name, 10, list + ' .form-item-relations-name');
    I.assertEqual(await I.grabTextFrom(list + ' .form-item-relations-name'), name, 'The field name is separate from its step');
    I.assertEqual(await I.grabTextFrom(list + ' .form-item-relations-step'), 'Krok 2', 'The target step is shown separately');
    I.assertEqual(await I.grabNumberOfVisibleElements(list + ' li'), 1, 'Each target is listed once per condition type');
    I.assertEqual(await I.grabTextFrom(list + 'Count'), '1', 'The count reflects distinct targets');
    I.dontSeeElement(list + 'Empty');
}

async function checkEmptyUsage(I, list) {
    I.see('Žiadne polia', list + 'Empty');
    I.assertEqual(await I.grabNumberOfVisibleElements(list + ' li'), 0, 'The previous field must not leave stale relationships');
    I.assertEqual(await I.grabTextFrom(list + 'Count'), '0', 'The count resets when there are no targets');
}

async function checkAddressUsage(I) {
    // Two visibility clauses use the same source; the target must still be listed once.
    for (const list of [visibilityUsage, requirementUsage]) await checkSingleUsage(I, list, 'Adresa');
}

function openPendingRelationConfirmation(I, DT, DTE) {
    openForm(I, DT);
    openSecondStep(I, DT);
    openField(I, DT, DTE, 'Meno a priezvisko');
    I.clickCss(advancedTab);
    DTE.fillField('placeholder', 'autotest-stale-confirmation-' + I.getRandomText());
    I.clickCss(relationsTab);
    I.clickCss(requirementUsage + ' button.form-item-relations-name');
    I.waitForText('Zahodiť neuložené zmeny?', 10, confirmDialog);
}

Scenario('Relations separate condition types and refresh when another field is opened', async ({ I, DT, DTE }) => {
    openForm(I, DT);
    openSecondStep(I, DT);

    openField(I, DT, DTE, 'Meno a priezvisko');
    await checkAddressUsage(I);
    DTE.cancel('formItemsDataTable');

    openField(I, DT, DTE, 'Telefónne číslo');
    for (const list of [visibilityUsage, requirementUsage]) await checkEmptyUsage(I, list);
    DTE.cancel('formItemsDataTable');

    openField(I, DT, DTE, 'Meno a priezvisko');
    await checkAddressUsage(I);
    DTE.cancel('formItemsDataTable');
});

Scenario('Relations include dependent fields in later steps', async ({ I, DT, DTE }) => {
    openForm(I, DT);
    openField(I, DT, DTE, 'Email');
    await checkSingleUsage(I, visibilityUsage, 'Telefónne číslo');
    await checkEmptyUsage(I, requirementUsage);
    DTE.cancel('formItemsDataTable');
});

Scenario('Relations are hidden for new and duplicated fields', async ({ I, DT, DTE }) => {
    openForm(I, DT);
    I.click(DT.btn.formItems_add_button);
    DTE.waitForEditor('formItemsDataTable');
    I.dontSeeElement(relationsTab);
    DTE.cancel('formItemsDataTable');

    I.click(locate(items + ' tbody tr').withText('Email').find('td:last-child'));
    I.click(DT.btn.formItems_duplicate_button);
    DTE.waitForEditor('formItemsDataTable');
    I.dontSeeElement(relationsTab);
    DTE.cancel('formItemsDataTable');

    openField(I, DT, DTE, 'Email');
    await checkSingleUsage(I, visibilityUsage, 'Telefónne číslo');
    await checkEmptyUsage(I, requirementUsage);
    DTE.cancel('formItemsDataTable');
});

Scenario('A requirement relation opens its target in the same step', async ({ I, DT, DTE }) => {
    openForm(I, DT);
    openSecondStep(I, DT);
    openField(I, DT, DTE, 'Meno a priezvisko');

    I.clickCss(requirementUsage + ' button.form-item-relations-name');
    await checkOpenedTarget(I, DTE, 'adresa-1', 'Adresa');
    I.dontSeeElement(confirmDialog);
    DTE.cancel('formItemsDataTable');
    I.see('Adresa', items + ' tbody tr.selected');
});

Scenario('A visibility relation opens a later step using the keyboard', async ({ I, DT, DTE }) => {
    openForm(I, DT);
    // Reopening another field must replace the previous editor's dirty-state baseline.
    openField(I, DT, DTE, 'Checkgroup');
    DTE.cancel('formItemsDataTable');
    openField(I, DT, DTE, 'Email');

    I.focus(visibilityUsage + ' button.form-item-relations-name');
    I.pressKey('Enter');
    await checkOpenedTarget(I, DTE, 'telefon-1', 'Telefónne číslo');
    I.dontSeeElement(confirmDialog);
    DTE.cancel('formItemsDataTable');
    I.see('Krok 2', '#formStepsDataTable tbody tr.selected');
    I.see('Telefónne číslo', items + ' tbody tr.selected');
});

Scenario('Cancelling a relation keeps unsaved changes and confirming discards them without saving', async ({ I, DT, DTE }) => {
    openForm(I, DT);
    openSecondStep(I, DT);
    openField(I, DT, DTE, 'Meno a priezvisko');
    I.clickCss(advancedTab);
    const originalPlaceholder = await I.grabValueFrom('#DTE_Field_placeholder');
    const changedPlaceholder = 'autotest-unsaved-' + I.getRandomText();
    DTE.fillField('placeholder', changedPlaceholder);
    I.clickCss(relationsTab);

    I.clickCss(visibilityUsage + ' button.form-item-relations-name');
    I.waitForText('Zahodiť neuložené zmeny?', 10, confirmDialog);
    I.click('Zrušiť', confirmDialog);
    I.waitForInvisible(confirmDialog, 10);
    I.clickCss(basicTab);
    I.seeInField('#DTE_Field_itemFormId', 'menopriezvisko-1');
    I.clickCss(advancedTab);
    I.seeInField('#DTE_Field_placeholder', changedPlaceholder);
    I.clickCss(relationsTab);

    I.clickCss(visibilityUsage + ' button.form-item-relations-name');
    I.waitForText('Zahodiť neuložené zmeny?', 10, confirmDialog);
    I.click('Otvoriť pole', confirmDialog);
    I.waitForInvisible(confirmDialog, 10);
    await checkOpenedTarget(I, DTE, 'adresa-1', 'Adresa');
    DTE.cancel('formItemsDataTable');

    // Reload from the server to verify that navigation never saved the source changes.
    openForm(I, DT);
    openSecondStep(I, DT);
    openField(I, DT, DTE, 'Meno a priezvisko');
    I.clickCss(advancedTab);
    I.seeInField('#DTE_Field_placeholder', originalPlaceholder);
    DTE.cancel('formItemsDataTable');
});

for (const field of [
    { label: 'Meno a priezvisko', id: 'menopriezvisko-1' },
    { label: 'Telefónne číslo', id: 'telefon-1' }
]) {
    Scenario('A stale relation confirmation preserves a new editor session for ' + field.id, async ({ I, DT, DTE }) => {
        openPendingRelationConfirmation(I, DT, DTE);
        DTE.cancel('formItemsDataTable');
        openField(I, DT, DTE, field.label);
        I.clickCss(advancedTab);
        const changedPlaceholder = 'autotest-new-editor-session-' + I.getRandomText();
        DTE.fillField('placeholder', changedPlaceholder);

        I.click('Otvoriť pole', confirmDialog);
        I.waitForInvisible(confirmDialog, 10);
        I.seeElement('#formItemsDataTable_modal.DTED.show');
        I.seeInField('#DTE_Field_placeholder', changedPlaceholder);
        I.clickCss(basicTab);
        I.seeInField('#DTE_Field_itemFormId', field.id);
        I.see(field.label, items + ' tbody tr.selected');

        if (field.id === 'menopriezvisko-1') {
            // Dismissing the old confirmation must preserve the new session's dirty-state baseline.
            I.clickCss(relationsTab);
            I.clickCss(requirementUsage + ' button.form-item-relations-name');
            I.waitForText('Zahodiť neuložené zmeny?', 10, confirmDialog);
            I.click('Zrušiť', confirmDialog);
            I.waitForInvisible(confirmDialog, 10);
        }
        DTE.cancel('formItemsDataTable');
    });
}

Scenario('Accepting a relation confirmation after closing its source cannot navigate on a later close', ({ I, DT, DTE }) => {
    openPendingRelationConfirmation(I, DT, DTE);
    DTE.cancel('formItemsDataTable');
    I.click('Otvoriť pole', confirmDialog);
    I.waitForInvisible(confirmDialog, 10);
    I.dontSeeElement('#formItemsDataTable_modal');

    openField(I, DT, DTE, 'Telefónne číslo');
    DTE.cancel('formItemsDataTable');
    I.see('Telefónne číslo', items + ' tbody tr.selected');
    I.dontSeeElement('#formItemsDataTable_modal');
});

Scenario('A delayed relation response cannot replace another field being edited', async ({ I, DT, DTE }) => {
    openForm(I, DT);
    openSecondStep(I, DT);
    openField(I, DT, DTE, 'Meno a priezvisko');
    const targetId = await I.executeScript(() => formItemsDataTable.EDITOR.currentJson.requirementUsage[0].id);
    const targetPath = '/admin/rest/form-items/' + targetId;
    const targetRoute = '**' + targetPath + '?*';
    let release;
    const pending = new Promise(resolve => { release = resolve; });
    let reads = 0;
    await I.mockRoute(targetRoute, async route => {
        reads++;
        const response = await route.fetch();
        await pending;
        return route.fulfill({ response });
    });

    I.clickCss(requirementUsage + ' button.form-item-relations-name');
    I.waitForInvisible('#formItemsDataTable_modal', 10);
    openField(I, DT, DTE, 'Telefónne číslo');
    I.clickCss(advancedTab);
    const changedPlaceholder = 'autotest-pending-relation-' + I.getRandomText();
    DTE.fillField('placeholder', changedPlaceholder);
    await I.seeInField('#DTE_Field_placeholder', changedPlaceholder);
    I.assertEqual(reads, 1, 'The related-field response must still be pending while another editor is opened');

    // Observe completion after the late response's success callback has run.
    await I.executeScript(([path]) => {
        window.autotestRelatedResponseCompleted = false;
        $(document).on('ajaxComplete.autotestRelations', (event, xhr, settings) => {
            if (new URL(settings.url, location.origin).pathname !== path) return;
            window.autotestRelatedResponseCompleted = true;
            $(document).off('ajaxComplete.autotestRelations');
        });
    }, [targetPath]);
    release();
    I.waitForFunction(() => window.autotestRelatedResponseCompleted === true, [], 15);
    I.seeInField('#DTE_Field_placeholder', changedPlaceholder);
    I.clickCss(basicTab);
    I.seeInField('#DTE_Field_itemFormId', 'telefon-1');
    I.see('Telefónne číslo', items + ' tbody tr.selected');
    DTE.cancel('formItemsDataTable');
    await I.stopMockingRoute(targetRoute);
});
