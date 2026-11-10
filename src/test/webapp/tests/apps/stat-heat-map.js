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
function mockMap(I, {listRequests, multipleVersions = false, comparison = false, comparisonFailure = false}) {
    const comparisonWidths = {
        "": [{width: 1280, mode: "DOCUMENT", clicks: 32}, {width: 1024, mode: "DOCUMENT", clicks: 5}, {width: 960, mode: "CONTAINER", clicks: 40, previewWidth: 1440}],
        "123": [{width: 1280, mode: "DOCUMENT", clicks: 20}, {width: 960, mode: "CONTAINER", clicks: 18, previewWidth: 1366}],
        "122": [{width: 1280, mode: "DOCUMENT", clicks: 12}, {width: 1024, mode: "DOCUMENT", clicks: 5}, {width: 960, mode: "CONTAINER", clicks: 22, previewWidth: 1600}],
        "121": []
    };
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
            if (comparison) return route.fulfill({json: comparisonWidths[url.searchParams.get("versionId") || ""]});
            if (url.searchParams.get("versionId") === "123") return route.fulfill({json: []});
            const clicks = url.searchParams.get("versionId") === "122" ? 10 : 32;
            return route.fulfill({json: [
                {width: 1280, mode: "DOCUMENT", clicks, previewWidth: 1280},
                {width: 1024, mode: "DOCUMENT", clicks: 5, previewWidth: 1024}
            ]});
        }
        if (url.pathname.endsWith("/metadata")) {
            const versionId = url.searchParams.get("versionId");
            if (comparison) {
                if (comparisonFailure && versionId === "122") return route.fulfill({status: 500, body: "autotest metadata failure"});
                const group = comparisonWidths[versionId || ""].find(item => item.width === Number(url.searchParams.get("width")) && item.mode === url.searchParams.get("mode"));
                if (!group) return route.fulfill({status: 404, body: "No clicks at this width"});
                return route.fulfill({json: {
                    ...group, title: "autotest comparison", tileSize: 1024, source: "history", containerSelector: ".container",
                    versions: [123, 122, 121].map((historyId, index) => ({historyId, effectiveFrom: 1788300000000 - index * 86400000}))
                }});
            }
            const fallback = multipleVersions && !versionId;
            const width = Number(url.searchParams.get("width"));
            let clicks = versionId === "122" ? 10 : 32;
            if (width === 1024) clicks = 5;
            return route.fulfill({json: {
                title: "autotest click map preview", url: "/autotest-click-map.html",
                width, previewWidth: width, clicks, tileSize: 1024,
                historyId: fallback ? null : Number(versionId || 123), effectiveFrom: 1788213600000,
                source: fallback ? "current" : "history",
                historicalUnavailable: fallback, multipleVersions: fallback,
                versions: multipleVersions ? [{historyId: 123, effectiveFrom: 1788300000000}, {historyId: 122, effectiveFrom: 1788213600000}] : []
            }});
        }
        if (url.pathname.endsWith("/area")) {
            return route.fulfill({json: {clicks: 8, totalClicks: 32}});
        }
        if (url.pathname.endsWith("/hotspots")) {
            return route.fulfill({json: {totalClicks: 32, hotspots: [
                {x1: 160, y1: 160, x2: 280, y2: 280, x: 220, y: 220, clicks: 20},
                {x1: 520, y1: 2320, x2: 640, y2: 2440, x: 580, y: 2380, clicks: 8}
            ]}});
        }
        if (url.pathname.endsWith("/html/preview")) {
            return route.fulfill({contentType: "text/html", body: '<!doctype html><meta name="webjet-heatmap-preview" content="'
                + Number(url.searchParams.get("docId")) + '"><style>body{margin:0}.container{max-width:960px;margin:auto;min-height:3500px}</style><main class="container">autotest page</main>'});
        }
        if (url.pathname.endsWith("/binary/tile")) {
            return route.fulfill({contentType: "image/png", body: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=", "base64")});
        }
        return route.continue();
    });
}

Scenario("enforces statistics permission", ({ DT }) => {
    DT.checkPerms("cmp_stat", listUrl, "heatMapDataTable");
});

Scenario("shows hotspot totals and toggles the responsive sidebar", ({ I }) => {
    mockMap(I, {});
    I.resizeWindow(1440, 1000);
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapHotspotList button", 20);
    I.see("Top 2: 28 kliknutí · 87,5 %", "#heatMapHotspotTop");
    I.see("Ostatné oblasti: 4 kliknutí · 12,5 %", "#heatMapHotspotOther");
    I.clickCss("#heatMapHotspots");
    I.dontSeeElement("#heatMapHotspotPanel");
    I.clickCss("#heatMapHotspots");
    I.seeElement("#heatMapHotspotPanel");
    I.resizeWindow(740, 900);
    // The responsive panel must sit below the preview.
    I.waitForFunction(() => document.getElementById("heatMapHotspotPanel").getBoundingClientRect().top
        >= document.getElementById("heatMapPreviews").getBoundingClientRect().bottom - 1, 10);
});

