Feature('apps.basket.basket-stats');

const STATS_ADMIN = "/apps/basket/admin/stats/";
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

Scenario('E-shop statistics charts and filters @screenshot', async ({ I, Document }) => {
    I.amOnPage(STATS_ADMIN);
    waitForStatistics(I);

    I.say("Check summary widgets and rendered charts");
    SUMMARY_IDS.forEach(id => I.seeElement("#" + id));
    await checkCharts(I);

    I.say("Filter statistics by date range");
    I.fillField("#basketStatsDateFrom", "03.03.2025");
    I.fillField("#basketStatsFilter input.datepicker.max", "10.10.2025");
    I.clickCss("#basketStatsFilter button.dt-filtrujem-dayDate");
    I.waitForInvisible(".webjetAnimatedLoader", 20);
    I.seeInField("#basketStatsDateFrom", "03.03.2025");
    I.seeInField("#basketStatsFilter input.datepicker.max", "10.10.2025");

    if (Document.isScreenshotsEnabled()) {

        Document.screenshot("/redactor/apps/eshop/stats/stats.png", 1920, 2500);

        await Document.switchDomain("shop.tau27.iway.sk");

        I.fillField("#basketStatsDateFrom", "03.03.2025");
        I.fillField("#basketStatsFilter input.datepicker.max", "10.10.2025");
        I.clickCss("#basketStatsFilter button.dt-filtrujem-dayDate");
        I.waitForInvisible(".webjetAnimatedLoader", 20);

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
});

Scenario('Logout after domain change', ({ I }) => {
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
