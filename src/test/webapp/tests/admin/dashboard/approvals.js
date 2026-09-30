const { waitForWidgets, mockDashboardBootstrap, dashboardPageRoute } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.approvals');

const card = '[data-widget-type="approvals"]';
const listUrl = '/admin/v9/webpages/web-pages-list/?show=toapprove';
const pageRoute = '**/admin/rest/webpages/toapprove/all*';
const groupRoute = '**/admin/rest/groups/toapprove/all*';
const documentRoute = `**${listUrl}`;
const approvalUrl = '/admin/approve.jsp?docid=18&historyid=108';
const approvalRoute = `**${approvalUrl}`;
const folderApprovalUrl = '/admin/v9/webpages/web-pages-list/?groupid=27&scheduleId=207';
const routes = [dashboardPageRoute, pageRoute, groupRoute, documentRoute, approvalRoute];
const result = (content = []) => ({ content, totalElements: content.length, totalPages: content.length ? 1 : 0, size: 6, number: 0 });

// The page controller swaps docId/historyId for DataTables.
const pages = result([
    { docId: 108, historyId: 18, title: 'autotest page update', authorName: 'autotest author', saveDate: 1789906080000 }
]);
const groups = result([
    { groupId: 27, schedulerId: 207, groupName: 'autotest folder update', userFullName: 'autotest author', saveDate: 1789906020000 }
]);

Before(({ login }) => login('admin'));

/** Resets context routes so each scenario also works with the repository's session restart mode. */
async function clearRoutes(I) {
    for (const route of routes) await I.stopMockingRoute(route);
}

/** Supplies current-user approval lists without writing a dashboard profile or approval records. */
async function openDashboard(I, pageData = pages) {
    await clearRoutes(I);
    const settings = {
        version: 1, configured: true, legacyBookmarksHandled: true, domainOptions: {}, items: [
            { id: 'autotest-approval-preview', type: 'approvals', size: '3x3', options: {} }
        ]
    };
    await mockDashboardBootstrap(I, () => ({ settings, notices: [] }));
    for (const [pattern, body] of [[pageRoute, pageData], [groupRoute, groups]]) {
        await I.mockRoute(pattern, route => route.fulfill({
            status: 200, contentType: 'application/json', body: JSON.stringify(body)
        }));
    }
    I.amOnPage('/admin/v9/');
    await waitForWidgets(I);
}

/** Checks that request links open separately while the heading and metric navigate to the approval list. */
Scenario('Approval actions open separately from dashboard list navigation', async ({ I }) => {
    await openDashboard(I);
    for (const href of [approvalUrl, folderApprovalUrl]) {
        I.seeElement(`${card} tbody a[href="${href}"][target="_blank"][rel~="noopener"]`);
    }
    for (const selector of ['.md-dashboard-widget__number', '.md-dashboard__title-link']) {
        I.assertEqual(await I.grabAttributeFrom(`${card} ${selector}`, 'href'), listUrl);
        const target = await I.grabAttributeFrom(`${card} ${selector}`, 'target');
        I.assertTrue(!target || target === '_self', 'List navigation must remain in the current window.');
    }
    // Check browser navigation without loading a real approval form for the fixture's identifiers.
    await I.mockRoute(approvalRoute, route => route.fulfill({ status: 200, contentType: 'text/html',
        body: '<!doctype html><html><body><h1>autotest approval destination</h1></body></html>' }));
    const tabCount = await I.grabNumberOfOpenTabs();
    I.clickCss(`${card} tbody a[href="${approvalUrl}"]`);
    I.waitForNumberOfTabs(tabCount + 1, 10);
    I.switchToNextTab();
    I.waitForText('autotest approval destination', 10);
    I.seeInCurrentUrl(approvalUrl);
    I.assertFalse(await I.executeScript(() => Boolean(window.opener)), 'The approval window must not retain an opener.');
    I.closeCurrentTab();
    I.seeElement(`${card} .md-dashboard__title-link`);
});

for (const hasPages of [false, true]) {
    /**
     * Checks where the approval heading takes the user: to page requests when both lists contain work, or
     * directly to folder requests when only folders need approval.
     */
    Scenario(`The approval deep link selects ${hasPages ? 'pages when both queues have requests' : 'folders when only folders need approval'}`, async ({ I }) => {
        await openDashboard(I, hasPages ? pages : result());
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

/**
 * Removes the simulated approval responses and reopens the dashboard with its normal data sources for
 * subsequent tests.
 */
Scenario('Restore unmocked approval routes', async ({ I }) => {
    I.closeOtherTabs();
    await clearRoutes(I);
    I.amOnPage('/admin/v9/');
    I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
});
