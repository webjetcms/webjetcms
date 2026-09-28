const { waitForWidgets } = require('../../helpers/dashboard-browser');

Feature('admin.dashboard-approvals');

const card = '[data-widget-type="approvals"]';
const listUrl = '/admin/v9/webpages/web-pages-list/?show=toapprove';
const settingsRoute = '**/admin/rest/dashboard/settings';
const noticesRoute = '**/admin/rest/dashboard/notices';
const pageRoute = '**/admin/rest/webpages/toapprove/all*';
const groupRoute = '**/admin/rest/groups/toapprove/all*';
const documentRoute = `**${listUrl}`;
const approvalUrl = '/admin/approve.jsp?docid=18&historyid=108';
const approvalRoute = `**${approvalUrl}`;
const routes = [settingsRoute, noticesRoute, pageRoute, groupRoute, documentRoute, approvalRoute];
const date = order => Date.UTC(2026, 8, 20, 12, order);
const result = (content = [], totalElements = content.length) => ({ content, totalElements, totalPages: Math.ceil(totalElements / 6), size: 6, number: 0 });

// The page controller swaps docId/historyId so repeated document IDs remain unique in DataTables.
const pages = result([
    { docId: 108, historyId: 18, title: 'autotest page update', authorName: 'autotest page author', saveDate: date(8), isDelete: false },
    { docId: 106, historyId: 16, title: 'autotest page deletion', authorName: 'autotest page author', saveDate: date(6), isDelete: true },
    { docId: 104, historyId: 14, title: '[DELETE] autotest legacy deletion', authorName: 'autotest page author', saveDate: date(4), isDelete: false },
    { docId: 101, historyId: 11, title: 'autotest older page', authorName: 'autotest page author', saveDate: date(1), isDelete: false }
], 17);
const groups = result([
    { groupId: 27, schedulerId: 207, groupName: 'autotest folder update', userFullName: 'autotest folder author', saveDate: date(7), isDelete: false },
    { groupId: 25, schedulerId: 205, groupName: 'autotest folder deletion', userFullName: 'autotest folder author', saveDate: date(5), isDelete: true },
    { groupId: 23, schedulerId: 203, groupName: 'autotest sixth request', userFullName: 'autotest folder author', saveDate: date(3), isDelete: false },
    { groupId: 22, schedulerId: 202, groupName: 'autotest older folder', userFullName: 'autotest folder author', saveDate: date(2), isDelete: false }
], 9);

Before(({ login }) => login('admin'));

/** Resets context routes so each scenario also works with the repository's session restart mode. */
async function clearRoutes(I) {
    for (const route of routes) await I.stopMockingRoute(route);
}

/** Supplies current-user approval lists without writing a dashboard profile or approval records. */
async function openDashboard(I, pageData = pages, groupData = groups, denied) {
    await clearRoutes(I);
    const requests = [];
    await I.mockRoute(settingsRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
        version: 1, configured: true, legacyBookmarksHandled: true, domainOptions: {}, items: [
            { id: 'autotest-approval-preview', type: 'approvals', size: '3x3', options: {} }
        ]
    }) }));
    await I.mockRoute(noticesRoute, route => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
    for (const [type, pattern, body] of [['pages', pageRoute, pageData], ['groups', groupRoute, groupData]]) {
        await I.mockRoute(pattern, route => {
            const request = route.request();
            requests.push({ type, url: request.url(), csrf: Boolean(request.headers()['x-csrf-token']) });
            let status = 200, payload = body;
            if (type === denied?.type) {
                status = denied.status;
                payload = { error: denied.error || 'Forbidden', data: [] };
            }
            return route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(payload) });
        });
    }
    I.amOnPage('/admin/v9/');
    await waitForWidgets(I);
    return requests;
}

Scenario('Merge the latest six requests and keep direct approval actions separate from list navigation', async ({ I }) => {
    const requests = await openDashboard(I);
    const state = await I.executeScript(selector => {
        const widget = document.querySelector(selector);
        const readLink = link => ({ title: link.textContent, href: link.getAttribute('href'), target: link.target, rel: link.rel });
        return {
            rows: [...widget.querySelectorAll('tbody tr')].map(row => ({ link: readLink(row.querySelector('a')), requester: row.cells[1].textContent })),
            total: readLink(widget.querySelector('.md-dashboard-widget__number')),
            header: readLink(widget.querySelector('.md-dashboard__title-link'))
        };
    }, card);
    I.assertDeepEqual(state.rows.map(row => row.link.title), [
        'autotest page update', 'autotest folder update', 'autotest page deletion',
        'autotest folder deletion', '[DELETE] autotest legacy deletion', 'autotest sixth request'
    ], 'The two lists must be merged by request date before applying the six-row limit.');
    I.assertDeepEqual(state.rows.map(row => row.link.href), [
        '/admin/approve.jsp?docid=18&historyid=108', '/admin/v9/webpages/approve-group/?scheduleId=207',
        '/admin/approve_delete.jsp?docid=16&historyid=106', '/admin/v9/webpages/approve-del-group/?scheduleId=205',
        '/admin/approve_delete.jsp?docid=14&historyid=104', '/admin/v9/webpages/approve-group/?scheduleId=203'
    ], 'The page IDs and folder scheduler IDs must address the pending request, including legacy deletion titles.');
    for (const row of state.rows) {
        I.assertEqual(row.link.target, '_blank');
        I.assertContain(row.link.rel.split(/\s+/), 'noopener');
        I.assertContain(row.requester, 'autotest');
    }
    I.assertEqual(state.total.title, '26', 'The metric must add the full totals rather than count preview rows.');
    for (const link of [state.total, state.header]) {
        I.assertEqual(link.href, listUrl);
        I.assertTrue(link.target === '' || link.target === '_self', 'List navigation must remain in the current window.');
    }
    I.assertDeepEqual(requests.map(request => request.type).sort(), ['groups', 'pages']);
    for (const request of requests) {
        const params = new URL(request.url).searchParams;
        I.assertEqual(params.get('size'), '6');
        I.assertEqual(params.get('page'), '0');
        I.assertEqual(params.get('sort'), 'saveDate,desc');
        I.assertTrue(request.csrf, 'Both shared approval requests must include the current CSRF token.');
    }
    // Intercept only this fixture's destination; clicking cannot load or submit a real approval form.
    await I.mockRoute(approvalRoute, route => route.fulfill({ status: 200, contentType: 'text/html',
        body: '<!doctype html><html><body><h1>autotest approval destination</h1></body></html>' }));
    const tabCount = await I.grabNumberOfOpenTabs();
    I.clickCss(`${card} tbody tr:first-child a`);
    I.waitForNumberOfTabs(tabCount + 1, 10);
    I.switchToNextTab();
    I.waitForText('autotest approval destination', 10);
    I.seeInCurrentUrl(approvalUrl);
    I.assertFalse(await I.executeScript(() => Boolean(window.opener)), 'The approval window must not retain an opener.');
    I.closeCurrentTab();
    I.seeElement(`${card} .md-dashboard-widget__number`);
    I.stopMockingRoute(approvalRoute);
});

