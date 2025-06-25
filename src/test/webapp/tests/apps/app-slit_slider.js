Feature('apps.app-slit_slider');

Before(({ I, login, DT }) => {
    login('admin');
    DT.addContext("slitSliderItems","#slitSliderItemsDataTable_wrapper");
});

Scenario("Slit slider - test zobrazovania", async ({ I }) => {
    var picture_A_text = locate("blockquote p").withText("Toto je na fotené na streche");
    var picture_A_headline = locate("h2").withText("Na streche");

    var picture_B_text = locate("blockquote p").withText("Toto je fotené pod mostom");
    var picture_B_headline = locate("h2").withText("Pod mostom");

    I.amOnPage("/apps/slit-slider/");
    I.waitForElement(".sl-slider-wrapper");
    I.seeElement(locate("h1").withText("Slit slider"));

    I.say("overenie automatickej výmeny obrázkov")
    let backgroundImageStyle = await I.grabAttributeFrom('.sl-slide-inner .bg-img', 'style');
    I.assertContain(backgroundImageStyle, "background-image: url(/thumb/images/gallery/test-vela-foto/dsc04188.jpeg?w=1200&ip=1)", 'The background image is not visible');
    I.seeElement( picture_A_headline );
    I.seeElement( picture_A_text );

    I.waitForVisible( picture_B_headline );
    I.seeElement( picture_B_headline );
    I.seeElement( picture_B_text );

    I.waitForVisible( picture_A_headline );
    I.seeElement( picture_A_headline );
    I.seeElement( picture_A_text );

    I.say("overenie manuálneho zvolenia obrázku");
    I.clickCss("#nav-dots > span:nth-of-type(2)");
    I.seeElement( picture_B_headline );
    I.seeElement( picture_B_text );
    I.wait(2);
    I.dontSeeElement( picture_A_text );

    I.say("overenie zastavenia automatickej výmeny obrázkov po manuálnom zvolení obrázku");
    I.clickCss("#nav-dots > span:nth-of-type(1)");
    I.seeElement( picture_A_headline );
    I.seeElement( picture_A_text );

    for(let i = 0; i < 5; i++) {
        I.dontSeeElement( picture_B_text );
        I.wait(1);
    }
});

Scenario('Slit slider app - test INCLUDE building', async ({ I, DT, DTE, Apps, Document }) => {
    Apps.insertApp('Slit slider', '#components-app-slit_slider-title', null, false);

    I.switchTo('#cke_121_iframe');
    I.switchTo('#editorComponent');

    I.say('Check tabs');
        I.seeElement("#pills-dt-component-datatable-basic-tab");
        I.seeElement("#pills-dt-component-datatable-files-tab");
        I.seeElement("#pills-dt-component-datatable-commonSettings-tab");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const defaultParams = {
        nivoSliderHeight: "500",
        headAlign: "icon_align_left",
        headingSize: "70",
        headingMargin: "0",
        subHeadingAlign: "icon_align_left",
        subHeadingSize: "30",
        subHeadingMargin: "0",
        editorData: 'JTVCJTVE'
    };

    await Apps.assertParams(defaultParams, "/components/app-slit_slider/news.jsp");

    Apps.openAppEditor();

    I.say("Change params");
        I.fillField("#DTE_Field_nivoSliderHeight", 400);
        I.clickCss("#DTE_Field_nivoSliderHeight");
        I.fillField("#DTE_Field_headingMargin", 15);
        I.click( locate("label[for='DTE_Field_headAlign_1']") );

    I.say("Set items");
        I.clickCss("#pills-dt-component-datatable-files-tab");
        I.waitForVisible("#DTE_Field_iframe", 5);
        I.switchTo("#DTE_Field_iframe");

        I.click(DT.btn.slitSliderItems_add_button);
        DTE.waitForEditor("slitSliderItemsDataTable");

        I.fillField(".DTE_Field_Name_image input", "/images/gallery/test-vela-foto/dsc04068.jpeg");
        I.fillField("#DTE_Field_title", "Test nadpis obrazka");
        I.fillField("#DTE_Field_subtitle", "Test podnadpis obrazka");
        I.fillField("#DTE_Field_headingColor", "#e70d0dff");
        I.fillField("#DTE_Field_subheadingColor", "#19e62eff");

        DTE.save("slitSliderItemsDataTable");

        DT.checkTableRow("slitSliderItemsDataTable", 1, ["", "/images/gallery/test-vela-foto/dsc04068.jpeg", "Test nadpis obrazka", "Test podnadpis obrazka", "", "#e70d0dff", "#19e62eff", ""]);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const changedParams = {
        nivoSliderHeight: "400",
        headAlign: "icon_align_center",
        headingSize: "70",
        headingMargin: "15",
        subHeadingAlign: "icon_align_left",
        subHeadingSize: "30",
        subHeadingMargin: "0",
        editorData: "JTVCJTdCJTIyaWQlMjI6MiwlMjJpbWFnZSUyMjolMjIvaW1hZ2VzL2dhbGxlcnkvdGVzdC12ZWxhLWZvdG8vZHNjMDQwNjguanBlZyUyMiwlMjJ0aXRsZSUyMjolMjJUZXN0JTIwbmFkcGlzJTIwb2JyYXprYSUyMiwlMjJzdWJ0aXRsZSUyMjolMjJUZXN0JTIwcG9kbmFkcGlzJTIwb2JyYXprYSUyMiwlMjJyZWRpcmVjdFVybCUyMjolMjIlMjIsJTIyaGVhZGluZ0NvbG9yJTIyOiUyMiNlNzBkMGRmZiUyMiwlMjJzdWJoZWFkaW5nQ29sb3IlMjI6JTIyIzE5ZTYyZWZmJTIyLCUyMmJhY2tncm91bmRDb2xvciUyMjolMjIlMjIlN0QlNUQ="
    };

    await Apps.assertParams(changedParams, "/components/app-slit_slider/news.jsp");

    I.say("Visual testing");
        I.clickCss('button.btn.btn-warning.btn-preview');
        await Document.waitForTab();
        I.switchToNextTab();

        I.waitForVisible("#slider");
        I.seeElement(".sl-slider-wrapper[style='height: 400px;']");
        within(".sl-slide-inner", () => {
            //Test image and bg color
            I.seeElement(".bg-img[style='background-image: url(/thumb/images/gallery/test-vela-foto/dsc04068.jpeg?w=1200&ip=1); background-color: !important;']");
            //Test header
            I.seeElement( locate("h2[style='padding-bottom:0px !important; padding-top: 15px; text-align:left ;size:70px; color:#e70d0dff!important;']").withText("Test nadpis obrazka") );
            //Test subheader
            I.seeElement( locate("p[style='padding-top: 0px; color:#19e62eff!important; text-align:icon_align_left ;size:30px;']").withText("Test podnadpis obrazka") );
        });
});