const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const { chromium } = require("playwright");

const adminPath = path.resolve(__dirname, "../../../main/webapp/apps/stat/admin");

test("container groups restore their offset at the representative viewport and hide mismatched maps", async t => {
    const browser = await chromium.launch({headless: true});
    t.after(() => browser.close());
    const page = await browser.newPage({viewport: {width: 1024, height: 1000}});
    const markup = fs.readFileSync(path.join(adminPath, "heat-map-details.html"), "utf8").split('<section id="heatMapViewer"')[1];
    const texts = {loading: "Loading", error: "Error", previewError: "Preview error", clicks: "Clicks",
        container: "Container", window: "Window", containerMissing: "Missing container {width} px"};
    let matching = true;
    let sizing = "max-width:1140px;width:100%";
    const tiles = [];
    await page.route("**/*", async route => {
        const url = new URL(route.request().url());
        if (url.pathname.endsWith("heat-map-viewer.js")) return route.fulfill({contentType: "text/javascript", body: fs.readFileSync(path.join(adminPath, "heat-map-viewer.js"), "utf8")});
        if (url.pathname.endsWith("heat-map.css")) return route.fulfill({contentType: "text/css", body: fs.readFileSync(path.join(adminPath, "heat-map.css"), "utf8")});
        if (url.pathname.endsWith("/widths")) return route.fulfill({json: [
            {width: 1140, mode: "CONTAINER", clicks: 20, previewWidth: 1369},
            {width: 1140, mode: "DOCUMENT", clicks: 2, previewWidth: 1140}]});
        if (url.pathname.endsWith("/metadata")) return route.fulfill({json: {title: "Preview", source: "current", tileSize: 1024,
            previewWidth: url.searchParams.get("mode") === "CONTAINER" ? 1369 : 1140, containerSelector: ".container"}});
        if (url.pathname.endsWith("/html/preview")) return route.fulfill({contentType: "text/html", body:
            '<!doctype html><meta name="webjet-heatmap-preview" content="11"><style>body{margin:0}.container{' + sizing + ';margin:auto;height:3000px}</style><div class="' + (matching ? "container" : "changed") + '">Content</div>'});
        if (url.pathname.endsWith("/binary/tile")) {
            tiles.push(url);
            return route.fulfill({contentType: "image/svg+xml", body: '<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024"/>'});
        }
        return route.fulfill({contentType: "text/html", body: '<!doctype html><style>.d-none{display:none!important}</style><link rel="stylesheet" href="/apps/stat/admin/heat-map.css"><section id="heatMapViewer"'
            + markup + '<script type="module">import {HeatMapViewer} from "/apps/stat/admin/heat-map-viewer.js";new HeatMapViewer(document.getElementById("heatMapViewer"),'
            + JSON.stringify(texts) + ').init();</script>'});
    });
    await page.goto("https://heatmap.test/view?docId=11");
    await page.locator("#heatMapTiles img").first().waitFor();
    assert.deepEqual(await page.locator("#heatMapWidth option").allTextContents(), [
        "Container 1140 px · Clicks: 20", "Window 1140 px · Clicks: 2"]);
    const frame = page.frames().find(item => item.url().includes("/html/preview"));
    assert.equal(await frame.evaluate(() => innerWidth), 1369);
    assert.equal(await page.locator("#heatMapTiles").evaluate(el => el.style.transform), "translate(114.5px, 0px)");
    assert.ok(tiles.every(url => url.searchParams.get("mode") === "CONTAINER" && url.searchParams.get("width") === "1140"));
    await frame.evaluate(() => scrollTo(0, 1400));
    await page.waitForFunction(() => document.getElementById("heatMapTiles").style.transform === "translate(114.5px, -1400px)");
    sizing = "width:1140px;max-width:100%";
    await page.locator("#heatMapReload").click();
    await page.locator("#heatMapTiles img").first().waitFor();
    assert.equal(await page.locator("#heatMapTiles").evaluate(el => el.style.transform), "translate(114.5px, 0px)",
        "Fixed CSS width restores the same container origin without a pixel max-width");
    sizing = "width:calc(100vw - 229px)";
    await page.locator("#heatMapReload").click();
    await page.getByText("Missing container 1140 px", {exact: true}).waitFor();
    assert.equal(await page.locator("#heatMapOverlay").isVisible(), false,
        "A fluid container must not match merely because its current width is 1140 pixels");
    matching = false;
    sizing = "max-width:1140px;width:100%";
    await page.locator("#heatMapReload").click();
    await page.getByText("Missing container 1140 px", {exact: true}).waitFor();
    assert.equal(await page.locator("#heatMapViewport").isVisible(), true);
    assert.equal(await page.locator("#heatMapOverlay").isVisible(), false);
    assert.equal(await page.locator("#heatMapTiles img").count(), 0);
    await page.selectOption("#heatMapWidth", "1140");
    await page.locator("#heatMapTiles img").first().waitFor();
    assert.equal(await page.locator("#heatMapOverlay").isVisible(), true, "Exact-coordinate fallback remains usable without a container");
    assert.equal(await frame.evaluate(() => innerWidth), 1140);
});

