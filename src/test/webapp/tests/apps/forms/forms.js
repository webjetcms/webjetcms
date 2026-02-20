Feature('apps.forms.forms');

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
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
        randomNumber2 = "2-"+randomNumber;
    }
});

Scenario('zoznam formularov', async ({ I, DT }) => {
    I.amOnPage("/apps/form/admin/");
    I.see("Názov formuláru");

    I.seeElement(locate('.nav-link.active').withText("Zoznam formulárov"));
    DT.filterContains("formName", "Brexit");
    I.click("Brexit");
    DT.waitForLoader("#formDetailDataTable_processing");
    const formName = await within("#pills-form-details", () => {
        return I.grabTextFrom('.active');
    });
    const url = await I.grabCurrentUrl();
    const formNameFromUrl = url.split('/?formName=').pop();
    I.amOnPage(`/apps/form/admin/detail/?formName=${formName}`);
    assert.equal(formName, formNameFromUrl);
    within("#pills-form-details", () => {
        I.waitForElement(locate('a').withText('Brexit'), 10);
        I.click('//li[1]')
        I.wait(1);
        I.waitForInvisible(locate('a').withText('Brexit'), 10)
    });
    DT.waitForLoader("#formsDataTable_processing");
    I.amOnPage("/apps/form/admin/");
});

Scenario("vyhladavanie podla oboch datumov", ({ I, DT }) => {
    I.amOnPage("/apps/form/admin/");
    DT.filterContains(`formName`, formName);

    I.click(formName);
    DT.waitForLoader("#formDetailDataTable_processing");

    within(".dt-footer-row", () => {
        I.see(numberOfTotalResults);
    });

    I.click("input.dt-filter-from-createDate");
    I.waitForElement("div.dt-datetime", 1);
    I.selectOption({css:".dt-datetime-year"}, yearFrom);
    I.selectOption({css:".dt-datetime-month"}, monthFrom);
    within(".dt-datetime-table", () => {
       I.click(dayFrom);
    });

    I.click("input.dt-filter-to-createDate");
    I.waitForElement("div.dt-datetime", 1);
    I.selectOption({css:".dt-datetime-year"}, yearTo);
    I.selectOption({css:".dt-datetime-month"}, monthTo);
    within(".dt-datetime-table", () => {
        I.click(dayTo);
    });

    I.pressKey("Enter", "input.dt-filter-to-createDate");
    DT.waitForLoader("#formDetailDataTable_processing");
    within(".dt-footer-row", () => {
        I.see(numberOfDateBothResults);
    });
});

Scenario("vyhladavanie podla datumu od", ({ I, DT }) => {
    I.amOnPage("/apps/form/admin/");
    I.fillField(`input.dt-filter-formName`, formName);
    I.pressKey('Enter', `input.dt-filter-dt-filter-formName`);

    I.click(formName);
    DT.waitForLoader("#formDetailDataTable_processing");

    I.click("input.dt-filter-from-createDate");
    I.waitForElement("div.dt-datetime", 1);
    I.selectOption({css:".dt-datetime-year"}, yearFrom);
    I.selectOption({css:".dt-datetime-month"}, monthFrom);
    within(".dt-datetime-table", () => {
       I.click(dayFrom);
    });

    I.pressKey("Enter", "input.dt-filter-to-createDate");
    DT.waitForLoader("#formDetailDataTable_processing");
    within(".dt-footer-row", () => {
        I.see(numberOfDateFromResults);
    });
});

