Feature('apps.pribuzne-stranky');

Scenario("Príbuzné stránky - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/pribuzne-stranky/");
    I.waitForElement(locate('b').withText('Prečítajte si aj'));
    I.click(locate('a').withText('KONSOLIDÁCIA NAPRIEČ TRHMI'));
    I.seeElement(locate('h1').withText('Konsolidácia naprieč trhmi'));
    I.amOnPage("/apps/pribuzne-stranky/");
    I.click(locate('a').withText('MCGREGOROV OBCHODNÝ ÚDERA'));
    I.seeElement(locate('h1').withText('McGregorov obchodný údera'));
});