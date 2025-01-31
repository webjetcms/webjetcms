Feature('apps.app-social_icon');

Before(({ I }) => {
    I.closeOtherTabs();
    I.amOnPage("/apps/odkazy-socialne-siete/");
    I.switchTo();
    I.waitForElement(locate("h1").withText("Odkazy na sociálne siete"), 10);
});

Scenario("Odkazy sociálne siete - facebook", async ({ I,Document }) => {
    I.waitForElement({ css: 'img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]'}, 10);
    I.seeElement({ css: 'img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]'});

    I.say("Overenie odkazu na Facebook");
    I.clickCss('img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText("Prihlásiť sa", 11);
    await I.clickIfVisible('[aria-label="Povoliť všetky cookies"]');
    I.wait(1);
    await I.clickIfVisible('[aria-label="Zavrieť"]');
    I.waitForText('WebJET CMS', 12);
    I.closeOtherTabs();
});

Scenario("Odkazy sociálne siete - youtube", async ({ I, Document }) => {
    I.waitForElement({ css: 'img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]'}, 10);
    I.seeElement({ css: 'img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]'});

    I.say("Overenie odkazu na Youtube");
    I.clickCss('img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText("Prihlásiť sa", 12);
    await I.clickIfVisible('[aria-label="Prijať všetko"]');
    I.waitForElement(locate('h1 > span').withText("WebJET od InterWay"), 30);
    I.closeOtherTabs();
});

Scenario("cleanup", ({ I }) => {
    I.closeOtherTabs();
});