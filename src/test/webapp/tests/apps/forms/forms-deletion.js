Feature('apps.forms.forms-deletion @singlethread', { timeout: 180 });

let fixtureName;
let fixturePageId;

Before(({ login }) => login('admin'));

After(async ({ I, DT, DTE }) => {
    if (!fixtureName) return;
    if (fixturePageId) {
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=22704');
        DT.filterEquals('title', fixtureName);
        I.clickCss('#datatableInit tbody td.dt-select-td');
        I.click(DT.btn.delete_button);
        I.click('Zmazať', 'div.DTE_Action_Remove');
        DTE.waitForLoader();
    }
    I.amOnPage('/apps/form/admin/');
    for (const endpoint of ['/admin/rest/forms/archive-list', '/admin/rest/forms-list']) {
        const rows = (await api(I, endpoint + '/all')).content.filter(row => row.formName === fixtureName);
        for (const row of rows) await api(I, endpoint + '/editor', { action: 'remove', data: { [row.id]: row } });
    }
    fixtureName = undefined;
    fixturePageId = undefined;
});

Scenario('Form permissions', ({ DT }) => {
    DT.checkPerms('cmp_form', '/apps/form/admin/', 'formsDataTable');
});

/** Uses the authenticated browser session to create and inspect isolated test fixtures. */
async function api(I, url, body) {
    const result = await I.executeScript(async ({ url, body }) => {
        const response = await fetch(url, {
            method: body ? 'POST' : 'GET',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: body ? JSON.stringify(body) : undefined
        });
        return { status: response.status, data: await response.json() };
    }, { url, body });
    I.assertEqual(result.status, 200, url);
    I.assertFalse(Boolean(result.data.error), JSON.stringify({ error: result.data.error, fieldErrors: result.data.fieldErrors }));
    I.assertEqual((result.data.fieldErrors || []).length, 0, JSON.stringify(result.data.fieldErrors));
    return result.data;
}

