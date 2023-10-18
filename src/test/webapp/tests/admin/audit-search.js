Feature('admin.audit-search');

/**
 * Otestovanie problemov so selectpickerom, ktory sa nedal zmenit ked sa:
 * - preslo na dalsiu stranku v zozname
 * - nastavil sa filter a nasledne sa zmazal
 * - otvorilo sa popup okno DT editora, zatvorilo a otvorilo sa znova
 */

Before(({ I, login }) => {
    login('admin');
});

function clearFilter(I) {
    I.click("#dt-filter-labels-link-logType");
    DT.waitForLoader();
}

Scenario("filter podla datumu", async ({ I, DT }) => {
  I.amOnPage("/admin/v9/apps/audit-search/");
  I.fillField({css: "input.dt-filter-from-createDate"}, "01. 01. 2020");
  I.fillField({css: "input.dt-filter-to-createDate"}, "31. 01. 2020");
  I.click({css: "div.dataTables_scrollHeadInner button.dt-filtrujem-createDate"});
  DT.waitForLoader();
  const value = await I.grabTextFrom({css: '#datatableInit > tbody > tr:nth-child(1) > td.dt-style-date > div'});
  const regex = /\d\d\.01\.2020\s\d\d\:\d\d/g
  const ok = regex.test(value);
  var assert = require('assert');
  assert.equal(ok, true);
});

Scenario("filter podla typu logu", ({ I, DT }) => {
  I.amOnPage("/admin/v9/apps/audit-search/");
  DT.filterSelect("logType", "CRON");
  I.see('CRON', {css: 'div.dt-filter-logType > button > div > div > div'});
  I.see('CRON', {css: '#datatableInit > tbody > tr:nth-child(1) > td:nth-child(3) > div'})
});

Scenario("filter podla user full name", ({ I, DT }) => {
  I.amOnPage("/admin/v9/apps/audit-search/");
  I.fillField({css: "input.dt-filter-userFullName"}, "WebJET Administrátor");
  I.click({css: "button.dt-filtrujem-userFullName"});
  DT.waitForLoader();
  I.see('WebJET Administrátor', {css: '#datatableInit > tbody > tr:nth-child(1) > td:nth-child(4) > div'})
});

Scenario("filter podla user description", async ({ I, DT }) => {
  I.amOnPage("/admin/v9/apps/audit-search/");
  I.fillField({css: "input.dt-filter-description"}, "Cron started!");
  I.click({css: "button.dt-filtrujem-description"});
  DT.waitForLoader();
  const value = await I.grabTextFrom({css: '#datatableInit > tbody > tr:nth-child(1) > td:nth-child(5) > div > a'});
  const regex = /Cron\sstarted\!/g
  const ok = regex.test(value);
  var assert = require('assert');
  assert.equal(ok, true);
});

Scenario("filter podla ip", ({ I, DT }) => {
  I.amOnPage("/admin/v9/apps/audit-search/");
  I.fillField({css: "input.dt-filter-ip"}, "127.0.0.1");
  I.click({css: "button.dt-filtrujem-ip"});
  DT.waitForLoader();
  I.see('127.0.0.1', {css: '#datatableInit > tbody > tr:nth-child(1) > td:nth-child(7) > div'})
});

Scenario("filter podla hostname", ({ I, DT }) => {
  I.amOnPage("/admin/v9/apps/audit-search/");
  I.fillField({css: "input.dt-filter-hostname"}, "localhost");
  I.click({css: "button.dt-filtrujem-hostname"});
  DT.waitForLoader();
  I.see('localhost', {css: '#datatableInit > tbody > tr:nth-child(1) > td:nth-child(8) > div'})
});
