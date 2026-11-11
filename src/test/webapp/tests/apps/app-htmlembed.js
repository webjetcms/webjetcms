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
    await within({ frame: 'iframe[src*="tiktok.com"]' }, async () => {
        // TikTok can show consent before its description; wait for either real player state.
        I.waitForVisible({ xpath: "//*[contains(text(), 'Starlink') or contains(text(), 'Allow cookies from TikTok') or contains(text(), 'Player error')]" }, 30);
        await I.clickIfVisible('Decline optional cookies');
        I.waitForText("Starlink", 30);
        I.dontSee('Player error');
    });
});
