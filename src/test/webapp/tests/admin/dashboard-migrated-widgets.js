const { waitForWidgets } = require('../../helpers/dashboard-browser');

Feature('admin.dashboard-migrated-widgets').tag('@singlethread');

const migratedWidgets = [
    ['changed-pages', '3x3'], ['audit', '3x3'], ['server-memory', '3x3'], ['server-cpu', '3x3'], ['logged-admins', '2x2']
];

function waitForSave(I) {
    I.waitForFunction(() => document.querySelector('webjet-overview-dashboard')?.dashboardController?.saving === false, 20);
}

async function widgetAction(I, id, action) {
    await I.clickIfVisible('.md-dashboard__toolbar-actions button[aria-pressed="false"]');
    I.clickCss(`[data-instance-id="${id}"] .dropdown > button`);
    I.waitForVisible(`[data-instance-id="${id}"] [data-dashboard-action="${action}"]`, 10);
    I.forceClick(`[data-instance-id="${id}"] [data-dashboard-action="${action}"]`);
}

async function waitForChart(I, type) {
    await waitForWidgets(I);
    return I.waitForFunction(([type]) => {
        const host = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
        return Boolean(host && host.querySelector('canvas') && window.am5?.registry.rootElements.some(root => root.dom === host));
    }, [type], 20);
}

async function rememberChart(I, type) {
    return I.executeScript(type => {
        const host = document.querySelector(`[data-widget-type="${type}"] .md-dashboard-widget__chart`);
        window.autotestMigratedChart = window.am5.registry.rootElements.find(root => root.dom === host);
        return host.id;
    }, type);
}

async function assertDisposedChart(I) {
    I.assertTrue(await I.executeScript(() => window.autotestMigratedChart.isDisposed()
        && !window.am5.registry.rootElements.includes(window.autotestMigratedChart)), 'Replaced monitoring charts must release their AmCharts root.');
}