test("click map keeps CSS coordinates while scrolling, scaling and switching widths", async t => {
    const browser = await chromium.launch({headless: true});
    t.after(() => browser.close());
    const page = await browser.newPage({viewport: {width: 1024, height: 1000}});
    const csrfToken = "autotest-csrf-token";
    await page.addInitScript(token => { window.csrfToken = token; }, csrfToken);
    const errors = [];
    page.on("pageerror", error => errors.push(error.message));
    const markup = fs.readFileSync(path.join(adminPath, "heat-map-details.html"), "utf8").split('<section id="heatMapViewer"')[1];
    const texts = {title: "Click map", loading: "Loading", empty: "Empty", error: "Error", previewError: "Preview error", tileError: "Tile error", clicks: "Clicks", historical: "Historic", current: "Current", fallback: "Fallback", multiple: "Multiple", unavailable: "Unavailable"};
    const html = '<!doctype html><html><head><style>.d-none{display:none!important}body{margin:20px}</style><link rel="stylesheet" href="/apps/stat/admin/heat-map.css"></head><body><section id="heatMapViewer"'
        + markup + '<script>window.notifications=[];window.WJ={formatDate:v=>new Date(v).toISOString(),formatDateTime:v=>new Date(v).toISOString(),notifyWarning:(...args)=>window.notifications.push(args)};</script><script type="module">import {HeatMapViewer} from "/apps/stat/admin/heat-map-viewer.js";new HeatMapViewer(document.getElementById("heatMapViewer"),'
        + JSON.stringify(texts) + ').init();</script></body></html>';
    let mode = "normal";
    let metadataPaused;
    let resumeMetadata;
    const requests = [];
    await page.route("**/*", async route => {
        const url = new URL(route.request().url());
        requests.push(url);
        if (url.pathname.startsWith("/admin/rest/stat/heat-map/")
            && !url.pathname.includes("/html/") && !url.pathname.includes("/binary/")
            && route.request().headers()["x-csrf-token"] !== csrfToken) {
            return route.fulfill({status: 403, body: "Missing CSRF token"});
        }
        if (url.pathname.endsWith("heat-map-viewer.js")) return route.fulfill({contentType: "text/javascript", body: fs.readFileSync(path.join(adminPath, "heat-map-viewer.js"), "utf8")});
        if (url.pathname.endsWith("heat-map.css")) return route.fulfill({contentType: "text/css", body: fs.readFileSync(path.join(adminPath, "heat-map.css"), "utf8")});
        if (url.pathname.endsWith("/widths")) {
            if (mode === "widths-error") return route.fulfill({status: 403, body: "Forbidden"});
            return route.fulfill({json: mode === "empty" ? [] : [{width: 1280, clicks: 32}, {width: 390, clicks: 8}]});
        }
        if (url.pathname.endsWith("/metadata")) {
            if (mode === "metadata-error") return route.fulfill({status: 500, body: "Unavailable"});
            if (mode === "delayed" && url.searchParams.get("width") === "390") {
                await new Promise(resolve => {
                    resumeMetadata = resolve;
                    metadataPaused();
                });
            }
            return route.fulfill({json: {
                title: "autotest preview " + url.searchParams.get("width"), url: "/autotest.html", width: Number(url.searchParams.get("width")),
                clicks: 32, tileSize: 1024, historyId: 123, effectiveFrom: 1788213600000,
                source: mode === "unavailable" ? "unavailable" : "history", historicalUnavailable: false, multipleVersions: true
            }});
        }
        if (url.pathname.endsWith("/html/preview")) return route.fulfill({contentType: "text/html", body: mode === "invalid"
            ? "<!doctype html><html><body>Login</body></html>"
            : '<!doctype html><html><head><meta name="webjet-heatmap-preview" content="11"><style>body{margin:0}main{height:3500px;min-width:2200px;background:linear-gradient(white,#ddd)}</style></head><body><main><h1>autotest preview</h1><a href="/leave">Link</a></main></body></html>'});
        if (url.pathname.endsWith("/binary/tile")) return route.fulfill({contentType: "image/svg+xml", body: '<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024"><circle cx="150" cy="100" r="50" fill="red" opacity=".6"/></svg>'});
        return route.fulfill({contentType: "text/html", body: html});
    });
    const dateRange = "daterange:1788213600000-1789423200000";
    await page.goto("http://heatmap.test/apps/stat/admin/heat-map-details/?docId=11&dateRange=" + dateRange);
    await page.locator("#heatMapViewport:not(.d-none)").waitFor();
    assert.deepEqual(await page.evaluate(() => window.notifications), [["Click map", "Multiple", 5000]]);
    assert.equal(await page.locator("#heatMapNotice").count(), 0);
    assert.equal(await page.locator("#heatMapWidth").inputValue(), "1280");
    const frame = page.frames().find(item => item.url().includes("/html/preview"));
    assert.equal(await frame.evaluate(() => innerWidth), 1280);
    await page.waitForFunction(() => {
        const frame = document.getElementById("heatMapFrame");
        return frame.getBoundingClientRect().height > 0 && frame.getBoundingClientRect().bottom <= innerHeight;
    });
    await page.waitForFunction(() => {
        const doc = document.getElementById("heatMapFrame").contentDocument.documentElement;
        return document.querySelectorAll("#heatMapTiles img").length === Math.ceil(doc.clientWidth / 1024) * Math.ceil(doc.clientHeight / 1024);
    });
    await frame.evaluate(() => scrollTo(0, 1400));
    await page.waitForFunction(() => document.getElementById("heatMapTiles").style.transform === "translate(0px, -1400px)");
    assert.equal(await page.locator('#heatMapTiles img[data-tile="0:0"]').count(), 0);
    assert.ok(await page.locator('#heatMapTiles img[data-tile="0:1"]').count());
    await frame.evaluate(() => scrollTo(900, 1400));
    await page.waitForFunction(() => document.getElementById("heatMapTiles").style.transform === "translate(-900px, -1400px)");
    assert.ok(await page.locator('#heatMapTiles img[data-tile="2:1"]').count(), "Horizontal overflow must render tiles beyond the viewport width");
    for (const url of requests.filter(item => item.pathname.endsWith("/binary/tile"))) {
        assert.equal(url.searchParams.get("dateRange"), dateRange);
        assert.equal(url.searchParams.get("width"), "1280");
    }
    await page.selectOption("#heatMapScale", "0.5");
    assert.equal(await frame.evaluate(() => innerWidth), 1280);
    const originalHeight = await frame.evaluate(() => innerHeight);
    await page.setViewportSize({width: 1024, height: 1200});
    await page.waitForFunction(height => document.getElementById("heatMapFrame").contentWindow.innerHeight > height, originalHeight);
    assert.equal(await frame.evaluate(() => innerWidth), 1280);
    await page.setViewportSize({width: 1024, height: 1000});
    await page.waitForFunction(height => document.getElementById("heatMapFrame").contentWindow.innerHeight === height, originalHeight);
    await page.selectOption("#heatMapWidth", "390");
    await page.waitForFunction(() => document.getElementById("heatMapFrame").contentWindow.innerWidth === 390 && !document.getElementById("heatMapViewport").classList.contains("d-none"));
    await page.waitForFunction(() => document.querySelectorAll("#heatMapTiles img").length === Math.ceil(document.getElementById("heatMapFrame").contentDocument.documentElement.clientHeight / 1024));
    await page.uncheck("#heatMapVisible");
    assert.equal(await page.locator("#heatMapOverlay").isVisible(), false);
    assert.equal(await page.locator("#heatMapOpacity").isDisabled(), true);
    await page.check("#heatMapVisible");
    assert.equal(await page.locator("#heatMapOpacity").isDisabled(), false);
    await page.locator("#heatMapOpacity").fill("40");
    assert.equal(await page.locator("#heatMapOpacityValue").textContent(), "40 %");
    assert.equal(await page.locator("#heatMapOverlay").evaluate(node => node.style.opacity), "0.4");
    assert.equal(await page.locator("#heatMapUrl, #heatMapPeriod, #heatMapVersion").count(), 0);
    mode = "delayed";
    await page.selectOption("#heatMapWidth", "1280");
    await page.locator("#heatMapViewport:not(.d-none)").waitFor();
    const paused = new Promise(resolve => { metadataPaused = resolve; });
    await page.selectOption("#heatMapWidth", "390");
    await paused;
    const canceled = page.waitForEvent("requestfailed", request => request.url().includes("/metadata") && new URL(request.url()).searchParams.get("width") === "390");
    await page.selectOption("#heatMapWidth", "1280");
    await canceled;
    resumeMetadata();
    await page.locator("#heatMapViewport:not(.d-none)").waitFor();
    assert.equal(await page.locator("#heatMapTitle").textContent(), "autotest preview 1280");
    assert.equal(await page.locator("#heatMapStatus").isVisible(), false);
    mode = "empty";
    await page.reload();
    await page.waitForFunction(() => document.getElementById("heatMapStatus").textContent === "Empty");
    assert.equal(await page.locator("#heatMapWidth").isDisabled(), true);
    mode = "invalid";
    await page.reload();
    await page.waitForFunction(() => document.getElementById("heatMapStatus").textContent === "Preview error");
    assert.equal(await page.locator("#heatMapViewport").isVisible(), false);
    for (const failureMode of ["widths-error", "metadata-error", "unavailable"]) {
        mode = failureMode;
        await page.reload();
        const expected = mode === "unavailable" ? "Unavailable" : "Error";
        await page.waitForFunction(text => document.getElementById("heatMapStatus").textContent === text, expected);
        assert.equal(await page.locator("#heatMapViewer").getAttribute("aria-busy"), "false");
        assert.equal(await page.locator("#heatMapViewport").isVisible(), false);
    }
    assert.deepEqual(errors, []);
});

