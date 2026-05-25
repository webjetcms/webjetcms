Feature('apps.forms.multistep-forms-conditions');

const formName = "multistep_condition_test";
const visibilityConditions = "#pills-dt-formItemsDataTable-visibilityConditions-tab";
const requirementConditions = "#pills-dt-formItemsDataTable-requirementConditions-tab";

const pageUrl = "/apps/multistep-formular/items-conditions-tests.html";

const baseAdminMail = "wjmultistep.conditions.admin";
const baseUserMail = "wjmultistep.conditions.user";

Before(({ I, DT, login }) => {
    login('admin');

    DT.addContext("formSteps", "#formStepsDataTable_wrapper");
    DT.addContext("formItems", "#formItemsDataTable_wrapper");
});


Scenario('Before test preparation', async ({ TempMail }) => {
    await TempMail.destroyInbox(baseAdminMail);
    await TempMail.destroyInbox(baseUserMail);
});

Scenario('Base tests', ({ I, DT, DTE }) => {
    I.amOnPage("/apps/form/admin/form-steps/?formName=" + formName);
    DT.waitForLoader();

    I.say("Test tabs visibility CREATE - do not see conditions tabs");
        I.click(DT.btn.formItems_add_button);
        DTE.waitForEditor("formItemsDataTable");
        I.dontSeeElement(visibilityConditions);
        I.dontSeeElement(requirementConditions);
        DTE.cancel();

    I.say("Test tabs visibility EDIT - see conditions tabs");
        I.clickCss("#formItemsDataTable td.sorting_1");
        I.click(DT.btn.formItems_edit_button);
        DTE.waitForEditor("formItemsDataTable");
        I.seeElement(visibilityConditions);
        I.seeElement(requirementConditions);

    I.say("Tab visibility change by field type");
        DTE.selectOption("fieldType", "Nový riadok (riadkové zobrazenie)");
        I.dontSeeElement(visibilityConditions);
        I.dontSeeElement(requirementConditions);

    DTE.selectOption("fieldType", "Nahrať súbory");
        I.seeElement(visibilityConditions);
        I.dontSeeElement(requirementConditions);

    DTE.selectOption("fieldType", "E-mailová adresa");
        I.seeElement(visibilityConditions);
        I.seeElement(requirementConditions);
});

Scenario('Visibility conditions', ({ I, DT, DTE }) => {
    const visibilityModal = "#datatableFieldDTE_Field_visibilityConditions_modal";
    const visibilityId = "datatableFieldDTE_Field_visibilityConditions";

    testFields(I, DT, DTE, visibilityConditions, visibilityModal, visibilityId);
});

Scenario('Requirement conditions', ({ I, DT, DTE }) => {
    const requirementModal = "#datatableFieldDTE_Field_requirementConditions_modal";
    const requirementId = "datatableFieldDTE_Field_requirementConditions";

    testFields(I, DT, DTE, requirementConditions, requirementModal, requirementId);
});

