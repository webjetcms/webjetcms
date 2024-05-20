Feature('apps.vyhladavanie');

Scenario("Vyhľadávanie - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/vyhladavanie/");

    I.waitForElement("#searchWords");
    I.fillField('#searchWords', 'Kontakt');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Približný počet výsledkov", 10);

    I.fillField('#searchWords', 'Nothing');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Časový limit medzi dvoma hľadaniami je 10 sekúnd. Počkajte a skúste vyhľadanie neskôr.", 10);

    I.wait(11);
    I.fillField('#searchWords', 'Nothing');
    I.click('form.smallSearchForm > p > input.smallSearchSubmit');
    I.waitForText("Neboli nájdené žiadne stránky vyhovujúce zadaným kritériám.", 10);
});