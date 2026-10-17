const fs = require('fs');
const path = require('path');
const fixture = fs.readFileSync(path.join(__dirname, '../../fixtures/wjstylescombo.html'), 'utf8');

Feature('webpages.editor-styles');

Before(async ({ I, login }) => {
    login('admin');
    await I.mockRoute('**/wjstyles-autotest.html*', route => route.fulfill({ contentType: 'text/html', body: fixture }));
});

for (const mode of ['classic', 'inline']) {
    Scenario(`${mode}: toolbar summary identifies the current element and active styles`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one">Autotest paragraph</p><h2 id="two">Autotest heading</h2><p id="styled" class="small bold">Autotest styled paragraph</p><h2 id="styled-heading" class="hero">Autotest styled heading</h2>', resolve)));
        for (const [id, expected] of [['one', 'Paragraph'], ['two', 'Heading 2'], ['styled', 'P + Small, Bold'], ['styled-heading', 'H2 + Heading appearance']]) {
            I.executeScript(id => autotestSelect(id), id);
            I.assertEqual(await I.grabTextFrom('.cke_button__wjstyles_label'), expected, 'The summary must identify the selected tag without replacing style names with a count');
            I.assertEqual(await I.grabAttributeFrom('.cke_button__wjstyles', 'title'), expected, 'The tooltip must retain the full summary');
            I.assertEqual(await I.grabAttributeFrom('.cke_button__wjstyles', 'aria-label'), `Styles: ${expected}`, 'The accessible name must include the current element');
        }
        I.executeScript(() => autotestSelect('one', 'two'));
        I.assertEqual(await I.grabTextFrom('.cke_button__wjstyles_label'), 'Selection', 'A mixed selection must not claim a single paragraph format');
        I.executeScript(() => autotestSelect('two'));
        I.click('.cke_button__wjstyles');
        I.click('.wj-styles__format[data-format="p"]');
        I.assertEqual(await I.grabTextFrom('.cke_button__wjstyles_label'), 'Paragraph', 'Changing the paragraph format must refresh the summary immediately');
        I.pressKey('Escape');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.waitForText('Heading 2', 5, '.cke_button__wjstyles_label');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Summary updates must not produce browser errors');
    });

    Scenario(`${mode}: category icon metadata survives filtering and resets on catalog reload`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one">Autotest icons</p>', resolve)));
        I.executeScript(() => {
            window.autotestIconStyles = [
                { name: 'Autotest invalid border', element: 'p', attributes: { class: 'autotest-invalid-border' }, wj: { category: 'Borders', categoryIcon: 'ti-border-all extra' } },
                { name: 'Autotest hidden border', element: 'section', attributes: { class: 'autotest-hidden-border' }, wj: { category: 'Borders', categoryIcon: 'ti-border-none' } },
                { name: 'Autotest visible border', element: 'p', attributes: { class: 'autotest-visible-border' }, wj: { category: 'Borders', categoryIcon: 'ti-border-all' } },
                { name: 'Autotest plain spacing', element: 'p', attributes: { class: 'autotest-spacing' }, wj: { category: 'Spacing' } },
                { name: 'Autotest hidden spacing', element: 'section', attributes: { class: 'autotest-hidden-spacing' }, wj: { category: 'Spacing', categoryIcon: 'ti-box-padding' } },
                { name: 'Autotest color', element: 'p', attributes: { class: 'autotest-color' }, wj: { category: 'Text color' } },
                { name: 'Autotest distinct category', element: 'p', attributes: { class: 'autotest-distinct' }, wj: { category: 'borders' } }
            ];
            autotestEditor.fire('stylesSet', { styles: autotestIconStyles });
            autotestSelect('one');
        });
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.assertDeepEqual(await I.grabTextFromAll('.wj-styles__group:not(.wj-styles__formats) .wj-styles__group-name'), ['Borders', 'Spacing', 'Text color', 'borders'], 'Category icons must keep exact category names independent');
        I.seeElementInDOM('.wj-styles__group-toggle[aria-controls$="-category-0"] .ti-border-none');
        I.seeElementInDOM('.wj-styles__group-toggle[aria-controls$="-category-1"] .ti-box-padding');
        I.seeElementInDOM('.wj-styles__group-toggle[aria-controls$="-category-2"] .ti-palette');
        I.seeElementInDOM('.wj-styles__group-toggle[aria-controls$="-category-3"] .ti-adjustments');
        I.dontSee('Autotest hidden border', '.wj-styles__list');
        I.dontSee('Autotest hidden spacing', '.wj-styles__list');
        I.fillField('.wj-styles__search', 'Autotest visible border');
        I.checkOption('.wj-styles__filters input');
        I.see('Autotest visible border', '.wj-styles__list');
        I.dontSee('Autotest hidden border', '.wj-styles__list');
        I.seeElementInDOM('.wj-styles__group-toggle .ti-border-none');
        I.dontSeeElementInDOM('.wj-styles__group-toggle .ti-border-all');
        I.pressKey('Escape');
        I.executeScript(() => {
            const definitions = autotestIconStyles.map(definition => ({ ...definition, wj: { category: definition.wj.category } }));
            autotestEditor.fire('stylesSet', { styles: definitions });
            autotestSelect('one');
        });
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.seeElementInDOM('.wj-styles__group-toggle .ti-adjustments');
        I.dontSeeElementInDOM('.wj-styles__group-toggle .ti-border-none');
        I.fillField('.wj-styles__search', '');
        I.seeElementInDOM('.wj-styles__group-toggle[aria-controls$="-category-1"] .ti-spacing-vertical');
        I.seeElementInDOM('.wj-styles__group-toggle[aria-controls$="-category-2"] .ti-palette');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Category icon metadata and reloads must not produce browser errors');
    });

    Scenario(`${mode}: structural breadcrumb roles follow configured selectors`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<section id="structure"><div id="container" class="container"><div id="row" class="row"><div id="column" class="col-12"><div id="generic" class="alert info"><p id="one" class="small"><span id="colored" class="red">Autotest structure</span></p></div></div></div></div></section>', resolve)));
        I.executeScript(() => {
            autotestEditor.config.wjstylescombo_structure = { section: '#structure', container: '.container', row: '.row', column: '.col-12' };
            autotestEditor.config.wjstylescombo_scope = editor => editor.editable().$;
            autotestEditor.wjstylescombo.items = autotestEditor.wjstylescombo.items.filter(item => !item.tags.includes('section'));
            autotestSelect('colored', null, 4, 4);
        });
        const original = await I.executeScript(() => autotestEditor.getData());
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        const labels = ['Section', 'Container', 'Row', 'Column', 'Block', 'Paragraph', 'Text'];
        I.assertDeepEqual(await I.grabTextFromAll('.wj-styles__target-option'), labels, 'Structural ancestors must remain navigable without matching catalog styles');
        I.assertDeepEqual(await I.grabTextFromAll('.wj-styles__target-part > span[aria-hidden="true"]'), Array(6).fill('›'), 'Breadcrumb separators must not become part of accessible button names');
        I.dontSee('Apply to', '.wj-styles__target');
        I.seeElement('.wj-styles__target-path[role="group"][aria-label="Apply to"]');
        I.assertDeepEqual(await I.executeScript(() => Array.from(document.querySelectorAll('.wj-styles__target-option[data-type]')).map(button => {
            const square = getComputedStyle(button, '::before');
            return { type: button.dataset.type, color: square.backgroundColor, width: square.width, height: square.height };
        })), [
            { type: 'section', color: 'rgb(100, 134, 255)', width: '7px', height: '7px' },
            { type: 'container', color: 'rgb(246, 77, 145)', width: '7px', height: '7px' },
            { type: 'row', color: 'rgb(104, 111, 131)', width: '7px', height: '7px' },
            { type: 'column', color: 'rgb(0, 161, 134)', width: '7px', height: '7px' }
        ], 'Structural breadcrumb squares must match the Page Builder role colors');
        I.assertTrue(await I.executeScript(() => Array.from(document.querySelectorAll('.wj-styles__target-option:not([data-type])')).every(button => {
            const square = getComputedStyle(button, '::before');
            return square.content === 'none' || square.display === 'none';
        })), 'Generic blocks, paragraphs and text must not display a structural square');
        I.assertEqual(await I.grabAttributeFrom('.wj-styles__target-option[data-target=""]', 'title'), 'span#colored.red', 'Short labels must retain the full element details in the tooltip');
        for (const type of ['section', 'container', 'row', 'column']) {
            I.click(`.wj-styles__target-option[data-type="${type}"]`);
            I.seeElement(`.wj-styles__target-option[data-type="${type}"][aria-pressed="true"]`);
        }
        I.click('Text', '.wj-styles__target');
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original, 'Navigating structural targets must not change the content');
        I.click('.wj-styles__search');
        I.executeScript(() => {
            autotestEditor.config.wjstylescombo_structure = { section: '#structure', container: '.configured-container', row: '.configured-row', column: '.configured-column' };
            for (const type of ['container', 'row', 'column']) autotestEditor.document.getById(type).setAttribute('class', `configured-${type}`);
            autotestEditor.document.getById('generic').setAttribute('class', 'alert info container row col-12');
            autotestEditor.wjstylescombo.render();
        });
        I.assertDeepEqual(await I.grabTextFromAll('.wj-styles__target-option'), labels, 'Both editor modes must use only the configured structural selectors');
        I.assertDeepEqual(await I.executeScript(() => Array.from(document.querySelectorAll('.wj-styles__target-option[data-type]')).map(button => [button.dataset.type, button.title])), [
            ['section', 'section#structure'], ['container', 'div#container.configured-container'], ['row', 'div#row.configured-row'], ['column', 'div#column.configured-column']
        ], 'Updated roles must keep accurate element tooltips and ignore old selector matches');
        I.executeScript(() => {
            autotestEditor.config.wjstylescombo_structure.column = 'div[class*="col-"]:not(.pb-not-column), div[class*="pb-col"]:not(.pb-not-column):not(.pb-column__content)';
            autotestEditor.document.getById('column').setAttribute('class', 'pb-col12');
        });
        for (const classes of ['pb-column__content', 'col-12 pb-col12 pb-not-column']) {
            I.executeScript(classes => {
                autotestEditor.document.getById('generic').setAttribute('class', `alert info ${classes}`);
                autotestEditor.wjstylescombo.render();
            }, classes);
            I.seeElement('.wj-styles__target-option[title="div#column.pb-col12"][data-type="column"]');
            I.seeElement('.wj-styles__target-option[title^="div#generic."]:not([data-type])');
        }
        I.pressKey('Escape');
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<main id="layout" class="autotest-layout">Autotest direct text</main>', resolve)));
        I.executeScript(() => {
            autotestEditor.config.wjstylescombo_structure = { section: '.autotest-layout' };
            autotestSelect('layout', null, 4, 4);
        });
        const directText = await I.executeScript(() => autotestEditor.getData());
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.assertDeepEqual(await I.grabTextFromAll('.wj-styles__target-option'), ['Section'], 'A custom structural tag containing direct text must remain the current target');
        I.seeElement('.wj-styles__target-option[data-target=""][data-type="section"][aria-pressed="true"]');
        I.assertEqual(await I.grabAttributeFrom('.wj-styles__target-option[data-target=""]', 'title'), 'main#layout.autotest-layout');
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), directText, 'Resolving a custom structural target must preserve its direct text');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Structural breadcrumb classification must not produce browser errors');
    });

    Scenario(`${mode}: Bootstrap tooltips follow target changes and clean up`, async ({ I }) => {
        I.amOnPage('/admin/');
        I.waitForFunction(() => typeof window.bootstrap?.Tooltip === 'function', [], 10);
        // Keep the real administration Bootstrap in the parent, as Page Builder does.
        I.executeScript(mode => {
            const frame = document.createElement('iframe');
            frame.id = 'autotest-tooltip-frame';
            frame.title = 'Autotest breadcrumb tooltips';
            frame.style.cssText = 'position:fixed;inset:0;width:100%;height:100%;border:0;z-index:2147483647;background:white';
            frame.src = `/wjstyles-autotest.html?mode=${mode}`;
            document.body.appendChild(frame);
        }, mode);
        I.waitForFunction(() => document.getElementById('autotest-tooltip-frame').contentWindow.autotestEditor?.status === 'ready', [], 10);
        I.switchTo('#autotest-tooltip-frame');
        I.assertTrue(await I.executeScript(() => !window.bootstrap && typeof parent.bootstrap?.Tooltip === 'function'), 'The fixture must reuse its parent Bootstrap without loading Bootstrap CSS or JavaScript locally');
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="custom small">Autotest tooltip</p>', resolve)));
        I.executeScript(() => autotestSelect('one'));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        const target = '.wj-styles__target-option[data-target=""]';
        I.moveCursorTo(target);
        I.waitForVisible('.wj-styles-tooltip.show');
        I.see('p#one.custom.small', '.wj-styles-tooltip .tooltip-inner');
        I.assertTrue(await I.executeScript(() => {
            const tip = window.autotestOldTooltip = document.querySelector('.wj-styles-tooltip');
            const inner = getComputedStyle(tip.querySelector('.tooltip-inner'));
            return tip.parentElement === document.body && getComputedStyle(tip).opacity === '1' &&
                Number(getComputedStyle(tip).zIndex) > Number(getComputedStyle(document.querySelector('.wj-styles')).zIndex) &&
                inner.color === 'rgb(255, 255, 255)' && inner.backgroundColor === 'rgb(19, 21, 27)' && inner.borderRadius === '6px';
        }), 'The tooltip must use the scoped administration theme above the panel in its own document');
        I.click('.wj-styles__item[data-style-id="1"]');
        I.assertFalse(await I.executeScript(() => autotestOldTooltip.isConnected), 'Rebuilding targets must remove the previous tooltip');
        I.dontSeeElement('.wj-styles-tooltip');
        I.moveCursorTo(target);
        I.waitForVisible('.wj-styles-tooltip.show');
        I.see('p#one.custom.large', '.wj-styles-tooltip .tooltip-inner');
        I.moveCursorTo('.wj-styles__chip');
        I.waitForElement('.wj-styles__chip[aria-describedby]');
        const chipTooltip = await I.grabAttributeFrom('.wj-styles__chip', 'aria-describedby');
        I.waitForText('large', 5, `#${chipTooltip} .tooltip-inner`);
        I.assertEqual(await I.grabAttributeFrom('.wj-styles__chip', 'aria-label'), 'Remove: Large', 'CSS tooltips must preserve the accessible removal action');
        I.moveCursorTo('.wj-styles__filters label span');
        I.waitForText('Show styles that cannot be applied to the current selection as well.', 5, '.wj-styles-tooltip.show .tooltip-inner');
        I.seeElementInDOM('.wj-styles__group-line button .ti-restore[aria-hidden="true"]');
        I.see('Remove', '.wj-styles__group-line button');
        I.pressKey('Escape');
        I.waitToHide('.wj-styles');
        I.dontSeeElement('.wj-styles-tooltip');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.moveCursorTo('.wj-styles__search');
        I.pressKey('Tab');
        I.pressKey('Tab');
        I.assertTrue(await I.executeScript(() => document.activeElement.matches('.wj-styles__filters input')), 'Keyboard traversal must reach the all-styles checkbox');
        I.waitForText('Show styles that cannot be applied to the current selection as well.', 5, '.wj-styles-tooltip.show .tooltip-inner');
        I.pressKey(['Shift', 'Tab']);
        I.pressKey(['Shift', 'Tab']);
        I.pressKey(['Shift', 'Tab']);
        I.pressKey(['Shift', 'Tab']);
        I.assertTrue(await I.executeScript(() => document.activeElement.matches('.wj-styles__target-option[data-target=""]')), 'Keyboard traversal must reach the current breadcrumb target');
        I.waitForVisible('.wj-styles-tooltip.show');
        I.see('p#one.custom.large', '.wj-styles-tooltip .tooltip-inner');
        I.executeScript(() => autotestEditor.destroy());
        I.dontSeeElement('.wj-styles-tooltip');
        I.dontSeeElement('.wj-styles');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Tooltip hover, focus and disposal must not produce browser errors');
        I.switchTo();
        I.executeScript(() => document.getElementById('autotest-tooltip-frame').remove());
    });

    Scenario(`${mode}: dragging the picker preserves selection and stays inside the viewport`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}${mode === 'classic' ? '&modal=1' : ''}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.resizeWindow(1200, 900);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one"><span id="selected" class="red custom">Before selected after</span></p>', resolve)));
        I.executeScript(() => autotestSelect('selected', null, 7, 15));
        const original = await I.executeScript(() => ({ html: autotestEditor.getData(), undo: autotestEditor.undoManager.index }));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.seeElement('.wj-styles__drag');
        I.seeElementInDOM('.wj-styles__drag .ti-grip-vertical');
        const readPosition = () => {
            const rect = document.querySelector('.wj-styles').getBoundingClientRect();
            return { left: Math.round(rect.left), top: Math.round(rect.top) };
        };
        const anchored = await I.executeScript(readPosition);
        // Pointer capture must be exercised outside the handle; dragAndDrop requires a destination element.
        await I.usePlaywrightTo('drag the picker by its handle', async ({ page }) => {
            const handle = await page.locator('.wj-styles__drag').boundingBox();
            const x = handle.x + handle.width / 2, y = handle.y + handle.height / 2;
            await page.mouse.move(x, y);
            await page.mouse.down();
            await page.mouse.move(x + 160, y + 60, { steps: 8 });
            await page.mouse.up();
        });
        const moved = { left: anchored.left + 160, top: anchored.top + 60 };
        I.assertDeepEqual(await I.executeScript(readPosition), moved, 'Dragging must move the panel by the pointer distance');
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original.html, 'Dragging must not alter editor content');
        I.assertEqual(await I.executeScript(() => autotestEditor.undoManager.index), original.undo, 'Dragging must not create undo operations');
        I.fillField('.wj-styles__search', 'Blue');
        I.assertDeepEqual(await I.executeScript(readPosition), moved, 'Searching must retain the chosen panel position');
        I.click('.wj-styles__item[data-style-id="5"]');
        I.seeElement('.wj-styles:not([hidden])');
        I.assertDeepEqual(await I.executeScript(readPosition), moved, 'Applying a style must retain the chosen panel position');
        I.assertDeepEqual(await I.executeScript(() => {
            const paragraph = autotestEditor.document.getById('one').$;
            return { text: paragraph.textContent, blue: paragraph.querySelector('.blue')?.textContent, surrounding: Array.from(paragraph.querySelectorAll('.red')).map(element => element.textContent).join('|') };
        }), { text: 'Before selected after', blue: 'selected', surrounding: 'Before | after' }, 'Dragging must preserve the exact original text selection');
        I.click('.wj-styles__drag');
        I.pressKey('ArrowRight');
        I.pressKey(['Shift', 'ArrowDown']);
        const nudged = { left: moved.left + 10, top: moved.top + 1 };
        I.assertDeepEqual(await I.executeScript(readPosition), nudged, 'The handle must support ten-pixel arrow movement and one-pixel Shift movement');
        I.resizeWindow(1300, 1000);
        I.assertDeepEqual(await I.executeScript(readPosition), nudged, 'A larger viewport must retain the chosen panel position');
        for (const corner of ['top-left', 'bottom-right']) {
            // Use real captured pointer events even when the pointer moves beyond the panel bounds.
            await I.usePlaywrightTo(`drag the picker to the ${corner} viewport corner`, async ({ page }) => {
                const handle = await page.locator('.wj-styles__drag').boundingBox();
                const viewport = page.viewportSize();
                await page.mouse.move(handle.x + handle.width / 2, handle.y + handle.height / 2);
                await page.mouse.down();
                await page.mouse.move(corner === 'top-left' ? 1 : viewport.width - 1, corner === 'top-left' ? 1 : viewport.height - 1, { steps: 8 });
                await page.mouse.up();
            });
            I.assertTrue(await I.executeScript(() => {
                const rect = document.querySelector('.wj-styles').getBoundingClientRect();
                return rect.left >= 0 && rect.top >= 0 && rect.right <= innerWidth && rect.bottom <= innerHeight;
            }), 'Dragging toward a viewport corner must keep the whole panel visible');
        }
        I.resizeWindow(767, 700);
        I.waitForFunction(() => {
            const rect = document.querySelector('.wj-styles').getBoundingClientRect();
            return rect.left >= 0 && rect.top >= 0 && rect.right <= innerWidth && rect.bottom <= innerHeight;
        }, [], 5);
        I.resizeWindow(1200, 900);
        I.pressKey('Escape');
        I.waitToHide('.wj-styles');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original.html, 'One undo must restore the text style applied after dragging');
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.assertDeepEqual(await I.executeScript(readPosition), anchored, 'Reopening must restore the default toolbar anchor');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Dragging must not produce browser errors');
    });

    Scenario(`${mode}: previews show typography, spacing and contextual alert appearance`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<div class="autotest-theme"><div id="alert" class="alert info custom">Autotest alert</div><p id="one">Autotest preview</p></div><div id="plain-alert" class="alert info">Autotest plain alert</div>', resolve)));
        I.executeScript(() => { autotestSelect('one'); autotestEditor.resetDirty(); });
        const baseline = await I.executeScript(() => ({ html: autotestEditor.getData(), undo: autotestEditor.undoManager.index }));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.assertTrue(await I.executeScript(() => document.activeElement.matches('.wj-styles__search')), 'Opening must focus the search field');
        I.assertDeepEqual(await I.executeScript(() => {
            const css = getComputedStyle(document.querySelector('.wj-styles__search'));
            return { outline: css.outlineStyle, border: css.borderTopColor, inset: css.boxShadow.includes('inset') };
        }), { outline: 'none', border: 'rgb(0, 99, 251)', inset: true }, 'Search focus must highlight the input border without an outer outline');
        I.fillField('.wj-styles__search', 'large');
        I.assertDeepEqual(await I.executeScript(() => {
            const css = getComputedStyle(document.querySelector('.wj-styles__item[data-style-id="1"] .wj-styles__sample-preview'));
            return { size: css.fontSize, weight: css.fontWeight, style: css.fontStyle, spacing: css.letterSpacing, transform: css.textTransform, decoration: css.textDecorationLine };
        }), { size: '40px', weight: '700', style: 'italic', spacing: '2px', transform: 'uppercase', decoration: 'underline' }, 'Typography previews must retain the actual CSS values instead of clamping font size');
        I.see('40 px', '.wj-styles__item[data-style-id="1"] .wj-styles__sample-value');
        I.fillField('.wj-styles__search', 'gap');
        I.seeElement('.wj-styles__item[data-style-id="13"] .wj-styles__sample--spacing');
        I.seeElement('.wj-styles__item[data-style-id="14"] .wj-styles__sample--spacing');
        I.see('8 px', '.wj-styles__item[data-style-id="13"] .wj-styles__sample-value');
        I.see('40 px', '.wj-styles__item[data-style-id="14"] .wj-styles__sample-value');
        I.dontSee('Aa', '.wj-styles__item[data-style-id="13"] .wj-styles__sample');
        I.pressKey('Escape');
        I.executeScript(() => autotestSelect('alert'));
        I.click('.cke_button__wjstyles');
        I.fillField('.wj-styles__search', 'Alert info');
        const readAlertPreview = () => {
            const css = getComputedStyle(document.querySelector('.wj-styles__item[data-style-id="8"] .wj-styles__sample-preview'));
            return { color: css.color, background: css.backgroundColor, border: css.borderTopColor, borderWidth: css.borderTopWidth, borderStyle: css.borderTopStyle, radius: css.borderTopLeftRadius };
        };
        I.assertDeepEqual(await I.executeScript(readAlertPreview), { color: 'rgb(17, 59, 25)', background: 'rgb(221, 246, 225)', border: 'rgb(33, 90, 45)', borderWidth: '2px', borderStyle: 'solid', radius: '8px' }, 'Alert previews must show text, fill and border from their actual ancestor context');
        I.pressKey('Escape');
        I.executeScript(() => autotestSelect('plain-alert'));
        I.click('.cke_button__wjstyles');
        I.assertDeepEqual(await I.executeScript(readAlertPreview), { color: 'rgb(20, 20, 20)', background: 'rgb(224, 239, 255)', border: 'rgb(16, 53, 110)', borderWidth: '2px', borderStyle: 'solid', radius: '8px' }, 'Moving to another context must refresh the preview instead of reusing cached colors');
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), baseline.html, 'Rendering previews must not change the editor content');
        I.assertFalse(await I.executeScript(() => autotestEditor.checkDirty()), 'Rendering previews must not mark the page as changed');
        I.assertEqual(await I.executeScript(() => autotestEditor.undoManager.index), baseline.undo, 'Rendering previews must not create undo operations');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Preview rendering must not produce browser errors');
    });

    Scenario(`${mode}: paragraph formats preserve CSS, markup and one-step undo`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="custom small"><strong id="selected">Autotest first</strong> tail</p><h2 id="two" class="large">Autotest second</h2>', resolve)));
        I.executeScript(() => autotestSelect('selected', 'two'));
        const original = await I.executeScript(() => autotestEditor.getData());
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.seeElement('.wj-styles__format[data-format="h2"][aria-pressed="mixed"]');
        I.click('.wj-styles__format[data-format="h2"]');
        I.seeElement('.wj-styles:not([hidden])');
        I.seeElement('.wj-styles__format[data-format="h2"][aria-pressed="true"]');
        I.assertDeepEqual(await I.executeScript(() => ['one', 'two'].map(id => {
            const element = autotestEditor.document.getById(id);
            return { tag: element.getName(), classes: element.getAttribute('class'), text: element.getText() };
        })), [
            { tag: 'h2', classes: 'custom small', text: 'Autotest first tail' },
            { tag: 'h2', classes: 'large', text: 'Autotest second' }
        ], 'Semantic formatting must convert every selected block while preserving CSS and text');
        I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('selected').getName()), 'strong', 'Inline emphasis must survive block conversion');
        const formatted = await I.executeScript(() => autotestEditor.getData());
        I.click('.wj-styles__format[data-format="h2"]');
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), formatted, 'Selecting the current format must not remove it');
        I.pressKey('Escape');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original, 'One undo must restore the complete mixed block selection');
        I.executeScript(() => autotestEditor.execCommand('redo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), formatted, 'Redo must restore semantic formatting and all CSS classes');
        I.assertNotContain(formatted, 'wjstyles:', 'Picker state must not be serialized into document content');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Semantic formatting must not produce browser errors');
    });

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
        I.seeElementInDOM('.wj-styles__group-toggle[aria-controls$="-category-0"] .ti-typography');
        I.seeElementInDOM('.wj-styles__group-toggle[aria-controls$="-category-1"] .ti-palette');
        I.seeElement('.wj-styles__item--exclusive[data-style-id="0"]');
        I.click('.wj-styles__item[data-style-id="0"]');
        const typography = '.wj-styles__group-toggle[aria-controls$="-category-0"]';
        I.click(typography);
        I.see('Expand all', '.wj-styles__toggle-all');
        I.dontSeeElement('.wj-styles__item[data-style-id="0"]');
        I.assertTrue(await I.executeScript(() => {
            const groups = document.querySelectorAll('.wj-styles__group:not(.wj-styles__formats)');
            return Math.abs(groups[0].getBoundingClientRect().bottom - groups[1].getBoundingClientRect().top) < 1;
        }), 'Editor section markers must not add gaps between accordion headers');
        I.pressKey('ArrowDown');
        I.assertTrue(await I.executeScript(() => document.activeElement === document.querySelector('.wj-styles__group-toggle[aria-controls$="-category-1"]')), 'Arrow navigation must skip collapsed choices');
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
        I.seeElement('.wj-styles__list > .wj-styles__formats:first-child');
        I.assertTrue(await I.executeScript(() => {
            const picker = autotestEditor.wjstylescombo;
            picker.list.scrollTop = picker.list.scrollHeight;
            return picker.list.scrollTop > 0;
        }), 'The list must be scrolled before testing reopening');
        const scroll = await I.executeScript(() => autotestEditor.wjstylescombo.list.scrollTop);
        I.executeScript(() => autotestApply('Gap small'));
        I.assertEqual(await I.executeScript(() => autotestEditor.wjstylescombo.list.scrollTop), scroll, 'Applying a style must preserve scrolling during the current opening');
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
        I.assertEqual(await I.executeScript(() => autotestEditor.wjstylescombo.list.scrollTop), 0, 'Reopening must start at the top of the list');
        I.seeElement('.wj-styles__formats .wj-styles__format[data-format="h2"]');
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

    Scenario(`${mode}: inline styles require selected text at a plain caret`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="custom">Autotest plain text</p><h2 id="heading">Autotest heading</h2><p><span class="red"><img id="image" alt="Autotest" src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"></span></p>', resolve)));
        const original = await I.executeScript(() => autotestEditor.getData());
        for (const id of ['one', 'heading', 'image']) {
            I.executeScript(id => autotestSelect(id, null, 4, 4), id);
            I.click('.cke_button__wjstyles');
            I.waitForVisible('.wj-styles');
            I.dontSeeElement('.wj-styles__item[data-style-id="4"]');
            I.dontSeeElement('.wj-styles__item[data-style-id="5"]');
            I.checkOption('.wj-styles__filters input');
            I.seeElement('.wj-styles__item[data-style-id="4"]:disabled');
            I.seeElement('.wj-styles__item[data-style-id="5"]:disabled');
            const undo = await I.executeScript(() => autotestEditor.undoManager.index);
            I.executeScript(() => autotestApply('Blue'));
            I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original, 'A plain caret or selected image must not create an inline style or filler');
            I.assertEqual(await I.executeScript(() => autotestEditor.undoManager.index), undo, 'An inapplicable inline style must not create an undo operation');
            I.uncheckOption('.wj-styles__filters input');
            I.pressKey('Escape');
        }
        I.executeScript(() => autotestSelect('one', null, 9, 14));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles__item[data-style-id="5"]:not(:disabled)');
        I.click('.wj-styles__item[data-style-id="5"]');
        I.assertDeepEqual(await I.executeScript(() => {
            const paragraph = autotestEditor.document.getById('one').$;
            return { tag: paragraph.tagName.toLowerCase(), classes: paragraph.className, text: paragraph.textContent, selected: paragraph.querySelector('span.blue')?.textContent, spans: paragraph.querySelectorAll('span').length };
        }), { tag: 'p', classes: 'custom', text: 'Autotest plain text', selected: 'plain', spans: 1 }, 'A text selection must wrap only the selected text while preserving its paragraph');
        I.pressKey('Escape');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original, 'One undo must remove the selected text wrapper');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Inline style eligibility must not produce browser errors');
    });

    Scenario(`${mode}: inline styles at a caret update the existing span`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<h2 id="heading">Before <span id="colored" class="red custom" title="Autotest span" data-autotest="keep"><strong id="inside">Autotest nested</strong> tail</span> after</h2><p><span id="outer" class="red"><span id="inner" class="custom"><em id="nested">Autotest child</em></span></span></p>', resolve)));
        I.executeScript(() => autotestSelect('inside', null, 4, 4));
        const original = await I.executeScript(() => {
            const span = autotestEditor.document.getById('colored').$;
            return { html: autotestEditor.getData(), spanHtml: span.innerHTML, attributes: Array.from(span.attributes).filter(attribute => attribute.name !== 'class').map(attribute => [attribute.name, attribute.value]).sort() };
        });
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.seeElement('.wj-styles__item[data-style-id="4"][aria-pressed="true"]');
        I.seeElement('.wj-styles__item[data-style-id="5"]:not(:disabled)');
        I.click('.wj-styles__item[data-style-id="5"]');
        I.assertDeepEqual(await I.executeScript(() => {
            const span = autotestEditor.document.getById('colored').$;
            const heading = autotestEditor.document.getById('heading').$;
            return {
                classes: Array.from(span.classList).sort(),
                spanHtml: span.innerHTML,
                attributes: Array.from(span.attributes).filter(attribute => attribute.name !== 'class').map(attribute => [attribute.name, attribute.value]).sort(),
                spans: heading.querySelectorAll('span').length,
                tag: heading.tagName.toLowerCase(),
                text: heading.textContent
            };
        }), { classes: ['blue', 'custom'], spanHtml: original.spanHtml, attributes: original.attributes, spans: 1, tag: 'h2', text: 'Before Autotest nested tail after' }, 'A nested caret must update the enclosing span while preserving unknown classes, attributes and markup');
        I.pressKey('Escape');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original.html, 'One undo must restore the existing span and its original color');
        I.executeScript(() => autotestSelect('nested', null, 4, 4));
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.seeElement('.wj-styles__item[data-style-id="4"][aria-pressed="false"]');
        I.dontSee('Červená', '.wj-styles__chips');
        I.click('.wj-styles__item[data-style-id="5"]');
        I.assertDeepEqual(await I.executeScript(() => {
            const outer = autotestEditor.document.getById('outer').$;
            const inner = autotestEditor.document.getById('inner').$;
            return { outer: outer.className, inner: Array.from(inner.classList).sort(), text: inner.textContent, nestedSpans: outer.querySelectorAll('span').length, emphasis: inner.querySelector('em')?.id };
        }), { outer: 'red', inner: ['blue', 'custom'], text: 'Autotest child', nestedSpans: 1, emphasis: 'nested' }, 'A caret must change only its nearest span without treating an outer color as removable');
        I.pressKey('Escape');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original.html, 'One undo must restore a nested span without changing its outer color');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Styling an existing span must not produce browser errors');
    });

    Scenario(`${mode}: span caret target separates paragraph and text styles`, async ({ I }) => {
        I.amOnPage(`/wjstyles-autotest.html?mode=${mode}`);
        I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
        I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="custom small"><strong id="emphasis"><span id="colored" class="red">Autotest text</span></strong></p>', resolve)));
        I.executeScript(() => autotestSelect('colored', null, 4, 4));
        const original = await I.executeScript(() => autotestEditor.getData());
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        const targets = mode === 'inline' ? ['Section', 'Paragraph', 'Text'] : ['Paragraph', 'Text'];
        I.assertDeepEqual(await I.grabTextFromAll('.wj-styles__target-option'), targets, 'The target path must end with the span containing the caret');
        I.assertEqual(await I.grabTextFrom('.wj-styles__target-option[aria-pressed="true"]'), 'Text');
        I.assertEqual(await I.grabTextFrom('.cke_button__wjstyles_label'), 'SPAN + Červená', 'A span caret must describe the text target rather than its parent paragraph');
        I.see('Červená', '.wj-styles__chips');
        I.dontSee('Small', '.wj-styles__chips');
        I.dontSeeElement('.wj-styles__item[data-style-id="1"]');
        I.dontSeeElement('.wj-styles__formats');
        I.checkOption('.wj-styles__filters input');
        I.seeElement('.wj-styles__item[data-style-id="1"]:disabled');
        I.seeElement('.wj-styles__format[data-format="h2"]:disabled');
        I.uncheckOption('.wj-styles__filters input');
        I.click('.wj-styles__item[data-style-id="5"]');
        I.assertDeepEqual(await I.executeScript(() => ['one', 'colored'].map(id => autotestEditor.document.getById(id).getAttribute('class'))), ['custom small', 'blue'], 'A text style must change only the selected span');
        const spanStyled = await I.executeScript(() => autotestEditor.getData());
        I.click('Paragraph', '.wj-styles__target');
        I.assertEqual(await I.grabTextFrom('.wj-styles__target-option[aria-pressed="true"]'), 'Paragraph');
        I.assertEqual(await I.grabTextFrom('.cke_button__wjstyles_label'), 'P + Small', 'Selecting a parent must update the summary to that target');
        I.see('Small', '.wj-styles__chips');
        I.dontSee('Blue', '.wj-styles__chips');
        I.seeElement('.wj-styles__item[data-style-id="1"]:not(:disabled)');
        I.seeElement('.wj-styles__format[data-format="h2"]:not(:disabled)');
        I.dontSeeElement('.wj-styles__item[data-style-id="5"]');
        I.click('.wj-styles__item[data-style-id="1"]');
        I.assertDeepEqual(await I.executeScript(() => ['one', 'colored'].map(id => autotestEditor.document.getById(id).getAttribute('class'))), ['custom large', 'blue'], 'A paragraph style must leave the span appearance unchanged');
        const paragraphStyled = await I.executeScript(() => autotestEditor.getData());
        I.click('.wj-styles__format[data-format="h2"]');
        I.assertEqual(await I.grabTextFrom('.wj-styles__target-option[aria-pressed="true"]'), 'Heading 2', 'Semantic formatting must retain the explicitly selected parent target');
        I.assertDeepEqual(await I.executeScript(() => {
            const block = autotestEditor.document.getById('one');
            const span = autotestEditor.document.getById('colored');
            return { tag: block.getName(), classes: block.getAttribute('class'), span: span.getAttribute('class'), parent: span.getParent().getName(), text: block.getText() };
        }), { tag: 'h2', classes: 'custom large', span: 'blue', parent: 'strong', text: 'Autotest text' }, 'Parent formatting must preserve the styled span and emphasis');
        I.click('Text', '.wj-styles__target');
        I.assertEqual(await I.grabTextFrom('.wj-styles__target-option[aria-pressed="true"]'), 'Text');
        I.see('Blue', '.wj-styles__chips');
        I.dontSee('Large', '.wj-styles__chips');
        I.dontSeeElement('.wj-styles__item[data-style-id="1"]');
        I.dontSeeElement('.wj-styles__formats');
        I.pressKey('Escape');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), paragraphStyled, 'One undo must restore the parent paragraph after semantic formatting');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), spanStyled, 'The next undo must restore only the paragraph appearance');
        I.executeScript(() => autotestEditor.execCommand('undo'));
        I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original, 'The final undo must restore the original span appearance');
        I.executeScript(() => {
            const picker = autotestEditor.wjstylescombo;
            picker.items = picker.items.filter(item => !item.tags.includes('p'));
            autotestSelect('colored', null, 4, 4);
        });
        I.click('.cke_button__wjstyles');
        I.waitForVisible('.wj-styles');
        I.assertDeepEqual(await I.grabTextFromAll('.wj-styles__target-option'), targets, 'The parent paragraph must remain available without explicit paragraph CSS definitions');
        I.click('Paragraph', '.wj-styles__target');
        I.assertEqual(await I.grabTextFrom('.wj-styles__target-option[aria-pressed="true"]'), 'Paragraph');
        I.seeElement('.wj-styles__format[data-format="h2"]:not(:disabled)');
        I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Switching between text and paragraph targets must not produce browser errors');
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
    I.waitForVisible('.wj-styles');
    I.assertDeepEqual(await I.grabTextFromAll('.wj-styles__target-option'), ['Section', 'Paragraph'], 'Targets must follow the outer-to-inner content hierarchy');
    I.seeElement('.wj-styles__target-option[data-target=""][aria-pressed="true"]');
    I.click('Section', '.wj-styles__target');
    I.seeElement('.wj-styles__target-option[data-target="0"][aria-pressed="true"]');
    I.seeElement('.wj-styles__target-option[data-target=""][aria-pressed="false"]');
    I.see('Light', '.wj-styles__chips');
    I.seeElement('.wj-styles__format[data-format="h2"]:disabled');
    I.click('.wj-styles__item[data-style-id="7"]');
    I.assertEqual(await I.executeScript(() => document.getElementById('autotest-section').className), 'structural bg-dark', 'Background replacement must preserve structural classes');
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'small');
    I.click('Paragraph', '.wj-styles__target');
    I.seeElement('.wj-styles__target-option[data-target=""][aria-pressed="true"]');
    I.seeElement('.wj-styles__target-option[data-target="0"][aria-pressed="false"]');
    I.see('Small', '.wj-styles__chips');
    I.seeElement('.wj-styles__format[data-format="h2"]:not(:disabled)');
    I.dontSee('Dark', '.wj-styles__chips');
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

