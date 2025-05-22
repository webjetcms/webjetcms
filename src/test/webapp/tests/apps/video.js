Feature('apps.video');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Video - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/video/");
    I.waitForElement('iframe[src*="youtube.com"]');
    within({frame: "#videoPlaceholder1"}, () => {
        I.waitForElement(locate("a").withText('WebJET CMS - komunitná OpenSource verzia'), 10);
    });
});

Scenario('testovanie app - Video @current', async ({ I, Apps, Document, DTE }) => {
    Apps.insertApp('Video', '#components-video-title', null, false);

    const defaultParams = {
        "field": "logo_youtube_color",
        "file": "https://www.youtube.com/watch?v=e-K-6Z_m-hg&amp;ab_channel=WebJETodInterWay",
        "widthType": "fixed",
        "videoAlignYT": "left",
        "videoAlignVimeo": "left",
        "videoAlignFacebook": "left",
        "width": "425",
        "height": "355",
        "percentageWidth": "100",
        "autoplay": "false",
        "showinfo": "false",
        "byline": "false",
        "branding": "false",
        "fullscreen": "true",
        "controls": "true",
        "rel": "true",
        "portrait": "true",
        "badge": "true"
    }

    I.switchTo('#cke_121_iframe');
    I.switchTo('#editorComponent');
    I.clickCss(".image_radio_item > label[for=DTE_Field_field_0]");
    DTE.fillField("file", defaultParams.file);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    I.seeElement(".videoBox.videoBox1");

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        "field": "logo_facebook_color",
        "file": "https://www.facebook.com/share/v/1AKvx8oJoW/",
        "widthType": "responsive",
        "videoAlignYT": "left",
        "videoAlignVimeo": "left",
        "videoAlignFacebook": "center",
        "width": "425",
        "height": "355",
        "percentageWidth": "100",
        "autoplay": "true",
        "showinfo": "true",
        "byline": "true",
        "branding": "false",
        "fullscreen": "false",
        "controls": "true",
        "rel": "true",
        "portrait": "true",
        "badge": "true"
    };

    I.clickCss(".image_radio_item > label[for=DTE_Field_field_2]");
    DTE.fillField("file", changedParams.file);
    I.checkOption("#DTE_Field_widthType_1");
    I.checkOption("#DTE_Field_videoAlignFacebook_1");
    I.checkOption("#DTE_Field_autoplay_0");
    I.checkOption("#DTE_Field_byline_0");
    I.checkOption("#DTE_Field_showinfo_0");
    I.uncheckOption("#DTE_Field_fullscreen_0");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    I.seeElement(".videoBox.videoBox1");
});