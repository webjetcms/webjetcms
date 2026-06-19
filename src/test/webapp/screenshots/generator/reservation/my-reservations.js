Feature('my-reservations');

const MY_RESERVATIONS_URL = "/apps/rezervacie/my-reservations.html";
const SCREENSHOT_PATH = "/redactor/apps/reservation/my-reservations-app";
const APP_ID = "#components-reservation-my_reservations-title";

Before(({ login }) => {
    login('admin');
});

Scenario('reservation screens - admin section', ({ I, DT, DTE, Document }) => {
    const texts = getAppStoreTexts(I.getConfLng());

    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.clickCss("#pills-dt-datatableInit-content-tab");
    I.clickCss("a.cke_button__components");

    I.switchTo(".cke_dialog_ui_iframe");
    I.waitForElement("#editorComponent", 10);
    I.switchTo("#editorComponent");
    I.waitForElement("input#search", 10);
    I.fillField("input#search", texts.searchText);

    I.waitForElement(APP_ID, 10);
    I.clickCss(APP_ID);
    I.waitForElement(locate("div.description").withText(texts.descriptionText), 10);

    I.switchTo();
    I.switchTo();

    Document.screenshotElement("div.cke_dialog_body", SCREENSHOT_PATH + "/app-adding.png");
    I.clickCss("a.cke_dialog_ui_button_cancel");
});

Scenario('reservation screens - PAGE section', async ({ I, Document, i18n }) => {
    I.relogin("tester2", true, true, I.getConfLng());

    openMyReservations(I, "2051-04-01", "2052-06-30");

    Document.screenshot("../../../src/main/webapp/apps/reservation/mvc/my-reservations-app-page"+i18n.getImgSuffix()+".png");

    Document.screenshotElement("#myReservationsApp", SCREENSHOT_PATH + "/app-page.png", 1400, 850);
    Document.screenshotElement("#myReservationsApp > form.row", SCREENSHOT_PATH + "/app-filter.png");
    Document.screenshotElement("div.table-responsive", SCREENSHOT_PATH + "/app-table.png", 1400, 550);
});

function openMyReservations(I, dateFrom, dateTo) {
    I.amOnPage(withLng(MY_RESERVATIONS_URL, I, { "NO_WJTOOLBAR": "true" }));
    I.waitForElement("#myReservationsTable", 10);

    if (dateFrom != null) fillDateInput(I, "#reservation-date-from", dateFrom);
    if (dateTo != null) fillDateInput(I, "#reservation-date-to", dateTo);

    if (dateFrom != null || dateTo != null) {
        I.click("#myReservationsApp > form.row button[type='submit']");
        if (dateFrom != null) I.waitInUrl(`reservation-date-from=${dateFrom}`, 10);
        if (dateTo != null) I.waitInUrl(`reservation-date-to=${dateTo}`, 10);
        I.seeInField("#reservation-date-from", dateFrom || "");
        I.seeInField("#reservation-date-to", dateTo || "");
        I.waitForElement("#myReservationsTable", 10);
    }
}

function fillDateInput(I, selector, dateValue) {
    I.fillField(selector, dateValue.split("-").reverse().join("-"));
    I.waitForValue(selector, dateValue, 10);
}

function withLng(url, I, params = {}) {
    const queryParams = new URLSearchParams(params);
    queryParams.set("language", I.getConfLng());
    return `${url}?${queryParams.toString()}`;
}

function getAppStoreTexts(lng) {
    switch (lng) {
        case "en":
            return {
                searchText: "My Reservations",
                descriptionText: "Overview of the logged-in user's own reservations"
            };
        case "cs":
            return {
                searchText: "Moje rezervace",
                descriptionText: "Přehled vlastních rezervací přihlášeného uživatele"
            };
        case "sk":
            return {
                searchText: "Moje rezervácie",
                descriptionText: "Prehľad vlastných rezervácií prihláseného používateľa"
            };
        default:
            throw new Error(`Unsupported language code: ${lng}`);
    }
}
