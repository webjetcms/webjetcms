Feature('admin.dashboard.data');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForFunction(() => typeof window.csrfToken === 'string' && window.csrfToken.length > 0, 20);
});

/**
 * Checks that the initial layout, notices, active sessions and online administrators are supplied with the
 * dashboard page. They must still appear without additional requests to the removed dashboard data services.
 */
Scenario('Initial settings, notices, sessions and administrators render from HTML without REST requests', async ({ I }) => {
    const requests = [];
    const routes = ['**/admin/rest/dashboard/settings', '**/admin/rest/dashboard/notices', '**/admin/rest/dashboard/data/sessions*', '**/admin/rest/dashboard/data/logged-admins*'];
    for (const pattern of routes) await I.mockRoute(pattern, route => {
        if (route.request().method() !== 'GET') return route.continue();
        requests.push(route.request().url());
        return route.fulfill({ status: 503, contentType: 'application/json', body: '{}' });
    });
    try {
        I.refreshPage();
        I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
        I.waitForElement('.md-dashboard__sessions .md-dashboard-widget__session-current', 20);
        await I.waitForFunction(() => document.querySelector('.md-dashboard__notice-list')?.getAttribute('aria-busy') === 'false', 20);
        I.assertEqual(requests.length, 0, 'The initial dashboard must render even when the removed read endpoints are unavailable.');
        I.assertTrue(await I.executeScript(() => {
            const dashboard = document.querySelector('webjet-overview-dashboard');
            return Array.isArray(dashboard.data.notices) && dashboard.dashboardController.settings.items.length > 0
                && dashboard.data.loggedAdmins.length > 0 && dashboard.data.loggedAdmins.every(admin => typeof admin.fullName === 'string');
        }), 'The page must contain real server-provided notices, online administrators and initialized widget preferences.');
    } finally {
        for (const pattern of routes) await I.stopMockingRoute(pattern);
    }
});

/**
 * Checks that the forms data available to the dashboard includes each form name, its submission count and
 * its latest submission date when one exists.
 */
Scenario('Forms reuse the module list with submission counts and latest dates', async ({ I }) => {
    const { status, body } = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/forms-list/all', { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
        return { status: response.status, body: await response.json() };
    });
    I.assertEqual(status, 200, 'Forms must load for an authorized administrator.');
    I.assertTrue(Array.isArray(body.content));
    for (const form of body.content) {
        I.assertEqual(typeof form.formName, 'string');
        I.assertTrue(Number.isInteger(form.count) && form.count >= 0);
        I.assertTrue(form.createDate === null || Number.isFinite(form.createDate));
    }
});

/**
 * Checks that page and folder approval data includes at most six recent requests per list and the full
 * waiting count. Dates and request identifiers must be present so the dashboard can order requests and link
 * to the right approval.
 */
Scenario('The shared approval lists expose bounded pages and their full request totals', async ({ I }) => {
    const responses = await I.executeScript(async () => Promise.all(['webpages', 'groups'].map(async type => {
        const response = await fetch(`/admin/rest/${type}/toapprove/all?size=6&page=0&sort=saveDate,desc`, {
            credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken }
        });
        return { type, status: response.status, body: await response.json() };
    })));
    for (const { type, status, body } of responses) {
        I.assertEqual(status, 200, `${type} approvals must use the existing authorized list endpoint.`);
        I.assertTrue(Array.isArray(body.content) && body.content.length <= 6);
        I.assertTrue(Number.isInteger(body.totalElements) && body.totalElements >= body.content.length);
        const timestamps = body.content.map(item => Number(new Date(item.saveDate)));
        I.assertTrue(timestamps.every(Number.isFinite), 'Approval submission dates must be available to merge both previews.');
        I.assertDeepEqual(timestamps, [...timestamps].sort((left, right) => right - left));
        for (const item of body.content) {
            if (type === 'webpages') {
                I.assertTrue(item.docId > 0 && item.historyId > 0, 'The existing page list must retain both swapped approval identifiers.');
            } else {
                I.assertTrue(item.schedulerId > 0, 'Folder actions must retain the pending scheduler identifier.');
                I.assertEqual(typeof item.userFullName, 'string');
            }
        }
    }
});

