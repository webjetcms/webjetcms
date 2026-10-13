Feature('admin.help-menu @singlethread');

const trigger = '#admin-help-toggle';
const panel = '#admin-help-menu';
let documentationSearchUrl;

Before(({ login }) => login('admin'));

function openHelp(I) {
    I.waitForElement(`${trigger}[data-help-initialized="true"]`, 15);
    I.clickCss(trigger);
    I.waitForVisible(`${panel}.show`, 5);
}

function menuItemStyle(I, selector) {
    return I.executeScript(selector => {
        const item = document.querySelector(selector);
        const style = getComputedStyle(item);
        return { background: style.backgroundColor, text: style.color, icon: getComputedStyle(item.querySelector('.ti')).color };
    }, selector);
}

Scenario('Header help shares release news and supplies documentation links', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    openHelp(I);
    I.assertDeepEqual(await I.executeScript(() => {
        const button = document.querySelector('#admin-help-toggle');
        const label = button.querySelector('span').getBoundingClientRect();
        const arrow = button.querySelector('.md-help-menu__chevron').getBoundingClientRect();
        return [button.getBoundingClientRect().height, arrow.left - label.right, button.getBoundingClientRect().right - arrow.right];
    }), [31, 3, 7], 'The Help trigger must keep its compact height and the specified arrow spacing.');
    const origin = await I.executeScript(() => window.adminHelpContext.documentationBaseUrl);
    I.see('Pomoc k tejto stránke', panel);
    I.see('Úvod', '.md-help-menu__page-title');
    I.see('Čo je nové vo verzii 2026.18', panel);
    I.see('Počet noviniek: 5', panel);
    I.assertEqual(await I.grabAttributeFrom('.md-help-menu__page', 'href'), `${origin}/latest/sk/redactor/admin/welcome`);
    I.assertEqual(await I.grabAttributeFrom('.md-help-menu__release', 'href'), `${origin}/latest/sk/CHANGELOG-2026?id=_202618`);
    I.assertEqual(await I.grabAttributeFrom('[data-help-path="/CHANGELOG"]', 'href'), await I.grabAttributeFrom('.md-dashboard-widget__news-more', 'href'));
    I.assertEqual(await I.executeScript(() => document.querySelector('.md-help-menu__new').hidden),
        await I.executeScript(() => document.querySelector('.md-dashboard-widget__news-badge').hidden));
    I.see('Enterprise', '.md-help-menu__system');
    I.assertEqual(await I.grabAttributeFrom('.md-help-menu__support', 'href'), 'https://www.webjetcms.sk/');
    I.dontSeeElement(`${panel} kbd`);
    const pageHelpStyle = await menuItemStyle(I, '.md-help-menu__page');
    I.assertDeepEqual(pageHelpStyle, { background: 'rgba(0, 0, 0, 0)', text: 'rgb(19, 21, 27)', icon: 'rgb(104, 111, 131)' },
        'Page help is an action and must have no selected-value highlight.');
    I.saveScreenshot('help-menu-desktop.png');
    I.moveCursorTo('.md-help-menu__page');
    I.assertDeepEqual(await menuItemStyle(I, '.md-help-menu__page'), { ...pageHelpStyle, background: 'rgb(243, 243, 246)' },
        'Hover must change only the action background.');
    I.moveCursorTo(trigger);
    I.pressKey('ArrowDown');
    I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains('md-help-menu__page')));
    I.assertDeepEqual(await menuItemStyle(I, '.md-help-menu__page'), { ...pageHelpStyle, background: 'rgb(243, 243, 246)' },
        'Arrow-key focus must use the same neutral action background.');
    I.pressKey('Escape');
    I.waitForInvisible(panel, 5);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'admin-help-toggle');
    I.assertDeepEqual(await I.executeScript(() => {
        const style = getComputedStyle(document.querySelector('#admin-help-toggle'));
        return [style.outlineWidth, style.outlineStyle, style.outlineColor];
    }), ['2px', 'solid', 'rgb(0, 99, 251)'], 'The Help trigger must have a blue keyboard focus outline.');

    const userMenu = 'ul[aria-labelledby="dropdownMenuUser"]';
    const profile = `${userMenu} .dropdown-item[data-userid]`;
    I.clickCss('#dropdownMenuUser');
    I.waitForVisible(`${userMenu}.show`, 5);
    const profileStyle = await menuItemStyle(I, profile);
    I.assertEqual(profileStyle.background, 'rgba(0, 0, 0, 0)');
    I.pressKey('ArrowDown');
    I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), profile),
        'ArrowDown must focus the profile action.');
    I.assertDeepEqual(await menuItemStyle(I, profile), { ...profileStyle, background: 'rgb(243, 243, 246)' });
    I.pressKey('ArrowDown');
    I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelectorAll(`${selector} .dropdown-item`)[1], userMenu),
        'ArrowDown must continue to the next user action without activating it.');
    I.pressKey('Escape');
    I.waitForInvisible(userMenu, 5);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'dropdownMenuUser');

    I.pressKey(['Shift', 'Tab']);
    I.pressKey('Tab');
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'dropdownMenuUser');
    I.assertDeepEqual(await I.executeScript(() => {
        const style = getComputedStyle(document.querySelector('#dropdownMenuUser'));
        return [style.outlineWidth, style.outlineStyle, style.outlineColor];
    }), ['2px', 'solid', 'rgb(0, 99, 251)'], 'The user trigger must have the same blue keyboard focus outline.');
    I.pressKey('ArrowDown');
    I.waitForVisible(`${userMenu}.show`, 5);
    I.waitForFunction(() => getComputedStyle(document.querySelector('#dropdownMenuUser')).backgroundColor === 'rgb(243, 243, 246)', [], 5);
    I.assertTrue(await I.executeScript(selector => document.activeElement === document.querySelector(selector), profile));
    I.assertDeepEqual(await menuItemStyle(I, '#dropdownMenuUser'), {
        background: 'rgb(243, 243, 246)', text: 'rgb(19, 21, 27)', icon: 'rgb(19, 21, 27)'
    }, 'The open user trigger must remain readable after keyboard focus moves into its menu.');
    I.moveCursorTo(trigger);
    I.waitForFunction(() => getComputedStyle(document.querySelector('#admin-help-toggle')).backgroundColor === 'rgb(243, 243, 246)', [], 5);
    I.assertDeepEqual(await menuItemStyle(I, '#dropdownMenuUser'), await menuItemStyle(I, '#admin-help-toggle'),
        'The open user trigger and hovered Help trigger must have the same neutral appearance.');
    I.saveScreenshot('user-menu-keyboard.png');
    I.pressKey('Escape');
    I.waitForInvisible(userMenu, 5);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'dropdownMenuUser');
    I.saveElementScreenshot('.ly-header', 'header-refinement.png');
});

