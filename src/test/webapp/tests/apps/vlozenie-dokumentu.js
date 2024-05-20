Feature('apps.vlozenie-dokumentu');

Scenario("Vlozenie dokumentu - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/vlozenie-dokumentu/");
    I.waitForElement('iframe');
    within({ frame: 'iframe[src*="docs.webjetcms.sk"]' }, () => {
        I.waitForText("Používateľská príručka", 120);
    });
});