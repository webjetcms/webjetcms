Feature('apps.basket.basket-stats');

const STATS_ADMIN = "/apps/basket/admin/stats/";
const STAT_FILTER_STORAGE_KEY = "webjet.apps.stat.filter";
const STAT_DATE_RANGE_STORAGE_KEY = "webjet.apps.stat.filter.dateRange";
const CHART_IDS = [
    "basketStats-sales",
    "basketStats-products",
    "basketStats-statuses",
    "basketStats-delivery",
    "basketStats-payment",
    "basketStats-categories"
];
const SUMMARY_IDS = [
    "invoiceCount",
    "averageInvoiceValue",
    "soldItemCount",
    "averageItemsPerInvoice",
    "invoiceTotal",
    "deliveryFees",
    "paymentFees",
    "netRevenue"
];

Before(({ login }) => {
    login('admin');
});

Scenario('Date range persists and is shared with visit statistics', async ({ I, DT }) => {
    I.amOnPage("/admin/v9/");
    await clearStoredDateRange(I);
    I.amOnPage(STATS_ADMIN);
    waitForStatistics(I);

    I.say("Save a date range with only the start date");
    I.fillField("#basketStatsDateFrom", "01.01.2026");
    I.clickCss("#basketStatsFilter button.dt-filtrujem-dayDate");
    I.waitForInvisible(".webjetAnimatedLoader", 20);

    I.assertDeepEqual(
        await I.executeScript(storageKey => JSON.parse(window.localStorage.getItem(storageKey)), STAT_DATE_RANGE_STORAGE_KEY),
        {".dt-filter-from-dayDate": "2026-01-01"}
    );

    await I.executeScript(storageKey => {
        const searchCriteria = JSON.parse(window.sessionStorage.getItem(storageKey)) || {};
        searchCriteria[".dt-filter-from-dayDate"] = "03.03.2025";
        searchCriteria[".dt-filter-to-dayDate"] = "10.10.2025";
        window.sessionStorage.setItem(storageKey, JSON.stringify(searchCriteria));
    }, STAT_FILTER_STORAGE_KEY);

    I.say("Restore the date range after a page reload");
    I.refreshPage();
    waitForStatistics(I);
    I.seeInField("#basketStatsDateFrom", "01.01.2026");
    I.seeInField("#basketStatsDateTo", "");

    I.say("Reuse the date range in visit statistics");
    I.amOnPage("/apps/stat/admin/");
    I.waitForInvisible("#loader", 20);
    DT.waitForLoader();
    DT.checkExtfilterDates("01.01.2026", "");

    I.say("Share an updated date range back to e-shop statistics");
    DT.setDates("02.01.2026", "", "#statsDataTable_extfilter");
    DT.waitForLoader();
    I.amOnPage(STATS_ADMIN);
    waitForStatistics(I);
    I.seeInField("#basketStatsDateFrom", "02.01.2026");
    I.seeInField("#basketStatsDateTo", "");

    I.say("Remove the stored date range after clearing both dates");
    I.click("#basketStatsDateFrom");
    I.pressKey(["CommandOrControl", "A"]);
    I.pressKey("Backspace");
    I.seeInField("#basketStatsDateFrom", "");
    I.clickCss("#basketStatsFilter button.dt-filtrujem-dayDate");
    I.waitForInvisible(".webjetAnimatedLoader", 20);
    I.assertEqual(
        await I.executeScript(storageKey => window.localStorage.getItem(storageKey), STAT_DATE_RANGE_STORAGE_KEY),
        null
    );
});

Scenario('E-shop statistics charts and filters @screenshot', async ({ I, Document }) => {
    I.amOnPage("/admin/v9/");
    await clearStoredDateRange(I);
    I.amOnPage(STATS_ADMIN);
    waitForStatistics(I);

    I.say("Check summary widgets and rendered charts");
    SUMMARY_IDS.forEach(id => I.seeElement("#" + id));
    await checkCharts(I);

    I.say("Filter statistics by date range");
    I.fillField("#basketStatsDateFrom", "03.03.2025");
    I.fillField("#basketStatsDateTo", "10.10.2025");
    I.clickCss("#basketStatsFilter button.dt-filtrujem-dayDate");
    I.waitForInvisible(".webjetAnimatedLoader", 20);
    I.seeInField("#basketStatsDateFrom", "03.03.2025");
    I.seeInField("#basketStatsDateTo", "10.10.2025");

    if (Document.isScreenshotsEnabled()) {

        await Document.switchDomain("shop.tau27.iway.sk");
        waitForStatistics(I);

        I.fillField("#basketStatsDateFrom", "03.03.2025");
        I.fillField("#basketStatsDateTo", "10.10.2025");
        I.clickCss("#basketStatsFilter button.dt-filtrujem-dayDate");
        I.waitForInvisible(".webjetAnimatedLoader", 20);

        Document.screenshot("/redactor/apps/eshop/stats/stats.png", 1920, 2500);

        Document.scrollTo("#basketStats-categories");
        Document.screenshotElement(
            "#basketStats-categories",
            "/redactor/apps/eshop/stats/category-tree.png",
            1440,
            700
        );
    }

    I.say("Filter statistics by currency");
    const currency = await I.executeScript(() => {
        const select = document.getElementById("currencySelect");
        const currentValue = select.value;
        const option = Array.from(select.options).find(item => item.value !== currentValue);
        return option == null ? null : option.value;
    });
    I.assertNotEqual(currency, null, "At least two currencies must be configured for statistics filtering");
    await I.executeScript(value => {
        const $select = $("#currencySelect");
        $select.selectpicker("val", value);
        $select.trigger("change");
    }, currency);
    I.waitForInvisible(".webjetAnimatedLoader", 20);
    I.waitForText(currency.toUpperCase(), 20, "#invoiceTotal");
    I.assertEqual(
        await I.executeScript(() => document.getElementById("currencySelect").value),
        currency
    );

    I.say("Filter statistics by invoice status");
    const status = await I.executeScript(() => {
        const option = document.querySelector("#invoiceStatusSelect option");
        return option == null ? null : option.value;
    });
    I.assertNotEqual(status, null, "Invoice status filter must contain options");
    await I.executeScript(value => {
        const $select = $("#invoiceStatusSelect");
        $select.selectpicker("val", [value]);
        $select.trigger("change");
    }, status);
    I.waitForInvisible(".webjetAnimatedLoader", 20);
    I.assertDeepEqual(
        await I.executeScript(() => $("#invoiceStatusSelect").val()),
        [status]
    );

    await checkCharts(I);
    await checkTreeChartLabels(I);
    await checkTreeChartWheelGestures(I);
});

