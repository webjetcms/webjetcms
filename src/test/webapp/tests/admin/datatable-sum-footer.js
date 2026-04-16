Feature('admin.datatable-sum-footer');

Before(({ I, login }) => {
    login('admin');
});

Scenario("Datatable SUM footer logic", async ({ I, DT }) => {
    I.amOnPage("/apps/stat/admin/");
    DT.resetTable("statsDataTable");

    I.say("Test default showed footer");
    DT.setDates("01.01.2025", "01.05.2025", "#statsDataTable_extfilter");
    DT.waitForLoader();
    DT.checkFooterSumValues(I, "statsDataTable", ["", "", 37957, 21902, 382]);

    I.say("Re-order columns and test footer");
    I.dragAndDrop("div.dt-scroll-headInner th.dt-th-visits", "div.dt-scroll-headInner th.dt-th-dayDate", { force: true });
    DT.checkFooterSumValues(I, "statsDataTable", ["", 37957, "", 21902, 382]);

    I.say("Show/hide collumns and test footer");

    await DT.showColumn("Mesiac", "statsDataTable");
    await DT.showColumn("Návštev", "statsDataTable");

    DT.checkFooterSumValues(I, "statsDataTable", ["", 37957, "", "", 382]);

    I.say("Change page, test values are same because mode 'all'");
    I.click( locate("button.page-link").withText("2") );
    DT.waitForLoader();
    DT.checkFooterSumValues(I, "statsDataTable", ["", 37957, "", "", 382]);

    I.say("Resfresh columns order");
});

Scenario("Reset columns order", ({ I, DT }) => {
    I.amOnPage("/apps/stat/admin/");
    DT.resetTable("statsDataTable");
});
