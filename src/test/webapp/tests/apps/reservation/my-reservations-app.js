Feature('apps.reservation.my-reservations-app');

const MY_RESERVATIONS_URL = "/apps/rezervacie/my-reservations.html";
const TIME_BOOK_URL = "/apps/rezervacie/rezervacia-tenisovych-kurtov.html";
const DAY_BOOK_URL = "/apps/rezervacie/rezervacia-spa.html";
const PURPOSE_PREFIX = "autotest-my-reservations-";
const DELETE_PASSWORD = "right_password";

const TIMED_DATE = "2051-04-10";
const TIMED_ACCEPTED_OBJECT = "Tenisovy kurt A";
const TIMED_WAITING_OBJECT = "Tenisovy kurt B";
const TIMED_ACCEPTED_PARTS = [TIMED_ACCEPTED_OBJECT, "10.04.2051 13:00", "15:00"];
const TIMED_WAITING_PARTS = [TIMED_WAITING_OBJECT, "10.04.2051 14:00", "16:00"];

const ALL_DAY_OBJECT = "Spa celodenné";
const ALL_DAY_FROM_TEXT = "10.06.2052";
const ALL_DAY_TO_TEXT = "12.06.2052";
const ALL_DAY_PARTS = [ALL_DAY_OBJECT, ALL_DAY_FROM_TEXT, ALL_DAY_TO_TEXT];

const PASSWORD_OBJECT = "reservation_base_tests";
const PASSWORD_DATE = "2053-07-10";
const PASSWORD_TIME_FROM = "09:00";
const PASSWORD_TIME_TO = "10:00";
const PASSWORD_PARTS = [PASSWORD_OBJECT, "10.07.2053 09:00", "10:00"];

let randomNumber;
let acceptedPurpose;
let waitingPurpose;
let allDayPurpose;
let passwordPurpose;
let acceptedTimedPrice;

Before(({ I, DT }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        acceptedPurpose = `${PURPOSE_PREFIX}${randomNumber}-accepted`;
        waitingPurpose = `${PURPOSE_PREFIX}${randomNumber}-waiting`;
        allDayPurpose = `${PURPOSE_PREFIX}${randomNumber}-all-day`;
        passwordPurpose = `${PURPOSE_PREFIX}${randomNumber}-password`;
    }
    DT.addContext("reservation", "#reservationDataTable_wrapper");
});

Scenario("Cleanup old My Reservations autotest rows", async ({ I, DT, DTE }) => {
    await deleteReservationsByPurpose(I, DT, DTE, PURPOSE_PREFIX);
});

Scenario("Create My Reservations test data", async ({ I, DT }) => {
    const passwordObjectId = await getReservationObjectId(I, DT, PASSWORD_OBJECT);

    I.relogin("tester2");

    acceptedTimedPrice = await createTimedReservation(I, {
        objectCellId: "2560",
        startHour: 13,
        endHour: 14,
        purpose: acceptedPurpose,
        requiresApproval: false
    });

    await createTimedReservation(I, {
        objectCellId: "2561",
        startHour: 14,
        endHour: 15,
        purpose: waitingPurpose,
        requiresApproval: true
    });

    await createAllDayReservation(I, allDayPurpose);
    await createPasswordTimedReservation(I, passwordObjectId, passwordPurpose);
});

Scenario("Render and filter My Reservations rows", async ({ I }) => {
    I.relogin("tester2");

    openMyReservations(I, "2051-04-01", "2052-06-30");
    await assertSingleDeleteForm(I);

    await assertRowVisible(I, TIMED_ACCEPTED_PARTS);
    await assertRowVisible(I, TIMED_WAITING_PARTS);
    await assertRowVisible(I, ALL_DAY_PARTS);

    await assertRowContains(I, TIMED_ACCEPTED_PARTS, acceptedTimedPrice);
    await assertRowContains(I, TIMED_ACCEPTED_PARTS, "Schválená");
    await assertRowContains(I, TIMED_WAITING_PARTS, "Nepotvrdená");
    await assertRowContains(I, ALL_DAY_PARTS, "Schválená");

    await assertDeleteButton(I, TIMED_ACCEPTED_PARTS, true);
    await assertDeleteButton(I, TIMED_WAITING_PARTS, false);
    await assertDeleteButton(I, ALL_DAY_PARTS, true);

    const allDayRow = await getRow(I, ALL_DAY_PARTS);
    I.assertNotContain(allDayRow.text, "14:00", "All-day reservation must not show arrival time.");
    I.assertNotContain(allDayRow.text, "10:30", "All-day reservation must not show departure time.");

    openMyReservations(I, "2052-06-01", null);
    await assertRowVisible(I, ALL_DAY_PARTS);
    await assertRowAbsent(I, TIMED_ACCEPTED_PARTS);

    openMyReservations(I, null, "2051-04-30");
    await assertRowVisible(I, TIMED_ACCEPTED_PARTS);
    await assertRowVisible(I, TIMED_WAITING_PARTS);
    await assertRowAbsent(I, ALL_DAY_PARTS);

    openMyReservations(I, "2052-06-01", "2052-06-30");
    await assertRowVisible(I, ALL_DAY_PARTS);
    await assertRowAbsent(I, TIMED_ACCEPTED_PARTS);
});

