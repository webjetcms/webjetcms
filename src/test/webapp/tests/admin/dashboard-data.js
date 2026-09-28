Feature('admin.dashboard-data');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForFunction(() => typeof window.csrfToken === 'string' && window.csrfToken.length > 0, 20);
});

Scenario('Initial settings, notices and sessions render from HTML without REST requests', async ({ I }) => {
    const requests = [];
    const routes = ['**/admin/rest/dashboard/settings', '**/admin/rest/dashboard/notices', '**/admin/rest/dashboard/data/sessions*'];
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
        I.assertEqual(requests.length, 0, 'The initial dashboard must render even when all three read endpoints are unavailable.');
        I.assertTrue(await I.executeScript(() => {
            const dashboard = document.querySelector('webjet-overview-dashboard');
            return Array.isArray(dashboard.data.notices) && dashboard.dashboardController.settings.items.length > 0;
        }), 'The page must contain real server-provided notices and initialized widget preferences.');
    } finally {
        for (const pattern of routes) await I.stopMockingRoute(pattern);
    }
});

Scenario('Read-only widget projections return bounded preview contracts', async ({ I }) => {
    const results = await I.executeScript(async () => {
        const types = ['publishing', 'forms', 'traffic', 'top-pages', 'search-terms', 'referrers', 'newsletter', 'errors'];
        const results = [];
        for (const type of types) {
            const response = await fetch(`/admin/rest/dashboard/data/${type}`, { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
            const body = await response.json();
            results.push({ type, status: response.status, body });
        }
        return results;
    });
    for (const { type, status, body } of results) {
        I.say(`${type}: HTTP ${status}, keys=${Object.keys(body).join(',')}`);
        if (type === 'errors' && status === 503) {
            I.assertEqual(body.reason, 'domain-unavailable', 'Shared-domain legacy 404 data must explicitly report unsupported domain scoping.');
            continue;
        }
        I.assertEqual(status, 200, `${type} must load for an authorized administrator.`);
        I.assertTrue(Number.isInteger(body.total) && body.total >= 0, `${type} must expose an actual nonnegative count.`);
        I.assertTrue(Array.isArray(body.items) && body.items.length <= 6, `${type} previews must be bounded.`);
        for (const item of body.items) {
            I.assertEqual(typeof item.title, 'string');
            I.assertTrue(typeof item.url === 'string' && item.url.startsWith('/'), 'Preview links must stay in the administration.');
            if (type === 'forms') I.assertStartsWith(item.url, '/apps/form/admin/detail/?formName=');
            if (type === 'top-pages') I.assertStartsWith(item.url, '/apps/stat/admin/top-details/?docId=');
        }
        if (type === 'traffic') {
            I.assertEqual(body.series.length, 7);
            I.assertEqual(body.previousSeries.length, 7);
            I.assertTrue(body.from < body.to);
        }
        if (type === 'newsletter' && body.selectedId) I.assertEqual(body.items[0].id, body.selectedId);
    }
});

Scenario('Invalid projection settings are rejected before querying data', async ({ I }) => {
    const statuses = await I.executeScript(async () => {
        return Promise.all(['traffic?days=365', 'traffic?metric=invalid', 'forms?formName=', 'newsletter?campaignId=-1', 'unknown'].map(async value => {
            const response = await fetch(`/admin/rest/dashboard/data/${value}`, { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
            return { value, status: response.status };
        }));
    });
    for (const result of statuses) I.assertEqual(result.status, 400, `${result.value} must be rejected.`);
});

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

Scenario('Recent pages remain available through the independent pilot projection', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/dashboard/recent-pages?size=6', { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
        return { status: response.status, body: await response.json() };
    });
    I.assertEqual(result.status, 200);
    I.assertTrue(Array.isArray(result.body) && result.body.length <= 6);
});

Scenario('Former overview blocks expose independent authorized widget projections', async ({ I }) => {
    const results = await I.executeScript(async () => Promise.all(['changed-pages', 'audit', 'logged-admins'].map(async type => {
        const response = await fetch(`/admin/rest/dashboard/data/${type}`, { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
        return { type, status: response.status, body: await response.json() };
    })));
    for (const { type, status, body } of results) {
        I.assertEqual(status, 200, `${type} must load for an authorized administrator.`);
        I.assertTrue(Array.isArray(body.items), `${type} must contain an item projection.`);
        if (type === 'logged-admins') I.assertEqual(body.items.length, body.total, 'All authorized online administrators must remain reachable in the scrolling card.');
        else I.assertTrue(body.items.length <= 6, `${type} must use a bounded activity preview.`);
        for (const item of body.items) {
            if (type === 'logged-admins') {
                I.assertEqual(typeof item.fullName, 'string');
                continue;
            }
            I.assertTrue(Number.isFinite(item.date), 'Activity timestamps must remain machine-readable.');
            I.assertTrue(typeof item.url === 'string' && item.url.startsWith('/'), 'Activity links must stay in the administration.');
            if (type === 'changed-pages') I.assertStartsWith(item.url, '/admin/v9/webpages/web-pages-list/?docid=');
            else I.assertEqual(typeof item.description, 'string');
        }
    }
});

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

Scenario('Statistical metrics and selected form projections preserve their contracts', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const get = async path => {
            const response = await fetch(`/admin/rest/dashboard/data/${path}`, { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
            return { status: response.status, body: response.headers.get('content-type')?.includes('json') ? await response.json() : null };
        };
        const metrics = [];
        for (const metric of ['views', 'sessions', 'uniqueUsers']) metrics.push(await get(`traffic?days=30&metric=${metric}`));
        const forms = await get('forms');
        const selectedName = forms.body.options?.[0]?.id;
        const selected = selectedName ? await get(`forms?formName=${encodeURIComponent(selectedName)}`) : null;
        const missing = await get('forms?formName=missing-dashboard-form-autotest');
        return { metrics, selectedName, selected, missingStatus: missing.status };
    });
    for (const metric of result.metrics) {
        I.assertEqual(metric.status, 200);
        I.assertEqual(metric.body.series.length, 30);
        I.assertEqual(metric.body.previousSeries.length, 30);
    }
    I.assertTrue(result.metrics[0].body.total >= result.metrics[1].body.total);
    I.assertTrue(result.metrics[0].body.total >= result.metrics[2].body.total);
    if (result.selected) {
        I.assertEqual(result.selected.status, 200);
        I.assertTrue(result.selected.body.items.every(item => item.title === result.selectedName));
    }
    I.assertEqual(result.missingStatus, 404);
});
