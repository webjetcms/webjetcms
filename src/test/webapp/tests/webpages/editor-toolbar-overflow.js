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

Scenario('PageBuilder integration: mobile overflow preserves the mode selector and closes on block change', async ({ I }) => {
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
            Math.abs(host.offsetHeight - top.offsetHeight) < 2 && more.title === 'Ďalšie nástroje';
    }), 'PB must reserve the selector, translate the trigger and avoid a stale empty toolbar band');
    I.saveScreenshot('wj-pb-toolbar-after-closed.png');
    I.click('.wj-cke-toolbar__more:visible');
    I.waitForVisible('.wj-cke-overflow');
    I.assertTrue(await I.executeScript(() => {
        const palette = [...document.querySelectorAll('.wj-cke-overflow')].find(node => !node.hidden);
        const rect = palette.getBoundingClientRect();
        return rect.left >= 0 && rect.right <= innerWidth && rect.bottom <= innerHeight;
    }), 'The palette must stay inside the mobile preview');
    I.saveScreenshot('wj-pb-toolbar-after-open.png');
    I.executeScript(() => {
        const previous = CKEDITOR.currentInstance;
        Object.values(CKEDITOR.instances).find(editor => editor !== previous).focus();
    });
    I.dontSeeElement('.wj-cke-overflow');
    I.click('a[href*="pbSetWindowSize(\'desktop\')"]');
    I.waitForInvisible('.wj-cke-toolbar__more', 10);
    I.switchTo();
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
    I.resizeWindow(1280, 958);
    I.waitForInvisible('.wj-cke-toolbar__more', 10);
    I.assertEqual(await I.executeScript(() => CKEDITOR.instances.data.getData()), original, 'Resizing and opening the palette must leave page HTML unchanged');
    DTE.cancel();
    I.dontSeeElement('.wj-cke-overflow');
});