Scenario('System diagnostics copy all context and restore the copy action after confirmation', async ({ I }) => {
    I.amOnPage('/admin/v9/search/');
    I.executeScript(() => {
        history.replaceState(null, '', '?autotest=diagnostics#details');
    });
    openHelp(I);
    I.executeScript(() => {
        Object.defineProperty(navigator, 'clipboard', { configurable: true, value: {
            writeText: async value => { window.copiedHelpDiagnostics = value; }
        } });
    });
    I.seeElement('.md-help-menu__version-number');
    I.seeElement('.md-help-menu__version-date');
    I.seeElement('.md-help-menu__server');
    I.assertTrue(await I.executeScript(() => {
        const version = document.querySelector('.md-help-menu__version');
        const buildDate = document.querySelector('.md-help-menu__version-date');
        return buildDate.getBoundingClientRect().top >= version.getBoundingClientRect().bottom
            && /\d{2}:\d{2}:\d{2}$/.test(buildDate.textContent.trim());
    }), 'The build timestamp must include seconds and appear below the version.');
    I.assertFalse(await I.executeScript(() => document.querySelector('.md-help-menu__system').textContent.includes('DEV')),
        'The footer must not repeat the environment badge.');
    I.clickCss('.md-help-menu__copy');
    I.waitForElement('.md-help-menu__copy.is-copied .ti-check', 5);
    I.seeAttributesOnElements('.md-help-menu__copy', { 'aria-label': 'Skopírované', 'data-bs-original-title': 'Skopírované' });
    I.assertEqual(await I.executeScript(() => window.copiedHelpDiagnostics), await I.executeScript(() =>
        window.adminHelpContext.systemInfo
        + (location.hostname.toLowerCase() === window.adminHelpContext.currentDomain.toLowerCase() ? '' : '\nCMS doména: ' + location.origin)
        + '\nStránka: ' + location.pathname + location.search + location.hash + '\nPrehliadač: ' + window.adminHelpContext.browser));
    I.assertTrue(await I.executeScript(() => document.querySelector('#admin-help-menu').classList.contains('show')),
        'Copying must keep Help open.');
    I.saveElementScreenshot('.md-help-menu__system', 'help-menu-system-info-copied.png');
    I.waitForElement('.md-help-menu__copy:not(.is-copied) .ti-copy', 5);
    I.seeAttributesOnElements('.md-help-menu__copy', { 'aria-label': 'Kopírovať informácie o WebJET CMS' });
    I.saveElementScreenshot('.md-help-menu__system', 'help-menu-system-info.png');
    I.pressKey('Escape');
});

