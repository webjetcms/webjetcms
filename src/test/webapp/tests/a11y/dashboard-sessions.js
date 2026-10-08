const { mockDashboardBootstrap, dashboardPageRoute } = require('../../helpers/dashboard-browser');

Feature('a11y.dashboard-sessions');

const modal = '.md-dashboard-modal--sessions';
const codeRoute = '**/admin/rest/security/login-events/*/code';
const emailPageRoute = '**/admin/v9/?securityEvent=*';
const mutationRoutes = ['**/admin/rest/sessions/logout*', '**/admin/rest/security/login-events/*/report', '**/admin/rest/security/login-events/*/confirm'];

Before(({ login }) => { login('admin'); });

After(async ({ I }) => {
    for (const route of [dashboardPageRoute, emailPageRoute, codeRoute, ...mutationRoutes]) await I.stopMockingRoute(route);
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
    paint(style.backgroundColor);
    const backgroundOpacity = context.getImageData(0, 0, 1, 1).data[3] / 255;
    context.clearRect(0, 0, 1, 1);
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
        red: element.classList.contains('btn-red'), opacity: Number(style.opacity), backgroundOpacity,
        foreground, background, textContrast: contrast(foreground, background),
        minimum: element.textContent.trim() ? 4.5 : 3,
        outlineContrast: contrast(outline, surrounding), outlineStyle: style.outlineStyle,
        outlineWidth: parseFloat(style.outlineWidth), focusVisible: element.matches(':focus-visible'),
        hover: element.matches(':hover'), active: element.matches(':active')
    };
}

/** Exercises real pointer and keyboard states without activating account actions. */
async function auditButtons(I, selectors, failures, dialog = modal) {
    for (const selector of selectors) {
        const record = async state => {
            await I.waitForFunction(selector => !document.querySelector(selector).getAnimations().some(animation => animation.playState === 'running'), [selector], 5);
            const colors = await I.executeScript(buttonContrast, selector);
            if (colors.textContrast < colors.minimum) failures.push(`${colors.name} / ${state}: ${colors.textContrast.toFixed(2)}:1 < ${colors.minimum}:1`);
            if (colors.red) {
                I.assertDeepEqual(colors.foreground, [255, 255, 255], `Red actions must retain white text in the ${state} state.`);
                I.assertEqual(colors.opacity, 1, `Red actions must remain opaque in the ${state} state.`);
                I.assertEqual(colors.backgroundOpacity, 1, `Red actions must retain a solid background in the ${state} state.`);
            }
            if (state.includes('focus') && (!colors.focusVisible || colors.outlineStyle === 'none' || colors.outlineWidth <= 0 || colors.outlineContrast < 3)) {
                failures.push(`${colors.name} / ${state}: keyboard focus must remain visible with 3:1 contrast`);
            }
            if (state.includes('hover')) I.assertTrue(colors.hover, 'The hover state must be exercised with the pointer.');
            if (state === 'active') I.assertTrue(colors.active, 'The pressed state must be exercised with a held pointer.');
        };
        I.moveCursorTo(`${dialog} .modal-title`);
        I.executeScript(() => document.activeElement.blur());
        await record('normal');
        I.moveCursorTo(selector);
        await record('hover');
        // CodeceptJS has no pointer-down/up steps; release outside the control to avoid activating it.
        await I.usePlaywrightTo('hold the button without activating its action', async ({ page }) => { await page.mouse.down(); });
        await record('active');
        I.moveCursorTo(`${dialog} .modal-title`);
        await I.usePlaywrightTo('release outside the button', async ({ page }) => { await page.mouse.up(); });
        I.pressKey('Tab');
        I.executeScript(selector => document.querySelector(selector).focus(), selector);
        await record('focus');
        if (await I.executeScript(selector => document.querySelector(selector).classList.contains('btn-red'), selector)) {
            I.executeScript(selector => { document.querySelector(selector).disabled = true; }, selector);
            await record('disabled');
            I.executeScript(selector => { document.querySelector(selector).disabled = false; }, selector);
        }
    }
    I.moveCursorTo(`${dialog} .modal-title`);
    I.executeScript(() => document.activeElement.blur());
}

