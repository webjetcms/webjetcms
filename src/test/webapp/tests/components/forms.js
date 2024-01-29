Feature('components.forms');

var yearFrom = "2013";
var monthFrom = "September";
var dayFrom = "28";
var yearTo = "2016";
var monthTo = "September";
var dayTo = "28";
var numberOfTotalResults = "1,184";
var numberOfDateBothResults = "550";
var numberOfDateFromResults = "1,175";
var numberOfDateToResults = "559";

var formName = "Elektornicky-formular";
var randomNumber;
var randomNumber2;
const assert = require('assert');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/apps/form/admin/");
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
        randomNumber2 = "2-"+randomNumber;
    }
});

Scenario('zoznam formularov', async ({ I, DT }) => {
    I.see("Názov formuláru");
    within(".active", () => {
        I.see("Zoznam formulárov");
    });
    DT.filter("formName", "Brexit");
    I.click("Brexit");
    DT.waitForLoader("#form-detail_processing");
    const formName = await within("#pills-forms", () => {
        return I.grabTextFrom('.active');
    });
    const url = await I.grabCurrentUrl();
    const formNameFromUrl = url.split('/').pop();
    I.amOnPage(`/apps/form/admin/#/detail/${formName}`);
    assert.equal(formName, formNameFromUrl);
    within("#pills-forms", () => {
        I.click('//li[1]')
        I.wait(1);
        I.see("Zoznam formulárov");
    });
    DT.waitForLoader("#forms-list_processing");
    I.amOnPage("/apps/form/admin/#/");
});

Scenario("vyhladavanie podla oboch datumov", ({ I, DT }) => {
    DT.filter(`formName`, formName);

    I.click(formName);
    DT.waitForLoader("#form-detail_processing");

    within(".dt-footer-row", () => {
        I.see(numberOfTotalResults);
    });

    I.click("input.dt-filter-from-dateTime");
    I.waitForElement("div.dt-datetime", 1);
    I.selectOption({css:".dt-datetime-year"}, yearFrom);
    I.selectOption({css:".dt-datetime-month"}, monthFrom);
    within(".dt-datetime-table", () => {
       I.click(dayFrom);
    });

    I.click("input.dt-filter-to-dateTime");
    I.waitForElement("div.dt-datetime", 1);
    I.selectOption({css:".dt-datetime-year"}, yearTo);
    I.selectOption({css:".dt-datetime-month"}, monthTo);
    within(".dt-datetime-table", () => {
        I.click(dayTo);
    });

    I.pressKey("Enter", "input.dt-filter-to-dateTime");
    DT.waitForLoader("#form-detail_processing");
    within(".dt-footer-row", () => {
        I.see(numberOfDateBothResults);
    });
});

Scenario("vyhladavanie podla datumu od", ({ I, DT }) => {
    I.fillField(`input.dt-filter-formName`, formName);
    I.pressKey('Enter', `input.dt-filter-dt-filter-formName`);

    I.click(formName);
    DT.waitForLoader("#form-detail_processing");

    I.click("input.dt-filter-from-dateTime");
    I.waitForElement("div.dt-datetime", 1);
    I.selectOption({css:".dt-datetime-year"}, yearFrom);
    I.selectOption({css:".dt-datetime-month"}, monthFrom);
    within(".dt-datetime-table", () => {
       I.click(dayFrom);
    });

    I.pressKey("Enter", "input.dt-filter-to-dateTime");
    DT.waitForLoader("#form-detail_processing");
    within(".dt-footer-row", () => {
        I.see(numberOfDateFromResults);
    });
});

Scenario("vyhladavanie podla datumu do", ({ I, DT }) => {
    I.fillField(`input.dt-filter-formName`, formName);
    I.pressKey('Enter', `input.dt-filter-dt-filter-formName`);

    I.click(formName);
    DT.waitForLoader("#form-detail_processing");

    I.click("input.dt-filter-to-dateTime");
    I.waitForElement("div.dt-datetime", 1);
    I.selectOption({css:".dt-datetime-year"}, yearTo);
    I.selectOption({css:".dt-datetime-month"}, monthTo);
    within(".dt-datetime-table", () => {
        I.click(dayTo);
    });

    I.pressKey("Enter", "input.dt-filter-to-dateTime");
    DT.waitForLoader("#form-detail_processing");
    within(".dt-footer-row", () => {
        I.see(numberOfDateToResults);
    });
});

