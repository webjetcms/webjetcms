Feature('apps.app-htmlembed');

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
        I.waitForText("Starlink", 30);
        I.dontSee('Player error');
    });
});