Scenario("opens a resizable dialog and returns to the unchanged page list", async ({ I, DT }) => {
    const listRequests = [];
    mockMap(I, {listRequests});
    I.resizeWindow(1440, 1000);
    I.executeScript(url => window.location.assign(url), listUrl + "?dateRange=" + encodeURIComponent(dateRange));
    I.waitForVisible("#heatMapDataTable_wrapper", 20);
    DT.waitForLoader("heatMapDataTable");
    I.dontSeeElement("#heatMapDataTable_wrapper .buttons-create, #heatMapDataTable_wrapper .buttons-remove");
    DT.setDates("01.09.2026", "14.09.2026", "#heatMapDataTable_extfilter");
    DT.waitForLoader("heatMapDataTable");
    DT.checkExtfilterDates("01.09.2026", "14.09.2026");
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
    I.verifyDisabled("#heatMapCompare");
    I.see("Kliknutia: 32", "#heatMapTotalClicks");
    I.selectOption("#heatMapScale", "0.5");
    I.see("50 %", "#heatMapScaleSummary");
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
    I.seeAttributesOnElements("#heatMapReload", {"data-bs-original-title": "Obnoviť náhľad"});
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
    I.see("Kliknutia: 10", "#heatMapTotalClicks");
    I.selectOption("#heatMapVersion", "123");
    I.waitForVisible("#heatMapViewport", 10);
    I.waitForText("Kliknutia: 0", 10, "#heatMapTotalClicks");
    I.see("sa nezaznamenali žiadne kliknutia", "#heatMapStatus");
    I.dontSeeElement("#heatMapTiles img");
    I.see("nie sú zaznamenané kliknutia", "#heatMapHotspotStatus");
    I.dontSeeElement("#heatMapHotspotList button");
});

/** Waits for the shared fit scale after responsive layout and iframe resizing. */
async function checkComparisonGeometry(I, width) {
    // Frame dimensions and CSS transforms settle asynchronously through ResizeObserver.
    I.waitForFunction(([width]) => {
        const prefixes = ["heatMap", "heatMapCompare"];
        const expectedScale = Math.min(1, ...prefixes.map(prefix => Math.max(1, document.getElementById(prefix + "Viewport").clientWidth - 10) / width));
        return prefixes.every(prefix => document.getElementById(prefix + "Frame").contentWindow.innerWidth === width
            && Math.abs(new DOMMatrix(getComputedStyle(document.getElementById(prefix + "Stage")).transform).a - expectedScale) < 0.00001);
    }, [width], 10);
    const panes = await I.executeScript(() => ["heatMap", "heatMapCompare"].map(prefix => {
        const frame = document.getElementById(prefix + "Frame");
        const url = new URL(frame.src);
        return {docId: url.searchParams.get("docId"), dateRange: url.searchParams.get("dateRange"), versionId: url.searchParams.get("versionId"),
            width: frame.contentWindow.innerWidth, height: frame.contentWindow.innerHeight,
            transform: getComputedStyle(document.getElementById(prefix + "Stage")).transform};
    }));
    I.assertEqual(panes[0].docId, "11");
    I.assertEqual(panes[1].docId, panes[0].docId);
    I.assertEqual(panes[0].dateRange, dateRange);
    I.assertEqual(panes[1].dateRange, panes[0].dateRange);
    I.assertNotEqual(panes[0].versionId, panes[1].versionId);
    I.assertEqual(panes[0].height, panes[1].height);
    I.assertEqual(panes[0].transform, panes[1].transform);
}