function fillFormSimple(I, random) {
    I.fillField("Meno a priezvisko", "Form-autotest-"+random);
    I.fillField("E-mailová adresa", "fixemail");
    I.fillField("E-mailová adresa", "autotest."+random+"@balat.sk");
    I.fillField("Vaša otázka", "Test odoslania formularu\nrandom: "+random);
    I.click("Súhlas s podmienkami");
    I.click("Odoslať");

    I.waitForElement("#ajaxFormResultContainer");
}

Scenario("vyplnenie formsimple", ({ I }) => {
    I.amOnPage("/apps/formular-lahko/");

    fillFormSimple(I, randomNumber);
    I.see("Formulár bol úspešne odoslaný");

    //over spam ochranu
    I.wait(5);
    fillFormSimple(I, randomNumber2);
    I.dontSee("Formulár bol úspešne odoslaný");
    I.see("Formulár bol detekovaný ako SPAM");

    I.wait(30);

    fillFormSimple(I, randomNumber2);
    I.see("Formulár bol úspešne odoslaný");
});

function checkFormSimpleRowHtml(I, random, rowNumber) {
    I.forceClick("#form-detail tbody tr:nth-child("+rowNumber+") a.form-view");
    I.wait(2);
    I.waitForElement("#modalIframeIframeElement", 10);
    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Meno a priezvisko", 10);
    I.waitForText("E-mailová adresa", 5);
    I.waitForText("Form-autotest-"+random, 5);
    I.waitForText("autotest."+random+"@balat.sk", 5);
    I.switchTo();
    I.click("#modalIframe div.modal-footer button.btn-primary");
}

function checkFormSimpleData(I, DT, random, random2) {
    //over zobrazenie
    DT.filter("col_meno-a-priezvisko", "Form-autotest-"+random);
    I.see(random);
    I.see("Záznamy 1 až 1 z 1");

    //over kliknutie na oko
    checkFormSimpleRowHtml(I, random, 1);

    DT.filter("col_meno-a-priezvisko", "Form-autotest-2-"+random);
    checkFormSimpleRowHtml(I, random2, 1);
}

Scenario("overenie vyplneneho formsimple", ({ I, DT }) => {
    var container = "#form-detail_wrapper";

    I.amOnPage("/apps/form/admin/#/detail/formular-lahko");

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    checkFormSimpleData(I, DT, randomNumber, randomNumber2);

    //BUG: ked sa preplo zobrazenie stlpcov, tak sa nevykonala render funkcia
    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");
    I.click(locate('span.column-title').withText('Meno'));
    I.click("Mesto");
    I.click("Priezvisko");
    I.click("button.btn.btn-primary.dt-close-modal");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    I.amOnPage("/admin/v9/");
    I.amOnPage("/apps/form/admin/#/detail/formular-lahko");

    checkFormSimpleData(I, DT, randomNumber, randomNumber2);

    //over, ze vidime vyplnene udaje testera v tabulke
    I.see("Tester", "#form-detail tbody tr:nth-child(1)");
    I.see("Playwright", "#form-detail tbody tr:nth-child(1)");
});

Scenario("Overenie zoznamu podla prihlaseneho pouzivatela", ({ I, DT }) => {
    //tester
    I.see("formular-lahko");
    I.see("Elektornicky-formular");

    I.wait(3);
    I.logout();
    I.wait(3);

    //prihlas sa ako tester2
    //toto z nejakeho dovodu nefungovalo login('tester2');
    I.amOnPage('/admin/');
    //aby sme vzdy v kazdom scenari mali prednastavenu velkost okna
    //odosli prihlasenie
    I.fillField("username", "tester2");
    I.fillField("password", secret("***REMOVED***"));
    I.click("login-submit");

    I.wait(3);

    I.amOnPage("/apps/form/admin/");
    I.see("formular-lahko");
    I.dontSee("Elektornicky-formular");

    I.click("formular-lahko");
    DT.waitForLoader();
    I.see("Form-autotest-2", "#form-detail");
    I.see("Form-autotest-", "#form-detail");

    //skus form na ktory nemas pravo
    //musime takto aby sa spravil dobre preklik, kedze niekedy to nerefreshlo dobre
    I.amOnPage("/admin/v9/");
    I.amOnPage("/apps/form/admin/#/detail/formular");
    I.see("Na túto operáciu nemáte prístupové právo");

    //odober pravo a vyskusaj zoznam formularov
    I.amOnPage("/apps/form/admin/?removePerm=cmp_form")
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");

    //lebo sme pred tym v teste zmenili usera
    I.logout();
});

