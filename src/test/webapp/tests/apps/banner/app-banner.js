Feature('apps.app-banner');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Banner', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Bannerový systém', '#components-banner-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.waitForElement('#editorComponent', 10);
    I.switchTo('#editorComponent');
    DTE.selectOption('group', 'banner-html');
    DTE.fillField('bannerIndex', '5');

    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    const defaultParams = {
        group: 'banner-html',
        status: 'enabled',
        displayMode: '1',
        refreshRate: '0',
        bannerIndex: '5',
        showInIframe: 'false',
        iframeWidth: '',
        iframeHeight: '',
        videoWrapperClass: '',
        jumbotronVideoClass: ''
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see('Toto je HTML banner');

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        group: 'banner-video-yt',
        status: 'enabled',
        displayMode: '3',
        refreshRate: '0',
        bannerIndex: '5',
        showInIframe: 'true',
        iframeWidth: '300',
        iframeHeight: '150',
        videoWrapperClass: 'embed-responsive embed-responsive-1by1 ratio ratio-1x1 banner-has-video',
        jumbotronVideoClass: 'jumbotron-has-video-fullscreen'
    };
    DTE.selectOption('group', changedParams.group);
    DTE.clickSwitch('displayMode_2');
    Apps.switchToTabByIndex(1);
    DTE.clickSwitch('showInIframe_0');
    DTE.fillField('refreshRate', changedParams.refreshRate);
    DTE.fillField('iframeWidth', changedParams.iframeWidth);
    DTE.fillField('iframeHeight', changedParams.iframeHeight);
    DTE.fillField('videoWrapperClass', changedParams.videoWrapperClass);
    DTE.fillField('jumbotronVideoClass', changedParams.jumbotronVideoClass);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.waitForElement('.bannerIframe',10);
    I.assertEqual(await I.grabCssPropertyFrom('.bannerIframe', 'height'), changedParams.iframeHeight + 'px');
    I.assertEqual(await I.grabCssPropertyFrom('.bannerIframe', 'width'), changedParams.iframeWidth + 'px');
    I.switchTo('.bannerIframe');
    I.seeElement('.embed-responsive-1by1');
});