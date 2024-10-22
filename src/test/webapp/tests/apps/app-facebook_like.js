Feature('apps.app-facebook_like');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app -  Facebook Like tlačidlo', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Facebook Like tlačidlo', '#components-app-facebook_like-title');
    const defaultParams = {
        dataHrefLike: 'lajkovat_cely_web',
        widthLike: '980',
        layoutLikeButton: 'standard',
        actionLikeButton: 'like',
        device: '',
        cacheMinutes: ''
    };

    await Apps.assertParams(defaultParams);
    Apps.openAppEditor();

    const changedParams = {
        dataHrefLike: 'lajkovat_aktualne',
        widthLike: '1020',
        layoutLikeButton: 'button_count',
        actionLikeButton: 'recommend',
        device: '',
        cacheMinutes: ''
    };

    DTE.clickSwitch('dataHrefLike_1');
    DTE.fillField('widthLike', changedParams.widthLike);
    DTE.clickSwitch('layoutLikeButton_1');
    DTE.clickSwitch('actionLikeButton_1');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);
});