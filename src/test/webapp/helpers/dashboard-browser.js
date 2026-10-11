/** Scrolls through the administration's transformed content and waits for the requested card. */
function showWidget(I, id) {
    I.executeScript(id => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        const inset = 64 + (document.querySelector('.md-dashboard.is-editing .md-dashboard__toolbar')?.offsetHeight || 0);
        const top = document.querySelector(`[data-instance-id="${id}"]`).getBoundingClientRect().top;
        if (scrollbar.limit.y > 0) scrollbar.setPosition(0, scrollbar.offset.y + top - inset);
        else window.scrollTo(0, window.scrollY + top - inset);
    }, id);
    return I.waitForFunction(id => {
        const card = document.querySelector(`[data-instance-id="${id}"]`);
        const bounds = card.getBoundingClientRect();
        const inset = 64 + (document.querySelector('.md-dashboard.is-editing .md-dashboard__toolbar')?.offsetHeight || 0);
        if (bounds.bottom <= inset || bounds.top >= window.innerHeight) {
            // Natural-height mobile cards can move the target as preceding widgets finish loading.
            const scrollbar = window.scrollbarMain;
            scrollbar.update();
            if (scrollbar.limit.y > 0) scrollbar.setPosition(0, scrollbar.offset.y + bounds.top - inset);
            else window.scrollTo(0, window.scrollY + bounds.top - inset);
            return false;
        }
        return card.querySelector('.md-dashboard__widget-body').getAttribute('aria-busy') === 'false';
    }, [id], 30);
}

/** Visits pending cards for tests that inspect the complete dashboard, then restores the scroll position. */
async function waitForWidgets(I) {
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    const { position, pending } = await I.executeScript(() => ({
        position: { smooth: window.scrollbarMain.offset.y, native: window.scrollY },
        pending: [...document.querySelectorAll('.md-dashboard__widget')]
            .filter(card => card.querySelector('.md-dashboard__widget-body').getAttribute('aria-busy') !== 'false')
            .map(card => card.dataset.instanceId)
    }));
    for (const id of pending) await showWidget(I, id);
    await I.executeScript(position => {
        window.scrollbarMain.setMomentum(0, 0);
        window.scrollbarMain.setPosition(0, position.smooth);
        window.scrollTo(0, position.native);
    }, position);
}

const dashboardPageRoute = '**/admin/v9/';

/** Overrides embedded data in the HTML response without changing the account's stored preferences. */
async function mockDashboardBootstrap(I, readData, readNoticePreferences, pageRoute = dashboardPageRoute) {
    await I.stopMockingRoute(pageRoute);
    await I.mockRoute(pageRoute, async route => {
        const response = await route.fetch();
        const html = await response.text();
        const marker = /window\.webjetOverviewDashboardBootstrapData = JSON\.parse\([^\n]+\);/;
        if (!marker.test(html)) throw new Error('The dashboard bootstrap assignment must exist in the HTML response.');
        const data = JSON.stringify(readData()).replace(/</g, '\\u003c').replace(/\u2028/g, '\\u2028').replace(/\u2029/g, '\\u2029');
        let body = html.replace(marker, match => `${match}\nObject.assign(window.webjetOverviewDashboardBootstrapData, ${data});`);
        if (readNoticePreferences) {
            const userMarker = /window\.currentUser = JSON\.parse\([^\n]+\);/;
            if (!userMarker.test(html)) throw new Error('The current-user assignment must exist in the HTML response.');
            const preferences = JSON.stringify(JSON.stringify(readNoticePreferences())).replace(/</g, '\\u003c');
            body = body.replace(userMarker, match => `${match}\nwindow.currentUser.adminSettings['dashboard.notices'] = ${preferences};`);
        }
        return route.fulfill({ response, body });
    });
}

/** Runs through I.executeScript to read fresh server preferences and sessions from the dashboard HTML. */
async function readDashboardBootstrap(query = '') {
    const response = await fetch(`/admin/v9/${query}`, { credentials: 'same-origin' });
    if (!response.ok) throw new Error(`Dashboard page: ${response.status}`);
    const document = new DOMParser().parseFromString(await response.text(), 'text/html');
    const script = [...document.scripts].find(script => script.textContent.includes('window.webjetOverviewDashboardBootstrapData ='));
    const json = script.textContent.match(/window\.webjetOverviewDashboardBootstrapData = JSON\.parse\((.+)\);/)[1];
    return JSON.parse(JSON.parse(json));
}

/** Restores the standard overview through its confirmation and Save controls, then checks persistence. */
async function restoreDefaultDashboard(I) {
    const dashboard = '.md-dashboard[data-loaded="true"]';
    const toolbar = '.md-dashboard__toolbar-actions';
    const dialog = '#toast-container-webjet .toast[role="dialog"]';
    const readLayout = () => {
        const settings = document.querySelector('webjet-overview-dashboard').dashboardController.settings;
        return settings.items.map(({ id, type, size, options }) => ({ type, size, options, domainOptions: settings.domainOptions[id] || {} }));
    };

    I.amOnPage('/admin/v9/');
    I.waitForElement(dashboard, 20);
    I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.setPosition(0, scrollbar.offset.y + document.querySelector('.md-dashboard__toolbar').getBoundingClientRect().top - 64);
    });
    I.clickCss(`${toolbar} button[aria-pressed="false"]`);
    I.waitForVisible(`${toolbar} .md-dashboard__reset`, 10);
    I.clickCss(`${toolbar} .md-dashboard__reset`);
    I.waitForVisible(`${dialog} button[id^="confirmationYes"]`, 10);
    I.clickCss(`${dialog} button[id^="confirmationYes"]`);
    I.waitForInvisible(dialog, 10);
    const defaults = await I.executeScript(readLayout);
    I.clickCss(`${toolbar} button[aria-pressed="true"]`);
    I.waitForFunction(() => {
        const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
        return !controller.editing && !controller.saving;
    }, 20);
    I.refreshPage();
    I.waitForElement(dashboard, 20);
    I.assertDeepEqual(await I.executeScript(readLayout), defaults, 'The default overview must remain saved after reloading.');
}

/** Opens sessions from a temporary personal widget without persisting a layout change. */
async function openDashboardSessions(I) {
    await I.executeScript(() => {
        const controller = document.querySelector('webjet-overview-dashboard').dashboardController;
        if (!controller.settings.items.some(item => item.id === 'autotest-session-dialog')) {
            controller.settings.items.unshift({ id: 'autotest-session-dialog', type: 'my-sessions', size: '2x3', options: {} });
            controller._render();
        }
    });
    await showWidget(I, 'autotest-session-dialog');
    I.clickCss('[data-instance-id="autotest-session-dialog"] .md-dashboard__title-action');
}

module.exports = { showWidget, waitForWidgets, mockDashboardBootstrap, dashboardPageRoute, readDashboardBootstrap, restoreDefaultDashboard, openDashboardSessions };
