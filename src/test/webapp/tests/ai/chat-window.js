Feature('ai.chat-window');

const container = '#toast-container-ai';
const dialog = `${container} .toast.toast-info`;
const input = `${container} .ai-chat-input`;
const minimize = `${container} .ai-chat-minimize`;
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

/** Reads coordinates so restoration can be checked independently of the positioning implementation. */
function windowBounds() {
    const bounds = document.querySelector('#toast-container-ai .toast').getBoundingClientRect();
    return { left: bounds.left, top: bounds.top, right: bounds.right, bottom: bounds.bottom };
}

Scenario('Move, minimize and restore preserve the conversation and release the editor', async ({ I, DTE }) => {
    openChat(I, DTE);
    I.resizeWindow(1440, 1000);
    I.fillField(input, 'Draft autotest');
    I.assertEqual(await I.grabAttributeFrom(minimize, 'aria-label'), 'Minimalizovať chat');
    I.attachFile(`${container} input[type="file"]`, 'screenshots/generator/ai/program-skolenia-autotest.csv');
    I.waitForVisible(`${container} .ai-chat-composer .ai-chat-attachment`, 10);
    I.clickCss(`${container} .ai-chat-toolbar [data-ai-action="files"]`);
    I.waitForVisible(`${container} .md-quick-search__chat-files`, 10);

    const original = await I.executeScript(windowBounds);
    // CodeceptJS dragAndDrop targets elements; a pointer drag by a measured offset needs the mouse API.
    await I.usePlaywrightTo('move the chat by its title bar', async ({ page }) => {
        const header = await page.locator(`${dialog} .toast-title`).boundingBox();
        await page.mouse.move(header.x + header.width / 2, header.y + header.height / 2);
        await page.mouse.down();
        await page.mouse.move(header.x + header.width / 2 + 100, header.y + header.height / 2 - 100, { steps: 5 });
        await page.mouse.up();
    });
    const moved = await I.executeScript(windowBounds);
    I.assertEqual(Math.round(moved.left - original.left), 100);
    I.assertEqual(Math.round(moved.top - original.top), -100);

    I.clickCss(minimize);
    I.waitForInvisible(dialog, 5);
    I.seeElement(restore);
    I.fillField(field, 'Editor remains editable autotest');
    I.seeInField(field, 'Editor remains editable autotest');
    // The editor traps Tab inside its modal, so its original AI button must also restore the chat.
    I.clickCss('.DTE_Field_Name_htmlData .btn-ai');
    I.waitForVisible(input, 5);
    I.seeInField(input, 'Draft autotest');
    I.see('program-skolenia-autotest.csv', `${container} .ai-chat-composer`);
    I.seeElement(`${container} .md-quick-search__chat-files`);
    I.assertDeepEqual(await I.executeScript(windowBounds), moved);
    I.assertEqual(await I.grabAttributeFrom(dialog, 'aria-modal'), 'false');

    // Focus the title through the DOM because it is a draggable region, not an input control.
    I.executeScript(() => document.querySelector('#toast-container-ai .toast-title').focus());
    I.pressKey('ArrowLeft');
    const keyboardMoved = await I.executeScript(windowBounds);
    I.assertEqual(Math.round(keyboardMoved.left - moved.left), -20);

    for (const [width, height] of [[1100, 800], [600, 700]]) {
        I.clickCss(minimize);
        I.resizeWindow(width, height);
        I.clickCss(restore);
        const bounds = await I.executeScript(windowBounds);
        I.assertTrue(bounds.left >= 0 && bounds.top >= 0 && bounds.right <= width && bounds.bottom <= height,
            'The restored chat must remain inside the viewport.');
        I.seeInField(input, 'Draft autotest');
    }

    I.clickCss(minimize);
    DTE.cancel();
    I.waitForInvisible(container, 5);
    I.dontSeeElement(restore);
    I.wjSetDefaultWindowSize();
});

Scenario('A pending answer completes while minimized and closing the chat removes the launcher', async ({ I, DTE }) => {
    openChat(I, DTE);
    // Hold the mocked response until the UI proves the chat is minimized; no request reaches an AI provider.
    I.executeScript(() => { window.autotestChatMinimized = false; });
    I.mockRoute('**/admin/rest/ai/assistant/response/', async route => {
        await route.request().frame().page().waitForFunction(() => window.autotestChatMinimized === true);
        await route.fulfill({ status: 200, contentType: 'application/json',
            body: JSON.stringify({ response: 'Completed while minimized autotest\n\n' + 'Paragraph autotest.\n\n'.repeat(80), totalTokens: 7 }) });
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
    I.executeScript(() => { document.querySelector('#toast-container-ai .ai-chat-history').scrollTop = 100; });
    // Scroll state lives in the chat model and must not be overwritten by the hidden element's zero dimensions.
    I.waitForFunction(() => window.webpagesDatatable.EDITOR.editorAi.aiUserInterface.currentChat.chat.state.scrollTop === 100, 5);
    I.clickCss(minimize);
    I.clickCss(restore);
    I.assertEqual(await I.executeScript(() => document.querySelector('#toast-container-ai .ai-chat-history').scrollTop), 100);
    I.clickCss(`${container} .toast-close-button`);
    I.waitForInvisible(container, 5);
    I.dontSeeElement(restore);
    DTE.cancel();
});