function testFields(I, DT, DTE, tab, modal, id) {
    const editor = "#pills-dt-editor-formItemsDataTable-Content";

    I.amOnPage("/apps/form/admin/form-steps/?formName=" + formName);
    DT.waitForLoader();

    I.say("Test I see only fields from step 1");
        I.clickCss("#formItemsDataTable td.sorting_1");
        I.click(DT.btn.formItems_edit_button);
        DTE.waitForEditor("formItemsDataTable");
        I.clickCss(tab);

        I.waitForVisible(editor);
        I.clickCss(editor + " button.buttons-create");
        DTE.waitForEditor(id);

        testFieldOptionsVisibililty(I, modal, ["(Krok 1) Pohlavie", "(Krok 1) Email", "(Krok 1) Poznamka"], ["(Krok 2) Adresa", "(Krok 2) Telefónne číslo"], "(Krok 1) Meno");

    I.say("When I select operator that do not use value - like is_empty - I do not see all params to set");
        I.seeElement(".DTE_Field_Name_value");
        I.seeElement(".DTE_Field_Name_caseInsensitive");

        DTE.selectOption("operator", "je prázdne");

        I.dontSeeElement(".DTE_Field_Name_value");
        I.dontSeeElement(".DTE_Field_Name_caseInsensitive");

    I.say("Cant select in condition myself fields");
        DTE.save(id);
        I.waitForText("Podmienka nemôže odkazovať sama na seba.", 5);
        I.clickCss(modal + " button.btn-close-editor");

    I.say("I cant save item when conditions are bound");
        DTE.save();
        I.see("Táto položka formulára sa používa v podmienkach. Naozaj chcete vykonať uloženie? Ak áno, môžete to povoliť v karte 'Pokročilé' zapnutím možnosti 'Uložiť aj pri existujúcich závislých položkách'.");
        I.waitForVisible("#toast-container-webjet");
        I.see("Zoznam závislých položiek:", "#toast-container-webjet .toast-message");
        I.see("(Krok 1) Toto je pre zenu.", "#toast-container-webjet .toast-message");
        I.clickCss("#toast-container-webjet button.toast-close-button");
        I.waitForInvisible("#toast-container-webjet");

    I.say("But you can allow it");
        I.clickCss("#pills-dt-formItemsDataTable-advanced-tab");
        I.checkOption("#DTE_Field_allowSaveWhenCondition_0");
        DTE.save();
        I.dontSee("Táto položka formulára sa používa v podmienkach. Naozaj chcete vykonať uloženie? Ak áno, môžete to povoliť v karte 'Pokročilé' zapnutím možnosti 'Uložiť aj pri existujúcich závislých položkách'.");

    I.say("Field in step 2 can see fields from step 2 and step 1");
        I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("Krok 2") );

        I.clickCss("#formItemsDataTable td.sorting_1");
        I.click(DT.btn.formItems_edit_button);
        DTE.waitForEditor("formItemsDataTable");
        I.clickCss(tab);

        I.waitForVisible(editor);
        I.clickCss(editor + " button.buttons-create");
        DTE.waitForEditor(id);

        testFieldOptionsVisibililty(I, modal, ["(Krok 1) Pohlavie", "(Krok 1) Email", "(Krok 1) Poznamka", "(Krok 2) Adresa", "(Krok 2) Telefónne číslo"], [], "(Krok 2) Meno a priezvisko");
}

function testFieldOptionsVisibililty(I, modal, seeArr = [], dontSeeArr = [], selectStep = null) {
    I.clickCss(modal + " .DTE_Field_Name_itemFormId button.dropdown-toggle");
    I.waitForVisible("div.dropdown-menu.show");

    seeArr.forEach(field => {
        I.seeElement( locate("div.dropdown-menu.show").find(locate("span.text").withText(field)) );
    });

    dontSeeArr.forEach(field => {
        I.dontSeeElement( locate("div.dropdown-menu.show").find(locate("span.text").withText(field)) );
    });

    if(selectStep != null) {
        I.click( locate("div.dropdown-menu.show").find(locate("span.text").withText(selectStep)) );
        I.waitForInvisible("div.dropdown-menu.show");
    }
}