/**
 * Checks that recent-page data contains at most six entries, newest first, with the titles, paths and image
 * information needed for page previews.
 */
Scenario('Recent pages reuse the Web pages list with bounded pagination', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const params = new URLSearchParams({ groupId: document.querySelector('webjet-overview-dashboard').config.recentPagesGroupId, size: 6, page: 0, sort: 'dateCreated,desc' });
        const response = await fetch(`/admin/rest/web-pages/all?${params}`, { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
        return { status: response.status, body: await response.json() };
    });
    I.assertEqual(result.status, 200);
    I.assertTrue(Array.isArray(result.body.content) && result.body.content.length <= 6);
    I.assertTrue(Number.isInteger(result.body.totalElements) && result.body.totalElements >= result.body.content.length);
    const dates = result.body.content.map(page => page.dateCreated);
    I.assertDeepEqual(dates, [...dates].sort((a, b) => b - a));
    for (const page of result.body.content) {
        I.assertTrue(page.docId > 0 && Number.isFinite(page.dateCreated));
        I.assertEqual(typeof page.title, 'string');
        I.assertEqual(typeof page.fullPath, 'string');
        I.assertEqual(typeof page.perexImage, 'string');
    }
});

/**
 * Checks that the dashboard can obtain recent page changes with authors and dates, together with the
 * publication and expiry dates needed to show scheduled changes.
 */
Scenario('Changed pages and publishing reuse the shared audit page lists', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const read = async path => {
            const response = await fetch(path, { headers: { 'X-CSRF-Token': window.csrfToken } });
            return { status: response.status, body: await response.json() };
        };
        return {
            changed: await read('/admin/rest/web-pages/all?auditVersion=true&size=6&page=0&sort=dateCreated%2Cdesc'),
            publishing: await read('/admin/rest/web-pages/history/all?auditVersion=true')
        };
    });
    for (const response of Object.values(result)) {
        I.assertEqual(response.status, 200);
        I.assertFalse(Boolean(response.body.error));
        I.assertTrue(Array.isArray(response.body.content));
        for (const page of response.body.content) {
            I.assertTrue(page.docId > 0);
            I.assertEqual(typeof page.title, 'string');
        }
    }
    I.assertEqual(result.changed.body.content.length, 6);
    I.assertTrue(result.changed.body.totalElements >= 6);
    for (const [index, page] of result.changed.body.content.entries()) {
        I.assertEqual(typeof page.authorName, 'string');
        I.assertEqual(typeof page.perexImage, 'string');
        I.assertTrue(Number.isFinite(page.dateCreated));
        if (index > 0) I.assertTrue(result.changed.body.content[index - 1].dateCreated >= page.dateCreated);
    }
    for (const page of result.publishing.body.content) {
        I.assertEqual(typeof page.disableAfterEnd, 'boolean');
        I.assertTrue(page.publishStartDate === null || Number.isFinite(page.publishStartDate));
        I.assertTrue(page.publishEndDate === null || Number.isFinite(page.publishEndDate));
    }
});

/**
 * Checks that activity and newsletter data is returned newest first in limited lists. Activity entries must
 * have understandable event labels, and campaigns must include their status and available sending counts.
 */
