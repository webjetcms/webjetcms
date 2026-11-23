Feature('ai.chat-quick-search');

const modal = '#admin-quick-search';
const chat = `${modal} #admin-quick-search-chat`;
const question = 'Quick search history autotest';
const answer = 'Saved answer autotest';

Before(({ login }) => login('admin'));

function openChat(I) {
    I.waitForElement('button.js-search-toggler[aria-expanded]', 15);
    I.clickCss('button.js-search-toggler');
    I.waitForVisible(`${modal} [data-scope="aiChat"]`, 10);
    I.clickCss(`${modal} [data-scope="aiChat"]`);
    I.waitForVisible(`${chat} .ai-chat-input`, 10);
}

Scenario('A standalone chat can be reopened from history after a page reload', async ({ I }) => {
    const assistant = { assistantId: 7, description: 'History autotest', provider: 'gemini', action: 'live_chat', persistentChat: true };
    let thread;
    // Keep the simulated assistant and sidebar preferences local to this scenario.
    I.mockRoute('**/admin/rest/admin-settings/', route => route.fulfill({ json: {} }));
    I.mockRoute('**/admin/rest/ai/assistant/chat-assistants/', route => route.fulfill({ json: [assistant] }));
    I.mockRoute('**/admin/rest/ai/assistant/runtime/?*', route => route.fulfill({ json: assistant }));
    I.mockRoute('**/admin/rest/ai/chat/threads?*', route => route.fulfill({ json: { items: thread ? [thread] : [], hasMore: false } }));
    I.mockRoute('**/admin/rest/ai/chat/messages', route => {
        const { requestId, userPrompt, assistantId } = route.request().postDataJSON();
        thread = { id: requestId, title: userPrompt, assistantId, revision: 1, canContinue: true };
        return route.fulfill({ json: { threadId: requestId, revision: 1, userMessageId: 'question', messageId: 'answer', response: answer, totalTokens: 7 } });
    });
    I.mockRoute('**/admin/rest/ai/chat/threads/*', route => route.fulfill({ json: {
        thread, messages: [
            { id: 'question', role: 'user', text: thread.title, status: 'complete' },
            { id: 'answer', turnId: 'question', role: 'assistant', text: answer, status: 'complete', totalTokens: 7 }
        ]
    } }));

    I.amOnPage('/admin/v9/');
    openChat(I);
    I.fillField(`${chat} .ai-chat-input`, question);
    I.clickCss(`${chat} .btn-ai-chat-send`);
    I.waitForText(answer, 10, `${chat} .ai-chat-assistant`);

    I.refreshPage();
    openChat(I);
    I.dontSeeElement(`${chat} .ai-chat-assistant`);
    if (!await I.grabNumberOfVisibleElements(`${chat} .ai-chat-host__history`)) {
        I.clickCss(`${chat} button[aria-controls$="-history"]`);
    }
    I.click(locate(`${chat} .ai-chat-host__history-select`).withText(question));
    I.waitForText(question, 10, `${chat} .ai-chat-user`);
    I.waitForText(answer, 10, `${chat} .ai-chat-assistant`);
    I.see('Použité tokeny: 7', `${chat} .ai-chat-assistant .ai-chat-usage`);
});


Scenario('An owner can finish a stuck deletion from saved chat history', async ({ I }) => {
    const assistant = { assistantId: 7, description: 'Deletion autotest', provider: 'openai', action: 'live_chat', persistentChat: true };
    const thread = { id: 'interrupted-autotest', title: 'Interrupted request autotest', status: 'DELETE_PENDING', deletionError: 'cleanupUnknown' };
    let deleted = false;
    I.mockRoute('**/admin/rest/admin-settings/', route => route.fulfill({ json: {} }));
    I.mockRoute('**/admin/rest/ai/assistant/chat-assistants/', route => route.fulfill({ json: [assistant] }));
    I.mockRoute('**/admin/rest/ai/chat/threads?*', route => route.fulfill({ json: { items: deleted ? [] : [thread], hasMore: false } }));
    I.mockRoute('**/admin/rest/ai/chat/threads/' + thread.id + '/local', route => {
        deleted = route.request().method() === 'DELETE';
        return route.fulfill({ json: { deleted } });
    });

    I.amOnPage('/admin/v9/');
    openChat(I);
    if (!await I.grabNumberOfVisibleElements(`${chat} .ai-chat-host__history`)) I.clickCss(`${chat} button[aria-controls$="-history"]`);
    I.amAcceptingPopups();
    I.click(locate(`${chat} button`).withAttr({ 'aria-label': 'Vymazať z WebJETu: ' + thread.title }));
    I.seeInPopup('U poskytovateľa môžu zostať uložené odpovede.');
    I.acceptPopup();
    await I.waitForInvisible(locate(`${chat} .ai-chat-host__history-select`).withText(thread.title), 5);
    I.assertTrue(deleted);
});

