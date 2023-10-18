Feature('components.cron-task');

var randomNumber = (new Date()).getTime();
var newTask = "sk.iway.iwcm." + randomNumber;

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/settings/cronjob");
});

Scenario('zoznam cronTaskov', ({ I }) => {
    I.see("Názov úlohy");
});

Scenario("pridanie noveho cronTasku", ({ I }) => {
    I.click("button.buttons-create");
    I.click("Pridať");
    I.wait(1);

    I.see("Povinné pole", "div.DTE_Field_Name_task");
    I.fillField("#DTE_Field_task", newTask);
    I.click("Pridať");
    I.wait(1);
});

Scenario('vyhladanie cronTasku', ({ I }) => {
    I.fillField("input.dt-filter-task", randomNumber);
    I.pressKey('Enter', "input.dt-filter-task");

    I.see(newTask);

    I.fillField("input.dt-filter-taskName", randomNumber);
    I.pressKey('Enter', "input.dt-filter-forward");

    I.see(newTask);
});

Scenario('upravenie cronTasku', ({ I, DTE }) => {
    I.fillField("input.dt-filter-task", newTask);
    I.pressKey('Enter', "input.dt-filter-task");
    I.wait(1);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-edit");
    I.wait(1);

    newTask = newTask + ".changed";

    I.fillField("#DTE_Field_task", newTask);
    DTE.save();

    I.fillField("input.dt-filter-task", randomNumber);
    I.pressKey('Enter', "input.dt-filter-task");
    I.wait(1);
    I.see(newTask);
});

Scenario('vymazanie cronTasku', ({ I }) => {
    I.fillField("input.dt-filter-task", newTask);
    I.pressKey('Enter', "input.dt-filter-key");
    I.wait(1);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.wait(1);

    I.fillField("input.dt-filter-task", randomNumber);
    I.pressKey('Enter', "input.dt-filter-key");
    I.dontSee(newTask);
});