Feature('apps.slider');

Before(({ I, login, DT }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
    DT.addContext("sliderItems","#sliderItemsDataTable_wrapper");
});

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



Scenario('testovanie app - Slider', async ({ I, DT, DTE, Apps, Document }) => {
    Apps.insertApp('Slider', '#components-slider-title', null, false);

    const defaultParams = {
        skin: 'Classic',
        fullWidthSlider: 'true',
        sliderWidth: '900',
        sliderHeight: '360',
        arrowStyle: 'mouseover',
        kenBurnsOnSlide: 'false',
        navStyle: 'bullets',
        showThumbnails: 'true',
        touchSwipe: 'true',
        randomPlay: 'false',
        autoplay: 'true',
        displayMode: '1',
        loopNumber: '0',
        autoplayInterval: '5000',
        showCountdown: 'true',
        autoplayCountdownColor: '#ffffff',
        countdownPosition: 'bottom',
        transitionOnFirstSlide: 'false',
        pauseOnMouseover: 'false',
        showNumbering: 'false',
        showShadowBottom: 'true',
        transition_fade: 'false',
        transition_cross_fade: 'false',
        transition_slide: 'false',
        transition_elastic: 'false',
        transition_slice: 'false',
        transition_blinds: 'false',
        transition_blocks: 'false',
        transition_shuffle: 'false',
        transition_tiles: 'false',
        transition_flip: 'false',
        transition_flip_with_zoom: 'false',
        transition_threed: 'false',
        transition_threed_horizontal: 'false',
        transition_threed_with_zoom: 'false',
        transition_threed_horizontal_with_zoom: 'false',
        transition_threed_flip: 'false',
        editorData: 'JTVCJTdCJTIyaWQlMjI6MiwlMjJpbWFnZSUyMjolMjIvaW1hZ2VzL2dhbGxlcnkvY2hyeXNhbnRoZW11bS5qcGclMjIsJTIydGl0bGUlMjI6JTIyQ2hyeXphbnRlbWElMjIsJTIyZGVzY3JpcHRpb24lMjI6JTIyQWolMjB2JTIwY2hsYWRlJTIwa3ZpdG5lbSUyMiwlMjJyZWRpcmVjdFVybCUyMjolMjJodHRwczovL3d3dy5pbnRlcndheS5zay8lMjIlN0QlNUQ='
    };

    I.switchTo('#cke_121_iframe');
    I.switchTo('#editorComponent');
    I.clickCss("#pills-dt-component-datatable-files-tab");
    I.switchTo("#DTE_Field_iframe");
    I.click(DT.btn.sliderItems_add_button);
    DTE.fillField("image", "/images/gallery/chrysanthemum.jpg");
    DTE.fillField("title", "Chryzantema");
    DTE.fillField("description", "Aj v chlade kvitnem");
    DTE.fillField("redirectUrl", "https://www.interway.sk/");
    I.click("Pridať");
    I.switchTo();
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    await Apps.assertParams(defaultParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForElement("#amazingslider-1 div.amazingslider-img-box-1 img", 10);
    I.moveCursorTo(".amazingslider-swipe-box-1");
    I.waitForElement(locate(".amazingslider-title-1").withText("Chryzantema"));
    I.waitForElement(locate(".amazingslider-description-1").withText("Aj v chlade kvitnem"));
    I.clickCss(".amazingslider-swipe-box-1");
    I.seeInCurrentUrl('www.interway.sk');

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
    skin: 'Cube',
    fullWidthSlider: 'true',
    sliderWidth: '800',
    sliderHeight: '320',
    arrowStyle: 'always',
    kenBurnsOnSlide: 'true',
    navStyle: 'numbering',
    showThumbnails: 'false',
    touchSwipe: 'false',
    randomPlay: 'true',
    autoplay: 'false',
    displayMode: '2',
    loopNumber: '20',
    autoplayInterval: '3000',
    showCountdown: 'false',
    autoplayCountdownColor: '#EEFFFF',
    countdownPosition: 'top',
    transitionOnFirstSlide: 'true',
    pauseOnMouseover: 'true',
    showNumbering: 'true',
    showShadowBottom: 'false',
    transition_fade: 'false',
    transition_cross_fade: 'false',
    transition_slide: 'false',
    transition_elastic: 'false',
    transition_slice: 'false',
    transition_blinds: 'false',
    transition_blocks: 'false',
    transition_shuffle: 'false',
    transition_tiles: 'false',
    transition_flip: 'false',
    transition_flip_with_zoom: 'false',
    transition_threed: 'false',
    transition_threed_horizontal: 'false',
    transition_threed_with_zoom: 'false',
    transition_threed_horizontal_with_zoom: 'false',
    transition_threed_flip: 'false',
    editorData: 'JTVCJTdCJTIyaWQlMjI6MiwlMjJpbWFnZSUyMjolMjIvaW1hZ2VzL2dhbGxlcnkvY2hyeXNhbnRoZW11bS5qcGclMjIsJTIydGl0bGUlMjI6JTIyQ2hyeXphbnRlbWElMjIsJTIyZGVzY3JpcHRpb24lMjI6JTIyQWolMjB2JTIwY2hsYWRlJTIwa3ZpdG5lbSUyMiwlMjJyZWRpcmVjdFVybCUyMjolMjJodHRwczovL3d3dy5pbnRlcndheS5zay8lMjIlN0QlNUQ='
    };

    DTE.selectOption("skin", "Cube");
    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    DTE.fillField("sliderWidth", "800");
    DTE.fillField("sliderHeight", "320");
    DTE.selectOption("arrowStyle", "Vždy");
    I.checkOption("#DTE_Field_kenBurnsOnSlide_0");
    DTE.selectOption("navStyle", "Čísla");
    I.uncheckOption("#DTE_Field_showThumbnails_0");
    I.uncheckOption("#DTE_Field_touchSwipe_0");
    I.checkOption("#DTE_Field_randomPlay_0");
    I.uncheckOption("#DTE_Field_autoplay_0");
    DTE.selectOption("displayMode", "Skončiť po");
    DTE.fillField("loopNumber", "20");
    DTE.fillField("autoplayInterval", 3000);
    I.uncheckOption("#DTE_Field_showCountdown_0");
    DTE.fillField("autoplayCountdownColor", "#EEFFFF");
    DTE.selectOption("countdownPosition", "Hore");
    I.checkOption("#DTE_Field_transitionOnFirstSlide_0");
    I.checkOption("#DTE_Field_pauseOnMouseover_0");
    I.checkOption("#DTE_Field_showNumbering_0");
    I.uncheckOption("#DTE_Field_showShadowBottom_0");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')


    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForElement("#amazingslider-1 div.amazingslider-img-box-1 img", 10);
    I.seeElement(locate(".amazingslider-title-1").withText("Chryzantema"));
    I.seeElement(locate(".amazingslider-description-1").withText("Aj v chlade kvitnem"));
    I.seeElement(".amazingslider-bullet-1");
});