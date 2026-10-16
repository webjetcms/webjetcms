const fs = require('fs');
const path = require('path');
const fixture = fs.readFileSync(path.join(__dirname, '../../fixtures/wjstylescombo.html'), 'utf8');

Feature('webpages.editor-styles');

Before(async ({ I, login }) => {
    login('admin');
    await I.mockRoute('**/wjstyles-autotest.html*', route => route.fulfill({ contentType: 'text/html', body: fixture }));
});

for (const mode of ['classic', 'inline']) {
    Scenario(`${mode}: non-interactive panel content keeps the picker open`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}${mode === 'classic' ? '&modal=1' : ''}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="custom" style="width:80px">Autotest text</p>', resolve)));
        I.executeScript(() => autotestSelect('one'));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        for (const target of ['.wj-styles__chips .wj-styles__empty', '.wj-styles__top strong', '.wj-styles__status']) {
            I.click(target);
            I.seeElement('.wj-styles:not([hidden])');
            I.assertTrue(await I.executeScript(() => document.querySelector('.wj-styles').contains(document.activeElement)), 'Clicking panel text must keep focus inside the picker');
        }
        I.click('.wj-styles__toggle-all');
        // With categories collapsed, the list's center is empty panel space.
        I.click('.wj-styles__list');
        I.seeElement('.wj-styles:not([hidden])');
        I.assertTrue(await I.executeScript(() => document.querySelector('.wj-styles').contains(document.activeElement)), 'Clicking empty panel space must keep focus inside the picker');
        I.click('All styles', '.wj-styles__filters');
        I.seeCheckboxIsChecked('.wj-styles__filters input');
        I.fillField('.wj-styles__search', 'large');
        I.click('.wj-styles__item[data-style-id="1"]');
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'custom large', 'Panel background clicks must preserve the target selection');
        I.click('.wj-styles__close');
        I.waitToHide('.wj-styles');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        if (mode === 'classic') I.switchTo('.cke_wysiwyg_frame');
        I.click('#one');
        if (mode === 'classic') I.switchTo();
        I.waitToHide('.wj-styles');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Panel background clicks must not produce browser errors');
    });

    Scenario(`${mode}: category accordions preserve selection and reveal search results`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one">Autotest text</p>', resolve)));
        I.executeScript(() => autotestSelect('one'));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.dontSeeElement('.wj-styles__filters select');
        I.see('Collapse all', '.wj-styles__toggle-all');
        I.dontSeeElement('.wj-styles__group-toggle[aria-expanded="false"]');
        I.seeElementInDOM('.wj-styles__group:first-child .wj-styles__group-toggle > .ti-chevron-down:first-child');
        I.seeElementInDOM('.wj-styles__group:first-child .ti-typography');
        I.seeElementInDOM('.wj-styles__group:nth-child(2) .ti-palette');
        I.seeElement('.wj-styles__item--exclusive[data-style-id="0"]');
        I.click('.wj-styles__item[data-style-id="0"]');
        const typography = '.wj-styles__group:first-child .wj-styles__group-toggle';
        I.click(typography);
        I.see('Expand all', '.wj-styles__toggle-all');
        I.dontSeeElement('.wj-styles__item[data-style-id="0"]');
        I.assertTrue(await I.executeScript(() => {
            const groups = document.querySelectorAll('.wj-styles__group');
            return Math.abs(groups[0].getBoundingClientRect().bottom - groups[1].getBoundingClientRect().top) < 1;
        }), 'Editor section markers must not add gaps between accordion headers');
        I.pressKey('ArrowDown');
        I.assertTrue(await I.executeScript(() => document.activeElement === document.querySelector('.wj-styles__group:nth-child(2) .wj-styles__group-toggle')), 'Arrow navigation must skip collapsed choices');
        I.click('.wj-styles__item[data-style-id="4"]');
        I.assertEqual(await I.grabAttributeFrom(typography, 'aria-expanded'), 'false', 'Applying another style must preserve collapsed categories');
        I.see('Small', '.wj-styles__chips');
        I.fillField('.wj-styles__search', 'small');
        I.seeElement('.wj-styles__item[data-style-id="0"]');
        I.assertEqual(await I.grabAttributeFrom('.wj-styles__item[data-style-id="0"]', 'aria-pressed'), 'true', 'Searching must preserve the active choice');
        I.click('.wj-styles__toggle-all');
        I.dontSeeElement('.wj-styles__item[data-style-id="0"]');
        I.checkOption('.wj-styles__filters input');
        I.dontSeeElement('.wj-styles__item[data-style-id="0"]');
        I.click('.wj-styles__toggle-all');
        I.seeElement('.wj-styles__item[data-style-id="0"]');
        I.uncheckOption('.wj-styles__filters input');
        I.fillField('.wj-styles__search', '');
        I.assertEqual(await I.grabAttributeFrom(typography, 'aria-expanded'), 'false', 'Clearing search must restore collapsed categories');
        const content = await I.executeScript(() => autotestEditor.getData());
        I.click('.wj-styles__toggle-all');
        I.dontSeeElement('.wj-styles__group-toggle[aria-expanded="false"]');
        I.seeElement('.wj-styles__item[data-style-id="0"]');
        I.see('Collapse all', '.wj-styles__toggle-all');
        I.pressKey('Enter');
        I.dontSeeElement('.wj-styles__group-toggle[aria-expanded="true"]');
        I.dontSeeElement('.wj-styles__item');
        I.click(typography);
        I.see('Expand all', '.wj-styles__toggle-all');
        I.click('.wj-styles__toggle-all');
        I.dontSeeElement('.wj-styles__group-toggle[aria-expanded="false"]');
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), content, 'Accordion controls must not change editor content');
        I.fillField('.wj-styles__search', 'autotest-no-matching-style');
        I.seeElement('.wj-styles__toggle-all:disabled');
        I.fillField('.wj-styles__search', '');
        I.seeElement('.wj-styles__toggle-all:not(:disabled)');
        I.checkOption('.wj-styles__filters input');
        I.seeElement('.wj-styles__item[data-style-id="10"]:not(.wj-styles__item--exclusive)');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Accordion interactions must not produce browser errors');
    });

    Scenario(`${mode}: reopen the panel after closing`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        // Keep the content click target beside the open panel rather than underneath it.
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="small" style="width:80px">Autotest text</p>', resolve)));
        I.executeScript(() => autotestSelect('one'));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        I.click('.wj-styles__close');
        I.waitToHide('.wj-styles');
        I.dontSeeElement('.wj-styles-summary');
        I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains('cke_button__wjstyles')), 'Closing must retain toolbar focus without displaying its tooltip');
        I.pressKey('ArrowLeft');
        I.pressKey('ArrowRight');
        I.see('Small', '.wj-styles-summary');
        I.pressKey('ArrowLeft');
        I.dontSeeElement('.wj-styles-summary');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        I.pressKey('Escape');
        I.waitToHide('.wj-styles');
        I.dontSeeElement('.wj-styles-summary');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        I.click('.cke_button__wjstyles');
        I.waitToHide('.wj-styles');
        I.dontSeeElement('.wj-styles-summary');
        I.moveCursorTo('.cke_combo__format');
        I.dontSeeElement('.wj-styles-summary');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        if (mode === 'classic') I.switchTo('.cke_wysiwyg_frame');
        I.click('#one');
        if (mode === 'classic') I.switchTo();
        I.waitToHide('.wj-styles');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        I.assertEqual(await I.grabAttributeFrom('.cke_button__wjstyles', 'aria-expanded'), 'true');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Reopening the picker must not produce browser errors');
    });

    Scenario(`${mode}: compose, replace, toggle, mixed selection and undo`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<h3 id="heading">Heading</h3><p id="one" class="custom small">First text</p><p id="two" class="large">Second text</p>', resolve)));
        I.executeScript(() => { autotestSelect('one'); autotestApply('Large'); autotestApply('Bold'); });
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'custom large bold', 'Unrelated classes and independent groups must compose');
        I.executeScript(() => autotestApply('Large'));
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'custom bold', 'An active choice must toggle off');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'custom large bold', 'One undo must restore the previous style');
        I.executeScript(() => { autotestSelect('heading'); autotestApply('Heading appearance'); });
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('heading').getName()), 'h3', 'CSS appearance must preserve the heading tag');
        I.executeScript(() => { autotestSelect('one', 'two'); autotestApply('Small'); });
        I.assertDeepEqual(await I.executeScript(() => ['one','two'].map(id => autotestEditor.document.getById(id).getAttribute('class'))), ['custom bold small','small'], 'Every selected paragraph must receive the exclusive choice');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'The picker must not produce browser errors');
    });

    Scenario(`${mode}: inline selection preserves surrounding text and markup`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one"><span class="red custom" id="red">Before selected after</span></p>', resolve)));
        I.executeScript(() => { autotestSelect('red',null,7,15); autotestApply('Blue'); });
        const result = await I.executeScript(() => {
            const p=autotestEditor.document.getById('one').$;
            const blue=p.querySelector('.blue');
            return {text:p.textContent, blue:blue?.textContent, nestedRed:!!blue?.closest('.red'), surrounding:Array.from(p.querySelectorAll('.red')).map(e=>e.textContent).join('|'), custom:!!blue?.closest('.custom')};
        });
        I.assertDeepEqual(result,{text:'Before selected after',blue:'selected',nestedRed:false,surrounding:'Before | after',custom:true},'Only the selected text may change color');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('red').getText()), 'Before selected after', 'Undo must restore the original inline element');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), []);
    });

    Scenario(`${mode}: panel stays open, search and keyboard work`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one">Autotest text</p>', resolve)));
        I.executeScript(() => autotestSelect('one'));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.click('.wj-styles__item[data-style-id="1"]');
        I.seeElement('.wj-styles:not([hidden])');
        I.see('Large', '.wj-styles__chips');
        I.assertTrue(await I.executeScript(() => {const p=document.querySelector('.wj-styles'),h=p.querySelector('.wj-styles__header');return h.getBoundingClientRect().height > 170;}), 'Search and active styles must stay visible above the scrolling list');
        I.fillField('.wj-styles__search', 'cervena');
        I.see('Červená', '.wj-styles__list');
        I.dontSee('Small', '.wj-styles__list');
        I.see('Large', '.wj-styles__chips');
        I.pressKey('Escape');
        I.dontSeeElement('.wj-styles');
        I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains('cke_button__wjstyles')), 'Escape must restore toolbar focus');
        I.assertContain(await I.grabAttributeFrom('.cke_button__wjstyles','title'), 'Large');
        for (const width of [1405,1100,767]) {
            I.resizeWindow(width,900);
            I.click('.cke_button__wjstyles');
            I.waitForVisible('.wj-styles');
            I.assertTrue(await I.executeScript(() => {
                const r=document.querySelector('.wj-styles').getBoundingClientRect();
                return r.left>=0 && r.right<=innerWidth && r.bottom<=innerHeight;
            }), 'The picker must fit the viewport');
            I.pressKey('Escape');
        }
    });
}

