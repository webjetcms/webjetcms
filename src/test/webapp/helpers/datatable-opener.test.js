const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');

/** Replays an ID search through the real opener classes and a real server-side DataTable. */
function fixture(t, initialFilter) {
    const dom = new JSDOM('<input id="opener-id"><table id="datatableInit" class="datatableInit"><thead><tr><th>ID</th><th>Title</th></tr></thead></table>', {
        runScripts: 'outside-only', url: 'http://localhost/'
    });
    const window = dom.window;
    t.after(() => window.close());
    const $ = window.$ = window.jQuery = require('jquery')(window);
    require('datatables.net')(window, $);
    const target = { id: 21345, title: 'Opener target autotest' };
    const table = $('#datatableInit').DataTable({
        serverSide: true, paging: false, info: false, rowId: 'id',
        columns: [{ data: 'id' }, { data: 'title' }],
        searchCols: [{ search: initialFilter }, null],
        ajax(data, respond) {
            const rows = String(target.id).includes(data.columns[0].search.value) ? [target] : [];
            respond({ draw: data.draw, recordsTotal: 1, recordsFiltered: rows.length, data: rows });
        }
    });
    table.DATA = { id: 'datatableInit', serverSide: true };
    table.EDITOR = $({});
    table.getAjaxUrl = () => '/admin/rest/web-pages';
    const opened = [];
    table.wjEditFetch = row => opened.push(row.attr('id'));
    const filter = $('<div class="dt-scroll-headInner"><span><input class="filter-input-id"><button class="filtrujem"></button></span></div>')
        .appendTo('#datatableInit_wrapper');
    filter.find('input').val(initialFilter);
    let searches = 0;
    filter.find('button').on('click', () => {
        searches++;
        table.column(0).search(filter.find('input').val()).draw();
    });

    // Advance the openers' polling and deferred searches without wall-clock delays.
    const timers = [], intervals = new Map();
    let timerId = 0;
    window.setTimeout = callback => { timers.push(callback); return ++timerId; };
    window.setInterval = callback => { intervals.set(++timerId, callback); return timerId; };
    window.clearInterval = id => intervals.delete(id);
    const directory = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/libs');
    for (const [file, declaration, name] of [
        ['tools/tools.js', 'export class Tools', 'Tools'],
        ['js-tree-extends/abstract-opener/abstract-js-tree-opener.js', 'export default class AbstractJsTreeOpener', 'AbstractJsTreeOpener'],
        ['data-tables-extends/datatable-opener/datatable-opener.js', 'export class DatatableOpener', 'DatatableOpener']
    ]) {
        window.eval(fs.readFileSync(path.join(directory, file), 'utf8')
            .replace(/^import .*;\r?$/gm, '').replace(declaration, `window.${name} = class ${name}`));
    }
    return {
        window, $, table, opened, filter: filter.find('input'), searches: () => searches,
        flush() {
            for (let turn = 0; turn < 20 && (timers.length || intervals.size); turn++) {
                for (const callback of intervals.values()) callback();
                const pending = timers.splice(0);
                for (const callback of pending) callback();
            }
            assert.equal(timers.length + intervals.size, 0, 'The opener must finish its deferred work');
        }
    };
}

function openTarget(f, type) {
    if (type === 'webpage') {
        const opener = new f.window.AbstractJsTreeOpener();
        opener.dataTable = f.table;
        opener.jstree = { loaded: true };
        opener._currentId = 21345;
        opener.openEditor(21345);
    } else {
        const opener = new f.window.DatatableOpener();
        opener.dataTable = f.table;
        opener.bindEvents();
        opener.bindInput(f.$('#opener-id'));
        f.$('#opener-id').val('21345').trigger(f.$.Event('keyup', { key: 'Enter' }));
    }
    f.flush();
}

for (const type of ['webpage', 'datatable']) {
    test(`${type} opener replaces an old ID filter after an empty server response`, t => {
        const f = fixture(t, '27827');
        assert.equal(f.table.rows().count(), 0);
        assert.deepEqual(Object.keys(f.table.settings()[0].aIds), []);
        openTarget(f, type);
        assert.equal(f.filter.val(), '21345');
        assert.equal(f.searches(), 1, 'The empty result must trigger a search for the new ID');
        assert.deepEqual(f.opened, ['21345'], 'The requested record must open exactly once');
    });

    test(`${type} opener opens a loaded record without replacing its filter`, t => {
        const f = fixture(t, '');
        openTarget(f, type);
        assert.equal(f.searches(), 0);
        assert.deepEqual(f.opened, ['21345']);
    });
}
