const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const sources = ['chat-history-api.js', 'ai-chat.js'].map(file => fs.readFileSync(path.resolve(__dirname,
    '../../../main/webapp/admin/v9/src/js/ai', file), 'utf8')
    .replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '')).join('\n');

/** Restores production chat state without mounting unrelated composer controls. */
function loadChat() {
    const AiChat = vm.runInNewContext(sources + '\nAiChat', {
        WJ: { translate: key => `translated:${key}` }
    });
    const chat = Object.create(AiChat.prototype);
    chat.state = chat._initialState();
    chat.refresh = () => {};
    return chat;
}

test('reopened chat keeps the token-limit explanation and permits a new retry after reset', () => {
    const chat = loadChat();
    chat.restoreHistory({
        thread: { id: 'thread', revision: 1, busy: false, canContinue: true },
        messages: [
            { id: 'question', role: 'user', text: 'Question' },
            { id: 'answer', turnId: 'question', role: 'assistant', status: 'error',
                errorCategory: 'TOKEN_LIMIT_REACHED', retryable: true }
        ]
    });

    const message = chat.state.messages[1];
    assert.equal(message.error, 'translated:components.ai_assistants.token_limit.reached.js');
    assert.equal(message.userPrompt, 'Question');
    assert.equal(message.confirmed, true);
    assert.equal(chat._canRetry(message), true);
    assert.equal(chat.state.serverBusy, false);
});

test('history retains attachment and generic errors while completed answers remain readable', () => {
    const chat = loadChat();
    chat.restoreHistory({
        thread: { id: 'thread', revision: 3, busy: false, canContinue: true },
        messages: [
            { role: 'assistant', status: 'error', errorCategory: 'ATTACHMENT_INVALID' },
            { role: 'assistant', status: 'error', errorCategory: 'UNKNOWN' },
            { role: 'assistant', status: 'complete', text: 'Saved answer' }
        ]
    });

    const [attachment, failed, completed] = chat.state.messages;
    assert.equal(attachment.error, 'translated:components.ai_assistants.chat.history.attachmentUnreadable.js');
    assert.equal(attachment.retryable, false);
    assert.equal(failed.error, 'translated:components.ai_assistants.chat.history.generationFailed.js');
    assert.equal(completed.error, null);
    assert.equal(completed.text, 'Saved answer');
});
