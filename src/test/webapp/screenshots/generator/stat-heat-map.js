Feature("screenshots.generator.stat-heat-map");

Before(({ login }) => {
    login("admin");
});

Scenario("heatmap documentation", async ({ I, DT, Document }) => {
    const dateRange = "daterange:" + new Date(2026, 10, 1).getTime() + "-" + new Date(2026, 10, 30, 23, 59, 59).getTime();
    I.resizeWindow(1440, 1000);
    I.amOnPage("/apps/stat/admin/heat-map/?dateRange=" + encodeURIComponent(dateRange));
    DT.waitForLoader("heatMapDataTable");
    I.waitForVisible("#heatMapDataTable tbody a", 20);
    Document.screenshot("/redactor/apps/stat/heat-map-list.png");
    const pages = await I.executeScript(async dateRange => {
        const rows = heatMapDataTable.rows().data().toArray();
        const result = [];
        for (const row of rows.slice(0, 8)) {
            const query = new URLSearchParams({docId: row.id, dateRange});
            const response = await fetch("/admin/rest/stat/heat-map/preview?" + query, {headers: {"X-CSRF-Token": window.csrfToken}});
            result.push({row, preview: await response.json()});
        }
        return result;
    }, dateRange);
    I.say(JSON.stringify(pages));
});
