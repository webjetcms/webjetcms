const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const sourceDirectory = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables/ai');
const sources = ['../../../src/js/ai/chat-history-api.js', '../../../src/js/ai/ai-stream.js', '../../../src/js/ai/ai-rest-transport.js',
    '../../../src/js/ai/ai-response-parser.js',
    'ai-execution-result.js', 'ai-rest-executor.js', '../../../src/js/ai/ai-chat-transport.js'].map(filename =>
    fs.readFileSync(path.join(sourceDirectory, filename), 'utf8').replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '')
).join('\n');

function loadTransport(fetch, notifications = [], AiBrowserExecutor) {
    const context = vm.createContext({
        TextDecoder, AbortController, URLSearchParams, FormData, fetch, AiBrowserExecutor, console: { log() {}, warn() {} },
        window: { csrfToken: 'test-csrf', csrfError: 'Token expired', alert: text => notifications.push(text) },
        WJ: {
            keepSessionShowLogoffMessage: () => notifications.push('Session expired'),
            keepSessionShowTokenMessage: text => notifications.push(text)
        }
    });
    return vm.runInContext(sources + '\n({ AiRestExecutor, AiChatTransport, requestAiText });', context);
}

function delta(text) {
    return JSON.stringify({ type: 'delta', text }) + '\n';
}

function result(payload) {
    return JSON.stringify({ type: 'result', result: payload }) + '\n';
}

for (const [persistentChat, streaming] of [[true, true], [true, false], [false, false], [false, true]]) {
    test(`chat uploads preserve context, CSRF and multipart data (saved=${persistentChat}, streaming=${streaming})`, async () => {
        let request;
        const file = new File(['Private attachment'], 'notes.txt', { type: 'text/plain' });
        const payload = { response: 'Answer', totalTokens: 7, conversationId: 'next-turn' };
        const accept = streaming ? 'application/x-ndjson' : 'application/json';
        const input = persistentChat ? { requestId: 'request', inputValue: '<p>Existing page</p>', inputIsSelectedTextInEditor: true,
            context: { entityType: '/admin/rest/web-pages', entityId: '42', fieldName: 'data' } } : {
            inputValue: '<p>Existing page</p>', inputIsSelectedTextInEditor: true, inputValueType: 'text', outputValueType: 'text', conversationId: 'previous-turn'
        };
        const { AiChatTransport } = loadTransport(async (url, options) => {
            request = { url, ...options };
            return new Response(streaming ? delta('Answer') + result(payload) : JSON.stringify(payload),
                { headers: { 'Content-Type': accept } });
        });
        const response = await new AiChatTransport().execute({ assistantId: 7, useStreaming: streaming }, {
            ...input, persistentChat, userPrompt: 'Read this file', files: [file]
        });
        assert.equal(response.success, true);
        assert.equal(response.response, 'Answer');
        assert.equal(response.conversationId, 'next-turn');
        assert.equal(request.url, persistentChat ? '/admin/rest/ai/chat/messages'
            : `/admin/rest/ai/assistant/${streaming ? 'response-stream' : 'response'}/`);
        assert.equal(request.headers['X-CSRF-Token'], 'test-csrf');
        assert.equal(request.headers.Accept, accept);
        assert.equal(request.headers['Content-Type'], undefined, 'The browser must supply the multipart boundary.');
        const { timestamp, persistentChat: ignored, ...message } = JSON.parse(request.body.get('message'));
        assert.deepEqual(message, { ...input, assistantId: 7, userPrompt: 'Read this file' });
        if (!persistentChat) assert.ok(Number.isInteger(timestamp));
        assert.equal(await request.body.get('files').text(), 'Private attachment');
        assert.equal(request.body.get('files').name, 'notes.txt');
    });
}

