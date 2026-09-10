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

Scenario("YouTube share URL preserves playback start time", async ({ I, Apps, DTE }) => {
    const videoUrl = "https://youtu.be/q8xs3qDq-G4?si=6uc7EwqSIvciV14s&t=115";

    Apps.insertApp('Video', '#components-video-title', null, false);
    I.switchTo('iframe[src$="webjetcomponet.jsp"]');
    I.switchTo('#editorComponent');
    I.clickCss(".image_radio_item > label[for=DTE_Field_field_0]");
    DTE.fillField("file", videoUrl);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.waitForInvisible('.cke_dialog');

    I.clickCss('#pills-dt-datatableInit-basic-tab');
    DTE.fillField("title", "autotest-video-start-" + randomNumber);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    await Apps.assertParams({ file: videoUrl.replace(/&/g, "&amp;") }, "/components/video/video_player.jsp");

    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    const iframe = '.videoBox iframe.embed-responsive-item';
    I.waitForVisible(iframe);
    const src = await I.grabAttributeFrom(iframe, "src");
    const embedUrl = new URL(src, await I.grabCurrentUrl());

    I.assertEqual(embedUrl.hostname, "www.youtube.com");
    I.assertEqual(embedUrl.pathname, "/embed/q8xs3qDq-G4");
    I.assertEqual(src.split("?").length, 2, "The embed URL must contain exactly one query separator");
    I.assertEqual(embedUrl.searchParams.get("start"), "115");
    I.assertEqual(embedUrl.searchParams.getAll("start").length, 1);
    I.assertFalse(embedUrl.searchParams.has("t"), "The share time must be converted to the embed start parameter");
    I.assertEqual(embedUrl.searchParams.get("si"), "6uc7EwqSIvciV14s");
    I.assertEqual(embedUrl.searchParams.get("enablejsapi"), "1");
    I.assertEqual(embedUrl.searchParams.get("autoplay"), "0");
    I.assertEqual(embedUrl.searchParams.get("controls"), "1");

    I.switchToPreviousTab();
    I.closeOtherTabs();
    DTE.cancel();
});

Scenario("Video - test zobrazovania v bannery", ({ I }) => {
    I.amOnPage("/en/apps/banner-system/classic_video_banner_yt.html");
    I.waitForElement('iframe[src*="youtube.com"]');
    within({frame: "#video"}, () => {
        I.waitForElement(locate("div.ytp-ce-playlist-title").withText('WebJET produkty'), 10);
        I.dontSeeElement("div.ytp-error-content-wrap-subreason");
    });
});

Scenario("Video - local", ({ I, DTE, Apps }) => {
    Apps.openAppEditor(162123)

    //click on app and verify link is correct
    I.seeInField(".DTE_Field_Name_videoFile input", "/images/video/bloky.mp4");
});

Scenario("Audio - local", ({ I, DTE, Apps }) => {
    Apps.openAppEditor(162124)

    //click on app and verify link is correct
    I.seeInField(".DTE_Field_Name_videoFile input", "/images/video/audio.mp3");
});
