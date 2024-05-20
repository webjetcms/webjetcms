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