Scenario("selects an area and compares versions at the same width and scale", async ({ I }) => {
    mockMap(I, {comparison: true});
    I.resizeWindow(1440, 1000);
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.clickCss("#heatMapSelectArea");
    I.waitForVisible("#heatMapAreaLayer", 10);
    I.dragAndDrop("#heatMapAreaLayer", "#heatMapAreaLayer", {
        sourcePosition: {x: 20, y: 20}, targetPosition: {x: 120, y: 80}
    });
    I.waitForText("Oblasť:", 10, "#heatMapAreaCount");
    I.clickCss("#heatMapCompare");
    I.waitForVisible("#heatMapCompareViewport", 10);
    I.waitForVisible("#heatMapViewport", 10);
    I.verifyDisabled("#heatMapSelectArea");
    I.verifyDisabled("#heatMapHotspots");
    I.dontSeeElement("#heatMapHotspotPanel");
    I.dontSeeElement("#heatMapAreaLayer, #heatMapAreaResult");
    I.see("Kliknutia: 20", "#heatMapTotalClicks");
    I.see("Kliknutia: 12", "#heatMapCompareTotalClicks");
    I.seeElement('#heatMapTiles img[src*="versionId=123"][src*="width=1280"]');
    I.seeElement('#heatMapCompareTiles img[src*="versionId=122"][src*="width=1280"]');
    I.seeElementInDOM('#heatMapVersion option[value="122"]:disabled');
    I.seeElementInDOM('#heatMapCompareVersion option[value="123"]:disabled');
    for (const screenWidth of [1440, 390]) {
        I.resizeWindow(screenWidth, 1000);
        await checkComparisonGeometry(I, 1280);
    }
    I.resizeWindow(1440, 1000);
    I.selectOption("#heatMapScale", "0.5");
    I.see("50 %", "#heatMapScaleSummary");
    I.see("50 %", "#heatMapCompareScaleSummary");
    const scales = await I.executeScript(() => ["heatMapStage", "heatMapCompareStage"].map(id => new DOMMatrix(getComputedStyle(document.getElementById(id)).transform).a));
    I.assertDeepEqual(scales, [0.5, 0.5]);
    I.clickCss("#heatMapVisible");
    I.seeAttributesOnElements("#heatMapVisible", {"aria-pressed": "false"});
    I.verifyDisabled("#heatMapOpacity");
    I.dontSeeElement("#heatMapOverlay");
    I.dontSeeElement("#heatMapCompareOverlay");
    I.clickCss("#heatMapVisible");
    I.seeAttributesOnElements("#heatMapVisible", {"aria-pressed": "true"});
    I.seeElement("#heatMapOpacity:enabled");
    I.seeElement("#heatMapOverlay");
    I.seeElement("#heatMapCompareOverlay");
    I.selectOption("#heatMapScale", "fit");
    I.selectOption("#heatMapWidth", "1024");
    I.waitForText("Kliknutia: 0", 10, "#heatMapTotalClicks");
    I.waitForText("Kliknutia: 5", 10, "#heatMapCompareTotalClicks");
    I.waitForVisible("#heatMapCompareViewport", 10);
    I.waitForVisible("#heatMapViewport", 10);
    await checkComparisonGeometry(I, 1024);
    I.see("Pre túto verziu a vybranú šírku nie sú zaznamenané kliknutia.", "#heatMapStatus");
    I.dontSeeElement("#heatMapOverlay");
    I.seeElement("#heatMapCompareOverlay");
    I.selectOption("#heatMapWidth", "container:960");
    I.waitForElement('#heatMapTiles img[src*="mode=CONTAINER"]', 10);
    I.waitForElement('#heatMapCompareTiles img[src*="mode=CONTAINER"]', 10);
    await checkComparisonGeometry(I, 1440);
    I.see("Kliknutia: 18", "#heatMapTotalClicks");
    I.see("Kliknutia: 22", "#heatMapCompareTotalClicks");
    I.executeScript(() => {
        const frame = document.getElementById("heatMapCompareFrame");
        frame.contentWindow.scrollTo(0, 400);
        window.autotestComparisonDocument = frame.contentDocument;
    });
    I.selectOption("#heatMapVersion", "121");
    I.waitForText("Kliknutia: 0", 10, "#heatMapTotalClicks");
    I.waitForVisible("#heatMapViewport", 10);
    I.waitForVisible("#heatMapCompareViewport", 10);
    await checkComparisonGeometry(I, 1440);
    I.see("Kliknutia: 22", "#heatMapCompareTotalClicks");
    I.assertTrue(await I.executeScript(() => {
        const frame = document.getElementById("heatMapCompareFrame");
        return frame.contentDocument === window.autotestComparisonDocument && frame.contentWindow.scrollY === 400;
    }), "Changing version A must preserve version B and its scroll position");
    I.clickCss("#heatMapCompare");
    I.waitForVisible("#heatMapViewport", 10);
    I.dontSeeElement("#heatMapComparePane");
    I.dontSeeElement("#heatMapCompareVersion");
    I.seeElement("#heatMapPaneLabel #heatMapVersion");
    I.see("Verzia stránky", "#heatMapVersionLabel");
    I.see("Kliknutia: 40", "#heatMapTotalClicks");
    I.seeInField("#heatMapVersion", "");
    I.seeElement("#heatMapSelectArea:enabled");
    I.waitForVisible("#heatMapHotspotList button", 10);
});

Scenario("keeps one comparison usable when the other version fails", ({ I }) => {
    mockMap(I, {comparison: true, comparisonFailure: true});
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.clickCss("#heatMapCompare");
    I.waitForText("Mapu kliknutí sa nepodarilo načítať.", 10, "#heatMapCompareStatus");
    I.waitForVisible("#heatMapViewport", 10);
    I.see("Kliknutia: 20", "#heatMapTotalClicks");
    I.dontSeeElement("#heatMapCompareSummary");
    I.dontSeeElement("#heatMapCompareViewport");
    I.clickCss("#heatMapCompare");
    I.waitForVisible("#heatMapViewport", 10);
    I.see("Kliknutia: 32", "#heatMapTotalClicks");
});
