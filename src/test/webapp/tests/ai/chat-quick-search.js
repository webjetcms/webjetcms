Feature('ai.chat-quick-search');

const modal = '#admin-quick-search';
const chat = `${modal} #admin-quick-search-chat`;
const question = 'Quick search history autotest';
const answer = 'Saved answer autotest';

Before(({ login }) => login('admin'));

function openChat(I) {
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
    I.mockRoute('**/admin/rest/ai/chat/threads?*', route => route.fulfill({ json: { items: thread ? [thread] : [], hasMore: false } }));
    I.mockRoute('**/admin/rest/ai/chat/messages', route => {
        const { requestId, userPrompt, assistantId } = route.request().postDataJSON();
        thread = { id: requestId, title: userPrompt, assistantId, revision: 1, canContinue: true };
        return route.fulfill({ json: { threadId: requestId, revision: 1, userMessageId: 'question', messageId: 'answer', response: answer, totalTokens: 7 } });
    });
    I.mockRoute('**/admin/rest/ai/chat/threads/*', route => route.fulfill({ json: {
        thread, messages: [
            { id: 'question', role: 'user', text: thread.title, status: 'complete' },
            { id: 'answer', turnId: 'question', role: 'assistant', text: answer, status: 'complete' }
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
});