Scenario("Delete accepted timed reservation", async ({ I }) => {
    I.relogin("tester2");
    openMyReservations(I, "2051-04-01", "2051-04-30");

    await assertDeletePasswordRequired(I, TIMED_ACCEPTED_PARTS, false);
    await clickDeleteButton(I, TIMED_ACCEPTED_PARTS);

    I.waitForElement(".alert.alert-success", 10);
    I.seeInField("#reservation-date-from", "2051-04-01");
    I.seeInField("#reservation-date-to", "2051-04-30");
    await assertRowAbsent(I, TIMED_ACCEPTED_PARTS);
    await assertRowVisible(I, TIMED_WAITING_PARTS);
});

Scenario("Delete accepted all-day reservation", async ({ I }) => {
    I.relogin("tester2");
    openMyReservations(I, "2052-06-01", "2052-06-30");

    await assertDeletePasswordRequired(I, ALL_DAY_PARTS, false);
    await clickDeleteButton(I, ALL_DAY_PARTS);

    I.waitForElement(".alert.alert-success", 10);
    I.seeInField("#reservation-date-from", "2052-06-01");
    I.seeInField("#reservation-date-to", "2052-06-30");
    await assertRowAbsent(I, ALL_DAY_PARTS);
});

Scenario("Delete password-protected reservation", async ({ I }) => {
    I.relogin("tester2");
    openMyReservations(I, "2053-07-01", "2053-07-31");

    await assertDeletePasswordRequired(I, PASSWORD_PARTS, true);
    await mockDeletePasswordPrompt(I, DELETE_PASSWORD);
    await clickDeleteButton(I, PASSWORD_PARTS);

    I.waitForElement(".alert.alert-success", 10);
    const promptMessage = await I.executeScript(() => window.sessionStorage.getItem("myReservationsPromptMessage"));
    I.assertContain(promptMessage, "Zadajte heslo", "Prompt message should include the translated title.");
    I.assertContain(promptMessage, PASSWORD_OBJECT, "Prompt message should include the reservation object name.");
    I.seeInField("#reservation-date-from", "2053-07-01");
    I.seeInField("#reservation-date-to", "2053-07-31");
    await assertRowAbsent(I, PASSWORD_PARTS);
});

Scenario("Final cleanup of My Reservations autotest rows", async ({ I, DT, DTE }) => {
    await deleteReservationsByPurpose(I, DT, DTE, PURPOSE_PREFIX);
});

async function createTimedReservation(I, config) {
    openTimedReservationDate(I, TIMED_DATE);
    I.waitForElement(timedCellSelector(config.objectCellId, config.startHour), 10);

    for (let hour = config.startHour; hour <= config.endHour; hour++) {
        I.clickCss(timedCellSelector(config.objectCellId, hour));
    }

    const endHourText = String(config.endHour + 1).padStart(2, "0");
    I.seeInField("#timeRange", `${String(config.startHour).padStart(2, "0")}:00 - ${endHourText}:00`);
    const price = await I.grabValueFrom("#price");
    I.assertNotEqual(price, "", "Reservation price must be calculated before submitting.");

    I.fillField("#purpose", config.purpose);
    I.waitForVisible(locate("#reservationForm button").withAttr({ type: "submit" }), 10);
    I.click(locate("#reservationForm button").withAttr({ type: "submit" }));

    if (config.requiresApproval) {
        I.waitForText("Vaša rezervácia bola úspešne vytvorená a teraz čaká na schválenie.", 10);
    } else {
        I.waitForText("Vaša rezervácia bola úspešne vytvorená a schválená.", 10);
    }

    return price;
}

