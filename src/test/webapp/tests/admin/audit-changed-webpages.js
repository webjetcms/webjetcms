Feature('admin.audit-changed-webpages');

var pageTitle = "audit-changed-webpage";
var pageDocId = "81552";
var randomNumber;
Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Permission check", ({ I, DT }) => {
    I.say("Requires 2 perms: 'menuWebpages OR cmp_news' and 'cmp_adminlog'");
    DT.checkPerms("cmp_adminlog", "/admin/v9/apps/audit-changed-webpages/");
});

Scenario("logout 1", ({ I }) => {
    I.logout();
});

Scenario("Permission check menuWebpages", ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-changed-webpages/?removePerm=menuWebpages");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
});

Scenario("logout 2", ({ I }) => {
    I.logout();
});

Scenario("Check that after change, page is on top of audit-changed-webpages + it works as webpage @singlethread", async ({ I, DT, DTE }) => {
    var newBody = "TestBody_" + randomNumber;

    I.say("change prepared web page");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+pageDocId);
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-content-tab");
    I.waitForElement("#cke_data", 10);
    await DTE.fillCkeditor(newBody);
    DTE.save();

    I.say("Verify change in audit-changed-webpages");
    I.amOnPage("/admin/v9/apps/audit-changed-webpages/");
    DT.checkTableRow("changedWebPagesDataTable", 1, [pageDocId, "", pageTitle, "Tester Playwright"]);

    I.say("check body");
    I.click(pageTitle);
    DTE.waitForEditor("changedWebPagesDataTable");
    I.click("#pills-dt-changedWebPagesDataTable-content-tab");
    I.waitForElement("#cke_data", 10);
    I.switchTo("iframe.cke_reset");
    I.see(newBody);
    I.switchTo();
});

Scenario("Check buttons preview", ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-changed-webpages/");
    DT.filterContains("title", pageTitle);
    I.clickCss("button.dt-filter-id");
    I.clickCss("button.buttons-history-preview");
    I.switchToNextTab();
    I.see(pageTitle.toUpperCase(), "div.container h1");
    I.closeCurrentTab();
    I.switchTo();
});

Scenario("Check buttons stat", ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-changed-webpages/");
    DT.filterContains("title", pageTitle);
    I.clickCss("button.dt-filter-id");
    I.clickCss("button.buttons-stat");
    I.switchToNextTab();
    I.waitForElement("div#topDetails-lineVariantA");
    I.closeCurrentTab();
    I.switchTo();
});