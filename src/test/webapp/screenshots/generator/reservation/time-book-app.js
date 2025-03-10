Feature('time-book-app');

Before(({ I, login }) => {
    login('admin');
});

Scenario('Remove old reservations for new screens', async ({ I, DT, Document }) => {
    I.amOnPage("/apps/reservation/admin/");

    var date = "03.01.2045";
    if ("en"===I.getConfLng()) date = "01/03/2045";

    I.fillField({css: "input.dt-filter-from-dateFrom"}, date);
    I.fillField({css: "input.dt-filter-to-dateFrom"}, date);
    I.clickCss("button.dt-filtrujem-dateFrom");
    I.clickCss("button.dt-filtrujem-dateFrom");

    DT.waitForLoader();

    let rows = await I.getTotalRows();

    if (rows > 0) {
        I.clickCss("button.buttons-select-all");
        I.clickCss("button.custom-buttons-remove");
        I.waitForElement("div.DTE_Action_Remove");
        I.clickCss("div.DTE_Action_Remove div.DTE_Footer div.DTE_Form_Buttons button.btn-primary");
        switch (I.getConfLng()) {
            case 'sk':
                I.see("Nenašli sa žiadne vyhovujúce záznamy");
                break;
            case 'en':
                I.see("No matching records found");
                break;
            case 'cs':
                I.see("Nenašly se žádné vyhovující záznamy");
                break;
            default:
                throw new Error('Unsupported language code');
        }
        
    }
});

Scenario('reservation screens - admin section', ({ I, DTE, Document }) => {
    let searchText;
    switch (I.getConfLng()) {
        case 'sk':
            searchText = "Rezervácia času";
            break;
        case 'en':
            searchText = "Reservation by time";
            break;
        case 'cs':
            searchText = "Rezervace času";
            break;
        default:
            throw new Error(`Unsupported language code: ${I.getConfLng()}`);
        }
    
    
    let appId = "#components-reservation-time_book-title";


    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=95273");
    DTE.waitForEditor();
    I.wait(5);

    Document.editorComponentOpen();
    I.wait(5);

    I.clickCss("button.dropdown-toggle");
    I.switchTo();
    I.switchTo();
    Document.screenshotElement("div.cke_dialog_body", "/redactor/apps/reservation/time-book-app/app-editor.png");
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
            I.waitForElement(locate("div.description").withText("Reservation in a time interval"));
            break;
        case 'sk':
            I.waitForElement(locate("div.description").withText("Rezervácia v časovom intervale, napr. tenisové kurty, welness, masáže"));
            break;
        case 'cs':
            I.waitForElement(locate("div.description").withText("Rezervace v časovém intervalu, například. tenisové kurty, welness, masáže"));
            break;
        default:
            throw new Error(`Unsupported language code: ${I.getConfLng()}`);
    }
    
    I.switchTo();
    I.switchTo();

    Document.screenshotElement("div.cke_dialog_body", "/redactor/apps/reservation/time-book-app/app-adding.png");
});

Scenario('reservation screens - PAGE section', ({ I, Document }) => {
    I.amOnPage("/apps/rezervacie/rezervacia-tenisovych-kurtov.html?NO_WJTOOLBAR=true&language="+I.getConfLng());
    I.waitForVisible("#reservationDate");

    I.say("Set date in future");
    I.fillField("#reservationDate", "01-01-2045");

    Document.screenshotElement("div.table-responsive", "/redactor/apps/reservation/time-book-app/app-table_A.png");

    I.say("Set date in future");
    I.fillField("#reservationDate", "02-01-2045");

    I.fillField("#email", "");
    Document.screenshot("/redactor/apps/reservation/time-book-app/app-page.png");
    Document.screenshot("../../../src/main/webapp/apps/reservation/mvc/app-page.png");

    Document.screenshotElement("div.dateHeader", "/redactor/apps/reservation/time-book-app/app-date-header.png");

    Document.screenshotElement("div.table-responsive", "/redactor/apps/reservation/time-book-app/app-table_B.png");

    Document.screenshotElement("td[id='2561_8'].unsupported", "/redactor/apps/reservation/time-book-app/app-cell-unsupported.png");
    Document.screenshotElement("td[id='2560_12'].free", "/redactor/apps/reservation/time-book-app/app-cell-free.png");
    Document.screenshotElement("td[id='2560_10'].full", "/redactor/apps/reservation/time-book-app/app-cell-full.png");

    I.clickCss("td[id='2560_13'].free");
    Document.screenshotElement("td[id='2560_13'].free.selectedTableCell", "/redactor/apps/reservation/time-book-app/app-cell-selected.png");

    I.clickCss("td[id='2560_14'].free");
    I.clickCss("td[id='2560_15'].free");

    Document.screenshotElement("#reservationForm", "/redactor/apps/reservation/time-book-app/app-reservation_form.png");

    Document.screenshot("/redactor/apps/reservation/time-book-app/app-ready-reservation.png");

    I.fillField("#reservationDate", "03-01-2045");
    I.clickCss("td[id='2560_13'].free");
    I.waitForVisible("button.btn.btn-primary", 1);
    I.clickCss("button.btn.btn-primary");
    I.waitForVisible( locate("div.alert.alert-success"));

    Document.screenshotElement("div.alert.alert-success", "/redactor/apps/reservation/time-book-app/app-reservation-saved-approved.png");

    I.clickCss("td[id='2561_13'].free");
    I.waitForVisible("button.btn.btn-primary", 1);
    I.clickCss("button.btn.btn-primary");
    I.waitForVisible( locate("div.alert.alert-success"));

    Document.screenshotElement("div.alert.alert-success", "/redactor/apps/reservation/time-book-app/app-reservation-saved-awaiting-approve.png");
});