Scenario("vyhladavanie podla datumu do", ({ I, DT }) => {
    I.amOnPage("/apps/form/admin/");
    I.fillField(`input.dt-filter-formName`, formName);
    I.pressKey('Enter', `input.dt-filter-dt-filter-formName`);

    I.click(formName);
    DT.waitForLoader("#formDetailDataTable_processing");

    I.click("input.dt-filter-to-createDate");
    I.waitForElement("div.dt-datetime", 1);
    I.selectOption({css:".dt-datetime-year"}, yearTo);
    I.selectOption({css:".dt-datetime-month"}, monthTo);
    within(".dt-datetime-table", () => {
        I.click(dayTo);
    });

    I.pressKey("Enter", "input.dt-filter-to-createDate");
    DT.waitForLoader("#formDetailDataTable_processing");
    within(".dt-footer-row", () => {
        I.see(numberOfDateToResults);
    });
});

function fillFormSimple(I, DTE, random) {
    if (DTE == null) I.fillField("Meno a priezvisko", "Form-autotest-"+random);
    else I.fillField("Meno a priezvisko", "Form-autotest-"+random+"<b>strong</b>");

    I.fillField("E-mailová adresa", "fixemail");
    I.fillField("E-mailová adresa", "autotest."+random+"@balat.sk");

    if (DTE == null) I.fillField("Vaša otázka", "Test odoslania formularu\nrandom: "+random);
    else {
        DTE.fillCleditor("form.formsimple > div.form-group", "Test odoslania formularu");
        I.pressKey(['CommandOrControl', 'A']);
        I.wait(0.3);
        I.pressKey(['CommandOrControl', 'B'])
        I.wait(0.3);
        I.pressKey('ArrowRight');
        I.wait(0.3);
        I.pressKey('Enter');
        I.pressKey(['CommandOrControl', 'B'])
        I.type("random: "+random, 50);
    }
    I.click("Súhlas s podmienkami");
    I.click("Odoslať");

    I.waitForElement("#ajaxFormResultContainer");
}

Scenario("vyplnenie formsimple", ({ I }) => {
    I.amOnPage("/apps/formular-lahko/");

    fillFormSimple(I, null, randomNumber);
    I.see("Formulár bol úspešne odoslaný");

    //over spam ochranu
    I.wait(5);
    fillFormSimple(I, null, randomNumber2);
    I.dontSee("Formulár bol úspešne odoslaný");
    I.see("Formulár bol detekovaný ako SPAM");

    I.wait(30);

    fillFormSimple(I, null, randomNumber2);
    I.see("Formulár bol úspešne odoslaný");
});

function checkFormSimpleRowHtml(I, random, rowNumber, isWysiwyg=false) {

    I.say("checkFormSimpleRowHtml");
    I.forceClick( locate("#formDetailDataTable tbody tr:nth-child("+rowNumber+") a").withChild("i.ti-eye") );
    I.wait(2);
    I.waitForElement("#modalIframeIframeElement", 10);
    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Meno a priezvisko", 10);
    I.waitForText("E-mailová adresa", 5);
    if (isWysiwyg) I.waitForText("Form-autotest-"+random+"<b>strong</b>", 5);
    else I.waitForText("Form-autotest-"+random, 5);
    I.waitForText("autotest."+random+"@balat.sk", 5);

    if (isWysiwyg) {
        I.seeElement(locate("span.formsimple-wysiwyg b").withText("Test odoslania formularu"));
        I.seeElement(locate("span.formsimple-wysiwyg div").withText("random: "+randomNumber));
    }

    I.switchTo();
    I.clickCss("#modalIframe div.modal-footer button.btn-primary");
}

function

checkFormSimpleData(I, DT, random, random2) {
    I.say("checkFormSimpleData");

    //over zobrazenie
    DT.filterStartsWith("col_meno-a-priezvisko", "Form-autotest-"+random);
    I.see(random);
    I.see("Záznamy 1 až 1 z 1");

    //over kliknutie na oko
    checkFormSimpleRowHtml(I, random, 1);

    DT.filterStartsWith("col_meno-a-priezvisko", "Form-autotest-2-"+random);
    checkFormSimpleRowHtml(I, random2, 1);
}

