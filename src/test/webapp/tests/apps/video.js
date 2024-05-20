Feature('apps.video');

Scenario("Video - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/video/");
    I.waitForElement('iframe[src*="youtube.com"]');
    within({frame: "#videoPlaceholder1"}, () => {
        I.waitForElement(locate("a").withText('WebJET CMS - komunitná OpenSource verzia'), 10);
    });
});