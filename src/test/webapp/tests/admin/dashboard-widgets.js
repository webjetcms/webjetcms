const assert = require('node:assert/strict');

Feature('admin.dashboard-widgets').tag('@singlethread');

let originalSettings;
let shortcutId;
let shortcutTitle;

function waitForSave(I) {
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard')?.dashboardController?.saving === false, 20);
}

function openAction(I, id, action) {
    I.clickCss(`[data-instance-id="${id}"] .dropdown > button`);
    I.waitForVisible(`[data-instance-id="${id}"] [data-dashboard-action="${action}"]`, 10);
    I.forceClick(`[data-instance-id="${id}"] [data-dashboard-action="${action}"]`);
}

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('webjet-overview-dashboard .md-dashboard__toolbar', 20);
});

Scenario('Authenticated dashboard endpoints and initial overview load', async ({ I }) => {
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    const responses = await I.executeScript(async () => {
        return Promise.all(['/admin/rest/dashboard/settings', '/admin/rest/dashboard/menu', '/admin/rest/dashboard/recent-pages'].map(async url => {
            const response = await fetch(url, { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
            return { url, status: response.status, contentType: response.headers.get('content-type') };
        }));
    });
    responses.forEach(response => assert.equal(response.status, 200, `${response.url} must be available to an authenticated administrator (${response.contentType})`));
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.seeElement('#toast-container-overview');
});

Scenario('Add and configure a personal shortcut and reload its server preferences', async ({ I }) => {
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    originalSettings = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    shortcutTitle = `dashboard-autotest-${I.getRandomText()}`;
    const existingIds = originalSettings.items.map(item => item.id);
    I.clickCss('.md-dashboard__toolbar > button');
    I.waitForVisible('.md-dashboard-modal', 10);
    I.clickCss('.md-dashboard__catalogue-item[data-widget-type="shortcut"] button');
    I.waitForInvisible('.md-dashboard-modal', 10);
    waitForSave(I);
    shortcutId = await I.executeScript(ids => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => !ids.includes(item.id) && item.type === 'shortcut')?.id, existingIds);
    assert.ok(shortcutId, 'Adding a shortcut must create a new stable instance');

    openAction(I, shortcutId, 'settings');
    I.waitForVisible('.md-dashboard-modal .md-dashboard__settings input[type="text"]', 10);
    I.fillField('.md-dashboard-modal .md-dashboard__settings input[type="text"]', shortcutTitle);
    I.seeInField('.md-dashboard-modal .md-dashboard__settings input[type="text"]', shortcutTitle);
    I.clickCss('.md-dashboard-modal .modal-footer .btn-primary');
    I.waitForInvisible('.md-dashboard-modal', 10);
    waitForSave(I);
    I.see(shortcutTitle, `[data-instance-id="${shortcutId}"]`);
    I.refreshPage();
    I.waitForElement(`.md-dashboard[data-loaded="true"] [data-instance-id="${shortcutId}"]`, 20);
    I.see(shortcutTitle, `[data-instance-id="${shortcutId}"]`);
});

Scenario('Move with drag and keyboard controls, collapse and resize without replacing alerts', async ({ I }) => {
    assert.ok(shortcutId, 'The shortcut setup scenario must complete first');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    await I.executeScript(() => { window.autotestDashboardAlerts = document.querySelector('#toast-container-overview'); });
    const firstId = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items[0].id);
    I.dragAndDrop(`[data-instance-id="${shortcutId}"] .md-dashboard__drag`, `[data-instance-id="${firstId}"] .md-dashboard__widget-header`, { force: true, timeout: 10000 });
    waitForSave(I);
    const movedFirst = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items[0].id);
    assert.equal(movedFirst, shortcutId, 'Dragging before another card must update the persisted order');
    I.clickCss(`[data-instance-id="${shortcutId}"] .md-dashboard__drag`);
    I.waitForVisible('.md-dashboard-modal select', 10);
    I.selectOption('.md-dashboard-modal select', '');
    I.clickCss('.md-dashboard-modal .modal-footer .btn-primary');
    I.waitForInvisible('.md-dashboard-modal', 10);
    waitForSave(I);
    const lastId = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.at(-1).id);
    assert.equal(lastId, shortcutId, 'Keyboard movement must update the persisted order');

    const recentId = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.type === 'recent-pages')?.id);
    assert.ok(recentId, 'The editor default must include recent pages');
    const wasCollapsed = await I.executeScript(id => document.querySelector('webjet-overview-dashboard').dashboardController.settings.items.find(item => item.id === id).collapsed, recentId);
    if (wasCollapsed) {
        openAction(I, recentId, 'collapse');
        waitForSave(I);
        I.waitForElement(`[data-instance-id="${recentId}"]:not(.is-collapsed)`, 10);
    }
    openAction(I, recentId, 'collapse');
    waitForSave(I);
    I.waitForElement(`[data-instance-id="${recentId}"].is-collapsed`, 10);
    I.seeElement(`[data-instance-id="${recentId}"] .md-dashboard-widget__more`);
    openAction(I, recentId, 'collapse');
    waitForSave(I);
    I.waitForElement(`[data-instance-id="${recentId}"]:not(.is-collapsed)`, 10);
    openAction(I, recentId, 'settings');
    I.waitForVisible('.md-dashboard-modal select', 10);
    I.selectOption('.md-dashboard-modal select', '2 × 3');
    I.clickCss('.md-dashboard-modal .modal-footer .btn-primary');
    I.waitForInvisible('.md-dashboard-modal', 10);
    waitForSave(I);
    I.waitForElement(`[data-instance-id="${recentId}"][data-size="2x3"]`, 10);
    const alertsPreserved = await I.executeScript(() => window.autotestDashboardAlerts === document.querySelector('#toast-container-overview'));
    assert.equal(alertsPreserved, true, 'Preference changes must preserve the active system alert container');
});

