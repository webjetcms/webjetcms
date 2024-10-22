Feature('apps.app-vyhladavanie');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Google vyhľadávanie - test zobrazovania", ({ I }) => {
    var searchText = "DB-HESLO";
    var resultText = "Zriadenie novej inštalácie - WebJET CMS";
    I.amOnPage("/apps/google-vyhladavanie/");
    I.waitForElement(".gsc-control-cse");
    I.seeElement(locate("h1").withText(("Google vyhľadávanie")));
    I.dontSee(resultText);

    I.dontSeeElement('.gs-snippet');

    I.say("Check search");
    I.fillField('search', searchText);
    I.clickCss('.gsc-search-button .gsc-search-button-v2');

    I.see(searchText, '.gs-snippet');
    I.waitForElement( locate(".gsc-expansionArea > div a.gs-title").withText(resultText), 10);

    I.say("Change order");
    I.clickCss("div.gsc-selected-option");
    I.clickCss("div.gsc-option-menu > div:nth-of-type(2) > div");

    I.say("Check changed order");
    //this is hard to test...
    //I.waitForElement( locate(".gsc-expansionArea > div").find( locate("a.gs-title").withText("Verzia 2024")), 10);
});

Scenario('testovanie app - app-vyhladavanie', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Google vyhľadávanie', '#components-app-vyhladavanie-title');
    
    const defaultParams = {
        customSearchId : ''
    };

    await Apps.assertParams(defaultParams);
    
    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.dontSee('Nastavenie cookies');

    I.switchToPreviousTab();
    I.closeOtherTabs();
    
    Apps.openAppEditor();

    const changedParams = {
        customSearchId :  'b26e7934939764ed1'
    };

    DTE.fillField('customSearchId', changedParams.customSearchId);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')
    
    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    
    I.waitForElement(".gsc-control-cse");
    I.seeElement('button.gsc-search-button');
});