Scenario('Preserve a used form, submit again, and completely remove an unused definition', async ({ I, DT, DTE }) => {
    const name = fixtureName = 'autotest-delete-' + I.getRandomText().toLowerCase();
    const query = '?formName=' + encodeURIComponent(name);
    let pageId;
    let form;
    I.amOnPage('/apps/form/admin/');
    I.waitForElement('#formsDataTable');
    form = await createForm(I, name);
    const step = (await api(I, '/admin/rest/form-steps/all' + query)).content[0];
    const itemQuery = query + '&stepId=' + step.id;
    const fields = [];
    for (const label of ['autotest-control', 'autotest-answer']) {
        const item = await api(I, '/admin/rest/form-items/-1' + itemQuery);
        Object.assign(item, { fieldType: 'meno', label, value: '', placeholder: '', tooltip: '', required: false, trimValue: false, compareInsensitive: false, showUnanswered: false });
        fields.push((await api(I, '/admin/rest/form-items/editor' + itemQuery, {
            action: 'create', data: { '-1': item }
        })).data[0]);
    }
    const conditionQuery = itemQuery + '&itemId=' + fields[1].id + '&conditionType=REQUIREMENT';
    await api(I, '/admin/rest/form-items-conditions/editor' + conditionQuery, {
        action: 'create', data: { '-1': {
            itemFormId: fields[0].itemFormId, operator: 'EQUALS', value: 'yes',
            joinOperatorType: 'AND', caseInsensitive: false, sortPriority: 10
        } }
    });

    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=22704');
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.fillField('#DTE_Field_title', name);
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.checkOption('#DTE_Field_available_0');
    I.clickCss('#pills-dt-datatableInit-content-tab');
    await DTE.fillCkeditor('!INCLUDE(sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp, formName=&quot;' + name + '&quot;,&#x20;cssTemplate=&quot;&quot;)!');
    DTE.save();
    DT.filterEquals('title', name);
    pageId = fixturePageId = (await I.grabTextFrom('#datatableInit > tbody tr:first-child > td:first-child > div')).trim();

    await submit(I, pageId, fields);
    I.amOnPage('/apps/form/admin/');
    await api(I, '/admin/rest/forms-list/action/archiveForm?ids%5B%5D=' + form.id, {});
    await submit(I, pageId, fields);

    I.amOnPage('/apps/form/admin/');
    removeActiveForm(I, DT, name, true);
    I.see('Dáta formulára boli zmazané.');
    I.see('docId: ' + pageId);
    I.see('dáta formulára sú v archíve');
    I.assertEqual((await api(I, '/admin/rest/forms-list/all?detail=true&size=100&formName=' + name)).content.length, 0);
    I.assertEqual((await api(I, '/admin/rest/form-items-conditions/all' + conditionQuery)).content.length, 1);
    I.assertEqual(await settingsCount(I, name), 1);

    const archived = (await api(I, '/admin/rest/forms/archive-list/all')).content.find(row => row.formName === name);
    const archiveDeleted = await api(I, '/admin/rest/forms/archive-list/editor', {
        action: 'remove', data: { [archived.id]: archived }
    });
    I.assertTrue(archiveDeleted.notify.some(notice => notice.text.includes('aktívnych formulárov')));
    I.assertEqual(await settingsCount(I, name), 1);
    await submit(I, pageId, fields);

    I.amOnPage('/apps/form/admin/');
    removeActiveForm(I, DT, name, true);
    I.see('docId: ' + pageId);
    I.assertEqual((await api(I, '/admin/rest/forms-list/all?detail=true&size=100&formName=' + name)).content.length, 0);
    I.assertEqual((await api(I, '/admin/rest/form-steps/all' + query)).content.length, 1);
    I.assertEqual((await api(I, '/admin/rest/form-items/all' + itemQuery)).content.length, 2);

    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + pageId);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
    I.uncheckOption('#DTE_Field_available_0');
    DTE.save();
    I.amOnPage('/apps/form/admin/');
    removeActiveForm(I, DT, name, false);
    I.see('Formulár bol zmazaný vrátane nastavení a položiek.');
    I.assertEqual(await settingsCount(I, name), 0);
    I.assertEqual((await api(I, '/admin/rest/form-steps/all' + query)).content.length, 0);
    I.assertEqual((await api(I, '/admin/rest/form-items/all' + itemQuery)).content.length, 0);
    I.assertEqual((await api(I, '/admin/rest/form-items-conditions/all' + conditionQuery)).content.length, 0);

    form = await createForm(I, name);
    I.assertEqual(await settingsCount(I, name), 1, 'The deleted name can be reused with one settings record');

});

async function createForm(I, name) {
    const entity = await api(I, '/admin/rest/forms-list/-1');
    entity.formName = name;
    entity.formSettings.recipients = '';
    entity.formSettings.addTechInfo = false;
    return (await api(I, '/admin/rest/forms-list/editor', { action: 'create', data: { '-1': entity } })).data[0];
}

async function submit(I, pageId, fields) {
    I.amOnPage('/showdoc.do?docid=' + pageId);
    I.waitForElement('.multistep-form-app input#f1-' + fields[0].itemFormId, 10);
    I.fillField('#f1-' + fields[0].itemFormId, 'yes');
    I.fillField('#f1-' + fields[1].itemFormId, 'autotest response');
    I.click('.multistep-form-app button[name="saveForm"]');
    I.waitForText('Formulár bol úspešne odoslaný', 10);
}

async function settingsCount(I, name) {
    return (await api(I, '/admin/rest/form-settings/search/findByColumns?searchFormName=' + encodeURIComponent('^' + name + '$'))).content.length;
}

function removeActiveForm(I, DT, name, preserve) {
    DT.filterEquals('formName', name);
    I.clickCss('#formsDataTable tbody td.dt-select-td');
    I.clickCss('#formsDataTable_wrapper button.buttons-remove');
    I.waitForElement('div.DTE_Action_Remove', 10);
    I.click('Zmazať', 'div.DTE_Action_Remove');
    I.waitForInvisible('div.DTE_Action_Remove', 10);
    DT.waitForLoader('formsDataTable');
    if (preserve) I.waitForText(name, 10, '#formsDataTable');
    else I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 10, '#formsDataTable');
}
