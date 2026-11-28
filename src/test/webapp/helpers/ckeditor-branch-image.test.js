const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const filename = path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/ckeditor-branch-image.js');
const configureBranchImageUrl = vm.runInNewContext(fs.readFileSync(filename, 'utf8')
    .replace('export function', 'function') + '\nconfigureBranchImageUrl;');
const prefix = '/admin/webpages/branch-preview/42';
const logicalUrl = value => value.startsWith(prefix + '/') ? value.substring(prefix.length) : value;
const previewUrl = value => /^\/(images|files|thumb)\//.test(value) ? prefix + value : value;

function fixture() {
    const attributes = {};
    const element = {
        getAttribute: name => attributes[name],
        setAttribute: (name, value) => { attributes[name] = value; }
    };
    let value = '';
    let initialValue;
    let loadedUrl;
    const field = {
        getValue: () => value,
        setValue(url, silent) { value = url; if (!silent) field.onChange(); },
        setInitValue() { initialValue = value; },
        isChanged: () => value !== initialValue,
        setup(type, image) {
            if (type !== 1) return;
            this.setValue(image.getAttribute('data-cke-saved-src') || image.getAttribute('src'));
            this.setInitValue();
        },
        onChange() { loadedUrl = this.getValue(); },
        commit(type, image) {
            if (type === 1 && (this.getValue() || this.isChanged())) {
                image.setAttribute('data-cke-saved-src', this.getValue());
                image.setAttribute('src', this.getValue());
            }
            if (type === 8) image.setAttribute('src', '');
        }
    };
    configureBranchImageUrl(field, previewUrl, logicalUrl);
    return { field, element, loadedUrl: () => loadedUrl };
}

test('Selecting an image loads and inserts WORK bytes while retaining a logical saved URL', () => {
    const { field, element, loadedUrl } = fixture();
    const url = '/images/autotest.jpg?v=123';
    field.setValue(url);
    assert.equal(field.getValue(), url);
    assert.equal(loadedUrl(), prefix + url);
    field.commit(1, element);
    assert.equal(element.getAttribute('src'), prefix + url);
    assert.equal(element.getAttribute('data-cke-saved-src'), url);
});

test('Reopening an existing image hides the preview prefix and preserves its initial state', () => {
    const { field, element, loadedUrl } = fixture();
    const url = '/thumb/images/autotest.jpg?w=160&h=90&ip=6&v=123';
    element.setAttribute('src', prefix + url);
    element.setAttribute('data-cke-saved-src', prefix + url);
    field.setup(1, element);
    assert.equal(field.getValue(), url);
    assert.equal(field.isChanged(), false);
    assert.equal(loadedUrl(), prefix + url);
    field.commit(1, element);
    assert.equal(element.getAttribute('src'), prefix + url);
    assert.equal(element.getAttribute('data-cke-saved-src'), url);
});

test('External images and preview cleanup retain the native dialog behavior', () => {
    const { field, element, loadedUrl } = fixture();
    const url = 'https://example.org/autotest.jpg?v=123';
    field.setValue(url);
    field.commit(1, element);
    assert.equal(field.getValue(), url);
    assert.equal(loadedUrl(), url);
    assert.equal(element.getAttribute('src'), url);
    field.commit(8, element);
    assert.equal(element.getAttribute('src'), '');
});
