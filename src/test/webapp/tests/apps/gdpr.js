Feature('apps.gdpr');


Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - gdpr', async ({ I, DTE, Apps }) => {
    Apps.insertApp('GDPR Cookies súhlas', '#components-gdpr-title');
    
    const defaultParams = {
        showLink : 'false'
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
        showLink : 'true'
    };

    DTE.clickSwitch('showLink_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')
    
    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    
     I.see('Nastavenie cookies');
});
