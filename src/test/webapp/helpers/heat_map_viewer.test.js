const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const { chromium } = require("playwright");
const { PNG } = require("pngjs");

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
        if (url.pathname === "/jquery.js") return route.fulfill({contentType: "text/javascript", body: fs.readFileSync(require.resolve("jquery/dist/jquery.min.js"), "utf8")});
        if (/\/heat-map-(viewer|area)\.js$/.test(url.pathname)) return route.fulfill({contentType: "text/javascript", body: fs.readFileSync(path.join(adminPath, path.basename(url.pathname)), "utf8")});
        if (url.pathname.endsWith("heat-map.css")) return route.fulfill({contentType: "text/css", body: fs.readFileSync(path.join(adminPath, "heat-map.css"), "utf8")});
        if (url.pathname.endsWith("/widths")) return route.fulfill({json: [
            {width: 1140, mode: "CONTAINER", clicks: 20, previewWidth: 1369},
            {width: 1140, mode: "DOCUMENT", clicks: 2, previewWidth: 1140}]});
        if (url.pathname.endsWith("/metadata")) return route.fulfill({json: {title: "Preview", source: "current", tileSize: 1024, clicks: 20,
            previewWidth: url.searchParams.get("mode") === "CONTAINER" ? 1369 : 1140, containerSelector: ".container"}});
        if (url.pathname.endsWith("/html/preview")) return route.fulfill({contentType: "text/html", body:
            '<!doctype html><meta name="webjet-heatmap-preview" content="11"><style>body{margin:0;background:white}header,nav{top:0;height:64px;background:white}'
            + 'header{position:fixed;left:20%;width:20%}nav{position:sticky;margin-left:60%;width:20%}.container{' + sizing + ';margin:auto;height:3000px}</style>'
            + '<header></header><nav></nav><div class="' + (matching ? "container" : "changed") + '">Content</div>'});
        if (url.pathname.endsWith("/binary/tile")) {
            tiles.push(url);
            return route.fulfill({contentType: "image/svg+xml", body: '<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024"><rect width="100%" height="100%" fill="red"/></svg>'});
        }
        return route.fulfill({contentType: "text/html", body: '<!doctype html><style>.d-none{display:none!important}</style><link rel="stylesheet" href="/apps/stat/admin/heat-map.css"><section id="heatMapViewer"'
            + markup + '<script src="/jquery.js"></script><script>$.fn.tooltip=function(){return this;};window.WJ={initTooltip(){}};</script>'
            + '<script type="module">import {HeatMapViewer} from "/apps/stat/admin/heat-map-viewer.js";new HeatMapViewer(document.getElementById("heatMapViewer"),'
            + JSON.stringify(texts) + ').init();</script>'});
    });
    await page.goto("https://heatmap.test/view?docId=11");
    await page.locator("#heatMapTiles img").first().waitFor();
    assert.deepEqual(await page.locator("#heatMapWidth option").allTextContents(), [
        "Container 1140 px · Clicks: 20", "Window 1140 px · Clicks: 2"]);
    const frame = page.frames().find(item => item.url().includes("/html/preview"));
    assert.equal(await page.locator("#heatMapVersion").isVisible(), false);
    assert.equal(await frame.evaluate(() => innerWidth), 1369);
    assert.equal(await page.locator("#heatMapTiles").evaluate(el => el.style.transform), "translate(114.5px, 0px)");
    assert.ok(tiles.every(url => url.searchParams.get("mode") === "CONTAINER" && url.searchParams.get("width") === "1140"));
    await frame.evaluate(() => scrollTo(0, 1400));
    await page.waitForFunction(() => document.getElementById("heatMapTiles").style.transform === "translate(114.5px, -1400px)");
    await t.test("scrolled marks disappear behind pinned headers and remain visible beside and below them", async () => {
        await page.locator('#heatMapTiles img[data-tile="0:1"]').evaluate(tile => tile.decode());
        const preview = page.locator("#heatMapFrame");
        const screenshot = PNG.sync.read(await preview.screenshot());
        // Sample the red tile against the white page in the scaled preview.
        function hasMark(image, xFraction, y) {
            const row = Math.round(y * image.width / 1369);
            const pixel = (row * image.width + Math.round(image.width * xFraction)) * 4;
            return image.data[pixel + 1] < 200;
        }
        assert.equal(hasMark(screenshot, 0.3, 32), false, "Fixed header");
        assert.equal(hasMark(screenshot, 0.7, 32), false, "Sticky navigation");
        assert.equal(hasMark(screenshot, 0.5, 32), true, "Gap between headers");
        assert.equal(hasMark(screenshot, 0.3, 80), true, "Content below the header");

        await frame.evaluate(() => scrollTo(0, 0));
        await page.locator('#heatMapTiles img[data-tile="0:0"]').evaluate(tile => tile.decode());
        assert.equal(hasMark(PNG.sync.read(await preview.screenshot()), 0.3, 32), true, "Original header position");
    });
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
