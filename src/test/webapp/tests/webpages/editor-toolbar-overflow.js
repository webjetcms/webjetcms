const fs = require('fs');
const path = require('path');
const fixture = fs.readFileSync(path.join(__dirname, '../../fixtures/ckeditor-toolbar-overflow.html'), 'utf8');
const helper = fs.readFileSync(path.join(__dirname, '../../../../main/webapp/admin/v9/src/js/ckeditor-toolbar-overflow.js'), 'utf8');

Feature('webpages.editor-toolbar-overflow');

Before(async ({ I, login }) => {
    login('admin');
    await I.mockRoute('**/wjtoolbar-autotest.html*', route => route.fulfill({ contentType: 'text/html', body: fixture }));
    await I.mockRoute('**/wjtoolbar-helper-autotest.js', route => route.fulfill({ contentType: 'text/javascript', body: helper }));
});

function openFixture(I, mode, extra = '') {
    I.amOnPage(`/wjtoolbar-autotest.html?mode=${mode}${extra}`);
    I.waitForFunction(() => window.autotestReady === true, [], 10);
    I.waitForFunction(() => autotestToolbar.top.offsetHeight > 0 && !autotestToolbar.frame, [], 5);
}

function resizeToolbar(I, width) {
    I.executeScript(width => { document.getElementById('autotest-shell').style.width = `${width}px`; }, width);
    I.waitForFunction(width => !autotestToolbar.frame && Math.abs(autotestToolbar.toolbox.getBoundingClientRect().width - width) <= 2, [width], 5);
}