Scenario("overenie vyplneneho formsimple", ({ I, DT }) => {
    var container = "#formDetailDataTable_wrapper";

    I.amOnPage("/apps/form/admin/detail/?formName=formular-lahko");

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.click("Obnoviť");
    I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");

    checkFormSimpleData(I, DT, randomNumber, randomNumber2);

    //BUG: ked sa preplo zobrazenie stlpcov, tak sa nevykonala render funkcia
    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");
    I.click(locate('span.column-title').withText('Meno'));
    I.click("Mesto");
    I.click("Priezvisko");
    I.click("button.btn.btn-primary.dt-close-modal");
    I.waitForInvisible("div.dt-button-collection div[role=menu] div.dt-button-collection div[role=menu]");

    I.amOnPage("/admin/v9/");
    I.amOnPage("/apps/form/admin/detail/?formName=formular-lahko");

    checkFormSimpleData(I, DT, randomNumber, randomNumber2);

    //over, ze vidime vyplnene udaje testera v tabulke
    I.see("Tester", "#formDetailDataTable tbody tr:nth-child(1)");
    I.see("Playwright", "#formDetailDataTable tbody tr:nth-child(1)");
});

Scenario("Overenie zoznamu podla prihlaseneho pouzivatela", ({ I, DT }) => {
    I.amOnPage("/apps/form/admin/");
    I.resizeWindow(1280, 1110);
    I.amOnPage("/apps/form/admin/");

    //tester
    I.see("formular-lahko");
    I.see("FAQ");

    //prihlas sa ako tester2
    I.relogin("tester2");

    I.amOnPage("/apps/form/admin/");
    I.see("formular-lahko");
    I.dontSee("FAQ");

    I.click("formular-lahko");
    DT.waitForLoader();
    I.see("Form-autotest-2", "#formDetailDataTable");
    I.see("Form-autotest-", "#formDetailDataTable");

    //skus form na ktory nemas pravo
    //musime takto aby sa spravil dobre preklik, kedze niekedy to nerefreshlo dobre
    I.amOnPage("/admin/v9/");
    I.amOnPage("/apps/form/admin/detail/?formName=formular");
    I.see("Na túto operáciu nemáte prístupové právo");

    //odober pravo a vyskusaj zoznam formularov
    I.amOnPage("/apps/form/admin/?removePerm=cmp_form")
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");

    //lebo sme pred tym v teste zmenili usera
    I.wjSetDefaultWindowSize();
    I.logout();
});

Scenario("odhlasenie", ({ I }) => {
    //lebo sme pred tym v teste zmenili domenu
    I.logout();
});

Scenario("vymazanie formsimple", ({ I, DT, DTE }) => {
    I.amOnPage("/apps/form/admin/detail/?formName=formular-lahko");

    DT.filterStartsWith("col_meno-a-priezvisko", "Form-autotest-2-"+randomNumber);
    I.see(randomNumber);
    I.see("Záznamy 1 až 1 z 1");

    //oznac zaznam a zmaz ho
    I.clickCss("#formDetailDataTable tbody tr:nth-child(1) td.dt-select-td");
    I.click("button.buttons-remove");
    I.waitForElement("#formDetailDataTable_modal");
    DTE.save();

    DT.filterStartsWith("col_meno-a-priezvisko", "Form-autotest-"+randomNumber);
    I.see(randomNumber);

    I.see("Záznamy 1 až 1 z 1");

    //zmaz cely formular
    I.amOnPage("/apps/form/admin/");
    DT.filterEquals("formName", "formular-lahko");

    I.clickCss("#formsDataTable tbody tr:nth-child(1) td.dt-select-td");
    I.click("button.buttons-remove");
    I.waitForElement("#formsDataTable_modal");
    DTE.save();

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.see("Záznamy 0 až 0 z 0");
});

