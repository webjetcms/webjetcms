Feature('apps.app-slit_slider');

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
    I.assertContain(backgroundImageStyle["backgroundImage"], "/thumb/images/gallery/test-vela-foto/dsc04188.jpeg?w=1200&ip=1", 'The background image is not visible');
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