Scenario('Page help is refreshed on each opening and omitted without a route', async ({ I }) => {
    I.amOnPage('/admin/v9/search/');
    I.waitForElement(`${trigger}[data-help-initialized="true"]`, 15);
    const origin = await I.executeScript(() => window.adminHelpContext.documentationBaseUrl);
    I.executeScript(() => { window.helpLink = ''; });
    openHelp(I);
    I.dontSeeElement('.md-help-menu__page');
    const mouseState = await I.executeScript(() => {
        const menu = document.querySelector('#admin-help-menu');
        const documentation = menu.querySelector('[data-help-path="/"]');
        return {
            focusedId: document.activeElement.id,
            menuItemFocused: menu.contains(document.activeElement),
            documentationBackground: getComputedStyle(documentation).backgroundColor
        };
    });
    I.assertEqual(mouseState.focusedId, 'admin-help-toggle');
    I.assertFalse(mouseState.menuItemFocused, 'Opening Help with the mouse must not focus or highlight its first item.');
    I.assertEqual(mouseState.documentationBackground, 'rgba(0, 0, 0, 0)');
    I.saveScreenshot('help-menu-without-page-help.png');
    I.pressKey('ArrowDown');
    I.assertEqual(await I.executeScript(() => document.activeElement.dataset.helpPath), '/');
    I.pressKey('Escape');
    I.executeScript(() => { window.helpLink = '/redactor/admin/keyboard-shortcuts'; WJ.setTitle('autotest help title'); });
    openHelp(I);
    I.see('autotest help title', '.md-help-menu__page-title');
    I.assertEqual(await I.grabAttributeFrom('.md-help-menu__page', 'href'), `${origin}/latest/sk/redactor/admin/keyboard-shortcuts`);
    I.pressKey('Escape');
});

Scenario('Feedback outside the dashboard returns to Help and preserves its draft', async ({ I }) => {
    I.amOnPage('/admin/v9/search/');
    openHelp(I);
    I.clickCss('.md-help-menu__feedback');
    I.waitForVisible('#feedback_modal.show', 10);
    I.dontSeeElement(panel);
    I.fillField('#feedback-group-text .ql-editor', 'autotest help feedback draft');
    I.clickCss('#feedback_modal .md-feedback__back');
    I.waitForInvisible('#feedback_modal', 10);
    I.waitForVisible(`${panel}.show`, 5);
    I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains('md-help-menu__feedback')));
    I.clickCss('.md-help-menu__feedback');
    I.waitForVisible('#feedback_modal.show', 10);
    I.see('autotest help feedback draft', '#feedback-group-text');
    I.clickCss('#feedback_modal .md-feedback__cancel');
    I.waitForInvisible('#feedback_modal', 10);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'admin-help-toggle');
});