Scenario('A second authenticated session reads the persisted personal dashboard', async ({ I }) => {
    assert.ok(shortcutId, 'The shortcut setup scenario must complete first');
    await session('dashboard preferences autotest', async () => {
        I.amOnPage('/admin/logon/');
        I.relogin('admin', false);
        I.amOnPage('/admin/v9/');
        I.waitForElement(`.md-dashboard[data-loaded="true"] [data-instance-id="${shortcutId}"]`, 20);
        I.see(shortcutTitle, `[data-instance-id="${shortcutId}"]`);
        I.logout();
    });
});

Scenario('Remove and undo restores the configured instance', async ({ I }) => {
    assert.ok(shortcutId, 'The shortcut setup scenario must complete first');
    I.waitForElement(`.md-dashboard[data-loaded="true"] [data-instance-id="${shortcutId}"]`, 20);
    openAction(I, shortcutId, 'remove');
    waitForSave(I);
    I.waitForInvisible(`[data-instance-id="${shortcutId}"]`, 10);
    I.clickCss('.md-dashboard__undo button');
    waitForSave(I);
    I.waitForElement(`[data-instance-id="${shortcutId}"]`, 10);
    I.see(shortcutTitle, `[data-instance-id="${shortcutId}"]`);
});

Scenario('A rejected preference update preserves the confirmed widget', async ({ I }) => {
    assert.ok(shortcutId, 'The shortcut setup scenario must complete first');
    I.waitForElement(`.md-dashboard[data-loaded="true"] [data-instance-id="${shortcutId}"]`, 20);
    await I.mockRoute('**/admin/rest/dashboard/settings', route => route.request().method() === 'PUT'
        ? route.fulfill({ status: 500, contentType: 'application/json', body: '{"error":"autotest simulated save failure"}' })
        : route.continue());
    openAction(I, shortcutId, 'remove');
    waitForSave(I);
    I.waitForElement('.md-dashboard__status .text-danger', 10);
    I.see(shortcutTitle, `[data-instance-id="${shortcutId}"]`);
    await I.stopMockingRoute('**/admin/rest/dashboard/settings');
});

