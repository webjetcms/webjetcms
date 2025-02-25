Feature('apps.inquiry-stat');

Before(({ login }) => {
    login('admin');
});

Scenario('inquiry stat screens', async ({I, DT, DTE, Document}) => {
    I.amOnPage("/apps/inquiry/admin/?id=578");
    DTE.waitForEditor("inquiryDataTable");

    I.clickCss("#pills-dt-inquiryDataTable-votes-tab");
    I.switchTo('#statIframe');

    I.waitForVisible("div.dt-extfilter-dayDate");
    setDates(I, DT, '05.11.2024', '06.11.2024');
    DT.waitForLoader();

    I.switchTo();
    Document.screenshotElement("#inquiryDataTable_modal > div > div.DTE", "/redactor/apps/inquiry/inquiry-editor_stat.png")

    DTE.cancel();

    DT.filterId("id", 578);
    I.clickCss("td.sorting_1")
    Document.screenshotElement("button.buttons-statistics", "/redactor/apps/inquiry/inquiry-stat_button.png")

    I.clickCss("button.buttons-statistics");
    I.waitForVisible("#pills-inquiryStat");

    Document.screenshot("/redactor/apps/inquiry/inquiry-stat_page.png");
    Document.screenshotElement("#pills-inquiryStat", "/redactor/apps/inquiry/inquiry-stat_filter.png");
    Document.screenshotElement("div.dt-extfilter-dayDate > form", "/redactor/apps/inquiry/inquiry-stat_filter_dayDate.png");

    I.clickCss('button[data-id="answerSelect"]');
    I.waitForElement("div.dropdown-menu.show");
    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/inquiry/inquiry-stat_filter_answerSelect.png");

    I.clickCss('button[data-id="userSelect"]');
    I.waitForElement("div.dropdown-menu.show");
    Document.screenshotElement("div.dropdown-menu.show", "/redactor/apps/inquiry/inquiry-stat_filter_userSelect.png");
    I.clickCss('button[data-id="userSelect"]');

    await adjustScrollbar(I, "#inquiryStat-pieVotes");
    Document.screenshotElement("#inquiryStat-pieVotes", "/redactor/apps/inquiry/inquiry-stat_pie_chart.png");

    await adjustScrollbar(I, "#inquiryStat-lineVotes");
    Document.screenshotElement("#inquiryStat-lineVotes", "/redactor/apps/inquiry/inquiry-stat_line_chart.png");

    await adjustScrollbar(I, "#tableDiv");
    Document.screenshotElement("#tableDiv", "/redactor/apps/inquiry/inquiry-stat_table.png");

});

function setDates(I, DT, dateFrom, dateTo) {
    I.fillField("div.dt-extfilter-dayDate > form > div.input-group > input.datepicker.min", dateFrom);
    I.fillField("div.dt-extfilter-dayDate > form > div.input-group > input.datepicker.max", dateTo);
    I.click("div.dt-extfilter-dayDate > form > div.input-group > button.filtrujem");
    DT.waitForLoader();
}

function setPageRowLimit(I, DT, rows) {
    I.click(DT.btn.settings_button);
    I.clickCss('.buttons-page-length');
    I.click(locate('button.btn.button-page-length').find('span').withText(rows));
    I.clickCss('button.btn.btn-primary.dt-close-modal');
}

async function getElementPositionFromTop(I, selector) {
    return await I.executeScript((selector) => {
        const element = document.querySelector(selector);
        if (element) {
            return element.offsetTop;
        } else {
            throw new Error(`Element not found for selector: ${selector}`);
        }
    }, selector);
}

async function adjustScrollbar(I, selector, offsetAdjustment = 0) {
    const yOffset = await getElementPositionFromTop(I, selector);
    const adjustedOffset = yOffset - offsetAdjustment;

    await I.executeScript((adjustedOffset) => {
        const scrollContent = document.querySelector('div.ly-content-wrapper div.scroll-content');
        if (scrollContent) {
            scrollContent.style.transform = `translate3d(0px, -${adjustedOffset}px, 0px)`;
        } else {
            throw Error('Scroll content container not found.');
        }
    }, adjustedOffset);
}