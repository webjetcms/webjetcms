Feature('apps.bloky');

Scenario("Bloky - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/bloky/");
    I.waitForElement(locate("h1").withText(("Bloky")));
    I.see("zmením text");
});