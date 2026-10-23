const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const jquery = require('jquery');

const admin = path.resolve(__dirname, '../../../main/webapp/admin/v9');

/** Uses the real toast, selectpicker and jQuery removal lifecycle that detaches the open list. */
async function fixture(t, groupType) {
    const dom = new JSDOM('<body><button id="opener">Actions</button></body>', {
        url: 'http://webjet.test/admin/', runScripts: 'outside-only', pretendToBeVisual: true
    });
    t.after(() => dom.window.close());
    const { window } = dom;
    const $ = window.$ = window.jQuery = jquery(window);
    for (const script of ['bootstrap/dist/js/bootstrap.bundle.js', 'bootstrap-select/dist/js/bootstrap-select.js', 'toastr/toastr.js']) {
        window.eval(fs.readFileSync(path.join(admin, 'node_modules', script), 'utf8'));
    }
    let resolveShown;
    let resolveHidden;
    const shown = new Promise(resolve => { resolveShown = resolve; });
    const hidden = new Promise(resolve => { resolveHidden = resolve; });
    const info = window.toastr.info;
    window.toastr.info = (message, title, options) => info(message, title, {
        ...options,
        onShown() { options.onShown(); resolveShown(); },
        onHidden() { options.onHidden(); resolveHidden(); }
    });
    window.toastr.options.hideDuration = 1;
    window.toastr.options.closeDuration = 1;
    const buttons = [];
    const actions = [];
    window.table = {
        DATA: { id: 'datatableInit', url: '/admin/rest/web-pages' },
        rows: () => ({ data: () => ({ toArray: () => [{ docId: 1 }] }) }),
        addMoreAction: button => buttons.push(button),
        button: () => ({ node: () => $('#opener') }),
        executeAction: (...args) => actions.push(args)
    };
    window.WJ = {
        translate: key => key,
        focusWithoutTooltip: element => element.focus(),
        urlAddPath: (url, suffix) => url + suffix,
        urlAddParam: (url, name, value) => `${url}?${name}=${value}`
    };
    const webjet = fs.readFileSync(path.join(admin, 'src/js/webjet.js'), 'utf8');
    const start = webjet.indexOf('    function confirm(options) {');
    window.eval(webjet.slice(start, webjet.indexOf('\n    /**', start + 1)) + '\nWJ.confirm = confirm;');
    window.eval(fs.readFileSync(path.join(admin, 'src/js/select-picker.js'), 'utf8').replace(/^export /gm, '') + '\nWJ.initSelectPicker = initSelectPicker;');
    $.ajaxTransport('+*', () => ({
        send(headers, complete) {
            complete(200, 'OK', { text: JSON.stringify([{ label: 'Group', value: '30' }]) }, 'Content-Type: application/json');
        },
        abort() {}
    }));
    window.eval(fs.readFileSync(path.join(admin, 'src/js/pages/web-pages-list/group-actions.js'), 'utf8')
        .replace(/^export /gm, '') + `\nnew GroupActions(table, '${groupType}').addButtons();`);
    $('#opener').trigger('focus');
    buttons[0].action();
    await shown;
    const picker = $(`#datatableInit_${groupType}GroupAction`);
    picker.selectpicker('toggle');
    assert.equal($(`.bs-container.${groupType}-group-picker`).length, 1, 'The dropdown must be open outside the toast before closing');
    return { window, $, picker, actions, hidden };
}

for (const groupType of ['perex', 'user']) {
    for (const [close, selector] of Object.entries({
        close: '.toast-close-button', cancel: '[id^="confirmationNo"]', confirm: '[id^="confirmationYes"]'
    })) {
        test(`${groupType}: ${close} removes an open detached picker with its toast`, { timeout: 3000 }, async t => {
            const { window, $, picker, actions, hidden } = await fixture(t, groupType);
            $(selector).trigger('click');
            await hidden;
            assert.equal(window.document.contains(picker[0]), false, 'The toast and its select must be removed');
            assert.equal($(`.bs-container.${groupType}-group-picker`).length, 0, 'Closing the toast must also remove its detached dropdown');
            assert.equal(actions.length, close === 'confirm' ? 1 : 0);
            assert.equal(window.document.activeElement.id, 'opener');
        });
    }
}
