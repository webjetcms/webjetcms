Feature('apps.inquiry.inquiry-stat');

var randomNumber;
const TOLERANCE = 5;

Before(({ I, DT }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }

    DT.addContext('inquiry', '#inquiryDataTable_wrapper');
    DT.addContext('inquiryStat', '#inquiryStatDataTable_wrapper');
});

const answers = [
    'Programovanie',
    'Analýza dát a vizualizácia',
    'Správa sietí a bezpečnosť',
    'Vývoj webových aplikácií',
    'AI a strojové učenie'
];

const users = [
    'Tester Playwright',
    'Tester2 Playwright2',
    'Janko Tester',
    'Tester_L2 Playwright',
    'Ferko Blogger',
    'Tester_Forum Playwright',
    'Permission Test',
    'Tester4 Playwright'
];

Scenario('inquiry - statistics', async ({ I, DT, DTE }) => {
    I.relogin('admin');

    I.resizeWindow(1280, 2000);

    const inquiryQuestion = 'Ktoré oblasti '; //Ktoré oblasti IT vás najviac zaujímajú? - IT is <strong>
    I.amOnPage('/apps/inquiry/admin/');
    DT.waitForLoader();
    DT.filterContains('questionText', inquiryQuestion);
    I.clickCss('.buttons-select-all');
    I.click(DT.btn.inquiry_edit_button);
    DTE.waitForModal('inquiryDataTable_modal');
    I.clickCss('#pills-dt-inquiryDataTable-votes-tab');
    I.switchTo('#statIframe');

    I.say("Check we dont see whole question and nav links, because we are in tab");
    I.dontSee('Ktoré oblasti IT vás najviac zaujímajú?');
    I.dontSeeElement( locate("#pills-inquiryStat").find("a.nav-link") );
    setDates(I, DT, '01.09.2024', '30.11.2024');
    checkSelects(I);

    setDates(I, DT, '01.11.2024', '15.11.2024');

    verifyInquiryStatisticsCharts(I);
});

Scenario('inquiry - statistics A1', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'A', 1);
});

Scenario('inquiry - statistics A2', async ({ I, DT,  Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'A', 2);
});

Scenario('inquiry - statistics A3', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'A', 3);
});

Scenario('inquiry - statistics A4', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'A', 4);
});

Scenario('inquiry - statistics A', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'A', -1);
});

Scenario('inquiry - statistics B init', async ({ I, DTE, DT }) => {
    I.switchTo();
    DTE.cancel();

    I.click(DT.btn.inquiry_stat_button);
    I.waitForVisible( locate("#pills-inquiryStat").find("a.nav-link") );
    I.dontSeeElement('#inquiryDataTable_modal');

    I.say("Check that question is visible on page and is generated correctly with tags");
    const question = "<p>Ktoré oblasti <strong>IT</strong> vás najviac zaujímajú?</p>";
    const questionFromPage = await I.grabHTMLFrom('#questionText');
    I.assertEqual(question, questionFromPage, "Question in page with html tags do not match with original.");
    setDates(I, DT, '01.09.2024', '30.11.2024');
    checkSelects(I);

    setDates(I, DT, '01.11.2024', '15.11.2024');

    verifyInquiryStatisticsCharts(I);
});

Scenario('inquiry - statistics B1', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'B', 1);
});

Scenario('inquiry - statistics B2', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'B', 2);
});

Scenario('inquiry - statistics B3', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'B', 3);
});

Scenario('inquiry - statistics B4', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'B', 4);
});

Scenario('inquiry - statistics B', async ({ I, DT, Document }) => {
    await validateFilteringBehavior(I, DT, Document, 'B', -1);
});

Scenario('inquiry - statistics final', async ({ I }) => {
    I.wjSetDefaultWindowSize();
});

function checkSelects(I) {
    I.say("Check that selects have wight values");

    I.clickCss('button[data-id="answerSelect"]');
    I.waitForElement("div.dropdown-menu.show");
    answers.forEach(answer => {
        I.seeElement( locate("div.dropdown-menu.show").find( locate("a.dropdown-item > span").withText(answer) ) );
    });

    I.clickCss('button[data-id="userSelect"]');
    I.waitForElement("div.dropdown-menu.show");
    users.forEach(user => {
        I.seeElement( locate("div.dropdown-menu.show").find( locate("a.dropdown-item > span").withText(user) ) );
    });
    I.pressKey('Escape');
}

/**
 *
 * @param {*} I
 * @param {*} DT
 * @param {*} Document
 * @param {*} imageNameSuffix
 * @param {*} index - used because of failed test steps and to generate all images in one run
 */