Scenario('paragraph formats respect the configured allowed-content filter', async ({ I }) => {
    I.amOnPage('/wjstyles-autotest.html?mode=classic&restricted=1');
    I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
    I.executeScript(() => new Promise(resolve => autotestEditor.setData('<p id="one" class="custom small">Autotest restricted content</p>', resolve)));
    I.executeScript(() => autotestSelect('one'));
    I.click('.cke_button__wjstyles');
    I.waitForVisible('.wj-styles');
    I.assertDeepEqual(await I.executeScript(() => Array.from(document.querySelectorAll('.wj-styles__format')).map(button => button.dataset.format)), ['p', 'h2'], 'Only formats accepted by the active content filter may be offered');
    I.click('.wj-styles__format[data-format="h2"]');
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getName()), 'h2', 'An allowed heading must be applied');
    I.assertEqual(await I.executeScript(() => autotestEditor.document.getById('one').getAttribute('class')), 'custom small', 'Allowed appearance classes must survive semantic formatting');
    I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Filtered semantic formatting must not produce browser errors');
});

Scenario('inline: fixed editable roots explain why paragraph formats are unavailable', async ({ I }) => {
    I.amOnPage('/wjstyles-autotest.html?mode=inline&blockless=1');
    I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
    I.executeScript(() => autotestSelect('autotest-inline'));
    const original = await I.executeScript(() => {
        window.autotestFixedRoot = autotestEditor.editable().$;
        return autotestEditor.getData();
    });
    I.click('.cke_button__wjstyles');
    I.waitForVisible('.wj-styles');
    I.see('The element type is fixed by this editable block.', '.wj-styles__format-hint');
    I.dontSeeElement('.wj-styles__format:not(:disabled)');
    I.seeElement('.wj-styles__format[data-format="h2"]:disabled');
    I.executeScript(() => {
        const picker = autotestEditor.wjstylescombo;
        picker.applyFormat(picker.formats().find(format => format.name === 'h2'));
    });
    I.assertTrue(await I.executeScript(() => autotestEditor.editable().$ === window.autotestFixedRoot && autotestEditor.editable().getName() === 'p'), 'Formatting must not replace a fixed editable root');
    I.assertEqual(await I.executeScript(() => autotestEditor.getData()), original, 'Formatting must not add nested block elements to a fixed editable root');
    I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Fixed editable roots must not produce browser errors');
});

