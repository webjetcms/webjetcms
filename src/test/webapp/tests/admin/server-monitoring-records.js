Feature('admin.server-monitoring-records');

/**
 * Otestovanie zobrazovania grafov v monitorovani servera
 * je to zatial sproste, rata to s datami co uz mame v DB
 * pre ine projekty to bude padat
 * TODO: na backende najskor pripravit test data a az potom spustit test
 */

Before(({ I, login }) => {
    login('admin');
});

Scenario("filter podla datumu", async ({ I, DT }) => {
  I.amOnPage("/apps/server_monitoring/admin/records/");

  within("#serverMonitoringRecordsTable_extfilter", () => {
    I.fillField({css: "input.dt-filter-from-dayDate"}, "17.7.2020 13:55");
    I.fillField({css: "input.dt-filter-to-dayDate"}, "17.7.2020 14:00");
    I.click({css: "button.dt-filtrujem-dayDate"});
  });
  DT.waitForLoader();

  //pockaj na loader pre grafy
  I.waitForInvisible("#loader", 20);

  //check at least values in datatable
  DT.checkTableRow("serverMonitoringRecordsTable", 3, ["949383", "18.07.2020 13:59:30", "webjet4.dev.iwa", "1", "3", "50 646 312", "243 269 632", "5", "0", "0", "0"]);
  DT.checkTableRow("serverMonitoringRecordsTable", 1, ["949385", "18.07.2020 14:00:00", "webjet4.dev.iwa", "3", "1", "97 182 960", "243 269 632", "5", "0", "0", "0"]);
  DT.checkTableRow("serverMonitoringRecordsTable", 6, ["949380", "18.07.2020 13:59:00", "webjet4.dev.iwa", "1", "3", "95 950 216", "243 269 632", "5", "0", "0", "0"]);


  //zmen rozsah na iny
  within("#serverMonitoringRecordsTable_extfilter", () => {
    I.fillField({css: "input.dt-filter-from-dayDate"}, "25.7.2020 08:00");
    I.fillField({css: "input.dt-filter-to-dayDate"}, "25.7.2020 08:05");
    I.click({css: "button.dt-filtrujem-dayDate"});
  });
  DT.waitForLoader();

  //pockaj na loader pre grafy
  I.waitForInvisible("#loader", 20);

  //check at least values in datatable
  DT.checkTableRow("serverMonitoringRecordsTable", 1, ["997351", "26.07.2020 08:05:00", "webjet4.dev.iwa", "1", "3", "88 842 728", "243 269 632", "6", "0", "0", "0"]);
  DT.checkTableRow("serverMonitoringRecordsTable", 3, ["997349", "26.07.2020 08:04:30", "webjet4.dev.iwa", "1", "3", "79 851 176", "243 269 632", "7", "0", "0", "0"]);
  DT.checkTableRow("serverMonitoringRecordsTable", 5, ["997347", "26.07.2020 08:04:00", "webjet4.dev.iwa", "1", "3", "39 923 768", "243 269 632", "7", "0", "0", "0"]);
});

Scenario("aktualne hodnoty", ({ I }) => {
  I.amOnPage("/apps/server_monitoring/admin/");

  //over fungovanie prekladov
  I.see("Dátum a čas spustenia servera");
  I.see("Správca jazyka JAVA");
  I.see("0 dní");
  I.see("hodín");
  I.see("minút");
});

/**
 * Phase 1, check we see delet button, then to web pages and open "Jet portal 4 - testovacia stranka", close current tab
 * This is importatnt so we can see chnages in out stat's
 */
function phase1(I) {
  I.seeElement("button.buttons-remove");
  I.click("button.buttons-remove")
  I.wait(1);
  I.see("Nenašli sa žiadne vyhovujúce záznamy");

  I.amOnPage("/admin/v9/webpages/web-pages-list/");
  I.click("tr.odd a.preview-page-link");
  I.wait(2);
  I.switchToNextTab();
  I.closeCurrentTab();
}

/**
 * Check that remove button (remove current records) works
 */
function phase2(I, DT) {
  DT.filter("whatWasExecuted", "");
  I.click("button.buttons-remove")
  I.wait(1);
  I.see("Nenašli sa žiadne vyhovujúce záznamy");
}

/**
 * Check that after changing NODe delete button will be hidden.
 * Then call refresh of data and check "no records", "loader" and "notification".
 */
function phase3(I) {
  I.seeElement("button.buttons-remove");
  I.click(locate("button.dropdown-toggle.bootstrap-select").withText('(Aktuálny uzol)'));
  I.click(locate("a.dropdown-item").withText("node3"));
  I.wait(1);
  I.dontSeeElement("button.buttons-remove");
  I.click({css: "button.buttons-refresh"});
  I.wait(1);
  I.see("Nenašli sa žiadne vyhovujúce záznamy");
  I.seeElement("#webjetAnimatedLoader");
  I.seeElement("#toast-container-webjet");
  I.see("Získavanie dát");
}

Scenario("Monitoring server components", ({I, DT}) => {
  I.amOnPage("/apps/server_monitoring/admin/components/");

  phase1(I);

  //Check values
  I.amOnPage("/apps/server_monitoring/admin/components/");
  DT.filter("whatWasExecuted", "/components/banner/banner.jsp");
  I.see("/components/banner/banner.jsp");
  I.dontSee("/components/menu/menu_ul_li.jsp");

  DT.filter("whatWasExecuted", "/components/menu/menu_ul_li.jsp");
  I.see("/components/menu/menu_ul_li.jsp");
  I.dontSee("/components/banner/banner.jsp");

  phase2(I, DT);

  phase3(I);
});

Scenario("Monitoring server documents", ({I, DT}) => {
  I.amOnPage("/apps/server_monitoring/admin/documents/");

  phase1(I);

  //Check values
  I.amOnPage("/apps/server_monitoring/admin/documents/");
  DT.filter("whatWasExecuted", "/admin/v9/webpages/web-pages-list/");
  I.see("/admin/v9/webpages/web-pages-list/");
  I.dontSee("/apps/server_monitoring/admin/documents/");

  DT.filter("whatWasExecuted", "/apps/server_monitoring/admin/documents/");
  I.see("/apps/server_monitoring/admin/documents/");
  I.dontSee("/admin/v9/webpages/web-pages-list/");

  phase2(I, DT);

  phase3(I);
});

Scenario("Monitoring server sql", ({I, DT}) => {
  I.amOnPage("/apps/server_monitoring/admin/sql/");

  phase1(I);

  //Check values
  I.amOnPage("/apps/server_monitoring/admin/sql/");
  DT.filter("whatWasExecuted", "SELECT id, doc_id, banner_id FROM banner_doc WHERE (banner_id = ?)");
  I.see("SELECT id, doc_id, banner_id FROM banner_doc WHERE (banner_id = ?)");
  I.dontSee("SELECT id, group_id, banner_id FROM banner_gr WHERE (banner_id = ?)");

  DT.filter("whatWasExecuted", "SELECT id, group_id, banner_id FROM banner_gr WHERE (banner_id = ?)");
  I.see("SELECT id, group_id, banner_id FROM banner_gr WHERE (banner_id = ?)");
  I.dontSee("SELECT id, doc_id, banner_id FROM banner_doc WHERE (banner_id = ?)");

  phase2(I, DT);

  phase3(I);
});