for (const mode of ['classic', 'inline']) {
    Scenario(`${mode}: groups fit one band, reserve the selector and restore their original nodes`, async ({ I }) => {
        openFixture(I, mode);
        const initial = await I.executeScript(() => ({ height: autotestToolbar.top.offsetHeight, data: autotestEditor.getData() }));
        for (const width of [576, 464, 320, 991, 1200]) {
            resizeToolbar(I, width);
            I.assertTrue(await I.executeScript(() => {
                const controller = autotestToolbar;
                const right = document.querySelector('.exit-inline-editor').getBoundingClientRect().left;
                const visible = controller.groups.filter(group => group.element.parentElement === controller.row);
                const tops = visible.map(group => group.element.getBoundingClientRect().top);
                return visible.every(group => group.element.getBoundingClientRect().right <= right) &&
                    (!tops.length || Math.max(...tops) - Math.min(...tops) < 1) &&
                    (controller.button.hidden || controller.button.getBoundingClientRect().right <= right);
            }), 'Visible groups and the overflow trigger must fit before the mode selector');
            I.assertEqual(await I.executeScript(() => autotestToolbar.top.offsetHeight), initial.height, 'Resizing must not add a second ribbon band');
        }
        I.dontSeeElement('.wj-cke-toolbar__more');
        I.assertTrue(await I.executeScript(() => autotestOriginalGroups.every((node, index) => autotestToolbar.row.children[index] === node)), 'Widening must restore the original controls in their original order');
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), initial.data, 'Layout changes must not change page HTML');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), []);
    });

    Scenario(`${mode}: formatting in overflow preserves selection, state and undo`, async ({ I }) => {
        openFixture(I, mode);
        resizeToolbar(I, 320);
        I.executeScript(() => autotestSelect());
        I.click('.wj-cke-toolbar__more');
        I.click('.wj-cke-overflow .cke_button__bold');
        I.waitForVisible('.wj-cke-overflow');
        I.assertEqual(await I.executeScript(() => autotestEditor.document.$.querySelector('strong').textContent), 'Autotest');
        I.seeElement('.wj-cke-overflow .cke_button__bold.cke_button_on');
        I.click('.wj-cke-overflow .cke_button__italic');
        I.assertEqual(await I.executeScript(() => autotestEditor.document.$.querySelector('em').textContent), 'Autotest');
        I.seeElement('.wj-cke-overflow .cke_button__italic.cke_button_on');
        I.executeScript(() => { autotestEditor.execCommand('undo'); autotestEditor.execCommand('undo'); });
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getText()), 'Autotest selected text.');
        I.assertFalse(await I.executeScript(() => /wj-cke|<strong>|<em>/.test(autotestEditor.getData())), 'Undo must restore text without serializing toolbar markup');
        I.click('#autotest-outside');
        I.dontSeeElement('.wj-cke-overflow');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), []);
    });

    Scenario(`${mode}: keyboard skips hidden groups and nested color panels retain the overflow`, async ({ I }) => {
        openFixture(I, mode);
        resizeToolbar(I, 576);
        I.executeScript(() => autotestEditor.execCommand('toolbarFocus'));
        const stops = await I.executeScript(() => autotestToolbar.row.children.length + 1);
        for (let step = 0; step < stops; step++) {
            I.assertTrue(await I.executeScript(() => document.activeElement.getClientRects().length > 0 && !document.activeElement.closest('.wj-cke-overflow')), 'Tab must never focus a control in the closed palette');
            I.pressKey('Tab');
        }
        resizeToolbar(I, 320);
        I.executeScript(() => { autotestSelect(); autotestEditor.execCommand('toolbarFocus'); });
        I.assertTrue(await I.executeScript(() => document.activeElement === autotestToolbar.button));
        I.pressKey('Enter');
        I.waitForVisible('.wj-cke-overflow');
        I.click('.wj-cke-overflow .cke_button__textcolor');
        I.waitForVisible('.cke_panel');
        I.assertTrue(await I.executeScript(() => {
            const panel = [...autotestToolbar.panels.keys()][0];
            return panel && Number(panel.element.$.style.zIndex) > Number(autotestToolbar.palette.style.zIndex);
        }), 'Native color panels must appear above the overflow');
        I.switchTo('.cke_panel iframe');
        I.pressKey('Escape');
        I.switchTo();
        I.seeElement('.wj-cke-overflow');
        I.pressKey('Escape');
        I.dontSeeElement('.wj-cke-overflow');
        I.assertTrue(await I.executeScript(() => document.activeElement === autotestToolbar.button));
        I.pressKey('Escape');
        I.assertTrue(await I.executeScript(() => autotestEditor.focusManager.hasFocus && document.activeElement !== autotestToolbar.button), 'Escape from the closed trigger must return to the editable content');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), []);
    });

    Scenario(`${mode}: style picker, dialogs and editor destruction clean up the palette`, async ({ I }) => {
        openFixture(I, mode, '&wjstyles=true');
        resizeToolbar(I, 320);
        I.executeScript(() => autotestSelect());
        I.click('.wj-cke-toolbar__more');
        I.seeElement('.wj-cke-overflow .cke_button__wjstyles_label');
        I.click('.wj-cke-overflow .cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.seeElement('.wj-cke-overflow');
        I.click('.wj-styles__format[data-format="h2"]');
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getName()), 'h2');
        I.pressKey('Escape');
        I.dontSeeElement('.wj-styles');
        I.seeElement('.wj-cke-overflow');
        I.click('.wj-cke-overflow .cke_button__link');
        I.waitForVisible('.cke_dialog');
        I.dontSeeElement('.wj-cke-overflow');
        I.click('.cke_dialog_ui_button_cancel');
        I.waitForInvisible('.cke_dialog');
        I.click('.wj-cke-toolbar__more');
        I.executeScript(() => autotestEditor.destroy());
        I.dontSeeElementInDOM('.wj-cke-overflow');
        I.dontSeeElementInDOM('.wj-cke-toolbar__more');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), []);
    });
}

