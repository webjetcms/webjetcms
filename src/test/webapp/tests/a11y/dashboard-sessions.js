const { mockDashboardBootstrap, dashboardPageRoute } = require('../../helpers/dashboard-browser');

Feature('a11y.dashboard-sessions');

const modal = '.md-dashboard-modal--sessions';
const codeRoute = '**/admin/rest/security/login-events/*/code';
const mutationRoutes = ['**/admin/rest/sessions/logout*', '**/admin/rest/security/login-events/*/report', '**/admin/rest/security/login-events/*/confirm'];

Before(({ login }) => { login('admin'); });

After(async ({ I }) => {
    for (const route of [dashboardPageRoute, codeRoute, ...mutationRoutes]) await I.stopMockingRoute(route);
    I.wjSetDefaultWindowSize();
});

/** Measures the rendered colors, including transparent ancestors and CSS color-mix surfaces. */
function buttonContrast(selector) {
    const element = document.querySelector(selector);
    const style = getComputedStyle(element);
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = 1;
    const context = canvas.getContext('2d');
    const paint = color => { context.fillStyle = color; context.fillRect(0, 0, 1, 1); };
    const pixel = () => Array.from(context.getImageData(0, 0, 1, 1).data).slice(0, 3);
    const ancestors = [];
    for (let parent = element.parentElement; parent; parent = parent.parentElement) ancestors.unshift(parent);
    paint('#fff');
    for (const ancestor of ancestors) paint(getComputedStyle(ancestor).backgroundColor);
    const surrounding = pixel();
    paint(style.backgroundColor);
    const background = pixel();
    // Tabler icons can have a different inherited color from their button.
    const icon = element.querySelector('.ti');
    paint(icon && !element.textContent.trim() ? getComputedStyle(icon, '::before').color : style.color);
    const foreground = pixel();
    context.clearRect(0, 0, 1, 1);
    paint(`rgb(${surrounding.join(',')})`);
    paint(style.outlineColor);
    const outline = pixel();
    const luminance = rgb => rgb.map(value => {
        value /= 255;
        return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
    }).reduce((sum, value, index) => sum + value * [0.2126, 0.7152, 0.0722][index], 0);
    const contrast = (a, b) => (Math.max(luminance(a), luminance(b)) + 0.05) / (Math.min(luminance(a), luminance(b)) + 0.05);
    return {
        name: element.getAttribute('aria-label') || element.textContent.trim(),
        foreground, background, textContrast: contrast(foreground, background),
        minimum: element.textContent.trim() ? 4.5 : 3,
        outlineContrast: contrast(outline, surrounding), outlineStyle: style.outlineStyle,
        outlineWidth: parseFloat(style.outlineWidth), focusVisible: element.matches(':focus-visible'),
        hover: element.matches(':hover'), active: element.matches(':active')
    };
}

/** Exercises real pointer and keyboard states without activating account actions. */
async function auditButtons(I, selectors, failures) {
    for (const selector of selectors) {
        const record = async state => {
            await I.waitForFunction(selector => !document.querySelector(selector).getAnimations().some(animation => animation.playState === 'running'), [selector], 5);
            const colors = await I.executeScript(buttonContrast, selector);
            if (colors.textContrast < colors.minimum) failures.push(`${colors.name} / ${state}: ${colors.textContrast.toFixed(2)}:1 < ${colors.minimum}:1`);
            if (state.includes('focus') && (!colors.focusVisible || colors.outlineStyle === 'none' || colors.outlineWidth <= 0 || colors.outlineContrast < 3)) {
                failures.push(`${colors.name} / ${state}: keyboard focus must remain visible with 3:1 contrast`);
            }
            if (state.includes('hover')) I.assertTrue(colors.hover, 'The hover state must be exercised with the pointer.');
            if (state === 'active') I.assertTrue(colors.active, 'The pressed state must be exercised with a held pointer.');
        };
        I.moveCursorTo(`${modal} .modal-title`);
        I.executeScript(() => document.activeElement.blur());
        await record('normal');
        I.moveCursorTo(selector);
        await record('hover');
        // CodeceptJS has no pointer-down/up steps; release outside the control to avoid activating it.
        await I.usePlaywrightTo('hold the button without activating its action', async ({ page }) => { await page.mouse.down(); });
        await record('active');
        I.moveCursorTo(`${modal} .modal-title`);
        await I.usePlaywrightTo('release outside the button', async ({ page }) => { await page.mouse.up(); });
        I.pressKey('Tab');
        I.executeScript(selector => document.querySelector(selector).focus(), selector);
        await record('focus');
    }
    I.moveCursorTo(`${modal} .modal-title`);
    I.executeScript(() => document.activeElement.blur());
}

