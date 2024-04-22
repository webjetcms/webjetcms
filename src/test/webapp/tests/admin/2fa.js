Feature('admin.2fa');

Scenario('overenie dropdown menu', ({ I }) => {
    I.relogin("admin");

    I.amOnPage("/admin/v9/");
    I.click("#dropdownMenuUser");
    I.see("Profil", "ul.dropdown-menu.show");
    I.see("Dvojstupňové overovanie", "ul.dropdown-menu.show");
    I.see("Správa šifrovacích kľúčov");
    I.see("Odhlásenie", "ul.dropdown-menu.show");
});

Scenario('overenie vyzvy pre zadanie kodu', ({ I }) => {
    I.relogin("testerga", true, false);

    I.see("Pre váš účet je zapnuté dvojstupňové overovanie.");
    I.click("Odhlásiť sa");

    I.see("Prihlásiť sa");
});

Scenario('odhlasenie', ({ I, login }) => {
    I.logout();
});

Scenario('overenie nezobrazenie spravy sifrovacich klucov', ({ I }) => {
    I.relogin("tester2");
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

Scenario('forced 2fa QR code scan at logon', ({ I }) => {
    //it's faked by mobile_device set to aaa value - faking isGoogleAuthRequiredForAdmin=true
    I.relogin("testerga2", true, false);

    I.waitForText("Pre využívanie dvojstupňového overenia si na váš telefón nainštalujte aplikáciu", 10);
    I.see("overenie zadaním kľúča: ");
    I.see("Zadajte zobrazený kód v aplikácii");
    I.seeElement("#qrImage img[alt='Scan me!']");

    I.click("Odhlásiť sa");

    I.see("Prihlásiť sa");
});