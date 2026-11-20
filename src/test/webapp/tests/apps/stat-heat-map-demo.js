Feature("apps.stat-heat-map-demo");

const pageUrl = "/investicie/?NO_WJTOOLBAR=true";
const adminUrl = "/apps/stat/admin/heat-map/";
const targetSelector = '.container h1, .container h2, .container h3, .container a.btn[href="#"], .container img';

Before(({ login }) => {
    login("admin");
});

/** Picks distinct targets using demo weights, without forcing any particular target into every run. */
function chooseTargets(candidates) {
    const remaining = [...candidates];
    const selected = [];
    const count = 3 + Math.floor(Math.random() * 2);
    for (let i = 0; i < count; i++) {
        let ticket = Math.random() * remaining.reduce((sum, target) => sum + target.weight, 0);
        const index = remaining.findIndex(target => (ticket -= target.weight) < 0);
        selected.push(remaining.splice(index < 0 ? remaining.length - 1 : index, 1)[0]);
    }
    return selected.sort((a, b) => a.top - b.top);
}

/** Concentrates clicks near the target anchor while keeping the maximum deviation at six CSS pixels. */
function jitter(coordinate, size) {
    return Math.max(2, Math.min(size - 2, coordinate + (Math.random() + Math.random() - 1) * 6));
}

/** Favors desktop traffic while still sampling tablet and mobile layouts. */
function viewportWidth() {
    if (process.env.CODECEPT_HEATMAP_WIDTH) {
        const width = Number(process.env.CODECEPT_HEATMAP_WIDTH);
        if (!Number.isInteger(width) || width < 360 || width > 1600) {
            throw new Error("CODECEPT_HEATMAP_WIDTH must be an integer between 360 and 1600.");
        }
        return width;
    }
    const chance = Math.random();
    const [min, max] = chance < 0.60 ? [1200, 1600]
        : chance < 0.75 ? [992, 1199]
        : chance < 0.85 ? [768, 991]
        : chance < 0.95 ? [576, 767]
        : [360, 430];
    return min + Math.floor(Math.random() * (max - min + 1));
}

