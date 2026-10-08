const { mockDashboardBootstrap, dashboardPageRoute } = require('../../helpers/dashboard-browser');
const { writeFileSync } = require('node:fs');

Feature('a11y.dashboard-sessions');

const modal = '.md-dashboard-modal--sessions';
const administratorsRoute = '**/admin/rest/sessions/administrators';
const historyRoute = '**/admin/rest/sessions/login-history*';
const codeRoute = '**/admin/rest/security/login-events/*/code';

Before(({ login }) => { login('admin'); });

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
async function auditButtons(I, root, measurements, failures) {
    const buttons = await I.executeScript(root => [...document.querySelector(root).querySelectorAll('button, a.btn')]
        .filter(element => element.getClientRects().length > 0 && getComputedStyle(element).visibility !== 'hidden')
        .map((element, index) => {
            element.dataset.contrastAutotest = String(index);
            return { selector: `${root} [data-contrast-autotest="${index}"]`, disabled: element.matches(':disabled') };
        }), root);
    for (const button of buttons) {
        if (button.disabled) continue; // WCAG excludes inactive controls from contrast requirements.
        const record = async state => {
            await I.waitForFunction(selector => !document.querySelector(selector).getAnimations().some(animation => animation.playState === 'running'), [button.selector], 5);
            const colors = await I.executeScript(buttonContrast, button.selector);
            measurements.push({ state, ...colors });
            if (colors.textContrast < colors.minimum) failures.push(`${colors.name} / ${state}: ${colors.textContrast.toFixed(2)}:1 < ${colors.minimum}:1`);
            if (state.includes('focus') && (!colors.focusVisible || colors.outlineStyle !== 'solid' || colors.outlineWidth < 2 || colors.outlineContrast < 3)) {
                failures.push(`${colors.name} / ${state}: keyboard focus must have a visible 2px outline with 3:1 contrast`);
            }
            if (state.includes('hover')) I.assertTrue(colors.hover, 'The hover state must be exercised with the pointer.');
            if (state === 'active') I.assertTrue(colors.active, 'The pressed state must be exercised with a held pointer.');
        };
        I.moveCursorTo(`${modal} .modal-title`);
        I.executeScript(() => document.activeElement.blur());
        await record('normal');
        I.moveCursorTo(button.selector);
        await record('hover');
        // CodeceptJS has no pointer-down/up steps; release outside the control to avoid activating it.
        await I.usePlaywrightTo('hold the button without activating its action', async ({ page }) => { await page.mouse.down(); });
        await record('active');
        I.moveCursorTo(`${modal} .modal-title`);
        await I.usePlaywrightTo('release outside the button', async ({ page }) => { await page.mouse.up(); });
        I.pressKey('Tab');
        I.executeScript(selector => document.querySelector(selector).focus(), button.selector);
        await record('focus');
        I.moveCursorTo(button.selector);
        await record('hover-focus');
    }
    I.moveCursorTo(`${modal} .modal-title`);
    I.executeScript(() => document.activeElement.blur());
}

