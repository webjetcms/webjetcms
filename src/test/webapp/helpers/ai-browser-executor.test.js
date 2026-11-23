const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { setImmediate: tick } = require('node:timers/promises');
const { JSDOM } = require('jsdom');

const directory = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables/ai');
const source = ['ai-execution-result.js', 'ai-browser-executor.js'].map(file =>
    fs.readFileSync(path.join(directory, file), 'utf8').replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '')
).join('\n');
const modelApi = (create, availability = 'available') => ({ create, availability: () => availability });

function fixture(apis, ui = {}, globals = {}) {
    const Executor = vm.runInNewContext(source + '\nAiBrowserExecutor', {
        self: apis, ...apis, AbortController, navigator: { userActivation: { isActive: true } },
        WJ: { translate: (...args) => args.join(':'), parseMarkdown: text => text }, ...globals
    });
    const executor = new Executor(null, { setCurrentStatus() {}, ...ui });
    const updates = [];
    const run = (instructions, input = { inputValue: 'Field' }, { useStreaming, reuse } = {}) =>
        executor.execute({ instructions, useStreaming }, input, reuse, (text, final) => updates.push([text, final]));
    return { executor, run, updates };
}

for (const [name, method] of Object.entries({
    Writer: 'write', Rewriter: 'rewrite', Translator: 'translate', LanguageDetector: 'detectLanguage',
    Summarizer: 'summarize', LanguageModel: 'prompt'
})) {
    test(name + ' dispatches ordinary and streaming requests', async () => {
        const api = modelApi(() => ({
            [method]: text => text + '!',
            async *[method + 'Streaming'](text) { yield text; yield '!'; }
        }));
        const { run, updates } = fixture({ [name]: api });
        const input = { userPrompt: 'Question', inputValue: 'Field' };
        assert.equal((await run(name + ': {}', input)).success, true);
        assert.equal((await run(name + ': {}', input, { useStreaming: true, reuse: true })).success, true);
        assert.deepEqual(updates, [['Question!', true], ['Question', false], ['Question!', false], ['Question!', true]]);
    });
}

test('late creation is discarded and only a configuration change replaces the active model', async () => {
    const late = Promise.withResolvers();
    const sessions = [];
    const { executor, run, updates } = fixture({ LanguageModel: modelApi(({ initialPrompts }) => {
        sessions.push(initialPrompts[0].content);
        return sessions.length === 1 ? late.promise : { prompt: text => text, destroy: () => sessions.push('destroy active') };
    }) });
    const ask = (text, content = 'Slovak') => run('LanguageModel: ' + JSON.stringify({ initialPrompts: [{ role: 'system', content }] }),
        { inputValue: text }, { reuse: true });
    const pending = ask('Old');
    await tick();
    executor.destroy();
    await ask('New');
    late.resolve({ destroy: () => sessions.push('discard late'), prompt: assert.fail });
    assert.equal((await pending).stopped, true);
    await ask('Next');
    await ask('Changed', 'English');
    assert.deepEqual(updates, [['New', true], ['Next', true], ['Changed', true]]);
    assert.deepEqual(sessions, ['Slovak', 'Slovak', 'discard late', 'destroy active', 'English']);
});

test('translation detects both languages with one detector and releases each model', async t => {
    const released = [];
    const detect = t.mock.fn(() => ({ detect: text => [{ detectedLanguage: text === 'hello' ? 'en' : 'sk' }],
        destroy: () => released.push('detector') }));
    const { executor, run, updates } = fixture({
        LanguageDetector: { create: detect }, Writer: modelApi(() => ({ write: () => 'hello', destroy() {} })),
        Translator: modelApi(({ sourceLanguage, targetLanguage }) => {
            assert.deepEqual([sourceLanguage, targetLanguage], ['en', 'sk']);
            return { translate: text => text + ' (sk)', destroy: () => released.push('translator') };
        })
    });
    assert.equal((await run('Writer: {"translateOutputLanguage":"autodetect"}', { inputValue: 'ahoj' })).success, true);
    assert.deepEqual(updates, [['hello', true], ['hello (sk)', true]]);
    assert.equal(detect.mock.callCount(), 1);
    assert.deepEqual(released, ['translator']);
    executor.destroy();
    assert.deepEqual(released, ['translator', 'detector']);
});

test('editor activation leads to a cancellable download', async t => {
    const dom = new JSDOM('<div id="toast-container-ai-content"></div>');
    t.after(() => dom.window.close());
    const $ = require('jquery')(dom.window);
    const userActivation = { isActive: false };
    const create = t.mock.fn();
    const { run } = fixture({ Translator: modelApi(create, 'downloadable') }, {}, { $, navigator: { userActivation } });
    const pending = run('Translator: {}');
    await tick();
    assert.match($('.btn-primary').text(), /userActivationPrompt/);
    userActivation.isActive = true;
    $('.btn-primary').trigger('click');
    await tick();
    assert.match($('.btn-primary').text(), /download_model.download/);
    $('.btn-outline-secondary').trigger('click');
    assert.match((await pending).errorText, /download_model.canceled/);
    assert.equal(create.mock.callCount(), 0);
});

test('skips unavailable models and reports native creation errors once available', async t => {
    const message = 'The device is unable to create a session to run the model. Please check the result of availability() first.';
    const create = t.mock.fn(() => { throw new Error(message); });
    const api = modelApi(create, 'unavailable');
    const { run } = fixture({ Rewriter: api });
    assert.match((await run('Rewriter: {}')).errorText, /model_unavailable\.js:Rewriter$/);
    assert.equal(create.mock.callCount(), 0);
    api.availability = () => 'available';
    const result = await run('Rewriter: {}');
    assert.equal(result.success, false);
    assert.equal(result.errorText, message);
});

for (const availability of ['downloadable', 'downloading']) {
    test(`model download reports completion and then executes the assistant (${availability})`, async () => {
        const progress = [];
        const api = {
            availability: async () => availability,
            create: async options => {
                options.monitor({ addEventListener: (name, callback) => {
                    assert.equal(name, 'downloadprogress');
                    callback({ loaded: 0.9 });
                    callback({ loaded: 1 });
                } });
                return { rewrite: async text => 'Rewritten: ' + text };
            }
        };
        const { run, updates } = fixture({ Rewriter: api }, {
            requestBrowserConfirmation: () => true, setDownloadStatus: percent => progress.push(percent)
        });
        assert.equal((await run('Rewriter: {}')).success, true);
        assert.deepEqual(progress, [90, 100]);
        assert.deepEqual(updates, [['Rewritten: Field', true]]);
    });
}
