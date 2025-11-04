Feature('apps.video');

Scenario("Video - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/video/");
    I.waitForElement('iframe[src*="youtube.com"]');
    within({frame: "#videoPlaceholder1"}, () => {
        I.waitForElement(locate("a").withText('WebJET CMS - komunitná OpenSource verzia'), 10);
    });
});

Scenario("Video - test zobrazovania v bannery", ({ I }) => {
    I.amOnPage("/en/apps/banner-system/classic_video_banner_yt.html");
    I.waitForElement('iframe[src*="youtube.com"]');
    within({frame: "#video"}, () => {
        I.waitForElement(locate("div.ytp-ce-playlist-title").withText('WebJET produkty'), 10);
        I.dontSeeElement("div.ytp-error-content-wrap-subreason");
    });
});