Feature('admin.environment').tag('@singlethread');

const badge = '.md-environment';
const headerTooltip = '.tooltip.wj-tooltip-hoverable.show';
const loginTooltip = '#environment-description';

let originalEnvironment;
Before(async ({ I, login }) => {
    login('admin');
    if (!originalEnvironment) {
        I.amOnPage('/admin/v9/');
        originalEnvironment = await I.executeScript(async () => {
            const names = ['dashboardEnvironmentName', 'dashboardEnvironmentDescription', 'dashboardEnvironmentIcon', 'dashboardEnvironmentColor'];
            return Object.fromEntries(await Promise.all(names.map(async name => {
                const response = await fetch('/admin/rest/settings/configuration/autocomplete/detail?name=' + name, { headers: { 'X-CSRF-Token': window.csrfToken } });
                if (!response.ok) throw new Error('Cannot preserve environment configuration: ' + response.status);
                return [name, (await response.json()).value];
            })));
        });
    }
});

/** Verifies the shared identity, fixed ordering and keyboard tooltip across layouts and viewports. */
Scenario('Environment stays before the page title on every v9 page', async ({ I }) => {
    const tooltip = locate(headerTooltip).withText('DEV');
    for (const url of ['/admin/v9/', '/admin/v9/webpages/web-pages-list/', '/apps/stat/admin/']) {
        I.amOnPage(url);
        I.waitForElement('.header-title[aria-label]', 20);
        I.see('DEV', `${badge} > span:first-of-type`);
        I.seeElement(`${badge} .ti-code`);
        I.dontSeeElement('.md-dashboard__environment');
        I.seeInTitle('[DEV]');
        I.assertFalse(await I.executeScript(() => performance.getEntriesByType('resource').some(resource => resource.name.includes('/dist/js/environment.js'))));
        I.assertEqual(await I.grabCssPropertyFrom(badge, 'background-color'), 'rgb(207, 245, 228)');
        for (const width of [1440, 1024, 390]) {
            I.resizeWindow(width, 1000);
            I.waitForFunction(() => {
                const badge = document.querySelector('.md-environment').getBoundingClientRect();
                const title = document.querySelector('.header-title').getBoundingClientRect();
                const menu = document.querySelector('.js-sidebar-toggler').getBoundingClientRect();
                return badge.top >= 0 && badge.bottom <= 48 && badge.right < title.left &&
                    (innerWidth >= 1200 || (menu.right < badge.left && Math.abs(menu.y + menu.height / 2 - badge.y - badge.height / 2) < 2));
            }, [], 5);
            if (width >= 1200) {
                I.assertTrue(await I.executeScript(() => {
                    if (!document.body.classList.contains('ly-submenu-active')) return true;
                    const tabs = document.querySelector('.ly-submenu').getBoundingClientRect();
                    const content = document.querySelector('.ly-content').getBoundingClientRect();
                    return Math.abs(tabs.bottom - content.top) < 1;
                }), 'Header tabs must meet the content without a gap');
                if (url === '/apps/stat/admin/') I.saveScreenshot('environment-header-tabs.png');
            }
            if (width < 1200) {
                I.clickCss('.js-sidebar-toggler');
                I.waitForFunction(() => {
                    const badge = document.querySelector('.md-environment').getBoundingClientRect();
                    const controls = document.querySelector('.header-link-wrapper').getBoundingClientRect();
                    return document.querySelector('.ly-page-wrapper').classList.contains('active') && badge.top >= 0 && badge.bottom <= 48 && badge.right < controls.left;
                }, [], 5);
                I.clickCss('.js-sidebar-toggler');
                I.waitForFunction(() => !document.querySelector('.ly-page-wrapper').classList.contains('active'), [], 5);
            }
            if (width === 390 && url === '/admin/v9/') I.saveScreenshot('environment-header-mobile.png');
        }
        I.wjSetDefaultWindowSize();
    }
    I.executeScript(() => document.querySelector('.md-environment').focus());
    I.waitForVisible(tooltip, 5);
    I.pressKey('Escape');
    I.waitForInvisible(tooltip, 5);
    I.pressKey('Tab');
    I.pressKey('Shift+Tab');
    I.waitForVisible(tooltip, 5);
    I.moveCursorTo('.header-title');
    I.pressKey('Tab');
    I.moveCursorTo(badge);
    I.waitForVisible(tooltip, 5);
    I.moveCursorTo(locate('.tooltip-inner').inside(tooltip));
    I.seeElement(tooltip);
    I.moveCursorTo('.header-title');
    I.waitForInvisible(tooltip, 5);
    I.executeScript(() => {
        WJ.setTitle('autotest long page title '.repeat(10));
        WJ.setTitle('autotest renamed page');
    });
    I.seeInTitle('[DEV] autotest renamed page');
    I.assertEqual(await I.grabAttributeFrom('.header-title', 'aria-label'), '[DEV] autotest renamed page');
    I.amOnPage('/admin/v9/');
    I.saveScreenshot('environment-header-desktop.png');
});

