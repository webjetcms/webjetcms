Feature('admin.welcome');

var random;

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/");
    I.wait(3);
    random = I.getRandomText();
});

Scenario("zalozky", ({ I }) => {
    var container = "#webjet-overview-dashboard .bookmark";

    I.see("Záložky", container);
    I.see("Web stránky", container);
    I.see("Formuláre", container);

    I.forceClick(container + " div.overview-logged__head__more i.ti");

    var title = "Autotest-"+random;

    I.waitForElement("#bookmark_modal");
    I.wait(2);

    I.click("#bookmark_modal button.btn-primary");
    I.see("Povinné pole. Zadajte aspoň jeden znak.", "#bookmark_modal");

    I.fillField("Názov záložky", title);
    I.fillField("Adresa stránky", "/admin/v9/"+random);
    I.click("#bookmark_modal button.btn-primary");

    I.waitForInvisible("#bookmark_modal");

    I.see(title, container);

    I.wait(1);

    //zmaz zaznamy
    I.forceClick(container+" div.overview-logged__content ul li:nth-child(3) button.buttons-remove");
    I.wait(1);
    I.dontSee(title, container);
    I.forceClick(container+" div.overview-logged__content ul li:nth-child(2) button.buttons-remove");
    I.wait(1);
    I.dontSee("Formuláre", container);
    I.forceClick(container+" div.overview-logged__content ul li:nth-child(1) button.buttons-remove");
    I.wait(1);

    //over, ze sa zobrazili defaultne
    I.see("Web stránky", container);
    I.see("Formuláre", container);
});

Scenario("feedback", ({ I }) => {
    var container = "#webjet-overview-dashboard .feedback";

    I.see("Spätná väzba", container);
    I.forceClick(container + " div.overview-logged__head__more i.ti");

    I.waitForElement("#feedback_modal");
    I.wait(2);

    I.forceClick("#feedback_modal button.btn-primary");
    I.see("Povinné pole. Zadajte aspoň jeden znak.", "#feedback_modal");

    I.fillField("#feedback-group-text", "Test spatna vazba\n"+random);
    I.forceClick("#feedback_modal button.btn-primary");

    I.waitForInvisible("#feedback_modal", 10);
    I.waitForElement(".toast-message", 10);

    I.waitForText("Spätná väzba bola odoslaná, ďakujeme za Váš čas.", 10, ".toast-message");
    I.toastrClose();

    //
    I.say("skus spam protection");
    I.forceClick(container + " div.overview-logged__head__more i.ti");

    I.waitForElement("#feedback_modal");
    I.wait(2);

    I.fillField("#feedback-group-text", "Test SPAM PROTECTION\n"+random);
    I.forceClick("#feedback_modal button.btn-primary");

    I.waitForInvisible("#feedback_modal", 10);
    I.waitForElement(".toast-message", 10);

    I.waitForText("Spätnú väzbu sa nepodarilo odoslať,", 10, ".toast-message");
    I.toastrClose();
});