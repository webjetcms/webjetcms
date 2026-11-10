Feature('ai.chat-window');

const container = '#toast-container-ai';
const dialog = `${container} .toast.toast-info`;
const input = `${container} .ai-chat-input`;
const minimize = `${container} .ai-chat-minimize`;
const maximize = `${container} .ai-chat-maximize`;
const restore = `${container} .ai-chat-restore`;
const field = '#DTE_Field_htmlData';

Before(({ login }) => login('admin'));

/** Uses page-local assistant metadata and mocked responses without changing server configuration. */
function openChat(I, DTE) {
    I.mockRoute('**/admin/rest/ai/chat/threads?*', route => route.fulfill({
        status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], hasMore: false })
    }));
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=16');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    I.executeScript(() => {
        const editor = window.webpagesDatatable.EDITOR;
        const column = editor.TABLE.DATA.fields.find(item => item.name === 'htmlData');
        column.ai = [{ assistantId: -100, action: 'live_chat', provider: 'gemini', providerTitle: 'Gemini',
            description: 'Chat window autotest', icon: 'sparkles', from: 'htmlData', to: 'htmlData', persistentChat: false }];
        editor.editorAi.bindEditorButtons();
    });
    I.clickCss('.DTE_Field_Name_htmlData .btn-ai');
    I.click(locate(`${container} .btn-ai-action`).withText('Chat window autotest'));
    I.waitForVisible(input, 10);
}

Scenario('Moving and resizing the chat keeps the draft and the editor usable', async ({ I, DTE }) => {
    openChat(I, DTE);
    I.resizeWindow(1440, 1000);
    I.fillField(input, 'Draft autotest');
    I.attachFile(`${container} input[type="file"]`, 'screenshots/generator/ai/program-skolenia-autotest.csv');
    I.waitForText('program-skolenia-autotest.csv', 5, `${container} .ai-chat-composer`);

    const left = () => document.querySelector('#toast-container-ai .toast').getBoundingClientRect().left;
    const original = await I.executeScript(left);
    // CodeceptJS drags to another element; here the title moves by a small offset.
    await I.usePlaywrightTo('drag the chat title', async ({ page }) => {
        const { x, y, width, height } = await page.locator(`${dialog} .toast-title`).boundingBox();
        await page.mouse.move(x + width / 2, y + height / 2);
        await page.mouse.down();
        await page.mouse.move(x + width / 2 - 60, y + height / 2);
        await page.mouse.up();
    });
    const moved = await I.executeScript(left);
    I.assertBelow(moved, original);

    I.clickCss(maximize);
    I.clickCss(minimize);
    I.waitForInvisible(dialog, 5);
    I.fillField(field, 'Editor remains editable autotest');
    I.clickCss('.DTE_Field_Name_htmlData .btn-ai');
    I.waitForVisible(dialog, 5);
    I.assertEqual(await I.grabAttributeFrom(maximize, 'aria-pressed'), 'true');
    I.clickCss(maximize);
    I.assertEqual(await I.executeScript(left), moved);

    I.resizeWindow(600, 700);
    I.clickCss(minimize);
    I.clickCss(restore);
    I.seeInField(input, 'Draft autotest');
    I.see('program-skolenia-autotest.csv', `${container} .ai-chat-composer`);
    I.clickCss(`${container} .toast-close-button`);
    I.seeInField(field, 'Editor remains editable autotest');
    DTE.cancel();
    I.wjSetDefaultWindowSize();
});

Scenario('A pending answer completes while minimized and closing the chat removes the launcher', async ({ I, DTE }) => {
    openChat(I, DTE);
    // Hold the mocked response until the UI proves the chat is minimized; no request reaches an AI provider.
    I.executeScript(() => { window.autotestChatMinimized = false; });
    I.mockRoute('**/admin/rest/ai/assistant/response/', async route => {
        await route.request().frame().page().waitForFunction(() => window.autotestChatMinimized === true);
        await route.fulfill({ status: 200, contentType: 'application/json',
            body: JSON.stringify({ response: 'Completed while minimized autotest', totalTokens: 7 }) });
    });
    I.fillField(input, 'Question autotest');
    I.clickCss(`${container} .btn-ai-chat-send`);
    I.waitForVisible(`${container} .btn-ai-chat-stop`, 5);
    I.clickCss(minimize);
    I.waitForInvisible(dialog, 5);
    I.executeScript(() => { window.autotestChatMinimized = true; });
    // This state is intentionally hidden, so visible-element assertions cannot observe completion.
    I.waitForFunction(() => document.querySelector('#toast-container-ai .ai-chat-assistant .ai-chat-message-text')
        ?.textContent.includes('Completed while minimized autotest'), 10);
    I.clickCss(restore);
    I.see('Completed while minimized autotest', `${container} .ai-chat-history`);
    I.dontSeeElement(`${container} .btn-ai-chat-stop`);
    I.clickCss(`${container} .toast-close-button`);
    I.waitForInvisible(container, 5);
    I.dontSeeElement(restore);
    DTE.cancel();
});
