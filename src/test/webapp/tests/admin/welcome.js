Feature('admin.welcome');

var random;

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/");
    I.waitForElement(".md-dashboard[data-loaded='true']", 20);
    random = I.getRandomText();
});

Scenario("Personal shortcuts replace the former bookmark section", ({ I }) => {
    I.seeElement('.md-dashboard__welcome .md-dashboard__shortcuts[aria-label="Vaše skratky"]');
    I.see("Upraviť skratky", ".md-dashboard__shortcut-actions");
    I.dontSeeElementInDOM(".md-dashboard__legacy");
    I.dontSeeElementInDOM("#webjet-overview-dashboard .bookmark");
    I.dontSeeElementInDOM("#bookmark_modal");
});

Scenario("feedback", ({ I }) => {
    var trigger = ".md-dashboard__feedback";

    I.see("Zaslať spätnú väzbu", trigger);
    I.clickCss(trigger);

    I.waitForVisible("#feedback_modal.show", 10);

    I.seeElementInDOM("#feedback_modal #feedback-upload.dz-clickable");
    I.seeElementInDOM("#feedback_modal #upload-wrapper.upload-wrapper");
    I.seeElementInDOM("#feedback_modal #toast-container-upload.toast-container-upload");
    I.seeElementInDOM("#feedback_modal #upload-toastr-template.upload-toastr-template .fa-progress-bar__progress");
    I.seeElementInDOM("#feedback_modal #upload-toastr-template .toast-error-message");

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
    I.waitForVisible(trigger, 10);
    I.clickCss(trigger);

    I.waitForVisible("#feedback_modal.show", 10);

    I.fillField("#feedback-group-text", "Test SPAM PROTECTION\n"+random);
    I.forceClick("#feedback_modal button.btn-primary");

    I.waitForInvisible("#feedback_modal", 10);
    I.waitForElement(".toast-message", 10);

    I.waitForText("Spätnú väzbu sa nepodarilo odoslať,", 10, ".toast-message");
    I.toastrClose();
});

Scenario("update-2023-18.jsp", ({ I }) => {
    //verify JSP is able to compile without errors
    I.amOnPage("/admin/update/update-2023-18.jsp");
    I.waitForText("Upraviť kód v JSP súboroch pre aktuálny WebJET", 10);
});
