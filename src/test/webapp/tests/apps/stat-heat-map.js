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
function mockMap(I, {listRequests, mapRequests, multipleVersions = false, comparison = false, comparisonFailure = false}) {
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
        if (mapRequests) mapRequests.push(url);
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

Scenario("allows area selection only in a single preview", async ({ I }) => {
    mockMap(I, {comparison: true});
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    const originalPreview = await I.grabElementBoundingRect("#heatMapViewport");
    I.clickCss("#heatMapSelectArea");
    I.waitForVisible("#heatMapAreaLayer", 10);
    I.dontSeeElement("#toast-container-heatmap .toast-info");
    I.assertDeepEqual(await I.grabElementBoundingRect("#heatMapViewport"), originalPreview,
        "Activating area selection must not move or resize the map");
    I.moveCursorTo("#heatMapSelectArea");
    I.waitForVisible(".tooltip.show", 5);
    I.see("Vybrať oblasť", ".tooltip.show");
    I.pressKey("Escape");
    I.waitToHide(".tooltip.show", 5);
    I.seeElement("#heatMapAreaLayer");
    I.pressKey("Escape");
    I.dontSeeElement("#heatMapAreaLayer");
    I.seeElement("#heatMapSelectArea:focus");
    I.pressKey("Enter");
    I.waitForVisible("#heatMapAreaLayer", 10);
    I.dragAndDrop("#heatMapAreaLayer", "#heatMapAreaLayer", {
        sourcePosition: {x: 20, y: 20},
        targetPosition: {x: 120, y: 80}
    });
    I.waitForText("Oblasť:", 10, "#heatMapAreaCount");
    I.clickCss("#heatMapCompare");
    I.waitForVisible("#heatMapCompareViewport", 10);
    I.verifyDisabled("#heatMapSelectArea");
    I.seeAttributesOnElements("#heatMapSelectArea", {"aria-pressed": "false"});
    I.dontSeeElement("#heatMapAreaLayer, #heatMapCompareAreaLayer, #heatMapAreaResult, #heatMapCompareAreaResult");
    I.clickCss("#heatMapCompare");
    I.waitForElement("#heatMapSelectArea:enabled", 10);
    I.clickCss("#heatMapSelectArea");
    I.waitForVisible("#heatMapAreaLayer", 10);
    I.dontSeeElement("#toast-container-heatmap .toast-info");
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
    I.verifyDisabled("#heatMapCompare");
    I.dontSeeElement("#heatMapUrl, #heatMapPeriod, #heatMapVersion");
    I.dontSeeElement('#heatMapViewer > p[data-th-text*="previewHelp"]');
    I.see("Kliknutia: 32", "#heatMapTotalClicks");
    I.see("Prispôsobiť priestoru", "#heatMapScaleSummary");
    I.see("Menej", ".pg-heatmap-legend");
    I.see("Viac kliknutí", ".pg-heatmap-legend");
    I.selectOption("#heatMapWidth", "1024");
    I.waitForText("Kliknutia: 5", 10, "#heatMapTotalClicks");
    I.selectOption("#heatMapWidth", "1280");
    I.waitForText("Kliknutia: 32", 10, "#heatMapTotalClicks");
    I.waitForVisible("#heatMapViewport", 10);
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

Scenario("compares versions of one page at the same width and scale", async ({ I }) => {
    mockMap(I, {comparison: true});
    I.resizeWindow(1440, 1000);
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.clickCss("#heatMapCompare");
    I.waitForVisible("#heatMapCompareViewport", 10);
    I.waitForVisible("#heatMapViewport", 10);
    I.see("Verzia A", "#heatMapPaneLabel");
    I.see("Verzia B", "#heatMapComparePaneLabel");
    I.seeElement("#heatMapPaneLabel #heatMapVersion");
    I.seeElement("#heatMapComparePaneLabel #heatMapCompareVersion");
    I.clickCss("#heatMapVersionLabel");
    I.seeElement("#heatMapVersion:focus");
    I.pressKey("Escape");
    I.clickCss("#heatMapCompareVersionLabel");
    I.seeElement("#heatMapCompareVersion:focus");
    I.pressKey("Escape");
    I.see("Kliknutia: 20", "#heatMapTotalClicks");
    I.see("Kliknutia: 12", "#heatMapCompareTotalClicks");
    I.seeElement('#heatMapTiles img[src*="versionId=123"][src*="width=1280"]');
    I.seeElement('#heatMapCompareTiles img[src*="versionId=122"][src*="width=1280"]');
    I.seeElementInDOM('#heatMapVersion option[value="122"]:disabled');
    I.seeElementInDOM('#heatMapCompareVersion option[value="123"]:disabled');
    for (const screenWidth of [1440, 1100, 700, 390]) {
        I.resizeWindow(screenWidth, 1000);
        await checkComparisonGeometry(I, 1280);
        const controlsFit = await I.executeScript(() => ["heatMap", "heatMapCompare"].every(prefix => {
            const header = document.getElementById(prefix + "PaneLabel").getBoundingClientRect();
            const select = document.getElementById(prefix + "Version").getBoundingClientRect();
            return select.left >= header.left && select.right <= header.right && select.top >= header.top && select.bottom <= header.bottom;
        }));
        I.assertTrue(controlsFit, "Each version picker must fit inside its own pane header");
        const toolbarFits = await I.executeScript(() => {
            const toolbar = document.querySelector(".pg-heatmap-controls").getBoundingClientRect();
            return [...document.querySelectorAll(".pg-heatmap-controls button, .pg-heatmap-controls select, .pg-heatmap-controls input")].every(control => {
                const rect = control.getBoundingClientRect();
                return rect.left >= toolbar.left && rect.right <= toolbar.right && rect.top >= toolbar.top && rect.bottom <= toolbar.bottom;
            });
        });
        I.assertTrue(toolbarFits, "Heatmap controls must stay inside the toolbar at narrow widths");
    }
    I.saveScreenshot("heatmap-comparison-narrow.png");
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
    I.saveScreenshot("heatmap-comparison-desktop.png");
    I.selectOption("#heatMapVersion", "121");
    I.waitForText("Kliknutia: 0", 10, "#heatMapTotalClicks");
    I.waitForVisible("#heatMapViewport", 10);
    I.waitForVisible("#heatMapCompareViewport", 10);
    await checkComparisonGeometry(I, 1440);
    I.see("Kliknutia: 22", "#heatMapCompareTotalClicks");
    I.clickCss("#heatMapCompare");
    I.waitForVisible("#heatMapViewport", 10);
    I.dontSeeElement("#heatMapComparePane");
    I.dontSeeElement("#heatMapCompareVersion");
    I.seeElement("#heatMapPaneLabel #heatMapVersion");
    I.see("Verzia stránky", "#heatMapVersionLabel");
    I.see("Kliknutia: 40", "#heatMapTotalClicks");
    I.seeInField("#heatMapVersion", "");
});

Scenario("reloads only the changed comparison version and preserves the other pane", async ({ I }) => {
    const mapRequests = [];
    mockMap(I, {comparison: true, mapRequests});
    I.resizeWindow(1440, 1000);
    I.executeScript(url => window.location.assign(url), detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.clickCss("#heatMapCompare");
    I.waitForVisible("#heatMapViewport", 10);
    I.waitForVisible("#heatMapCompareViewport", 10);
    I.selectOption("#heatMapScale", "0.5");

    for (const change of [
        {changed: "heatMap", unchanged: "heatMapCompare", version: "121", clicks: 0},
        {changed: "heatMapCompare", unchanged: "heatMap", version: "123", clicks: 20},
        {changed: "heatMap", unchanged: "heatMapCompare", version: "122", clicks: 12}
    ]) {
        await I.executeScript(async prefix => {
            const frame = document.getElementById(prefix + "Frame");
            frame.contentWindow.scrollTo(0, 400);
            // Let scroll-driven tile updates finish before retaining their DOM references.
            await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
            window.autotestUnchangedHeatMap = {
                doc: frame.contentDocument, src: frame.src, scrollY: frame.contentWindow.scrollY,
                tiles: [...document.querySelectorAll("#" + prefix + "Tiles img")],
                summary: document.getElementById(prefix + "TotalClicks").textContent
            };
        }, change.unchanged);
        mapRequests.length = 0;
        I.selectOption("#" + change.changed + "Version", change.version);
        I.waitForText("Kliknutia: " + change.clicks, 10, "#" + change.changed + "TotalClicks");
        await I.waitForVisible("#" + change.changed + "Viewport", 10);
        const preserved = await I.executeScript(prefix => {
            const frame = document.getElementById(prefix + "Frame");
            const previous = window.autotestUnchangedHeatMap;
            return {
                document: frame.contentDocument === previous.doc, source: frame.src === previous.src,
                scroll: frame.contentWindow.scrollY === previous.scrollY && previous.scrollY === 400,
                tiles: previous.tiles.every(tile => tile.isConnected),
                summary: document.getElementById(prefix + "TotalClicks").textContent === previous.summary
            };
        }, change.unchanged);
        I.assertDeepEqual(preserved, {document: true, source: true, scroll: true, tiles: true, summary: true});
        I.assertDeepEqual([...new Set(mapRequests.map(url => url.searchParams.get("versionId")))], [change.version]);
        I.seeInField("#heatMapWidth", "1280");
        I.seeInField("#heatMapScale", "0.5");
    }

    // Both version changes start in one browser task, before either width response arrives.
    I.executeScript(() => {
        for (const [id, version] of [["heatMapVersion", "121"], ["heatMapCompareVersion", "122"]]) {
            const select = document.getElementById(id);
            select.value = version;
            select.dispatchEvent(new Event("change", {bubbles: true}));
        }
    });
    I.waitForText("Kliknutia: 0", 10, "#heatMapTotalClicks");
    I.waitForText("Kliknutia: 12", 10, "#heatMapCompareTotalClicks");
    I.waitForVisible("#heatMapViewport", 10);
    I.waitForVisible("#heatMapCompareViewport", 10);
    I.seeElement('#heatMapCompareTiles img[src*="versionId=122"]');

    // A shared width change must use the new version even while its widths are loading.
    I.executeScript(() => {
        const version = document.getElementById("heatMapVersion");
        version.value = "123";
        version.dispatchEvent(new Event("change", {bubbles: true}));
        const width = document.getElementById("heatMapWidth");
        width.value = "container:960";
        width.dispatchEvent(new Event("change", {bubbles: true}));
    });
    I.waitForText("Kliknutia: 18", 10, "#heatMapTotalClicks");
    I.waitForText("Kliknutia: 22", 10, "#heatMapCompareTotalClicks");
    I.waitForElement('#heatMapTiles img[src*="versionId=123"][src*="mode=CONTAINER"]', 10);
    I.waitForElement('#heatMapCompareTiles img[src*="versionId=122"][src*="mode=CONTAINER"]', 10);
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
