const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

const directory = path.resolve(__dirname, '../../../main/webapp/admin/v9/npm_packages/webjetdatatables');
const source = fs.readFileSync(path.join(directory, 'index.js'), 'utf8').replace(/\r/g, '');
const calculate = source.slice(source.indexOf('    function calculateAutoPageLength('), source.indexOf('    if ($(dataTableSelector).length'));
const headerSelection = source.match(/    const versionHeader = [\s\S]*?;/)[0];

/** Give jQuery a viewport and DataTables a measurable scroll body, which jsdom does not lay out. */
function fixture(t, { gallery = false, versioned = true, height = 468 } = {}) {
    const id = gallery ? 'galleryTable' : 'statTable';
    const dom = new JSDOM(`<div class="ly-header ${versioned ? 'ly-header--versions' : ''}" style="height:48px"></div>
        <table id="${id}" class="${gallery ? 'cardView' : ''}" style="width:330px"></table>`, { url: 'http://localhost/' });
    t.after(() => dom.window.close());
    const { window } = dom;
    const { document } = window;
    const $ = require('jquery')(window);
    Object.defineProperties(document.documentElement, { clientWidth: { value: 760 }, clientHeight: { value: height } });
    const DATA = { id, nestedModal: false, autoHeight: gallery, autoPageLength: 10 };
    const context = vm.createContext({
        $, window, document, DATA, TABLE: undefined, getComputedStyle: window.getComputedStyle.bind(window),
        // These top-level pages are outside both iframe and modal layouts.
        dtWJ: { versionHeaderSelector: '.ly-header--versions', getScrollbarWidth: () => 0 },
        WJ: { translate: (key, count) => String(count) }
    });
    vm.runInContext(headerSelection + '\n' + calculate, context);
    return {
        capacity() {
            context.calculateAutoPageLength(false);
            return DATA.autoPageLength;
        },
        measure(width, height) {
            const wrapper = document.createElement('div');
            wrapper.id = id + '_wrapper';
            const body = document.createElement('div');
            body.className = 'dt-scroll-body';
            body.style.width = width + 'px';
            Object.defineProperties(body, { clientWidth: { value: width }, clientHeight: { value: height } });
            wrapper.append(body);
            body.append(document.getElementById(id));
            document.body.append(wrapper);
            context.TABLE = { table: () => ({ container: () => wrapper }) };
        }
    };
}

test('Gallery cards reclaim the hidden filter header only in the initial height estimate', t => {
    const gallery = fixture(t, { gallery: true });
    assert.equal(gallery.capacity(), 4, 'Two rows of two cards fit before the scroll body is available');
    gallery.measure(360, 227);
    assert.equal(gallery.capacity(), 2, 'Only one full 136px row fits the measured body');
});

for (const [width, height, expected] of [[610, 45, 3], [85, 330, 2], [85, 45, 1]]) {
    test(`Gallery paging stays positive in a ${width}×${height}px card area`, t => {
        const gallery = fixture(t, { gallery: true });
        gallery.measure(width, height);
        assert.equal(gallery.capacity(), expected);
    });
}

test('Tables with autoHeight disabled retain the mobile page length with a version header', t => {
    for (const [height, expected] of [[390, 6], [650, 12]]) {
        for (const versioned of [false, true]) {
            const stats = fixture(t, { versioned, height });
            assert.equal(stats.capacity(), expected);
            stats.measure(400, 41);
            assert.equal(stats.capacity(), expected, 'Fixed-height tables ignore their scroll body when sizing pages');
        }
    }
});