Scenario('Representative session actions remain readable with pointer and keyboard interaction', async ({ I }) => {
    const now = Date.now();
    const data = {
        settings: { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true, items: [], domainOptions: {} },
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
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    I.clickCss('[data-widget-type="sessions"] .md-dashboard__title-action');
    I.waitForVisible(modal, 20);
    I.waitForFunction(selector => getComputedStyle(document.querySelector(selector)).opacity === '1'
        && getComputedStyle(document.querySelector(`${selector} .modal-dialog`)).transform === 'none', [modal], 10);
    const failures = [];
    // Cover each button treatment and the warning/new-device surfaces once.
    await auditButtons(I, [
        `${modal} .btn-close`,
        `${modal} .md-dashboard-sessions__summary button`,
        `${modal} .md-dashboard-sessions__confirm-device`,
        `${modal} .md-dashboard-sessions__deny-device`
    ], failures);
    I.saveScreenshot('dashboard-sessions-red-device-action.png');
    I.clickCss(`${modal} .md-dashboard-sessions__summary button`);
    I.waitForVisible('.md-dashboard-modal--logout', 10);
    await auditButtons(I, [`${modal} .modal-footer .btn-red`], failures);
    I.saveScreenshot('dashboard-sessions-red-logout-action.png');
    I.clickCss(`${modal} .modal-footer .btn-outline-secondary`);
    I.waitForInvisible('.md-dashboard-modal--logout', 10);
    I.clickCss(`${modal} .md-dashboard-sessions__confirm-device`);
    I.waitForElement(`${modal} .md-dashboard-device-confirmation[aria-busy="false"]`, 10);
    await auditButtons(I, [`${modal} .md-dashboard-device-confirmation [type="submit"]`], failures);
    const violations = await I.runA11yCheck({ context: { include: [`${modal} button`, `${modal} a.btn`, `${modal} input`] } });
    I.assertDeepEqual(mutations, [], 'Contrast checks must not activate logout, device reporting or confirmation.');
    I.assertDeepEqual(failures, [], 'Representative actions must retain text/icon contrast and visible keyboard focus.');
    I.assertDeepEqual(violations.map(violation => ({ id: violation.id, targets: violation.nodes.map(node => node.target) })), [], 'Session actions and code entry must pass the accessibility audit.');
});

/** Email navigation is read-only; all subsequent writes and account destinations are intercepted. */
Scenario('Email device review uses a compact accessible dialog and requires explicit blocking', async ({ I }) => {
    const now = Date.now();
    const event = { id: 900043, createDate: now, browserName: 'Firefox 131', operatingSystem: 'Windows 11', ipAddress: '192.0.2.2' };
    const data = {
        settings: { version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true, items: [], domainOptions: {} },
        requestedSecurityEvent: event, securityEventRequested: true,
        notices: [{ id: 'twoFactor', severity: 'warning', title: 'Two-factor autotest', action: { type: 'popup', url: '/admin/2factorauth.jsp' } }],
        currentSessions: { currentSessionId: 'autotest-current', userSessions: [{ userSessions: [
            { sessionId: 'autotest-current', browserName: 'Chrome autotest', deviceId: 900042, deviceConfirmed: true, logonTime: now }
        ] }] }
    };
    const dialog = '.md-dashboard-modal--device-security';
    const report = mutationRoutes[1];
    let reports = 0;
    await mockDashboardBootstrap(I, () => data, () => ({ dismissedUntil: {} }), emailPageRoute);
    await I.mockRoute(report, route => {
        reports++;
        event.reportedAt = Date.now();
        return route.fulfill({ contentType: 'application/json', body: JSON.stringify(event) });
    });
    const open = async () => {
        I.amOnPage(`/admin/v9/?securityEvent=${event.id}`);
        I.waitForVisible(dialog, 20);
        I.waitForFunction(selector => getComputedStyle(document.querySelector(selector)).opacity === '1'
            && getComputedStyle(document.querySelector(`${selector} .modal-dialog`)).transform === 'none', [dialog], 10);
        I.executeScript(() => {
            window.autotestSecurityDestination = null;
            const capture = (...args) => { window.autotestSecurityDestination = {
                args, modalPresent: !!document.querySelector('.md-dashboard-modal, .modal-backdrop')
            }; };
            window.openProfileDialog = capture;
            window.WJ.openPopupDialog = capture;
        });
    };
    I.resizeWindow(1440, 1100);
    await open();
    I.see('Zabezpečte svoj účet', dialog);
    I.dontSeeElement(modal);
    I.assertEqual(reports, 0, 'Opening an email link must not block a device.');
    I.click('Neskôr', dialog);
    I.waitForDetached(dialog, 10);
    I.assertEqual(reports, 0, 'Later must not confirm blocking.');
    await open();
    const failures = [];
    await auditButtons(I, [`${dialog} .md-dashboard-device-security__report`], failures, dialog);
    I.assertEqual(reports, 0, 'Pointer and keyboard state checks must not submit a report.');
    I.clickCss(`${dialog} .md-dashboard-device-security__report`);
    I.waitForVisible(`${dialog} .md-dashboard-device-security__result`, 10);
    I.see('Firefox 131 · Windows 11 · 192.0.2.2 sme zablokovali.', dialog);
    I.see('Pri ďalšom prihlásení bude po overení prihlasovacích údajov potrebný kód z e-mailu.', dialog);
    await auditButtons(I, [`${dialog} .btn-close`, `${dialog} .modal-footer .btn-link`, `${dialog} .btn-white`, `${dialog} .btn-primary`], failures, dialog);
    const violations = await I.runA11yCheck({ context: { include: [dialog] } });
    I.assertDeepEqual(failures, [], 'Compact dialog actions must retain readable contrast and visible keyboard focus.');
    I.assertDeepEqual(violations.map(violation => ({ id: violation.id, targets: violation.nodes.map(node => node.target) })), [], 'The compact security dialog must pass the accessibility audit.');
    I.saveScreenshot('dashboard-device-security-desktop.png');
    I.resizeWindow(390, 850);
    I.assertTrue(await I.executeScript(selector => {
        const content = document.querySelector(`${selector} .modal-content`);
        const bounds = content.getBoundingClientRect();
        return bounds.left >= 0 && bounds.right <= innerWidth && content.scrollWidth <= content.clientWidth + 1
            && [...content.querySelectorAll('button')].every(button => {
                const rect = button.getBoundingClientRect();
                return rect.left >= bounds.left && rect.right <= bounds.right;
            });
    }, dialog), 'All compact dialog content and actions must fit a mobile viewport.');
    I.saveScreenshot('dashboard-device-security-mobile.png');
    I.click('Zapnúť 2FA', dialog);
    I.waitForFunction(() => window.autotestSecurityDestination !== null, 10);
    I.assertDeepEqual(await I.executeScript(() => window.autotestSecurityDestination), {
        args: ['/admin/2factorauth.jsp'], modalPresent: false
    }, '2FA must reuse its existing destination after the modal and backdrop close.');
    await open();
    I.click('Zmeniť heslo', dialog);
    I.waitForFunction(() => window.autotestSecurityDestination !== null, 10);
    const userId = await I.executeScript(() => window.currentUser.userId);
    I.assertDeepEqual(await I.executeScript(() => window.autotestSecurityDestination), {
        args: [userId, true], modalPresent: false
    }, 'Password change must open the current profile after the modal and backdrop close.');
    I.assertEqual(reports, 1, 'Reopening the result must not submit another report.');
});
