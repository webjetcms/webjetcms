Feature('admin.controller-perms');

// REST controllers used by a single admin page must check the permission of that page.
const controllers = [
    { name: 'prepared configuration', url: '/admin/rest/settings/prepared/all?mode=history&name=license', perm: 'menuConfig' },
    { name: 'gallery dimension', url: '/admin/rest/components/gallery-dimension/all?dimensionId=-1&path=/images/gallery', perm: 'menuGallery' }
];

Before(({ login }) => {
    login('admin');
});

/**
 * Reads the given REST URL from the admin page context and returns the parsed JSON response.
 * An authorization error is returned as {"error":"Access Denied"}.
 */
async function readJson(I, url) {
    return I.executeScript(async (url) => {
        const response = await fetch(url, { headers: { 'Accept': 'application/json', 'X-CSRF-Token': window.csrfToken } });
        return response.json();
    }, url);
}

Scenario('controllers are available with permission', async ({ I }) => {
    I.amOnPage('/admin/v9/');
    for (const controller of controllers) {
        const result = await readJson(I, controller.url);
        I.assertTrue(Array.isArray(result.content), controller.name + ': records must be returned');
    }
});

for (const controller of controllers) {
    Scenario('controller requires ' + controller.perm + ' - ' + controller.name, async ({ I }) => {
        I.amOnPage('/admin/v9/?removePerm=' + controller.perm);
        const result = await readJson(I, controller.url);
        I.assertEqual(result.error, 'Access Denied', controller.name + ' must be denied without ' + controller.perm);
        I.assertTrue(typeof result.content === 'undefined', controller.name + ': no records without ' + controller.perm);
        // Logout restores the removed permission for the next scenario.
        I.amOnPage('/logoff.do?forward=/admin/');
    });
}
