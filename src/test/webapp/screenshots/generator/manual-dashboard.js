const { waitForWidgets } = require('../../helpers/dashboard-browser');

Feature('manual-dashboard');

const settingsRoute = '**/admin/rest/dashboard/settings';
const editButton = '.md-dashboard__toolbar-actions > button[aria-pressed]';
const traffic = '[data-widget-type="traffic"]';
const dialog = '.md-dashboard-modal';

Before(async ({ I, login }) => {
    login('admin');
    // Render the current defaults and keep screenshot-only preferences out of the stored profile.
    let settings = { version: 1, configured: false, legacyBookmarksHandled: true, items: [], domainOptions: {} };
    await I.mockRoute(settingsRoute, route => {
        if (route.request().method() === 'PUT') settings = { ...route.request().postDataJSON(), configured: true };
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(settings) });
    });
    I.amOnPage('/admin/v9/');
    I.resizeWindow(1440, 1100);
    await waitForWidgets(I);
    I.waitForFunction(() => document.querySelector('.md-dashboard__notice-list')?.getAttribute('aria-busy') === 'false', 30);
});

/** Places the overview toolbar and the first widget row together in the viewport. */
function showOverview(I) {
    I.executeScript(() => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        const top = document.querySelector('.md-dashboard__toolbar').getBoundingClientRect().top;
        scrollbar.setPosition(0, scrollbar.offset.y + top - 80);
    });
}

function closeDialog(I) {
    I.clickCss(`${dialog} .btn-close`);
    I.waitForDetached(dialog, 10);
}

Scenario('Overview, catalogue and widget settings', async ({ I, Document }) => {
    I.clickCss('.md-dashboard-widget__news-toggle');
    I.waitForVisible('.is-news-collapsed .md-dashboard-widget__news-summary', 10);
    I.waitForVisible('div.toast-container .toast-close-button', 10);
    Document.notifyClose();
    I.waitForInvisible('div.toast-container .toast', 10);
    I.moveCursorTo('.ly-header');
    Document.screenshot('/redactor/admin/dashboard.png');

    showOverview(I);
    I.clickCss(editButton);
    I.waitForElement('.md-dashboard.is-editing', 10);
    I.clickCss(`${traffic} [data-bs-toggle="dropdown"]`);
    I.waitForVisible(`${traffic} .dropdown-menu.show`, 10);
    I.moveCursorTo('.md-dashboard__title');
    Document.screenshot('/redactor/admin/dashboard-edit.png');

    I.clickCss(`${traffic} [data-dashboard-action="settings"]`);
    I.waitForVisible(`${dialog}.show .md-dashboard__settings select`, 10);
    I.waitForEnabled(`${dialog} .modal-footer .btn-primary`, 10);
    Document.screenshotElement(`${dialog} .modal-content`, '/redactor/admin/dashboard-widget-settings.png');
    closeDialog(I);

    I.clickCss('.md-dashboard__toolbar .md-dashboard__edit-control.btn-primary');
    I.waitForVisible(`${dialog}.show .md-dashboard__catalogue-item`, 10);
    I.resizeWindow(1440, 1800);
    Document.screenshotElement(`${dialog} .modal-content`, '/redactor/admin/dashboard-catalogue.png');
    closeDialog(I);
    I.resizeWindow(1440, 1100);
    I.clickCss(editButton);
    I.stopMockingRoute(settingsRoute);
    I.wjSetDefaultWindowSize();
});

Scenario('Shortcut settings and feedback', async ({ I, Document }) => {
    I.clickCss('.md-dashboard__shortcut-actions button[aria-pressed]');
    I.clickCss('.md-dashboard__shortcut-actions button.btn-primary:not([aria-pressed])');
    I.waitForVisible(`${dialog}.show`, 10);
    // Use the translated option text from the real select to operate its Bootstrap picker in any language.
    const urlLabel = await I.grabTextFrom('select[name="dashboardShortcutSource"] option[value="url"]');
    I.clickCss('select[name="dashboardShortcutSource"] + button');
    I.click(locate('.dropdown-menu.show .dropdown-item').withText(urlLabel));
    I.waitForVisible('input[name="dashboardShortcutUrl"]', 10);
    I.fillField('input[name="dashboardShortcutUrl"]', `https://docs.webjetcms.sk/latest/${I.getConfLng()}/`);
    I.fillField('input[name="dashboardShortcutTitle"]', 'WebJET CMS Docs');
    I.fillField('input[name="dashboardShortcutIcon"]', 'book');
    I.clickCss('.md-dashboard__shortcut-swatch:has(input[value="blue"])');
    I.moveCursorTo(`${dialog} .modal-title`);
    Document.screenshotElement(`${dialog} .modal-content`, '/redactor/admin/dashboard-shortcut-settings.png');
    closeDialog(I);
    I.clickCss('.md-dashboard__shortcut-actions button[aria-pressed]');

    showOverview(I);
    Document.screenshotElement('.md-dashboard__toolbar', '/redactor/admin/feedback.png');
    I.clickCss('.md-dashboard__feedback');
    I.waitForVisible('#feedback_modal.show', 10);
    Document.screenshotElement('#feedback_modal .modal-content', '/redactor/admin/feedback-modal.png');
    I.clickCss('#feedback_modal .btn-close-editor');
    I.waitForInvisible('#feedback_modal', 10);
    I.stopMockingRoute(settingsRoute);
    I.wjSetDefaultWindowSize();
});

Scenario('Active sessions', async ({ I, Document }) => {
    await session('dashboard screenshot first login', () => I.relogin('tester3'));
    await session('dashboard screenshot second login', () => I.relogin('tester3'));

    I.relogin('tester3');
    I.amOnPage('/admin/v9/');
    I.resizeWindow(1440, 1100);
    I.waitForFunction(() => document.querySelectorAll('.md-dashboard__sessions .md-dashboard-widget__session').length >= 3, 20);
    I.seeElement('.md-dashboard__sessions .md-dashboard-widget__session-current');
    I.moveCursorTo('.ly-header');
    Document.screenshotElement('.md-dashboard__sessions', '/redactor/admin/sessions.png');

    await session('dashboard screenshot first login', () => I.logout());
    await session('dashboard screenshot second login', () => I.logout());
    I.logout();
    I.stopMockingRoute(settingsRoute);
    I.wjSetDefaultWindowSize();
});
