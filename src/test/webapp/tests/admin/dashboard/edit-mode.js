const { mockDashboardBootstrap, dashboardPageRoute, showWidget } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.edit-mode');

const settingsRoute = '**/admin/rest/dashboard/settings';
const resetRoute = '**/admin/rest/dashboard/settings/reset';
const editButton = '.md-dashboard__toolbar-actions > button[aria-pressed]';
const formsId = 'edit-autotest-forms';
const pagesId = 'edit-autotest-pages';
const trafficId = 'edit-autotest-traffic';
let saved, mutations, rejectSave;

async function openFixture(I, longLayout = false, additionalItems = []) {
    saved = {
        version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true,
        acknowledgedNewsVersion: null, domainOptions: {}, items: [
            { id: trafficId, type: 'traffic', size: '3x3', options: { days: 7, metric: 'visits' } },
            { id: formsId, type: 'forms', size: '1x1', options: {} },
            { id: pagesId, type: 'recent-pages', size: '3x2', options: {} },
            ...(longLayout ? [
                { id: 'edit-autotest-extra-pages-1', type: 'recent-pages', size: '3x3', options: {} },
                { id: 'edit-autotest-extra-pages-2', type: 'recent-pages', size: '3x3', options: {} }
            ] : []),
            ...additionalItems,
            { id: 'edit-autotest-sessions', type: 'sessions', size: '2x3', options: {} }
        ]
    };
    mutations = [];
    rejectSave = false;
    for (const route of [settingsRoute, resetRoute]) await I.mockRoute(route, request => {
        mutations.push(request.request().postDataJSON());
        if (rejectSave) return request.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"autotest save failure"}' });
        saved = { ...request.request().postDataJSON(), configured: true };
        return request.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(saved) });
    });
    await mockDashboardBootstrap(I, () => ({ settings: saved, notices: [{
        id: 'edit-autotest-warning', severity: 'warning', icon: 'ti-info-circle', title: 'Notice autotest',
        description: 'Notice action stays active while editing.', action: { type: 'link', url: '/admin/v9/', label: 'Notice action autotest' }
    }] }));
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    await showWidget(I, formsId);
    const surfaces = await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__layout [data-instance-id]')].map(card => getComputedStyle(card).backgroundColor));
    I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector('.md-dashboard__toolbar').getBoundingClientRect().top - 64);
    });
    I.clickCss(editButton);
    await I.waitForElement('.md-dashboard.is-editing', 10);
    I.assertDeepEqual(await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__layout [data-instance-id]')].map(card => getComputedStyle(card).backgroundColor)), surfaces, 'Widget colors must remain unchanged during editing');
}

function focusGrip(I, id) {
    I.executeScript(id => {
        const card = document.querySelector(`[data-instance-id="${id}"]`);
        window.scrollbarMain.setMomentum(0, 0);
        window.scrollbarMain.setPosition(0, window.scrollbarMain.offset.y + card.getBoundingClientRect().top - 170);
        card.querySelector('.md-dashboard__drag').focus({ preventScroll: true });
    }, id);
}

function waitForSave(I) {
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard').dashboardController.saving === false, 20);
}

Before(({ login }) => { login('admin'); });

