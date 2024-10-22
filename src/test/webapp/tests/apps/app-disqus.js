Feature('apps.app-disqus');


Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app -  Disqus komentáre', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Disqus komentáre', '#components-app-disqus-title');

    const defaultParams = {
        login: ''
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see('comments powered by Disqus');

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        login: 'webjet8.disqus.com',
    };

    DTE.fillField('login', changedParams.login);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    const disqusError = I.grabNumberOfVisibleElements('#error');
    if (disqusError == 0)
        I.see('TESTER PLAYWRIGHT');
});

Scenario('testovanie app -  Disqus komentáre - closetabs', async ({ I, DTE, Apps }) => {
    I.closeOtherTabs();
});
