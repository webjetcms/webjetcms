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

Scenario("filter podla datumu", async ({ I, DT }) => {
  I.amOnPage("/admin/v9/apps/audit-search/");
  I.fillField({css: "input.dt-filter-from-createDate"}, "01. 01. 2020");
  I.fillField({css: "input.dt-filter-to-createDate"}, "31. 01. 2020");
  I.click({css: "div.dt-scroll-headInner button.dt-filtrujem-createDate"});
  I.wait(0.5);
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

Scenario("Audit - check menu items", ({ I }) => {
  I.say("Test admin can see everything");
  I.relogin("admin");
  I.amOnPage("/admin/v9/apps/audit-search/");
  I.see("Vyhľadávanie", "div.menu-wrapper");
  I.see("Zoznam notifikácií", "div.menu-wrapper");
  I.see("Zmenené stránky", "div.menu-wrapper");
  I.see("Čaká na publikovanie", "div.menu-wrapper");
  I.see("Úrovne logovania", "div.menu-wrapper");
  I.see("Log súbory", "div.menu-wrapper");
  I.see("Posledné logy", "div.menu-wrapper");

  I.amOnPage("/admin/v9/apps/audit-search/?removePerm=menuWebpages");
  I.see("Vyhľadávanie", "div.menu-wrapper");
  I.see("Zoznam notifikácií", "div.menu-wrapper");
  I.dontSee("Zmenené stránky", "div.menu-wrapper");
  I.dontSee("Čaká na publikovanie", "div.menu-wrapper");
  I.see("Úrovne logovania", "div.menu-wrapper");
  I.see("Log súbory", "div.menu-wrapper");
  I.see("Posledné logy", "div.menu-wrapper");

  I.amOnPage("/admin/v9/apps/audit-search/?removePerm=cmp_in-memory-logging");
  I.see("Vyhľadávanie", "div.menu-wrapper");
  I.see("Zoznam notifikácií", "div.menu-wrapper");
  I.see("Úrovne logovania", "div.menu-wrapper");
  I.dontSee("Log súbory", "div.menu-wrapper");
  I.dontSee("Posledné logy", "div.menu-wrapper");

  I.say("Test NON admin to see only Audit based items");
  I.relogin("permissionTest");
  I.amOnPage("/admin/v9/apps/audit-search/");
  I.see("Vyhľadávanie", "div.menu-wrapper");
  I.see("Zoznam notifikácií", "div.menu-wrapper");
  I.dontSee("Zmenené stránky", "div.menu-wrapper");
  I.dontSee("Čaká na publikovanie", "div.menu-wrapper");
  I.dontSee("Úrovne logovania", "div.menu-wrapper");
  I.see("Log súbory", "div.menu-wrapper");
  I.see("Posledné logy", "div.menu-wrapper");
});

Scenario("Audit - check menu items-logout", ({ I }) => {
  I.logout();
});
