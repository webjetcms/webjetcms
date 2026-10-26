const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const sourceDirectory = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables/ai');
const sources = ['ai-execution-result.js', 'ai-rest-executor.js'].map(filename =>
    fs.readFileSync(path.join(sourceDirectory, filename), 'utf8').replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '')
).join('\n');

function delta(text) {
    return JSON.stringify({ type: 'delta', text }) + '\n';
}

function result(totalTokens = 42, error = null) {
    return JSON.stringify({ type: 'result', result: { response: null, totalTokens, error } }) + '\n';
}

/** Exercises the real executor, substituting only the HTTP response and editor. */
async function execute(chunks, { status = 200, contentType = 'application/x-ndjson', readError = false, callback = true } = {}) {
    const updates = [];
    let request;
    let cancelled = false;
    const response = new Response(new ReadableStream({
        pull(controller) {
            if (chunks.length > 0) controller.enqueue(chunks.shift());
            else if (readError) controller.error(new Error('Connection lost'));
            else controller.close();
        },
        cancel() { cancelled = true; }
    }), { status, headers: { 'Content-Type': contentType } });
    const context = vm.createContext({
        TextDecoder, window: { csrfToken: 'test-csrf' }, console: { log() {} },
        fetch: async (url, options) => { request = { url, ...options }; return response; }
    });
    const Executor = vm.runInContext(sources + '\nAiRestExecutor;', context);
    const executor = new Executor({ set: (field, text) => updates.push({ field, text }) }, null);
    const execution = await executor.execute({ assistantId: 7, useStreaming: true, to: 'data' }, {},
        callback ? (text, final) => updates.push({ text, final }) : null);
    assert.equal(executor.curentAssistantId, null);
    assert.equal(executor.curentTimestamp, null);
    return { execution, updates, request, cancelled, locked: response.body.locked };
}

test('joined text and metadata preserve the token count and keep metadata out of the editor', async () => {
    const { execution, updates, request, locked } = await execute([Buffer.from(delta('Generated text.') + result())]);
    assert.equal(execution.totalTokens, 42);
    assert.equal(execution.success, true);
    assert.equal(execution.errorText, null);
    assert.deepEqual(updates.at(-1), { text: 'Generated text.', final: true });
    assert.equal(request.headers.Accept, 'application/x-ndjson');
    assert.equal(request.headers['X-CSRF-Token'], 'test-csrf');
    assert.equal(locked, false);
});

test('every byte split preserves Unicode, newlines, JSON-looking text and terminal metadata', async () => {
    const text = 'Žltý kôň 🐎\n{"totalTokens":999,"error":"generated text"}';
    const bytes = Buffer.from(delta(text) + result());
    for (let split = 1; split < bytes.length; split++) {
        const { execution, updates } = await execute([bytes.subarray(0, split), bytes.subarray(split)]);
        assert.equal(execution.totalTokens, 42, `split ${split}`);
        assert.equal(execution.success, true, `split ${split}`);
        assert.deepEqual(updates.at(-1), { text, final: true }, `split ${split}`);
    }
});

test('one-byte chunks retain Markdown HTML and explanatory text', async () => {
    const wire = delta('```html\n<body><p>Ahoj 🌍</p></body>\n```\nVysvetlenie úprav.') + result();
    const { execution, updates } = await execute([...Buffer.from(wire)].map(byte => Uint8Array.of(byte)));
    assert.equal(execution.success, true);
    assert.equal(execution.totalTokens, 42);
    assert.equal(execution.explanatoryText, 'Vysvetlenie úprav.');
    assert.deepEqual(updates.at(-1), { text: '<p>Ahoj 🌍</p>', final: true });
});

test('separate deltas update the editor progressively without a callback', async () => {
    const { execution, updates } = await execute([delta('Hello '), delta('world'), result()].map(value => Buffer.from(value)), { callback: false });
    assert.equal(execution.success, true);
    assert.deepEqual(updates, [
        { field: 'data', text: 'Hello ' }, { field: 'data', text: 'Hello world' }, { field: 'data', text: 'Hello world' }
    ]);
});

test('a provider error after partial text never reports success or finalizes the edit', async () => {
    const { execution, updates } = await execute([Buffer.from(delta('Partial text') + result(0, 'Provider failed'))]);
    assert.equal(execution.success, false);
    assert.equal(execution.errorText, 'Provider failed');
    assert.equal(execution.totalTokens, 0);
    assert.deepEqual(updates, [{ text: 'Partial text', final: false }]);
});

for (const [name, wire, options] of [
    ['missing terminal result', delta('Partial text'), {}],
    ['truncated terminal JSON', delta('Partial text') + '{"type":"result",', {}],
    ['malformed record', delta('Partial text') + 'invalid\n', {}],
    ['unknown record', delta('Partial text') + '{"type":"unknown"}\n', {}],
    ['missing token count', '{"type":"result","result":{}}\n', {}],
    ['duplicate result', result() + result(), {}],
    ['text after result', result() + delta('Late text'), {}],
    ['connection failure', delta('Partial text'), { readError: true }],
    ['HTTP error', '<html>Forbidden</html>', { status: 403, contentType: 'text/html' }],
    ['login redirect body', '<html>Login</html>', { contentType: 'text/html' }]
]) {
    test(`${name} is a failure, even after partial text`, async () => {
        const { execution, updates } = await execute([Buffer.from(wire)], options);
        assert.equal(execution.success, false);
        assert.ok(execution.errorText);
        assert.equal(updates.some(update => update.final), false);
        assert.equal(updates.some(update => update.text?.includes('<html>')), false);
    });
}

test('a complete final record without a trailing newline and zero tokens is valid', async () => {
    const { execution, updates } = await execute([Buffer.from(delta('Local answer') + result(0).trimEnd())]);
    assert.equal(execution.success, true);
    assert.equal(execution.totalTokens, 0);
    assert.deepEqual(updates.at(-1), { text: 'Local answer', final: true });
});