Before(async ({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    await waitForWidgets(I);
});

Scenario('Migrated overview widgets persist independently and clean up monitoring charts', async ({ I }) => {
    const original = await I.executeScript(() => JSON.parse(JSON.stringify(document.querySelector('webjet-overview-dashboard').dashboardController.settings)));
    try {
        const applied = await I.executeScript(async definitions => {
            const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
            const next = JSON.parse(JSON.stringify(controller.settings));
            const chosen = [];
            for (const [type, size] of definitions) {
                let item = next.items.find(item => item.type === type);
                if (!item) {
                    item = { id: `migrated-autotest-${type}`, type, size, options: {} };
                    next.items.push(item);
                }
                item.size = size;
                    chosen.push(item);
            }
            next.items = [...next.items.filter(item => !definitions.some(([type]) => item.type === type)), ...chosen];
            return { saved: await controller._commit(next), items: next.items.filter(item => definitions.some(([type]) => item.type === type)) };
        }, migratedWidgets);
        I.assertTrue(applied.saved, 'The migrated widget fixture must fit the account profile and persist successfully.');
        I.waitForText(await I.executeScript(() => WJ.translate('admin.dashboard.saved.js')), 10, '#toast-container-webjet .toast-success');
        I.assertEqual(await I.executeScript(() => document.querySelector('.md-dashboard__status').textContent), '', 'Saved preferences must use the standard notification instead of persistent inline text.');
        I.toastrClose();
        await waitForWidgets(I);
        I.refreshPage();
        await waitForWidgets(I);
        const reloaded = await I.executeScript(() => document.querySelector('webjet-overview-dashboard').dashboardController.settings);
        for (const item of applied.items) {
            I.assertDeepEqual(reloaded.items.find(saved => saved.id === item.id), item, `${item.type} must retain its stable instance and size across reload.`);
            I.seeElementInDOM(`.md-dashboard__layout [data-instance-id="${item.id}"]`);
            I.dontSeeElementInDOM(`[data-instance-id="${item.id}"] .md-dashboard__widget-content > .text-danger:not([hidden])`);
        }
        I.dontSeeElementInDOM('.md-dashboard__legacy');
        I.dontSeeElementInDOM('#webjet-overview-dashboard .bookmark');
        I.seeElementInDOM('.md-dashboard__shortcut-actions button[aria-pressed]');
        for (const type of ['server-memory', 'server-cpu']) {
            await waitForChart(I, type);
            I.seeElementInDOM(`[data-widget-type="${type}"] .md-dashboard-widget__chart[role="img"][aria-label]`);
            I.seeElementInDOM(`[data-widget-type="${type}"] .visually-hidden .md-dashboard-widget__table tbody tr`);
        }
        const ids = Object.fromEntries(applied.items.map(item => [item.type, item.id]));
        const firstMemory = await rememberChart(I, 'server-memory');
        await widgetAction(I, ids['server-memory'], 'refresh');
        await waitForWidgets(I);
        await waitForChart(I, 'server-memory');
        await assertDisposedChart(I);
        I.assertNotEqual(await I.grabAttributeFrom('[data-widget-type="server-memory"] .md-dashboard-widget__chart', 'id'), firstMemory);

        await rememberChart(I, 'server-cpu');
        await widgetAction(I, ids['server-cpu'], 'remove');
        waitForSave(I);
        I.waitForInvisible(`[data-instance-id="${ids['server-cpu']}"]`, 10);
        await assertDisposedChart(I);
        I.clickCss('.md-dashboard__undo button');
        waitForSave(I);
        await waitForChart(I, 'server-cpu');
        I.assertTrue(await I.executeScript(() => {
            const hosts = [...document.querySelectorAll('.md-dashboard-widget__chart')];
            const roots = window.am5.registry.rootElements.filter(root => root.dom.id.startsWith('dashboard-chart-'));
            return roots.length === hosts.length && hosts.every(host => roots.filter(root => root.dom === host).length === 1);
        }), 'Every current dashboard chart must own exactly one live root.');
        I.clickCss('.md-dashboard__toolbar-actions button[aria-pressed="true"]');
        I.waitForElement('.md-dashboard:not(.is-editing)', 10);
        for (const width of [1337, 390]) {
            I.resizeWindow(width, 1052);
            if (width < 768 && await I.executeScript(() => document.querySelector('.ly-sidebar')?.classList.contains('active'))) I.clickCss('.js-sidebar-toggler');
            if (width < 768) I.waitForFunction(() => document.querySelector('.ly-sidebar').getBoundingClientRect().right <= 1, 10);
            const overflow = await I.executeScript(types => {
                const boundary = document.querySelector('.md-dashboard__layout').getBoundingClientRect();
                return types.flatMap(([type]) => {
                    const card = document.querySelector(`[data-widget-type="${type}"]`);
                    const bounds = card.getBoundingClientRect();
                    return bounds.left < boundary.left - 1 || bounds.right > boundary.right + 1 || card.scrollWidth > card.clientWidth + 1 ? [type] : [];
                });
            }, migratedWidgets);
            I.assertDeepEqual(overflow, [], `Migrated cards must remain inside the dashboard at ${width}px.`);
            for (const type of ['changed-pages', 'server-memory', 'logged-admins']) {
                I.executeScript(type => {
                    const scrollbar = window.scrollbarMain;
                    scrollbar.setMomentum(0, 0);
                    scrollbar.update();
                    scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector(`[data-widget-type="${type}"]`).getBoundingClientRect().top - 64);
                }, type);
                I.saveScreenshot(`dashboard-migrated-${type}-${width}.png`, true);
            }
        }
    } finally {
        I.wjSetDefaultWindowSize();
        const restored = await I.executeScript(async settings => {
            const response = await fetch('/admin/rest/dashboard/settings', {
                method: 'PUT', credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken }, body: JSON.stringify(settings)
            });
            return response.status;
        }, original);
        I.assertEqual(restored, 200, 'The original dashboard and active-domain options must be restored.');
        I.refreshPage();
        await waitForWidgets(I);
    }
});
