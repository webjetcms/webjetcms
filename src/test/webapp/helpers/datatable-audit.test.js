const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

/** Runs the shared audit assertions against records returned by the audit screen. */
async function checkAudit(records) {
    let row = 0;
    const I = {
        say() {}, resizeWindow() {}, amOnPage() {}, fillField() {}, wjSetDefaultWindowSize() {},
        formatDateTime: () => '2026-10-28 10:00:00',
        getTotalRows: async () => records.length,
        grabTextFrom: async selector => records[Number(selector.match(/nth-child\((\d+)\)/)[1]) - 1].split('\n')[0],
        clickCss(selector) {
            const match = selector.match(/nth-child\((\d+)\)/);
            if (match) row = Number(match[1]) - 1;
        },
        grabValueFrom: async () => records[row],
        assertAbove: (actual, expected, message) => assert.ok(actual > expected, message),
        assertTrue: (actual, message) => assert.ok(actual, message)
    };
    const context = vm.createContext({
        module: { exports: {} }, inject: () => ({ I }),
        require: () => ({ waitForLoader() {}, waitForEditor() {}, cancel() {} })
    });
    const filename = path.resolve(__dirname, '../pages/DataTables.js');
    vm.runInContext(fs.readFileSync(filename, 'utf8'), context, { filename });
    await context.module.exports.testAuditRecords(Date.now(), 'cookie-autotest');
}

const supplementaryUpdate = 'UPDATE:\nprovider: null -> components.gdpr.cookies.cookie-autotest-dupli.cate.provider';

test('Audit accepts a duplicate translation update alongside the required rename', async () => {
    await checkAudit([
        'CREATE:\ncookieName: cookie-autotest',
        'UPDATE:\ncookieName: cookie-autotest -> cookie-autotest-chan.ge',
        supplementaryUpdate,
        'DELETE:\ncookieName: cookie-autotest-chan.ge'
    ]);
});

test('Supplementary updates cannot replace the required rename audit record', async () => {
    await assert.rejects(checkAudit([
        'CREATE:\ncookieName: cookie-autotest',
        supplementaryUpdate,
        'DELETE:\ncookieName: cookie-autotest-chan.ge'
    ]), /UPDATE/);
});
