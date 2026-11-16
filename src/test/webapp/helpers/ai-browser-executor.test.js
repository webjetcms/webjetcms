const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const directory = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables/ai');
const source = ['../../../src/js/ai/ai-tool-transport.js', 'ai-execution-result.js', 'ai-browser-executor.js'].map(file =>
    fs.readFileSync(path.join(directory, file), 'utf8').replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '')
).join('\n');

function executor(api, ui, apiName = 'Rewriter') {
    const Executor = vm.runInNewContext(source + '\nAiBrowserExecutor', {
        self: { [apiName]: api }, navigator: { userActivation: { isActive: true } }, AbortController,
        WJ: { translate: (key, ...params) => [key, ...params].join(':') }
    });
    return new Executor(null, ui);
}

test('changing the active tools rebuilds the session with its assistant and skill instructions', async t => {
    const model = { prompt: message => JSON.stringify({ type: 'final', text: message }), destroy() {} };
    const create = t.mock.fn(async () => model);
    const instance = executor({ availability: async () => 'available', create }, null, 'LanguageModel');
    const assistant = { instructions: 'LanguageModel: {"initialPrompts":[{"role":"system","content":"Speak Slovak"}]}',
        toolsEnabled: true, toolsWrite: true, tools: [], skillInstructions: 'Quiz guidance' };
    for (const toolInstructions of ['Quiz tools', 'Page tools']) {
        const reply = await instance.execute({ ...assistant, toolInstructions }, { userPrompt: 'Hello' }, true);
        assert.deepEqual(JSON.parse(reply.explanatoryText), { prompt: 'Hello' });
    }
    assert.deepEqual(create.mock.calls.map(call => call.arguments[0].initialPrompts[0].content),
        ['Speak Slovak\n\nQuiz guidance\n\nQuiz tools', 'Speak Slovak\n\nQuiz guidance\n\nPage tools']);
});

test('an unavailable model reports its status without attempting to create a session', async () => {
    let creates = 0;
    const instance = executor({
        availability: async () => 'unavailable',
        create: async () => {
            creates++;
            throw new Error('The device is unable to create a session to run the model. Please check the result of availability() first.');
        }
    });
    const result = await instance.execute({ instructions: 'Rewriter: {"tone":"more-casual"}' }, { inputValue: 'Text' });
    assert.equal(creates, 0);
    assert.match(result.errorText, /components\.ai_assistants\.browser\.model_unavailable\.js:Rewriter/);
});

test('completed model download replaces the previous percentage with the preparation status', () => {
    const statuses = [];
    const instance = executor({}, { setDownloadStatus: percent => statuses.push(percent) });
    instance._setDownloadStatus({ loaded: 0.9 });
    instance._setDownloadStatus({ loaded: 1 });
    assert.deepEqual(statuses, [90, 100]);
});

test('a native session failure is still reported even when Chrome claims the model is available', async () => {
    const message = 'The device is unable to create a session to run the model. Please check the result of availability() first.';
    const instance = executor({ availability: async () => 'available', create: async () => { throw new Error(message); } });
    const result = await instance.execute({ instructions: 'Rewriter: {"tone":"more-casual"}' }, { inputValue: 'Text' });
    assert.equal(result.success, false);
    assert.equal(result.errorText, message);
});

for (const availability of ['downloadable', 'downloading']) {
    test(`model download reports completion and then executes the assistant (${availability})`, async () => {
        const progress = [];
        const output = [];
        const instance = executor({
            availability: async () => availability,
            create: async options => {
                options.monitor({ addEventListener: (name, callback) => {
                    assert.equal(name, 'downloadprogress');
                    callback({ loaded: 0.9 });
                    callback({ loaded: 1 });
                } });
                return { rewrite: async text => 'Rewritten: ' + text };
            }
        }, { requestBrowserConfirmation: async () => true, setDownloadStatus: value => progress.push(value) });
        const result = await instance.execute({ instructions: 'Rewriter: {"tone":"more-casual"}' },
            { inputValue: 'Original' }, false, text => output.push(text));
        assert.equal(result.success, true);
        assert.deepEqual(progress, [90, 100]);
        assert.deepEqual(output, ['Rewritten: Original']);
    });
}