Scenario('paragraph formats register before initial automatic filtering without a Format toolbar', async ({ I }) => {
    I.amOnPage('/wjstyles-autotest.html?mode=classic&automatic=1');
    I.waitForFunction(() => window.autotestEditor?.status === 'ready', [], 10);
    I.dontSeeElement('.cke_combo__format');
    I.assertContain(await I.executeScript(() => autotestEditor.getData()), '<h2>Autotest initial heading</h2>', 'Initial heading markup must survive automatic filtering when only WJStyles exposes paragraph formats');
    I.executeScript(() => {
        const editor = autotestEditor;
        editor.focus();
        const range = editor.createRange();
        range.selectNodeContents(editor.editable().findOne('h2'));
        editor.getSelection().selectRanges([range]);
        editor.wjstylescombo.capture();
    });
    I.click('.cke_button__wjstyles');
    I.waitForVisible('.wj-styles');
    I.seeElement('.wj-styles__format[data-format="h2"][aria-pressed="true"]');
    I.click('.wj-styles__format[data-format="p"]');
    I.assertContain(await I.executeScript(() => autotestEditor.getData()), '<p>Autotest initial heading</p>', 'Registered formats must allow a paragraph conversion');
    I.click('.wj-styles__format[data-format="h2"]');
    I.assertContain(await I.executeScript(() => autotestEditor.getData()), '<h2>Autotest initial heading</h2>', 'Registered formats must allow the heading to be restored');
    I.assertDeepEqual(await I.executeScript(() => autotestErrors), [], 'Automatic filtering must not produce browser errors');
});
