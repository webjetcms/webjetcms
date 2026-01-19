Feature('apps.multistep-forms');

var randomNumber;
var newMultistepFormName;
Before(({ I, DT, login }) => {
    login('admin');
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
        newMultistepFormName = "Multistepform_" + randomNumber;
    }

    DT.addContext("formSteps", "#formStepsDataTable_wrapper");
    DT.addContext("formItems", "#formItemsDataTable_wrapper");
});

Scenario('Check editor tabs', ({ I, DT, DTE }) => {
    I.amOnPage("/apps/form/admin/");

    DT.filterEquals("formName", "Kontaktny_formular");
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-edit");
    DTE.waitForEditor("formsDataTable");

    checkEditorTabVsibility(I, ["basic", "settings-basic", "settings-email", "settings-advanced", "settings-deprecated"], []);
    DTE.cancel();

    DT.filterEquals("formName", "Multistepform_1");
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-edit");
    DTE.waitForEditor("formsDataTable");

    checkEditorTabVsibility(I, ["basic", "settings-basic", "settings-email", "settings-advanced"], ["settings-deprecated"]);
});

Scenario('Base multistep form ', ({ I, DT, DTE }) => {
    I.amOnPage("/apps/form/admin/");

    checkNavigationTabVsibility(I, ["Zoznam formulárov", "Archív formulárov", "Regulárne výrazy"], ["Obsah formuláru"]);

    I.say("Create new multistep form");
    I.click("button.buttons-create");
    DTE.waitForEditor("formsDataTable");

    checkEditorTabVsibility(I, ["basic", "settings-basic", "settings-email", "settings-advanced"], ["settings-deprecated"]);

    DTE.fillField("formName", "Multistepform_1");
    DTE.save();
    I.waitForText("Názov formuláru musí byť jedinečný.", 5);

    DTE.fillField("formName", newMultistepFormName);
    DTE.save();

    DT.filterEquals("formName", newMultistepFormName);
    I.click( locate("a").withChild("i.ti-eye") );
    I.waitForVisible("#formDetailDataTable");

    I.say("Check form detail page");
    checkNavigationTabVsibility(I, ["Zoznam formulárov", "Archív formulárov", "Regulárne výrazy", "Obsah formuláru"], []);
    I.seeElement( locate("#pills-form-details > li > a.nav-link.active").withText(newMultistepFormName) );
    I.see("Nenašli sa žiadne vyhovujúce záznamy"); // no filled answers yet

    I.say("Check default form content");
    I.click( locate("ul.nav > li.nav-item > a.nav-link").withText("Obsah formuláru") );
    I.waitForVisible("div.stepPreviewWrapper");

    //Default step
    I.seeElement( locate("table#formStepsDataTable > tbody > tr > td").withText("DEFAULT") );
    //No item
    I.seeElement( locate("table#formItemsDataTable > tbody > tr > td").withText("Nenašli sa žiadne vyhovujúce záznamy") );
});

