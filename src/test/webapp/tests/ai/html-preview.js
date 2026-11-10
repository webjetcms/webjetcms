Feature('ai.html-preview');

const container = '#toast-container-ai';
const card = `${container} .ai-chat-html-response`;
const input = `${container} .ai-chat-input`;
const reply = '```html\n<section class="autotest-preview-marker"><h1>HTML preview autotest</h1>'
    + '<p>A styled response without changing the editor.</p>'
    + '<script>window.top.autotestPreviewExecuted = true;</script>'
    + '<img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" onload="window.top.autotestPreviewExecuted = true">'
    + '<iframe src="/admin/"></iframe><a href="/admin/" target="_top">Link autotest</a>'
    + '</section>\n```';

Before(({ login }) => login('admin'));

/** Uses a field launcher whose assistant targets the real page editor without saving any test data. */
function openChat(I, DTE, pageBuilder = false) {
    I.mockRoute('**/admin/rest/ai/chat/threads?*', route => route.fulfill({
        status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], hasMore: false })
    }));
    I.mockRoute('**/admin/rest/ai/assistant/response/', route => route.fulfill({
        status: 200, contentType: 'application/json', body: JSON.stringify({ response: route.request().postDataJSON().userPrompt.startsWith('Revise')
            ? reply.replace('HTML preview autotest', 'Revised preview autotest') : reply, totalTokens: 7 })
    }));
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${pageBuilder ? process.env.CODECEPT_PAGEBUILDER_DOCID || 57 : 16}`);
    DTE.waitForEditor();
    I.executeScript(async pageBuilder => {
        const field = window.webpagesDatatable.EDITOR.field('data');
        await field.s.opts.wjeditorReadyPromise;
        const mode = pageBuilder ? 'pageBuilder' : '';
        if (field.s.opts.wjeditor.editingMode !== mode) await field.s.opts.wjeditor.switchEditingMode(mode, false);
    }, pageBuilder);
    if (pageBuilder) {
        // Page Builder initializes its document after the editor has switched modes.
        I.waitForFunction(() => document.getElementById('DTE_Field_data-pageBuilderIframe')?.contentWindow?.pageBuilderReady === true, [], 20);
    }
    I.executeScript(pageBuilder => {
        const editor = window.webpagesDatatable.EDITOR;
        const wjeditor = editor.field('data').s.opts.wjeditor;
        const doc = pageBuilder ? document.getElementById('DTE_Field_data-pageBuilderIframe').contentDocument : wjeditor.ckEditorInstance.document.$;
        const style = doc.createElement('style');
        const wrapper = pageBuilder ? '.autotest-preview-wrapper ' : '';
        style.textContent = `.autotest-preview-context ${wrapper}.autotest-preview-marker { color: rgb(12, 89, 123); background-color: rgb(232, 244, 250); padding: 24px; }`;
        doc.head.append(style);
        doc.body.classList.add('autotest-preview-context');
        if (pageBuilder) doc.getElementById('wjInline-docdata').classList.add('autotest-preview-wrapper');
        window.autotestPreviewOriginal = editor.get('data');
        window.autotestPreviewExecuted = false;
        const column = editor.TABLE.DATA.fields.find(item => item.name === 'htmlData');
        column.ai = [{ assistantId: -100, action: 'live_chat', provider: 'gemini', providerTitle: 'Gemini',
            description: 'HTML preview autotest', icon: 'sparkles', from: 'data', to: 'data', persistentChat: false }];
        editor.editorAi.bindEditorButtons();
    }, pageBuilder);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    I.clickCss('.DTE_Field_Name_htmlData .btn-ai');
    I.click(locate(`${container} .btn-ai-action`).withText('HTML preview autotest'));
    I.waitForVisible(input, 10);
    I.fillField(input, 'Create an HTML preview autotest');
    I.clickCss(`${container} .btn-ai-chat-send`);
    I.waitForVisible(`${container} [data-ai-action="copy"]`, 10);
}

/** Reads rendered content, stylesheet effects and sandbox results through the permitted parent access. */
function previewState() {
    const iframe = document.querySelector('#toast-container-ai .ai-chat-html-preview iframe');
    const doc = iframe.contentDocument;
    const marker = doc.querySelector('.autotest-preview-marker');
    return {
        color: iframe.contentWindow.getComputedStyle(marker).color,
        scripts: doc.querySelectorAll('script, iframe, [onload]').length,
        executed: window.autotestPreviewExecuted,
        unchanged: window.webpagesDatatable.EDITOR.get('data') === window.autotestPreviewOriginal
    };
}

Scenario('A styled HTML response has an isolated preview, source and expansion', async ({ I, DTE }) => {
    openChat(I, DTE);
    I.waitForVisible(`${card} .ai-chat-html-preview iframe`, 10);
    I.switchTo(`${card} .ai-chat-html-preview iframe`);
    I.waitForText('HTML preview autotest', 10);
    I.switchTo();
    const state = await I.executeScript(previewState);
    I.assertEqual(state.color, 'rgb(12, 89, 123)', 'The preview must use the page editor styles and body classes.');
    I.assertEqual(state.scripts, 0);
    I.assertFalse(state.executed, 'Generated scripts and event handlers must not run.');
    I.assertTrue(state.unchanged, 'Opening a preview must not apply the answer to the editor.');
    I.click('HTML kód', card);
    I.see('<section class="autotest-preview-marker">', `${card} pre`);
    I.click('Náhľad', card);
    I.clickCss(`${card} button[aria-label="Maximalizovať okno"]`);
    I.waitForVisible(`${container} .ai-chat-html-expanded`, 5);
    I.switchTo(`${card} .ai-chat-html-preview iframe`);
    I.clickCss('h1');
    I.pressKey('Escape');
    I.switchTo();
    I.waitForInvisible(`${container} .ai-chat-html-expanded`, 5);
    I.clickCss(`${container} .toast-close-button`);
    DTE.cancel();
});

Scenario('Page Builder response previews inherit the loaded page styles', async ({ I, DTE }) => {
    openChat(I, DTE, true);
    I.waitForVisible(`${card} .ai-chat-html-preview iframe`, 10);
    I.switchTo(`${card} .ai-chat-html-preview iframe`);
    I.waitForText('HTML preview autotest', 10);
    I.switchTo();
    const state = await I.executeScript(previewState);
    I.assertEqual(state.color, 'rgb(12, 89, 123)');
    I.assertTrue(state.unchanged);
    I.clickCss(`${container} .toast-close-button`);
    DTE.cancel();
});

Scenario('Reopening the chat restores the selected HTML preview', ({ I, DTE }) => {
    openChat(I, DTE);
    I.fillField(input, 'Revise the HTML preview autotest');
    I.clickCss(`${container} .btn-ai-chat-send`);
    const first = locate(card).at(1);
    const second = locate(card).at(2);
    I.waitForVisible(second.find('iframe'), 10);
    I.dontSeeElement(first.find('iframe'));
    I.click(first.find('summary'));
    I.waitForVisible(first.find('iframe'), 5);
    I.clickCss(`${container} .toast-close-button`);
    I.waitForDetached(container, 5);
    I.clickCss('.DTE_Field_Name_htmlData .btn-ai');
    I.click(locate(`${container} .btn-ai-action`).withText('HTML preview autotest'));
    I.waitForVisible(first.find('iframe'), 10);
    I.dontSeeElement(second.find('iframe'));
    I.switchTo(first.find('iframe'));
    I.waitForText('HTML preview autotest', 5);
    I.switchTo();
    I.clickCss(`${container} .toast-close-button`);
    DTE.cancel();
});
