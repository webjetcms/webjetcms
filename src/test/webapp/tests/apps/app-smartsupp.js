Feature('apps.app-smartsupp');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Live chat', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Live chat', '#components-app-smartsupp-title');
    
    const defaultParams = {
        kluc : ''
    };

    await Apps.assertParams(defaultParams);
    
    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.dontSeeElement({ css: '#widgetButtonFrame' });

    I.switchToPreviousTab();
    I.closeOtherTabs();
    
    Apps.openAppEditor();

    const changedParams = {
        kluc : '148a0ed3ea0c817b1c895f032fc9a1bfb9365cae'
    };

    DTE.fillField('kluc', changedParams.kluc);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')
    
    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    
    I.waitForVisible({ css: '#widgetButtonFrame' });
    I.seeElement({ css: '#widgetButtonFrame' });
    I.switchTo({ css: '#widgetButtonFrame' });
    I.see('Chat', { css: '.flex-center.whitespace-nowrap.pl-4.pr-1[data-testid="widgetButtontext"]' });
});