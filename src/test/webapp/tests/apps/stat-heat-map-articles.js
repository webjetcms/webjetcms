Feature("apps.stat-heat-map-articles");

const groupId = 123864;
const templateName = "Article_heatmap";
const adminUrl = "/apps/stat/admin/heat-map/";
const contentSelector = "article.ly-content";
const targetSelector = ["h1", "h2", "h3", "p", "li", "img", "a[href]", ".hm-demo summary", ".hm-demo button.hm-note-toggle"]
    .map(tag => `${contentSelector} ${tag}`).join(", ");
const widths = [1280, 1366, 1440, 1536, 1920, 2560];
const visits = 5;

Before(({ login }) => {
    login("admin");
});

/** Reads every page in the folder, including rows beyond the first administration table page. */
async function discoverPages(I, DT) {
    I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${groupId}`);
    DT.waitForLoader();
    return I.executeScript(async ({groupId, templateName}) => {
        async function read(url) {
            const response = await fetch(url, {cache: "no-store", headers: {"X-CSRF-Token": window.csrfToken}});
            if (!response.ok) throw new Error(`Cannot discover heatmap pages: HTTP ${response.status} (${url})`);
            return response.json();
        }
        const templates = await read("/admin/rest/templates/temps-list/all");
        const templateIds = templates.content.filter(template => template.tempName === templateName).map(template => template.tempId);
        if (!templateIds.length) throw new Error(`Template ${templateName} was not found.`);
        const pages = [];
        for (let page = 0; ; page++) {
            const data = await read(`/admin/rest/web-pages/all?groupId=${groupId}&recursive=false&size=100&page=${page}`);
            for (const item of data.content) {
                if (item.groupId !== groupId || !item.available || !templateIds.includes(item.tempId)) continue;
                const url = new URL(item.docLink, location.origin);
                if (url.origin !== location.origin) throw new Error(`Page ${item.docId} belongs to another origin: ${url.origin}`);
                url.searchParams.set("NO_WJTOOLBAR", "true");
                pages.push({docId: item.docId, title: item.title, url: url.pathname + url.search});
            }
            if (page + 1 >= data.totalPages) break;
        }
        return pages;
    }, {groupId, templateName});
}

/** Picks distinct weighted targets, with a smaller click count on nearly empty pages. */
function chooseTargets(candidates) {
    const remaining = [...candidates];
    const selected = [];
    const count = 4 + Math.floor(Math.random() * 5);
    while (remaining.length && selected.length < count) {
        let ticket = Math.random() * remaining.reduce((sum, target) => sum + target.weight, 0);
        const index = remaining.findIndex(target => (ticket -= target.weight) < 0);
        const [target] = remaining.splice(index < 0 ? remaining.length - 1 : index, 1);
        selected.push(target);
        // A reader may finish a visit by following one link to another article.
        if (target.navigates) {
            for (let i = remaining.length - 1; i >= 0; i--) if (remaining[i].navigates) remaining.splice(i, 1);
        }
    }
    return selected.sort((a, b) => Number(a.navigates) - Number(b.navigates) || a.top - b.top);
}

/** Mixes central clusters with wider scatter so repeated visits do not produce identical dots. */
function randomPosition(start, size) {
    const fraction = Math.random() < 0.75 ? (Math.random() + Math.random()) / 2 : Math.random();
    return start + 3 + fraction * (size - 6);
}

/** Collects visible article targets whose clicks remain inside the configured tracking container. */
async function collectTargets(I, pages) {
    return I.executeScript(({selector, pages}) => {
        const containerSelector = document.getElementById("wj-heatmap-tracker").dataset.containerSelector;
        const elements = [...document.querySelectorAll(selector)];
        const articlePaths = new Set(pages.map(page => new URL(page.url, location.origin).pathname));
        return elements.flatMap(element => {
            const rect = element.getBoundingClientRect();
            const style = getComputedStyle(element);
            if (rect.width < 12 || rect.height < 12 || style.visibility !== "visible") return [];
            if (element.closest(".collapse:not(.show)") || (element.tagName !== "SUMMARY" && element.closest("details:not([open])"))) return [];
            if (element.tagName === "IMG" && (!element.complete || !element.naturalWidth)) return [];
            // Exclude form controls, banner redirects, downloads, lightboxes and scripted links.
            const noteToggle = element.matches('button[type="button"][data-bs-toggle="collapse"].hm-note-toggle')
                && document.getElementById(element.getAttribute("aria-controls"))?.matches(".hm-demo .collapse");
            if (element.closest("form, [onclick], [role=button]") || (element.closest("button") && !noteToggle)) return [];
            const link = element.closest("a");
            let anchorLink = false;
            if (link) {
                const url = new URL(link.href, location.href);
                anchorLink = link.getAttribute("href").startsWith("#") && url.hash.length > 1
                    && document.getElementById(decodeURIComponent(url.hash.substring(1))) !== null;
                if (url.origin !== location.origin || link.hasAttribute("download") || link.target === "_blank"
                        || (link.getAttribute("href").startsWith("#") && !anchorLink)
                        || !articlePaths.has(url.pathname)) return [];
            }
            // Give linked images their own target, without also selecting their wrapping link or paragraph.
            if (element.tagName !== "IMG" && element.querySelector("img, a, button, input")) return [];
            if (element.tagName !== "IMG" && !element.textContent.trim()) return [];
            let anchor = rect;
            if (element.tagName !== "IMG") {
                const range = document.createRange();
                range.selectNodeContents(element);
                const lines = [...range.getClientRects()].filter(part => part.width >= 12 && part.height >= 12);
                if (lines.length) anchor = lines[Math.floor(Math.random() * lines.length)];
            }
            const container = containerSelector ? element.closest(containerSelector) : document.documentElement;
            if (!container) return [];
            const bounds = container.getBoundingClientRect();
            const left = Math.max(anchor.left, bounds.left, 0);
            const right = Math.min(anchor.right, bounds.right, document.documentElement.clientWidth);
            const top = Math.max(anchor.top, bounds.top);
            const bottom = Math.min(anchor.bottom, bounds.bottom);
            if (right - left < 12 || bottom - top < 12) return [];
            let weight = 1;
            if (/^H[1-3]$/.test(element.tagName)) weight = 3;
            if (element.tagName === "IMG") weight = 7;
            if (link) weight = 9;
            if (element.tagName === "SUMMARY" || noteToggle || link?.classList.contains("hm-button")) weight = 12;
            // Captions and clipped corners of rounded images belong to the wrapping link or paragraph.
            const imageParagraph = element.tagName === "IMG" ? element.closest("p") : null;
            const clickElement = link || imageParagraph || element;
            const clickRect = clickElement.getBoundingClientRect();
            const clickStyle = getComputedStyle(clickElement);
            return [{
                index: elements.indexOf(clickElement), top: top + scrollY, weight,
                label: (element.getAttribute("alt") || element.textContent.trim() || "image").slice(0, 70),
                x: left - clickRect.left - parseFloat(clickStyle.borderLeftWidth),
                y: top - clickRect.top - parseFloat(clickStyle.borderTopWidth),
                width: right - left, height: bottom - top,
                containerWidth: containerSelector ? Math.round(bounds.width) : 0,
                navigates: Boolean(link) && !anchorLink, disclosure: element.tagName === "SUMMARY" || noteToggle
            }];
        });
    }, {selector: targetSelector, pages});
}

/** Checks the native or Bootstrap disclosure after its layout has settled. Runs in the browser. */
function disclosureHasState([selector, index, open]) {
    const trigger = document.querySelectorAll(selector)[index];
    if (trigger.tagName === "SUMMARY") return trigger.parentElement.open === open;
    const panel = document.getElementById(trigger.getAttribute("aria-controls"));
    return !panel.classList.contains("collapsing") && panel.classList.contains("show") === open;
}

/** Waits for tracker and layout resources, which cannot be checked with a visible-text assertion. */
async function waitForArticle(I, docId, width) {
    I.waitForVisible(contentSelector, 15);
    I.waitForElement(`#wj-heatmap-tracker[data-doc-id="${docId}"]`, 15);
    I.waitForFunction(() => window.webjetHeatMapTracker && document.fonts.status === "loaded", 15);
    const state = await I.executeScript(() => ({width: innerWidth, consent: window.webjetHeatMapTracker.checkConsent()}));
    I.assertEqual(state.width, width, `Unexpected viewport width on page ${docId}.`);
    I.assertTrue(state.consent, `Statistics consent must remain enabled on page ${docId}.`);
    I.waitForFunction(([contentSelector]) => [...document.querySelectorAll(`${contentSelector} img`)]
        .filter(image => image.loading !== "lazy" && image.getClientRects().length > 0).every(image => image.complete), [contentSelector], 30);
}

