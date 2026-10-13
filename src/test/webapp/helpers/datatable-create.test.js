const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

/** Exercises WebJET initialization with the real Editor event and form lifecycle. */
function fixture(t, url = '/admin/rest/web-pages?groupId=67') {
    const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://localhost/' });
    t.after(() => dom.window.close());
    const $ = require('jquery')(dom.window);
    require('datatables.net')(dom.window, $);
    require('datatables.net-editor')(dom.window, $);
    let opens = 0;
    $.fn.dataTable.Editor.display.autotest = {
        init() { return this; },
        open(editor, form, callback) { opens++; callback(); },
        close(editor, callback) { callback(); },
        node() { return dom.window.document.body; }
    };
    const editor = new $.fn.dataTable.Editor({ fields: [{ name: 'title', label: 'Title' }], display: 'autotest' });
    const requests = [];
    const responses = [];
    const notifications = [];
    const errors = [];
    const initializations = [];
    const valuesAtAjaxCompletion = [];
    // Keep the real Ajax callback and global-event ordering while replacing only the HTTP transport.
    $.ajaxTransport('+*', options => ({
        send(headers, complete) {
            requests.push(options.url);
            const response = responses.shift();
            setImmediate(() => complete(
                response.status ?? 200,
                response.status ? 'Service unavailable' : 'OK',
                { text: response.body ?? JSON.stringify(response.json ?? {}) },
                'Content-Type: application/json'
            ));
        },
        abort() {}
    }));
    $(dom.window.document).on('ajaxComplete', () => valuesAtAjaxCompletion.push(editor.val('title')));
    const context = vm.createContext({
        $, EDITOR: editor, DATA: { fetchOnCreate: true }, TABLE: { getAjaxUrl: () => url },
        WJ: { translate: key => key, notifyError: (...args) => notifications.push(args) },
        console: { error: (...args) => errors.push(args) }
    });
    const filename = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables/index.js');
    const source = fs.readFileSync(filename, 'utf8').replace(/\r/g, '');
    const setJsonStart = source.indexOf('        $.fn.dataTable.Editor.prototype.setJson = function (json) {');
    vm.runInContext(source.slice(setJsonStart, source.indexOf('\n        //fix prace s datumami', setJsonStart)), context);
    const createStart = source.indexOf('        if (DATA.fetchOnCreate) {');
    vm.runInContext(source.slice(createStart, source.indexOf("\n        EDITOR.on('initEdit'", createStart)), context);
    editor.on('initCreate', event => { initializations.push(event.result); });
    return { editor, requests, responses, notifications, errors, initializations, valuesAtAjaxCompletion, opens: () => opens };
}

/** Resolves when the real Editor lifecycle reaches the expected outcome. */
function createAndWait(editor, event) {
    return new Promise(resolve => {
        editor.one(event, resolve);
        editor.create();
    });
}

test('New-record defaults preserve query parameters and populate the form before Ajax completion', { timeout: 5000 }, async t => {
    const { editor, requests, responses, notifications, valuesAtAjaxCompletion, opens } = fixture(t);
    responses.push({ json: { title: 'Autotest server defaults' } });
    await createAndWait(editor, 'opened');
    assert.deepEqual(requests, ['/admin/rest/web-pages/-1?groupId=67']);
    assert.equal(editor.val('title'), 'Autotest server defaults');
    assert.equal(opens(), 1);
    assert.equal(notifications.length, 0);
    assert.deepEqual(valuesAtAjaxCompletion, ['Autotest server defaults'], 'Global Ajax listeners must see populated defaults');
});

test('Failed defaults cancel opening, report the error and allow a later successful create', { timeout: 5000 }, async t => {
    const { editor, responses, notifications, errors, opens } = fixture(t, '/admin/rest/web-pages');
    responses.push({ json: { title: 'Autotest previous form' } });
    await createAndWait(editor, 'opened');
    editor.close();

    responses.push({ status: 503 });
    await createAndWait(editor, 'cancelOpen');
    assert.equal(editor.display(), false, 'A failed request must not open a form with stale defaults');
    assert.equal(opens(), 1);
    assert.deepEqual(notifications, [['datatables.error.title.js', 'datatables.error.network.js']]);
    assert.equal(errors.length, 1);
    assert.equal(errors[0][1], 503, 'Diagnostics must identify the failed HTTP response');

    responses.push({ json: { title: 'Autotest retry defaults' } });
    await createAndWait(editor, 'opened');
    assert.equal(editor.val('title'), 'Autotest retry defaults');
    assert.equal(opens(), 2, 'The one-time cancellation must not block the next create');
});

test('Errors applying defaults also cancel initialization and permit retry', { timeout: 5000 }, async t => {
    const { editor, responses, notifications, errors, opens } = fixture(t);
    const originalVal = editor.val;
    editor.val = function (name, value) {
        if (value === 'Autotest invalid defaults') throw new Error('Autotest invalid defaults');
        return originalVal.apply(this, arguments);
    };
    responses.push({ json: { title: 'Autotest invalid defaults' } });
    await createAndWait(editor, 'cancelOpen');
    assert.equal(opens(), 0);
    assert.equal(notifications.length, 1);
    assert.match(String(errors[0][2]), /Autotest invalid defaults/);

    responses.push({ json: { title: 'Autotest valid defaults' } });
    await createAndWait(editor, 'opened');
    assert.equal(editor.val('title'), 'Autotest valid defaults');
    assert.equal(opens(), 1);
});

test('Malformed JSON cancels initialization and permits retry', { timeout: 5000 }, async t => {
    const { editor, responses, notifications, errors, opens } = fixture(t);
    responses.push({ body: 'Autotest invalid JSON' });
    await createAndWait(editor, 'cancelOpen');
    assert.equal(opens(), 0);
    assert.equal(notifications.length, 1);
    assert.equal(errors[0][1], 200, 'Parsing may fail even after a successful HTTP response');

    responses.push({ json: { title: 'Autotest valid defaults' } });
    await createAndWait(editor, 'opened');
    assert.equal(editor.val('title'), 'Autotest valid defaults');
    assert.equal(opens(), 1);
});

for (const action of ['create', 'edit', 'remove']) {
    test(`A failed hidden create does not cancel the next ${action}`, { timeout: 5000 }, async t => {
        const { editor, responses, notifications, initializations, opens } = fixture(t);
        responses.push({ status: 503 });
        editor.create(false);
        await initializations.at(-1);
        assert.equal(editor.display(), false);
        assert.equal(opens(), 0);
        assert.equal(notifications.length, 1);

        const opened = new Promise(resolve => editor.one('opened', resolve));
        if (action === 'create') {
            responses.push({ json: { title: 'Autotest retry defaults' } });
            editor.create();
        } else {
            editor[action]();
        }
        await opened;
        assert.equal(opens(), 1, 'Cancellation must belong only to the failed initialization');
        if (action === 'create') assert.equal(editor.val('title'), 'Autotest retry defaults');
    });
}
