Feature('webpages.external-link-cache');

let suffix;

Before(({ I, login }) => {
    login('admin');
    if (!suffix) suffix = I.getRandomText();
});

function openPageEditor(I, DTE, folderName) {
    I.amOnPage('/admin/v9/webpages/web-pages-list/');
    I.jstreeClick(folderName);
    I.click(folderName, '#datatableInit');
    DTE.waitForEditor();
    I.clickCss('#pills-dt-datatableInit-basic-tab');
}

async function getResponse(I, url, anonymous = false) {
    // Standard navigation follows redirects; inspect their original headers without using the browser cache.
    return I.usePlaywrightTo('inspect page response', async ({ page, browser }) => {
        const absoluteUrl = new URL(url, page.url()).href;
        const context = anonymous ? await browser.newContext() : page.context();
        try {
            const response = await context.request.get(absoluteUrl, { maxRedirects: 0 });
            return { status: response.status(), headers: response.headers() };
        } finally {
            if (anonymous) await context.close();
        }
    });
}

for (const variant of [
    { name: 'public-relative', available: true, absolute: false },
    { name: 'hidden-relative', available: false, absolute: false },
    { name: 'public-absolute', available: true, absolute: true }
]) {
    Scenario('Changing and removing a page redirect - ' + variant.name, async ({ I, DTE }) => {
        const folderSuffix = suffix + '-' + variant.name;
        const folderName = 'name-autotest-' + folderSuffix;
        const targetPath = '/xiaomi?autotest=' + folderSuffix;
        I.amOnPage('/admin/v9/webpages/web-pages-list/');
        I.createFolderStructure(folderSuffix, false);
        I.click(folderName, '#datatableInit');
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        const sourceUrl = await I.grabValueFrom('#DTE_Field_virtualPath');
        const baseUrl = new URL(await I.grabCurrentUrl()).origin;
        const targetUrl = variant.absolute ? baseUrl + targetPath : targetPath;
        if (!variant.available) {
            I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_available_0').find('.form-check-label'));
            I.dontSeeCheckboxIsChecked('#DTE_Field_available_0');
        }
        I.fillField('#DTE_Field_externalLink', targetUrl);
        DTE.save();

        const redirect = await getResponse(I, sourceUrl);
        I.assertEqual(redirect.status, variant.absolute ? 302 : 301);
        I.assertEqual(redirect.headers.location, baseUrl + targetPath);
        I.assertContain(redirect.headers['cache-control'] || '', 'no-store');
        I.assertEqual(redirect.headers.pragma, 'no-cache');
        I.assertEqual(Date.parse(redirect.headers.expires), 0);

        const publicResponse = await getResponse(I, sourceUrl, true);
        if (variant.available) {
            I.assertEqual(publicResponse.status, redirect.status);
            I.assertEqual(publicResponse.headers.location, redirect.headers.location);
            // Absolute redirects can already carry headers that disable caching.
            if (!variant.absolute) I.assertNotContain(publicResponse.headers['cache-control'] || '', 'no-store');
        } else {
            // A custom error page can change the status; hidden pages must not redirect visitors.
            I.assertTrue(publicResponse.headers.location === undefined);
        }
        I.amOnPage(sourceUrl);
        I.seeInCurrentUrl(targetPath);

        openPageEditor(I, DTE, folderName);
        I.fillField('#DTE_Field_externalLink', targetUrl + '&changed=1');
        DTE.save();
        I.amOnPage(sourceUrl);
        I.seeInCurrentUrl(targetPath + '&changed=1');

        openPageEditor(I, DTE, folderName);
        I.fillField('#DTE_Field_externalLink', '');
        DTE.save();
        I.click(folderName, '#datatableInit');
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.seeInField('#DTE_Field_externalLink', '');
        DTE.cancel();

        const cleared = await getResponse(I, sourceUrl);
        I.assertEqual(cleared.status, 200);
        I.assertTrue(cleared.headers.location === undefined);
        I.amOnPage(sourceUrl);
        I.seeInCurrentUrl(sourceUrl);
        I.dontSeeInCurrentUrl('/xiaomi');
    });

    Scenario('Cleanup page redirect test - ' + variant.name, ({ I }) => {
        I.deleteFolderStructure(suffix + '-' + variant.name);
    });
}
