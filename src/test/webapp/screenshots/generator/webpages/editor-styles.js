Feature('webpages.editor-styles');

Before(({ login }) => login('admin'));

Scenario('style picker and paragraph formats', async ({ I, Document }) => {
    if (!Document.isScreenshotsEnabled()) return;

    // Use a page whose template provides typography, spacing and alert metadata.
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
        const styles = await I.executeScript(() => {
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
            const alert = picker.items.find(item => item.classes.includes('alert-warning') && picker.targets(item).length);
            if (!typography || !spacing || !alert) throw new Error('The screenshot template must provide p.lead, mb-4 and paragraph alert styles');
            return { typographyId: typography.id, spacingId: spacing.id, spacingCategory: spacing.category, alertCategory: alert.category };
        });
        I.waitForVisible('.wj-styles:not([hidden])');
        I.click('.wj-styles__toggle-all');
        I.click(locate('.wj-styles__group-toggle').withText(styles.spacingCategory));
        I.click(locate('.wj-styles__group-toggle').withText(styles.alertCategory));
        I.executeScript(() => document.fonts.ready.then(() => true));
        I.executeScript(() => {
            const list = CKEDITOR.instances.data.wjstylescombo.list;
            list.scrollTop = list.scrollHeight;
        });
        I.moveCursorTo('.wj-styles__top');
        Document.screenshot('/redactor/webpages/working-in-editor/roletka.png');

        I.click(locate('.wj-styles__group-toggle').withText(styles.spacingCategory));
        I.click(locate('.wj-styles__group-toggle').withText(styles.alertCategory));
        I.executeScript(styles => {
            const editor = CKEDITOR.instances.data, picker = editor.wjstylescombo;
            const paragraph = picker.targets(picker.items.find(item => item.id === styles.typographyId))[0];
            // Restore the paragraph selection after interacting with the panel.
            editor.focus();
            const range = editor.createRange();
            range.selectNodeContents(new CKEDITOR.dom.element(paragraph));
            range.collapse(true);
            editor.getSelection().selectRanges([range]);
            picker.capture();
            for (const id of [styles.typographyId, styles.spacingId]) {
                const item = picker.items.find(item => item.id === id);
                if (picker.state(item) !== 'true') picker.apply(item);
            }
        }, styles);
        I.click('.wj-styles__formats .wj-styles__group-toggle');
        I.executeScript(() => {
            const picker = CKEDITOR.instances.data.wjstylescombo;
            picker.list.scrollTop = 0;
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
