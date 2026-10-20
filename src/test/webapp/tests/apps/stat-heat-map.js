Feature("apps.stat-heat-map");

const listUrl = "/apps/stat/admin/heat-map/";
const dateRange = "daterange:1788213600000-1789423200000";
const detailsUrl = "/apps/stat/admin/heat-map-details/?docId=11&dateRange=" + encodeURIComponent(dateRange);
const apiPattern = /\/admin\/rest\/stat\/heat-map(?:\/|\?|$)/;
const modalFrame = "#modalIframeIframeElement";

Before(({ I, login }) => {
    login("admin");
    I.stopMockingRoute(apiPattern);
});

/** Install bounded response fixtures to test rendering independently of visitor data. */
function mockMap(I, options = {}) {
    I.mockRoute(apiPattern, async route => {
        const url = new URL(route.request().url());
        if (["/admin/rest/stat/heat-map", "/admin/rest/stat/heat-map/all", "/admin/rest/stat/heat-map/search/findByColumns"].includes(url.pathname) && options.listRequests) {
            options.listRequests.push(url.toString());
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
            return route.fulfill({json: options.empty ? [] : [{width: 1280, clicks: 32}, {width: 390, clicks: 8}]});
        }
        if (url.pathname.endsWith("/metadata")) {
            return route.fulfill({json: {
                title: "autotest click map preview", url: "/autotest-click-map.html",
                width: Number(url.searchParams.get("width")), clicks: 32, tileSize: 1024,
                historyId: options.fallback ? null : 123, effectiveFrom: 1788213600000,
                source: options.fallback ? "current" : "history",
                historicalUnavailable: Boolean(options.fallback), multipleVersions: Boolean(options.multiple)
            }});
        }
        if (url.pathname.endsWith("/html/preview")) {
            return route.fulfill({contentType: "text/html", body: options.invalidPreview
                ? "<!doctype html><html><body>autotest unavailable preview</body></html>"
                : '<!doctype html><html><head><meta name="webjet-heatmap-preview" content="' + url.searchParams.get('docId') + '"><style>body{margin:0}main{height:3500px;min-width:2200px;background:linear-gradient(white,#ddd)}h1{margin:0}</style></head><body><main><h1>autotest page</h1><a href="/autotest-navigation">autotest link</a></main></body></html>'});
        }
        if (url.pathname.endsWith("/binary/tile")) {
            return route.fulfill({contentType: "image/png", body: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=", "base64")});
        }
        return route.continue();
    });
}

/** Wait until the map contains exactly the tiles intersecting the current preview viewport. */
function waitForVisibleTiles(I) {
    I.waitForFunction(() => {
        const frame = document.getElementById("heatMapFrame").contentWindow;
        const doc = frame.document.documentElement;
        const columns = Math.floor((Math.min(doc.scrollWidth, frame.scrollX + doc.clientWidth) - 1) / 1024) - Math.floor(frame.scrollX / 1024) + 1;
        const rows = Math.floor((Math.min(doc.scrollHeight, frame.scrollY + doc.clientHeight) - 1) / 1024) - Math.floor(frame.scrollY / 1024) + 1;
        return document.querySelectorAll("#heatMapTiles img").length === columns * rows;
    }, 10);
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

Scenario("keeps exact viewport width and loads only visible tiles", async ({ I }) => {
    mockMap(I);
    // Navigate from the administration to satisfy the same-origin referrer check.
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.seeElement("#heatMapBack");
    I.seeElement("#heatMapWidth");
    I.seeElement("#heatMapScale");
    I.dontSeeElement(".pg-heatmap-controls .bootstrap-select");
    I.seeInField("#heatMapWidth", "1280");
    I.see("autotest click map preview", "#heatMapTitle");
    I.dontSeeElement("#heatMapUrl, #heatMapPeriod, #heatMapVersion");
    I.assertEqual(await I.grabAttributeFrom("#heatMapReload", "title"), "Obnoviť náhľad");
    I.assertEqual(await I.executeScript(() => document.getElementById("heatMapFrame").contentWindow.innerWidth), 1280);
    I.waitForFunction(() => document.getElementById("heatMapFrame").getBoundingClientRect().bottom <= innerHeight, 10);
    waitForVisibleTiles(I);
    I.executeScript(() => document.getElementById("heatMapFrame").contentWindow.scrollTo(0, 1400));
    I.waitForFunction(() => document.getElementById("heatMapTiles").style.transform === "translate(0px, -1400px)", 10);
    I.dontSeeElement('#heatMapTiles img[data-tile="0:0"]');
    I.seeElement('#heatMapTiles img[data-tile="0:1"]');
    I.executeScript(() => document.getElementById("heatMapFrame").contentWindow.scrollTo(900, 1400));
    I.waitForFunction(() => document.getElementById("heatMapTiles").style.transform === "translate(-900px, -1400px)", 10);
    I.seeElement('#heatMapTiles img[data-tile="2:1"]');
    const tileParameters = await I.executeScript(() => Array.from(document.querySelectorAll("#heatMapTiles img"), tile => {
        const params = new URL(tile.src).searchParams;
        return {dateRange: params.get("dateRange"), width: params.get("width")};
    }));
    tileParameters.forEach(params => {
        I.assertEqual(params.dateRange, dateRange);
        I.assertEqual(params.width, "1280");
    });
    I.selectOption("#heatMapScale", "0.5");
    I.assertEqual(await I.executeScript(() => document.getElementById("heatMapFrame").contentWindow.innerWidth), 1280);
    I.assertEqual(await I.executeScript(() => document.getElementById("heatMapStage").style.transform), "scale(0.5)");
    I.selectOption("#heatMapWidth", "390");
    I.waitForVisible("#heatMapViewport", 20);
    I.waitForFunction(() => document.getElementById("heatMapFrame").contentWindow.innerWidth === 390, 10);
    waitForVisibleTiles(I);
    I.uncheckOption("#heatMapVisible");
    I.dontSeeElement("#heatMapOverlay");
    I.verifyDisabled("#heatMapOpacity");
    I.checkOption("#heatMapVisible");
    I.seeElement("#heatMapOverlay");
    I.assertFalse(await I.executeScript(() => document.getElementById("heatMapOpacity").disabled));
    I.executeScript(() => {
        const opacity = document.getElementById("heatMapOpacity");
        opacity.value = "40";
        opacity.dispatchEvent(new Event("input", {bubbles: true}));
    });
    I.see("40 %", "#heatMapOpacityValue");
    I.assertEqual(await I.executeScript(() => document.getElementById("heatMapOverlay").style.opacity), "0.4");
    const previewUrl = await I.grabAttributeFrom("#heatMapFrame", "src");
    I.clickCss("#heatMapReload");
    I.waitForVisible("#heatMapViewport", 20);
    I.assertNotEqual(await I.grabAttributeFrom("#heatMapFrame", "src"), previewUrl);
});

Scenario("shows historical fallback and mixed-version notices", ({ I }) => {
    mockMap(I, {fallback: true, multiple: true});
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.dontSeeElement("#heatMapVersion");
    I.dontSeeElement("#heatMapNotice");
    I.waitForVisible("#toast-container-webjet .toast-warning", 5);
    I.see("Mapa kliknutí", "#toast-container-webjet .toast-warning .toast-title");
    I.see("Historickú verziu sa nepodarilo určiť", "#toast-container-webjet .toast-warning");
    I.see("Mapa spája kliknutia z viacerých verzií", "#toast-container-webjet .toast-warning");
    I.waitToHide("#toast-container-webjet .toast-warning", 7);
    I.seeElement("#heatMapViewport");
});

Scenario("shows empty data and refuses an unrelated preview document", ({ I }) => {
    mockMap(I, {empty: true});
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForText("Vo vybranom období nie sú zaznamenané kliknutia", 20, "#heatMapStatus");
    I.verifyDisabled("#heatMapWidth");
    I.dontSeeElement("#heatMapViewport");
    I.stopMockingRoute(apiPattern);
    mockMap(I, {invalidPreview: true});
    I.refreshPage();
    I.waitForText("Náhľad stránky nie je dostupný", 20, "#heatMapStatus");
    I.dontSeeElement("#heatMapViewport");
});

Scenario("removes click map fixtures", ({ I }) => {
    I.stopMockingRoute(apiPattern);
});