Scenario("domainId overenie zobrazenia zoznamu", ({ I, DT, Document }) => {
    I.amOnPage("/apps/form/admin/");
    I.amOnPage("/apps/form/admin/");

    I.see("formular-lahko-encrypted");
    I.dontSee("OPLZ");
    DT.filterContains("formName", "OPLZ");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    I.see("Záznamy 0 až 0 z 0");

    //prepni domenu
    Document.switchDomain("test23.tau27.iway.sk");

    I.dontSee("Dotaznik-spokojnosti-externy");
    I.see("OPLZ");
    DT.filterContains("formName", "OPLZ");
    I.see("Korupcia-OPLZ");
    I.see("Formular-korupcia-OPLZ");
    I.see("Záznamy 1 až 2 z 2");

    I.wjSetDefaultWindowSize();
});

Scenario("domainId odhlasenie", ({ I }) => {
    //lebo sme pred tym v teste zmenili domenu
    I.logout();
});

Scenario("formsimple-encrypted", ({ I }) => {
    I.amOnPage("/apps/formular-lahko/formular-lahko-encrypted.html");

    fillFormSimple(I, null, randomNumber);
    I.see("Formulár bol úspešne odoslaný");

    //over zobrazenie v admine
    I.amOnPage("/apps/form/admin/detail/?formName=formular-lahko-encrypted");
    I.see("encrypted-tink-tester_42");

    I.click( locate("#formDetailDataTable tbody tr:nth-child(1) a").withChild("i.ti-eye") );
    I.switchTo("#modalIframeIframeElement");
    I.see("encrypted-tink");
    I.dontSee("Technické info");
    I.switchTo()

    //
    I.say("Setting decrypt key");
    I.amOnPage("/components/crypto/admin/keymanagement");
    I.fillField(".privateKey", "decrypt_key-tink-tester_42:eyJwcmltYXJ5S2V5SWQiOjEzNzAyODYzNDUsImtleSI6W3sia2V5RGF0YSI6eyJ0eXBlVXJsIjoidHlwZS5nb29nbGVhcGlzLmNvbS9nb29nbGUuY3J5cHRvLnRpbmsuRWNpZXNBZWFkSGtkZlByaXZhdGVLZXkiLCJ2YWx1ZSI6IkVxTUJFbHdLQkFnQ0VBTVNVaEpRQ2poMGVYQmxMbWR2YjJkc1pXRndhWE11WTI5dEwyZHZiMmRzWlM1amNubHdkRzh1ZEdsdWF5NUJaWE5EZEhKSWJXRmpRV1ZoWkV0bGVSSVNDZ1lLQWdnUUVCQVNDQW9FQ0FNUUVCQWdHQUVZQVJvZ1RKMWJQYm83MW5ZTk56V1lVZmFITnJsZEJyUkp2ZXF3NHI1My9DMVhtTFFpSVFESU12TWtyUlhQdlNNN2hVaklHblcybWdob1BCd21HaDJKaEFhMEg0MlhWaG9nQ29oTno4bWR5NktBbWFmaCt2T2JUb2pTRkl6Q2VIWE50WS91SnFmNWx6RT0iLCJrZXlNYXRlcmlhbFR5cGUiOiJBU1lNTUVUUklDX1BSSVZBVEUifSwic3RhdHVzIjoiRU5BQkxFRCIsImtleUlkIjoxMzcwMjg2MzQ1LCJvdXRwdXRQcmVmaXhUeXBlIjoiVElOSyJ9XX0K");
    I.click("Nastaviť");

    I.amOnPage("/apps/form/admin/detail/?formName=formular-lahko-encrypted");
    I.dontSee("encrypted-tink-tester_42");
    I.see("Form-autotest-"+randomNumber);
    I.see("autotest."+randomNumber+"@balat.sk")
    I.see("Test odoslania formularu random: "+randomNumber);
    I.see("true");

    I.click( locate("#formDetailDataTable tbody tr:nth-child(1) a").withChild("i.ti-eye") );
    I.switchTo("#modalIframeIframeElement");
    I.dontSee("encrypted-tink");
    I.see("Technické info");
    I.see(randomNumber, "span.form-control.emailInput-text");
    I.switchTo();
    I.see("Vytlačiť", "#modalIframe div.modal-footer button");
});

