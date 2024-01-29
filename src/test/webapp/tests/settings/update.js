Feature('settings.update');

Before(({ I, login }) =>{
    login('admin');
    I.amOnPage("/admin/v9/settings/update/");
});

Scenario('update from server', async ({ I }) => {
    /* DISABLED kym nebudeme mat aktualizacie aj z JAR suborov
    I.waitForElement("#version-selector");
    I.wait(1);

    I.click("2023.40");
    I.wait(1);

    I.see("Pre zjednodušenie aktualizácie môžete použiť skript /admin/update/update-2023-18.jsp pre kontrolu a opravu JSP súborov.");
    I.seeElement("#submitButton");
    */
});

Scenario('update from file', async ({ I }) => {
    I.waitForElement("#version-selector");
    I.wait(1);

    I.click("Aktualizovať zo súboru");
    I.dontSeeElement("#submitButton");
    I.seeElement("#UPLOAD > form");

    I.click("#uploadSubmitButton");
    I.waitForText("Dokument nemôže byť prázdny");
    let count = await I.grabNumberOfVisibleElements(locate("div.alert-danger li").withText("Dokument nemôže byť prázdny"));
    I.assertEqual(count, 1);
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