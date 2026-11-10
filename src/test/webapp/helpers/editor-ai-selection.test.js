const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.resolve(__dirname,
    '../../../main/webapp/admin/v9/npm_packages/webjetdatatables/editor-ai.js'), 'utf8')
    .replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '');

for (const persistent of [false, true]) {
    test(`sending and retrying an HTML selection preserves its mode and original prompt (saved=${persistent})`, async () => {
        const sent = [];
        const chatSource = fs.readFileSync(path.resolve(__dirname,
            '../../../main/webapp/admin/v9/src/js/ai/ai-chat.js'), 'utf8')
            .replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '');
        const AiChat = vm.runInNewContext(chatSource + '\nAiChat', {
            chatRequestId: () => 'request-id',
            AiChatTransport: class {
                async execute(assistant, input) {
                    sent.push(input);
                    return { success: false, retryable: true, errorText: 'Temporary failure' };
                }
                dispose() {}
            }
        });
        const chat = Object.create(AiChat.prototype);
        chat.state = chat._initialState();
        chat.assistant = { assistantId: 1 };
        let inputIsSelectedTextInEditor = true;
        chat.options = { persistent,
            context: { entityType: '/admin/rest/web-pages', entityId: '42', fieldName: 'data' },
            getInputData: () => ({ inputIsSelectedTextInEditor, inputValue: 'Selected text' }) };
        chat._changed = () => {};
        const prompt = 'Daj na hlavné výrazy tučné písmo.';
        await chat.send(prompt);
        assert.equal(sent[0].inputIsSelectedTextInEditor, true);
        assert.equal(sent[0].userPrompt, prompt);
        inputIsSelectedTextInEditor = false;
        await chat.send(prompt, chat.state.messages.at(-1));
        assert.equal(sent[1].inputIsSelectedTextInEditor, true, 'Retry must use the original selection mode');
        await chat.send('Create a page');
        assert.equal(sent[2].inputIsSelectedTextInEditor, false, 'A new turn must use the current selection mode');
    });
}

test('a valid editor selection is identified as an HTML fragment, independently of PageBuilder wrappers', () => {
    const EditorAi = vm.runInNewContext(source + '\nEditorAi', {
        AiChat: class { constructor(options) { this.options = options; } }
    });
    const integration = Object.create(EditorAi.prototype);
    integration.chatConversations = new Map();
    integration.EDITOR = { get: () => '<article class="custom-layout">Whole page</article>' };
    integration._getChatContext = () => null;
    integration._getColumnType = () => 'text';
    const selection = { inputValue: 'Intenzívny šesťhodinový kurz v&nbsp;marketingu.' };
    integration._captureChatSelection = () => selection;
    let valid = true;
    integration._isChatSelectionValid = value => value === selection && valid;
    const chat = integration.getChatConversation(null, { name: 'data', type: 'wysiwyg' },
        { assistantId: 1, action: 'live_chat', from: 'data', to: 'data' }).chat;

    assert.equal(chat.options.getInputData().inputValue, selection.inputValue);
    assert.equal(chat.options.getInputData().inputIsSelectedTextInEditor, true);
    valid = false;
    assert.equal(chat.options.getInputData().inputIsSelectedTextInEditor, false);
    assert.equal(chat.options.getInputData().inputValue, integration.EDITOR.get());
});