Scenario('Folder confirmation continues the locked chat and the gauge shows recorded usage', async ({ I }) => {
    const assistant = { assistantId: 7, description: 'Folder autotest', provider: 'gemini', action: 'live_chat', toolsEnabled: true };
    const second = { ...assistant, assistantId: 8, description: 'Second autotest' };
    const prompts = [];
    I.mockRoute('**/admin/rest/admin-settings/', route => route.fulfill({ json: {} }));
    I.mockRoute('**/admin/rest/ai/assistant/chat-assistants/', route => route.fulfill({ json: [assistant, second] }));
    I.mockRoute('**/admin/rest/ai/assistant/runtime/?*', route => route.fulfill({ json: assistant }));
    I.mockRoute('**/admin/rest/ai/chat/threads?*', route => route.fulfill({ json: { items: [], hasMore: false } }));
    I.mockRoute('**/admin/rest/ai/assistant/response/', route => {
        prompts.push(route.request().postDataJSON().userPrompt);
        const first = prompts.length === 1;
        return route.fulfill({ json: { response: first ? 'Choose a folder' : 'Folder received', folderRequested: first,
            totalTokens: first ? 17 : 25, tokenBreakdown: { inputTokens: first ? 10 : 20, outputTokens: first ? 7 : 5 }, conversationId: 'folder-autotest' } });
    });
    I.mockRoute('**/admin/rest/groups/tree/tree?*', route => route.fulfill({ json: { result: true, items: [
        { id: '42', text: '<span class="jstree-text-icon ti ti-lock-filled"></span> Folder autotest', icon: 'ti ti-folder-filled', children: false,
            groupDetails: { groupId: 42, fullPath: '/Folder autotest' } }
    ] } }));
    I.amOnPage('/admin/v9/');
    openChat(I);
    I.fillField(`${chat} .ai-chat-input`, 'Create a page autotest');
    I.clickCss(`${chat} .btn-ai-chat-send`);
    I.waitForVisible(`${chat} .ai-chat-host__locked-assistant`, 10);
    I.dontSeeElement(`${chat} .ai-chat-host__assistant .dropdown-toggle`);
    const tree = `${chat} .ai-chat-folder-picker`;
    I.waitForVisible(`${tree} .jstree-anchor`, 10);
    I.seeElement(`${tree} .jstree-anchor .ti-lock-filled`);
    I.fillField(`${chat} .ai-chat-input`, 'Keep this unsent draft');
    I.click(locate(`${tree} .jstree-anchor`).withText('Folder autotest'));
    I.waitForText('/Folder autotest', 5, `${tree} .ai-chat-folder-path`);
    I.clickCss(`${tree} .btn-primary`);
    await I.waitForText('Folder received', 10, chat);
    I.see('Použité tokeny: 17', locate(`${chat} .ai-chat-assistant`).withText('Choose a folder'));
    I.see('Použité tokeny: 25', locate(`${chat} .ai-chat-assistant`).withText('Folder received'));
    I.assertEqual(prompts.length, 2);
    I.assertContain(prompts[1], '42');
    I.seeInField(`${chat} .ai-chat-input`, 'Keep this unsent draft');
    I.dontSeeElement(tree);
    I.moveCursorTo(`${chat} button.ai-chat-token-usage`);
    I.waitForText('30', 5, '.tooltip.show dd');
    I.see('12', '.tooltip.show dd');
    I.see('42', '.tooltip.show dd');
    if (await I.grabNumberOfVisibleElements(`${chat} .ai-chat-host__new`)) I.clickCss(`${chat} .ai-chat-host__new`);
    else I.clickCss(`${chat} [data-ai-action="new"]`);
    I.waitForVisible(`${chat} .ai-chat-host__assistant .dropdown-toggle`, 5);
});
