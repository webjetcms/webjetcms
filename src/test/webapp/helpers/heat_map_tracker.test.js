const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");

const tracker = fs.readFileSync(path.resolve(__dirname, "../../../main/webapp/components/stat/heat-map-tracker.js"), "utf8");

for (const sizing of ["max-width:1140px;width:100%", "width:1140px;max-width:100%"]) {
    test(`tracker combines centered fixed containers across desktop viewport widths (${sizing})`, async t => {
        const browser = await chromium.launch({headless: true});
        t.after(() => browser.close());
        const context = await browser.newContext({viewport: {width: 1369, height: 800}});
        await context.route("**/*", route => route.fulfill(route.request().url().endsWith("tracker.js")
            ? {contentType: "text/javascript", body: tracker}
            : {contentType: "text/html", body: '<!doctype html><style>body{margin:0}.container{box-sizing:border-box;' + sizing + ';margin:auto;height:200px;padding:12px}section{padding-top:50px}@media(min-width:2000px){.container{max-width:80vw;width:80vw}}</style>'
                + '<section><div class="container">First</div></section><section><div class="container">Second</div></section>'
                + '<script id="wj-heatmap-tracker" src="/tracker.js" data-doc-id="123" data-allow-all="true" data-container-selector=".container"></script>'}));
        const page = await context.newPage();
        await page.goto("https://heatmap.test/page");
        await page.mouse.click(601.5, 350);
        await page.setViewportSize({width: 1223, height: 800});
        await page.mouse.click(528.5, 350);
        const values = (await context.cookies()).filter(cookie => cookie.name.startsWith("wj_hm_")).map(cookie => cookie.value);
        assert.equal(values.length, 2);
        assert.ok(values.some(value => /^v2\.123\.1369\.\d+\.487\.350\.1140$/.test(value)));
        assert.ok(values.some(value => /^v2\.123\.1223\.\d+\.487\.350\.1140$/.test(value)));
        for (const width of [390, 430]) {
            await context.clearCookies();
            await page.setViewportSize({width, height: 800});
            await page.mouse.click(100, 350);
            const cookie = (await context.cookies()).find(item => item.name.startsWith("wj_hm_"));
            assert.match(cookie.value, new RegExp("^v2\\.123\\." + width + "\\.\\d+\\.100\\.350\\.0$"),
                "A fixed width constrained by a fluid maximum must use exact viewport coordinates");
        }
    });
}

test("selector excludes outside clicks and preserves exact coordinates for fluid and offset containers", async t => {
    const browser = await chromium.launch({headless: true});
    t.after(() => browser.close());
    const context = await browser.newContext({viewport: {width: 1223, height: 800}});
    const page = await context.newPage();
    let selector = ".container";
    await page.route("**/*", route => route.fulfill(route.request().url().endsWith("tracker.js")
        ? {contentType: "text/javascript", body: tracker}
        : {contentType: "text/html", body: '<!doctype html><style>body{margin:0}.container{box-sizing:border-box;max-width:1140px;width:100%;margin:auto;height:200px}.inner{width:300px;max-width:300px;margin-left:30px}.overflow{position:absolute;left:0;top:10px;width:20px;height:20px}</style>'
            + '<div class="container"><div class="container inner">Nested</div><span class="overflow">Overflow</span></div>'
            + '<div class="container">Second</div>'
            + '<script id="wj-heatmap-tracker" src="/tracker.js" data-doc-id="123" data-allow-all="true" data-container-selector="' + selector + '"></script>'}));
    await page.goto("https://heatmap.test/page");
    const values = async () => (await context.cookies()).filter(cookie => cookie.name.startsWith("wj_hm_")).map(cookie => cookie.value);
    await page.mouse.click(5, 300);
    await page.mouse.click(5, 15);
    assert.equal((await values()).length, 0, "Outside margins and overflowing descendants are excluded");
    await page.mouse.click(100, 50);
    assert.match((await values())[0], /^v2\.123\.1223\.\d+\.100\.50\.0$/, "Nearest nested container uses exact fallback");
    for (const width of [390, 430]) {
        await page.setViewportSize({width, height: 800});
        await page.mouse.click(100, 250);
        assert.ok((await values()).some(value => new RegExp("^v2\\.123\\." + width + "\\.\\d+\\.100\\.250\\.0$").test(value)));
    }
    await page.setViewportSize({width: 1223, height: 800});
    for (const sizing of ["max-width:none;width:80%", "max-width:none;width:80vw", "max-width:80vw;width:100%"]) {
        await context.clearCookies();
        await page.locator(".container").last().evaluate((container, css) => container.style.cssText = css, sizing);
        await page.mouse.click(200, 250);
        assert.equal((await values()).length, 1);
        assert.match((await values())[0], /^v2\.123\.1223\.\d+\.200\.250\.0$/,
            "Relative widths must retain document coordinates: " + sizing);
    }
    await context.clearCookies();
    selector = ".missing";
    await page.reload();
    await page.mouse.click(100, 250);
    assert.equal((await values()).length, 0);
    selector = "[invalid";
    await page.reload();
    await page.mouse.click(100, 250);
    assert.equal((await values()).length, 0);
});