Scenario('PageBuilder integration: mobile overflow preserves the mode selector and closes on block change', async ({ I, Document }) => {
    Document.resetPageBuilderMode();
    I.resizeWindow(1216, 958);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57&userlngr=true');
    I.waitForVisible('#DTE_Field_data-pageBuilderIframe', 20);
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.waitForVisible('.wj-cke-toolbar', 20);
    I.click('a[href*="pbSetWindowSize(\'phone\')"]');
    I.waitForVisible('.wj-cke-toolbar__more', 10);
    I.assertTrue(await I.executeScript(() => {
        const box = [...document.querySelectorAll('.wj-cke-toolbar')].find(node => node.getClientRects().length);
        const more = box.querySelector('.wj-cke-toolbar__more');
        const top = box.closest('.cke_top');
        const host = document.querySelector('#wjInlineCkEditorToolbarOffsetElement');
        return more.getBoundingClientRect().right <= document.querySelector('.exit-inline-editor').getBoundingClientRect().left &&
            Math.abs(host.offsetHeight - top.offsetHeight) < 2 && more.getAttribute('aria-label') === 'Ďalšie nástroje';
    }), 'PB must reserve the selector, translate the trigger and avoid a stale empty toolbar band');
    I.moveCursorTo('.wj-cke-toolbar__more:visible');
    I.waitForVisible('.wj-cke-toolbar-tooltip.show', 5);
    I.see('Ďalšie nástroje', '.wj-cke-toolbar-tooltip');
    I.assertTrue(await I.executeScript(() => {
        const more = document.querySelector('.wj-cke-toolbar__more:not([hidden])');
        const tooltip = document.getElementById(more.getAttribute('aria-describedby'));
        const lastGroup = more.previousElementSibling.lastElementChild;
        const gap = more.getBoundingClientRect().left - lastGroup.getBoundingClientRect().right;
        return !more.hasAttribute('title') && tooltip.ownerDocument === more.ownerDocument &&
            tooltip.getBoundingClientRect().top >= more.getBoundingClientRect().bottom && gap >= 0 && gap <= 8;
    }), 'The WJ tooltip must appear below the trigger in the PB frame, next to the last group');
    I.saveScreenshot('wj-pb-toolbar-tooltip.png');
    const tooltipId = await I.grabAttributeFrom('.wj-cke-toolbar__more:visible', 'aria-describedby');
    I.moveCursorTo(`#${tooltipId}`);
    I.seeElement(`#${tooltipId}.show`);
    I.moveCursorTo('.exit-inline-editor');
    I.waitForInvisible('.wj-cke-toolbar-tooltip.show', 5);
    I.saveScreenshot('wj-pb-toolbar-after-closed.png');
    I.executeScript(() => document.querySelector('.wj-cke-toolbar__more:not([hidden])').focus());
    I.waitForVisible('.wj-cke-toolbar-tooltip.show', 5);
    I.click('.wj-cke-toolbar__more:visible');
    I.waitForVisible('.wj-cke-overflow');
    I.dontSeeElement('.wj-cke-toolbar-tooltip.show');
    I.assertTrue(await I.executeScript(() => {
        const palette = [...document.querySelectorAll('.wj-cke-overflow')].find(node => !node.hidden);
        const rect = palette.getBoundingClientRect();
        return rect.left >= 0 && rect.right <= innerWidth && rect.bottom <= innerHeight;
    }), 'The palette must stay inside the mobile preview');
    I.saveScreenshot('wj-pb-toolbar-after-open.png');
    I.pressKey('Escape');
    I.dontSeeElement('.wj-cke-overflow');
    I.dontSeeElement('.wj-cke-toolbar-tooltip.show');
    I.pressKey('Tab');
    I.pressKey('Shift+Tab');
    I.waitForVisible('.wj-cke-toolbar-tooltip.show', 5);
    I.pressKey('Enter');
    I.waitForVisible('.wj-cke-overflow');
    I.executeScript(() => {
        const previous = CKEDITOR.currentInstance;
        Object.values(CKEDITOR.instances).find(editor => editor !== previous).focus();
    });
    I.dontSeeElement('.wj-cke-overflow');
    I.click('a[href*="pbSetWindowSize(\'desktop\')"]');
    I.waitForInvisible('.wj-cke-toolbar__more', 10);
    I.switchTo();
});