Scenario('TETS CONDITIONS in form', async ({ I, DT, DTE, TempMail }) => {
    I.amOnPage(pageUrl);

    I.waitForText("This is step one.", 10);

    // OR joining of conditions (one condition is EQUAL and another CONTAINS)
    I.say("Dont see Step1 - Meno until -> Step1 - Radiogroup is valB OR valC");
        I.dontSeeElement("#meno-1");

        I.clickCss("input#radiogroup-1-0[value='valA']");
        I.dontSeeElement("#meno-1");

        I.clickCss("input#radiogroup-1-1[value='valB']");
        I.seeElement("#meno-1");

        I.clickCss("input#radiogroup-1-2[value='valC']");
        I.seeElement("#meno-1");

        I.clickCss("input#radiogroup-1-0[value='valA']");
        I.dontSeeElement("#meno-1");

    I.say("Dont see Step1 - Email until -> Step1 - Priezvisko is not_empty AND Step1 - Checkgroup is not_empty AND Step1 - Radiogroup not_contains 'auto'");
        I.dontSeeElement("#email-1");

        I.fillField("input#priezvisko-1", "test");
        I.dontSeeElement("#email-1");

        I.clickCss("input#checkboxgroup-1-0[value='auto']");
        I.dontSeeElement("#email-1");

        I.clickCss("input#checkboxgroup-1-1[value='autobus']");
        I.dontSeeElement("#email-1");

        I.clickCss("input#checkboxgroup-1-2[value='lietadlo']");
        I.dontSeeElement("#email-1");

        I.clickCss("input#checkboxgroup-1-0[value='auto']");
        I.dontSeeElement("#email-1");

        I.clickCss("input#checkboxgroup-1-1[value='autobus']");
        I.seeElement("#email-1");

    I.say(" Dont see Step1 - popiska-1 until - > Step1 - Pohlavie MUZ ... in codition is error where text do not match case BUT we allowed case insensitivity");
        I.dontSeeElement("label[for='popiska-1']");

        I.clickCss("#pohlavie-false-muz");
        I.seeElement("label[for='popiska-1']");

    I.say("Step1 - popiska-1 requires - > Step1 - Pohlavie MUZ ... BUT we amde case error and DONT allowed case insensitivity - sooo field will not be visible");
        I.dontSeeElement("label[for='popiska-2']");

        I.clickCss("#pohlavie-false-zena");
        I.dontSeeElement("label[for='popiska-2']");

    I.say("Check that Meno is required and email is not required");
        I.clickCss("input#radiogroup-1-2[value='valC']"); // show field
        I.seeElement("#meno-1");

    I.clickCss("button[type='submit']");
        I.see("Meno - povinné pole");
        I.see("Poznamka - povinné pole");
        I.dontSee("Email - povinné pole");

    I.say("Hide Meno");
        I.clickCss("input#radiogroup-1-0[value='valA']");
        I.dontSeeElement("#meno-1");

    I.say("Change Step1 - POhlavie to make field Step1 - Email required .... check that required span was added");
        I.dontSeeElement("label[for='email-1'] span.requirement-mark");

        I.clickCss("#pohlavie-false-ine");
        I.seeElement("label[for='email-1'] span.requirement-mark");

    I.clickCss("button[type='submit']");
        I.dontSee("Meno - povinné pole"); // this is hidden so no more required
        I.see("Poznamka - povinné pole"); // this is allways required (not through condition)
        I.see("Email - povinné pole");    // this is required through condition and now condition is met so it is required

    I.say("Fill required and go to next step");
        I.fillField("input#email-1", baseUserMail + TempMail.getTempMailDomain());
        I.fillField("textarea#poznamka-1", "Test poznamka");
        I.clickCss("button[type='submit']");

    I.say("Check we see second step");
        I.waitForText("This is step two.", 10);

    I.say("Check conditions on step 2 - it contains combination with values from step 1");

    I.say("I see Step2 - menopriezvisko-1 because -> Step1 - radiogroup-1 is valA and Step1 - checkgroup EQ lietadlo");
        I.seeElement("input#menopriezvisko-1");

    I.say("I see Step2 - adresa-1 because IF menopriezvisko-1 is Tester_1 OR Tester_2");
        I.dontSeeElement("input#adresa-1");

        I.fillField("input#menopriezvisko-1", "Tester_1");
        I.seeElement("input#adresa-1");

        I.fillField("input#menopriezvisko-1", "Tester_2");
        I.seeElement("input#adresa-1");

        I.fillField("input#menopriezvisko-1", "Tester_3");
        I.dontSeeElement("input#adresa-1");

        //case sensitive
        I.fillField("input#menopriezvisko-1", "tester_1");
        I.dontSeeElement("input#adresa-1");

    I.say("I see Step2 - telefon-1 when -> Step1 - email-1 starts with " + baseUserMail + " (ignore case) AND Step2 - radiogroup-2-1 is selected");
        I.dontSeeElement("input#telefon-1");

        I.clickCss("input#radiogroup-2-0[value='one']");
        I.dontSeeElement("input#telefon-1");

        I.clickCss("input#radiogroup-2-1[value='two']");
        I.seeElement("input#telefon-1");

        I.clickCss("input#radiogroup-2-2[value='three']");
        I.dontSeeElement("input#telefon-1");

    I.say("Test validations");
        I.clickCss("input#radiogroup-2-1[value='two']");
        I.seeElement("input#telefon-1");
        I.clickCss("button[type='submit']");
        I.waitForText("Hodnota () pre pole Telefónne číslo je neplatná", 5);
        I.dontSee("Adresa - povinné pole");

        I.fillField("input#menopriezvisko-1", "Tester_1");
        I.seeElement("input#adresa-1");
        I.clickCss("button[type='submit']");
        I.waitForText("Hodnota () pre pole Telefónne číslo je neplatná", 5);
        I.dontSee("Adresa - povinné pole");

        I.fillField("input#menopriezvisko-1", "Tester_2");
        I.seeElement("input#adresa-1");
        I.clickCss("button[type='submit']");
        I.waitForText("Hodnota () pre pole Telefónne číslo je neplatná", 5);
        I.waitForText("Adresa - povinné pole");

        I.fillField("input#telefon-1", "0123456789");
        I.clickCss("button[type='submit']");
        I.waitForText("Adresa - povinné pole", 5);
        I.dontSee("Hodnota () pre pole Telefónne číslo je neplatná");

        I.fillField("input#menopriezvisko-1", "Tester_X");
        I.dontSeeElement("input#adresa-1");

        I.clickCss("button[type='submit']");
        I.waitForText("Formulár bol úspešne odoslaný", 10);
});