test("unreadable cross-origin CSS falls back to exact coordinates", async t => {
    const browser = await chromium.launch({headless: true});
    t.after(() => browser.close());
    const context = await browser.newContext({viewport: {width: 1369, height: 800}});
    await context.route("**/*", route => {
        if (route.request().url().endsWith("tracker.js")) return route.fulfill({contentType: "text/javascript", body: tracker});
        if (route.request().url().endsWith("extra.css")) return route.fulfill({contentType: "text/css", body: "body{color:black}"});
        return route.fulfill({contentType: "text/html", body: '<!doctype html><link rel="stylesheet" href="https://styles.test/extra.css">'
            + '<style>body{margin:0}.container{width:100%;max-width:1140px;margin:auto;height:200px}</style><div class="container">Content</div>'
            + '<script id="wj-heatmap-tracker" src="/tracker.js" data-doc-id="123" data-allow-all="true" data-container-selector=".container"></script>'});
    });
    const page = await context.newPage();
    await page.goto("https://heatmap.test/page");
    await page.mouse.click(600, 100);
    const values = (await context.cookies()).filter(cookie => cookie.name.startsWith("wj_hm_")).map(cookie => cookie.value);
    assert.equal(values.length, 1);
    assert.match(values[0], /^v2\.123\.1369\.\d+\.600\.100\.0$/);
});

for (const protocol of ["http", "https"]) {
    test(`tracker records exact click widths and scroll coordinates without sending requests over ${protocol}`, async t => {
        const origin = `${protocol}://heatmap.test`;
        const browser = await chromium.launch({headless: true});
        t.after(() => browser.close());
        const context = await browser.newContext({viewport: {width: 1280, height: 800}, hasTouch: true});
        const requests = [];
        await context.route("**/*", async route => {
            requests.push(route.request().url());
            if (route.request().url().endsWith("tracker.js")) return route.fulfill({contentType: "text/javascript", body: tracker});
            return route.fulfill({contentType: "text/html", headers: {"Content-Security-Policy": "script-src 'self'"}, body:
                '<!doctype html><html><body style="margin:0;height:4000px"><button style="position:absolute;top:1700px;left:50px">autotest target</button>'
                + '<script id="wj-heatmap-tracker" defer src="/tracker.js" data-doc-id="123" data-cookie-name="enableCookieCategory" data-allow-all="false" data-before-consent="false" data-decline-name="decline" data-decline-value="yes"></script>'
                + '<script defer src="/tracker.js"></script></body></html>'});
        });
        const page = await context.newPage();
        await page.goto(`${origin}/page.html`);
        const pending = async () => (await context.cookies()).filter(cookie => cookie.name.startsWith("wj_hm_"));
        await page.mouse.click(100, 100);
        assert.equal((await pending()).length, 0, "No collection before statistical consent");
        await context.addCookies([
            {name: "cookies-gdpr-policy", value: "saved", url: origin},
            {name: "enableCookieCategory", value: "nutne_statisticke_marketingove", url: origin}
        ]);
        const beforeClicks = requests.length;
        await page.mouse.click(100, 150);
        let events = await pending();
        assert.equal(events.length, 1, "Duplicate script inclusion has only one listener");
        assert.match(events[0].name, /^wj_hm_[a-f0-9]{32}$/);
        assert.match(events[0].value, /^v2\.123\.1280\.[0-9]+\.100\.150\.0$/);
        assert.equal(events[0].domain, "heatmap.test");
        assert.equal(events[0].path, "/");
        assert.equal(events[0].sameSite, "Lax");
        assert.equal(events[0].secure, protocol === "https");
        assert.ok(Math.abs(events[0].expires - Date.now() / 1000 - 86400) < 10);
        await page.setViewportSize({width: 390, height: 800});
        await page.evaluate(() => scrollTo(0, 1400));
        await page.touchscreen.tap(70, 310);
        events = await pending();
        assert.equal(events.length, 2);
        assert.ok(events.some(event => /^v2\.123\.390\.[0-9]+\.70\.1710\.0$/.test(event.value)), "Tap retains document coordinates after resize and scroll");
        assert.equal(requests.length, beforeClicks, "Clicks never invoke a collector or beacon");
        await page.evaluate(() => document.dispatchEvent(new MouseEvent("click", {bubbles:true, clientX:20, clientY:20})));
        assert.equal((await pending()).length, 2, "Synthetic clicks are ignored");
        const secondPage = await context.newPage();
        await secondPage.goto(`${origin}/second.html`);
        await Promise.all([page.mouse.click(80, 310), secondPage.mouse.click(90, 90)]);
        events = await pending();
        assert.equal(events.length, 4, "Independent tab events do not overwrite each other");
        assert.equal(new Set(events.map(event => event.name)).size, 4);
        for (let index = 0; index < 22; index++) await secondPage.mouse.click(100 + index, 100);
        events = await pending();
        assert.equal(events.length, 16);
        assert.ok(events.reduce((bytes, event) => bytes + event.name.length + event.value.length + 2, 0) <= 2048);
        await page.evaluate(() => {
            document.cookie = "enableCookieCategory=nutne; Path=/; SameSite=Lax";
            document.dispatchEvent(new Event("webjet:cookie-consent"));
        });
        assert.equal((await pending()).length, 0, "Withdrawal removes queued events immediately");
        await secondPage.mouse.click(100, 100);
        assert.equal((await pending()).length, 0, "Other tabs observe withdrawal before collecting");
    });
}
