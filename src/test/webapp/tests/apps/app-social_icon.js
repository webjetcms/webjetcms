Feature('apps.app-social_icon');

Before(({ I }) => {
    I.amOnPage("/apps/odkazy-socialne-siete/");
    I.closeOtherTabs();
    I.waitForElement(locate("h1").withText("Odkazy na sociálne siete"), 10);
});

Scenario("Odkazy sociálne siete - facebook", async ({ I }) => {
    I.waitForElement({ css: 'img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]'}, 5);
    I.seeElement({ css: 'img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]'});

    I.say("Overenie odkazu na Facebook");
    I.clickCss('img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]');
    I.wait(1);
    I.switchToNextTab();
    I.wait(1);
    I.waitForText("Prihlásiť sa", 10);
    await I.clickIfVisible('[aria-label="Povoliť všetky cookies"]');
    I.wait(1);
    await I.clickIfVisible('[aria-label="Zavrieť"]');
    I.wait(2);
    I.waitForText('WebJET CMS', 10);
    I.closeOtherTabs();
});

Scenario("Odkazy sociálne siete - youtube", async ({ I }) => {
    I.waitForElement({ css: 'img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]'}, 5);
    I.seeElement({ css: 'img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]'});

    I.say("Overenie odkazu na Youtube");
    I.clickCss('img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]');
    I.wait(1);
    I.switchToNextTab();
    I.wait(1);
    I.waitForText("Prihlásiť sa", 10);
     await I.clickIfVisible('[aria-label="Prijať všetko"]');
    I.waitForElement(locate('h1 > span').withText("WebJET od InterWay"), 30);
    I.closeOtherTabs();
});

Scenario("cleanup", ({ I }) => {
    I.closeOtherTabs();
});