Scenario('Cleanup and logout after domain change @screenshot', async ({ I }) => {
    await clearStoredDateRange(I);
    I.logout();
});

function waitForStatistics(I) {
    I.waitForInvisible(".webjetAnimatedLoader", 20);
    I.waitForVisible(".basket-stats-summary", 20);
    I.waitForElement("#basketStats-sales > div", 20);
}

async function checkCharts(I) {
    const renderedChartIds = await I.executeScript(() => {
        return window.am5.registry.rootElements.map(root => root.dom.id);
    });

    CHART_IDS.forEach(id => {
        I.seeElement("#" + id);
        I.assertContain(renderedChartIds, id, "Chart was not rendered: " + id);
    });
}

async function checkTreeChartWheelGestures(I) {
    const result = await I.executeScript(() => {
        const root = window.am5.registry.rootElements.find(item => item.dom.id === "basketStats-categories");
        const chart = root.container.children.getIndex(0);
        const rect = root.dom.getBoundingClientRect();
        const eventOptions = {
            bubbles: true,
            cancelable: true,
            clientX: rect.left + rect.width / 2,
            clientY: rect.top + rect.height / 2,
            deltaY: -40
        };
        const target = root.dom.querySelector("canvas");
        const initialZoomLevel = chart.contents.get("scale", 1);
        const normalPrevented = target.dispatchEvent(new WheelEvent("wheel", eventOptions)) === false;
        const controlPinchPrevented = target.dispatchEvent(new WheelEvent("wheel", {...eventOptions, ctrlKey: true})) === false;
        const controlTargetZoomLevel = chart._za?.to;
        const commandPinchPrevented = target.dispatchEvent(new WheelEvent("wheel", {...eventOptions, metaKey: true})) === false;

        return {
            initialZoomLevel,
            normalPrevented,
            controlPinchPrevented,
            controlTargetZoomLevel,
            commandPinchPrevented,
            commandTargetZoomLevel: chart._za?.to
        };
    });

    I.assertFalse(result.normalPrevented, "Regular wheel scrolling must remain available for the page");
    I.assertTrue(result.controlPinchPrevented, "Control + trackpad pinch must be handled by the tree chart");
    I.assertAbove(result.controlTargetZoomLevel, result.initialZoomLevel, "Control + trackpad pinch must increase the tree zoom level");
    I.assertTrue(result.commandPinchPrevented, "Command + trackpad pinch must be handled by the tree chart");
    I.assertAbove(result.commandTargetZoomLevel, result.controlTargetZoomLevel, "Command + trackpad pinch must increase the tree zoom level");
}

async function checkTreeChartLabels(I) {
    const result = await I.executeScript(() => {
        const root = window.am5.registry.rootElements.find(item => item.dom.id === "basketStats-categories");
        const chart = root.container.children.getIndex(0);
        const series = chart.contents.children.getIndex(0);

        return {
            nodeRadius: series.circles.template.get("radius"),
            labels: series.labels.values.map(label => ({
                maxWidth: label.get("maxWidth"),
                maxHeight: label.get("maxHeight"),
                oversizedBehavior: label.get("oversizedBehavior")
            }))
        };
    });

    I.assertAbove(result.labels.length, 0, "Tree chart labels must be rendered");
    if (result.nodeRadius > 8) {
        result.labels.forEach(label => {
            I.assertEqual(label.maxWidth, 200, "Sparse tree labels must use the external label width");
            I.assertEqual(label.maxHeight, 20, "Sparse tree labels must remain on one line");
            I.assertEqual(label.oversizedBehavior, "none", "Sparse tree labels must not be truncated");
        });
    } else {
        result.labels.forEach(label => {
            I.assertEqual(label.oversizedBehavior, "truncate", "Compact tree labels must remain bounded");
        });
    }
}

async function clearStoredDateRange(I) {
    await I.executeScript(({filterStorageKey, dateRangeStorageKey}) => {
        window.localStorage.removeItem(dateRangeStorageKey);

        const searchCriteria = JSON.parse(window.sessionStorage.getItem(filterStorageKey));
        if (searchCriteria == null) return;

        delete searchCriteria[".dt-filter-from-dayDate"];
        delete searchCriteria[".dt-filter-to-dayDate"];
        if (Object.keys(searchCriteria).length === 0) window.sessionStorage.removeItem(filterStorageKey);
        else window.sessionStorage.setItem(filterStorageKey, JSON.stringify(searchCriteria));
    }, {
        filterStorageKey: STAT_FILTER_STORAGE_KEY,
        dateRangeStorageKey: STAT_DATE_RANGE_STORAGE_KEY
    });
}
