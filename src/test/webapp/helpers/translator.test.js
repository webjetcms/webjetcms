const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

function fixture(t, cachedLanguage) {
    const dom = new JSDOM('', { url: 'https://example.test/admin/' });
    t.after(() => dom.window.close());
    const { window } = dom;
    window.userLng = 'sk';
    window.propertiesLastModified = 2;
    if (cachedLanguage) {
        window.localStorage.setItem('translate', JSON.stringify({
            currentLanguage: cachedLanguage, lastmodified: '1', label: 'original'
        }));
    }
    const requests = [];
    const context = vm.createContext({ window, console, $: { ajax: options => requests.push(options) } });
    for (const file of ['tools/tools', 'store/store', 'storage-handler/storage-handler', 'translator/translator']) {
        const filename = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/libs', file + '.js');
        const source = fs.readFileSync(filename, 'utf8').replace(/^import .*$/gm, '').replace(/^export (default )?/gm, '');
        vm.runInContext(source, context, { filename });
    }
    const translator = vm.runInContext('new Translator()', context);
    let ready = 0;
    translator.onAfterLoad(() => ready++);
    return { translator, requests, ready: () => ready };
}

test('a failed translation refresh starts the page with its cached language and leaves the update pending', t => {
    const { translator, requests, ready } = fixture(t, 'sk');
    translator.load();
    assert.equal(requests[0].data.since, 1);
    requests[0].error({ status: 404 });
    assert.equal(ready(), 1);
    assert.equal(translator.translate('label'), 'original');

    translator.load();
    assert.equal(requests[1].data.since, 1);
    requests[1].success({ lastmodified: '2', label: 'updated' });
    assert.equal(ready(), 2);
    assert.equal(translator.translate('label'), 'updated');
    assert.equal(translator.date, 2);
});

for (const [language, status] of [[null, 404], ['en', 404], ['sk', 401], ['sk', 403]]) {
    test(`translation failure ${status} with cache ${language} does not start the page`, t => {
        const { translator, requests, ready } = fixture(t, language);
        translator.load();
        requests[0].error({ status });
        assert.equal(ready(), 0);
    });
}
