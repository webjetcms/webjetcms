/** Opens history regardless of the user's saved sidebar preference. */
async function openHistory(I, host) {
    const panel = `${host} .ai-chat-host__history`;
    const input = `${panel} input[type="search"]`;
    await I.waitForElement(panel, 20);
    if (!await I.grabNumberOfVisibleElements(panel)) {
        I.clickCss(`${host} button[aria-controls$="-history"]`);
    }
    I.waitForVisible(panel, 20);
    if (!await I.grabNumberOfVisibleElements(input)) {
        I.clickCss(`${panel} button[aria-controls$="-history-filter"]`);
    }
    I.waitForVisible(input, 20);
}

async function deleteHistory(I, chatTitle, host) {
    await openHistory(I, host);
    I.fillField(`${host} .ai-chat-host__history input[type="search"]`, chatTitle);
    I.waitForResponse(response => response.url().includes('/admin/rest/ai/chat/threads?')
        && response.url().includes('query=') && response.status() === 200, 20);
    I.waitForFunction(([title, host]) => {
        const titles = Array.from(document.querySelectorAll(`${host} .ai-chat-host__history-item .ai-chat-host__history-title`));
        return titles.every(item => item.textContent.startsWith(title));
    }, [chatTitle, host], 20);
    const items = await I.grabNumberOfVisibleElements(`${host} .ai-chat-host__history-item`);
    for (let index = 0; index < items; index++) {
        const remove = locate(`${host} .ai-chat-host__history-item`).withText(chatTitle).find('button.ai-chat-host__history-action').first();
        const label = await I.grabAttributeFrom(remove, 'aria-label');
        I.amAcceptingPopups();
        I.click(remove);
        if (label.startsWith('Vymazať:')) I.acceptPopup();
        I.waitForFunction(([expected, host]) => document.querySelectorAll(`${host} .ai-chat-host__history-item`).length === expected, [items - index - 1, host], 30);
    }
}

module.exports = { openHistory, deleteHistory };
