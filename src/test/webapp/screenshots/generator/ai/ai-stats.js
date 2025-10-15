Feature('ai.ai-stats');

Before(({ login }) => {
    login('admin');
});

Scenario('ai-stats screenshots', async ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/settings/ai-stats/");

    // TODO - az to bude merged, nastavit date range aby boli pekne grafy

    I.fillField("input.datetimepicker.min", "01.08.2025 00:00");
    I.fillField("input.datetimepicker.max", "31.10.2025 23:59");
    I.clickCss("button.dt-filtrujem-created");
    DT.waitForLoader();

    I.resizeWindow(1920, 1080);
    Document.screenshot("/redactor/ai/stat/all.png");

    I.resizeWindow(1920, 3500);
    Document.screenshotElement(locate("#graphsDiv > div.row:nth-child(1)"), "/redactor/ai/stat/graph-1.png");

    I.scrollTo("#ai-lineChartDaysUsage");
    Document.screenshotElement(locate("#graphsDiv > div.row:nth-child(2)"), "/redactor/ai/stat/graph-2.png");

    I.scrollTo("#ai-lineChartDaysTokens");
    Document.screenshotElement(locate("#graphsDiv > div.row:nth-child(3)"), "/redactor/ai/stat/graph-3.png");

    I.scrollTo("#tableDiv");
    await I.executeScript(() => {
        $("#tableDiv").css("padding", "5px");
        $("#tableDiv_2").css("padding", "5px");
    });
    Document.screenshotElement("#tableDiv", "/redactor/ai/stat/datatable.png");

    I.scrollTo("#tableDiv_2");
    Document.screenshotElement("#tableDiv_2", "/redactor/ai/stat/datatable_2.png");

    I.fillField("input.datetimepicker.min", "10.10.2025 00:00");
    I.fillField("input.datetimepicker.max", "15.10.2025 23:59");
    I.clickCss("button.dt-filtrujem-created");
    DT.waitForLoader();

    I.scrollTo("div#graphsDiv_2");
    Document.screenshotElement(locate("div#graphsDiv_2 > div"), "/redactor/ai/stat/graph-4.png");
});