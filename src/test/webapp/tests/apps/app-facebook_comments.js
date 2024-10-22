Feature('apps.app-facebook_comments');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});



Scenario('testovanie app - Facebook komentáre', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Facebook komentáre', '#components-app-facebook_comments-title');
    const defaultParams = {
        numberComments: '5',
        widthComments: '980',
        device: '',
        cacheMinutes: ''
    };

    await Apps.assertParams(defaultParams);
    Apps.openAppEditor();

    const changedParams = {
        numberComments: '2',
        widthComments: '1020',
        device: '',
        cacheMinutes: ''
    };

    DTE.fillField('numberComments', changedParams.numberComments);
    DTE.fillField('widthComments', changedParams.widthComments);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);
});