Feature('webpages.pagebuilder-save');

const assert = require('node:assert/strict');
const groupId = process.env.CODECEPT_PAGEBUILDER_GROUPID || 34495;
const frameSelector = '#DTE_Field_data-pageBuilderIframe';

Before(({ login }) => login('admin'));

Scenario('adding a block with no CKEditor instances closes the library and initializes the new editor', async ({ I, DT, DTE, Document }) => {
    Document.resetPageBuilderMode();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=' + groupId);
    DT.waitForLoader();
    const routePattern = '**/*inlineEditorAdmin=true*';
    const loadWithoutEditors = async route => {
        const response = await route.fetch();
        const body = await response.text();
        assert.ok(body.includes('initializePageBuilder();'), 'The response must contain the real Page Builder startup');
        // Inject the initial HTML before startup so the frame never inherits WJ from an earlier editor.
        const html = '<section id="no-editor-autotest"><div class="container"><h1>Heading without columns autotest</h1></div></section>';
        const startup = '$("#wjInline-docdata").html(' + JSON.stringify(html) + '); initializePageBuilder();';
        await route.fulfill({ response, body: body.replace('initializePageBuilder();', startup) });
    };
    await I.mockRoute(routePattern, loadWithoutEditors, { times: 1 });
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-content-tab');
    // The readiness flag and editor registry have no equivalent visible UI condition.
    I.waitForFunction(() => {
        const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow;
        return frame?.pageBuilderReady === true && frame.document.querySelector('#no-editor-autotest') !== null;
    }, [], 20);
    I.switchTo(frameSelector);
    const count = await I.executeScript(() => Object.keys(CKEDITOR.instances).length);
    assert.equal(count, 0, 'The page must start without any CKEditor instance');
    I.waitForElement('.pb-library .library-tab-link[data-library-type="basic"]', 10);
    I.click('.pb-empty-placeholder-wrapper .pb-empty-placeholder__button');
    I.waitForVisible('.pb-library', 10);
    I.click('.pb-library--section .library-tab-link[data-library-type="basic"]');
    I.click('.library-template-block--section .library-tab-item-button[data-library-item-id="pb-basic-2.4"]');
    I.seeNumberOfElements('#wjInline-docdata > section', 2);
    I.waitForInvisible('.pb-library', 5);
    I.see('Heading without columns autotest', '#no-editor-autotest');
    I.switchTo();
    I.waitForFunction(() => {
        const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe').contentWindow;
        return Object.values(frame.CKEDITOR.instances).some(editor => editor.status === 'ready');
    }, [], 20);
    DTE.cancel();
    I.acceptPopup();
    await I.stopMockingRoute(routePattern, loadWithoutEditors);
    Document.resetPageBuilderMode();
});

Scenario('malformed Page Builder HTML preserves unowned content and live edits through save and reopen', async ({ I, DT, DTE, Document }) => {
    const title = 'pb-save-autotest-' + I.getRandomText();
    const source = '<section id="heading-autotest" data-pb-id="heading"><div class="container">' +
        '<h1 class="display-3">Nastavenia autotest</h1><p>Hlasové a&nbsp;dátové služby.</p>' +
        '<div id="stale-autotest" data-ckeditor-instance="missing-editor-autotest" role="textbox" tabindex="0">' +
        '<p>Pôvodný obsah autotest</p></div></div></section>' +
        '<section><div class="container"><div class="row"><div class="col-12"><div class="column-content pb-editable">' +
        '<div data-ckeditor-instance="editor5" aria-multiline="true" role="textbox" spellcheck="true" tabindex="0">' +
        '<h3 id="editable-autotest">Hľadám pomoc autotest</h3><a href="/apple">iOS</a></div>' +
        '</div></div></div></div></section>';
    let saved = false;
    Document.resetPageBuilderMode();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=' + groupId);
    DT.waitForLoader();
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    DTE.fillField('title', title);
    DTE.fillField('navbar', title);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    // Readiness and instance ownership are JavaScript state rather than visible UI conditions.
    I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow?.pageBuilderReady === true, [], 20);
    I.switchTo(frameSelector);
    I.selectOption('.exit-inline-editor select', 'html');
    I.switchTo();
    I.waitForVisible('.CodeMirror', 15);
    await I.executeScript(html => {
        document.querySelector('.CodeMirror').CodeMirror.setValue(html);
        window.switchEditorType({ value: 'pageBuilder' });
    }, source);
    I.waitForFunction(() => {
        const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow;
        const host = frame?.document.querySelector('#editable-autotest')?.closest('.editableElement');
        return frame?.pageBuilderReady === true && frame.CKEDITOR.instances[host?.dataset.ckeditorInstance]?.status === 'ready';
    }, [], 20);

    try {
        I.switchTo(frameSelector);
        I.click('#editable-autotest');
        I.pressKey(['CommandOrControl', 'End']);
        I.type(' edited autotest');
        const result = await I.executeScript(() => {
            const pb = window.pageBuilder;
            const host = document.querySelector('#editable-autotest').closest('.editableElement');
            // A persisted editor name can collide with an unrelated new instance after reopening.
            document.querySelector('#stale-autotest').dataset.ckeditorInstance = host.dataset.ckeditorInstance;
            const before = pb.$wrapper.html();
            const html = window.getSaveData().editable.find(item => item.wjAppField === 'doc_data').data;
            return { html, unchanged: before === pb.$wrapper.html(), copied: pb.getSaveNode(host).text() };
        });
        assert.ok(result.unchanged, 'Serialization must not change the live document');
        assert.match(result.html, /Pôvodný obsah autotest/, 'A stale name must not replace its content with a different region');
        assert.match(result.html, /edited autotest/, 'Live CKEditor edits must be serialized');
        assert.match(result.copied, /edited autotest/, 'Exporting an editable root must read its own editor');
        assert.doesNotMatch(result.html, /data-ckeditor-instance|contenteditable|pb-toolbar|role="textbox"|aria-multiline|spellcheck|tabindex/);
        I.switchTo();
        DTE.save();
        saved = true;
        DT.filterEquals('title', title);
        I.click(locate('#datatableInit a').withText(title));
        DTE.waitForEditor();
        I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow?.pageBuilderReady === true, [], 20);
        I.switchTo(frameSelector);
        I.see('Nastavenia autotest', '#heading-autotest h1');
        I.see('Pôvodný obsah autotest', '#stale-autotest');
        I.see('edited autotest', '#wjInline-docdata');
        I.dontSeeElementInDOM('#heading-autotest .row, #heading-autotest .column-content');
        I.seeElementInDOM('#wjInline-docdata a[href="/apple"]');
        I.switchTo();
        DTE.cancel();
    } finally {
        I.switchTo();
        I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=' + groupId);
        DT.waitForLoader();
        if (saved) {
            DT.filterEquals('title', title);
            DT.deleteAll();
        }
        Document.resetPageBuilderMode();
    }
});
