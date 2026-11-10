const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const context = vm.createContext({ Feature() {}, Before() {}, Scenario() {} });
const filename = path.resolve(__dirname, '../tests/apps/forms/multistep-forms-conditions.js');
vm.runInContext(fs.readFileSync(filename, 'utf8'), context, { filename });
const I = { assertEqual: assert.equal };
const expected = '<form><div><label>Email</label><span>autotest@example.com</span></div></form>';

/** Mail clients may strip the class identifying an empty form hint. */
test('Email comparison ignores empty hint containers with or without attributes', () => {
    for (const hint of ['<div class="form-hint"></div>', '<div class="form-hint" id="hint"></div>', '<div></div>']) {
        const actual = expected.replace('</label>', '</label>' + hint);
        context.compareTwoHtml(I, actual, expected);
    }
});

/** Content remains part of the assertion even when empty layout containers are ignored. */
test('Email comparison detects a nonempty hint or an unexpectedly visible field', () => {
    for (const content of ['<div class="form-hint">Important hint</div>', '<div><label>Hidden question</label><span>Hidden answer</span></div>']) {
        const actual = expected.replace('</form>', content + '</form>');
        assert.throws(() => context.compareTwoHtml(I, actual, expected), /Form HTML does not match exactly/);
    }
});

/** Selected answers must still match exactly. */
test('Email comparison detects a changed checkbox answer', () => {
    assert.throws(() => context.compareTwoHtml(I, '<form><span>[X]</span></form>', '<form><span>[&nbsp;]</span></form>'),
        /Form HTML does not match exactly/);
});
