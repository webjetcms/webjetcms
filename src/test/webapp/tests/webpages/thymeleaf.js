Feature('webpages.thymeleaf');

Scenario('Overenie zobrazenia cez thymeleaf sablonu', ({ I }) => {

    I.amOnPage("/uvodna-stranka-thymeleaf.html");

    I.see("Zo sveta financií");
    I.see("Produktová stránka");

    I.see("Konsolidácia naprieč trhmi");
    I.see("Matej Pavlík");
    I.see("Prinášame pohľad na");

    I.see("DEMO STRÁNKA NA");

});