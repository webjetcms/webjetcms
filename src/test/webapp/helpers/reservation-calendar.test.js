const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

test('A late reservation response cannot replace the selected month or its occupancy', t => {
    const root = path.resolve(__dirname, '../../../main/webapp');
    const template = fs.readFileSync(path.join(root, 'apps/reservation/mvc/day-book.html'), 'utf8');
    const dom = new JSDOM(template, { runScripts: 'outside-only' });
    t.after(() => dom.window.close());
    const { window } = dom;
    const { document } = window;
    window.matchMedia = () => ({ matches: false, addEventListener() {} });
    document.querySelector('[data-th-field="*{price}"]').id = 'price';
    const requests = [];
    window.$ = require('jquery')(window);
    window.$.ajax = options => requests.push(options);
    const context = dom.getInternalVMContext();
    vm.runInContext(fs.readFileSync(path.join(root, 'components/_common/vannila-calendar-pro/index.js'), 'utf8'), context);
    const year = new Date().getFullYear() + 5;
    const script = document.querySelector('script[th\\:inline]').textContent
        .replace('/*[[${localization}]]*/ ""', '"sk"')
        .replace('/*[[${currentYear}]]*/ 0', String(year))
        .replace('/*[[${currentMonth}]]*/ 0', '10');
    vm.runInContext(script, context);
    vm.runInContext('currentMonth = 0; loadSupportData();', context);
    assert.equal(requests.length, 2);

    function deliver(request, month, capacity) {
        request.success(JSON.stringify({
            currentYear: year, currentMonth: month, reservationCountMap: {}, disabledDays: [],
            maxReservationCount: capacity, reservationPriceMap: {}, checkOutOnlyDays: []
        }));
    }
    deliver(requests[1], 0, 2);
    deliver(requests[0], 10, 5);
    assert.equal(document.querySelector('#actualDate').value, `${year}-01`);
    assert.equal(document.querySelector(`[data-vc-date="${year}-01-01"] .dayBookCalendarMeta`)?.textContent, '0/2');
});