Scenario('Editing headers align controls, hide supplementary icons and keep reset hover readable', async ({ I }) => {
    const formsGeometry = () => I.executeScript(id => {
        const card = document.querySelector(`[data-instance-id="${id}"]`);
        const bounds = card.getBoundingClientRect();
        return {
            header: card.querySelector('.md-dashboard__widget-header').getBoundingClientRect().height,
            title: card.querySelector('.md-dashboard__widget-title span').getBoundingClientRect().top - bounds.top,
            number: card.querySelector('.md-dashboard-widget__number').getBoundingClientRect().top - bounds.top
        };
    }, formsId);
    for (const width of [1440, 1024, 390]) {
        I.resizeWindow(width, 1000);
        await openFixture(I, false, [
            { id: 'edit-autotest-approvals', type: 'approvals', size: '1x1', options: {} },
            { id: 'edit-autotest-errors', type: 'errors', size: '1x1', options: {} }
        ]);
        I.dontSeeElementInDOM('.md-dashboard__resize');
        I.dontSeeElement('.md-dashboard__layout .md-dashboard__widget-header > .ti');
        I.dontSeeElement('.md-dashboard__layout .md-dashboard__title-link > .ti');
        I.dontSeeElement('.md-dashboard__layout .md-dashboard__header-link');
        const pageHeading = `[data-instance-id="${pagesId}"] .md-dashboard__title-link`;
        I.assertEqual(await I.grabAttributeFrom(pageHeading, 'href'), '/admin/v9/webpages/web-pages-list/');
        const alignment = await I.executeScript(() => [...document.querySelectorAll('.md-dashboard__layout .md-dashboard__widget-header')].map(header => {
            const range = document.createRange();
            range.selectNodeContents(header.querySelector('.md-dashboard__widget-title span'));
            const text = [...range.getClientRects()].at(-1);
            const grip = header.querySelector('.md-dashboard__drag > .ti').getBoundingClientRect();
            const menu = header.querySelector('.md-dashboard__widget-controls button > .ti').getBoundingClientRect();
            return { id: header.parentElement.dataset.instanceId, grip: grip.bottom - text.bottom, menu: menu.bottom - text.bottom };
        }));
        I.assertTrue(alignment.every(row => Math.abs(row.grip) <= 2 && Math.abs(row.menu) <= 2), `Header controls must align with the last text line at ${width}px: ${JSON.stringify(alignment)}`);
        I.moveCursorTo('.md-dashboard__reset');
        I.assertEqual(await I.grabCssPropertyFrom('.md-dashboard__reset', 'color'), 'rgb(19, 21, 27)', 'Reset hover must keep dark readable text');
        I.saveScreenshot(`dashboard-edit-headers-${width}.png`);
        const editingGeometry = await formsGeometry();
        I.clickCss('.md-dashboard__cancel');
        I.waitForElement('.md-dashboard:not(.is-editing)', 10);
        const viewingGeometry = await formsGeometry();
        I.assertTrue(Object.keys(viewingGeometry).every(key => Math.abs(editingGeometry[key] - viewingGeometry[key]) < 0.1),
            `Editing must preserve the compact header height and title/content positions at ${width}px: ${JSON.stringify({ viewingGeometry, editingGeometry })}`);
        I.seeElement(`${pageHeading} > .ti`);
        I.seeElement(`[data-instance-id="${formsId}"] .md-dashboard__widget-header > .ti`);
        I.dontSeeElementInDOM(`[data-instance-id="${pagesId}"] .md-dashboard__header-link`);
    }
    I.wjSetDefaultWindowSize();
});

