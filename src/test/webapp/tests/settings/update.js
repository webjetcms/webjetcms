Feature('settings.update');

Before(({ I, login }) =>{
    login('admin');
    I.amOnPage("/admin/v9/settings/update/");
});

Scenario('update from server', async ({ I }) => {
    I.waitForElement("#version-selector");
    I.wait(1);

    I.click("2024.18");

    I.waitForText("v Štatistika-Prehliadače dôjde k drobným rozdielom, ale údaje o prehliadači anonymizovane zaznamenávame aj bez Cookies súhlasu.", 10);
    I.seeElement("#submitButton");
});

Scenario('update from file', async ({ I }) => {
    I.waitForElement("#version-selector");
    I.wait(1);

    I.click("Aktualizovať zo súboru");
    I.dontSeeElement("#submitButton");
    I.seeElement("#UPLOAD > form");

    I.click("#uploadSubmitButton");
    I.waitForText("Dokument nemôže byť prázdny");
    I.waitForElement(locate("div.alert-danger li").withText("Dokument nemôže byť prázdny"), 10);
    I.dontSeeElement("#submitButton");
    I.seeElement("#UPLOAD > form");

});

Scenario('test perms', async ({ I }) => {
    I.amOnPage("/admin/v9/settings/update/?removePerm=modUpdate");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
});

Scenario('logout', ({ I }) => {
    I.logout();
});