Scenario("odhlasenie", ({ I }) => {
    //lebo sme pred tym v teste zmenili domenu
    I.logout();
});


Scenario("vymazanie formsimple", ({ I, DT, DTE }) => {
    I.amOnPage("/apps/form/admin/#/detail/formular-lahko");

    DT.filter("col_meno-a-priezvisko", "Form-autotest-2-"+randomNumber);
    I.see(randomNumber);
    I.see("Záznamy 1 až 1 z 1");

    //oznac zaznam a zmaz ho
    I.click("#form-detail tbody tr:nth-child(1) td.dt-select-td");
    I.click("button.buttons-remove");
    I.waitForElement("#form-detail_modal");
    DTE.save();

    DT.filter("col_meno-a-priezvisko", "Form-autotest-"+randomNumber);
    I.see(randomNumber);

    I.see("Záznamy 1 až 1 z 1");

    //zmaz cely formular
    I.amOnPage("/apps/form/admin/");
    DT.filter("formName", "formular-lahko", "Rovná sa");

    I.click("#forms-list tbody tr:nth-child(1) td.dt-select-td");
    I.click("button.buttons-remove");
    I.waitForElement("#forms-list_modal");
    DTE.save();

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.see("Záznamy 0 až 0 z 0");
});

Scenario("domainId overenie zobrazenia zoznamu", ({ I, DT }) => {
    I.amOnPage("/apps/form/admin/");

    I.see("Dotaznik-spokojnosti-externy");
    I.dontSee("OPLZ");
    DT.filter("formName", "OPLZ");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.see("Záznamy 0 až 0 z 0");

    //prepni domenu
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    //I.click(locate('.dropdown-item').withText("test23.tau27.iway.sk"));
    I.click("#bs-select-1-1");
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");
    I.wait(1);

    I.dontSee("Dotaznik-spokojnosti-externy");
    I.see("OPLZ");
    DT.filter("formName", "OPLZ");
    I.see("Korupcia-OPLZ");
    I.see("Formular-korupcia-OPLZ");
    I.see("Záznamy 1 až 2 z 2");
});

Scenario("domainId odhlasenie", ({ I }) => {
    //lebo sme pred tym v teste zmenili domenu
    I.logout();
});

Scenario("formsimple-encrypted", ({ I }) => {
    I.amOnPage("/apps/formular-lahko/formular-lahko-encrypted.html");

    fillFormSimple(I, randomNumber);
    I.see("Formulár bol úspešne odoslaný");

    //over zobrazenie v admine
    I.amOnPage("/apps/form/admin/#/detail/formular-lahko-encrypted");
    I.see("encrypted-tink-tester_42");

    I.click("#form-detail tbody tr:nth-child(1) a.form-view");
    I.switchTo("#modalIframeIframeElement");
    I.see("encrypted-tink");
    I.dontSee("Technické info");
    I.switchTo();
    I.click("#modalIframe button.btn-close");

    //
    I.say("Setting decrypt key");
    I.amOnPage("/components/crypto/admin/keymanagement");
    I.fillField(".privateKey", "decrypt_key-tink-tester_42:eyJwcmltYXJ5S2V5SWQiOjEzNzAyODYzNDUsImtleSI6W3sia2V5RGF0YSI6eyJ0eXBlVXJsIjoidHlwZS5nb29nbGVhcGlzLmNvbS9nb29nbGUuY3J5cHRvLnRpbmsuRWNpZXNBZWFkSGtkZlByaXZhdGVLZXkiLCJ2YWx1ZSI6IkVxTUJFbHdLQkFnQ0VBTVNVaEpRQ2poMGVYQmxMbWR2YjJkc1pXRndhWE11WTI5dEwyZHZiMmRzWlM1amNubHdkRzh1ZEdsdWF5NUJaWE5EZEhKSWJXRmpRV1ZoWkV0bGVSSVNDZ1lLQWdnUUVCQVNDQW9FQ0FNUUVCQWdHQUVZQVJvZ1RKMWJQYm83MW5ZTk56V1lVZmFITnJsZEJyUkp2ZXF3NHI1My9DMVhtTFFpSVFESU12TWtyUlhQdlNNN2hVaklHblcybWdob1BCd21HaDJKaEFhMEg0MlhWaG9nQ29oTno4bWR5NktBbWFmaCt2T2JUb2pTRkl6Q2VIWE50WS91SnFmNWx6RT0iLCJrZXlNYXRlcmlhbFR5cGUiOiJBU1lNTUVUUklDX1BSSVZBVEUifSwic3RhdHVzIjoiRU5BQkxFRCIsImtleUlkIjoxMzcwMjg2MzQ1LCJvdXRwdXRQcmVmaXhUeXBlIjoiVElOSyJ9XX0K");
    I.click("Nastaviť");

    I.amOnPage("/apps/form/admin/#/detail/formular-lahko-encrypted");
    I.dontSee("encrypted-tink-tester_42");
    I.see("Form-autotest-"+randomNumber);
    I.see("autotest."+randomNumber+"@balat.sk")
    I.see("Test odoslania formularu random: "+randomNumber);
    I.see("true");

    I.click("#form-detail tbody tr:nth-child(1) a.form-view");
    I.switchTo("#modalIframeIframeElement");
    I.dontSee("encrypted-tink");
    I.see("Technické info");
    I.see(randomNumber, "span.form-control.emailInput-text");
    I.switchTo();
    I.click("#modalIframe button.btn-close");
});

