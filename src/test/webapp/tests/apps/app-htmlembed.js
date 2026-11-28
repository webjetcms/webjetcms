Feature('apps.app-htmlembed');

Scenario('Empty HTML embed renders no content', async ({ I, login, Apps, DTE, Document }) => {
    login('admin');
    Apps.insertApp('Vloženie HTML kódu', '#components-app-htmlembed-title');
    await Apps.assertParams({ html: '' }, '/components/app-htmlembed/embed.jsp');

    const include = await I.executeScript(() => {
        return window.ckEditorInstance.getData().match(/!INCLUDE\([\s\S]*?\)!/)[0];
    });
    I.assertContain(include, 'html=,');
    await DTE.fillCkeditor(
        `<div id="html-embed-autotest">${include}</div>` +
        '<div id="html-embed-missing-autotest">!INCLUDE(/components/app-htmlembed/embed.jsp)!</div>' +
        '<p>HTML embed autotest</p>'
    );

    I.clickCss('button.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText('HTML embed autotest', 20);
    I.seeTextEquals('', '#html-embed-autotest');
    I.seeTextEquals('', '#html-embed-missing-autotest');
    I.closeCurrentTab();
});

Scenario('HTML embed preserves Unicode content and can be cleared', async ({ I, login, Apps, DTE, Document }) => {
    login('admin');
    const text = 'HTML embed autotest: Ľščťžýáíé € 汉字';
    const html = `<p>${text}</p>`;
    Apps.insertApp('Vloženie HTML kódu', '#components-app-htmlembed-title');
    Apps.openAppEditor();
    DTE.fillField('html', html);
    Apps.confirm();

    const include = await I.executeScript(() => {
        return window.ckEditorInstance.getData().match(/!INCLUDE\([\s\S]*?\)!/)[0];
    });
    const legacyEncoded = Buffer.from('utf8:' + encodeURIComponent(html)).toString('base64');
    await DTE.fillCkeditor(
        `<div id="html-embed-autotest">${include}</div>` +
        `<div id="html-embed-legacy-autotest">!INCLUDE(/components/app-htmlembed/embed.jsp, html=${legacyEncoded})!</div>` +
        '<p>HTML embed autotest</p>'
    );

    I.clickCss('button.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText(text, 20, '#html-embed-autotest');
    I.seeTextEquals(text, '#html-embed-legacy-autotest');
    I.closeCurrentTab();

    Apps.openAppEditor();
    I.seeInField('#DTE_Field_html', html);
    DTE.fillField('html', '');
    Apps.confirm();
    const clearedInclude = await I.executeScript(() => {
        return window.ckEditorInstance.getData().match(/!INCLUDE\([\s\S]*?\)!/)[0];
    });
    I.assertContain(clearedInclude, 'html=,');

    I.clickCss('button.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText('HTML embed autotest', 20);
    I.seeTextEquals('', '#html-embed-autotest');
    I.closeCurrentTab();
});

Scenario("Vloženie HTML kódu - test zobrazovania", async ({ I }) => {
    I.amOnPage("/apps/vlozenie-html-kodu/");
    I.waitForText("X/TWITTER", 10);
    I.waitForText("TIKTOK", 10);
    I.waitForElement('iframe[title="X Post"]', 10);
    await within({ frame: 'iframe[title="X Post"]' }, () => {
        I.waitForText("Falcon 9", 15);
    });

    I.waitForElement('iframe[src*="tiktok.com"]', 10);
    I.scrollTo('iframe[src*="tiktok.com"]');
    await within({ frame: 'iframe[src*="tiktok.com"]' }, async () => {
        // TikTok renders consent buttons in an open shadow root, which XPath cannot enter.
        const cookieButton = 'button:has-text("Decline optional cookies")';
        I.waitForVisible(`${cookieButton}, a[aria-label="Open description on TikTok"]`, 30);
        if (await I.grabNumberOfVisibleElements(cookieButton)) {
            I.clickCss(cookieButton);
            I.waitForInvisible(cookieButton, 10);
        }
        // Report a real player failure directly instead of timing out on its description.
        const playerError = ':text("Player error")';
        I.waitForVisible(`a[aria-label="Open description on TikTok"], ${playerError}`, 30);
        I.dontSeeElement(playerError);
        I.waitForText("Starlink", 30);
        I.dontSeeElement(playerError);
    });
}).config('Playwright', { waitForNavigation: 'domcontentloaded' });
