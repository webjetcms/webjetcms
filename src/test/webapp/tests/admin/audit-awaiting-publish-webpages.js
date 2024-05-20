Feature('admin.audit-awaiting-publish-webpage');

var pageTitle = "Test publikovania v buducnosti";

Before(({ login }) => {
    login('admin');
});

Scenario("Permission check", ({ I, DT }) => {
    I.say("Requires 2 perms: 'menuWebpages' OR 'cmp_adminlog'");
    DT.checkPerms("cmp_adminlog", "/admin/v9/apps/audit-awaiting-publish-webpages/");
});

Scenario("Permission check menuWebpages", ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-awaiting-publish-webpages/?removePerm=menuWebpages");
    DT.filter("title", pageTitle);
    DT.checkTableRow("awaitingPublishWebpagesDataTable", 1, ["22955", pageTitle, "01.12.2030 06:00:00", "", "/Test stavov/"+pageTitle]);
});

Scenario("logout", ({ I }) => {
    I.logout();
});

Scenario("Check values", ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-awaiting-publish-webpages/");
    DT.filter("title", pageTitle);
    DT.checkTableRow("awaitingPublishWebpagesDataTable", 1, ["22955", pageTitle, "01.12.2030 06:00:00", "", "/Test stavov/"+pageTitle]);
});

Scenario("Check buttons preview", ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-awaiting-publish-webpages/");
    DT.filter("title", pageTitle);
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-history-preview");
    I.wait(2);
    I.switchToNextTab();
    I.see("TEST PUBLIKOVANIA V BUDUCNOSTI", "div.container h1");
    I.see("Test publikovania v\xa0buducnosti", "div.container p");
    I.closeCurrentTab();
});

Scenario("Check buttons stat", ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-awaiting-publish-webpages/");
    DT.filter("title", pageTitle);
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-stat");
    I.wait(2);
    I.switchToNextTab();
    I.waitForElement("div#topDetails-lineVariantA");
    I.closeCurrentTab();
});