Scenario("form with note field", ({ I, DT }) => {
    //BUG: there is problem with name conflict between form field name and system fields eg. note
    I.amOnPage("/apps/form/admin/#/detail/formsimple-with-note-field");
    DT.waitForLoader();

    I.waitForText("Admin poznamka", 20, "#form-detail_wrapper");
    I.see("Druhy zaznam formularu");
    I.see("Toto je poznamka vo formulari z frontendu");

    DT.filter("col_note", "Druhy");
    I.see("Admin poznamka");
    I.see("Druhy zaznam formularu");
    I.dontSee("Toto je poznamka vo formulari z frontendu");

    DT.filter("col_note", "Toto je");
    I.dontSee("Admin poznamka");
    I.dontSee("Druhy zaznam formularu");
    I.see("Toto je poznamka vo formulari z frontendu");

    I.amOnPage("/admin/v9/");
    I.amOnPage("/apps/form/admin/#/detail/formsimple-with-note-field");
    DT.waitForLoader();

    I.waitForText("Admin poznamka", 20, "#form-detail_wrapper");
    DT.filter("note", "Poznamka");
    I.see("Admin poznamka");
    I.see("Druhy zaznam formularu");
    I.dontSee("Toto je poznamka vo formulari z frontendu");
});

function handleDownload(I, fileName, ext, url=null) {
    I.handleDownloads("downloads/"+fileName+"-"+randomNumber+ext);
    if (url==null) I.click(locate("#form-detail td.cell-not-editable a").withText(fileName+ext));
    else {
        I.executeScript((url) => {
            window.location.href=url;
        }, url);
    }
    I.amInPath('../../../build/test/downloads');
    I.waitForFile(fileName+"-"+randomNumber+ext, 30);
}

Scenario("form attachments", async ({ I }) => {

    var url11size = "/apps/forms/admin/attachment/?name=11_size.txt";

    I.amOnPage("/apps/form/admin/#/detail/multiupload");
    I.waitForText("testXlsxAttachmentFile.xlsx", 10);

    handleDownload(I, "testJpgAttachmentFile", ".jpg");

    handleDownload(I, "11_size", ".txt", url11size);

    //
    I.say("Check perms");
    I.logout();
    I.amOnPage("/apps/forms/admin/attachment/?name=23833_testXlsxAttachmentFile.xlsx");
    I.see("Prihlásiť sa");

    //
    I.say("User tester2 doesn't have access right to form Kontaktny_formular")
    I.relogin("tester2");
    I.executeScript((url) => {
        window.location.href=url;
    }, url11size);
    //should be redirected to homepage
    I.see("Vitajte, Tester2 Playwright2");
});

Scenario("odhlasenie2", async ({ I }) => {
    I.logout();
});