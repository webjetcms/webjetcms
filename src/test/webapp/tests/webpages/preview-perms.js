Feature('webpages.preview-perms');

// Handlers inherited from WebpagesDatatable must check the permission of the controller that exposes them.
const controllers = {
    webpages: { url: '/admin/rest/web-pages', perm: 'menuWebpages' },
    news: { url: '/admin/rest/news/news-list', perm: 'cmp_news' }
};

Before(({ login }) => {
    login('admin');
});

/**
 * Calls the preview and http-headers handlers of the given controller from the admin page context.
 * Returns parsed JSON responses, an authorization error is returned as {"error":"Access Denied"}.
 */
async function callHandlers(I, baseUrl) {
    return I.executeScript(async (baseUrl) => {
        const page = {
            docId: -1,
            groupId: 1,
            title: 'autotest-preview-perms',
            data: '<p>autotest-preview-perms</p>',
            editorFields: { groupDetails: { groupId: 1 } }
        };
        const post = async (path) => {
            const response = await fetch(baseUrl + path, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken },
                body: JSON.stringify(page)
            });
            return response.json();
        };
        return { preview: await post('/preview/'), headers: await post('/http-headers') };
    }, baseUrl);
}

function assertAllowed(I, result, name) {
    I.assertEqual(result.preview.ok, true, name + ': preview must be allowed');
    I.assertTrue(typeof result.headers['Referrer-Policy'] !== 'undefined', name + ': http-headers must be allowed');
}

function assertDenied(I, result, name) {
    I.assertEqual(result.preview.error, 'Access Denied', name + ': preview must be denied');
    I.assertEqual(result.headers.error, 'Access Denied', name + ': http-headers must be denied');
}

Scenario('preview and http-headers are allowed with module permission', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    assertAllowed(I, await callHandlers(I, controllers.webpages.url), 'web-pages');
    assertAllowed(I, await callHandlers(I, controllers.news.url), 'news');
});

Scenario('preview and http-headers require menuWebpages on web-pages controller', async ({ I }) => {
    I.amOnPage('/admin/v9/?removePerm=' + controllers.webpages.perm);
    assertDenied(I, await callHandlers(I, controllers.webpages.url), 'web-pages without menuWebpages');
    // News controller checks cmp_news only, so it stays available.
    assertAllowed(I, await callHandlers(I, controllers.news.url), 'news without menuWebpages');
    // Logout restores the removed permission for the next scenario.
    I.amOnPage('/logoff.do?forward=/admin/');
});

Scenario('preview and http-headers require cmp_news on news controller', async ({ I }) => {
    I.amOnPage('/admin/v9/?removePerm=' + controllers.news.perm);
    assertDenied(I, await callHandlers(I, controllers.news.url), 'news without cmp_news');
    assertAllowed(I, await callHandlers(I, controllers.webpages.url), 'web-pages without cmp_news');
    I.amOnPage('/logoff.do?forward=/admin/');
});