Scenario('Fill and test form content', async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/form/admin/form-content/?formName=" + newMultistepFormName);

    I.say("Edit first default step");
    I.waitForVisible("#formStepsDataTable_wrapper");

    I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("DEFAULT") );
    I.click(DT.btn.formSteps_edit_button);
    DTE.waitForEditor("formStepsDataTable");

    I.fillField("#DTE_Field_stepName", "1 - Primárny nadpis");
    I.fillField("#DTE_Field_stepSubName", "1 - Sekundárny nadpis");
    DTE.save();

    I.seeElement( locate("table#formStepsDataTable > tbody > tr > td").withText("1 - Primárny nadpis") );

    I.say("Add elements to the first step");
    createAndFillFormItem(I, DT, DTE, 'Meno', true, "Vase meno", "!LOGGED_USER_FIRSTNAME!", "Vase prve meno", null);
    createAndFillFormItem(I, DT, DTE, 'Priezvisko', false, "Vase priezvisko", "!LOGGED_USER_LASTNAME!", "Vase rodinne meno", null);
    createAndFillFormItem(I, DT, DTE, 'E-mailová adresa', true, "Emailova adresa", null, null, "nieco@interway.sk");
    createAndFillFormItem(I, DT, DTE, 'Skupina zaškrtávacích polí', false, null, "A,B,C", null, null);
    createAndFillFormItem(I, DT, DTE, 'Skupina výberových polí', false, null, "D,E,F", null, null);

    I.say("Test genearted preview of the first step - using screenshot compare");
    I.resizeWindow(1920, 1080);
    await Document.compareScreenshotElement("div.stepPreviewWrapper > div.stepPreview", "multistep-form/multistep-form-step-1.png", null, null, 5);

    I.say("Add second step");
    I.click(DT.btn.formSteps_add_button);
    DTE.waitForEditor("formStepsDataTable");

    I.fillField("#DTE_Field_stepName", "2 - Druhy krok");
    I.fillField("#DTE_Field_stepSubName", "2 - Sekundárny nadpis druheho kroku");
    DTE.save();

    I.seeElement( locate("table#formStepsDataTable > tbody > tr > td").withText("2 - Druhy krok") );

    I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("2 - Druhy krok") );
    I.say("Add elements to the second step");
    createAndFillFormItem(I, DT, DTE, 'Nahrať obrázky', null, "Pridajte obrazky", null, null, null);
    createAndFillFormItem(I, DT, DTE, 'Výberový zoznam - select', false, "Select pole", "A,B,C,D", "zoznam tooltip", null);
    createAndFillFormItem(I, DT, DTE, 'Formátované textové pole', true, "WYSIWYG", "happy wysiwyg placeholder", "wysiwyg tooltip", null);

    I.resizeWindow(1920, 1080);
    await Document.compareScreenshotElement("div.stepPreviewWrapper > div.stepPreview", "multistep-form/multistep-form-step-2.png", null, null, 5);
});

