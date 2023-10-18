Feature('settings.missing-keys');

var missing_key_a = "Zoznam kontaktov";
var missing_key_b = "Upload example";

Before(({ I, login }) =>{
    login('admin');
    I.amOnPage("/admin/v9/settings/missing-keys/");
});

Scenario('zakladne testy', async ({ I, DT }) =>{
    I.fillField("input.dt-filter-key", missing_key_a);
    I.pressKey('Enter', "input.dt-filter-key");
    DT.waitForLoader();
    I.see(missing_key_a);

    I.fillField("input.dt-filter-key", missing_key_b);
    I.pressKey('Enter', "input.dt-filter-key");
    DT.waitForLoader();
    I.see(missing_key_b);
});