Feature('settings.missing-keys');

var missing_key_a = "Prompt JSON to XML Converter";
var missing_key_b = "components.crontab.menu";

Before(({ I, login }) =>{
    login('admin');
    I.amOnPage("/admin/modules_allinfo.jsp");
    I.amOnPage("/admin/v9/settings/ai-assistants/");
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