async function validateFilteringBehavior(I, DT, Document, imageNameSuffix, index) {
    await adjustScrollbar(I, "#inquiryStatDataTable_extfilter", imageNameSuffix);

    if (index == 1) {
        await setPageRowLimit(I, DT, 'Všetky', imageNameSuffix);

        setDates(I, DT, '05.11.2024', '06.11.2024');
        I.waitForText('Záznamy 1 až 11 z 11', 10);

        await verifyGraphs(I, Document, `1${imageNameSuffix}`, imageNameSuffix);
    } else if (index == 2) {
        setDates(I, DT, '05.11.2024', '07.11.2024');
        I.waitForText('Záznamy 1 až 16 z 16');

        await verifyGraphs(I, Document, `2${imageNameSuffix}`, imageNameSuffix);
    } else if (index == 3) {
        I.clickCss('button[data-id="userSelect"]');
        I.click('Prihlásení');
        I.waitForText('Záznamy 1 až 13 z 13');

        await verifyGraphs(I, Document, `3${imageNameSuffix}`, imageNameSuffix);
    } else if (index == 4) {

        I.clickCss('button[data-id="userSelect"]');
        I.click('Neprihlásení');
        I.waitForText('Záznamy 1 až 3 z 3');

        await verifyGraphs(I, Document, `4${imageNameSuffix}`, imageNameSuffix);
    } else {
        const table = '#inquiryStatDataTable_wrapper';
        await adjustScrollbar(I, table, imageNameSuffix);
        DT.filterSelect('userFullName', 'Tester Playwright');
        I.waitForText('Nenašli sa žiadne vyhovujúce záznamy');

        await adjustScrollbar(I, "#inquiryStatDataTable_extfilter", imageNameSuffix);
        I.clickCss('button[data-id="userSelect"]');
        I.click('Všetci');
        DT.waitForLoader();
        await adjustScrollbar(I, table, imageNameSuffix);
        DT.checkTableRow("inquiryStatDataTable", 1, ["698", "Tester Playwright", "Programovanie"]);
        DT.checkTableRow("inquiryStatDataTable", 2, ["709", "Tester Playwright", "Správa sietí a bezpečnosť"]);
        I.see("Tester Playwright", "td .datatable-column-width");

        DT.clearFilter('userFullName');
        await adjustScrollbar(I, table, imageNameSuffix);
        DT.filterSelect('answerText', 'Programovanie');
        I.waitForText('Záznamy 1 až 1 z 1');
        DT.checkTableRow("inquiryStatDataTable", 1, ["698", "Tester Playwright", "Programovanie"]);

        DT.clearFilter('answerText');
        DT.filterId('id', '706');
        I.waitForText('Záznamy 1 až 1 z 1');
        DT.checkTableRow("inquiryStatDataTable", 1, ["706", "Janko Tester", "Vývoj webových aplikácií"]);
        await setPageRowLimit(I, DT, 'Automaticky', imageNameSuffix);
    }
}

async function verifyGraphs(I, Document, screenshotNumber, imageNameSuffix) {
    const pieVotesSelector = "#inquiryStat-pieVotes";
    const inquiryStatSelector = "#inquiryStat-lineVotes";

    await adjustScrollbar(I, pieVotesSelector, imageNameSuffix);
    await Document.compareScreenshotElement(pieVotesSelector, `pievotes${screenshotNumber}.png`, 1280, 1200, TOLERANCE);
    await adjustScrollbar(I, inquiryStatSelector, imageNameSuffix);
    await Document.compareScreenshotElement(inquiryStatSelector, `linevotes${screenshotNumber}.png`, 1280, 1200, TOLERANCE);
    await adjustScrollbar(I, '#pills-inquiryStat', imageNameSuffix);
    await adjustScrollbar(I, pieVotesSelector, imageNameSuffix);
    await Document.compareScreenshotElement(pieVotesSelector, `pievotes${screenshotNumber}.png`, 1280, 1200, TOLERANCE);
    await adjustScrollbar(I, inquiryStatSelector, imageNameSuffix);
    await Document.compareScreenshotElement(inquiryStatSelector, `linevotes${screenshotNumber}.png`, 1280, 1200, TOLERANCE);
    await adjustScrollbar(I, '#pills-inquiryStat', imageNameSuffix);
}

async function setPageRowLimit(I, DT, rows, imageNameSuffix) {
    await adjustScrollbar(I, "#inquiryStatDataTable_wrapper", imageNameSuffix);
    I.click(DT.btn.inquiryStat_settings_button);
    I.clickCss('.buttons-page-length');
    await adjustScrollbar(I, "#pills-inquiryStat", imageNameSuffix);
    I.click(locate('button.btn.button-page-length').withText(rows));
    I.clickCss('button.btn.btn-primary.dt-close-modal');
}

function verifyInquiryStatisticsCharts(I) {

    I.say("Verifying inquiry statistics charts");
    I.seeElement('#inquiryStatDataTable');

    const chartIDs = ["#inquiryStat-pieVotes", "#inquiryStat-lineVotes"];

    chartIDs.forEach(chartID => {
        I.seeElement(chartID);

        // Eevrz question is label in chart
        answers.forEach(label => {
            I.seeElement(locate(`${chartID} [aria-label='${label}; Press ENTER to toggle']`));
        });
    });

    I.seeElement(locate("#inquiryStat-lineVotes [aria-label='Zoom Out']").withAttr({ role: "button" }));
    I.seeElement(locate("#inquiryStat-lineVotes [aria-label^='Od ']").withAttr({ role: "slider" }));
    I.seeElement(locate("#inquiryStat-lineVotes [aria-label^='Do ']").withAttr({ role: "slider" }));

}

function setDates(I, DT, dateFrom, dateTo) {
    I.fillField("div.dt-extfilter-dayDate > form > div.input-group > input.datepicker.min", dateFrom);
    I.fillField("div.dt-extfilter-dayDate > form > div.input-group > input.datepicker.max", dateTo);
    I.click("div.dt-extfilter-dayDate > form > div.input-group > button.filtrujem");
    DT.waitForLoader();
}

async function adjustScrollbar(I, selector, imageNameSuffix) {
    I.switchTo();

    const inModal = imageNameSuffix === 'A';
    await I.executeScript(({selector, inModal}) => {
        const view = inModal ? document.querySelector('#statIframe').contentWindow : window;
        const target = view.document.querySelector(selector);
        if (!target) throw new Error(`Element not found for selector: ${selector}`);
        const scrollbar = view.scrollbarMain;
        const top = scrollbar.contentEl.contains(target)
            ? target.getBoundingClientRect().top - scrollbar.contentEl.getBoundingClientRect().top
            : 0;
        scrollbar.setPosition(0, top);
    }, {selector, inModal});

    if (inModal) I.switchTo('#statIframe');
}
