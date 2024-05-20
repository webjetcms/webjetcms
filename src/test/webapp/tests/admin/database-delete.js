Feature('admin.database-delete');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/settings/database-delete");
});

Scenario("overenie prvotneho zoznamu", async ({ I, DTE }) => {
  DTE.waitForLoader();
  I.seeElement("div.toast-message");
  let rows = await I.getTotalRows();
  I.assertAbove(rows+1, 20, "Nedostatocny pocet zaznamov");
});

Scenario("filtrovanie zoznamu", async ({ I, DT }) => {
  I.fillField({css: "#dateDependentEntriesTable_extfilter input.dt-filter-from-from"}, "15.06.2020");
  I.fillField({css: "#dateDependentEntriesTable_extfilter input.dt-filter-to-from"}, "21.06.2020");
  I.seeElement("div.toast-message");
  I.toastrClose();
  I.click({css: "#dateDependentEntriesTable_extfilter button.filtrujem"});
  DT.waitForLoader();
  I.dontSeeElement("div.toast-message");

  //chybne stranky
  I.see("54", {css: "tr:nth-child(4)"});
  //monitorovanie servera
  I.see("79 343", {css: "tr:nth-child(12)"});
  //CRON
  I.see("2", {css: "tr:nth-child(19)"});

  let rows = await I.getTotalRows();
  I.assertAbove(rows+1, 20, "Nedostatocny pocet zaznamov");
});
