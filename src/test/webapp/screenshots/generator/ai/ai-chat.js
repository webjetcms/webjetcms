const { openHistory, deleteHistory } = require('../../../pages/ai-chat-history');

Feature('ai.ai-chat');

const modal = '#admin-quick-search';
const chatTitle = 'Školenie – autotest';
const filesButton = `${modal} button[aria-controls="admin-ai-chat-files"]`;
const chatModel = "gpt-6.1-sol";

Before(({ I }) => {
    //use tester3 for AI Chat tests because there will not be history for this user
    I.relogin('tester3');
});

Scenario('AI Chat documentation screenshots', async ({ I, Document }) => {
    I.amOnPage('/admin/v9/');
    I.resizeWindow(1440, 1000);
    I.clickCss('button.js-search-toggler');
    I.waitForVisible(modal, 10);
    I.clickCss(`${modal} [data-scope="aiChat"]`);
    I.waitForVisible('#admin-quick-search-chat-assistant', 20);
    I.selectOption('#admin-quick-search-chat-assistant', chatModel);
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

    await openHistory(I, modal);
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

Scenario('AI Chat editor documentation screenshots', async ({ I, DTE, Document }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=16');
    DTE.waitForEditor();
    I.resizeWindow(1440, 1000);
    I.clickCss('#pills-dt-datatableInit-perex-tab');
    I.fillField('#DTE_Field_htmlData', 'Školenie WebJET CMS pre redaktorov: úprava stránok, obrázky a AI asistenti.');
    I.clickCss('div.DTE_Field_Name_htmlData button.btn-ai');
    I.waitForVisible('#toast-container-ai', 10);
    I.click(locate('button.btn-ai-action').withText(chatModel));
    I.waitForVisible('#toast-container-ai .ai-chat-input', 10);
    I.fillField('#toast-container-ai .ai-chat-input', 'Napíš dvojvetovú pozvánku na školenie WebJET CMS: úprava stránok, obrázky a AI asistenti (autotest). Bez HTML a bez úvodu.');
    I.clickCss('#toast-container-ai .btn-ai-chat-send');
    I.waitForVisible('#toast-container-ai .ai-chat-assistant .ai-chat-message-actions button', 120);
    I.waitForInvisible('#toast-container-ai .btn-ai-chat-stop', 120);
    Document.screenshot('/redactor/ai/chat/chat-editor.png');

    //delete this thread after taking the screenshot
    await deleteHistory(I, "Napíš dvojvetovú", '#toast-container-ai');

    I.clickCss('#toast-container-ai .toast-close-button');
    DTE.cancel();
    I.wjSetDefaultWindowSize();
});

Scenario('Cleanup AI Chat documentation conversation', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    I.clickCss('button.js-search-toggler');
    I.waitForVisible(modal, 10);
    I.clickCss(`${modal} [data-scope="aiChat"]`);
    await deleteHistory(I, chatTitle, modal);
});

Scenario('logout', async ({ I }) => {
    I.logout();
});