Scenario('Widget controls have neutral active states, outlined hover and accessible tooltips', async ({ I }) => {
    await openFixture(I);
    const grip = `[data-instance-id="${formsId}"] .md-dashboard__drag`;
    const menu = `[data-instance-id="${formsId}"] [data-bs-toggle="dropdown"]`;
    I.moveCursorTo('.md-dashboard__toolbar-heading');
    for (const control of [grip, menu]) {
        I.assertEqual(await I.grabCssPropertyFrom(control, 'border-color'), 'rgba(0, 0, 0, 0)', 'Header controls must not have a visible resting border');
        I.assertEqual(await I.grabCssPropertyFrom(control, 'background-color'), 'rgba(0, 0, 0, 0)');
        I.assertTrue((await I.grabAttributeFrom(control, 'aria-label')).length > 0, 'Icon controls need accessible names');
        const bounds = await I.executeScript(selector => {
            const rect = document.querySelector(selector).getBoundingClientRect();
            return [rect.width, rect.height];
        }, control);
        I.assertTrue(bounds.every(size => size >= 24), 'Header controls need padded pointer targets');
        I.executeScript(selector => document.querySelector(selector).focus(), control);
        I.moveCursorTo(control);
        I.waitForVisible('.tooltip.show', 10);
        I.see(await I.grabAttributeFrom(control, 'aria-label'), '.tooltip.show');
        I.assertEqual(await I.grabCssPropertyFrom(control, 'border-color'), 'rgb(0, 99, 251)');
        I.assertEqual(await I.grabCssPropertyFrom(control, 'background-color'), 'rgba(0, 0, 0, 0)');
        if (control === menu) {
            I.saveScreenshot('dashboard-widget-controls-hover.png');
        } else {
            I.saveScreenshot('dashboard-widget-grip-hover.png');
        }
        I.pressKey('Escape');
        I.waitForInvisible('.tooltip.show', 10);
    }
    I.assertEqual(await I.grabAttributeFrom(grip, 'aria-describedby'), await I.grabAttributeFrom('.md-dashboard > p.visually-hidden', 'id'), 'Tooltip dismissal must restore the keyboard movement hint');
    I.clickCss(menu);
    I.waitForVisible(`[data-instance-id="${formsId}"] .dropdown-menu.show`, 10);
    I.waitForInvisible('.tooltip.show', 10);
    I.assertEqual(await I.grabAttributeFrom(menu, 'aria-expanded'), 'true');
    I.moveCursorTo('.md-dashboard__toolbar-heading');
    I.assertEqual(await I.grabCssPropertyFrom(menu, 'background-color'), 'rgba(0, 0, 0, 0)');
    I.assertEqual(await I.grabCssPropertyFrom(menu, 'border-color'), 'rgb(0, 99, 251)');
    I.saveScreenshot('dashboard-widget-controls-menu.png');
    I.pressKey('Escape');
    I.waitForInvisible(`[data-instance-id="${formsId}"] .dropdown-menu.show`, 10);
    I.assertEqual(await I.grabAttributeFrom(menu, 'aria-expanded'), 'false');
    I.moveCursorTo('.md-dashboard__toolbar-heading');
    I.waitForFunction(([selector]) => getComputedStyle(document.querySelector(selector)).borderColor === 'rgba(0, 0, 0, 0)', [menu], 10);
    I.assertEqual(await I.grabCssPropertyFrom(menu, 'border-color'), 'rgba(0, 0, 0, 0)', 'Closing the menu and leaving the button must remove its blue border');
    // A held pointer is required to inspect the grip's active state before mouseup.
    await I.usePlaywrightTo('hold the widget grip', async ({ page }) => {
        const rect = await page.locator(grip).boundingBox();
        await page.mouse.move(rect.x + rect.width / 2, rect.y + rect.height / 2);
        await page.mouse.down();
    });
    I.assertEqual(await I.grabCssPropertyFrom(grip, 'background-color'), 'rgba(0, 0, 0, 0)', 'A held grip must retain a transparent background');
    I.saveScreenshot('dashboard-widget-controls-grip.png');
    await I.usePlaywrightTo('release the widget grip', async ({ page }) => { await page.mouse.up(); });
    I.pressKey('Escape');
    I.dontSeeElement('.md-dashboard__widget-drag-helper');
    I.clickCss('.md-dashboard__cancel');
    I.waitForElement('.md-dashboard:not(.is-editing)', 10);
    I.dontSeeElement('.tooltip.show');
});