Scenario('Representative session actions remain readable with pointer and keyboard interaction', async ({ I }) => {
    const now = Date.now();
    const event = { id: 900043, createDate: now, expiresAt: now + 86400000, browserName: 'Firefox autotest', operatingSystem: 'Linux', ipAddress: '192.0.2.2' };
    const data = {
        settings: { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true, items: [], domainOptions: {} },
        requestedSecurityEvent: event, securityEventRequested: true,
        notices: [{ id: 'twoFactor', action: { url: '/admin/2factorauth.jsp' } }],
        currentSessions: { currentSessionId: 'autotest-current', userSessions: [{ userSessions: [
            { sessionId: 'autotest-current', browserName: 'Chrome autotest', deviceId: 900042, deviceConfirmed: true, logonTime: now },
            { sessionId: 'autotest-other', browserName: 'Safari autotest', deviceId: 900044, deviceConfirmed: true, logonTime: now - 1000 },
            { sessionId: 'autotest-new', browserName: 'Firefox autotest', deviceId: 900043, deviceConfirmed: false, logonTime: now - 2000 }
        ] }] }
    };
    await mockDashboardBootstrap(I, () => data, () => ({ dismissedUntil: {} }));
    await I.mockRoute(codeRoute, route => route.fulfill({ status: 204, body: '' }));
    // Fail closed if a pointer-state check accidentally activates an account action.
    const mutations = [];
    for (const route of mutationRoutes) await I.mockRoute(route, request => {
        mutations.push(request.request().url());
        return request.fulfill({ status: 400, contentType: 'application/json', body: '{}' });
    });
    I.resizeWindow(1440, 1230);
    I.amOnPage('/admin/v9/');
    I.waitForVisible(modal, 20);
    I.waitForFunction(selector => getComputedStyle(document.querySelector(selector)).opacity === '1'
        && getComputedStyle(document.querySelector(`${selector} .modal-dialog`)).transform === 'none', [modal], 10);
    const failures = [];
    // Cover each button treatment and the warning/new-device surfaces once.
    await auditButtons(I, [
        `${modal} .btn-close`,
        `${modal} .md-dashboard-sessions__summary button`,
        `${modal} .md-dashboard-sessions__report`,
        `${modal} .md-dashboard-sessions__confirm-device`,
        `${modal} .md-dashboard-sessions__deny-device`
    ], failures);
    I.clickCss(`${modal} .md-dashboard-sessions__confirm-device`);
    I.waitForElement(`${modal} .md-dashboard-device-confirmation[aria-busy="false"]`, 10);
    await auditButtons(I, [`${modal} .md-dashboard-device-confirmation [type="submit"]`], failures);
    const violations = await I.runA11yCheck({ context: { include: [`${modal} button`, `${modal} a.btn`, `${modal} input`] } });
    I.assertDeepEqual(mutations, [], 'Contrast checks must not activate logout, device reporting or confirmation.');
    I.assertDeepEqual(failures, [], 'Representative actions must retain text/icon contrast and visible keyboard focus.');
    I.assertDeepEqual(violations.map(violation => ({ id: violation.id, targets: violation.nodes.map(node => node.target) })), [], 'Session actions and code entry must pass the accessibility audit.');
});
