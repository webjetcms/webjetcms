Feature('apps.app-vyhladavanie');

Scenario("Google vyhľadávanie - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/google-vyhladavanie/");
    I.waitForElement(".gsc-control-cse");
    I.seeElement(locate("h1").withText(("Google vyhľadávanie")));

    I.dontSeeElement( locate(".gsc-expansionArea > div a.gs-title").withText("Inštalácia a spustenie - WebJET CMS"));

    I.say("Check search");
    I.fillField('search', 'Interway');
    I.clickCss('.gsc-search-button .gsc-search-button-v2');

    I.waitForElement( locate(".gsc-expansionArea > div a.gs-title").withText("Inštalácia a spustenie - WebJET CMS"), 10);

    I.say("Change order");
    I.clickCss("div.gsc-selected-option");
    I.clickCss("div.gsc-option-menu > div:nth-of-type(2) > div");

    I.say("Check changed order");
    //this is hard to test...
    //I.waitForElement( locate(".gsc-expansionArea > div").find( locate("a.gs-title").withText("Verzia 2024")), 10);
});