function timedCellSelector(objectCellId, hour) {
    return `td[id='${objectCellId}_${hour}'].free`;
}

function openTimedReservationDate(I, dateValue) {
    I.amOnPage(TIME_BOOK_URL);
    I.waitForVisible("#reservationDate", 10);
    fillDateInput(I, "#reservationDate", dateValue);
    I.waitInUrl(`reservation-date=${dateValue}`, 10);
    I.waitForValue("#reservationDate", dateValue, 10);
}

function fillDateInput(I, selector, dateValue) {
    I.fillField(selector, dateValue.split("-").reverse().join("-"));
    I.waitForValue(selector, dateValue, 10);
}

async function createAllDayReservation(I, purpose) {
    I.amOnPage(DAY_BOOK_URL);
    I.waitForVisible("#reservationObjectId", 10);
    I.selectOption("#reservationObjectId", ALL_DAY_OBJECT);

    await I.executeScript(({ dateFrom, dateTo, purposeText }) => {
        const setValue = (selector, value) => {
            const field = document.querySelector(selector);
            field.removeAttribute("readonly");
            field.value = value;
            field.dispatchEvent(new Event("input", { bubbles: true }));
            field.dispatchEvent(new Event("change", { bubbles: true }));
        };

        setValue("#dateFrom", `${dateFrom} - 14:00`);
        setValue("#dateTo", `${dateTo} - 10:30`);
        setValue("#purpose", purposeText);
        setValue("#phoneNumber", "0912345678");
        setValue("#actualDate", "2052-06");
    }, {
        dateFrom: ALL_DAY_FROM_TEXT,
        dateTo: ALL_DAY_TO_TEXT,
        purposeText: purpose
    });

    I.wait(1);
    await submitReservationForm(I);
    I.waitForText("Vaša rezervácia bola úspešne vytvorená a schválená.", 10);
}

async function createPasswordTimedReservation(I, reservationObjectId, purpose) {
    I.amOnPage(TIME_BOOK_URL);
    I.waitForVisible("#reservationForm", 10);

    await I.executeScript(({ reservationDate, reservationObjectIdValue, purposeText, timeFrom, timeTo }) => {
        const setValue = (selector, value, triggerEvents = true) => {
            const field = document.querySelector(selector);
            field.value = value;
            if(triggerEvents) {
                field.dispatchEvent(new Event("input", { bubbles: true }));
                field.dispatchEvent(new Event("change", { bubbles: true }));
            }
        };

        setValue("#reservationDate", reservationDate, false);
        setValue("#reservationDateHidden", reservationDate);
        setValue("#reservationObjectId", reservationObjectIdValue);
        setValue("#timeRange", `${timeFrom} - ${timeTo}`);
        setValue("#purpose", purposeText);
    }, {
        reservationDate: PASSWORD_DATE,
        reservationObjectIdValue: String(reservationObjectId),
        purposeText: purpose,
        timeFrom: PASSWORD_TIME_FROM,
        timeTo: PASSWORD_TIME_TO
    });

    await submitReservationForm(I);
    I.waitForText("Vaša rezervácia bola úspešne vytvorená a schválená.", 10);
}

async function submitReservationForm(I) {
    await I.executeScript(() => {
        const form = document.getElementById("reservationForm");
        const submitButton = form.querySelector("button[name='saveForm']");
        if (form.requestSubmit) {
            form.requestSubmit(submitButton);
        } else {
            const submitInput = document.createElement("input");
            submitInput.type = "hidden";
            submitInput.name = "saveForm";
            submitInput.value = "true";
            form.appendChild(submitInput);
            form.submit();
        }
    });
}

async function deleteReservationsByPurpose(I, DT, DTE, purposePrefix) {
    I.relogin("admin");
    I.amOnPage("/apps/reservation/admin/");
    DT.waitForLoader("reservationDataTable");
    await DT.showColumn("Účel", "reservationDataTable");
    DT.filterContains("purpose", purposePrefix);

    const rows = await I.getTotalRows();
    if (rows < 1) return;

    I.clickCss("button.buttons-select-all");
    I.clickCss("button.custom-buttons-remove");

    if (await I.grabNumberOfVisibleElements("#toast-container-webjet div.toastr-input > input") > 0) {
        I.fillField("#toast-container-webjet div.toastr-input > input", DELETE_PASSWORD);
        I.clickCss("#toast-container-webjet div.toastr-buttons > button.btn.btn-primary");
    }

    I.waitForElement("div.DTE_Action_Remove", 10);
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForModalClose("reservationDataTable_modal");
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 30);
}

