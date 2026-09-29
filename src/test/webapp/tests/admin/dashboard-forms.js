const { showWidget, waitForWidgets, mockDashboardBootstrap, dashboardPageRoute } = require('../../helpers/dashboard-browser');

Feature('admin.dashboard-forms');

const formName = 'Multistepform_screens';
const card = '[data-widget-type="forms"]';
const overviewRoute = '**/admin/rest/forms-list/overview*';
const detailRoute = '**/admin/rest/forms-list/search/findByColumns*';

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});

/** Displays one selected form without changing the account's saved dashboard. */
async function openDashboard(I, overview) {
    await mockDashboardBootstrap(I, () => ({ notices: [], settings: {
        version: 1, configured: true, legacyBookmarksHandled: true,
        items: [{ id: 'autotest-form-preview', type: 'forms', size: '3x3', options: { days: 7 } }],
        domainOptions: { 'autotest-form-preview': { formName } }
    } }));
    if (overview) await I.mockRoute(overviewRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(overview) }));
    I.amOnPage('/admin/v9/');
    await waitForWidgets(I);
}

Scenario('Selected form displays real submission values and opens the selected record', async ({ I, DTE }) => {
    const source = await I.executeScript(async formName => {
        const read = async url => {
            const response = await fetch(url, { headers: { 'X-CSRF-Token': window.csrfToken } });
            if (!response.ok) throw new Error(`Form request failed: ${response.status}`);
            return response.json();
        };
        const [submissions, metadata] = await Promise.all([
            read(`/admin/rest/forms-list/all?detail=true&formName=${encodeURIComponent(formName)}&size=20&page=0&sort=createDate%2Cdesc`),
            read(`/admin/rest/forms-list/columns/${encodeURIComponent(formName)}`)
        ]);
        return { submissions, columns: metadata.columns };
    }, formName);
    I.assertEqual(source.submissions.content.length, 20, 'The existing multistep form fixture must contain twenty submissions.');
    const items = source.submissions.content.filter((item, index) => index % 2 === 0);
    // Use the real latest records regardless of fixture age, retaining the live detail and columns endpoints.
    await openDashboard(I, { total: source.submissions.totalElements, from: items.at(-1).createDate, to: items[0].createDate,
        items: items.map(item => ({ id: String(item.id), title: formName, date: item.createDate })) });
    const rendered = await I.executeScript(selector => [...document.querySelectorAll(`${selector} tbody tr`)].map(row => ({
        text: row.cells[0].textContent, href: row.querySelector('a').getAttribute('href'), date: row.cells[1].textContent
    })), card);
    I.assertEqual(rendered.length, items.length);
    const fields = ['meno-1', 'priezvisko-1', 'email-1'];
    I.assertTrue(fields.every(field => source.columns.some(column => column.value === field)), 'The multistep fixture must expose the three contact fields.');
    for (const [index, row] of rendered.entries()) {
        I.assertEqual(row.text, fields.map(field => items[index].columnNamesAndValues[field]).filter(Boolean).join(' '));
        I.assertEqual(row.href, `/apps/form/admin/detail/?formName=${formName}&id=${items[index].id}`);
        I.assertTrue(row.date.length > 0);
    }
    for (const width of [1280, 1100, 600]) {
        I.resizeWindow(width, 900);
        await showWidget(I, 'autotest-form-preview');
        I.assertTrue(await I.executeScript(selector => {
            const table = document.querySelector(`${selector} table`);
            const widget = document.querySelector(selector);
            return table.scrollWidth <= widget.clientWidth && widget.scrollWidth <= widget.clientWidth + 1;
        }, card), `Submission text must wrap within the widget at ${width}px.`);
    }
    I.wjSetDefaultWindowSize();
    await showWidget(I, 'autotest-form-preview');
    I.saveScreenshot('dashboard-forms-desktop.png');
    const list = `${card} .md-dashboard-widget__forms`;
    I.assertTrue(await I.executeScript(selector => {
        const list = document.querySelector(selector);
        return list.scrollHeight > list.clientHeight && list.clientHeight > 0;
    }, list), 'All ten submissions must remain accessible in a bounded scroll area.');
    const outerScroll = await I.executeScript(() => window.scrollbarMain.offset.y);
    // Real wheel input verifies that the administration's smooth scrollbar does not consume it.
    await I.usePlaywrightTo('scroll the form submission list', async ({ page }) => {
        const bounds = await page.locator(list).boundingBox();
        await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + 30);
        await page.mouse.wheel(0, 130);
    });
    I.waitForFunction(selector => document.querySelector(selector).scrollTop > 0, [list], 10);
    I.assertEqual(await I.executeScript(() => window.scrollbarMain.offset.y), outerScroll, 'Wheel input must stay inside the submission list.');
    I.executeScript(selector => { const list = document.querySelector(selector); list.focus({ preventScroll: true }); list.scrollTop = 0; }, list);
    I.pressKey('End');
    I.waitForFunction(selector => {
        const list = document.querySelector(selector);
        return Math.abs(list.scrollHeight - list.clientHeight - list.scrollTop) <= 1;
    }, [list], 10);
    I.assertEqual(await I.executeScript(() => window.scrollbarMain.offset.y), outerScroll, 'Keyboard input must stay inside the submission list.');
    I.executeScript(selector => { document.querySelector(selector).scrollTop = 0; }, list);
    I.clickCss(`${card} tbody tr:first-child a`);
    I.seeInCurrentUrl(`/apps/form/admin/detail/?formName=${formName}&id=${items[0].id}`);
    DTE.waitForEditor('formDetailDataTable');
    DTE.cancel('formDetailDataTable');
    I.stopMockingRoute(overviewRoute);
    I.stopMockingRoute(dashboardPageRoute);
});

Scenario('Empty selected form shows an empty preview without loading details', async ({ I }) => {
    let detailRequests = 0;
    await I.mockRoute(detailRoute, route => { detailRequests++; return route.continue(); });
    await openDashboard(I, { total: 0, items: [] });
    I.see('0', `${card} .md-dashboard-widget__number`);
    I.dontSeeElementInDOM(`${card} tbody tr`);
    I.assertEqual(detailRequests, 0);
    I.stopMockingRoute(detailRoute);
    I.stopMockingRoute(overviewRoute);
    I.stopMockingRoute(dashboardPageRoute);
});

Scenario('Denied submission details show an error with no partial form preview', async ({ I }) => {
    await I.mockRoute(detailRoute, route => route.fulfill({ status: 403, contentType: 'application/json', body: '{}' }));
    await openDashboard(I, { total: 1, items: [{ id: '168347' }] });
    I.see(await I.executeScript(() => WJ.translate('admin.dashboard.permissionDenied.js')), `${card} .md-dashboard__widget-content > .text-danger`);
    I.dontSeeElementInDOM(`${card} .md-dashboard-widget__number`);
    I.dontSeeElementInDOM(`${card} tbody tr`);
    I.stopMockingRoute(detailRoute);
    I.stopMockingRoute(overviewRoute);
    I.stopMockingRoute(dashboardPageRoute);
});
