const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { JSDOM } = require('jsdom');

const webjet = fs.readFileSync(path.resolve(__dirname, '../../../main/webapp/admin/v9/src/js/webjet.js'), 'utf8');
const start = webjet.indexOf('    function parseMarkdown(');
const parseMarkdown = vm.runInNewContext(webjet.slice(start, webjet.indexOf('    function initTooltip(', start)) + '\nparseMarkdown;');

test('WebJET Markdown renders pipe tables with optional borders, CRLF, alignment and escaped pipes', t => {
    const dom = new JSDOM();
    t.after(() => dom.window.close());
    const content = dom.window.document.createElement('div');
    content.innerHTML = parseMarkdown('Size | Count\r\n:--- | ---:\r\nSmall\\|Medium | **28**\r\nLarge\n\n'
        + '| Product | Price |\n| :---: | --- |\n| Cake | 10 |\n| Pie |\n\nAfter the tables.');
    const tables = content.querySelectorAll('table');
    assert.equal(tables.length, 2);
    assert.deepEqual([...tables[0].querySelectorAll('th')].map(cell => cell.textContent), ['Size', 'Count']);
    assert.equal(tables[0].querySelector('td').textContent, 'Small|Medium');
    assert.equal(tables[0].querySelector('td.text-end b').textContent, '28');
    assert.equal(tables[1].querySelector('th').className, 'text-center');
    assert.equal(tables[1].querySelectorAll('tbody tr')[1].lastElementChild.textContent, '');
    assert.match(content.textContent, /After the tables\./);
});

test('WebJET Markdown leaves incomplete tables, ordinary pipes and fenced table examples readable', () => {
    for (const markdown of [
        '| Size | Count |\n| --- |',
        '| Size | Count |\n| a | b |',
        'Use a | b in this sentence.',
        '```\n| Size | Count |\n| --- | --- |\n| Small | 28 |\n```'
    ]) {
        const html = parseMarkdown(markdown);
        assert.doesNotMatch(html, /<table>/);
        assert.match(html, /\|/);
    }
});