Scenario('Responsive grid preserves visual order and keeps widgets inside the dashboard', async ({ I }) => {
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.waitForFunction(() => [...document.querySelectorAll('.md-dashboard__widget-body')].every(body => body.getAttribute('aria-busy') !== 'true'), 20);
    for (const width of [320, 359, 360, 390, 767, 768, 1024, 1199, 1200, 1337, 1920]) {
        I.resizeWindow(width, 1000);
        const geometry = await I.executeScript(() => {
            const host = document.querySelector('.md-dashboard__layout');
            const boundary = host.getBoundingClientRect();
            const cards = [...host.querySelectorAll('[data-instance-id]')].map(card => {
                const rect = card.getBoundingClientRect();
                return { id: card.dataset.instanceId, left: rect.left, top: rect.top, right: rect.right, bottom: rect.bottom };
            });
            const clippedCells = [...host.querySelectorAll('.md-dashboard-widget__table td, .md-dashboard-widget__table th')]
                .map((cell, index) => ({ index, type: cell.closest('[data-widget-type]').dataset.widgetType, whiteSpace: getComputedStyle(cell).whiteSpace, width: cell.clientWidth, contentWidth: cell.scrollWidth }))
                .filter(cell => cell.whiteSpace !== 'normal' || cell.contentWidth > cell.width + 1);
            return { left: boundary.left, right: boundary.right, cards, clippedCells };
        });
        for (const card of geometry.cards) {
            assert.ok(card.left >= geometry.left - 1 && card.right <= geometry.right + 1, `Widget ${card.id} must fit at ${width}px`);
        }
        const visual = [...geometry.cards].sort((a, b) => Math.abs(a.top - b.top) > 1 ? a.top - b.top : a.left - b.left);
        assert.deepEqual(visual.map(card => card.id), geometry.cards.map(card => card.id), `Visual order must follow DOM order at ${width}px`);
        assert.deepEqual(geometry.clippedCells, [], `Table headers and cells must wrap without clipping at ${width}px`);
    }
    I.wjSetDefaultWindowSize();
    const clipped = await I.executeScript(() => {
        const root = document.querySelector('.md-dashboard__layout');
        const sizes = [...root.querySelectorAll('*')].map(element => [element, parseFloat(getComputedStyle(element).fontSize)]);
        sizes.forEach(([element, size]) => { element.style.fontSize = `${size * 2}px`; });
        const bodies = [...root.querySelectorAll('.md-dashboard__widget-body')]
            .filter(body => !body.hidden && body.scrollHeight > body.clientHeight + 1)
            .map(body => body.closest('[data-instance-id]').dataset.instanceId);
        const cells = [...root.querySelectorAll('.md-dashboard-widget__table td, .md-dashboard-widget__table th')]
            .map((cell, index) => ({ index, type: cell.closest('[data-widget-type]').dataset.widgetType, whiteSpace: getComputedStyle(cell).whiteSpace, width: cell.clientWidth, contentWidth: cell.scrollWidth }))
            .filter(cell => cell.whiteSpace !== 'normal' || cell.contentWidth > cell.width + 1);
        return { bodies, cells };
    });
    assert.deepEqual(clipped.bodies, [], 'Widget bodies must remain readable with text enlarged to 200 percent');
    assert.deepEqual(clipped.cells, [], 'Table headers and cells must wrap without clipping with text enlarged to 200 percent');
});

Scenario('Restore the initial personal dashboard preferences', async ({ I }) => {
    await I.stopMockingRoute('**/admin/rest/dashboard/settings');
    if (!originalSettings) return;
    const status = await I.executeScript(async settings => {
        const response = await fetch('/admin/rest/dashboard/settings', {
            method: 'PUT', credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
            body: JSON.stringify(settings)
        });
        return response.status;
    }, originalSettings);
    assert.equal(status, 200, 'The original effective dashboard preferences must be restored');
    I.refreshPage();
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    if (shortcutId) I.dontSeeElement(`[data-instance-id="${shortcutId}"]`);
});
