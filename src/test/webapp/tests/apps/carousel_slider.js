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

Scenario('testovanie aplikácie - Posobiva prezentacii', async ({ I, DT, DTE, Apps, Document }) => {
    Apps.insertApp('Carousel Slider', '#components-app-carousel_slider-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.say("Check tabs");
        I.seeElement("#pills-dt-component-datatable-basic-tab");
        I.seeElement("#pills-dt-component-datatable-advanced-tab");
        I.seeElement("#pills-dt-component-datatable-files-tab");
        I.seeElement("#pills-dt-component-datatable-commonSettings-tab");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    const defaultParams = {
        skin: "Classic",
        carouselWidth: "900",
        carouselHeight: "300",
        imageWidth: "300",
        imageHeight: "300",
        imgPerSlide: "4",
        direction: "horizontal",
        showLightbox: "true",
        rowNumber: "1",
        nav_style: "bullets",
        arrow_style: "always",
        touch_swipe: "true",
        random_play: "false",
        autoplay: "true",
        pause_on_mouse_over: "false",
        circular: "true",
        show_shadow_bottom: "false",
        display_mode: "1",
        loop_number: "0",
        autoplay_interval: "5000",
        editorData: "JTVCJTVE"
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    I.say("Set custom params");
        I.clickCss("#pills-dt-component-datatable-basic-tab");
        DTE.selectOption("skin", "Gallery");

        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        I.fillField("#DTE_Field_carouselWidth", 500);
        I.fillField("#DTE_Field_carouselHeight", 500);
        DTE.selectOption("imgPerSlide", "1");
        DTE.selectOption("display_mode", "Skončiť po");
        I.waitForVisible("#DTE_Field_loop_number");
        I.fillField("#DTE_Field_loop_number", 2);

        I.say("Test ITEMS inner table in tab");
            I.clickCss("#pills-dt-component-datatable-files-tab");
            I.waitForVisible("#DTE_Field_iframe", 5);
            I.switchTo("#DTE_Field_iframe");
            I.seeElement("#carouselSliderItemsDataTable_wrapper");

            I.say("Add item");
            I.clickCss("button.buttons-create");
            DTE.waitForEditor("carouselSliderItemsDataTable");

            // Set image
            I.fillField(locate(".DTE_Field_Name_image").find("input"), "/images/gallery/test-vela-foto/dsc04074.jpeg");

            // Item title and subtitle
            I.fillField("#DTE_Field_title", "Toto je nadpis obrazka");
            I.fillField("#DTE_Field_description", "Toto je podnadpis obrazka");

            // Save new item
            DTE.save('carouselSliderItemsDataTable');

            // Check item in DT
            DT.checkTableRow("carouselSliderItemsDataTable", 1, ["", "/images/gallery/test-vela-foto/dsc04074.jpeg", "Toto je nadpis obrazka", "Toto je podnadpis obrazka", ""]);


        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok');

    const changedParams = {
        skin: "Gallery",
        carouselWidth: "500",
        carouselHeight: "500",
        imageWidth: "300",
        imageHeight: "300",
        imgPerSlide: "1",
        direction: "horizontal",
        showLightbox: "true",
        rowNumber: "1",
        nav_style: "bullets",
        arrow_style: "always",
        touch_swipe: "true",
        random_play: "false",
        autoplay: "true",
        pause_on_mouse_over: "false",
        circular: "true",
        show_shadow_bottom: "false",
        display_mode: "2",
        loop_number: "2",
        autoplay_interval: "5000",
        editorData: "JTVCJTdCJTIyaWQlMjI6MiwlMjJpbWFnZSUyMjolMjIvaW1hZ2VzL2dhbGxlcnkvdGVzdC12ZWxhLWZvdG8vZHNjMDQwNzQuanBlZyUyMiwlMjJ0aXRsZSUyMjolMjJUb3RvJTIwamUlMjBuYWRwaXMlMjBvYnJhemthJTIyLCUyMmRlc2NyaXB0aW9uJTIyOiUyMlRvdG8lMjBqZSUyMHBvZG5hZHBpcyUyMG9icmF6a2ElMjIsJTIycmVkaXJlY3RVcmwlMjI6JTIyJTIyJTdEJTVE"
    };

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.say("Check app");
    I.waitForVisible(".amazingcarousel-item-container", 5);
    I.seeElement(locate(".amazingcarousel-image").withChild("a[href='/images/gallery/test-vela-foto/dsc04074.jpeg']"));
    I.seeElement(locate(".amazingcarousel-title").withText("Toto je nadpis obrazka"));
    I.seeElement(locate(".amazingcarousel-description").withText("Toto je podnadpis obrazka"));
});

function seeLightboxImage(I, expectedImage) {
    I.seeElement( locate("div#html5-image > div#html5-image-container").find({ css: 'img[src="' + expectedImage + '"]'}) );
}

async function checkCarouselBackgroundImage(I, position, expectedImage) {
    const backgroundImage = await I.grabAttributeFrom(`div.amazingcarousel-list-container > div.amazingcarousel-list-wrapper > ul > li:nth-child(${position}) > div > div.amazingcarousel-image > a`, 'href');
    I.assertContain(backgroundImage, expectedImage);
}