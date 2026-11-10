Feature("screenshots.generator.stat-heat-map");

const listUrl = "/apps/stat/admin/heat-map/";
const modalFrame = "#modalIframeIframeElement";
const imagePath = "/redactor/apps/stat/heat-map-";
const pages = {overview: 182083, packages: 182084, planner: 182085};
const dateRange = "daterange:" + new Date(2026, 10, 1).getTime() + "-" + new Date(2026, 10, 10, 23, 59, 59).getTime();

Before(({ I, login }) => {
    login("admin");
    I.resizeWindow(1440, 1000);
});

/** Opens the real report and limits the list to the prepared Heatmap folder's URLs. */
function openList(I, DT) {
    I.amOnPageAsync(listUrl + "?dateRange=" + encodeURIComponent(dateRange));
    I.waitForVisible("#heatMapDataTable", 20);
    DT.waitForLoader("heatMapDataTable");
    DT.filterContains("url", "/heatmap/", "#heatMapDataTable_wrapper");
    for (const docId of Object.values(pages)) {
        I.waitForVisible(`#heatMapDataTable tbody a[href*="docId=${docId}&"]`, 20);
    }
}

/** Waits for real preview resources and heatmap tiles, not just the iframe element. */
function waitForPreview(I, prefixes = ["heatMap"]) {
    I.waitForElement('#heatMapViewer[aria-busy="false"]', 30);
    for (const prefix of prefixes) I.waitForVisible(`#${prefix}Viewport`, 30);
    // Resource readiness needs browser state; CodeceptJS waitForFunction only works in the top page.
    I.switchTo();
    I.waitForFunction(prefixes => {
        const modal = document.getElementById("modalIframeIframeElement").contentDocument;
        return prefixes.every(prefix => {
            const frame = modal.getElementById(prefix + "Frame");
            const doc = frame.contentDocument;
            const tiles = [...modal.querySelectorAll("#" + prefix + "Tiles img")];
            return doc?.readyState === "complete" && doc.fonts.status === "loaded"
                && [...doc.images].filter(image => image.loading !== "lazy").every(image => image.complete && image.naturalWidth > 0)
                && tiles.length > 0 && tiles.every(tile => tile.complete && tile.naturalWidth > 0);
        });
    }, prefixes, 30);
    I.switchTo(modalFrame);
    // Moving away from Reload lets the overlapping notice finish its hover-paused timeout.
    I.moveCursorTo("#heatMapScaleSummary");
    I.waitToHide("#toast-container-webjet .toast", 10);
}

/** Opens a page through the same modal link used by an administrator. */
function openPreview(I, DT, docId) {
    openList(I, DT);
    I.clickCss(`#heatMapDataTable tbody a[href*="docId=${docId}&"]`);
    I.waitForVisible("#modalIframe.show", 10);
    I.switchTo(modalFrame);
    waitForPreview(I);
    I.waitForVisible("#heatMapHotspotList button", 20);
}

/** Captures the modal, including its title and controls, without the dimmed page behind it. */
function capturePreview(I, Document, name) {
    I.clickCss("#heatMapScaleSummary");
    I.waitToHide(".tooltip", 10);
    I.switchTo();
    I.clickCss("#modalIframe .modal-title");
    I.waitToHide(".tooltip", 10);
    Document.screenshotElement("#modalIframe .modal-content", imagePath + name + ".png");
    I.switchTo(modalFrame);
}

/** Selects by label because CodeceptJS first searches option text even when given a value. */
async function selectPreviewOption(I, selector, value) {
    const [label] = await I.grabTextFromAll(`${selector} option[value="${value}"]`);
    await I.selectOption(selector, label);
}