/** Covers server-rendered overrides and accepts unknown icon names without rendering a fallback. */
Scenario('Environment configuration controls the header and login', async ({ I, Document }) => {
    const tooltip = locate(headerTooltip).withText('autotest staging node – autotest environment description');
    Document.setConfigValue('dashboardEnvironmentName', 'autotest staging node', true);
    Document.setConfigValue('dashboardEnvironmentDescription', 'autotest environment description', true);
    Document.setConfigValue('dashboardEnvironmentIcon', 'rocket', true);
    Document.setConfigValue('dashboardEnvironmentColor', '#183153', true);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.header-title[aria-label]', 20);
    I.see('AUTOTEST', `${badge} > span:first-of-type`);
    I.seeElement(`${badge} .ti-rocket`);
    I.seeInTitle('[AUTOTEST]');
    I.assertEqual(await I.grabCssPropertyFrom(badge, 'color'), 'rgb(255, 255, 255)');
    I.assertEqual(await I.grabCssPropertyFrom(badge, 'background-color'), 'rgb(24, 49, 83)');
    I.executeScript(() => document.querySelector('.md-environment').focus());
    I.waitForVisible(tooltip, 5);
    I.see('autotest staging node – autotest environment description', tooltip);

    Document.setConfigValue('dashboardEnvironmentIcon', 'autotest-unknown-icon', true);
    I.amOnPage('/admin/v9/');
    I.waitForElement(`${badge} .ti-autotest-unknown-icon`, 20);
    I.dontSeeElement(`${badge} .ti-code`);
    I.assertEqual(await I.executeScript(() => getComputedStyle(document.querySelector('.md-environment .ti'), '::before').content), 'none');
    Document.setConfigValue('dashboardEnvironmentIcon', 'rocket', true);

    I.logout();
    I.amOnPage('/admin/logon/');
    I.waitForElement('#username', 20);
    I.see('AUTOTEST', `${badge} > span:first-of-type`);
    I.seeElement(`${badge} .ti-rocket`);
    I.seeInTitle('[AUTOTEST]');
    I.executeScript(() => document.querySelector('.md-environment').focus());
    I.waitForVisible(loginTooltip, 5);
    I.see('autotest staging node – autotest environment description', loginTooltip);
    I.pressKey('Escape');
    I.dontSeeElement(loginTooltip);
    I.pressKey('Tab');
    I.pressKey('Shift+Tab');
    I.waitForVisible(loginTooltip, 5);
    I.pressKey('Tab');
    I.moveCursorTo('#username');
    const pointerTarget = await I.executeScript(() => {
        const badge = document.querySelector('.md-environment');
        const bounds = badge.getBoundingClientRect();
        const target = document.elementFromPoint(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
        return { reachable: badge.contains(target), coveringElement: target?.className };
    });
    I.assertTrue(pointerTarget.reachable, 'The login environment badge must receive pointer input; covering element: ' + pointerTarget.coveringElement);
    I.moveCursorTo(badge);
    I.waitForVisible(loginTooltip, 5);
    I.moveCursorTo(`${loginTooltip} > span`);
    I.seeElement(loginTooltip);
    I.moveCursorTo('#username');
    I.waitForInvisible(loginTooltip, 5);
    I.saveScreenshot('environment-login.png');
});

/** Checks the opt-out on administration and login, including the browser tab title. */
Scenario('Empty environment name hides the badge and title prefix', ({ I, Document }) => {
    Document.setConfigValue('dashboardEnvironmentName', '', true);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.header-title[aria-label]', 20);
    I.dontSeeElement(badge);
    I.dontSeeInTitle('[AUTOTEST]');
    I.logout();
    I.amOnPage('/admin/logon/');
    I.waitForElement('#username', 20);
    I.dontSeeElement(badge);
    I.dontSeeInTitle('[AUTOTEST]');
});

/** Restores temporary configuration independently so cleanup also runs after a failed scenario. */
Scenario('Cleanup environment configuration', ({ I, Document }) => {
    for (const [name, value] of Object.entries(originalEnvironment || {})) Document.setConfigValue(name, value, true);
    I.amOnPage('/admin/v9/');
    I.waitForElement(badge, 20);
    I.seeInTitle('[DEV]');
});