async function getReservationObjectId(I, DT, objectName) {
    I.relogin("admin");
    I.amOnPage("/apps/reservation/admin/reservation-objects/");
    DT.waitForLoader("reservationObjectDataTable");
    DT.filterContains("name", objectName);
    I.waitForElement("#reservationObjectDataTable tbody tr", 10);

    const objectId = await I.executeScript((name) => {
        const row = Array.from(document.querySelectorAll("#reservationObjectDataTable tbody tr"))
            .find((tableRow) => tableRow.innerText.includes(name));
        if(row == null) return null;

        const rowId = row.getAttribute("id") || "";
        const idMatch = rowId.match(/\d+$/);
        return idMatch == null ? null : idMatch[0];
    }, objectName);

    I.assertTrue(objectId != null, `Reservation object id not found for: ${objectName}`);
    return objectId;
}

function openMyReservations(I, dateFrom, dateTo) {
    I.amOnPage(MY_RESERVATIONS_URL);
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

async function assertSingleDeleteForm(I) {
    const formCounts = await I.executeScript(() => ({
        deleteForms: document.querySelectorAll("#myReservationsDeleteForm").length,
        rowForms: document.querySelectorAll("#myReservationsTable tbody form").length
    }));

    I.assertEqual(formCounts.deleteForms, 1, "There must be exactly one hidden delete form.");
    I.assertEqual(formCounts.rowForms, 0, "Reservation rows must not render per-row delete forms.");
}

async function getRows(I) {
    return I.executeScript(() => Array.from(document.querySelectorAll("#myReservationsTable tbody tr")).map((row) => ({
        text: row.innerText,
        deleteButtonCount: row.querySelectorAll(".reservation-delete-button").length,
        deletePasswordRequired: row.querySelector(".reservation-delete-button")?.getAttribute("data-delete-password-required") || null
    })));
}

async function getRow(I, parts) {
    const rows = await getRows(I);
    return rows.find((row) => parts.every((part) => row.text.includes(part)));
}

async function assertRowVisible(I, parts) {
    const row = await getRow(I, parts);
    I.assertTrue(row != null, `Expected reservation row containing: ${parts.join(" | ")}`);
}

async function assertRowAbsent(I, parts) {
    const row = await getRow(I, parts);
    I.assertTrue(row == null, `Reservation row should not be visible: ${parts.join(" | ")}`);
}

async function assertRowContains(I, parts, text) {
    const row = await getRow(I, parts);
    I.assertTrue(row != null, `Expected reservation row containing: ${parts.join(" | ")}`);
    I.assertContain(row.text, text, `Reservation row should contain: ${text}`);
}

async function assertDeleteButton(I, parts, expectedVisible) {
    const row = await getRow(I, parts);
    I.assertTrue(row != null, `Expected reservation row containing: ${parts.join(" | ")}`);
    I.assertEqual(row.deleteButtonCount > 0, expectedVisible, `Delete button visibility mismatch for row: ${parts.join(" | ")}`);
}

async function assertDeletePasswordRequired(I, parts, expectedRequired) {
    const row = await getRow(I, parts);
    I.assertTrue(row != null, `Expected reservation row containing: ${parts.join(" | ")}`);
    I.assertEqual(row.deletePasswordRequired === "true", expectedRequired, `Delete password flag mismatch for row: ${parts.join(" | ")}`);
}

async function clickDeleteButton(I, parts) {
    await I.executeScript((rowParts) => {
        const row = Array.from(document.querySelectorAll("#myReservationsTable tbody tr"))
            .find((tableRow) => rowParts.every((part) => tableRow.innerText.includes(part)));
        if (row == null) throw new Error(`Reservation row not found: ${rowParts.join(" | ")}`);

        const button = row.querySelector(".reservation-delete-button");
        if (button == null) throw new Error(`Reservation delete button not found: ${rowParts.join(" | ")}`);

        button.click();
    }, parts);
}

async function mockDeletePasswordPrompt(I, password) {
    await I.executeScript((promptPassword) => {
        window.sessionStorage.removeItem("myReservationsPromptMessage");
        window.WJ = window.WJ || {};
        window.WJ.prompt = null;
        window.prompt = (message) => {
            window.sessionStorage.setItem("myReservationsPromptMessage", message);
            return promptPassword;
        };
    }, password);
}
