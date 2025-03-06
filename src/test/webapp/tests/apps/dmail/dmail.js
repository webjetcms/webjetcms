Feature('apps.dmail');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Hromadný e-mail' , async ({ I, DTE, Apps }) => {
    Apps.insertApp('Hromadný e-mail', '#components-dmail-title' );

    const defaultParams = {
        typeId : 'subscribe',
        senderName : 'Tester Playwright',
        senderEmail : 'tester@balat.sk',
        emailBodyId : '-1'
    };
    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.waitForElement('#subscribeForm');
    I.seeInField('#meno', 'Tester');
    I.seeInField('#priez', 'Playwright');
    I.seeInField('#mail', defaultParams.senderEmail);

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        typeId : 'subscribe',
        senderName : 'User Codecept',
        senderEmail : 'user@codecept.sk',
        emailBodyId : '141'
    };

    DTE.fillField('senderName', changedParams.senderName);
    DTE.fillField('senderEmail', changedParams.senderEmail);
    I.clickCss('#editorAppDTE_Field_emailBodyId  .input-group > button');
    I.click(locate('.jstree-node').withChild(locate('.jstree-anchor').withText('Jet portal 4')).find(locate('.jstree-icon')));
    I.click(locate('.jstree-anchor').withText('Jet portal 4 - testovacia stranka'));

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);
});