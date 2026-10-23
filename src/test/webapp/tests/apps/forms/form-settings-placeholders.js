Feature('apps.forms.form-settings-placeholders @singlethread', { timeout: 180 });

let marker;
let fieldType;
let pageId;
const forms = [];
const subject = 'Subject "quoted" & <text>';
const html = '<strong class="settings-html">Stored HTML ${id} ${settings.subject}</strong>';
const inputTemplate = '<div class="settings-probe" data-subject="${settings.subject}"><div class="settings-custom">${settings.fieldA}</div><div class="settings-before">${settings.emailTextBefore}</div><div class="settings-after">${settings.emailTextAfter}</div><span class="settings-name">${settings.formName}</span><span class="settings-limit">${settings.maxSizeInKilobytes}</span></div>';

Before(({ I, login }) => {
    login('admin');
    if (!marker) {
        marker = process.env.CODECEPT_SETTINGS_MARKER || 'autotest-settings-' + I.getRandomTextShort().toLowerCase();
        fieldType = marker;
    }
});

/** Uses the authenticated application's REST API for isolated fixtures. */
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
    I.assertFalse(Boolean(result.data.error), JSON.stringify(result.data.error));
    I.assertEqual((result.data.fieldErrors || []).length, 0, JSON.stringify(result.data.fieldErrors));
    return result.data;
}

Scenario('Create isolated settings and translation fixtures', async ({ I }) => {
    I.amOnPage('/apps/form/admin/');
    I.waitForElement('#formsDataTable');
    for (const [prefix, text] of [['label', marker], ['input', inputTemplate]]) {
        await api(I, '/admin/v9/settings/translation-keys/editor', {
            action: 'create', data: { '-1': { key: `components.formsimple.${prefix}.${fieldType}`, fieldA: text, fieldB: text, fieldC: text } }
        });
    }
    for (const type of ['simple', 'multistep']) {
        const entity = await api(I, '/admin/rest/forms-list/-1');
        entity.formName = marker + '-' + type;
        entity.formType = type;
        Object.assign(entity.formSettings, {
            recipients: '', subject, fieldA: html,
            emailTextBefore: '<em>Before HTML</em>', emailTextAfter: '<em>After HTML</em>',
            maxSizeInKilobytes: 1234, addTechInfo: false
        });
        const created = await api(I, '/admin/rest/forms-list/editor', { action: 'create', data: { '-1': entity } });
        const form = created.data[0];
        forms.push(form);
        if (type !== 'multistep') continue;
        const query = '?formName=' + encodeURIComponent(form.formName);
        const firstStep = (await api(I, '/admin/rest/form-steps/all' + query)).content[0];
        const next = await api(I, '/admin/rest/form-steps/-1' + query);
        next.sortPriority = 20;
        next.header = 'Second step';
        const secondStep = (await api(I, '/admin/rest/form-steps/editor' + query, {
            action: 'create', data: { '-1': next }
        })).data[0];
        for (const step of [firstStep, secondStep]) {
            const itemQuery = query + '&stepId=' + step.id;
            const item = await api(I, '/admin/rest/form-items/-1' + itemQuery);
            Object.assign(item, { fieldType, label: marker, required: false, trimValue: false, compareInsensitive: false, showUnanswered: false });
            await api(I, '/admin/rest/form-items/editor' + itemQuery, { action: 'create', data: { '-1': item } });
        }
    }
});

Scenario('Persist HTML settings and render the administration preview', async ({ I, DT, DTE }) => {
    for (const form of forms) {
        I.amOnPage('/apps/form/admin/');
        DT.filterEquals('formName', form.formName);
        I.clickCss('#formsDataTable tbody td.dt-select-td');
        I.clickCss('#formsDataTable_wrapper button.buttons-edit');
        DTE.waitForEditor('formsDataTable');
        I.clickCss('#pills-dt-formsDataTable-fields-tab');
        I.seeInField('#DTE_Field_formSettings-fieldA', html);
        I.fillField('#DTE_Field_formSettings-fieldB', '<i>Persisted optional field B</i>');
        DTE.save('formsDataTable');
    }
    I.amOnPage('/apps/form/admin/form-steps/?formName=' + forms[1].formName);
    I.waitForElement('.stepPreviewWrapper .settings-probe');
    checkProbe(I, '.stepPreviewWrapper', forms[1].formName);
});

