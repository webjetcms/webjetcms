Feature('apps.social-icon');

Scenario("Odkazy sociálne siete - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/odkazy-socialne-siete/");
    I.waitForElement(locate("h1").withText("Odkazy na sociálne siete"), 10);
    I.seeElement({ css: 'img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]'});
    I.seeElement({ css: 'img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]'});

    I.say("Overenie odkazu na Facebook");
    I.clickCss('img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]');
    I.switchToNextTab();
    I.clickIfVisible('[aria-label="Povoliť všetky cookies"]');
    I.clickIfVisible('[aria-label="Zavrieť"]');
    I.see('WebJET CMS');
    I.switchToPreviousTab();
    I.closeOtherTabs();

    I.say("Overenie odkazu na Youtube");
    I.clickCss('img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]');
    I.switchToNextTab();
    I.clickIfVisible('[aria-label="Prijať všetko"]');
    I.see('WebJET od InterWay');
    I.switchToPreviousTab();
    I.closeOtherTabs();
});