/**
 * Test of request multipart processing in various scenarious:
 * - forms
 * - spring
 * - stripes
 * - JSP upload forms
 * - admin upload listeners
 */
Feature('admin.multipart');

const fileName = 'ľščťú žýáíéô.png';
const fileNameSanitized = 'lsctu-zyaieo.png';
const filePath = 'tests/admin/'+fileName;
const fileSize = 51918;
let randomNumber;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

function verifyForm(formName, text, I, DT, DTE) {
    I.amOnPage("/apps/form/admin/#/detail/"+formName);
    DT.waitForLoader();
    DT.filterContains("col_meno-a-priezvisko", text);
    I.see("Záznamy 1 až 1", ".dt-info");
    I.see(fileNameSanitized, "#form-detail td.cell-not-editable a");

    DT.addContext("form", "#form-detail_wrapper");
    I.click(DT.btn.form_select_all_button);
    I.click(DT.btn.form_delete_button);
    DTE.waitForEditor("form-detail");
    DTE.save("form-detail");
    DTE.waitForModalClose("form-detail_modal");
}

function submitForm(I, DT, DTE) {
    //spam protection
    I.wait(5);

    I.click("Odoslať", "#formMailForm-multiupload .btn-primary");

    I.waitForElement("#ajaxFormResultContainer");
    I.see("Formulár bol úspešne odoslaný");
}

Scenario('form upload', ({ I, DT, DTE }) => {
    let text = "Single file test "+randomNumber;
    I.amOnPage("/apps/formular/formular-upload.html");
    I.attachFile('input[accept=".gif,.png,.jpg,.jpeg,.svg"]', filePath);
    I.fillField("input[name=meno-a-priezvisko]", text);
    I.fillField("input[name=email]", "**");
    I.fillField("input[name=email]", "tester@balat.sk");
    I.fillField("textarea[name=otazka]", "Otázka ľščťžýáíéôň");

    I.wait(5);
    I.click("Odoslať", "form[name=formMailForm] .btn-primary");

    verifyForm("Formular-upload", text, I, DT, DTE);
});

Scenario('form multiupload', ({ I, DT, DTE }) => {
    let text = "Multipart test "+randomNumber;
    I.amOnPage("/apps/formular-lahko/multiupload.html");
    I.dontSee("Odstrániť súbor");
    I.attachFile('input[accept=".gif,.png,.jpg,.jpeg,.svg"]', filePath);
    I.waitForText("Odstrániť súbor", 5, "a.dz-remove");
    I.seeElement("#nahrat-obrazky-dropzone.dz-started");

    I.fillField("#meno-a-priezvisko", text);

    submitForm(I, DT, DTE);
    verifyForm("multiupload", text, I, DT, DTE);
});

Scenario('spring upload @current', ({ I }) => {
    I.amOnPage("/apps/spring-app/kontakty/");
    I.click("Upraviť", ".table-responsive table tbody tr:nth-child(4) .btn-secondary");
    let text = "Spring upload test "+randomNumber;

    I.seeInField("#name", "Tretia firma a. s.");
    I.fillField("#street", text);
    I.attachFile('input[type="file"]', filePath);

    I.click("Potvrdiť", ".container form .btn-primary");

    I.waitForText("Formulár bol úspešne odoslaný", 5, ".alert-success");
    I.see("Uploaded file: "+fileName+" ("+fileSize+" bytes)", ".alert-info");

    //
    I.say("Verify data");
    I.amOnPage("/apps/spring-app/kontakty/");
    I.click("Upraviť", ".table-responsive table tbody tr:nth-child(4) .btn-secondary");
    I.seeInField("#street", text);
});

Scenario("stripes app in webpage", ({ I, DT, DTE }) => {
    I.amOnPage("/apps/prihlaseny-pouzivatel/moj-profil/moj-profil-file.html");
    let text = "Stripes upload test "+randomNumber;
    I.fillField("#usrSignature", text);
    I.attachFile('#userImageId', filePath);

    I.click("Uložiť", "#regUserFormDiv .btn-primary");

    I.waitForElement(".stripesErrors");

    I.waitForText("Zle zadané staré heslo", 10, ".stripesErrors");
    I.see("Obrázok "+fileName+" (veľkosť "+fileSize+" B) je potrebné znova nastaviť do formuláru po neúspešnom odoslaní formuláru.", ".stripesErrors");
    I.seeInField("#usrSignature", text);
});