/** Exercises the real executor, substituting only the HTTP response and editor. */
async function execute(chunks, streaming = true) {
    const updates = [];
    let request;
    const response = new Response(new ReadableStream({
        pull(controller) {
            if (chunks.length > 0) controller.enqueue(chunks.shift());
            else controller.close();
        }
    }), { headers: { 'Content-Type': streaming ? 'application/x-ndjson' : 'application/json' } });
    const { AiRestExecutor } = loadTransport(async (url, options) => { request = { url, ...options }; return response; });
    const executor = new AiRestExecutor(null, null);
    const execution = await executor.execute({ assistantId: 7, useStreaming: streaming, to: 'data' }, {},
        (text, final) => updates.push({ text, final }));
    assert.equal(executor.curentAssistantId, null);
    assert.equal(executor.curentTimestamp, null);
    return { execution, updates, request };
}

test('split streaming text preserves Unicode, editor formatting and token usage', async () => {
    const wire = delta('```html\n<body><p>Ahoj 🌍</p></body>\n```\nVysvetlenie úprav.')
        + result({ totalTokens: 42, error: null });
    const { execution, updates, request } = await execute([...Buffer.from(wire)].map(byte => Uint8Array.of(byte)));
    assert.equal(execution.success, true);
    assert.equal(execution.totalTokens, 42);
    assert.equal(execution.explanatoryText, 'Vysvetlenie úprav.');
    assert.deepEqual(updates.at(-1), { text: '<p>Ahoj 🌍</p>', final: true });
    assert.equal(request.headers.Accept, 'application/x-ndjson');
});

test('an incomplete stream keeps partial text but never finalizes the edit', async () => {
    const { execution, updates } = await execute([Buffer.from(delta('Partial text'))]);
    assert.equal(execution.success, false);
    assert.match(execution.errorText, /Incomplete AI response stream/);
    assert.deepEqual(updates, [{ text: 'Partial text', final: false }]);
});

test('rejected local generation preserves the editor value and never reports success', async () => {
    const payload = { response: null, error: 'The local model did not return usable text.',
        errorCategory: 'UNKNOWN', totalTokens: 0 };
    const { execution, updates } = await execute([Buffer.from(JSON.stringify(payload))], false);
    assert.equal(execution.success, false);
    assert.equal(execution.errorText, payload.error);
    assert.deepEqual(updates, [], 'An error response must not replace the editor content.');
});

test('shared requests keep raw chat text while ordinary JSON generation formats the editor', async () => {
    const text = '```html\n<body><p>Hello</p></body>\n```\nExplanation of changes.';
    const payload = { response: text, totalTokens: 42, conversationId: 'next-turn', error: null };
    const { execution, updates, request } = await execute([Buffer.from(JSON.stringify(payload))], false);
    assert.equal(execution.success, true);
    assert.equal(execution.explanatoryText, 'Explanation of changes.');
    assert.deepEqual(updates, [{ text: '<p>Hello</p>', final: true }]);
    assert.equal(request.url, '/admin/rest/ai/assistant/response/');
    assert.equal(request.headers.Accept, 'application/json');
    assert.equal(request.headers['X-CSRF-Token'], 'test-csrf');

    let sent;
    const { AiChatTransport } = loadTransport(async (url, options) => {
        sent = JSON.parse(options.body);
        return new Response(JSON.stringify(payload), { headers: { 'Content-Type': 'application/json' } });
    });
    const chat = await new AiChatTransport().execute({ assistantId: 7 }, { conversationId: 'previous-turn' });
    assert.equal(chat.success, true);
    assert.equal(chat.response, text);
    assert.equal(chat.conversationId, 'next-turn');
    assert.equal(sent.conversationId, 'previous-turn');
});

test('streaming failures preserve structured errors and partial chat text', async () => {
    const payload = { error: 'Start a new conversation', totalTokens: 0,
        errorCategory: 'INVALID_CONTINUATION', providerStatusCode: 400, retryable: false };
    const { AiChatTransport } = loadTransport(async () => new Response(delta('Partial text') + result(payload),
        { headers: { 'Content-Type': 'application/x-ndjson' } }));
    const chat = await new AiChatTransport().execute({ assistantId: 7, useStreaming: true }, {});
    assert.equal(chat.success, false);
    assert.equal(chat.response, 'Partial text');
    assert.equal(chat.errorText, payload.error);
    assert.equal(chat.errorCategory, payload.errorCategory);
    assert.equal(chat.providerStatusCode, 400);
    assert.equal(chat.retryable, false);
});