Scenario('inline: explicit ancestor target preserves content and supports undo', async ({ I }) => {
    I.amOnPage('/wjstyles-autotest.html?mode=inline');
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
    I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="small">Autotest text</p>', resolve)));
    I.executeScript(() => { autotestSelect('one'); autotestEditor.wjstylescombo.open(); });
    I.selectOption('.wj-styles__target select', 'Section (section).structural.bg-light');
    I.click('.wj-styles__item[data-style-id="7"]');
    I.assertEqual(await I.executeScript(() => document.getElementById('autotest-section').className), 'structural bg-dark', 'Background replacement must preserve structural classes');
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'small');
    I.pressKey('Escape');
    I.executeScript(() => autotestEditor.execCommand('undo'));
    I.assertEqual(await I.executeScript(() => document.getElementById('autotest-section').className), 'structural bg-light', 'Undo must restore the ancestor background');
    I.executeScript(() => autotestEditor.execCommand('redo'));
    I.assertEqual(await I.executeScript(() => document.getElementById('autotest-section').className), 'structural bg-dark');
    I.assertNotContain(await I.executeScript(() => autotestEditor.getData()), 'wjstyles:', 'Undo metadata must never appear in saved HTML');
});

Scenario('component reset preserves base and unknown classes in one undo step', async ({ I }) => {
    I.amOnPage('/wjstyles-autotest.html?mode=classic');
    I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
    I.executeScript(() => new Promise(resolve => autotestEditor.setData('<div id="alert" class="alert info custom">Autotest alert</div>', resolve)));
    I.executeScript(() => { autotestSelect('alert'); autotestApply('Alert warning'); });
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('alert').getAttribute('class')), 'alert custom warning');
    I.executeScript(() => autotestEditor.wjstylescombo.resetComponent());
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('alert').getAttribute('class')), 'alert custom');
    I.executeScript(() => autotestEditor.execCommand('undo'));
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('alert').getAttribute('class')), 'alert custom warning');
});