/** Scrolls inside a preview while leaving the administration controls in view. */
function scrollPreview(I) {
    I.switchTo("#heatMapFrame");
    I.clickCss("h1");
    I.pressKey("PageDown");
    I.switchTo();
    // Keyboard scrolling is asynchronous; waitForFunction needs the top page in this CodeceptJS version.
    I.waitForFunction(() => {
        const modal = document.getElementById("modalIframeIframeElement").contentDocument;
        return modal.getElementById("heatMapFrame").contentWindow.scrollY > 0;
    }, 10);
    I.switchTo(modalFrame);
    waitForPreview(I);
}

Scenario("heatmap documentation - tracking setup @heatmap-doc @screenshot", ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.waitForLoader();
    DT.filterEquals("tempName", "Article_heatmap");
    I.click("Article_heatmap", "#datatableInit tbody");
    DTE.waitForEditor();
    I.seeInField("#DTE_Field_statisticsMode", "2");
    Document.screenshotElement(".DTE.modal-content", imagePath + "tracking.png", null, null, ".DTE_Field_Name_statisticsMode");
    DTE.cancel();
});

Scenario("heatmap documentation - list @heatmap-doc @screenshot", async ({ I, DT, Document }) => {
    I.resizeWindow(1440, 620);
    openList(I, DT);
    I.assertEqual(await I.grabNumberOfVisibleElements("#heatMapDataTable tbody a"), 3);
    Document.screenshot(imagePath + "list.png");
});

Scenario("heatmap documentation - preview controls @heatmap-doc @screenshot", async ({ I, DT, Document }) => {
    openPreview(I, DT, pages.overview);
    capturePreview(I, Document, "preview");

    await selectPreviewOption(I, "#heatMapWidth", "container:1760");
    waitForPreview(I);
    I.seeInField("#heatMapWidth", "container:1760");
    capturePreview(I, Document, "width");

    await selectPreviewOption(I, "#heatMapWidth", "container:1140");
    waitForPreview(I);
    await selectPreviewOption(I, "#heatMapScale", "0.5");
    I.see("50 %", "#heatMapScaleSummary");
    capturePreview(I, Document, "scale");

    await selectPreviewOption(I, "#heatMapScale", "fit");
    scrollPreview(I);
    capturePreview(I, Document, "scroll");

    I.clickCss("#heatMapHotspotList li:first-child button");
    I.waitForVisible("#heatMapHotspotResult", 10);
    waitForPreview(I);
    I.clickCss("#heatMapOpacity");
    I.pressKey("Home");
    for (let step = 0; step < 5; step++) I.pressKey("ArrowRight");
    I.see("25 %", "#heatMapOpacityValue");
    capturePreview(I, Document, "opacity");

    I.clickCss("#heatMapVisible");
    I.seeAttributesOnElements("#heatMapVisible", {"aria-pressed": "false"});
    I.dontSeeElement("#heatMapOverlay");
    capturePreview(I, Document, "hidden");
    I.clickCss("#heatMapVisible");
    I.clickCss("#heatMapOpacity");
    I.pressKey("End");
    for (let step = 0; step < 7; step++) I.pressKey("ArrowLeft");
    I.see("65 %", "#heatMapOpacityValue");

    I.clickCss("#heatMapReload");
    waitForPreview(I);
    I.seeAttributesOnElements("#heatMapVisible", {"aria-pressed": "true"});
    Document.highlightElement("#heatMapReload");
    capturePreview(I, Document, "reload");
    Document.unhighlightElement("#heatMapReload");

    I.switchTo();
    I.clickCss("#modalIframe .dialog-buttons .maximize");
    I.waitForVisible("#modalIframe .modal-dialog.modal-fullscreen", 10);
    I.switchTo(modalFrame);
    waitForPreview(I);
    capturePreview(I, Document, "fullscreen");
});

