Feature('apps.slider');

Before(({ I, login, DT }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
    DT.addContext("sliderItems","#datatableFieldDTE_Field_editorData_wrapper");
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
        arrow_style: 'mouseover',
        ken_burns_on_slide: 'false',
        nav_style: 'bullets',
        show_thumbnails: 'true',
        touch_swipe: 'true',
        random_play: 'false',
        autoplay: 'true',
        display_mode: '1',
        loop_number: '0',
        autoplay_interval: '5000',
        show_countdown: 'true',
        autoplay_countdown_color: '#ffffff',
        countdown_position: 'bottom',
        transition_on_first_slide: 'false',
        pause_on_mousover: 'false',
        show_numbering: 'false',
        show_shadow_bottom: 'true',
        transitions_all: '',
        editorData: 'JTVCJTdCJTIyaW1hZ2UlMjI6JTIyL2ltYWdlcy9nYWxsZXJ5L2NocnlzYW50aGVtdW0uanBnJTIyLCUyMnRpdGxlJTIyOiUyMkNocnl6YW50ZW1hJTIyLCUyMmRlc2NyaXB0aW9uJTIyOiUyMkFqJTIwdiUyMGNobGFkZSUyMGt2aXRuZW0lMjIsJTIycmVkaXJlY3RVcmwlMjI6JTIyaHR0cHM6Ly93d3cuaW50ZXJ3YXkuc2svJTIyJTdEJTVE'
    };

    I.switchTo('#cke_121_iframe');
    I.switchTo('#editorComponent');

    I.say('Check tabs');
        I.seeElement("#pills-dt-component-datatable-basic-tab");
        I.seeElement("#pills-dt-component-datatable-advanced-tab");
        I.seeElement("#pills-dt-component-datatable-transitions-tab");
        I.seeElement("#pills-dt-component-datatable-files-tab");
        I.seeElement("#pills-dt-component-datatable-commonSettings-tab");

    I.say("Set items in slider");
        I.clickCss("#pills-dt-component-datatable-files-tab");
        I.waitForVisible("#datatableFieldDTE_Field_editorData_wrapper", 5);

        I.say("Add item");
        I.click(DT.btn.sliderItems_add_button);
        DTE.waitForEditor("datatableFieldDTE_Field_editorData");

        // Set image
        I.fillField(locate(".DTE_Field_Name_image").find("input"), "/images/gallery/chrysanthemum.jpg");

        // Item title and description
        DTE.fillField("title", "Chryzantema");
        DTE.fillField("description", "Aj v chlade kvitnem");

        //Set redirect
        I.fillField(locate(".DTE_Field_Name_redirectUrl").find("input"), "https://www.interway.sk/");

        // Save new item
        DTE.save('datatableFieldDTE_Field_editorData');

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
        arrow_style: 'always',
        ken_burns_on_slide: 'true',
        nav_style: 'numbering',
        show_thumbnails: 'false',
        touch_swipe: 'false',
        random_play: 'true',
        autoplay: 'false',
        display_mode: '2',
        loop_number: '20',
        autoplay_interval: '3000',
        show_countdown: 'false',
        autoplay_countdown_color: '#EEFFFF',
        countdown_position: 'top',
        transition_on_first_slide: 'true',
        pause_on_mousover: 'true',
        show_numbering: 'true',
        show_shadow_bottom: 'false',
        transitions_all: 'tiles',
        editorData: 'JTVCJTdCJTIyaW1hZ2UlMjI6JTIyL2ltYWdlcy9nYWxsZXJ5L2NocnlzYW50aGVtdW0uanBnJTIyLCUyMnRpdGxlJTIyOiUyMkNocnl6YW50ZW1hJTIyLCUyMmRlc2NyaXB0aW9uJTIyOiUyMkFqJTIwdiUyMGNobGFkZSUyMGt2aXRuZW0lMjIsJTIycmVkaXJlY3RVcmwlMjI6JTIyaHR0cHM6Ly93d3cuaW50ZXJ3YXkuc2svJTIyJTdEJTVE'
    };

    I.say("Set changed parameters");
        DTE.selectOption("skin", "Cube");

        I.clickCss("#pills-dt-component-datatable-advanced-tab");
        DTE.fillField("sliderWidth", "800");
        DTE.fillField("sliderHeight", "320");
        DTE.selectOption("arrow_style", "Vždy");
        I.checkOption("#DTE_Field_ken_burns_on_slide_0");
        DTE.selectOption("nav_style", "Čísla");
        I.uncheckOption("#DTE_Field_show_thumbnails_0");
        I.uncheckOption("#DTE_Field_touch_swipe_0");
        I.checkOption("#DTE_Field_random_play_0");
        I.uncheckOption("#DTE_Field_autoplay_0");
        DTE.selectOption("display_mode", "Skončiť po");
        DTE.fillField("loop_number", "20");
        DTE.fillField("autoplay_interval", 3000);
        I.uncheckOption("#DTE_Field_show_countdown_0");
        DTE.fillField("autoplay_countdown_color", "#EEFFFF");
        DTE.selectOption("countdown_position", "Hore");
        I.checkOption("#DTE_Field_transition_on_first_slide_0");
        I.checkOption("#DTE_Field_pause_on_mousover_0");
        I.checkOption("#DTE_Field_show_numbering_0");
        I.uncheckOption("#DTE_Field_show_shadow_bottom_0");

        I.say("Change transition style");
        I.clickCss("#pills-dt-component-datatable-transitions-tab");
        DTE.selectOption("transitions_all", "Tiles");

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