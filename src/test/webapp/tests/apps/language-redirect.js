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

Scenario('appstore params test', async ({ I, DTE, Apps, login }) => {
    login('admin');
    Apps.insertApp('Presmerovanie podľa jazyka prehliadača', '#apps-app-language-redirect-title', null, false);

    const defaultParams = {
        defaultLanguage: 'sk',
        rootOnly: 'false',
        respectCookie: 'true',
        mapping1Lang: '',
        mapping1Url: '',
        mapping2Lang: '',
        mapping2Url: '',
        mapping3Lang: '',
        mapping3Url: '',
        mapping4Lang: '',
        mapping4Url: '',
        mapping5Lang: '',
        mapping5Url: '',
        mapping6Lang: '',
        mapping6Url: '',
        mapping7Lang: '',
        mapping7Url: '',
        mapping8Lang: '',
        mapping8Url: ''
    };

    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    await Apps.assertParams(defaultParams);

    Apps.openAppEditor();

    const changedParams = {
        defaultLanguage: 'sk',
        rootOnly: 'true',
        respectCookie: 'false',
        mapping1Lang: 'sk',
        mapping1Url: '/apps/language-redirect/slovensky.html',
        mapping2Lang: '',
        mapping2Url: '/apps/language-redirect/english.html',
        mapping3Lang: 'en',
        mapping3Url: '',
        mapping4Lang: '',
        mapping4Url: '',
        mapping5Lang: '',
        mapping5Url: '',
        mapping6Lang: '',
        mapping6Url: '',
        mapping7Lang: '',
        mapping7Url: '',
        mapping8Lang: '',
        mapping8Url: ''
    };

    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    DTE.clickSwitch('rootOnly_0');
    DTE.clickSwitch('respectCookie_0'); //uncheck checked by default
    I.clickCss("#pills-dt-component-datatable-basic-tab");
    DTE.selectOption("mapping1Lang", "Slovenský");
    DTE.selectOption("mapping3Lang", "Anglický");
    DTE.fillField('mapping1Url', changedParams.mapping1Url);
    DTE.fillField('mapping2Url', changedParams.mapping2Url);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    await Apps.assertParams(changedParams);
});

Scenario('cleanup', ({ I }) => {
    I.clearCookie('lng');
    I.setPlaywrightRequestHeaders({});
});
