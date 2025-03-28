Feature('webpages.webjet-toolbar');

function testToolbarVisibility(I, visible=true) {
    if (visible) {
        I.waitForElement("#webjetToolbar", 10);
        I.see("Produktová stránka", "#webjetToolbar");
        I.see("Tester Playwright", "#webjetToolbar");
        I.see("11", "#webjetToolbar");
    } else {
        I.wait(2);
        I.dontSeeElement("#webjetToolbar");
    }

}

Scenario('verify toolbar visibility', async ({ I, Document, DTE }) => {

    I.relogin("admin");
    Document.setConfigValue("disableWebJETToolbar", "false");

    I.amOnPage("/admin/v9/");
    I.amOnPage("/investicie/");
    testToolbarVisibility(I, false);


    //visit web sites and then check toolbar visibility
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.amOnPage("/investicie/");
    testToolbarVisibility(I, true);

    I.click("11", "#webjetToolbar");
    await Document.waitForTab(2);
    I.switchToNextTab();
    I.closeOtherTabs();

    DTE.waitForEditor();
    I.waitForText("Produktová stránka", 10, "#datatableInit_modal h5.modal-title");

    I.amOnPage("/investicie/");
    testToolbarVisibility(I, true);

    I.logout();

    I.amOnPage("/admin/v9/");
    I.amOnPage("/investicie/");

    testToolbarVisibility(I, false);
});

Scenario('verify toolbar visibility-revert', async ({ I, Document }) => {
    I.relogin("admin");
    Document.setConfigValue("disableWebJETToolbar", "true");
    I.amOnPage("/investicie/");
    testToolbarVisibility(I, false);

    I.amOnPage("/investicie/?NO_WJTOOLBAR=false");
    testToolbarVisibility(I, true);

    I.amOnPage("/investicie/?NO_WJTOOLBAR=true");
    testToolbarVisibility(I, false);

    I.amOnPage("/investicie/");
    testToolbarVisibility(I, false);
});

