Feature('webpages.editor-styles');

Before(({ login }) => login('admin'));

Scenario('style picker and paragraph formats', async ({ I, Document }) => {
    if (!Document.isScreenshotsEnabled()) return;

    // Use a page whose template provides typography and spacing metadata.
    const docId = process.env.CODECEPT_STYLES_DOCID || '57';
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
    I.resizeWindow(1202, 1231);
    I.waitForVisible('#datatableInit_modal.DTED.show[data-dte-focus-state="ready"]', 30);
    const originalMode = await I.executeScript(() => window.editorTypeForced || '');
    let originalData;
    try {
        I.executeScript(() => window.switchEditorType({ value: '' }));
        I.waitForFunction(() => CKEDITOR.instances.data?.status === 'ready' && CKEDITOR.instances.data.mode === 'wysiwyg' && CKEDITOR.instances.data.wjstylescombo?.items.length > 0, [], 30);
        originalData = await I.executeScript(() => CKEDITOR.instances.data.getData());
        const category = await I.executeScript(() => {
            const editor = CKEDITOR.instances.data, picker = editor.wjstylescombo;
            const paragraphs = Array.from(editor.editable().$.querySelectorAll('p')).filter(element => element.textContent.trim());
            const paragraph = paragraphs.find(element => element.closest('.container')) || paragraphs[0];
            if (!paragraph) throw new Error('The screenshot page must contain a paragraph');
            editor.focus();
            const range = editor.createRange();
            range.selectNodeContents(new CKEDITOR.dom.element(paragraph));
            range.collapse(true);
            editor.getSelection().selectRanges([range]);
            picker.open();
            const typography = picker.items.find(item => item.classes.includes('lead') && picker.targets(item).length);
            const spacing = picker.items.find(item => item.classes.includes('mb-4') && picker.targets(item).length);
            if (!typography || !spacing) throw new Error('The screenshot template must provide p.lead and mb-4 styles');
            for (const item of [typography, spacing]) if (picker.state(item) !== 'true') picker.apply(item);
            return typography.category;
        });
        I.waitForVisible('.wj-styles:not([hidden])');
        I.click('.wj-styles__toggle-all');
        I.click(locate('.wj-styles__group-toggle').withText(category));
        I.executeScript(category => {
            const picker = CKEDITOR.instances.data.wjstylescombo;
            const group = Array.from(picker.list.querySelectorAll('.wj-styles__group')).find(group => group.querySelector('.wj-styles__group-name').textContent === category);
            picker.list.scrollTop += group.getBoundingClientRect().top - picker.list.getBoundingClientRect().top;
        }, category);
        I.executeScript(() => document.fonts.ready.then(() => true));
        I.moveCursorTo('.wj-styles__top');
        Document.screenshotElement('.wj-styles:not([hidden])', '/redactor/webpages/working-in-editor/roletka.png');

        I.click(locate('.wj-styles__group-toggle').withText(category));
        I.click('.wj-styles__formats .wj-styles__group-toggle');
        I.executeScript(() => {
            const picker = CKEDITOR.instances.data.wjstylescombo;
            picker.list.scrollTop = picker.list.scrollHeight;
        });
        I.seeElement('.wj-styles__format[data-format="p"][aria-pressed="true"]');
        I.seeElement('.wj-styles__format[data-format="h6"]:not(:disabled)');
        I.moveCursorTo('.wj-styles__top');
        Document.screenshotElement('.wj-styles:not([hidden])', '/redactor/webpages/working-in-editor/format-odseku.png');
    } finally {
        if (originalData !== undefined) {
            I.executeScript(data => new Promise(resolve => {
                const editor = CKEDITOR.instances.data;
                editor.wjstylescombo.close(false);
                editor.setData(data, resolve);
            }), originalData);
            I.assertEqual(await I.executeScript(() => CKEDITOR.instances.data.getData()), originalData, 'Screenshots must not change the article content');
        }
        I.executeScript(mode => {
            if ((window.editorTypeForced || '') !== mode) window.switchEditorType({ value: mode });
        }, originalMode);
    }
});