Scenario('All session dialog buttons retain contrast in normal, hover, active and keyboard focus states', async ({ I }) => {
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
    let failAdmins = true, failHistory = true;
    await mockDashboardBootstrap(I, () => data, () => ({ dismissedUntil: {} }));
    await I.mockRoute(codeRoute, route => route.fulfill({ status: 204, body: '' }));
    await I.mockRoute(administratorsRoute, route => route.fulfill({ status: failAdmins ? 503 : 200, contentType: 'application/json', body: JSON.stringify([
        { userId: 900001, fullName: 'Current autotest', login: 'autotest-current', current: true, sessionCount: 1, lastActivity: now, clients: ['Chrome'] },
        { userId: 900002, fullName: 'Other autotest', login: 'autotest-other', sessionCount: 2, lastActivity: now, clients: ['Firefox'], email: 'autotest@example.com' }
    ]) }));
    await I.mockRoute(historyRoute, route => {
        const page = Number(new URL(route.request().url()).searchParams.get('page'));
        return route.fulfill({ status: failHistory ? 503 : 200, contentType: 'application/json', body: JSON.stringify({
            content: [{ createDate: now, ip: '192.0.2.1', description: 'Login autotest' }], totalElements: 60, totalPages: 3, first: page === 0, last: page === 2
        }) });
    });
    // Fail closed if a pointer-state check accidentally activates an account action.
    const mutations = [];
    const mutationRoutes = ['**/admin/rest/sessions/logout*', '**/admin/rest/security/login-events/*/report', '**/admin/rest/security/login-events/*/confirm'];
    for (const route of mutationRoutes) await I.mockRoute(route, request => {
        mutations.push(request.request().url());
        return request.fulfill({ status: 400, contentType: 'application/json', body: '{}' });
    });
    I.resizeWindow(1440, 1230);
    I.amOnPage('/admin/v9/');
    I.waitForVisible(modal, 20);
    I.waitForFunction(selector => getComputedStyle(document.querySelector(selector)).opacity === '1'
        && getComputedStyle(document.querySelector(`${selector} .modal-dialog`)).transform === 'none', [modal], 10);
    I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), modal),
        'Opening a session dialog without inputs must focus the dialog itself.');
    I.dontSeeElement(`${modal} [role="tab"]:focus`);
    I.dontSeeElement('.tooltip.show');
    I.pressKey('Tab');
    I.seeElement(`${modal} .btn-close:focus-visible`);
    I.pressKey('Tab');
    I.seeElement(`${modal} [role="tab"][id$="-mine"]:focus-visible`);
    I.pressKey('ArrowRight');
    I.seeElement(`${modal} [role="tab"][id$="-admins"][aria-selected="true"]:focus-visible`);
    I.pressKey('ArrowLeft');
    I.seeElement(`${modal} [role="tab"][id$="-mine"][aria-selected="true"]:focus-visible`);
    const measurements = [], failures = [], violations = [];
    await auditButtons(I, modal, measurements, failures);
    I.clickCss(`${modal} .md-dashboard-sessions__confirm-device`);
    I.waitForElement(`${modal} .md-dashboard-device-confirmation[aria-busy="false"]`, 10);
    await auditButtons(I, `${modal} .md-dashboard-device-confirmation`, measurements, failures);
    const verify = `${modal} .md-dashboard-device-confirmation [type="submit"]`;
    I.moveCursorTo(verify);
    I.saveScreenshot('dashboard-session-code-hover.png');
    I.moveCursorTo(`${modal} .modal-title`);
    I.pressKey('Tab');
    I.executeScript(selector => document.querySelector(selector).focus(), verify);
    I.saveScreenshot('dashboard-session-code-focus.png');
    violations.push(...await I.runA11yCheck({ context: { include: [`${modal} button`, `${modal} a.btn`] } }));

    I.clickCss(`${modal} [role="tab"][id$="-admins"]`);
    I.waitForElement(`${modal} .md-dashboard-sessions__admins [role="alert"]`, 10);
    await auditButtons(I, `${modal} .md-dashboard-sessions__admins`, measurements, failures);
    failAdmins = false;
    I.clickCss(`${modal} .md-dashboard-sessions__admins > button`);
    I.waitForVisible(`${modal} .md-dashboard-sessions__admins-table`, 10);
    I.seeElement(`${modal} [data-admin-user-id="900002"] button`);
    await auditButtons(I, `${modal} .md-dashboard-sessions__admins`, measurements, failures);
    violations.push(...await I.runA11yCheck({ context: { include: [`${modal} button`, `${modal} a.btn`] } }));

    I.clickCss(`${modal} [role="tab"][id$="-history"]`);
    I.waitForElement(`${modal} .md-dashboard-sessions__history [role="alert"]`, 10);
    await auditButtons(I, `${modal} .md-dashboard-sessions__history`, measurements, failures);
    failHistory = false;
    I.clickCss(`${modal} .md-dashboard-sessions__history > button`);
    I.waitForVisible(`${modal} .md-dashboard-sessions__pagination`, 10);
    I.seeElement(`${modal} .md-dashboard-sessions__pagination button:disabled`);
    I.clickCss(`${modal} .md-dashboard-sessions__pagination button:last-child`);
    I.waitForText('2 / 3', 10, `${modal} .md-dashboard-sessions__pagination`);
    await auditButtons(I, `${modal} .md-dashboard-sessions__history`, measurements, failures);
    violations.push(...await I.runA11yCheck({ context: { include: [`${modal} button`, `${modal} a.btn`] } }));
    writeFileSync('../../../build/test/dashboard-session-contrast.json', JSON.stringify({ measurements, failures, violations }, null, 2));
    I.assertDeepEqual(mutations, [], 'Contrast checks must not activate logout, device reporting or confirmation.');
    I.assertDeepEqual(failures, [], 'Every enabled button must retain WCAG AA text/icon and keyboard focus contrast.');
    I.assertDeepEqual(violations.map(violation => ({ id: violation.id, targets: violation.nodes.map(node => node.target) })), [], 'Buttons in all three session tabs must pass the WCAG AA audit.');
    for (const route of [dashboardPageRoute, codeRoute, administratorsRoute, historyRoute, ...mutationRoutes]) await I.stopMockingRoute(route);
    I.wjSetDefaultWindowSize();
});
