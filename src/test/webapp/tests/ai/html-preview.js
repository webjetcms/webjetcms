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
function openChat(I, DTE, pageBuilder = false, Document) {
    I.mockRoute('**/admin/rest/ai/chat/threads?*', route => route.fulfill({
        status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], hasMore: false })
    }));
    I.mockRoute('**/admin/rest/ai/assistant/response/', route => route.fulfill({
        status: 200, contentType: 'application/json', body: JSON.stringify({ response: reply, totalTokens: 7 })
    }));
    if (pageBuilder) Document.resetPageBuilderMode();
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${pageBuilder ? process.env.CODECEPT_PAGEBUILDER_DOCID || 57 : 16}`);
    DTE.waitForEditor();
    I.executeScript(async pageBuilder => {
        const field = window.webpagesDatatable.EDITOR.field('data');
        await field.s.opts.wjeditorReadyPromise;
        if (!pageBuilder) await field.s.opts.wjeditor.switchEditingMode('', false);
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
        unchanged: window.webpagesDatatable.EDITOR.get('data') === window.autotestPreviewOriginal,
        text: marker.textContent,
        width: iframe.contentWindow.innerWidth
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
    I.assertEqual(state.width, 1280);
    I.saveScreenshot('ai-html-preview.png');
    I.moveCursorTo(`${card} button[aria-label="Desktop"]`);
    I.waitForText('Desktop', 5, '.tooltip.show');
    I.clickCss(`${card} button[aria-label="Tablet"]`);
    I.assertEqual((await I.executeScript(previewState)).width, 768);
    I.assertEqual(await I.grabAttributeFrom(`${card} button[aria-label="Tablet"]`, 'aria-pressed'), 'true');
    I.clickCss(`${card} button[aria-label="Mobil"]`);
    I.assertEqual((await I.executeScript(previewState)).width, 375);
    I.click('HTML kód', card);
    I.see('<section class="autotest-preview-marker">', `${card} pre`);
    I.click('Náhľad', card);
    I.clickCss(`${card} button[aria-label="Maximalizovať okno"]`);
    I.waitForVisible(`${container} .ai-chat-html-expanded`, 5);
    I.saveScreenshot('ai-html-preview-expanded.png');
    I.switchTo(`${card} .ai-chat-html-preview iframe`);
    I.clickCss('h1');
    I.pressKey('Escape');
    I.switchTo();
    I.waitForInvisible(`${container} .ai-chat-html-expanded`, 5);
    I.seeElement(card);
    I.clickCss(`${card} button[aria-label="Maximalizovať okno"]`);
    I.pressKey('Escape');
    I.waitForInvisible(`${container} .ai-chat-html-expanded`, 5);
    I.clickCss(`${card} button[aria-label="Maximalizovať okno"]`);
    I.clickCss(`${container} .ai-chat-html-expanded summary`);
    I.waitForInvisible(`${container} .ai-chat-html-expanded`, 5);
    I.clickCss(`${card} summary`);
    I.waitForVisible(`${card} iframe`, 5);
    for (const [width, height] of [[1100, 800], [600, 700]]) {
        I.resizeWindow(width, height);
        I.seeElement(`${card} .ai-chat-html-preview iframe`);
        I.assertTrue(await I.executeScript(() => {
            const bounds = document.querySelector('#toast-container-ai .ai-chat-html-response').getBoundingClientRect();
            return bounds.width > 0 && bounds.left >= 0 && bounds.right <= innerWidth;
        }), 'The HTML response must fit in the chat at smaller widths.');
    }
    I.clickCss(`${container} .toast-close-button`);
    DTE.cancel();
    I.wjSetDefaultWindowSize();
});

Scenario('Page Builder response previews inherit the loaded page styles', async ({ I, DTE, Document }) => {
    openChat(I, DTE, true, Document);
    I.waitForVisible(`${card} .ai-chat-html-preview iframe`, 10);
    I.switchTo(`${card} .ai-chat-html-preview iframe`);
    I.waitForText('HTML preview autotest', 10);
    I.switchTo();
    const state = await I.executeScript(previewState);
    I.assertEqual(state.color, 'rgb(12, 89, 123)');
    I.assertTrue(state.unchanged);
    I.saveScreenshot('ai-html-preview-page-builder.png');
    I.clickCss(`${container} .toast-close-button`);
    DTE.cancel();
});

Scenario('Older previews collapse and reopening the chat recreates only the visible preview', async ({ I, DTE }) => {
    openChat(I, DTE);
    I.waitForVisible(`${card} iframe`, 10);
    I.fillField(input, 'Revise the HTML preview autotest');
    I.clickCss(`${container} .btn-ai-chat-send`);
    I.waitForElement(locate(`${container} [data-ai-action="copy"]`).at(2), 10);
    const openPreviews = () => [...document.querySelectorAll('#toast-container-ai .ai-chat-html-response')].map(element => element.open);
    I.assertDeepEqual(await I.executeScript(openPreviews), [false, true]);
    I.click(locate(`${card} summary`).at(1));
    I.waitForVisible(`${card} iframe`, 10);
    I.assertDeepEqual(await I.executeScript(openPreviews), [true, false]);
    I.executeScript(() => { window.autotestPreviewFrame = document.querySelector('#toast-container-ai .ai-chat-html-preview iframe'); });
    I.clickCss(`${container} .toast-close-button`);
    I.assertTrue(await I.executeScript(() => window.autotestPreviewFrame.parentElement === null), 'Unmounting must release the preview document.');
    I.waitForDetached(container, 5);
    I.clickCss('.DTE_Field_Name_htmlData .btn-ai');
    I.waitForVisible(`${container} .btn-ai-action`, 5);
    I.click(locate(`${container} .btn-ai-action`).withText('HTML preview autotest'));
    I.waitForVisible(`${card} iframe`, 10);
    I.assertDeepEqual(await I.executeScript(openPreviews), [true, false]);
    I.assertTrue((await I.executeScript(previewState)).unchanged);
    I.clickCss(`${container} .toast-close-button`);
    DTE.cancel();
});