Scenario("adds realistic demo clicks on investment page @heatmap-demo", async ({ I }) => {
    // Inspect the enabled tracker rather than injecting one or changing shared CMS configuration.
    I.amOnPage(pageUrl);
    I.waitForElement("#wj-heatmap-tracker", 10);
    const tracking = await I.executeScript(() => ({
        ...document.getElementById("wj-heatmap-tracker").dataset,
        origin: location.origin
    }));
    I.assertAbove(Number(tracking.docId), 0, "The investment page must have heatmap tracking enabled.");
    await I.setCookie({name: tracking.cookieName, value: "nutne_statisticke", url: tracking.origin});
    // Keep the original page variant so accumulated clicks share the same content and vertical positions.
    await I.setCookie({name: "wjabtesting", value: "a", url: tracking.origin});
    // This CodeceptJS version clears every cookie in clearCookie(), so expire only the consent cookie.
    if (tracking.declineName) await I.setCookie({name: tracking.declineName, value: "", expires: 1, url: tracking.origin});

    const now = Date.now();
    const api = "/admin/rest/stat/heat-map/widths?docId=" + tracking.docId
        + "&dateRange=daterange:" + (now - 86400000) + "-" + (now + 86400000);
    I.amOnPage(adminUrl);
    const before = await I.executeScript(async url => {
        const response = await fetch(url, {cache: "no-store", headers: {"X-CSRF-Token": window.csrfToken}});
        if (!response.ok) throw new Error("Cannot read heatmap baseline: HTTP " + response.status);
        return (await response.json()).reduce((sum, group) => sum + group.clicks, 0);
    }, api);

    I.logout();
    const width = viewportWidth();
    I.resizeWindow(width, 900);
    I.amOnPage(pageUrl);
    I.waitForFunction(() => window.webjetHeatMapTracker && document.fonts.status === "loaded"
        && [...document.querySelectorAll(".container img")].every(image => image.complete), 15);
    I.assertEqual(await I.grabAttributeFrom("#wj-heatmap-tracker", "data-doc-id"), tracking.docId,
        "Anonymous visitors must remain on the original page variant.");

    const candidates = await I.executeScript(selector => {
        const typeWeights = {A: 8, H1: 6, H2: 3, H3: 2, IMG: 1};
        return [...document.querySelectorAll(selector)].flatMap((element, index) => {
            const rect = element.getBoundingClientRect();
            const style = getComputedStyle(element);
            const container = element.closest(".container").getBoundingClientRect();
            if (rect.width < 16 || rect.height < 16 || style.visibility !== "visible"
                    || (element.tagName === "IMG" && element.closest("a, button, [onclick]"))) return [];
            let anchor = rect;
            if (/^H[1-3]$/.test(element.tagName)) {
                const range = document.createRange();
                range.selectNodeContents(element);
                anchor = [...range.getClientRects()].find(part => part.width > 0 && part.height > 0) || rect;
            }
            const x = anchor.left + anchor.width / 2;
            const y = anchor.top + anchor.height / 2;
            if (x < container.left || x >= container.right || y < container.top || y >= container.bottom) return [];
            const top = y + scrollY;
            const relativeX = Math.max(0, Math.min(1, (x - container.left) / container.width));
            // These are illustrative demo weights, not measurements of real visitor behavior.
            const weight = typeWeights[element.tagName] * (1.5 - relativeX) / Math.pow(1 + top / innerHeight, 2);
            const borderX = parseFloat(style.borderLeftWidth);
            const borderY = parseFloat(style.borderTopWidth);
            return [{
                index, top, weight,
                label: (element.textContent.trim() || element.getAttribute("alt") || element.tagName).slice(0, 80),
                x: x - rect.left - borderX, y: y - rect.top - borderY,
                width: rect.width - borderX - parseFloat(style.borderRightWidth),
                height: rect.height - borderY - parseFloat(style.borderBottomWidth)
            }];
        });
    }, targetSelector);
    I.assertAbove(candidates.length, 4, "The demo needs a larger target pool than the 3–4 selected places.");
    const selected = chooseTargets(candidates);
    I.say("autotest heatmap demo: viewport " + width + ", " + selected.length + " of " + candidates.length + " targets");
    for (const [index, target] of selected.entries()) {
        const position = {x: jitter(target.x, target.width), y: jitter(target.y, target.height)};
        I.say("autotest click: " + target.label + ", weight " + target.weight.toFixed(2)
            + ", offset " + position.x.toFixed(1) + "/" + position.y.toFixed(1));
        // The standard click uses a trusted browser mouse event and scrolls the target into view.
        I.click(locate(targetSelector).at(target.index + 1), null, {position});
        I.waitForFunction(([docId, count]) => document.cookie.split(";").filter(cookie => {
            const [name, value] = cookie.trim().split("=");
            return name.startsWith("wj_hm_") && value?.split(".")[1] === docId;
        }).length === count, [tracking.docId, index + 1], 5);
    }

    // An ordinary page request delivers the real tracker cookies to the server.
    I.refreshPage();
    I.waitForFunction(() => !document.cookie.split(";").some(cookie => cookie.trim().startsWith("wj_hm_")), 10);
    I.relogin("admin");
    I.amOnPage(adminUrl);
    // StatWriteBuffer normally flushes once a minute. Poll the real API at most once every two seconds.
    I.waitForFunction(async ([url, expected]) => {
        const state = window.autotestHeatMapDemoPoll || (window.autotestHeatMapDemoPoll = {nextCheck: 0});
        if (Date.now() < state.nextCheck) return false;
        state.nextCheck = Date.now() + 2000;
        const response = await fetch(url, {cache: "no-store", headers: {"X-CSRF-Token": window.csrfToken}});
        if (!response.ok) throw new Error("Cannot verify stored heatmap clicks: HTTP " + response.status);
        state.count = (await response.json()).reduce((sum, group) => sum + group.clicks, 0);
        return state.count >= expected;
    }, [api, before + selected.length], 90);
    I.say("autotest heatmap demo: " + selected.length + " clicks recorded; demo data intentionally retained");
    await I.setCookie({name: tracking.cookieName, value: "", expires: 1, url: tracking.origin});
    await I.setCookie({name: "wjabtesting", value: "", expires: 1, url: tracking.origin});
});
