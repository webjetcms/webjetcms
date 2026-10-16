const fs = require('fs');
const path = require('path');
const fixture = fs.readFileSync(path.join(__dirname, '../../fixtures/wjstylescombo.html'), 'utf8');

Feature('webpages.editor-styles');

Before(async ({ I, login }) => {
    login('admin');
    await I.mockRoute('**/wjstyles-autotest.html*', route => route.fulfill({ contentType: 'text/html', body: fixture }));
});

for (const mode of ['classic', 'inline']) {
    Scenario(`${mode}: reopen the panel after closing`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        // Keep the content click target beside the open panel rather than underneath it.
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" style="width:80px">Autotest text</p>', resolve)));
        I.executeScript(() => autotestSelect('one'));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        I.click('.wj-styles__close');
        I.waitToHide('.wj-styles');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        I.pressKey('Escape');
        I.waitToHide('.wj-styles');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles', 5);
        I.click('.cke_button__wjstyles');
        I.waitToHide('.wj-styles');
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