Scenario('Widget removal stays provisional, supports Undo and Ctrl Z, and Cancel confirms discarding', async ({ I }) => {
    await openFixture(I);
    I.seeElement('.md-dashboard__notices:not([inert])');
    I.assertDeepEqual(await I.executeScript(() => {
        const style = getComputedStyle(document.querySelector('.md-dashboard__notices'));
        return [style.opacity, style.pointerEvents];
    }), ['1', 'auto'], 'System notices must retain their normal appearance and interactions during editing');
    I.executeScript(() => document.querySelector('.md-dashboard__notice-action').focus());
    I.waitForFunction(() => document.activeElement === document.querySelector('.md-dashboard__notice-action'), 10);
    I.saveScreenshot('dashboard-active-notices.png');
    I.clickCss(`[data-instance-id="${formsId}"] .dropdown > button`);
    I.clickCss(`[data-instance-id="${formsId}"] [data-dashboard-action="remove"]`);
    I.dontSeeElementInDOM(`[data-instance-id="${formsId}"]`);
    I.seeElement('[data-dashboard-widget-undo]');
    I.assertEqual(mutations.length, 0, 'Removing a widget must not persist the draft');
    I.clickCss('[data-dashboard-widget-undo]');
    I.seeElement(`[data-instance-id="${formsId}"]`);
    focusGrip(I, formsId);
    I.pressKey('Space');
    I.pressKey('ArrowLeft');
    I.pressKey('Enter');
    I.pressKey(['Control', 'z']);
    I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__layout [data-instance-id]').dataset.instanceId), trafficId);
    I.clickCss(`[data-instance-id="${formsId}"] .dropdown > button`);
    I.clickCss(`[data-instance-id="${formsId}"] [data-dashboard-action="remove"]`);
    I.clickCss('.md-dashboard__cancel');
    I.waitForVisible('.md-dashboard-modal--confirm', 10);
    I.waitForFunction(() => document.activeElement === document.querySelector('.md-dashboard-modal--confirm .modal-footer .btn-outline-secondary'), 10);
    I.clickCss('.md-dashboard-modal--confirm .btn-danger');
    I.waitForInvisible('.md-dashboard-modal--confirm', 10);
    I.dontSeeElement('.md-dashboard.is-editing');
    I.seeElement(`[data-instance-id="${formsId}"]`);
    I.assertEqual(mutations.length, 0, 'Discarding must preserve the saved profile');
});

Scenario('Settings retain the size grid in a centered modal and a rejected Save keeps changes for retry', async ({ I, a11y }) => {
    await openFixture(I);
    await showWidget(I, pagesId);
    I.clickCss(`[data-instance-id="${pagesId}"] .dropdown > button`);
    I.clickCss(`[data-instance-id="${pagesId}"] [data-dashboard-action="settings"]`);
    I.waitForVisible('.md-dashboard-modal--settings.show', 10);
    I.waitForEnabled('.md-dashboard-modal--settings .btn-primary', 20);
    I.seeElement('.modal-backdrop');
    I.seeNumberOfElements('.md-dashboard-modal--settings .md-dashboard__size-grid > span', 18);
    I.waitForFunction(() => {
        const rect = document.querySelector('.md-dashboard-modal--settings .modal-content').getBoundingClientRect();
        return Math.abs(rect.x + rect.width / 2 - window.innerWidth / 2) < 2 && Math.abs(rect.y + rect.height / 2 - window.innerHeight / 2) < 2;
    }, 10);
    await a11y.check('.md-dashboard-modal--settings');
    I.clickCss('.md-dashboard-modal--settings .bootstrap-select:has(select[id^="dashboard-size-"]) > button');
    I.waitForVisible('.md-dashboard-modal--settings .bootstrap-select .dropdown-menu.show', 10);
    I.saveScreenshot('dashboard-restored-settings.png');
    I.click(locate('.md-dashboard-modal--settings .dropdown-item').withText('2 × 3'));
    I.clickCss('.md-dashboard-modal--settings .btn-primary');
    I.waitForInvisible('.md-dashboard-modal--settings', 10);
    I.seeElement(`[data-instance-id="${pagesId}"][data-size="2x3"]`);
    I.assertEqual(mutations.length, 0);
    rejectSave = true;
    I.clickCss(editButton);
    waitForSave(I);
    await I.waitForVisible('.md-dashboard__status .text-danger', 10);
    I.seeElement('.md-dashboard.is-editing');
    I.seeElement(`[data-instance-id="${pagesId}"][data-size="2x3"]`);
    I.assertEqual(saved.items.find(item => item.id === pagesId).size, '3x2');
    rejectSave = false;
    I.clickCss(editButton);
    waitForSave(I);
    I.waitForElement('.md-dashboard:not(.is-editing)', 10);
    await I.waitForVisible('#toast-container-webjet .toast-success', 10);
    I.assertEqual(saved.items.find(item => item.id === pagesId).size, '2x3');
});