Scenario('Render JSP and multistep settings and navigate between steps', async ({ I, DT, DTE }) => {
    const editorData = Buffer.from(encodeURIComponent(JSON.stringify([
        { fieldType, label: marker, value: '', placeholder: '', tooltip: '', required: false }
    ]))).toString('base64');
    const content = `<div class="autotest-simple">!INCLUDE(/components/formsimple/form.jsp, formName=${forms[0].formName}, editorData=${editorData})!</div>` +
        `!INCLUDE(sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp, formName=${forms[1].formName})!`;
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=22704');
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.fillField('#DTE_Field_title', marker);
    await DTE.fillCkeditor(content);
    DTE.save();
    DT.filterEquals('title', marker);
    I.clickCss('#datatableInit tbody td.dt-select-td');
    I.click(DT.btn.edit_button);
    DTE.waitForEditor();
    pageId = await I.grabValueFrom('#DTE_Field_docId');
    DTE.cancel();

    I.amOnPage('/showdoc.do?docid=' + pageId);
    I.waitForElement('.autotest-simple .settings-probe');
    I.waitForElement('.multistep-form-app .settings-probe');
    checkProbe(I, '.autotest-simple', forms[0].formName);
    checkProbe(I, '.multistep-form-app', forms[1].formName);
    I.click('.multistep-form-app button[name="saveForm"]');
    I.waitForElement('.multistep-form-app [data-multistep-back-step]');
    checkProbe(I, '.multistep-form-app', forms[1].formName);
    I.click('.multistep-form-app [data-multistep-back-step]');
    I.waitForInvisible('.multistep-form-app [data-multistep-back-step]');
    checkProbe(I, '.multistep-form-app', forms[1].formName);
});

Scenario('Cleanup isolated settings fixtures', async ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=22704');
    DT.filterEquals('title', marker);
    if (await I.grabNumberOfVisibleElements('#datatableInit tbody td.dt-select-td')) {
        I.clickCss('#datatableInit tbody td.dt-select-td');
        I.click(DT.btn.delete_button);
        I.click('Zmazať', 'div.DTE_Action_Remove');
        DTE.waitForLoader();
    }
    I.amOnPage('/apps/form/admin/');
    const storedForms = (await api(I, '/admin/rest/forms-list/all?size=1000')).content;
    for (const form of storedForms.filter(form => [marker + '-simple', marker + '-multistep'].includes(form.formName))) {
        await api(I, '/admin/rest/forms-list/editor', { action: 'remove', data: { [form.id]: form } });
    }
    const storedKeys = (await api(I, '/admin/v9/settings/translation-keys/search/findByColumns?sort=key,asc&key=' + encodeURIComponent(marker))).content;
    for (const translation of storedKeys.filter(key => ['components.formsimple.label.' + marker, 'components.formsimple.input.' + marker].includes(key.key))) {
        await api(I, '/admin/v9/settings/translation-keys/editor', { action: 'remove', data: { [translation.id]: translation } });
    }
});

function checkProbe(I, container, formName) {
    I.see('Stored HTML ${id} ${settings.subject}', container + ' .settings-html');
    I.see('Before HTML', container + ' .settings-before em');
    I.see('After HTML', container + ' .settings-after em');
    I.see(formName, container + ' .settings-name');
    I.see('1234', container + ' .settings-limit');
    I.seeElement(container + ' .settings-probe[data-subject="Subject \\"quoted\\" & <text>"]');
}
