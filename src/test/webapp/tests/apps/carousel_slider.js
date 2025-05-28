Feature('apps.carousel_slider');

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Carousel slider - test zobrazovania", async ({ I }) => {
    I.amOnPage("/apps/carousel-slider/");
    I.waitForElement("#amazingcarousel-container-1");
    I.seeElement(locate("h1").withText(("Carousel Slider")));

    I.say("Check loaded images in carousel");
    await checkCarouselBackgroundImage(I, 1, '/images/gallery/test-vela-foto/dsc04082.jpeg');
    await checkCarouselBackgroundImage(I, 2, '/images/gallery/test-vela-foto/dsc04068.jpeg');
    await checkCarouselBackgroundImage(I, 3, '/images/gallery/test-vela-foto/dsc04095.jpeg');
    await checkCarouselBackgroundImage(I, 4, '/images/gallery/test-vela-foto/dsc04121.jpeg');
    await checkCarouselBackgroundImage(I, 5, '/images/gallery/test-vela-foto/dsc04159.jpeg');
    await checkCarouselBackgroundImage(I, 6, '/images/gallery/test-vela-foto/dsc04082.jpeg');

    I.say("Check buttons in carousel");
    I.seeElement("div.amazingcarousel-list-container > div.amazingcarousel-prev");
    I.seeElement("div.amazingcarousel-list-container > div.amazingcarousel-next");

    I.say("Test carousel navigation");
    I.dontSeeElement('ul.amazingcarousel-list > li:nth-child(1) > div.amazingcarousel-item-container > div.amazingcarousel-image > div.amazingcarousel-hover-effect');
    I.clickCss("div.amazingcarousel-bullet-list > div.amazingcarousel-bullet-0");
    I.moveCursorTo('ul.amazingcarousel-list > li:nth-child(1)');
    I.seeElement('ul.amazingcarousel-list > li:nth-child(1) > div.amazingcarousel-item-container > div.amazingcarousel-image > div.amazingcarousel-hover-effect');

    I.say("Test carousel lightbox");
    I.clickCss('ul.amazingcarousel-list > li:nth-child(1) > div.amazingcarousel-item-container > div.amazingcarousel-image > div.amazingcarousel-hover-effect');
    I.waitForElement('#carousel-html5-lightbox');
    within("#html5-elem-box", () => {
        seeLightboxImage(I, '/images/gallery/test-vela-foto/dsc04082.jpeg');
        I.clickCss("div#html5-play");
        I.waitForVisible( locate("div#html5-image > div#html5-image-container").find({ css: 'img[src="/images/gallery/test-vela-foto/dsc04068.jpeg"]'}), 10 );
        I.clickCss("div#html5-pause");
        I.wait(10);
        seeLightboxImage(I, '/images/gallery/test-vela-foto/dsc04068.jpeg');
        I.clickCss("div#html5-next");
        seeLightboxImage(I, '/images/gallery/test-vela-foto/dsc04095.jpeg');
        I.clickCss("div#html5-prev");
        I.seeElement( locate("div#html5-image > div#html5-image-container").find({ css: 'img[src="/images/gallery/test-vela-foto/dsc04068.jpeg"]'}) );
        seeLightboxImage(I, '/images/gallery/test-vela-foto/dsc04068.jpeg');
    });
    I.clickCss("#html5-close");
    I.waitForInvisible("#carousel-html5-lightbox");
});

Scenario('testovanie aplikácie - Posobiva prezentacii', async ({ I, Apps, Document }) => {
    Apps.insertApp('Carousel Slider', '#components-app-carousel_slider-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    const defaultParams = {
        skin: "AutoScroller",
        carouselWidth: "",
        carouselHeight: "",
        imageWidth: "",
        imageHeight: "",
        imgPerSlide: "1",
        direction: "horizontal",
        showLightbox: "true",
        rowNumber: "",
        nav_style: "none",
        arrow_style: "none",
        touch_swipe: "true",
        random_play: "false",
        autoplay: "true",
        pause_on_mouse_over: "false",
        circular: "true",
        show_shadow_bottom: "false",
        autoplay_interval: ""
    };
    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();
    I.clickCss("#pills-dt-component-datatable-style-tab");

    const changedParams = {
        skin: "AutoScroller",
        carouselWidth: "",
        carouselHeight: "",
        imageWidth: "",
        imageHeight: "",
        imgPerSlide: "1",
        direction: "horizontal",
        showLightbox: "true",
        rowNumber: "",
        nav_style: "none",
        arrow_style: "none",
        touch_swipe: "true",
        random_play: "false",
        autoplay: "true",
        pause_on_mouse_over: "false",
        circular: "true",
        show_shadow_bottom: "false",
        autoplay_interval: ""
    };

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

});

function seeLightboxImage(I, expectedImage) {
    I.seeElement( locate("div#html5-image > div#html5-image-container").find({ css: 'img[src="' + expectedImage + '"]'}) );
}

async function checkCarouselBackgroundImage(I, position, expectedImage) {
    const backgroundImage = await I.grabAttributeFrom(`div.amazingcarousel-list-container > div.amazingcarousel-list-wrapper > ul > li:nth-child(${position}) > div > div.amazingcarousel-image > a`, 'href');
    I.assertContain(backgroundImage, expectedImage);
}