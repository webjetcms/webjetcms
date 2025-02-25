Feature('banner');

Before(({ I, login }) => {
    login('admin');
});

Scenario('banner admin', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/banner/admin/");
    DT.waitForLoader();

    Document.screenshot("/redactor/apps/banner/datatable.png");

    DT.filterContains("name", "WebJET");

    I.click("WebJET CMS");
    DTE.waitForEditor("bannerDataTable");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/banner/editor.png");

    I.clickCss("#pills-dt-bannerDataTable-restrictions-tab");
    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/apps/banner/editor-restrictions.png");
});

Scenario('banner editor', ({ I, DTE, Document }) => {
    const confLng = I.getConfLng();
    switch (confLng) {
        case 'sk':
        case 'cs':
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=21343");
            break;
        case 'en':
            I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=81749");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.click("iframe.wj_component");
    I.wait(5);

    Document.screenshot("/redactor/apps/banner/editor-dialog.png");
    I.switchTo();

    switch (confLng) {
        case 'sk':
        case 'cs':
            I.amOnPage("/apps/bannerovy-system/");
            break;
        case 'en':
            I.amOnPage("/apps/bannerovy-system/banner-system.html");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    I.wait(5);
    Document.screenshotElement("div.banner-image", "/redactor/apps/banner/banner-image.png");
    Document.screenshotElement("div.banner-html", "/redactor/apps/banner/banner-html.png");
    Document.screenshotElement("div.banner-content", "/redactor/apps/banner/banner-content.png");
});

Scenario('Video banner', ({ I, Document, Browser }) => {
   //I.assertEqual(Browser.isFirefox(), true, "There is a problem to play video in chromium, please run the test in Firefox");
    //TODO - problem with video, but manually it works
    I.amOnPage("/apps/bannerovy-system/klasicky_video_banner_yt.html");
    //I.seeElement("div.embed-responsive > iframe.video");
    Document.screenshot("/redactor/apps/banner/banner-video.png", 1000, 800);
});

Scenario('Device type specific banner screens', ({ I, DTE, Document }) => {
    //
    I.amOnPage("/test-stavov/device_specific_baner/multi_devices_banner.html?forceBrowserDetector=pc");
    Document.screenshot("/redactor/apps/banner/pc-only-banner.png");

    //
    I.amOnPage("/test-stavov/device_specific_baner/multi_devices_banner.html?forceBrowserDetector=phone");
    Document.screenshot("/redactor/apps/banner/phone-tablet-only-banner.png");


    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=73932");
    DTE.waitForEditor();

    I.wait(4);
    Document.screenshot("/redactor/apps/banner/multiple-devieces-banner-edit.png");

        //Open banner setting
        I.waitForElement(".cke_wysiwyg_frame.cke_reset", 30);
        I.switchTo(".cke_wysiwyg_frame.cke_reset");
        I.wait(1);
        I.waitForElement("iframe.wj_component", 10);
        I.switchTo(locate("iframe.wj_component").first());
        I.wait(1);
            I.forceClickCss(".inlineComponentButtons > .inlineComponentButton.cke_button");
        I.switchTo();

        I.waitForElement("table.cke_dialog");

        I.switchTo("#cke_121_iframe");
        I.switchTo("#editorComponent");
        I.waitForElement("#tabLinkcommonAdvancedSettings");
        I.clickCss("#tabLinkcommonAdvancedSettings");

    Document.screenshot("/redactor/apps/banner/banner-device-setting-tab.png");
});