Scenario('Check admin email', async ({ I, DT, DTE, TempMail }) => {
    await TempMail.login(baseAdminMail);
    await TempMail.openLatestEmail();

    I.say("Check admin email with filled answers email CANNOT contain hidden fields by condition");

    const expectedHtml = `
        <form action="/rest/multistep-form/save-form?form-name=multistep_condition_test&amp;step-id=-1" method="post" name="formMailForm-multistep_condition_test"><div><div><p>This is step one.</p></div><div><label for="radiogroup-1">Radiogroup:</label><div><span>[X]</span> <label for="radiogroup-1-0">valA</label></div>
        <div><span>[&nbsp;]</span> <label for="radiogroup-1-1">valB</label></div><div><span>[&nbsp;]</span> <label for="radiogroup-1-2">valC</label></div>
        </div><div><label for="priezvisko-1">Priezvisko:</label> <span>test</span></div><div><label for="checkboxgroup-1">Checkgroup:</label><div><span>[&nbsp;]</span> <label for="checkboxgroup-1-0">auto</label></div>
        <div><span>[&nbsp;]</span> <label for="checkboxgroup-1-1">autobus</label></div><div><span>[X]</span> <label for="checkboxgroup-1-2">lietadlo</label></div>
        </div><div><label for="email-1">Email:</label> <span>wjmultistep.conditions.user@fexpost.com</span></div>
        <div><label>Pohlavie:</label></div><div><span>[&nbsp;]</span> <label for="pohlavie-false-muz">MUZ</label> </div><div><span>[&nbsp;]</span> <label for="pohlavie-false-zena">ZENA</label> </div><div><span>[X]</span> <label for="pohlavie-false-ine">ine</label> </div>
        <div><label for="poznamka-1">Poznamka<span style="color: red;">&nbsp;*</span>:</label> <span style="height: auto;">Test poznamka</span></div></div><hr><div><div><p>This is step two.</p></div><div><label for="menopriezvisko-1">Meno a priezvisko:</label> <span>Tester_X</span></div><div><label for="radiogroup-2">Skupina výberových polí:</label><div><span>[&nbsp;]</span> <label for="radiogroup-2-0">one</label></div>
        <div><span>[X]</span> <label for="radiogroup-2-1">two</label></div><div><span>[&nbsp;]</span> <label for="radiogroup-2-2">three</label></div>
        </div><div><label for="telefon-1">Telefónne číslo:</label> <span>0123456789</span></div></div>  </form>
    `;

    checkEmailWithForm(I, expectedHtml);
    TempMail.closeEmail();
    await TempMail.destroyInbox(baseAdminMail);
});

Scenario('Check user email', async ({ I, DT, DTE, TempMail }) => {
    I.say("We are also checking user email even if its set page because I want to be sure, that emial field is correctly hanled in BE after so many action on it (like conditional visibility and requirement)");

    await TempMail.login(baseUserMail);
    await TempMail.openLatestEmail();

    I.waitForText("YOUR FORM WAS SAVED", 5);

    TempMail.closeEmail();
    await TempMail.destroyInbox(baseUserMail);
});

function checkEmailWithForm(I, expectedHtml) {
    I.executeScript(() => {
        const selector = "form[name='formMailForm-multistep_condition_test']";
        const form = document.querySelector(selector);
        return form ? form.outerHTML : null;
    }).then(actualHtml => {
        I.say("Compare actual vs expected EMAIL form HTML");
        compareTwoHtml(I, actualHtml, expectedHtml);
    });
}

function compareTwoHtml(I, actualHtml, expectedHtml) {
    const normalize = html =>
        html
            .replace(/\s*<br>\s*/gi, '<br>')
            // Remove all HTML attributes from opening tags: <tag attr="x"> -> <tag>
            .replace(/<([a-zA-Z][\w:-]*)(\s[^>]*?)?(\/?)>/g, '<$1$3>')
            .replace(/\s+/g, ' ')
            .replace(/>\s+</g, '><')
            .trim();

         // I.say(normalize(actualHtml));
         // I.say(normalize(expectedHtml));

        I.assertEqual(
            normalize(actualHtml),
            normalize(expectedHtml),
            'Form HTML does not match exactly'
        );
}