Scenario('Audit and newsletter reuse bounded module lists', async ({ I }) => {
    const responses = await I.executeScript(async () => {
        const read = async path => {
            const response = await fetch(path, { headers: { 'X-CSRF-Token': window.csrfToken } });
            return { status: response.status, body: await response.json() };
        };
        return {
            audit: await read('/admin/rest/audit/log/all?size=6&page=0&sort=id%2Cdesc'),
            newsletter: await read('/admin/rest/dmail/campaings/all?size=14&page=0&sort=id%2Cdesc')
        };
    });
    for (const [type, response] of Object.entries(responses)) {
        I.assertEqual(response.status, 200);
        I.assertFalse(Boolean(response.body.error));
        I.assertTrue(response.body.content.length > 0 && response.body.content.length <= (type === 'audit' ? 6 : 14));
        for (const [index, item] of response.body.content.entries()) {
            if (index > 0) I.assertTrue(response.body.content[index - 1].id > item.id);
            if (type === 'audit') {
                I.assertEqual(typeof item.description, 'string');
                I.assertTrue(Number.isFinite(item.createDate));
                I.assertTrue(response.body.options.logType.some(option => String(option.value) === String(item.logType)));
            } else {
                I.assertEqual(typeof item.subject, 'string');
                I.assertEqual(typeof item.editorFields.status, 'string');
                I.assertTrue(item.countOfRecipients === null || Number.isInteger(item.countOfRecipients));
                I.assertTrue(item.countOfSentMails === null || Number.isInteger(item.countOfSentMails));
            }
        }
    }
});

/**
 * Checks that server monitoring supplies current memory and CPU readings with a recent timestamp, without
 * requiring previously recorded monitoring history.
 */
Scenario('Live monitoring reads a current server snapshot independently of persisted history', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/monitoring/actual', { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
        return { status: response.status, body: await response.json(), received: Date.now() };
    });
    I.assertEqual(result.status, 200);
    I.assertTrue(Math.abs(result.received - result.body.serverActualTime) < 30000, 'The monitoring endpoint must return a fresh server timestamp.');
    for (const field of ['memUsed', 'memFree', 'memTotal']) I.assertTrue(Number.isFinite(result.body[field]) && result.body[field] >= 0, `${field} must contain the current byte count.`);
    I.assertEqual(result.body.memUsed + result.body.memFree, result.body.memTotal);
    for (const field of ['cpuUsage', 'cpuUsageProcess']) I.assertTrue(Number.isFinite(result.body[field]), `${field} must contain the current CPU reading or the unavailable sentinel.`);
});

/**
 * Checks that a selected form returns only submissions from that form and period, newest first. The preview
 * is limited to ten rows while the total includes all matching submissions.
 */
Scenario('Selected forms reuse the module date filter, count and descending pagination', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const formName = 'Multistepform_screens';
        const to = Date.now(), from = to - 90 * 86400000;
        const params = new URLSearchParams({ detail: true, formName, searchCreateDate: `daterange:${from}-${to}`, size: 10, page: 0, sort: 'createDate,desc' });
        const response = await fetch(`/admin/rest/forms-list/search/findByColumns?${params}`, { headers: { 'X-CSRF-Token': window.csrfToken } });
        return { status: response.status, body: await response.json(), from, to, formName };
    });
    I.assertEqual(result.status, 200);
    I.assertTrue(Number.isInteger(result.body.totalElements) && result.body.totalElements >= result.body.content.length);
    I.assertTrue(result.body.content.length <= 10);
    for (const [index, item] of result.body.content.entries()) {
        I.assertEqual(item.formName, result.formName);
        I.assertTrue(item.createDate >= result.from && item.createDate <= result.to);
        if (index > 0) I.assertTrue(result.body.content[index - 1].createDate >= item.createDate);
    }
});

/**
 * Checks that traffic data covers the requested days and that popular pages include visit counts, titles,
 * paths and optional thumbnails for the dashboard preview.
 */