Scenario('Insert multistep into page and test it', async ({ I, DTE, Document, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=128791");

    // Set new multistep form as form for the page
    DTE.waitForEditor();
    Apps.openAppEditor();
    DTE.selectOption("formName", newMultistepFormName);
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    DTE.save();

    I.say("Test visual of step one");
    I.amOnPage("/apps/multistep-formular/app-insert-test.html");
    I.resizeWindow(1920, 1080);
    await Document.compareScreenshotElement("div.multistep-form-app", "multistep-form/multistep-form-page-step-1.png", null, null, 5);

    I.say("Test and submit step 1");
    I.clickCss("button[type='submit']");
    I.waitForText("Emailova adresa - povinné pole");
    I.fillField("#email-1", "sivan@fexpost.com");
    // chebbox group
    I.clickCss("#checkboxgroup-1-0");
    I.clickCss("#checkboxgroup-1-1");
    // radio button group
    I.clickCss("#radiogroup-1-1");
    I.clickCss("#radiogroup-1-2");
    //submit
    I.clickCss("button[type='submit']");

    I.say("Test visual of step one");
    I.waitForVisible("#multiupload_images-1-dropzone");
    I.resizeWindow(1920, 1080);
    await Document.compareScreenshotElement("div.multistep-form-app", "multistep-form/multistep-form-page-step-2.png", null, null, 5);

    I.say("Test and submit step 2 - final");

    I.dontSee("Odstrániť súbor");
    I.attachFile('input[accept=".gif,.png,.jpg,.jpeg,.svg"]', 'tests/apps/penguin.jpg');
    I.waitForText("Odstrániť súbor", 5, "a.dz-remove");

    // Try second same image - save should be blocked with error
    I.attachFile('input[accept=".gif,.png,.jpg,.jpeg,.svg"]', 'tests/apps/penguin.jpg');
    I.wait(5);

    I.selectOption("#select-1", "C");

    //submit
    I.clickCss("button[type='submit']");

    I.see("Súbor penguin.jpg bol nahratý viac krát.");

    // remove one image
    I.clickCss("a.dz-remove");

    //submit
    I.clickCss("button[type='submit']");
    I.waitForText("Formulár bol úspešne odoslaný");
});

Scenario('Test form detail and filled data', ({ I, DT, DTE }) => {

    I.amOnPage("/apps/form/admin/detail/?formName=" + newMultistepFormName);

    I.see("Záznamy 1 až 1 z 1");

    const columnNames = [
        { name: "meno 1", value: "Tester" },
        { name: "priezvisko 1", value: "Playwright" },
        { name: "email 1", value: "sivan@fexpost.com" },
        { name: "checkboxgroup 1", value: "A,B" },
        { name: "radiogroup 1", value: "F" },
        { name: "multiupload images 1 fileNames", value: "" },
        { name: "select 1", value: "C" },
        { name: "wysiwyg 1", value: "happy wysiwyg placeholder" }
    ];

    // Check column headers
    columnNames.forEach(({ name }) => { I.seeElement(locate("span.dt-column-title").withText(name)); });

    // Check values in table row (with 6 leading utility columns)
    const expectedRow = Array(5).fill("").concat("penguin.jpg").concat(columnNames.map(c => c.value));
    DT.checkTableRow("formDetailDataTable", 1, expectedRow);

    // Check values in editor
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-edit");
    DTE.waitForEditor("formDetailDataTable");

    I.clickCss("#pills-dt-formDetailDataTable-content-tab");
    columnNames.forEach(({ name, value }) => {
        if (value !== "") {
            I.seeInField("#DTE_Field_col_" + name.replace(/\s+/g, "-"), value);
        }
    });

    DTE.cancel();

    const expectedHtml = `
    <div class="form-step"> <p class="step-primaryHeader">1 - Primárny nadpis</p> <p class="step-secondaryHeader">1 - Sekundárny nadpis</p><div class="form-group mb-3"><label for="meno-1"><p>Vase meno</p>&nbsp;*:</label> <span class="form-control emailInput-text">Tester</span></div><div class="form-group mb-3"><label for="priezvisko-1"><p>Vase priezvisko</p>:</label> <span class="form-control emailInput-text">Playwright</span></div><div class="form-group mb-3"><label for="email-1"><p>Emailova adresa</p>&nbsp;*:</label> <span class="form-control emailInput-text">sivan@fexpost.com</span></div><div class="form-group mb-3"><label for="checkboxgroup-1">Skupina zaškrtávacích polí:</label><div class="form-check"><span class="inputcheckbox emailinput-cb input-checked">[X]</span> <label for="checkboxgroup-1-0" class="form-check-label">A</label></div>
    <div class="form-check"><span class="inputcheckbox emailinput-cb input-checked">[X]</span> <label for="checkboxgroup-1-1" class="form-check-label">B</label></div>
    <div class="form-check"><span class="inputcheckbox emailinput-cb input-unchecked">[&nbsp;]</span> <label for="checkboxgroup-1-2" class="form-check-label">C</label></div>
    </div><div class="form-group mb-3"><label for="radiogroup-1">Skupina výberových polí:</label><div class="form-check"><span class="inputradio emailinput-radio input-unchecked">[&nbsp;]</span> <label for="radiogroup-1-0" class="form-check-label">D</label></div>
    <div class="form-check"><span class="inputradio emailinput-radio input-unchecked">[&nbsp;]</span> <label for="radiogroup-1-1" class="form-check-label">E</label></div>
    <div class="form-check"><span class="inputradio emailinput-radio input-checked">[X]</span> <label for="radiogroup-1-2" class="form-check-label">F</label></div>
    </div></div><div class="form-step"> <p class="step-primaryHeader">2 - Druhy krok</p> <p class="step-secondaryHeader">2 - Sekundárny nadpis druheho kroku</p><div class="form-group mb-3"><label for="multiupload_images-1"><p>Pridajte obrazky</p>:</label> <span class="form-control emailInput-text">164214_penguin.jpg</span> </div><div class="form-group mb-3"><label for="select-1"><p>Select pole</p>:</label><span class="form-control emailInput-select">C</span></div><div class="form-group mb-3"><label for="wysiwyg-1"><p>WYSIWYG</p>&nbsp;*:</label> <span class="form-control emailInput-textarea" style="height: auto;">happy wysiwyg placeholder</span></div></div>
    `;

    checkSubmitedFormPreview(I, expectedHtml);
});

Scenario('Remove form and test it', ({ I, DT }) => {
    I.amOnPage("/apps/form/admin/");

    DT.filterEquals("formName", newMultistepFormName);
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.waitForText("Naozaj chcete zmazať položku?", 5);
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Insert and test multiple forms in one page - before clear page', ({ I, DTE, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=128792");
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    Apps.switchEditor('html');

    I.clickCss("div.CodeMirror-lines");

    I.pressKey(['CommandOrControl', 'A']);
    I.wait(0.3);

    I.pressKey(['Backspace']);
    I.wait(0.3);

    DTE.save();
});

/**
 * Why we allways adding 2 same apps in one page AND do not used allready prepared page?
 * Because we need to test that APP editor will add unique datetime param for each app instance - this prevent app intefering (every app must be init separetly to have its own thymeleaf model instance).
 */
Scenario('Insert and test multiple forms in one page - insert two same apps', ({ I, DTE, Apps }) => {
    I.say("Insert FIRST app");
        Apps.insertApp('Viackrokový formulár', '#multistep_form-title', 128792 , false);

        I.switchTo('.cke_dialog_ui_iframe');
        I.waitForElement('#editorComponent', 10);
        I.switchTo('#editorComponent');

        DTE.selectOption("formName", "Multistepform_light");
        I.switchTo();
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok');

        DTE.save();

    I.say("Insert SECOND app");
        Apps.insertApp('Viackrokový formulár', '#multistep_form-title', 128792 , false);

        I.switchTo('.cke_dialog_ui_iframe');
        I.waitForElement('#editorComponent', 10);
        I.switchTo('#editorComponent');

        DTE.selectOption("formName", "Multistepform_light");
        I.switchTo();
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok');

        DTE.save();
});

/**
 * Form id contains special characters (like ==) - need to be escaped for CSS selector
 * @param {*} id
 * @returns
 */
function escapeCssId(id) {
    return id.replace(/([!"#$%&'()*+,./:;<=>?@[\\\]^`{|}~])/g, '\\$1');
}

const nameRequiredError = "MENO - povinné pole";
const nameMinLengthError = "MENO musí obsahovať minimálne 3 znakov";
const formSendSuccessMessage = "Formulár bol úspešne odoslaný";
Scenario('Insert and test multiple forms in one page - test apps impedepent behaviour', async ({ I }) => {
    I.amOnPage("/apps/multistep-formular/app-multi-insert-test.html");

    I.say("Test number of apps on the page");
    const formsCount = await I.grabNumberOfVisibleElements("div.multistep-form-app");
    I.assertEqual(formsCount, 2, "There should be 2 multistep forms on the page");

    // First get form ids
    const formIds = await I.grabAttributeFromAll('div.multistep-form-app', 'id');
    const escapedFormIds = formIds.map(id => escapeCssId(id));

    I.say("Test that one app do not interfere with the second one - fill and submit first form");

    I.say('Do submit first form');
    I.click("#" + escapedFormIds[0] + " button[type='submit']");

    I.say("Check that first form shows validation error and second form is untouched");
    I.waitForText(nameRequiredError, 5, locate("#" + escapedFormIds[0]));
    I.waitForText(nameMinLengthError, 5, locate("#" + escapedFormIds[0]));
    I.dontSee(nameRequiredError, locate("#" + escapedFormIds[1]));
    I.dontSee(nameMinLengthError, locate("#" + escapedFormIds[1]));

    I.say("DO another change and check");
    I.fillField("#" + escapedFormIds[0] + " input[name='meno-1']", "X");
    I.click("#" + escapedFormIds[0] + " button[type='submit']");
    I.wait(1);

    I.dontSee(nameRequiredError, locate("#" + escapedFormIds[0]));
    I.waitForText(nameMinLengthError, locate("#" + escapedFormIds[0]));
    I.dontSee(nameRequiredError, locate("#" + escapedFormIds[1]));
    I.dontSee(nameMinLengthError, locate("#" + escapedFormIds[1]));

    I.say("Now submit second form and test both forms");
    I.fillField("#" + escapedFormIds[1] + " input[name='meno-1']", "KOKOS");
    I.click("#" + escapedFormIds[1] + " button[type='submit']");
    I.wait(1);

    I.waitForText(formSendSuccessMessage, 5, locate("#" + escapedFormIds[1] + " div.alert.alert-success"));
    I.dontSee(nameRequiredError, locate("#" + escapedFormIds[1]));
    I.dontSee(nameMinLengthError, locate("#" + escapedFormIds[1]));
    I.dontSee(formSendSuccessMessage, locate("#" + escapedFormIds[0]));
    I.dontSee(nameRequiredError, locate("#" + escapedFormIds[0]));
    I.waitForText(nameMinLengthError, 5, locate("#" + escapedFormIds[0]));


    I.say('Now fix first form and submit it');
    I.fillField("#" + escapedFormIds[0] + " input[name='meno-1']", "XYZ");
    I.click("#" + escapedFormIds[0] + " button[type='submit']");
    I.wait(1);

    I.say("BUT it must be detected as spam because both forms where submitted too fast - check spam message AND second form is untouched");
    I.waitForText("Nastala chyba pri spracovani sekcie", 5, locate("#" + escapedFormIds[0] + " div.alert.alert-danger"));
    I.waitForText(formSendSuccessMessage, 5, locate("#" + escapedFormIds[1] + " div.alert.alert-success"));
});

Scenario('RowView version - test appearance', async ({ I, DT, Document }) => {
    I.say("Test generated preview");
    I.amOnPage("/apps/form/admin/form-content/?formName=Multistepform_rowView");
    I.waitForVisible("#formStepsDataTable_wrapper");

    I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("Only step") );
    I.waitForText("This form is used to check render of rowView multistep form", 5, locate("div.stepPreview"));

    I.resizeWindow(1920, 1080);
    await Document.compareScreenshotElement("div.stepPreviewWrapper > div.stepPreview", "multistep-form/multistep-form-rowView.png", null, null, 5);

    I.say("Test visual of rowView form on page");
    I.amOnPage("/apps/multistep-formular/rowviewversion.html");
    I.waitForVisible("div.multistep-form-app");

    I.resizeWindow(1920, 1080);
    await Document.compareScreenshotElement("div.multistep-form-app", "multistep-form/multistep-form-page-rowView.png", null, null, 5);

    I.say("Submit rowView form, so we can test generated filled version");
    I.fillField("#meno-1", "Vlad");
    I.fillField("#priezvisko-1", "Priezvisko-" + randomNumber);
    I.fillField("#email-1", "test@balat.sk");
    I.fillField("#email-1", "test@balat.sk");
    I.fillField("#adresa-1", "Askaban");
    I.clickCss("button[type='submit']");
    I.waitForText("Formulár bol úspešne odoslaný");

    I.say('Test generated filled version of rowView form');
    I.amOnPage("/apps/form/admin/detail/?formName=Multistepform_rowView");

    DT.filterStartsWith("col_priezvisko-1", "Priezvisko-" + randomNumber);
    I.see("Záznamy 1 až 1 z 1");

    const expectedHtml = `
    <div class="form-step"> <p class="step-primaryHeader">Only step</p> <p class="step-secondaryHeader">This form is used to check render of rowView multistep form</p><div class="row"><div class="col"><div class="form-group mb-3"><label for="meno-1"><p>Meno</p>:</label> <span class="form-control emailInput-text">Vlad</span></div></div><div class="col">
    <div class="form-group mb-3"><label for="priezvisko-1"><p>Priezvisko</p>:</label> <span class="form-control emailInput-text">Priezvisko-${randomNumber}</span></div></div></div><div class="row"><div class="col"><div class="form-group mb-3"><label for="email-1"><p>Email</p>:</label> <span class="form-control emailInput-text">test@balat.sk</span></div>
    </div><div class="col">&nbsp;</div></div><div class="row"><div class="col"><div class="form-group mb-3"><label for="adresa-1"><p>Adresa</p>:</label> <span class="form-control emailInput-text">Askaban</span></div></div></div></div>
    `;

    checkSubmitedFormPreview(I, expectedHtml);
});

function checkSubmitedFormPreview(I, expectedHtml) {
    I.click( locate("a").withChild("i.ti-eye") );
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");

    I.executeScript(() => {
        const form = document.querySelector('form.multistep-form');
        return form.innerHTML;
    }).then(actualHtml => {
        I.say("Compare actual vs expected form HTML");

        const normalize = html =>
            html
            .replace(/\s+/g, ' ')        // collapse whitespace
            .replace(/>\s+</g, '><')     // remove space between tags
            .trim();

        I.assertEqual(
            normalize(actualHtml),
            normalize(expectedHtml),
            'Form HTML does not match exactly'
        );
    });
}

function createAndFillFormItem(I, DT,  DTE, fieldType, required, label, value, tooltip, placeholder) {
    I.click(DT.btn.formItems_add_button);
    DTE.waitForEditor("formItemsDataTable");
    fillFormItem(I, DTE, fieldType, required, label, value, tooltip, placeholder);
}

function fillFormItem(I, DTE, fieldType, required, label, value, tooltip, placeholder) {
    selectFieldType(I, fieldType);

    if(required !== null) {
        if (required) {
            I.checkOption("#DTE_Field_required_0");
        } else {
            I.uncheckOption("#DTE_Field_required_0");
        }
    }

    if(label !== null) {
        DTE.fillQuill("label", label);
    }

    if(value !== null) {
        I.fillField("#DTE_Field_value", value);
    }

    if(tooltip !== null) {
        DTE.fillQuill("tooltip", tooltip);
    }

    if(placeholder !== null) {
        I.fillField("#DTE_Field_placeholder", placeholder);
    }

    DTE.save();
}

function selectFieldType(I, value) {
    selectValue(I, value, ".DTE_Field_Name_fieldType");
}

function selectValue(I, value, locator) {
    I.click( locate(locator).find("button.dropdown-toggle") );
    I.waitForVisible("div.dropdown-menu.show");
    within("div.dropdown-menu.show", () => {
        I.fillField("input[type='search']", value);
        I.click( locate("a.dropdown-item").withChild( locate("span.text").withTextEquals(value) ) );
    });
    I.waitForInvisible("div.dropdown-menu.show");
}

function checkNavigationTabVsibility(I, seeTabs, notSeeTabs) {
    I.say("Checking Navigation tabs visibility");
    seeTabs.forEach(tabTitle => { I.seeElement( locate("ul.nav > li.nav-item > a.nav-link").withText(tabTitle) ); });
    notSeeTabs.forEach(tabTitle => { I.dontSeeElement( locate("ul.nav > li.nav-item > a.nav-link").withText(tabTitle) ); });
}

function checkEditorTabVsibility(I, seeTabs, notSeeTabs) {
    I.say("Checking Editor tabs visibility");
    seeTabs.forEach(tabTitle => { I.seeElement("#pills-dt-formsDataTable-" + tabTitle + "-tab"); });
    notSeeTabs.forEach(tabTitle => { I.dontSeeElement("#pills-dt-formsDataTable-" + tabTitle + "-tab"); });
}