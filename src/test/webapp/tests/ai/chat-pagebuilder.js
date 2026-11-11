Feature('ai.chat-pagebuilder');

const host = '#toast-container-ai';
const frame = '#DTE_Field_data-pageBuilderIframe';
const undo = `${host} button[data-ai-action="undo"]`;
const pageId = process.env.CODECEPT_PAGEBUILDER_DOCID || 57;

Before(({ login }) => login('admin'));

// Readiness of the CKEditor instances inside the frame isn't observable from the parent DOM.
function regionsReady() {
    const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow;
    const regions = frame?.document.querySelectorAll('#wjInline-docdata [data-ckeditor-instance]');
    return frame?.pageBuilderReady && regions.length > 0 &&
        Array.from(regions).every(node => frame.CKEDITOR.instances[node.dataset.ckeditorInstance]?.status === 'ready');
}

async function replaceContent(I, DTE) {
    I.mockRoute('**/admin/rest/ai/chat/threads?*', route => route.fulfill({ json: { items: [], hasMore: false } }));
    I.mockRoute('**/admin/rest/ai/assistant/response/', route => route.fulfill({
        json: { response: '```html\n<h2>Autotest requirements</h2><ol><li>First</li><li>Second</li></ol>\n```', totalTokens: 7 }
    }));
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + pageId);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-content-tab');
    I.executeScript(async () => {
        const editor = window.webpagesDatatable.EDITOR;
        const field = editor.field('data');
        await field.s.opts.wjeditorReadyPromise;
        if (field.s.opts.wjeditor.editingMode !== 'pageBuilder') await field.s.opts.wjeditor.switchEditingMode('pageBuilder', false);
        const column = editor.TABLE.DATA.fields.find(item => item.name === 'data');
        column.ai = [{ assistantId: -100, action: 'live_chat', provider: 'gemini', description: 'PageBuilder autotest',
            from: 'data', to: 'data', persistentChat: false }];
        editor.editorAi.bindEditorButtons();
    });
    I.waitForFunction(regionsReady, [], 20);
    const original = await I.executeScript(() => window.webpagesDatatable.EDITOR.get('data'));
    I.switchTo(frame);
    I.clickCss('span.cke_button__aibutton_icon');
    I.switchTo();
    I.click(locate(`${host} .btn-ai-action`).withText('PageBuilder autotest'));
    I.fillField(`${host} .ai-chat-input`, 'Prepare a list of requirements');
    I.clickCss(`${host} .btn-ai-chat-send`);
    I.waitForVisible(`${host} button[data-ai-action="replace"]`, 10);
    I.clickCss(`${host} button[data-ai-action="replace"]`);
    I.waitForEnabled(undo, 10);
    I.switchTo(frame);
    I.waitForText('Autotest requirements', 10, '#wjInline-docdata');
    I.switchTo();
    return original;
}

Scenario('Undo restores the original Page Builder content', async ({ I, DTE }) => {
    const original = await replaceContent(I, DTE);
    I.clickCss(undo);
    I.waitForInvisible(undo, 10);
    I.assertEqual(await I.executeScript(() => window.webpagesDatatable.EDITOR.get('data')), original);
    I.clickCss(`${host} .toast-close-button`);
    DTE.cancel();
});

Scenario('Undo preserves a manual edit made after a Page Builder chat replacement', async ({ I, DTE }) => {
    await replaceContent(I, DTE);
    I.clickCss(`${host} .ai-chat-minimize`);
    I.switchTo(frame);
    I.fillField('#wjInline-docdata [contenteditable="true"]', 'Autotest manual edit');
    I.switchTo();
    I.clickCss(`${host} .ai-chat-restore`);
    I.clickCss(undo);
    const stale = await I.executeScript(() => WJ.translate('components.ai_assistants.chat.undoStale.js'));
    I.waitForText(stale, 5, host);
    I.clickCss(`${host} .toast-close-button`);
    I.switchTo(frame);
    I.see('Autotest manual edit', '#wjInline-docdata');
    I.switchTo();
    DTE.cancel();
});