Scenario("form with note field", ({ I, DT }) => {
    //BUG: there is problem with name conflict between form field name and system fields eg. note
    I.amOnPage("/apps/form/admin/detail/?formName=formsimple-with-note-field");
    DT.waitForLoader();

    I.waitForText("Admin poznamka", 20, "#formDetailDataTable_wrapper");
    I.see("Druhy zaznam formularu");
    I.see("Toto je poznamka vo formulari z frontendu");

    DT.filterStartsWith("col_note", "Druhy");
    I.see("Admin poznamka");
    I.see("Druhy zaznam formularu");
    I.dontSee("Toto je poznamka vo formulari z frontendu");

    DT.filterStartsWith("col_note", "Toto je");
    I.dontSee("Admin poznamka");
    I.dontSee("Druhy zaznam formularu");
    I.see("Toto je poznamka vo formulari z frontendu");

    I.amOnPage("/admin/v9/");
    I.amOnPage("/apps/form/admin/detail/?formName=formsimple-with-note-field");
    DT.waitForLoader();

    I.waitForText("Admin poznamka", 20, "#formDetailDataTable_wrapper");
    DT.filterContains("note", "Poznamka");
    I.see("Admin poznamka");
    I.see("Druhy zaznam formularu");
    I.dontSee("Toto je poznamka vo formulari z frontendu");
});

async function handleDownload(I, fileName, ext, url=null) {
    await I.handleDownloads("downloads/"+fileName+"-"+randomNumber+ext);
    if (url==null) I.click(locate("#formDetailDataTable td.cell-not-editable a").withText(fileName+ext));
    else {
        await I.executeScript((url) => {
            window.location.href=url;
        }, url);
    }
    //I.amInPath('../../../build/test/downloads');
    await I.waitForFile('../../../build/test/downloads/'+fileName+"-"+randomNumber+ext, 30);
}

Scenario("form attachments", async ({ I }) => {

    var url11size = "/apps/forms/admin/attachment/?name=11_size.txt";

    I.amOnPage("/apps/form/admin/detail/?formName=multiupload");
    I.waitForText("testXlsxAttachmentFile.xlsx", 10);

    await handleDownload(I, "testJpgAttachmentFile", ".jpg");

    await handleDownload(I, "11_size", ".txt", url11size);

    //
    I.say("Check perms");
    I.logout();
    I.amOnPage("/apps/forms/admin/attachment/?name=23833_testXlsxAttachmentFile.xlsx");
    I.see("Prihlásiť sa");

    //
    I.say("User tester2 doesn't have access right to form Kontaktny_formular")
    I.relogin("tester2");
    await I.executeScript((url) => {
        window.location.href=url;
    }, url11size);
    //should be redirected to homepage
    I.see("Vitajte, Tester2 Playwright2");
});

Scenario("odhlasenie2", async ({ I }) => {
    I.logout();
});

