Feature('components.navbar');

Before(({ I, login }) => {
    login('admin');
});

Scenario('zobrazenie navbaru', ({ I }) => {
    I.amOnPage("/apps/navbar/podadresar-1/stranka-zobrazi-navigacnej-liste.html");
    I.see("Stránka sa zobrazí v navigačnej lište", "div.navbartest");

    I.amOnPage("/apps/navbar/podadresar-1/stranka-nezobrazi-navigacnej-liste.html");
    I.dontSee("Stránka sa nezobrazí v navigačnej lište", "div.navbartest");

    I.amOnPage("/apps/navbar/podadresar-1/stranka-zobrazi-navigacnej-liste-1.html");
    I.see("Stránka sa zobrazí v navigačnej lište podľa menu", "div.navbartest");

    I.amOnPage("/apps/navbar/podadresar-1/stranka-zobrazi-navigacnej-liste-1-1.html");
    I.dontSee("Stránka sa nezobrazí v navigačnej lište podľa menu", "div.navbartest");
});