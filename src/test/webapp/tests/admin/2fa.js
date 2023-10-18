Feature('admin.2fa');

Scenario('overenie dropdown menu', ({ I, login }) => {
    login("admin");

    I.amOnPage("/admin/v9/");
    I.click("#dropdownMenuUser");
    I.see("Profil", "ul.dropdown-menu.show");
    I.see("Dvojstupňové overovanie", "ul.dropdown-menu.show");
    I.see("Správa šifrovacích kľúčov");
    I.see("Odhlásenie", "ul.dropdown-menu.show");
});

Scenario('overenie vyzvy pre zadanie kodu', ({ I, login }) => {
    I.logout();

    login("testerga");

    I.see("Pre váš účet je zapnuté dvojstupňové overovanie.");
    I.click("Odhlásiť sa");

    I.see("Prihlásiť sa");
});

Scenario('odhlasenie', ({ I, login }) => {
    I.logout();
});

Scenario('overenie nezobrazenie spravy sifrovacich klucov', ({ I, login }) => {
    login("tester2");
    I.amOnPage("/admin/v9/?removePerm=cmp_form");
    I.click("#dropdownMenuUser");
    I.see("Profil", "ul.dropdown-menu.show");
    I.see("Dvojstupňové overovanie", "ul.dropdown-menu.show");
    I.dontSee("Správa šifrovacích kľúčov");
    I.see("Odhlásenie", "ul.dropdown-menu.show");
});

Scenario('odhlasenie2', ({ I, login }) => {
    I.logout();
});