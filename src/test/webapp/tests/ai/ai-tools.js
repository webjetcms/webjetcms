Feature('ai.ai-tools');

const table = 'aiToolsDataTable';
const modal = '#' + table + '_modal';
const inputTree = modal + ' .DTE_Field_Name_inputSchema .ai-tool-schema';
const outputTree = modal + ' .DTE_Field_Name_response .ai-tool-schema';

Before(({ I, login, DT }) => {
    login('admin');
    I.amOnPage('/admin/v9/settings/ai-tools/');
    I.uncheckOption('#ai-tool-group-by-controller');
    I.waitForVisible('#' + table, 10);
    DT.waitForLoader(table);
});

/** Verifies nested requirements, resolved models and the original JSON schema without calling a write tool. */
Scenario('web page tool preview resolves nested schemas and preserves examples', async ({ I, DT, DTE }) => {
    DT.filterEquals('name', 'webpages_add', '#' + table + '_wrapper');
    I.click('webpages_add', '#' + table + ' tbody');
    DTE.waitForEditor(table);
    I.clickCss('#pills-dt-' + table + '-input-tab');
    I.seeInField(modal + ' #DTE_Field_required', 'body, body.title, body.navbar');

    const example = JSON.parse(await I.grabValueFrom(modal + ' #DTE_Field_inputSchema'));
    I.assertDeepEqual(example, { body: { title: 'string', navbar: 'string' } });
    I.dontSeeElement(inputTree);

    I.clickCss('label[for="ai-tool-input-view-schema"]');
    I.waitForVisible(inputTree, 10);
    I.seeElement(inputTree + ' details[data-property="body"][open]');
    for (const name of ['title', 'navbar']) {
        const property = inputTree + ' [data-property="body.' + name + '"]';
        I.seeElement(property + ' .ai-tool-schema-required');
        I.see('string', property + ' .ai-tool-schema-type');
        I.seeElement(property + ' .ai-tool-schema-description');
    }
    I.dontSeeElement(inputTree + ' [data-property="body.groupId"] .ai-tool-schema-required');
    I.dontSee('$defs', inputTree);
    I.dontSee('$ref', inputTree);
    I.dontSee('doc_details_', inputTree);
    I.saveScreenshot('autotest-ai-tools-schema.png');
    I.resizeWindow(700, 760);
    I.seeElement(inputTree + ' [data-property="body.title"] .ai-tool-schema-required');
    I.saveScreenshot('autotest-ai-tools-schema-mobile.png');
    I.wjSetDefaultWindowSize();

    const nested = inputTree + ' details[data-property="body.editorFields"]';
    I.seeElement(nested + ':not([open])');
    I.clickCss(nested + ' > summary');
    I.waitForVisible(inputTree + ' [data-property="body.editorFields.redactorNote"]', 10);
    I.see('string', inputTree + ' [data-property="body.editorFields.redactorNote"] .ai-tool-schema-type');
    I.clickCss(nested + ' > summary');
    I.dontSeeElement(inputTree + ' [data-property="body.editorFields.redactorNote"]');

    I.clickCss('label[for="ai-tool-input-view-json"]');
    I.dontSeeElement(inputTree);
    const schema = JSON.parse(await I.grabValueFrom(modal + ' #DTE_Field_inputSchema'));
    I.assertDeepEqual(schema.required, ['body']);
    I.assertStartsWith(schema.properties.body.$ref, '#/$defs/');
    const body = schema.$defs[schema.properties.body.$ref.substring('#/$defs/'.length)];
    I.assertDeepEqual(body.required, ['title', 'navbar']);

    I.clickCss('label[for="ai-tool-input-view-example"]');
    I.assertDeepEqual(JSON.parse(await I.grabValueFrom(modal + ' #DTE_Field_inputSchema')), example);
    I.dontSeeElement(inputTree);

    I.clickCss('#pills-dt-' + table + '-output-tab');
    I.clickCss('label[for="ai-tool-output-view-schema"]');
    I.waitForVisible(outputTree, 10);
    I.see('string', outputTree + ' [data-property="title"] .ai-tool-schema-type');
    I.dontSee('$defs', outputTree);
    I.dontSee('$ref', outputTree);
    I.dontSee('doc_details_', outputTree);
    DTE.cancel(table);

    I.click('webpages_add', '#' + table + ' tbody');
    DTE.waitForEditor(table);
    I.clickCss('#pills-dt-' + table + '-input-tab');
    I.seeCheckboxIsChecked('#ai-tool-input-view-example');
    I.assertDeepEqual(JSON.parse(await I.grabValueFrom(modal + ' #DTE_Field_inputSchema')), example);
    DTE.cancel(table);
});

Scenario('tool previews require the AI tools permission', ({ DT }) => {
    DT.checkPerms('cmp_ai_tools', '/admin/v9/settings/ai-tools/', table);
});