Scenario("heatmap documentation - hotspots and area selection @heatmap-doc @screenshot", async ({ I, DT, Document }) => {
    openPreview(I, DT, pages.packages);
    I.clickCss("#heatMapHotspotList li:first-child button");
    I.waitForVisible("#heatMapHotspotBox", 10);
    I.waitForVisible("#heatMapHotspotResult", 10);
    waitForPreview(I);
    capturePreview(I, Document, "hotspots");

    I.clickCss("#heatMapHotspots");
    I.dontSeeElement("#heatMapHotspotPanel");
    capturePreview(I, Document, "hotspots-hidden");
    I.clickCss("#heatMapHotspots");
    I.waitForVisible("#heatMapHotspotList button", 10);
    I.clickCss("#heatMapHotspotList li:first-child button");
    I.waitForVisible("#heatMapHotspotBox", 10);
    const hotspot = await I.grabElementBoundingRect("#heatMapHotspotBox");
    I.clickCss("#heatMapSelectArea");
    I.waitForVisible("#heatMapAreaLayer", 10);
    // Use the visible hotspot bounds so the selected area demonstrates a nonzero real count.
    const layerBounds = await I.grabElementBoundingRect("#heatMapAreaLayer");
    const positions = {
        sourcePosition: {
            x: Math.max(10, hotspot.x - layerBounds.x - 45),
            y: Math.max(85, hotspot.y - layerBounds.y - 45)
        },
        targetPosition: {
            x: Math.min(layerBounds.width - 10, hotspot.x + hotspot.width - layerBounds.x + 45),
            y: Math.min(layerBounds.height - 10, hotspot.y + hotspot.height - layerBounds.y + 45)
        }
    };
    // CodeceptJS dragAndDrop targets the top page; this layer is inside the modal iframe.
    I.usePlaywrightTo("select an area inside the heatmap modal", async ({page}) => {
        const layer = page.frameLocator(modalFrame).locator("#heatMapAreaLayer");
        await layer.dragTo(layer, positions);
    });
    I.waitForText("Oblasť:", 15, "#heatMapAreaCount");
    const [areaCount] = await I.grabTextFromAll("#heatMapAreaCount");
    I.assertAbove(Number(areaCount.match(/\d+/)?.[0]), 0, "The selected area must contain recorded clicks.");
    capturePreview(I, Document, "area");
    I.clickCss("#heatMapClearArea");
    I.dontSeeElement("#heatMapAreaResult");
    I.dontSeeElement("#heatMapAreaBox");
    capturePreview(I, Document, "area-cleared");
});

Scenario("heatmap documentation - versions and comparison @heatmap-doc @screenshot", async ({ I, DT, Document }) => {
    openPreview(I, DT, pages.planner);
    const versions = await I.grabAttributeFromAll("#heatMapVersion option", "value");
    const historyIds = versions.filter(Boolean);
    I.assertAbove(historyIds.length, 1, "The planner needs two published history versions for the documentation.");
    await selectPreviewOption(I, "#heatMapVersion", historyIds[0]);
    waitForPreview(I);
    I.seeElement(`#heatMapFrame[src*="versionId=${historyIds[0]}"]`);
    capturePreview(I, Document, "version");

    I.clickCss("#heatMapCompare");
    waitForPreview(I, ["heatMap", "heatMapCompare"]);
    I.seeAttributesOnElements("#heatMapCompare", {"aria-pressed": "true"});
    I.assertNotEqual(await I.grabValueFrom("#heatMapVersion"), await I.grabValueFrom("#heatMapCompareVersion"));
    capturePreview(I, Document, "comparison");

    scrollPreview(I);
    I.switchTo("#heatMapCompareFrame");
    const comparisonScroll = await I.grabPageScrollPosition();
    I.assertEqual(comparisonScroll.y, 0);
    I.switchTo();
    I.switchTo(modalFrame);
    capturePreview(I, Document, "comparison-scroll");
    I.clickCss("#heatMapCompare");
    waitForPreview(I);
    I.dontSeeElement("#heatMapComparePane");
    I.seeInField("#heatMapVersion", historyIds[0]);
    capturePreview(I, Document, "comparison-closed");
});
