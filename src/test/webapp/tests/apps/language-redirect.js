Feature('apps.language-redirect');

/**
 * Helper to set Accept-Language header via Playwright's page context.
 * Sets extra HTTP headers for all subsequent requests in the test.
 */
async function setAcceptLanguage(I, lang) {
    /*const { page } = codeceptjs.helpers['Playwright']();
    return page.context().setExtraHTTPHeaders({
        'Accept-Language': lang
    });*/
    I.clearCookie('lng');
    I.setPlaywrightRequestHeaders({
        'Accept-Language': lang,
    });
}

/**
 * Verify that the current URL matches the expected path after redirect.
 */
async function seeRedirectedTo(I, expectedPath) {
    I.seeInCurrentUrl(expectedPath);
}

Scenario('redirect to slovensky when Accept-Language is sk', async ({ I }) => {
    await setAcceptLanguage(I, 'sk');
    I.amOnPage('/apps/language-redirect/');
    I.waitForText('Slovensky', 10);
    await seeRedirectedTo(I, '/apps/language-redirect/slovensky.html');
});

Scenario('redirect to english when Accept-Language is en', async ({ I }) => {
    await setAcceptLanguage(I, 'en');
    I.amOnPage('/apps/language-redirect/');
    I.waitForText('English', 10);
    await seeRedirectedTo(I, '/apps/language-redirect/english.html');
});

Scenario('redirect to slovensky when Accept-Language is sk-SK', async ({ I }) => {
    await setAcceptLanguage(I, 'sk-SK');
    I.amOnPage('/apps/language-redirect/');
    I.waitForText('Slovensky', 10);
    await seeRedirectedTo(I, '/apps/language-redirect/slovensky.html');
});

Scenario('redirect to english when Accept-Language is en-US', async ({ I }) => {
    await setAcceptLanguage(I, 'en-US');
    I.amOnPage('/apps/language-redirect/');
    I.waitForText('English', 10);
    await seeRedirectedTo(I, '/apps/language-redirect/english.html');
});

Scenario('redirect to default (slovensky) when Accept-Language is cs', async ({ I }) => {
    await setAcceptLanguage(I, 'cs');
    I.amOnPage('/apps/language-redirect/');
    I.waitForText('Slovensky', 10);
    await seeRedirectedTo(I, '/apps/language-redirect/slovensky.html');
});

Scenario('redirect to default (slovensky) when Accept-Language header is empty', async ({ I }) => {
    await setAcceptLanguage(I, '');
    I.amOnPage('/apps/language-redirect/');
    I.waitForText('Slovensky', 10);
    await seeRedirectedTo(I, '/apps/language-redirect/slovensky.html');
});

Scenario('redirect to slovensky with multiple languages sk-CS;q=0.9,en;q=0.7', async ({ I }) => {
    await setAcceptLanguage(I, 'sk-SK;q=0.9,en;q=0.7');
    I.amOnPage('/apps/language-redirect/');
    I.waitForText('Slovensky', 10);
    await seeRedirectedTo(I, '/apps/language-redirect/slovensky.html');
});

Scenario('redirect to english with multiple languages en;q=0.9,sk;q=0.7', async ({ I }) => {
    await setAcceptLanguage(I, 'en;q=0.9,sk;q=0.7');
    I.amOnPage('/apps/language-redirect/');
    I.waitForText('English', 10);
    await seeRedirectedTo(I, '/apps/language-redirect/english.html');
});

Scenario('cleanup', ({ I }) => {
    I.clearCookie('lng');
    I.setPlaywrightRequestHeaders({});
});