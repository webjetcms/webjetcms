Feature('webpages.pagebuilder-whitespace');

const assert = require('assert');
// Customer development databases can supply their own existing Page Builder page.
const pageId = process.env.CODECEPT_PAGEBUILDER_DOCID || 57;
const source = `<section><div class="container"><div class="pb-grid"><div class="pb-col"><div class="column-content">
<ul class="pb-grid"><li class="pb-col pb-not-editable pb-always-mark"><svg><use href="#tick"></use></svg><div>Autotest item</div></li></ul>
<div class="pb-custom-container"><p>Autotest nested container</p></div>
<aside class="autotest-authored-aside">Autotest authored aside</aside>
<pre>First line\n\n\n    indented line</pre>
<textarea>First line\n\n\n    indented line</textarea>
<p class="autotest-inline"><span>First</span> <span>second</span>&nbsp;third</p>
</div></div></div></div><div class="autotest-prewrap" style="white-space: pre-wrap">First line\n\n\n    indented line</div></section>`;

Before(({ login }) => login('admin'));

Scenario('export removes nested controls before formatting and preserves authored whitespace', async ({ I, DTE, Document }) => {
    Document.resetPageBuilderMode();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + pageId);
    DTE.waitForEditor();
    I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow?.pageBuilderReady === true, [], 20);

    let html = source;
    const results = [];
    for (let round = 0; round < 3; round++) {
        I.executeScript(html => window.webpagesDatatable.EDITOR.field('data').s.opts.wjeditor.pbInsertContent(html, 'replace', true, false), html);
        I.waitForFunction(() => {
            const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe').contentWindow;
            const regions = Array.from(frame.document.querySelectorAll('#wjInline-docdata [data-ckeditor-instance]'));
            return regions.length > 0 && regions.every(element => frame.CKEDITOR.instances[element.dataset.ckeditorInstance]?.status === 'ready');
        }, [], 10);
        const result = await I.executeScript(() => {
            const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe').contentWindow;
            const pb = frame.pageBuilder;
            const controlsBefore = pb.$wrapper.find('li > aside').length;
            const node = pb.getSaveNode();
            return {
                html: node.html(),
                itemHtml: node.find('li').html(),
                containerHtml: node.find('.pb-custom-container').html(),
                controlsBefore,
                controlsAfter: pb.$wrapper.find('li > aside').length,
                exportedControls: node.find('aside:not(.autotest-authored-aside)').length,
                authoredAside: node.find('.autotest-authored-aside').text(),
                pre: node.find('pre').text(),
                textarea: node.find('textarea').text(),
                preWrap: node.find('.autotest-prewrap').text(),
                inline: node.find('.autotest-inline').text()
            };
        });
        results.push(result);
        html = result.html;
    }
    DTE.cancel();
    I.acceptPopup();

    for (const result of results) {
        assert.ok(result.controlsBefore > 0, 'The fixture must contain actual Page Builder controls inside the editor');
        assert.equal(result.controlsAfter, result.controlsBefore, 'Export must keep the live controls');
        assert.equal(result.exportedControls, 0, 'Export must remove editor controls');
        assert.ok(!/(?:\n[\t ]*){4,}/.test(result.itemHtml), 'Removed controls must not leave blank lines in list items');
        assert.ok(!/(?:\n[\t ]*){4,}/.test(result.containerHtml), 'Removed controls must not leave blank lines in containers');
        assert.equal(result.authoredAside, 'Autotest authored aside');
        assert.equal(result.pre, 'First line\n\n\n    indented line');
        assert.equal(result.textarea, 'First line\n\n\n    indented line');
        assert.equal(result.preWrap, 'First line\n\n\n    indented line');
        assert.equal(result.inline, 'First second\u00a0third');
    }
});

