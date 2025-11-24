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

Scenario('testovanie app - Video', async ({ I, Apps, Document, DTE }) => {
    Apps.insertApp('Video', '#components-video-title', null, false);

    const defaultParams = {
        "field": "logo_youtube_color",
        "file": "https://www.youtube.com/watch?v=e-K-6Z_m-hg&amp;ab_channel=WebJETodInterWay",
        "widthType": "responsive",
        "align": "left",
        "width": "425",
        "height": "355",
        "percentageWidth": "100",
        "autoplay": "0",
        "showinfo": "0",
        "byline": "0",
        "branding": "0",
        "fullscreen": "1",
        "controls": "1",
        "rel": "1",
        "portrait": "1",
        "badge": "1"
    }

    I.switchTo('iframe[src$="webjetcomponet.jsp"]');
    I.switchTo('#editorComponent');

    I.say("Check images");
        I.seeElement("img[src='/components/video/admin-styles/logo_youtube_color.png']");
        I.seeElement("img[src='/components/video/admin-styles/logo_vimeo_color.png']");
        I.seeElement("img[src='/components/video/admin-styles/logo_facebook_color.png']");
        I.seeElement("img[src='/components/video/admin-styles/logo_video_color.png']");

    I.say("Check that for video we have elfinder select");
        I.clickCss(".image_radio_item > label[for=DTE_Field_field_0]");
        I.seeElement("#DTE_Field_file");
        I.dontSeeElement(".DTE_Field_Name_videoFile");
        I.clickCss(".image_radio_item > label[for=DTE_Field_field_1]");
        I.seeElement("#DTE_Field_file");
        I.dontSeeElement(".DTE_Field_Name_videoFile");
        I.clickCss(".image_radio_item > label[for=DTE_Field_field_2]");
        I.seeElement("#DTE_Field_file");
        I.dontSeeElement(".DTE_Field_Name_videoFile");
        I.clickCss(".image_radio_item > label[for=DTE_Field_field_3]");
        I.dontSeeElement("#DTE_Field_file");
        I.seeElement(".DTE_Field_Name_videoFile");


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
        "width": "425",
        "height": "355",
        "align": "center",
        "percentageWidth": "100",
        "autoplay": "1",
        "showinfo": "1",
        "byline": "1",
        "branding": "0",
        "fullscreen": "0",
        "controls": "1",
        "rel": "1",
        "portrait": "1",
        "badge": "1"
    };

    I.clickCss(".image_radio_item > label[for=DTE_Field_field_2]");
    DTE.fillField("file", changedParams.file);
    I.checkOption("#DTE_Field_widthType_1");
    I.checkOption("#DTE_Field_align_1");
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

Scenario("Video - test zobrazovania v bannery", ({ I }) => {
    I.amOnPage("/en/apps/banner-system/classic_video_banner_yt.html");
    I.waitForElement('iframe[src*="youtube.com"]');
    within({frame: "#video"}, () => {
        I.waitForElement(locate("div.ytp-ce-playlist-title").withText('WebJET produkty'), 10);
        I.dontSeeElement("div.ytp-error-content-wrap-subreason");
    });
});