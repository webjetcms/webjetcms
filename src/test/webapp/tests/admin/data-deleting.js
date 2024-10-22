Feature('admin.data-deleting');

Before(({ I, login }) => {
    login('admin');
});

Scenario("overenie prvotneho zoznamu", async ({ I, DT, DTE }) => {
  I.amOnPage("/admin/v9/settings/database-delete");
  DTE.waitForLoader();
  I.seeElement("div.toast-message");
  let rows = await I.getTotalRows();
  I.assertAbove(rows+1, 20, "Nedostatocny pocet zaznamov");

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

  rows = await I.getTotalRows();
  I.assertAbove(rows+1, 20, "Nedostatocny pocet zaznamov");
});

Scenario('cache-objects', ({ I, DT }) => {
  I.amOnPage("/admin/v9/settings/cache-objects/");
  I.waitForText('Zoznam cache objektov', 10);
  DT.filter("name", "browser");
  I.see("browserDetector-");
  DT.filter("name", "welcome");
  I.see("welcomeDataBackTimes-domainId=");

  I.amOnPage("/admin/v9/settings/persistent-cache-objects/");
  I.waitForText('Zoznam persistent cache objektov', 10);
  I.see("http://demo.webjetcms.sk/admin/mem.jsp");
  //check there is current date on update field, so the data was updated tody
  I.see(I.formatDate(new Date()), "#datatableInit tbody tr:nth-child(1) td:nth-child(5)");
});

Scenario('test permissions', ({ I }) => {
  I.amOnPage("/admin/v9/settings/database-delete/?removePerm=cmp_data_deleting");
  I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
  I.amOnPage("/admin/v9/settings/cache-objects/");
  I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
  I.amOnPage("/admin/v9/settings/persistent-cache-objects/");
  I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
});

Scenario('logout', ({ I }) => {
  I.logout();
});