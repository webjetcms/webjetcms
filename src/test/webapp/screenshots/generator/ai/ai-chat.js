Feature('ai.ai-chat');

const modal = '#admin-quick-search';
const chatTitle = 'Školenie – autotest';
const historyButton = `${modal} button[aria-controls="admin-ai-chat-history"]`;
const filesButton = `${modal} button[aria-controls="admin-ai-chat-files"]`;

Before(({ login }) => {
    login('admin');
});

Scenario('AI Chat documentation screenshots', ({ I, Document }) => {
    I.amOnPage('/admin/v9/');
    I.resizeWindow(1440, 1000);
    I.clickCss('button.js-search-toggler');
    I.waitForVisible(modal, 10);
    I.clickCss(`${modal} [data-scope="aiChat"]`);
    I.waitForVisible('#admin-quick-search-chat-assistant', 20);
    I.selectOption('#admin-quick-search-chat-assistant', 'Gemini Chat');
    I.waitForEnabled(`${modal} .btn-ai-chat-attach`, 10);

    I.fillField(`${modal} .ai-chat-input`, `${chatTitle}: z prílohy vytvor tabuľku Čas, Téma, Trvanie.`);
    I.attachFile(`${modal} input[type="file"]`, 'screenshots/generator/ai/program-skolenia-autotest.csv');
    I.waitForVisible(`${modal} .ai-chat-composer .ai-chat-attachment`, 10);
    I.moveCursorTo(`${modal} .md-quick-search__input-row`);
    Document.screenshotElement(`${modal} .ai-chat-composer`, '/redactor/ai/chat/chat-attachment.png');

    I.clickCss(`${modal} .btn-ai-chat-send`);
    I.waitForVisible(`${modal} .ai-chat-assistant table`, 120);
    I.waitForInvisible(`${modal} .btn-ai-chat-stop`, 120);
    I.waitForVisible(`${modal} .ai-chat-assistant .ai-chat-message-actions button`, 10);

    I.clickCss(historyButton);
    I.fillField('#admin-ai-chat-history-search', chatTitle);
    I.waitForText(chatTitle, 20, '.ai-chat-host__history-item');
    I.clickCss(`${modal} .md-quick-search__input-row`);
    Document.screenshotElement(`${modal} .modal-content`, '/redactor/ai/chat/chat-history.png');

    I.clickCss('#admin-ai-chat-history .ai-chat-host__history-heading button');
    I.clickCss(filesButton);
    I.waitForVisible('.ai-chat-host__file-download', 10);
    I.clickCss(`${modal} .md-quick-search__input-row`);
    Document.screenshotElement(`${modal} .modal-content`, '/redactor/ai/chat/chat-files.png');
    I.pressKey('Escape');
    I.wjSetDefaultWindowSize();
});

Scenario('AI Chat editor documentation screenshots', ({ I, DTE, Document }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=16');
    DTE.waitForEditor();
    I.resizeWindow(1440, 1000);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    I.fillField('#DTE_Field_htmlData', 'Školenie WebJET CMS pre redaktorov: úprava stránok, obrázky a AI asistenti.');
    I.clickCss('div.DTE_Field_Name_htmlData button.btn-ai');
    I.waitForVisible('#toast-container-ai', 10);
    I.click(locate('button.btn-ai-action').withText('Gemini Chat'));
    I.waitForVisible('#toast-container-ai .ai-chat-input', 10);
    I.fillField('#toast-container-ai .ai-chat-input', 'Napíš dvojvetovú pozvánku na školenie WebJET CMS: úprava stránok, obrázky a AI asistenti (autotest). Bez HTML a bez úvodu.');
    I.clickCss('#toast-container-ai .btn-ai-chat-send');
    I.waitForVisible('#toast-container-ai .ai-chat-assistant .ai-chat-message-actions button', 120);
    I.waitForInvisible('#toast-container-ai .btn-ai-chat-stop', 120);
    I.moveCursorTo('#toast-container-ai .ai-chat-toolbar');
    Document.screenshotElement('#toast-container-ai .toast', '/redactor/ai/chat/chat-editor.png');
    I.clickCss('#toast-container-ai .toast-close-button');
    DTE.cancel();
    I.wjSetDefaultWindowSize();
});

Scenario('Cleanup AI Chat documentation conversation', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    I.clickCss('button.js-search-toggler');
    I.waitForVisible(modal, 10);
    I.clickCss(`${modal} [data-scope="aiChat"]`);
    I.waitForVisible(historyButton, 20);
    I.clickCss(historyButton);
    I.fillField('#admin-ai-chat-history-search', chatTitle);
    I.waitForResponse(response => response.url().includes('/admin/rest/ai/chat/threads?')
        && response.url().includes('query=') && response.status() === 200, 20);
    I.waitForFunction(([title]) => {
        const titles = Array.from(document.querySelectorAll('.ai-chat-host__history-item .ai-chat-host__history-title'));
        return titles.every(item => item.textContent.startsWith(title));
    }, [chatTitle], 20);
    const items = await I.grabNumberOfVisibleElements('.ai-chat-host__history-item');
    for (let index = 0; index < items; index++) {
        const remove = locate('.ai-chat-host__history-item').withText(chatTitle).find('button[aria-label]').first();
        const label = await I.grabAttributeFrom(remove, 'aria-label');
        I.amAcceptingPopups();
        I.click(remove);
        if (label.startsWith('Vymazať:')) I.acceptPopup();
        I.waitForFunction(([expected]) => document.querySelectorAll('.ai-chat-host__history-item').length === expected, [items - index - 1], 30);
    }
});
