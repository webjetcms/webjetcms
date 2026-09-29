const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");

const tracker = fs.readFileSync(path.resolve(__dirname, "../../../main/webapp/components/stat/heat-map-tracker.js"), "utf8");

test("tracker records exact click widths and scroll coordinates without sending requests", async t => {
    const browser = await chromium.launch({headless: true});
    t.after(() => browser.close());
    const context = await browser.newContext({viewport: {width: 1280, height: 800}, hasTouch: true});
    const requests = [];
    await context.route("**/*", async route => {
        requests.push(route.request().url());
        if (route.request().url().endsWith("tracker.js")) return route.fulfill({contentType: "text/javascript", body: tracker});
        return route.fulfill({contentType: "text/html", headers: {"Content-Security-Policy": "script-src 'self'"}, body:
            '<!doctype html><html><body style="margin:0;height:4000px"><button style="position:absolute;top:1700px;left:50px">autotest target</button>'
            + '<script id="wj-heatmap-tracker" defer src="/tracker.js" data-doc-id="123" data-cookie-name="consent" data-allow-all="false" data-before-consent="false" data-decline-name="decline" data-decline-value="yes"></script>'
            + '<script defer src="/tracker.js"></script></body></html>'});
    });
    const page = await context.newPage();
    await page.goto("https://heatmap.test/page.html");
    const pending = async () => (await context.cookies()).filter(cookie => cookie.name.startsWith("wj_hm_"));
    await page.mouse.click(100, 100);
    assert.equal((await pending()).length, 0, "No collection before statistical consent");
    await context.addCookies([{name: "consent", value: "nutne_statisticke", url: "https://heatmap.test/"}]);
    const beforeClicks = requests.length;
    await page.mouse.click(100, 150);
    let events = await pending();
    assert.equal(events.length, 1, "Duplicate script inclusion has only one listener");
    assert.match(events[0].name, /^wj_hm_[a-f0-9]{32}$/);
    assert.match(events[0].value, /^v1\.123\.1280\.[0-9]+\.100\.150$/);
    assert.equal(events[0].domain, "heatmap.test");
    assert.equal(events[0].path, "/");
    assert.equal(events[0].sameSite, "Lax");
    assert.equal(events[0].secure, true);
    assert.ok(Math.abs(events[0].expires - Date.now() / 1000 - 86400) < 10);
    await page.setViewportSize({width: 390, height: 800});
    await page.evaluate(() => scrollTo(0, 1400));
    await page.touchscreen.tap(70, 310);
    events = await pending();
    assert.equal(events.length, 2);
    assert.ok(events.some(event => /^v1\.123\.390\.[0-9]+\.70\.1710$/.test(event.value)), "Tap retains document coordinates after resize and scroll");
    assert.equal(requests.length, beforeClicks, "Clicks never invoke a collector or beacon");
    await page.evaluate(() => document.dispatchEvent(new MouseEvent("click", {bubbles:true, clientX:20, clientY:20})));
    assert.equal((await pending()).length, 2, "Synthetic clicks are ignored");
    const secondPage = await context.newPage();
    await secondPage.goto("https://heatmap.test/second.html");
    await Promise.all([page.mouse.click(80, 310), secondPage.mouse.click(90, 90)]);
    events = await pending();
    assert.equal(events.length, 4, "Independent tab events do not overwrite each other");
    assert.equal(new Set(events.map(event => event.name)).size, 4);
    for (let index = 0; index < 22; index++) await secondPage.mouse.click(100 + index, 100);
    events = await pending();
    assert.equal(events.length, 16);
    assert.ok(events.reduce((bytes, event) => bytes + event.name.length + event.value.length + 2, 0) <= 2048);
    await page.evaluate(() => {
        document.cookie = "consent=nutne; Path=/; Secure; SameSite=Lax";
        document.dispatchEvent(new Event("webjet:cookie-consent"));
    });
    assert.equal((await pending()).length, 0, "Withdrawal removes queued events immediately");
    await secondPage.mouse.click(100, 100);
    assert.equal((await pending()).length, 0, "Other tabs observe withdrawal before collecting");
});