test("returning from a preview restores open and empty date ranges without saved endpoints", () => {
    const script = fs.readFileSync(path.join(adminPath, "heat-map.html"), "utf8").match(/<script>([\s\S]*?)<\/script>/)[1];
    const start = "1788213600000";
    const end = "1789423200000";
    for (const [range, expectedFrom, expectedTo] of [[start, start, ""], ["-" + end, "", end], ["", "", ""]]) {
        let configuration;
        const jquery = {empty() {}, appendTo() {}, on() {}};
        vm.runInNewContext(script, {
            heatMapColumns: [{data: "name"}, {data: "clicks"}], URLSearchParams,
            window: {location: {search: "?dateRange=" + encodeURIComponent(range ? "daterange:" + range : "")}, domReady: {add: callback => callback()}},
            WJ: {breadcrumb() {}, formatDate: value => String(value), DataTable: options => { configuration = options; return {}; }},
            ChartTools: {getSearchCriteria: () => ({".dt-filter-from-dayDate": "old from", ".dt-filter-to-dayDate": "old to"})},
            $: () => jquery
        });
        assert.equal(configuration.defaultSearch[".dt-filter-from-dayDate"], expectedFrom);
        assert.equal(configuration.defaultSearch[".dt-filter-to-dayDate"], expectedTo);
        assert.equal(configuration.order.length, 1);
        assert.equal(configuration.order[0][0], 1);
        assert.equal(configuration.order[0][1], "desc");
    }
});
