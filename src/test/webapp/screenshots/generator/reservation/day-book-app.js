Feature('day-book-app');

Before(({ I, login }) => {
    login('admin');
});

Scenario('reservation screens - admin section', ({ I, DTE, Document }) => {
    let searchText;
    switch (I.getConfLng()) {
        case 'sk':
            searchText = "Rezervácia dní";
            break;
        case 'en':
            searchText = "Reservation of days";
            break;
        case 'cs':
            searchText = "Rezervace dní";
            break;
        default:
            throw new Error(`Unsupported language code: ${I.getConfLng()}`);
    }

    let appId = "#components-reservation-day_book-title";

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=111487");
    DTE.waitForEditor();
    I.wait(5);

    Document.editorComponentOpen();
    I.wait(5);

    I.clickCss("button.dropdown-toggle");
    I.switchTo();
    I.switchTo();
    Document.screenshotElement("div.cke_dialog_body", "/redactor/apps/reservation/day-book-app/app-editor.png");
    I.clickCss("a.cke_dialog_ui_button_ok");

    I.clickCss("a.cke_button__components");

    //1. iframe
    I.waitForElement("div.cke_dialog_body > table.cke_dialog_contents > tbody > tr > td.cke_dialog_contents_body > div > table > tbody > tr > td > iframe", 5);
    I.switchTo("div.cke_dialog_body > table.cke_dialog_contents > tbody > tr > td.cke_dialog_contents_body > div > table > tbody > tr > td > iframe");

    //2. iframe
    I.switchTo("iframe#editorComponent");
    I.seeElement("input#search");
    I.wait(2);
    I.fillField("input#search", searchText);

    I.waitForElement(appId);
    I.wait(2);
    I.clickCss(appId);

    switch (I.getConfLng()) {
        case 'en':
            I.waitForElement(locate("div.description").withText("Reservation of all-day services, e.g. accommodation, skis, bicycles"));
            break;
        case 'sk':
            I.waitForElement(locate("div.description").withText("Rezervácia celodňových služieb, napr. ubytovanie, lyže, bicykel"));
            break;
        case 'cs':
            I.waitForElement(locate("div.description").withText("Rezervace celodenních služeb, například. ubytování, lyže, kolo"));
            break;
        default:
            throw new Error(`Unsupported language code: ${I.getConfLng()}`);
    }

    I.switchTo();
    I.switchTo();

    Document.screenshotElement("div.cke_dialog_body", "/redactor/apps/reservation/day-book-app/app-adding.png");
});

Scenario('reservation screens - PAGE section', ({ I, Document }) => {
    I.amOnPage("/apps/rezervacie/rezervacia-izby.html?NO_WJTOOLBAR=true&language="+I.getConfLng());
    I.waitForVisible("#calendarContainer");

    I.say("Set reservation object");
    I.waitForVisible("#reservationObjectId");
    I.clickCss("#reservationObjectId");
    I.executeScript(function() {
        $("#reservationObjectId").val("4146");
    });

    I.click( locate("div.vc-header__content").find("button.vc-month") );
    I.waitForVisible("div.vc-months");
    Document.screenshotElement("#calendar", "/redactor/apps/reservation/day-book-app/calendar_month_select.png");

    I.click( locate("div.vc-header__content").find("button.vc-year") );
    I.waitForVisible("div.vc-years");
    Document.screenshotElement("#calendar", "/redactor/apps/reservation/day-book-app/calendar_year_select.png");
    I.click( locate("div.vc-header__content").find("button.vc-year") );

    I.say("Set date in future");
    I.waitForVisible("#calendar > .vc-controls");
    setDate(I, 2032, 6);

    Document.screenshotElement(getDayLocate("2032-06-03"), "/redactor/apps/reservation/day-book-app/normal_day.png");
    Document.screenshotElement(getDayLocate("2032-07-21"), "/redactor/apps/reservation/day-book-app/parcial_normal_day.png");
    Document.screenshotElement(getDayLocate("2032-06-05"), "/redactor/apps/reservation/day-book-app/weekend_day.png");
    Document.screenshotElement(getDayLocate("2032-06-16"), "/redactor/apps/reservation/day-book-app/old_or_booked_day.png");
    Document.screenshotElement(getDayLocate("2032-06-15"), "/redactor/apps/reservation/day-book-app/check_out_only_day.png");

    Document.screenshotElement(".ly-content .container", "/redactor/apps/reservation/day-book-app/app-table_A.png");

    Document.screenshotElement("#calendar", "/redactor/apps/reservation/day-book-app/calendar_base.png");

    I.moveCursorTo( getDayLocate("2032-06-15") );
    Document.screenshotElement("#calendar", "/redactor/apps/reservation/day-book-app/calendar_D.png");

    I.click( getDayLocate("2032-07-19") );
    Document.screenshotElement("#calendar", "/redactor/apps/reservation/day-book-app/calendar_A.png");

    I.moveCursorTo( getDayLocate("2032-07-27") );
    Document.screenshotElement("#calendar", "/redactor/apps/reservation/day-book-app/calendar_B.png");

    I.click( getDayLocate("2032-07-27") );
    Document.screenshotElement("#calendar", "/redactor/apps/reservation/day-book-app/calendar_C.png");

    Document.screenshotElement(".ly-content .container", "/redactor/apps/reservation/day-book-app/app-table_B.png");
    Document.screenshotElement(".ly-content .container", "/../../src/main/webapp/apps/reservation/admin/day-book-screenshot-1.png");

    Document.screenshotElement("#reservationForm", "/redactor/apps/reservation/day-book-app/reservation_form.png");

    // I.click( locate("button.btn.btn-primary").withAttr({ 'name': 'saveForm' }) );
    // I.waitForVisible( locate("div.alert.alert-success"));

    // Document.screenshotElement("div.alert.alert-success", "/redactor/apps/reservation/day-book-app/app-reservation-saved-approved.png");

    // I.clickCss(".vc-controls > button.vc-arrow vc-arrow_prev")
    // Document.screenshotElement("#calendar", "/redactor/apps/reservation/day-book-app/calendar_E.png");
});

function getDayLocate(id) {
    return locate(".vc-date").withAttr({ 'data-vc-date': id }).find("button.vc-date__btn")
}

/**
 * Set Date
 * @param {number} year
 * @param {number} month - starts from 1
 */
function setDate(I, year, month) {
    I.clickCss("button.vc-year");
    I.waitForVisible(".vc-years");
    I.clickCss("button[data-vc-years-year='" + year + "']");
    I.waitForInvisible(".vc-years");

    I.clickCss("button.vc-month");
    I.waitForVisible("button.vc-months__month[data-vc-months-month='" + (month - 1) + "']");
    I.clickCss("button.vc-months__month[data-vc-months-month='" + (month - 1) + "']");
    I.waitForInvisible("button.vc-months__month[data-vc-months-month='" + (month - 1) + "']");

    const monthId = month < 10 ? "0" + month : "" + month;
    I.waitForVisible("div.vc-date[data-vc-date='" + year + "-" + monthId + "-01']");
}