Scenario("generates article heatmap clicks across viewport widths @current", async ({ I, DT }) => {
    const pages = await discoverPages(I, DT);
    I.assertAbove(pages.length, 0, `Folder ${groupId} must contain published ${templateName} pages.`);
    const now = Date.now();
    const dateRange = `daterange:${now - 86400000}-${now + 86400000}`;

    for (const width of widths) {
        I.amOnPage(adminUrl);
        const baseline = await I.executeScript(async ({pages, dateRange}) => {
            return Promise.all(pages.map(async page => {
                const api = `/admin/rest/stat/heat-map/widths?docId=${page.docId}&dateRange=${dateRange}`;
                const response = await fetch(api, {cache: "no-store", headers: {"X-CSRF-Token": window.csrfToken}});
                if (!response.ok) throw new Error(`Cannot read heatmap baseline: HTTP ${response.status}`);
                const groups = await response.json();
                return {docId: page.docId, api, groups};
            }));
        }, {pages, dateRange});

        I.amOnPage(pages[0].url);
        I.waitForElement("#wj-heatmap-tracker", 15);
        const tracking = await I.executeScript(() => ({...document.getElementById("wj-heatmap-tracker").dataset, origin: location.origin}));
        await I.setCookie({name: tracking.cookieName, value: "nutne_statisticke", url: tracking.origin});
        await I.setCookie({name: "wjabtesting", value: "a", url: tracking.origin});
        // Expire only this cookie: the installed helper's clearCookie() clears the whole cookie jar.
        if (tracking.declineName) await I.setCookie({name: tracking.declineName, value: "", expires: 1, url: tracking.origin});
        I.logout();
        I.resizeWindow(width, 900);

        for (const page of pages) {
            let clicks = 0;
            const containerWidths = new Set();
            for (let visit = 1; visit <= visits; visit++) {
                I.amOnPage(page.url);
                await waitForArticle(I, page.docId, width);
                const candidates = await collectTargets(I, pages);
                I.assertAbove(candidates.length, 0, `No trackable article targets on ${page.url} at ${width}px.`);
                const selected = chooseTargets(candidates);
                I.say(`autotest heatmap: ${page.docId}, ${width}px, visit ${visit}/${visits}, ${selected.length} clicks`);
                for (const target of selected) {
                    containerWidths.add(target.containerWidth);
                    const position = {x: randomPosition(target.x, target.width), y: randomPosition(target.y, target.height)};
                    I.say(`autotest click: ${target.label}, offset ${position.x.toFixed(1)}/${position.y.toFixed(1)}`);
                    // Center the target below the sticky header; instant scrolling avoids the template's smooth-scroll animation.
                    I.executeScript(({selector, index}) => document.querySelectorAll(selector)[index]
                        .scrollIntoView({block: "center", behavior: "instant"}), {selector: targetSelector, index: target.index});
                    // A normal click scrolls into view and produces the trusted mouse event required by the tracker.
                    I.click(locate(targetSelector).at(target.index + 1), null, {position});
                    clicks++;
                    if (target.disclosure) {
                        // Opening and closing a reading note creates two genuine clicks and restores the article layout.
                        I.waitForFunction(disclosureHasState, [targetSelector, target.index, true], 5);
                        I.click(locate(targetSelector).at(target.index + 1), null, {position});
                        clicks++;
                        I.waitForFunction(disclosureHasState, [targetSelector, target.index, false], 5);
                    }
                }
                // Following a link sends the pending cookies; a visit ending on the same article needs a reload.
                if (!selected.some(target => target.navigates)) I.refreshPage();
                await I.waitForFunction(() => !document.cookie.split(";").some(cookie => cookie.trim().startsWith("wj_hm_")), 10);
            }
            Object.assign(baseline.find(item => item.docId === page.docId), {expected: clicks, width, containerWidths: [...containerWidths]});
            console.log(`autotest heatmap generated: ${page.docId}, ${width}px, ${visits} visits, ${clicks} clicks`);
        }

        I.relogin("admin");
        I.amOnPage(adminUrl);
        // This Playwright version treats an async predicate as truthy before it resolves. Poll synchronous state.
        I.waitForFunction(baseline => {
            const state = window.autotestArticleHeatMapPoll || (window.autotestArticleHeatMapPoll = {nextCheck: 0, pending: false});
            if (state.error) throw new Error(state.error);
            if (state.results?.every(item => item.added >= item.expected)) return true;
            if (state.pending || Date.now() < state.nextCheck) return false;
            state.nextCheck = Date.now() + 2000;
            state.pending = true;
            Promise.all(baseline.map(async item => {
                const response = await fetch(item.api, {cache: "no-store", headers: {"X-CSRF-Token": window.csrfToken}});
                if (!response.ok) throw new Error(`Cannot verify heatmap for ${item.docId}: HTTP ${response.status}`);
                const groups = await response.json();
                const relevantClicks = rows => rows.filter(group => group.mode === "CONTAINER" ? item.containerWidths.includes(group.width)
                    : group.width === item.width).reduce((sum, group) => sum + group.clicks, 0);
                return {docId: item.docId, added: relevantClicks(groups) - relevantClicks(item.groups), expected: item.expected, groups};
            })).then(results => { state.results = results; })
                .catch(error => { state.error = error.message; })
                .finally(() => { state.pending = false; });
            return false;
        }, baseline, 90);
        const results = await I.executeScript(() => window.autotestArticleHeatMapPoll.results);
        for (const result of results) {
            I.assertAbove(result.added, result.expected - 1, `Missing stored clicks for ${result.docId} at ${width}px.`);
            console.log(`autotest heatmap stored: ${result.docId}, ${width}px, added ${result.added}, expected ${result.expected}; groups: `
                + result.groups.map(group => `${group.mode}:${group.width} (${group.clicks})`).join(", "));
        }
        await I.setCookie({name: tracking.cookieName, value: "", expires: 1, url: tracking.origin});
        await I.setCookie({name: "wjabtesting", value: "", expires: 1, url: tracking.origin});
        I.say(`autotest heatmap: ${width}px complete; generated data intentionally retained`);
    }
});
