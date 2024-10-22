Feature('apps.app-facebook_like_box');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});


Scenario('testovanie app  -  Like Box', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Facebook Like Box', '#components-app-facebook_like_box-title');
    const defaultParams = {
        dataHrefLikeBox: 'https://www.facebook.com/interway.sk/',
        widthLikeBox: '980',
        heightLikeBox: '',
        showFacesLikeBox: 'true',
        showPostLikeBox: 'true',
        device: '',
        cacheMinutes: ''
    };

    await Apps.assertParams(defaultParams);
    Apps.openAppEditor();

    const changedParams = {
        dataHrefLikeBox: 'https://www.facebook.com/apple/',
        widthLikeBox: '1020',
        heightLikeBox: '500',
        showFacesLikeBox: 'false',
        showPostLikeBox: 'false',
        device: '',
        cacheMinutes: ''
    };

    DTE.fillField('dataHrefLikeBox', changedParams.dataHrefLikeBox);
    DTE.fillField('widthLikeBox', changedParams.widthLikeBox);
    DTE.fillField('heightLikeBox', changedParams.heightLikeBox);
    DTE.clickSwitch('showFacesLikeBox_0');
    DTE.clickSwitch('showPostLikeBox_0');


    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);
});