Feature('ai.ai-stats');

Before(({ login }) => {
    login('admin');
});

Scenario('ai-stats screenshots', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/settings/ai-stats/");

    // TODO - az to bude merged, nastavit date range aby boli pekne grafy

    I.resizeWindow(1920, 1080);
    Document.screenshot("/redactor/ai/stat/all.png");

    I.resizeWindow(1920, 2500);
    Document.screenshotElement(locate("#graphsDiv > div.row:nth-child(1)"), "/redactor/ai/stat/graph-1.png");

    I.scrollTo("#ai-lineChartDaysUsage");
    Document.screenshotElement(locate("#graphsDiv > div.row:nth-child(2)"), "/redactor/ai/stat/graph-2.png");

    I.scrollTo("#ai-lineChartDaysTokens");
    Document.screenshotElement(locate("#graphsDiv > div.row:nth-child(3)"), "/redactor/ai/stat/graph-3.png");

    I.scrollTo("#tableDiv");
    Document.screenshotElement("#tableDiv", "/redactor/ai/stat/datatable.png");
});