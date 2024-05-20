Feature('apps.slider');

Scenario("Slider - test zobrazovania", ({ I }) => {

    var slider_image_A = locate('div.amazingslider-img-1').find({css: 'img[src="/thumb/images/gallery/test-vela-foto/dsc04077.jpeg?ip=5&w=900&h=360"]'});
    var slider_image_B = locate('div.amazingslider-img-1').find({css: 'img[src="/thumb/images/gallery/test-vela-foto/dsc04101.jpeg?ip=5&w=900&h=360"]'});

    var bullet_image_A = locate('div.amazingslider-bullet-image-1').find({css: 'img[src="/thumb/images/gallery/test-vela-foto/dsc04077.jpeg?ip=5&w=70&h=70"]'});
    var bullet_image_B = locate('div.amazingslider-bullet-image-1').find({css: 'img[src="/thumb/images/gallery/test-vela-foto/dsc04101.jpeg?ip=5&w=70&h=70"]'});

    I.amOnPage("/apps/slider/");

    I.say("Check slider rotating");
    I.waitForElement( slider_image_A );
    I.waitForElement( slider_image_B );
    I.waitForElement( slider_image_A );

    I.say("check rotating with arrows");
    I.waitForElement( slider_image_A );
    I.moveCursorTo('#amazingslider-wrapper-1');
    I.clickCss(".amazingslider-arrow-right-1");
    I.seeElement( slider_image_B );
    I.moveCursorTo('#amazingslider-wrapper-1');
    I.clickCss(".amazingslider-arrow-left-1");
    I.seeElement( slider_image_A );

    I.say("Check bullet images with text");
    I.seeElement( bullet_image_A );
    I.seeElement( locate(".amazingslider-bullet-text-1 > .amazingslider-nav-thumbnail-tite-1").withText("Pohľad z boku") );
    I.seeElement( bullet_image_B );
    I.seeElement( locate(".amazingslider-bullet-text-1 > .amazingslider-nav-thumbnail-tite-1").withText("Viac zadný pohľad") );
});