test('HTTP authentication failures preserve session and CSRF notifications', async () => {
    for (const [status, notice] of [[401, 'Session expired'], [403, 'Token expired']]) {
        const notifications = [];
        const { requestAiText } = loadTransport(async () => new Response('Access denied', { status }), notifications);
        await assert.rejects(requestAiText({}), new RegExp(`HTTP ${status}`));
        assert.deepEqual(notifications, [notice]);
    }
});

test('chat cancellation aborts the shared fetch and stops the same server task', async () => {
    const requests = [];
    const { AiChatTransport } = loadTransport((url, options) => {
        requests.push({ url, ...options });
        if (options.method === 'GET') return Promise.resolve(new Response());
        return new Promise((resolve, reject) => options.signal.addEventListener('abort',
            () => reject(new Error('Aborted')), { once: true }));
    });
    const transport = new AiChatTransport();
    const pending = transport.execute({ assistantId: 7 }, {});
    const data = JSON.parse(requests[0].body);
    transport.cancel();
    const chat = await pending;
    assert.equal(chat.stopped, true);
    assert.equal(chat.success, false);
    assert.equal(requests[0].signal.aborted, true);
    assert.equal(requests[1].url, `/admin/rest/ai/assistant/stop-assistant/?assistantId=7&timestamp=${data.timestamp}`);
    assert.equal(requests[1].headers['X-CSRF-Token'], 'test-csrf');
    assert.equal(transport.request, null);
});

for (const response of [
    { status: 504, statusText: 'Gateway Timeout' },
    { status: 403, statusText: '', responseJSON: { error: 'Image generation is not allowed' } },
    { status: 0, statusText: '' }
]) {
    test(`Image HTTP failure (${response.status}) returns an error and releases the active request`, async t => {
        const { JSDOM } = require('jsdom');
        const window = new JSDOM('').window;
        t.after(() => window.close());
        const $ = require('jquery')(window);
        $.ajax = () => {
            const request = $.Deferred();
            queueMicrotask(() => request.reject(response, 'error', response.statusText));
            return request.promise();
        };
        const context = vm.createContext({ $, WJ: { translate: key => key } });
        const Executor = vm.runInContext(sources + '\nAiRestExecutor;', context);
        const executor = new Executor(null, { aiUserInterface: {
            renderImageSelection() { assert.fail('A failed request must not show image selection'); }
        } });
        const execution = await executor.executeImageAction({ assistantId: 7 }, {}, null);
        assert.equal(execution.success, false);
        assert.equal(execution.errorText, response.responseJSON?.error || response.statusText || 'HTTP ' + response.status);
        assert.equal(executor.curentAssistantId, null);
        assert.equal(executor.curentTimestamp, null);
    });
}

test('A successful image response keeps the image picker and clears the active request', async () => {
    const selections = [];
    const context = vm.createContext({
        $: { ajax: async () => ({ tempFiles: ['/autotest.png'], generatedFileName: 'autotest.png', totalTokens: 12 }) },
        WJ: { translate: key => key }
    });
    const Executor = vm.runInContext(sources + '\nAiRestExecutor;', context);
    const executor = new Executor(null, { aiUserInterface: { renderImageSelection: (...args) => selections.push(args) } });
    const execution = await executor.executeImageAction({ assistantId: 7, to: 'image' }, {}, null);
    assert.equal(execution.errorText, null);
    assert.equal(execution.totalTokens, 12);
    assert.equal(execution.success, false, 'The image picker replaces the generic success dialog');
    assert.equal(selections.length, 1);
    assert.equal(executor.curentAssistantId, null);
    assert.equal(executor.curentTimestamp, null);
});