Scenario('Pointer movement uses a raised helper and dashed target and Escape restores the layout', async ({ I }) => {
    await openFixture(I);
    focusGrip(I, trafficId);
    // Intermediate pointer positions exercise the midpoint threshold and cancellation during a held gesture.
    await I.usePlaywrightTo('drag a widget across a target midpoint', async ({ page }) => {
        const source = await page.locator(`[data-instance-id="${formsId}"] .md-dashboard__drag`).boundingBox();
        const target = await page.locator(`[data-instance-id="${trafficId}"]`).boundingBox();
        await page.mouse.move(source.x + source.width / 2, source.y + source.height / 2);
        await page.mouse.down();
        await page.mouse.move(source.x - 12, source.y, { steps: 3 });
        const unchangedTarget = await page.locator(`[data-instance-id="${trafficId}"]`).boundingBox();
        if (Math.abs(unchangedTarget.y - target.y) > 2) throw new Error('Lifting must not shift surrounding widgets before crossing a target');
        await page.mouse.move(target.x + target.width / 4, target.y + 40, { steps: 12 });
    });
    I.seeElement('.md-dashboard__widget-drag-helper');
    I.seeElement('.is-widget-placeholder[data-drop-label]');
    I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__layout [data-instance-id]').dataset.instanceId), formsId);
    I.pressKey('Escape');
    await I.usePlaywrightTo('release the cancelled pointer gesture', async ({ page }) => { await page.mouse.up(); });
    I.dontSeeElement('.md-dashboard__widget-drag-helper');
    I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__layout [data-instance-id]').dataset.instanceId), trafficId);
    I.assertEqual(mutations.length, 0);
    I.clickCss('.md-dashboard__cancel');
    I.waitForElement('.md-dashboard:not(.is-editing)', 10);
});

Scenario('The edit toolbar stays below the header while scrolling and keyboard movement works at all grid breakpoints', async ({ I }) => {
    for (const width of [1440, 1024, 390]) {
        I.resizeWindow(width, 1000);
        await openFixture(I, true);
        focusGrip(I, formsId);
        I.pressKey('Space');
        I.pressKey('ArrowLeft');
        I.pressKey('Enter');
        I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__layout [data-instance-id]').dataset.instanceId), formsId);
        I.executeScript(() => {
            window.scrollbarMain.setMomentum(0, 0);
            window.scrollbarMain.setPosition(0, window.scrollbarMain.limit.y);
        });
        I.waitForFunction(() => {
            window.scrollbarMain.setPosition(0, window.scrollbarMain.limit.y);
            const bounds = document.querySelector('.md-dashboard__toolbar').getBoundingClientRect();
            return bounds.top >= 47 && bounds.top <= 49;
        }, 10);
        I.seeElement(editButton);
        I.seeElement('.md-dashboard__cancel');
        I.assertTrue(await I.executeScript(() => {
            const card = document.querySelector('.md-dashboard__layout [data-instance-id]');
            const rect = card.getBoundingClientRect();
            return rect.left >= -1 && rect.right <= window.innerWidth + 1;
        }), 'Editing widgets must stay within the viewport');
        I.clickCss('.md-dashboard__cancel');
        I.waitForVisible('.md-dashboard-modal--confirm', 10);
        I.clickCss('.md-dashboard-modal--confirm .btn-danger');
        I.waitForInvisible('.md-dashboard-modal--confirm', 10);
    }
    I.wjSetDefaultWindowSize();
});

Scenario('Restore normal dashboard routes after edit-mode checks', async ({ I }) => {
    for (const route of [dashboardPageRoute, settingsRoute, resetRoute]) await I.stopMockingRoute(route);
    I.wjSetDefaultWindowSize();
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});
