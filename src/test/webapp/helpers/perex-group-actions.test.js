const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');
const jquery = require('jquery');

const source = fs.readFileSync(path.resolve(__dirname,
    '../../../main/webapp/admin/v9/src/js/pages/web-pages-list/group-actions.js'), 'utf8').replace(/^export /gm, '');

/** Runs the real jQuery request serialization and intercepts only the HTTP transport. */
function fixture(t, ids, groupType) {
    const dom = new JSDOM('<body></body>', { url: 'https://webjet.test/admin/' });
    t.after(() => dom.window.close());
    const $ = jquery(dom.window);
    const requests = [];
    const buttons = [];
    $.ajaxTransport('+*', options => ({
        send(headers, complete) {
            requests.push({ method: options.type, url: options.url, body: options.data, headers });
            complete(200, 'OK', { text: '[]' }, 'Content-Type: application/json');
        },
        abort() {}
    }));
    const table = {
        DATA: { id: 'datatableInit', url: '/admin/rest/web-pages?groupId=67' },
        rows: () => ({ data: () => ({ toArray: () => ids.map(docId => ({ docId })) }) }),
        addMoreAction: button => buttons.push(button)
    };
    const WJ = {
        translate: key => key,
        notifyInfo() {},
        urlAddPath(url, suffix) {
            const target = new URL(url, dom.window.location.href);
            target.pathname += suffix;
            return target.pathname + target.search;
        },
        urlAddParam(url, name, value) {
            const target = new URL(url, dom.window.location.href);
            target.searchParams.append(name, value);
            return target.pathname + target.search;
        }
    };
    vm.runInNewContext(source + '\nnew GroupActions(table, groupType).addButtons();', {
        document: dom.window.document, $, WJ, table, groupType
    });
    return { buttons, requests };
}

for (const groupType of ['perex', 'user']) {
    for (const remove of [false, true]) {
        test(`${groupType}: ${remove ? 'Remove' : 'Add'} choices send 1000 IDs in JSON and keep the URL short`, t => {
            const ids = Array.from({ length: 1000 }, (_, index) => 100000 + index);
            const { buttons, requests } = fixture(t, ids, groupType);
            buttons[remove ? 1 : 0].action();

            assert.equal(requests.length, 1);
            const request = requests[0];
            assert.equal(request.method, 'POST');
            assert.equal(request.url, `/admin/rest/web-pages/${groupType}-groups?groupId=67&remove=${remove}`);
            assert.equal(request.headers['Content-Type'], 'application/json');
            assert.deepEqual(JSON.parse(request.body), ids);
        });
    }
}
