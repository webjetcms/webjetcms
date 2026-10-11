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

Scenario('Header help shares release news and supplies documentation links', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    openHelp(I);
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
    I.saveScreenshot('help-menu-desktop.png');
    I.pressKey('Escape');
    I.waitForInvisible(panel, 5);
    I.assertEqual(await I.executeScript(() => document.activeElement.id), 'admin-help-toggle');
});

Scenario('Page help is refreshed on each opening and omitted without a route', async ({ I }) => {
    I.amOnPage('/admin/v9/search/');
    I.waitForElement(`${trigger}[data-help-initialized="true"]`, 15);
    const origin = await I.executeScript(() => window.adminHelpContext.documentationBaseUrl);
    I.executeScript(() => { window.helpLink = ''; });
    openHelp(I);
    I.dontSeeElement('.md-help-menu__page');
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
    Document.setConfigValue('adminDocumentationSearchUrl', '', true);
    I.amOnPage('/admin/v9/search/');
    openHelp(I);
    I.assertEqual(await I.grabAttributeFrom('[data-help-path="/"]', 'href'), 'https://docs.webjetcms.sk/latest/sk/');
    I.assertEqual(await I.executeScript(() => window.adminSearchContext.documentationSearchUrl), '');
    I.pressKey('Escape');
});

Scenario('Restore documentation search configuration', ({ Document }) => {
    if (documentationSearchUrl !== undefined) Document.setConfigValue('adminDocumentationSearchUrl', documentationSearchUrl, true);
});