Scenario('switching to HTML cleans existing blank lines and preserves significant whitespace', async ({ I, DTE, Document }) => {
    Document.resetPageBuilderMode();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=' + pageId);
    DTE.waitForEditor();
    I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow?.pageBuilderReady === true, [], 20);

    const blankLines = '\t' + '\n\t'.repeat(12);
    const legacySource = source.replace('</section>', `<div class="pb-custom-container autotest-legacy">
<ul class="pb-grid"><li class="pb-col pb-not-editable pb-always-mark"><span>First</span>${blankLines}<span>second</span>${blankLines}</li></ul>${blankLines}
<div class="autotest-nbsp">&nbsp;${blankLines}&nbsp;</div>
<pre class="autotest-preserved">${blankLines}</pre><textarea class="autotest-preserved">${blankLines}</textarea>
<script type="application/json" class="autotest-preserved">${blankLines}</script><style class="autotest-preserved">${blankLines}</style>
${['pre', 'pre-wrap', 'pre-line', 'break-spaces'].map(mode => `<div style="white-space: ${mode}"><span class="autotest-preserved"><b>First</b>${blankLines}<b>second</b></span></div>`).join('')}
</div></section>`);
    I.executeScript(html => window.webpagesDatatable.EDITOR.field('data').s.opts.wjeditor.pbInsertContent(html, 'replace', true, false), legacySource);

    const results = [];
    for (let round = 0; round < 3; round++) {
        I.waitForFunction(() => {
            const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe').contentWindow;
            const regions = Array.from(frame.document.querySelectorAll('#wjInline-docdata [data-ckeditor-instance]'));
            return regions.length > 0 && regions.every(element => frame.CKEDITOR.instances[element.dataset.ckeditorInstance]?.status === 'ready');
        }, [], 10);
        results.push(await I.executeScript(async () => {
            const editor = window.webpagesDatatable.EDITOR.field('data').s.opts.wjeditor;
            const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe').contentWindow;
            const liveHtmlBefore = frame.document.querySelector('.autotest-legacy').innerHTML;
            const copiedHtml = frame.pageBuilder.getSaveNode(frame.document.querySelector('.autotest-legacy')).find('li').html();
            await editor.switchEditingMode('html', true);
            const html = editor.getData();
            const node = new DOMParser().parseFromString(html, 'text/html');
            const result = {
                copiedHtml,
                itemHtml: node.querySelector('.autotest-legacy li').innerHTML,
                containerWhitespace: Array.from(node.querySelector('.autotest-legacy').childNodes).filter(n => n.nodeType === 3).map(n => n.data),
                nbsp: node.querySelector('.autotest-nbsp').textContent,
                preserved: Array.from(node.querySelectorAll('.autotest-preserved')).map(n => n.innerHTML),
                liveUnchanged: frame.document.querySelector('.autotest-legacy').innerHTML === liveHtmlBefore,
                codeMirrorHtml: document.querySelector('.CodeMirror').CodeMirror.getValue(),
                html
            };
            await editor.switchEditingMode('pageBuilder', true);
            return result;
        }));
    }
    DTE.cancel();
    I.acceptPopup();

    for (const result of results) {
        assert.ok(!/(?:\n[\t ]*){4,}/.test(result.itemHtml), 'HTML mode must clean already saved blank lines in list items');
        assert.ok(!/(?:\n[\t ]*){4,}/.test(result.copiedHtml), 'Block export must clean already saved blank lines too');
        assert.ok(result.containerWhitespace.every(text => !/(?:\n[\t ]*){4,}/.test(text)), 'HTML mode must clean already saved blank lines in containers');
        assert.match(result.itemHtml, /<span>First<\/span>\s+<span>second<\/span>/, 'Cleanup must keep the separator between inline elements');
        assert.equal(result.nbsp, '\u00a0' + blankLines + '\u00a0', 'Nonbreaking spaces must be preserved');
        assert.deepEqual(result.preserved, [blankLines, blankLines, blankLines, blankLines, ...Array(4).fill('<b>First</b>' + blankLines + '<b>second</b>')]);
        assert.ok(result.liveUnchanged, 'Export must not modify the live content');
        assert.equal(result.codeMirrorHtml, result.html, 'The source shown in HTML mode must contain the cleaned output');
    }
});