Scenario('Empty shared approval lists show an empty preview and a zero total', async ({ I }) => {
    await openDashboard(I, result(), result());
    I.see('0', `${card} .md-dashboard-widget__number`);
    I.dontSeeElementInDOM(`${card} tbody tr`);
    I.dontSeeElementInDOM(`${card} .md-dashboard__widget-content > .text-danger`);
    I.see(await I.executeScript(() => WJ.translate('admin.dashboard.empty.js')), `${card} .md-dashboard__widget-content`);
});

for (const type of ['pages', 'groups']) {
    Scenario(`A forbidden ${type} list shows an error instead of a partial approval total`, async ({ I }) => {
        await openDashboard(I, pages, groups, { type, status: 403 });
        I.seeElement(`${card} .md-dashboard__widget-content > .text-danger`);
        I.see(await I.executeScript(() => WJ.translate('admin.dashboard.permissionDenied.js')), `${card} .md-dashboard__widget-content > .text-danger`);
        I.dontSeeElementInDOM(`${card} .md-dashboard-widget__number`);
        I.dontSeeElementInDOM(`${card} tbody tr`);
        I.seeElement(`${card} .md-dashboard__widget-content > button`);
    });
}

for (const [type, error] of [['pages', 'Access Denied'], ['groups', 'Access is denied']]) {
    Scenario(`An HTTP 200 DataTable denial from ${type} retains the permission error`, async ({ I }) => {
        await openDashboard(I, pages, groups, { type, status: 200, error });
        I.see(await I.executeScript(() => WJ.translate('admin.dashboard.permissionDenied.js')), `${card} .md-dashboard__widget-content > .text-danger`);
        I.dontSeeElementInDOM(`${card} .md-dashboard-widget__number`);
        I.dontSeeElementInDOM(`${card} tbody tr`);
        I.seeElement(`${card} .md-dashboard__widget-content > button`);
    });
}

for (const hasPages of [false, true]) {
    Scenario(`The approval deep link selects ${hasPages ? 'pages when both queues have requests' : 'folders when only folders need approval'}`, async ({ I }) => {
        await openDashboard(I, hasPages ? pages : result(), groups);
        let replaced = 0;
        // Keep the real page and its shared tab handlers, replacing only server-rendered queue flags.
        await I.mockRoute(documentRoute, async route => {
            const response = await route.fetch();
            let body = await response.text();
            body = body.replace(/\blet hasPagesToApprove\s*=\s*(?:true|false)\s*;/, () => { replaced++; return `let hasPagesToApprove = ${hasPages};`; });
            body = body.replace(/\blet hasGroupsToApprove\s*=\s*(?:true|false)\s*;/, () => { replaced++; return 'let hasGroupsToApprove = true;'; });
            await route.fulfill({ response, body });
        });
        // Queue navigation is independent of table rows; avoid introducing full editor entities into this fixture.
        for (const pattern of [pageRoute, groupRoute]) await I.mockRoute(pattern, route => route.fulfill({
            status: 200, contentType: 'application/json', body: JSON.stringify(result())
        }));
        I.clickCss(`${card} .md-dashboard__title-link`);
        I.waitForElement('#pills-pages #pills-waiting-tab.active', 20);
        const expectedTab = hasPages ? 'pills-waiting-tab' : 'pills-waiting-folder-tab';
        I.waitForElement(`#pills-pages_sub #${expectedTab}.active`, 20);
        I.waitForVisible(hasPages ? '#datatableInit_wrapper' : '#groups-datatable_wrapper', 20);
        const destination = await I.executeScript(hasPages => hasPages ? webpagesDatatable.getAjaxUrl() : groupsDatatable.getAjaxUrl(), hasPages);
        I.assertContain(destination, hasPages ? '/admin/rest/webpages/toapprove' : '/admin/rest/groups/toapprove');
        I.assertEqual(replaced, 2, 'Both real template flags must be replaced for the deterministic deep-link fixture.');
    });
}

Scenario('Restore unmocked approval routes', async ({ I }) => {
    await clearRoutes(I);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});
