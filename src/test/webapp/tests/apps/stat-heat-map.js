Feature("apps.stat-heat-map");

const listUrl = "/apps/stat/admin/heat-map/";
const dateRange = "daterange:1788213600000-1789423200000";
const detailsUrl = "/apps/stat/admin/heat-map-details/?docId=11&dateRange=" + encodeURIComponent(dateRange);
const apiPattern = /\/admin\/rest\/stat\/heat-map(?:\/|\?|$)/;
const modalFrame = "#modalIframeIframeElement";

Before(({ login }) => {
    login("admin");
});

After(({ I }) => {
    I.stopMockingRoute(apiPattern);
});

/** Install bounded response fixtures to test rendering independently of visitor data. */
function mockMap(I, {listRequests, multipleVersions = false}) {
    I.mockRoute(apiPattern, async route => {
        const url = new URL(route.request().url());
        if (["/admin/rest/stat/heat-map", "/admin/rest/stat/heat-map/all", "/admin/rest/stat/heat-map/search/findByColumns"].includes(url.pathname) && listRequests) {
            listRequests.push(url.toString());
            const content = Array.from({length: 15}, (_, index) => ({
                id: 11 + index, name: "autotest click map preview " + (index + 1),
                url: "/autotest-click-map-" + index + ".html", clicks: 32 + index, dayDate: 1788300000000
            }));
            return route.fulfill({json: {content, totalElements: content.length, numberOfElements: content.length, size: content.length, number: 0, totalPages: 1, empty: false}});
        }
        if (!url.pathname.startsWith("/admin/rest/stat/heat-map/")) return route.continue();
        if (!url.pathname.includes("/html/") && !url.pathname.includes("/binary/")) {
            const csrfToken = await route.request().frame().evaluate(() => window.csrfToken);
            if (!csrfToken || route.request().headers()["x-csrf-token"] !== csrfToken) {
                return route.fulfill({status: 403, body: "Missing CSRF token"});
            }
        }
        if (url.pathname.endsWith("/widths")) {
            return route.fulfill({json: [{width: 1280, mode: "DOCUMENT", clicks: 32, previewWidth: 1280}]});
        }
        if (url.pathname.endsWith("/metadata")) {
            const versionId = url.searchParams.get("versionId");
            const fallback = multipleVersions && !versionId;
            return route.fulfill({json: {
                title: "autotest click map preview", url: "/autotest-click-map.html",
                width: 1280, previewWidth: 1280, clicks: 32, tileSize: 1024,
                historyId: fallback ? null : Number(versionId || 123), effectiveFrom: 1788213600000,
                source: fallback ? "current" : "history",
                historicalUnavailable: fallback, multipleVersions: fallback,
                versions: multipleVersions ? [{historyId: 123, effectiveFrom: 1788300000000}, {historyId: 122, effectiveFrom: 1788213600000}] : []
            }});
        }
        if (url.pathname.endsWith("/html/preview")) {
            return route.fulfill({contentType: "text/html", body: '<!doctype html><meta name="webjet-heatmap-preview" content="'
                + Number(url.searchParams.get("docId")) + '"><main>autotest page</main>'});
        }
        if (url.pathname.endsWith("/binary/tile")) {
            return route.fulfill({contentType: "image/png", body: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=", "base64")});
        }
        return route.continue();
    });
}

Scenario("readonly list uses the shared date filter", ({ I, DT }) => {
    I.amOnPage(listUrl);
    I.waitForElement("#heatMapDataTable_wrapper", 20);
    DT.waitForLoader("heatMapDataTable");
    I.dontSeeElement("#heatMapDataTable_wrapper .buttons-create");
    I.dontSeeElement("#heatMapDataTable_wrapper .buttons-remove");
    DT.setDates("01.09.2026", "14.09.2026", "#heatMapDataTable_extfilter");
    DT.waitForLoader("heatMapDataTable");
    DT.checkExtfilterDates("01.09.2026", "14.09.2026");
});

Scenario("enforces statistics permission", ({ DT }) => {
    DT.checkPerms("cmp_stat", listUrl, "heatMapDataTable");
});

Scenario("opens a resizable dialog and returns to the unchanged page list", async ({ I, DT }) => {
    const listRequests = [];
    mockMap(I, {listRequests});
    I.resizeWindow(1440, 1000);
    I.executeScript(url => window.location.assign(url), listUrl + "?dateRange=" + encodeURIComponent(dateRange));
    I.waitForVisible("#heatMapDataTable_wrapper", 20);
    DT.waitForLoader("heatMapDataTable");
    I.waitForVisible('#heatMapDataTable tbody a', 20);
    const tableState = await I.executeScript(() => {
        heatMapDataTable.order([[heatMapDataTable.settings()[0].aoColumns.findIndex(column => column.data === "name"), "asc"]]).page.len(5).search("autotest").draw();
        heatMapDataTable.page(1).draw("page");
        const link = document.querySelector("#heatMapDataTable tbody a");
        link.id = "autotestHeatMapLink";
        window.autotestHeatMapTable = heatMapDataTable;
        return {url: location.href, title: link.textContent, page: heatMapDataTable.page(), order: heatMapDataTable.order(), search: heatMapDataTable.search()};
    });
    const requestCount = listRequests.length;
    I.clickCss("#autotestHeatMapLink");
    I.waitForVisible("#modalIframe.show", 10);
    I.see(tableState.title, "#modalIframe .modal-title");
    I.dontSeeElement("#modalIframe .modal-footer");
    I.seeElement("#modalIframe .dialog-buttons .maximize");
    I.switchTo(modalFrame);
    I.waitForVisible("#heatMapViewport", 20);
    I.dontSeeElement("#heatMapBack");
    I.dontSeeElement("#heatMapTitle");
    I.dontSeeElement("#heatMapUrl, #heatMapPeriod, #heatMapVersion");
    I.dontSeeElement('#heatMapViewer > p[data-th-text*="previewHelp"]');
    I.selectOption("#heatMapScale", "0.5");
    const originalHeight = await I.executeScript(() => document.getElementById("heatMapFrame").contentWindow.innerHeight);
    I.switchTo();
    I.clickCss("#modalIframe .dialog-buttons .maximize");
    I.waitForVisible("#modalIframe .modal-dialog.modal-fullscreen", 10);
    I.waitForFunction(([height]) => document.getElementById("modalIframeIframeElement").contentDocument.getElementById("heatMapFrame").contentWindow.innerHeight > height, [originalHeight], 10);
    I.assertEqual(await I.executeScript(() => document.getElementById("modalIframeIframeElement").contentDocument.getElementById("heatMapFrame").contentWindow.innerWidth), 1280);
    I.clickCss("#modalIframe .dialog-buttons .minimize");
    I.waitToHide("#modalIframe .modal-dialog.modal-fullscreen", 10);
    I.waitForFunction(([height]) => Math.abs(document.getElementById("modalIframeIframeElement").contentDocument.getElementById("heatMapFrame").contentWindow.innerHeight - height) <= 1, [originalHeight], 10);
    I.clickCss("#modalIframe .dialog-buttons button.btn-close");
    I.waitToHide("#modalIframe.show", 10);
    I.waitForFunction(() => document.activeElement?.id === "autotestHeatMapLink", 10);
    const afterClose = await I.executeScript(() => ({url: location.href, page: heatMapDataTable.page(), order: heatMapDataTable.order(), search: heatMapDataTable.search(), sameTable: window.autotestHeatMapTable === heatMapDataTable}));
    I.assertEqual(afterClose.url, tableState.url);
    I.assertEqual(afterClose.page, tableState.page);
    I.assertDeepEqual(afterClose.order, tableState.order);
    I.assertEqual(afterClose.search, tableState.search);
    I.assertTrue(afterClose.sameTable);
    I.assertEqual(listRequests.length, requestCount, "Closing the dialog must not reload the page list");
});

Scenario("shows historical fallback and mixed-version notices", ({ I }) => {
    mockMap(I, {multipleVersions: true});
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.seeElement("#heatMapBack");
    I.see("autotest click map preview", "#heatMapTitle");
    I.seeAttributesOnElements("#heatMapReload", {title: "Obnoviť náhľad"});
    I.dontSeeElement(".pg-heatmap-controls .bootstrap-select");
    I.seeElement("#heatMapVersion");
    I.dontSeeElement("#heatMapNotice");
    I.waitForVisible("#toast-container-webjet .toast-warning", 5);
    I.see("Mapa kliknutí", "#toast-container-webjet .toast-warning .toast-title");
    I.see("Historickú verziu sa nepodarilo určiť", "#toast-container-webjet .toast-warning");
    I.see("Mapa spája kliknutia z viacerých verzií", "#toast-container-webjet .toast-warning");
    I.waitToHide("#toast-container-webjet .toast-warning", 7);
    I.selectOption("#heatMapVersion", "122");
    I.waitForElement('#heatMapTiles img[src*="versionId=122"]', 10);
    I.seeElement('#heatMapFrame[src*="versionId=122"]');
});
