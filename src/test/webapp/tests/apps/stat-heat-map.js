const assert = require("node:assert/strict");

Feature("apps.stat-heat-map");

const listUrl = "/apps/stat/admin/heat-map/";
const dateRange = "daterange:1788213600000-1789423200000";
const detailsUrl = "/apps/stat/admin/heat-map-details/?docId=11&dateRange=" + encodeURIComponent(dateRange);

Before(({ login }) => {
    login("admin");
});

After(({ I }) => {
    I.usePlaywrightTo("remove click map fixtures", async ({ page }) => {
        await page.unroute("**/admin/rest/stat/heat-map/**");
    });
});

/** Install bounded response fixtures to test rendering independently of visitor data. */
function mockMap(I, options = {}) {
    I.usePlaywrightTo("provide click map preview fixtures", async ({ page }) => {
        await page.route("**/admin/rest/stat/heat-map/**", async route => {
            const url = new URL(route.request().url());
            if (url.pathname.endsWith("/widths")) {
                await route.fulfill({json: options.empty ? [] : [{width: 1280, clicks: 32}, {width: 390, clicks: 8}]});
            } else if (url.pathname.endsWith("/metadata")) {
                await route.fulfill({json: {
                    title: "autotest click map preview", url: "/autotest-click-map.html",
                    width: Number(url.searchParams.get("width")), clicks: 32, tileSize: 1024,
                    historyId: options.fallback ? null : 123, effectiveFrom: 1788213600000,
                    source: options.fallback ? "current" : "history",
                    historicalUnavailable: Boolean(options.fallback), multipleVersions: Boolean(options.multiple)
                }});
            } else if (url.pathname.endsWith("/preview")) {
                await route.fulfill({contentType: "text/html", body: options.invalidPreview
                    ? "<!doctype html><html><body>autotest unavailable preview</body></html>"
                    : '<!doctype html><html><head><meta name="webjet-heatmap-preview" content="11"><style>body{margin:0}main{height:3500px;min-width:2200px;background:linear-gradient(white,#ddd)}h1{margin:0}</style></head><body><main><h1>autotest page</h1><a href="/autotest-navigation">autotest link</a></main></body></html>'});
            } else if (url.pathname.endsWith("/tile")) {
                await route.fulfill({contentType: "image/png", body: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=", "base64")});
            } else await route.continue();
        });
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

Scenario("keeps exact viewport width and loads only visible tiles", ({ I }) => {
    mockMap(I);
    I.amOnPage(detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.seeInField("#heatMapWidth", "1280");
    I.see("autotest click map preview", "#heatMapTitle");
    I.usePlaywrightTo("verify width, scale and lazy tile alignment", async ({ page }) => {
        const frame = page.frames().find(item => item.url().includes("/heat-map/preview"));
        assert.ok(frame, "The authenticated preview frame must be loaded");
        assert.equal(await frame.evaluate(() => window.innerWidth), 1280);
        assert.equal(await frame.evaluate(() => window.innerHeight), 800);
        await page.waitForFunction(() => document.querySelectorAll("#heatMapTiles img").length === 2);
        assert.deepEqual(await page.locator("#heatMapTiles img").evaluateAll(tiles => tiles.map(tile => tile.dataset.tile)), ["0:0", "1:0"]);
        await frame.evaluate(() => window.scrollTo(0, 1400));
        await page.waitForFunction(() => document.getElementById("heatMapTiles").style.transform === "translate(0px, -1400px)");
        assert.equal(await page.locator('#heatMapTiles img[data-tile="0:0"]').count(), 0);
        assert.ok(await page.locator('#heatMapTiles img[data-tile="0:1"]').count());
        await frame.evaluate(() => window.scrollTo(900, 1400));
        await page.waitForFunction(() => document.getElementById("heatMapTiles").style.transform === "translate(-900px, -1400px)");
        assert.ok(await page.locator('#heatMapTiles img[data-tile="2:1"]').count(), "Horizontal overflow must include tiles beyond the viewport width");
        for (const src of await page.locator("#heatMapTiles img").evaluateAll(tiles => tiles.map(tile => tile.src))) {
            assert.equal(new URL(src).searchParams.get("dateRange"), dateRange);
            assert.equal(new URL(src).searchParams.get("width"), "1280");
        }
    });
    I.selectOption("#heatMapScale", "0.5");
    I.usePlaywrightTo("scaling must not change the page layout width", async ({ page }) => {
        const frame = page.frames().find(item => item.url().includes("/heat-map/preview"));
        assert.equal(await frame.evaluate(() => window.innerWidth), 1280);
        assert.equal(await page.locator("#heatMapStage").evaluate(node => node.style.transform), "scale(0.5)");
    });
    I.selectOption("#heatMapWidth", "390");
    I.waitForVisible("#heatMapViewport", 20);
    I.usePlaywrightTo("mobile preview uses its exact width", async ({ page }) => {
        await page.waitForFunction(() => document.getElementById("heatMapFrame").contentWindow.innerWidth === 390);
        await page.waitForFunction(() => document.querySelectorAll("#heatMapTiles img").length === 1);
    });
    I.uncheckOption("#heatMapVisible");
    I.dontSeeElement("#heatMapOverlay");
});

Scenario("shows historical fallback and mixed-version notices", ({ I }) => {
    mockMap(I, {fallback: true, multiple: true});
    I.amOnPage(detailsUrl);
    I.waitForVisible("#heatMapViewport", 20);
    I.see("Aktuálny obsah", "#heatMapVersion");
    I.see("Historickú verziu sa nepodarilo určiť", "#heatMapNotice");
    I.see("Mapa spája kliknutia z viacerých verzií", "#heatMapNotice");
});

Scenario("shows empty data and refuses an unrelated preview document", ({ I }) => {
    mockMap(I, {empty: true});
    I.amOnPage(detailsUrl);
    I.waitForText("Vo vybranom období nie sú zaznamenané kliknutia", 20, "#heatMapStatus");
    I.verifyDisabled("#heatMapWidth");
    I.dontSeeElement("#heatMapViewport");
    I.usePlaywrightTo("replace the empty fixture", async ({ page }) => page.unroute("**/admin/rest/stat/heat-map/**"));
    mockMap(I, {invalidPreview: true});
    I.refreshPage();
    I.waitForText("Náhľad stránky nie je dostupný", 20, "#heatMapStatus");
    I.dontSeeElement("#heatMapViewport");
});