Scenario('Help remains reachable and contained at tablet and phone sizes', async ({ I }) => {
    I.amOnPage('/admin/v9/search/');
    I.waitForElement(`${trigger}[data-help-initialized="true"]`, 15);
    for (const width of [1024, 390]) {
        I.resizeWindow(width, 800);
        I.clickCss('.js-sidebar-toggler');
        I.waitForVisible('.ly-page-wrapper.active', 5);
        openHelp(I);
        I.assertTrue(await I.executeScript(() => {
            const menu = document.querySelector('#admin-help-menu').getBoundingClientRect();
            return menu.left >= 0 && menu.right <= innerWidth && menu.top >= 0 && menu.bottom <= innerHeight;
        }), `The Help menu must fit at ${width}px.`);
        I.saveScreenshot(`help-menu-${width}.png`);
        I.pressKey('Escape');
        I.clickCss('.js-sidebar-toggler');
        I.waitForInvisible('.ly-page-wrapper.active', 5);
    }
    I.wjSetDefaultWindowSize();
});

Scenario('Keyboard access and accessibility of the Help menu', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/search/');
    I.waitForElement(`${trigger}[data-help-initialized="true"]`, 15);
    I.executeScript(() => document.querySelector('#admin-help-toggle').focus());
    I.pressKey('Enter');
    I.waitForVisible(`${panel}.show`, 5);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'admin-help-toggle');
    I.pressKey('ArrowDown');
    I.assertTrue(await I.executeScript(() => document.activeElement.classList.contains('md-help-menu__page')));
    I.pressKey('ArrowDown');
    I.assertEqual(await I.executeScript(() => document.activeElement.dataset.helpPath), '/');
    await a11y.check('.md-help-menu');
    I.pressKey('Escape');
    I.waitForInvisible(panel, 5);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'admin-help-toggle');
});

Scenario('Configured documentation origin reaches the layout and WJ helper', async ({ I, Document }) => {
    I.amOnPage('/admin/v9/search/');
    I.waitForElement(`${trigger}[data-help-initialized="true"]`, 15);
    documentationSearchUrl = await I.executeScript(() => window.adminSearchContext.documentationSearchUrl);
    Document.setConfigValue('adminDocumentationSearchUrl', 'http://manual.autotest.invalid:8081/manual/rest/rag/markdown/search', true);
    I.amOnPage('/admin/v9/search/');
    openHelp(I);
    I.assertEqual(await I.grabAttributeFrom('[data-help-path="/"]', 'href'), 'http://manual.autotest.invalid:8081/latest/sk/');
    I.assertEqual(await I.executeScript(() => WJ.getDocumentationUrl('/redactor/admin/keyboard-shortcuts')),
        'http://manual.autotest.invalid:8081/latest/sk/redactor/admin/keyboard-shortcuts');
    const legacyHelp = await I.executeScript(async () => {
        const response = await fetch('/admin/scripts/common.jsp');
        return { status: response.status, configured: (await response.text()).includes('http://manual.autotest.invalid:8081') };
    });
    I.assertEqual(legacyHelp.status, 200);
    I.assertTrue(legacyHelp.configured, 'The legacy JSP helper must use the same configured origin.');
    I.pressKey('Escape');
});

Scenario('Restore documentation search configuration', ({ Document }) => {
    if (documentationSearchUrl !== undefined) Document.setConfigValue('adminDocumentationSearchUrl', documentationSearchUrl, true);
});
