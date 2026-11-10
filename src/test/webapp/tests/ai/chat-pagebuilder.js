Feature('ai.chat-pagebuilder');

const assert = require('node:assert/strict');
const pageId = process.env.CODECEPT_PAGEBUILDER_DOCID || 57;

Before(({ login }) => login('admin'));

// Readiness of the CKEditor instances inside the frame isn't observable from the parent DOM.
function regionsReady() {
    const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow;
    const regions = frame?.document.querySelectorAll('#wjInline-docdata [data-ckeditor-instance]');
    return frame?.pageBuilderReady && regions.length > 0 &&
        Array.from(regions).every(node => frame.CKEDITOR.instances[node.dataset.ckeditorInstance]?.status === 'ready');
}

async function replaceContent(I, DTE, Document) {
    Document.resetPageBuilderMode();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + pageId);
    DTE.waitForEditor();
    I.waitForFunction(regionsReady, [], 20);
    const original = await I.executeScript(async () => {
        const editor = window.webpagesDatatable.EDITOR;
        const ai = editor.editorAi;
        const column = editor.TABLE.DATA.fields.find(item => item.name === 'data');
        const assistant = { assistantId: -100, action: 'live_chat', provider: 'gemini', to: 'data', from: 'data' };
        const conversation = ai.getChatConversation(null, { ...column, ai: [assistant] }, assistant);
        const original = editor.get('data');
        const message = { status: 'complete', text: '```html\n<h2>Autotest requirements</h2><ol><li>First</li><li>Second</li></ol>\n```' };
        await ai.insertChatAnswer(conversation, message, 'replace');
        return original;
    });
    I.waitForFunction(regionsReady, [], 20);
    const beforeUndo = await I.executeScript(() => window.webpagesDatatable.EDITOR.get('data'));
    assert.match(beforeUndo, /Autotest requirements/);
    return original;
}

async function undoReplacement(I) {
    return I.executeScript(async () => {
        const ai = window.webpagesDatatable.EDITOR.editorAi;
        const conversation = [...ai.chatConversations.values()].find(item => item.aiCol.to === 'data');
        await ai.undoChatInsertion(conversation);
        return conversation.chat.state.notice;
    });
}

Scenario('Undo restores Page Builder content after a chat replacement initializes its inline editors', async ({ I, DTE, Document }) => {
    const original = await replaceContent(I, DTE, Document);
    assert.equal(await undoReplacement(I), null);
    I.waitForFunction(regionsReady, [], 20);
    assert.equal(await I.executeScript(() => window.webpagesDatatable.EDITOR.get('data')), original);
});

Scenario('Undo preserves a manual edit made after a Page Builder chat replacement', async ({ I, DTE, Document }) => {
    await replaceContent(I, DTE, Document);
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.fillField('#wjInline-docdata [contenteditable="true"]', 'Autotest manual edit');
    I.switchTo();
    const stale = await I.executeScript(() => WJ.translate('components.ai_assistants.chat.undoStale.js'));
    assert.equal(await undoReplacement(I), stale);
    assert.match(await I.executeScript(() => window.webpagesDatatable.EDITOR.get('data')), /Autotest manual edit/);
});