Scenario('Classic integration: maximized editor keeps content below the toolbar', async ({ I, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=100605');
    DTE.waitForEditor();
    I.waitForVisible('.wj-cke-toolbar', 10);
    const original = await I.executeScript(() => CKEDITOR.instances.data.getData());

    for (const [width, height] of [[1303, 752], [1303, 600], [1100, 958], [760, 958], [600, 600], [400, 600], [1280, 958]]) {
        I.resizeWindow(width, height);
        I.waitForFunction(() => !CKEDITOR.instances.data.wjToolbarOverflow.frame, [], 5);
        const layout = await I.executeScript(() => {
            const controller = CKEDITOR.instances.data.wjToolbarOverflow;
            const controls = [...controller.row.children, controller.selector, controller.button]
                .filter(element => element.getClientRects().length);
            return {
                contentTop: document.querySelector('#cke_data .cke_wysiwyg_frame').getBoundingClientRect().top,
                toolbarBottom: controller.top.getBoundingClientRect().bottom,
                controlsBottom: Math.max(...controls.map(element => element.getBoundingClientRect().bottom))
            };
        });
        I.assertTrue(layout.toolbarBottom >= layout.controlsBottom, `Toolbar background must contain its controls at ${width}x${height}`);
        I.assertTrue(layout.contentTop >= layout.toolbarBottom, `Page content must start below the toolbar at ${width}x${height}`);
        if (width === 1303 && height === 752) I.saveScreenshot('editor-toolbar-overlap-after.png');
    }

    I.assertEqual(await I.executeScript(() => CKEDITOR.instances.data.getData()), original, 'Responsive layout must leave page HTML unchanged');
    DTE.cancel();
});

Scenario('Classic integration: administration resizing keeps all commands reachable', async ({ I, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=21');
    DTE.waitForLoader();
    I.waitForVisible('.cke_wysiwyg_frame', 20);
    I.waitForVisible('.wj-cke-toolbar', 10);
    const original = await I.executeScript(() => CKEDITOR.instances.data.getData());
    for (const width of [760, 600, 400]) {
        I.resizeWindow(width, 958);
        I.waitForVisible('.wj-cke-toolbar__more', 10);
        I.assertTrue(await I.executeScript(() => {
            const controller = CKEDITOR.instances.data.wjToolbarOverflow;
            const right = controller.selector.getBoundingClientRect().left;
            return controller.button.getBoundingClientRect().right <= right &&
                controller.groups.filter(group => group.element.parentElement === controller.row)
                    .every(group => group.element.getBoundingClientRect().right <= right);
        }), 'Classic toolbar controls must fit before the mode selector');
    }
    I.click('.wj-cke-toolbar__more:visible');
    I.waitForVisible('.wj-cke-overflow');
    I.saveScreenshot('wj-classic-toolbar-after-open.png');
    I.seeElement('.wj-cke-overflow .cke_button__wjstyles_label');
    I.pressKey('Escape');
    I.dontSeeElement('.wj-cke-overflow');
    I.dontSeeElement('.wj-cke-toolbar-tooltip.show');
    I.assertTrue(await I.executeScript(() => document.activeElement === CKEDITOR.instances.data.wjToolbarOverflow.button), 'Closing the palette must restore focus without opening a tooltip');
    I.resizeWindow(1280, 958);
    I.waitForInvisible('.wj-cke-toolbar__more', 10);
    I.assertEqual(await I.executeScript(() => CKEDITOR.instances.data.getData()), original, 'Resizing and opening the palette must leave page HTML unchanged');
    DTE.cancel();
    I.dontSeeElement('.wj-cke-overflow');
});

for (const restore of ['close', 'minimize']) {
    Scenario(`PageBuilder preview: desktop width recovers after dialog ${restore}`, async ({ I }) => {
        I.resizeWindow(1303, 958);
        I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57');
        I.waitForVisible('#DTE_Field_data-pageBuilderIframe', 20);
        I.switchTo('#DTE_Field_data-pageBuilderIframe');
        I.waitForVisible('.wj-cke-toolbar', 20);
        I.click("a[href*=\"pbSetWindowSize('tablet')\"]");
        I.switchTo();
        I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe').clientWidth === 991, [], 5);
        I.switchTo('#DTE_Field_data-pageBuilderIframe');
        I.click(locate('[contenteditable="true"] h3').first());
        I.click('.cke_button__link:visible');
        I.waitForVisible('.cke_dialog');
        I.dontSeeElement('.cke_dialog_maximized');
        I.click('.cke_dialog_maximize_button');
        I.switchTo();
        I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe').clientWidth === window.innerWidth, [], 5);
        I.switchTo('#DTE_Field_data-pageBuilderIframe');
        if (restore === 'minimize') I.click('.cke_dialog_minimize_button');
        I.click('.cke_dialog_ui_button_cancel');
        I.waitForInvisible('.cke_dialog');
        I.switchTo();
        I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe').clientWidth === 991, [], 5);
        I.switchTo('#DTE_Field_data-pageBuilderIframe');
        I.click("a[href*=\"pbSetWindowSize('phone')\"]");
        I.switchTo();
        I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe').clientWidth === 576, [], 5);
        I.switchTo('#DTE_Field_data-pageBuilderIframe');
        I.click("a[href*=\"pbSetWindowSize('desktop')\"]");
        I.switchTo();
        I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe').clientWidth > 1000, [], 5);
        I.saveScreenshot(`wj-pb-width-after-dialog-${restore}.png`);
    });
}

for (const [command, close] of [['image', 'cancel'], ['link', 'close'], ['components', 'Escape'], ['link', 'ok']]) {
    Scenario(`PageBuilder preview: oversized ${command} dialog restores mobile preview after ${close}`, async ({ I, DTE }) => {
        I.resizeWindow(1303, 958);
        I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57');
        I.waitForVisible('#DTE_Field_data-pageBuilderIframe', 20);
        I.switchTo('#DTE_Field_data-pageBuilderIframe');
        I.waitForVisible('.wj-cke-toolbar', 20);
        I.click("a[href*=\"pbSetWindowSize('phone')\"]");
        I.click(locate('[contenteditable="true"] h3').withText('intranetové riešenie'));
        const original = await I.executeScript(() => {
            const editor = CKEDITOR.currentInstance;
            const range = editor.createRange();
            range.selectNodeContents(editor.editable().findOne('h3'));
            editor.getSelection().selectRanges([range]);
            window.scrollTo({top: 200, behavior: 'instant'});
            return {html: editor.getData(), text: editor.getSelection().getSelectedText(), scroll: window.scrollY};
        });
        for (let attempt = 0; attempt < 2; attempt++) {
            I.click('.wj-cke-toolbar__more:visible');
            I.click(`.wj-cke-overflow .cke_button__${command}`);
            I.waitForVisible('.cke_dialog_maximized', 10);
            if (command === 'image') I.waitForVisible('.cke_dialog_tabs a[title="Informácie o obrázku"]', 5);
            I.dontSeeElement('.cke_dialog_minimize_button');
            I.dontSeeElement('.wj-cke-overflow');
            I.assertTrue(await I.executeScript(() => {
                const dialog = document.querySelector('.cke_dialog_maximized').getBoundingClientRect();
                return window.frameElement.clientWidth === window.top.innerWidth && dialog.left >= 0 &&
                    dialog.right <= innerWidth && dialog.top >= 0 && dialog.bottom <= innerHeight;
            }), 'The complete dialog, including its actions, must fit the expanded preview');
            if (attempt === 0) I.saveScreenshot(`wj-pb-dialog-auto-${command}-${close}.png`);
            if (close === 'ok') {
                I.switchTo('#wjLinkIframe');
                I.waitForVisible('#txtUrl', 10);
                I.fillField('#txtUrl', '/autotest-dialog-width');
                I.switchTo();
                I.switchTo('#DTE_Field_data-pageBuilderIframe');
                I.click('.cke_dialog_ui_button_ok');
            } else if (close === 'Escape') {
                I.pressKey('Escape');
            } else {
                I.click(close === 'cancel' ? '.cke_dialog_ui_button_cancel' : '.cke_dialog_close_button');
            }
            I.waitForInvisible('.cke_dialog', 10);
            I.switchTo();
            I.waitForFunction(scroll => {
                const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe');
                return frame.clientWidth === 576 && Math.abs(frame.contentWindow.scrollY - scroll) <= 1;
            }, [original.scroll], 5);
            I.switchTo('#DTE_Field_data-pageBuilderIframe');
            if (close === 'ok') {
                I.assertEqual(await I.executeScript(() => CKEDITOR.currentInstance.editable().findOne('a[href="/autotest-dialog-width"]').getText()), original.text, 'The link must wrap the original selection');
                I.executeScript(() => CKEDITOR.currentInstance.execCommand('undo'));
            }
            I.assertEqual(await I.executeScript(() => CKEDITOR.currentInstance.getData()), original.html, 'Opening and closing a dialog must preserve page content');
        }
        I.click("a[href*=\"pbSetWindowSize('desktop')\"]");
        I.switchTo();
        I.waitForFunction(() => document.querySelector('#DTE_Field_data-pageBuilderIframe').clientWidth > 1000, [], 5);
        if (command === 'image') {
            I.switchTo('#DTE_Field_data-pageBuilderIframe');
            I.click('.cke_button__image:visible');
            I.waitForVisible('.cke_dialog');
            I.dontSeeElement('.cke_dialog_maximized');
            I.saveScreenshot('wj-pb-dialog-desktop-image.png');
            I.click('.cke_dialog_ui_button_cancel');
            I.switchTo();
        }
        DTE.cancel();
    });
}
