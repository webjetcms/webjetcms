Feature('apps.app-testimonials');

Scenario("Odporúčania - test zobrazovania", async ({ I }) => {
    I.amOnPage("/apps/odporucania/");
    I.waitForElement("#testImonials1");
    I.seeElement(locate("h1").withText(("Odporúčania")));

    I.seeElement(locate("#testImonials1 > div.testimonials_content.wow.slideInLeft > .testimonials-text")
    .withText("WebJET CMS je bezpečný a spoľahlivý systém, vysoko ho odporúčam pre všetkých."));
    I.seeElement(locate("#testImonials1 > div.testimonials_content.wow.slideInLeft > div > span > .testimonials-name").withText("Arnold"));
    const backgroundImageLeft = await I.grabCssPropertyFrom("#testImonials1 > div.testimonials_content.wow.slideInLeft > span", "background-image");
    I.assertContain(backgroundImageLeft, '/thumb/images/gallery/user/admin.jpg?w=100&h=100&ip=5")', 'Left Image is not loaded');

    I.seeElement(locate("#testImonials1 > div.testimonials_content.wow.slideInUp > .testimonials-text")
    .withText("Z pohľadu marketingu oceňujem jednoduchosť použitia a rýchlosť úprav, ktoré viem realizovať sama."));
    I.seeElement(locate("#testImonials1 > div.testimonials_content.wow.slideInUp > div > span > .testimonials-name").withText("Mária"));
    const backgroundImageUp = await I.grabCssPropertyFrom("#testImonials1 > div.testimonials_content.wow.slideInUp > span", "background-image");
    I.assertContain(backgroundImageUp, '/thumb/images/gallery/user/dwaynejohnson.jpg?w=100&h=100&ip=5")', 'Up Image is not loaded');
});

Scenario("Editor test", ({ I, login, Apps }) => {
    login('admin');

    Apps.openAppEditor(77773);

    I.say("Change style");
    I.clickCss("#pills-dt-component-datatable-basic-tab");
    I.click( locate("div.image_radio_item") );

    I.say("Turn off images and names");
    I.clickCss("#pills-dt-component-datatable-advanced-tab");
    I.uncheckOption("#DTE_Field_showPhoto_0");
    I.uncheckOption("#DTE_Field_showName_0");

    I.say("Now go check the changes");
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.seeElement("#testImonials1");

    I.seeElement("#testImonials1 div.testimonials_content:nth-child(1) span div.testimonials-text");
    I.seeElement( locate("#testImonials1 div.testimonials_content:nth-child(1) span div.testimonials-text p").withText("WebJET CMS je bezpečný a spoľahlivý systém, vysoko ho odporúčam pre všetkých.") );
    I.seeElement("#testImonials1 div.testimonials_content:nth-child(1) span div.testimonials-text h3.testimonials-name");
    I.dontSeeElement( locate("#testImonials1 div.testimonials_content:nth-child(1) span div.testimonials-text h3.testimonials-name").withText("Arnold") );
    I.dontSeeElement("#testImonials1 div.testimonials_content:nth-child(1) span span.testimonials-avatar");


    I.seeElement("#testImonials1 div.testimonials_content:nth-child(2) span div.testimonials-text");
    I.seeElement( locate("#testImonials1 div.testimonials_content:nth-child(2) span div.testimonials-text p").withText("Z pohľadu marketingu oceňujem jednoduchosť použitia a rýchlosť úprav, ktoré viem realizovať sama.") );
    I.dontSeeElement( locate("#testImonials1 div.testimonials_content:nth-child(2) span div.testimonials-text h3.testimonials-name").withText("Mária") );
    I.dontSeeElement("#testImonials1 div.testimonials_content:nth-child(2) span span.testimonials-avatar");
});