Scenario('Traffic and TOP pages reuse the statistics module contracts', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const root = document.querySelector('webjet-overview-dashboard').data.statRootGroupId;
        const to = new Date(); to.setHours(0, 0, 0, 0);
        const from = new Date(to); from.setDate(from.getDate() - 14);
        const params = new URLSearchParams({ searchRootDir: root, searchDayDate: `daterange:${from.getTime()}-${to.getTime() - 1}`,
            searchFilterBotsOut: true, statType: 'days', size: 14, page: 0, sort: 'order,asc', pagination: true });
        const read = async type => {
            const response = await fetch(`/admin/rest/stat/${type}/search/findByColumns?${params}`, { headers: { 'X-CSRF-Token': window.csrfToken } });
            return { status: response.status, body: await response.json() };
        };
        const views = await read('views');
        params.set('size', '6');
        // Reuse the statistics module's populated fixture period to verify cached page metadata.
        params.set('searchDayDate', `daterange:${new Date(2022, 4, 1).getTime()}-${new Date(2022, 5, 1).getTime() - 1}`);
        params.set('searchFilterBotsOut', 'false');
        const top = await read('top');
        return { root, views, top };
    });
    I.assertTrue(result.root > 0);
    I.assertEqual(result.views.status, 200);
    I.assertEqual(result.views.body.content.length, 14);
    for (const day of result.views.body.content) {
        I.assertTrue(Number.isFinite(day.dayDate));
        for (const metric of ['visits', 'sessions', 'uniqueUsers']) I.assertTrue(Number.isFinite(day[metric]) && day[metric] >= 0);
    }
    I.assertEqual(result.top.status, 200);
    I.assertTrue(result.top.body.content.length > 0 && result.top.body.content.length <= 100,
        'The statistics module returns a ranking capped at 100; the widget displays its first six rows.');
    for (const page of result.top.body.content) {
        I.assertTrue(page.docId > 0 && page.visits >= 0);
        I.assertEqual(typeof page.title, 'string');
        I.assertEqual(typeof page.name, 'string');
        I.assertTrue(page.perexImage === null || typeof page.perexImage === 'string');
    }
});

/**
 * Checks that search terms and referring sites include counts and percentages. Error data must also provide
 * the full request total, rather than counting only the displayed preview rows.
 */
Scenario('Search terms, referrers and errors reuse statistics responses and summaries', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const root = document.querySelector('webjet-overview-dashboard').data.statRootGroupId;
        const read = async (type, filters) => {
            const params = new URLSearchParams({ searchDayDate: `daterange:${new Date(2022, 4, 1).getTime()}-${new Date(2022, 5, 1).getTime() - 1}`,
                searchRootDir: root, size: 6, page: 0, sort: 'order,asc', pagination: true, ...filters });
            const response = await fetch(`/admin/rest/stat/${type}/search/findByColumns?${params}`, { headers: { 'X-CSRF-Token': window.csrfToken } });
            return { status: response.status, body: await response.json() };
        };
        return {
            queries: await read('search-engines', { searchWebPage: -1, searchEngine: '' }),
            sources: await read('referer', { searchChartType: 'not_chart' }),
            errors: await read('error', { searchFilterBotsOut: false, searchurl: '', sort: 'count,desc',
                searchDayDate: `daterange:${new Date(2023, 9, 1).getTime()}-${new Date(2023, 10, 1).getTime() - 1}` })
        };
    });
    for (const response of Object.values(result)) {
        I.assertEqual(response.status, 200);
        I.assertFalse(Boolean(response.body.error));
        I.assertTrue(response.body.content.length > 0);
    }
    for (const [key, name, count] of [['queries', 'queryName', 'queryCount'], ['sources', 'serverName', 'visits']]) {
        I.assertTrue(result[key].body.content.length <= 100);
        for (const row of result[key].body.content) {
            I.assertEqual(typeof row[name], 'string');
            I.assertTrue(Number.isFinite(row[count]) && row[count] >= 0);
            I.assertTrue(Number.isFinite(row.percentage) && row.percentage >= 0 && row.percentage <= 100);
        }
    }
    I.assertEqual(result.errors.body.content.length, 6);
    I.assertTrue(result.errors.body.summary.count >= result.errors.body.content.reduce((sum, row) => sum + row.count, 0));
    for (const row of result.errors.body.content) I.assertEqual(typeof row.url, 'string');
});