Scenario('mixed state, selected objects and read-only mode', async ({ I }) => {
    I.amOnPage('/wjstyles-autotest.html?mode=classic');
    I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
    I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="small">First</p><p id="two">Second</p><p><a id="link" href="#">Link</a><img id="image" alt="Autotest" src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"></p><table><tbody><tr><td id="cell">Cell</td></tr></tbody></table>', resolve)));
    I.executeScript(() => autotestSelect('one','two'));
    I.assertEqual(await I.executeScript(() => { const p=autotestEditor.wjstylescombo; return p.state(p.items.find(i=>i.label==='Small')); }), 'mixed');
    I.executeScript(() => { autotestApply('Small'); autotestSelect('link'); autotestApply('Link'); autotestSelect('image'); autotestApply('Image'); autotestSelect('cell'); autotestApply('Cell'); });
    I.assertDeepEqual(await I.executeScript(() => ['one','two','link','image','cell'].map(id=>autotestEditor.document.getById(id).getAttribute('class'))),['small','small','link-style','image-style','cell-style']);
    I.executeScript(() => { autotestSelect('one'); autotestEditor.setReadOnly(true); autotestApply('Large'); });
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'small');
    I.assertDeepEqual(await I.executeScript(() => autotestErrors), []);
});

Scenario('a caret style applies to subsequent typing without storing a filler', async ({ I }) => {
    I.amOnPage('/wjstyles-autotest.html?mode=classic');
    I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
    I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one">Autotest</p>', resolve)));
    I.executeScript(() => { autotestSelect('one',null,4,4); autotestApply('Blue'); autotestEditor.insertText('typed'); });
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').$.querySelector('.blue')?.textContent.replace(/\u200b/g,'')), 'typed');
    I.assertNotContain(await I.executeScript(() => autotestEditor.getData()), '\u200b');
});

Scenario('classic: host modal retains panel focus after opening and applying', async ({ I }) => {
    I.amOnPage('/wjstyles-autotest.html?mode=classic&modal=1');
    I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
    I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one">Autotest text</p>', resolve)));
    I.executeScript(() => autotestSelect('one'));
    I.click('.cke_button__wjstyles');
    I.executeScript(() => {
        autotestEditor.addCommand('showFloatingTools', {exec: function() {}});
        autotestEditor.execCommand('showFloatingTools');
    });
    I.fillField('.wj-styles__search', 'Large');
    I.click('.wj-styles__item[data-style-id="1"]');
    I.seeElement('.wj-styles:not([hidden])');
    I.assertTrue(await I.executeScript(() => document.querySelector('.wj-styles').contains(document.activeElement)), 'The host modal must allow focus inside the style panel');
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'large');
    I.pressKey('Escape');
    I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains('cke_button__wjstyles')));
});
