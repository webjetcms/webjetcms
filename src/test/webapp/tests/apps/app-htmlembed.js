Feature('apps.app-htmlembed');

Scenario("Vloženie HTML kódu - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/vlozenie-html-kodu/");
    I.waitForText("X/TWITTER", 10);
    I.waitForText("TIKTOK", 10);
    I.waitForElement('iframe[title="X Post"]', 10);
    within({ frame: 'iframe[title="X Post"]' }, () => {
        I.waitForText("Falcon 9", 15);
    });

    I.waitForElement('iframe[src*="tiktok.com"]', 10);
    within({ frame: 'iframe[src*="tiktok.com"]' }, () => {
        I.waitForText("Starlink", 15);
    });
});