Scenario("BUG switch tabs by arrow key", ({ I, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=87145");
    DTE.waitForEditor();
    I.waitForElement("a.cke_button__wjforms", 10);
    I.wait(1);
    I.clickCss("a.cke_button__wjforms");
    I.waitForElement("iframe.cke_panel_frame");
    I.switchTo("iframe.cke_panel_frame");
    I.click("Formulár");
    I.switchTo();

    I.click(locate("div.cke_dialog_tabs a.cke_dialog_tab").withText("Rozšírené nastavenia"));

    I.waitForElement(locate("input[name=attribute_subject]"));
    I.fillField(locate("input[name=attribute_subject]"), "test");
    I.pressKey("ArrowLeft");
    //
    I.say("Confirm that the tab was not changed");
    I.seeElement(locate("div.cke_dialog_tabs a.cke_dialog_tab.cke_dialog_tab_selected").withText("Rozšírené nastavenia"));

    //
    I.click(locate("div.cke_dialog_tabs a.cke_dialog_tab").withText("Limity na súbory"));

    I.fillField(locate("input[name=attribute_allowedExtensions]"), "docx");
    I.pressKey("ArrowLeft");
    //
    I.say("Confirm that the tab was not changed");
    I.seeElement(locate("div.cke_dialog_tabs a.cke_dialog_tab.cke_dialog_tab_selected").withText("Limity na súbory"));

    I.click(locate("table.cke_dialog_contents td.cke_dialog_footer a").withText("Zrušiť"));
    DTE.cancel();
});

Scenario("formsimple-wysiwyg", ({ I, DT, DTE }) => {
    I.amOnPage("/apps/formular-lahko/formular-lahko-wysiwyg.html");
    I.wait(30); //prevent spam protection

    fillFormSimple(I, DTE, randomNumber);
    I.see("Formulár bol úspešne odoslaný");

    //
    I.say("Check the form in the admin");
    I.amOnPage("/apps/form/admin/detail/?formName=formsimple-wysiwyg");
    DT.waitForLoader();
    I.waitForText("Form-autotest-"+randomNumber+"<b>strong</b>", 10);
    I.seeElement(locate("td.cell-not-editable div.datatable-column-width b").withText("Test odoslania formularu"));
    I.seeElement(locate("td.cell-not-editable div.datatable-column-width div").withText("random: "+randomNumber));

    //
    I.say("Verify HTML/email version");
    DT.filterStartsWith("col_meno-a-priezvisko", "Form-autotest-"+randomNumber);
    checkFormSimpleRowHtml(I, randomNumber, 1, true);

    //
    I.say("Delete form record");
    I.clickCss("#formDetailDataTable tbody tr:nth-child(1) td.dt-select-td");
    I.click("button.buttons-remove");
    I.waitForElement("#formDetailDataTable_modal");
    DTE.save();
});

Scenario("BUG double opt in column in admin not shown", ({ I, DT }) => {
    I.amOnPage("/apps/form/admin/detail/?formName=Formular-doubleoptin");
    I.waitForText("Dátum potvrdenia súhlasu", 10, "th.dt-th-doubleOptinConfirmationDate");
    I.see("06.06.2024 11:11:11", "td.cell-not-editable div");
    I.see("06.06.2024 11:20:20", "td.cell-not-editable div");

    //
    I.say("Filter by date");
    DT.filterContains("from-doubleOptinConfirmationDate", "06.06.2024 11:15");
    I.see("06.06.2024 11:20:20", "td.cell-not-editable div");
    I.dontSee("06.06.2024 11:11:11", "td.cell-not-editable div");

    //verify that the column is not shown in the normal form
    I.amOnPage("/admin/v9/");
    I.amOnPage("/apps/form/admin/detail/?formName=Kontakt");
    I.waitForText("Dátum posledného exportu", 10, "th.dt-th-lastExportDate");
    I.dontSee("Dátum potvrdenia súhlasu");
});

Scenario("vyplnenie formsimple spamProtection=false @singlethread", ({ I, Document }) => {
    //BUG: spamProtection=false required CSRF token, which was not generated
    Document.setConfigValue("spamProtection", "false");
    I.amOnPage("/apps/formular-lahko/formular-lahko-encrypted.html");
    randomNumber = I.getRandomText();

    fillFormSimple(I, null, randomNumber);
    I.waitForText("Formulár bol úspešne odoslaný", 10);
    I.dontSee("Formulár bol detekovaný ako SPAM");

    Document.setConfigValue("spamProtection", "true");
});

Scenario("vyplnenie formsimple spamProtection=false - revert config @singlethread", ({ I, Document }) => {
    Document.setConfigValue("spamProtection", "true");
});