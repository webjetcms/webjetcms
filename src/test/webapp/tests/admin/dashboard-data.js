Feature('admin.dashboard-data');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/');
    I.waitForFunction(() => typeof window.csrfToken === 'string' && window.csrfToken.length > 0, 20);
});

Scenario('Read-only widget projections return bounded preview contracts', async ({ I }) => {
    const results = await I.executeScript(async () => {
        const types = ['approvals', 'publishing', 'forms', 'traffic', 'top-pages', 'search-terms', 'referrers', 'newsletter', 'errors', 'sessions'];
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
        if (type === 'sessions') I.assertTrue(Array.isArray(body.currentSessions.userSessions));
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

Scenario('Recent pages remain available through the independent pilot projection', async ({ I }) => {
    const result = await I.executeScript(async () => {
        const response = await fetch('/admin/rest/dashboard/recent-pages?size=6', { credentials: 'same-origin', headers: { 'X-CSRF-Token': window.csrfToken } });
        return { status: response.status, body: await response.json() };
    });
    I.assertEqual(result.status, 200);
    I.assertTrue(Array.isArray(result.body) && result.body.length <= 6);
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
