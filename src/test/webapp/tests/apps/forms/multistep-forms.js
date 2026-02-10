Feature('apps.forms.multistep-forms');

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

const baseAdminMail = "wjmultistep.admin";
const baseUserMail = "wjmultistep.user";
const appInsertTestPageId = 156277;
const appMultiInsertTestPageId = 156272;

Scenario('Check editor tabs', async ({ I, DT, DTE, TempMail }) => {
    await TempMail.destroyInbox(baseAdminMail);
    await TempMail.destroyInbox(baseUserMail);

    I.amOnPage("/apps/form/admin/");

    DT.filterEquals("formName", "Kontaktny_formular");
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-edit");
    DTE.waitForEditor("formsDataTable");
    checkEditorTabVsibility(I, ["basic", "settings-basic", "settings-email", "settings-advanced"]);
});

Scenario('Base multistep form', ({ I, DT, DTE, TempMail }) => {
    I.amOnPage("/apps/form/admin/");

    checkNavigationTabVsibility(I, ["Zoznam formulárov", "Archív formulárov", "Regulárne výrazy"], ["Položky formuláru"]);

    I.say("Create new multistep form");
    I.click("button.buttons-create");
    DTE.waitForEditor("formsDataTable");

    DTE.fillField("formName", "Multistepform_1");
    DTE.save();
    I.waitForText("Názov formuláru musí byť jedinečný.", 5);

    DTE.fillField("formName", newMultistepFormName);

    I.fillField("#DTE_Field_formSettings-recipients", baseAdminMail + TempMail.getTempMailDomain());
    DTE.save();

    I.say("Check default form content - page is auto redirected");
    I.waitForVisible("div.stepPreviewWrapper");

    //Default step
    I.seeElement( locate("table#formStepsDataTable > tbody > tr > td").withText("Krok 1") );
    //No item
    I.seeElement( locate("table#formItemsDataTable > tbody > tr > td").withText("Nenašli sa žiadne vyhovujúce záznamy") );
});

Scenario('Fill and test form content', async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/apps/form/admin/form-steps/?formName=" + newMultistepFormName);

    I.say("Edit first default step");
    I.waitForVisible("#formStepsDataTable_wrapper");

    I.waitForElement( locate("table#formStepsDataTable > tbody > tr.selected > td").withText("Krok 1") );
    I.click(DT.btn.formSteps_edit_button);
    DTE.waitForEditor("formStepsDataTable");

    DTE.fillQuill("header", "1 - Primarny nadpis | 1 - Sekundarny nadpis");
    DTE.save();

    I.say("Add elements to the first step");
    createAndFillFormItem(I, DT, DTE, 'Meno', true, "Vase meno", "!LOGGED_USER_FIRSTNAME!", "Vase prve meno", null);
    createAndFillFormItem(I, DT, DTE, 'Priezvisko', false, "Vase priezvisko", "!LOGGED_USER_LASTNAME!", "Vase rodinne meno", null);
    createAndFillFormItem(I, DT, DTE, 'E-mailová adresa', true, "Emailova adresa", null, null, "nieco@interway.sk");
    createAndFillFormItem(I, DT, DTE, 'Skupina zaškrtávacích polí', false, null, "A,B,C", null, null);
    createAndFillFormItem(I, DT, DTE, 'Skupina výberových polí', false, null, "D,E,F", null, null);

    I.say("Test generated preview of the first step - using screenshot compare");

    I.say("Add second step");
    I.click(DT.btn.formSteps_add_button);
    DTE.waitForEditor("formStepsDataTable");

    DTE.fillQuill("header", "2 - Druhy krok | 2 - Sekundarny nadpis druheho kroku");
    DTE.save();

    I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("Krok 2") );
    I.say("Add elements to the second step");
    createAndFillFormItem(I, DT, DTE, 'Nahrať obrázky', null, "Pridajte obrazky", null, null, null);
    createAndFillFormItem(I, DT, DTE, 'Výberový zoznam - select', false, "Select pole", "A,B,C,D", "zoznam tooltip", null);
    createAndFillFormItem(I, DT, DTE, 'Formátované textové pole', true, "WYSIWYG", "happy wysiwyg placeholder", "wysiwyg tooltip", null);

    I.resizeWindow(1360, 850);

    //compare screenshots
    I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("Krok 1") );
    await Document.compareScreenshotElement("div.stepPreviewWrapper > div.stepPreview", "multistep-form/multistep-form-step-1.png", null, null, 5);
    I.click( locate("table#formStepsDataTable > tbody > tr > td").withText("Krok 2") );
    await Document.compareScreenshotElement("div.stepPreviewWrapper > div.stepPreview", "multistep-form/multistep-form-step-2.png", null, null, 5);
});

Scenario('Insert multistep into page and test it', async ({ I, DTE, Document, Apps }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + appInsertTestPageId);

    // Set new multistep form as form for the page
    DTE.waitForEditor();
    Apps.openAppEditor();
    DTE.selectOption("formName", newMultistepFormName);
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    DTE.save();

    I.say("Test visual of step one");
    I.amOnPage("/apps/multistep-formular/app-insert-test.html");
    await Document.compareScreenshotElement("div.multistep-form-app", "multistep-form/multistep-form-page-step-1.png", null, null, 5);

    I.say("Test and submit step 1");
    I.clickCss("button[type='submit']");
    I.waitForText("Emailova adresa - povinné pole");
    I.fillField("#email-1", "sivan@noopmail.com");
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

Scenario('Test form detail and filled data ', async ({ I, DT, DTE }) => {

    I.amOnPage("/apps/form/admin/detail/?formName=" + newMultistepFormName);

    I.see("Záznamy 1 až 1 z 1");

    const columnNames = [
        {id: "meno-1", name: "Vase meno (Krok 1)", value: "Tester" },
        {id: "priezvisko-1", name: "Vase priezvisko (Krok 1)", value: "Playwright" },
        {id: "email-1", name: "Emailova adresa (Krok 1)", value: "sivan@noopmail.com" },
        {id: "checkboxgroup-1", name: "Skupina zaškrtávacích polí (Krok 1)", value: "A,B" },
        {id: "radiogroup-1", name: "Skupina výberových polí (Krok 1)", value: "F" },
        {id: "multiupload_images-1-fileNames", name: "Pridajte obrazky (Krok 2)", value: "penguin.jpg" },
        {id: "select-1", name: "Select pole (Krok 2)", value: "C" },
        {id: "wysiwyg-1", name: "WYSIWYG (Krok 2)", value: "happy wysiwyg placeholder" }
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
    columnNames.forEach(({ id, value }) => {
        if (value !== "") {
            I.seeInField("#DTE_Field_col_" + id, value);
        }
    });

    DTE.cancel();

    const expectedHtml = `
        <div class="form-step mt-3"><div class="step-header"><p>1 - Primarny nadpis | 1 - Sekundarny nadpis</p></div><div class="form-group mb-3"><label for="meno-1">Vase meno&nbsp;*:</label><span class="form-control emailInput-text">Tester</span></div>
        <div class="form-group mb-3"><label for="priezvisko-1">Vase priezvisko:</label><span class="form-control emailInput-text">Playwright</span></div><div class="form-group mb-3"><label for="email-1">Emailova adresa&nbsp;*:</label><span class="form-control emailInput-text">sivan@noopmail.com</span></div>
        <div class="form-group mb-3"><label for="checkboxgroup-1">Skupina zaškrtávacích polí:</label><div class="form-check"><span class="inputcheckbox emailinput-cb input-checked">[X]</span><label for="checkboxgroup-1-0" class="form-check-label">A</label></div><div class="form-check"><span class="inputcheckbox emailinput-cb input-checked">[X]</span><label for="checkboxgroup-1-1" class="form-check-label">B</label></div>
        <div class="form-check"><span class="inputcheckbox emailinput-cb input-unchecked">[&nbsp;]</span><label for="checkboxgroup-1-2" class="form-check-label">C</label></div></div><div class="form-group mb-3"><label for="radiogroup-1">Skupina výberových polí:</label><div class="form-check"><span class="inputradio emailinput-radio input-unchecked">[&nbsp;]</span><label for="radiogroup-1-0" class="form-check-label">D</label></div><div class="form-check"><span class="inputradio emailinput-radio input-unchecked">[&nbsp;]</span><label for="radiogroup-1-1" class="form-check-label">E</label></div><div class="form-check"><span class="inputradio emailinput-radio input-checked">[X]</span><label for="radiogroup-1-2" class="form-check-label">F</label></div></div></div>
        <hr><div class="form-step mt-3"><div class="step-header"><p>2 - Druhy krok | 2 - Sekundarny nadpis druheho kroku</p></div><div class="form-group mb-3"><label for="multiupload_images-1">Pridajte obrazky:</label><span class="form-control emailInput-text">penguin.jpg</span></div><div class="form-group mb-3"><label for="select-1">Select pole:</label><span class="form-control emailInput-select">C</span></div>
        <div class="form-group mb-3"><label for="wysiwyg-1">WYSIWYG&nbsp;*:</label><span class="form-control emailInput-textarea" style="height: auto;">happy wysiwyg placeholder</span></div></div>
    `;

    const actualHtml = await getSubmitedFormPreview(I);

    I.say("Compare actual vs expected form HTML");
    compareTwoHtml(I, actualHtml, expectedHtml);
});

Scenario('Test send email', async ({ I, TempMail }) => {
    await TempMail.login(baseAdminMail);
    await TempMail.openLatestEmail();

    const expectedHtml = `
        <form action="/rest/multistep-form/save-form?form-name=${newMultistepFormName}&amp;step-id=-1" method="post" name="formMailForm-${newMultistepFormName}"><div><div><p>1 - Primarny nadpis | 1 - Sekundarny nadpis</p></div>
        <div><label for="meno-1">Vase meno&nbsp;*:</label> <span>Tester</span></div><div><label for="priezvisko-1">Vase priezvisko:</label> <span>Playwright</span></div><div><label for="email-1">Emailova adresa&nbsp;*:</label> <span>sivan@noopmail.com</span></div>
        <div><label for="checkboxgroup-1">Skupina zaškrtávacích polí:</label><div><span>[X]</span> <label for="checkboxgroup-1-0">A</label></div><div><span>[X]</span> <label for="checkboxgroup-1-1">B</label></div>
        <div><span>[&nbsp;]</span> <label for="checkboxgroup-1-2">C</label></div>
        </div><div><label for="radiogroup-1">Skupina výberových polí:</label><div><span>[&nbsp;]</span> <label for="radiogroup-1-0">D</label></div>
        <div><span>[&nbsp;]</span> <label for="radiogroup-1-1">E</label></div>
        <div><span>[X]</span> <label for="radiogroup-1-2">F</label></div>
        </div></div><hr><div><div><p>2 - Druhy krok | 2 - Sekundarny nadpis druheho kroku</p></div><div><label for="multiupload_images-1">Pridajte obrazky:</label> <span>penguin.jpg</span> </div><div><label for="select-1">Select pole:</label><span>C</span></div>
        <div><label for="wysiwyg-1">WYSIWYG&nbsp;*:</label> <span style="height: auto;">happy wysiwyg placeholder</span></div></div>  </form>
    `;

    checkEmailWithForm(I, expectedHtml);
    TempMail.checkAttachments(["penguin.jpg"]);
    TempMail.closeEmail();
    await TempMail.destroyInbox(baseAdminMail);
});

Scenario('Change form_settings and test it No.1', async ({ I, DT, DTE, TempMail }) => {

    I.amOnPage("/apps/form/admin/");
    DT.filterEquals("formName", newMultistepFormName);
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-edit");
    DTE.waitForEditor("formsDataTable");

    I.clickCss("#pills-dt-formsDataTable-settings-basic-tab");
    I.checkOption("#DTE_Field_formSettings-messageAsAttach_0");
    I.waitForVisible("#DTE_Field_formSettings-messageAsAttachFileName");
    I.fillField("#DTE_Field_formSettings-messageAsAttachFileName", "filled_form");

    I.clickCss("#pills-dt-formsDataTable-settings-email-tab");
    I.checkOption("#DTE_Field_formSettings-forceTextPlain_0");
    I.fillField("#DTE_Field_formSettings-emailTextBefore", "START form text");
    I.fillField("#DTE_Field_formSettings-emailTextAfter", "END form text");
    DTE.save();

    I.say("Fill the form again to test changed email settings");
    I.amOnPage("/apps/multistep-formular/app-insert-test.html");

    I.say("Fill and submit step 1");
    I.fillField("#email-1", baseUserMail + TempMail.getTempMailDomain());
    I.clickCss("#checkboxgroup-1-0");
    I.clickCss("#checkboxgroup-1-2");
    I.clickCss("#radiogroup-1-2");
    //submit
    I.clickCss("button[type='submit']");

    I.say("Fill and submit step 2 - final");
    I.dontSee("Odstrániť súbor");
    I.attachFile('input[accept=".gif,.png,.jpg,.jpeg,.svg"]', 'tests/apps/penguin.jpg');
    I.waitForText("Odstrániť súbor", 5, "a.dz-remove");

    I.clickCss("button[type='submit']");
    I.waitForText("Formulár bol úspešne odoslaný");

    I.say("Test generated filled form in admin");
    I.amOnPage("/apps/form/admin/detail/?formName=" + newMultistepFormName);
    DT.filterStartsWith("col_email-1", baseUserMail + TempMail.getTempMailDomain());
    I.see("Záznamy 1 až 1 z 1");

    const expectedHtml = `
        START form text
        <br>1 - Primarny nadpis | 1 - Sekundarny nadpis<br><br>
        <br>Vase meno *: Tester<br>
        <br>Vase priezvisko: Playwright<br>
        <br>Emailova adresa *: ${baseUserMail + TempMail.getTempMailDomain()}<br>
        <br>Skupina zaškrtávacích polí: [X] A<br><br>[ ] B<br><br>[X] C<br><br>
        <br>Skupina výberových polí: [ ] D<br><br>[ ] E<br><br>[X] F<br><br>
        <br>
        <br>
        <br>2 - Druhy krok | 2 - Sekundarny nadpis druheho kroku<br><br>
        <br>Pridajte obrazky: penguin.jpg<br>
        <br>Select pole: A<br>
        <br>WYSIWYG *: happy wysiwyg placeholder<br>
        <br>
        <br>
        <br>END form text
    `;

    const actualHtml = await getSubmitedFormPreview_plain(I);

    I.say("Compare actual vs expected form HTML");
    compareTwoHtml(I, actualHtml, expectedHtml);
    I.switchTo();

    I.click( locate("div.modal-dialog").find("button.btn-close") );

    I.say("Now check admin email");
    await TempMail.login(baseAdminMail);
    await TempMail.openLatestEmail();

    I.see("Pozrite si priložený súbor");
    TempMail.checkAttachments(["penguin.jpg", "filled_form"]);
    TempMail.closeEmail();
    await TempMail.destroyInbox(baseAdminMail);

    // TODO, check file with form ?
});

Scenario('Change form_settings and test it No.2', async ({ I, DT, DTE, TempMail }) => {
    I.say("too fast - sometimes spam proble -> wait 35 sec");
    I.wait(35);

    I.amOnPage("/apps/form/admin/");
    DT.filterEquals("formName", newMultistepFormName);
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-edit");
    DTE.waitForEditor("formsDataTable");

    I.clickCss("#pills-dt-formsDataTable-settings-basic-tab");
    I.uncheckOption("#DTE_Field_formSettings-messageAsAttach_0");
    I.click("#editorAppDTE_Field_formSettings-formmailSendUserInfoDoc button.btn-vue-jstree-item-edit");
    I.waitForVisible("#jsTree");
    I.click(locate('.jstree-node.jstree-closed').withText('Aplikácie').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jstree-node.jstree-closed').withText('Multistep formulár').find('.jstree-icon.jstree-ocl'));
    I.click(locate('a.jstree-anchor').withText('UserNotifyAftertSave'));
    I.waitForInvisible("#jsTree");

    I.clickCss("#pills-dt-formsDataTable-settings-email-tab");
    I.uncheckOption("#DTE_Field_formSettings-forceTextPlain_0");
    I.checkOption("#DTE_Field_formSettings-formMailEncoding_0");
    I.checkOption("#DTE_Field_formSettings-addTechInfo_0");
    DTE.save();

    I.say("Fill the form again to test changed email settings");
    I.amOnPage("/apps/multistep-formular/app-insert-test.html");

    I.say("Fill and submit step 1");
    I.fillField("#email-1", baseUserMail + TempMail.getTempMailDomain());
    I.clickCss("#checkboxgroup-1-0");
    I.clickCss("#checkboxgroup-1-2");
    I.clickCss("#radiogroup-1-2");
    //submit
    I.clickCss("button[type='submit']");

    I.say("Fill and submit step 2 - final");
    I.dontSee("Odstrániť súbor");
    I.attachFile('input[accept=".gif,.png,.jpg,.jpeg,.svg"]', 'tests/apps/penguin.jpg');
    I.waitForText("Odstrániť súbor", 5, "a.dz-remove");

    I.clickCss("button[type='submit']");
    I.waitForText("Formulár bol úspešne odoslaný");

    I.say("Now check admin email");
    await TempMail.login(baseAdminMail);
    await TempMail.openLatestEmail();

    I.say("Because email contains dynamic tech info, we will just check presence of some static texts");
    I.see("Primarny nadpis");
    I.see("Skupina zaskrtavacich poli");
    I.see("Sekundarny nadpis druheho kroku");
    I.see("penguin.jpg");
    I.see("Technicke info:");
    I.see("IP adresa:");

    TempMail.checkAttachments(["penguin.jpg"]);
    TempMail.closeEmail();
    await TempMail.destroyInbox(baseAdminMail);

    I.say("Check allso send user email");
    await TempMail.login(baseUserMail);
    await TempMail.openLatestEmail();

    I.see("YOUR FORM WAS SAVED");

    TempMail.closeEmail();
    await TempMail.destroyInbox(baseUserMail);
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
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + appMultiInsertTestPageId);
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
        Apps.insertApp('Formulár', '#multistep_form-title', appMultiInsertTestPageId , false);

        I.switchTo('.cke_dialog_ui_iframe');
        I.waitForElement('#editorComponent', 10);
        I.switchTo('#editorComponent');

        DTE.selectOption("formName", "Multistepform_light");
        I.switchTo();
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok');

        DTE.save();

    I.say("Insert SECOND app");
        Apps.insertApp('Formulár', '#multistep_form-title', appMultiInsertTestPageId , false);

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
Scenario('Insert and test multiple forms in one page - test apps independent behaviour', async ({ I }) => {
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
    I.say("too fast - sometimes spam proble -> wait 35 sec");
    I.wait(35);

    I.say("Test generated preview");
    I.amOnPage("/apps/form/admin/form-steps/?formName=Multistepform_rowView");
    I.waitForVisible("#formStepsDataTable_wrapper");

    I.waitForElement( locate("table#formStepsDataTable > tbody > tr.selected > td").withText("Krok 1") );
    I.waitForElement(".form-step input#meno-1");
    I.wait(3);

    await Document.compareScreenshotElement("div.stepPreviewWrapper > div.stepPreview", "multistep-form/multistep-form-rowView.png", null, null, 5);

    I.say("Test visual of rowView form on page");
    I.amOnPage("/apps/multistep-formular/rowviewversion.html");
    I.waitForVisible("div.multistep-form-app");

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
        <div class="form-step mt-3"><div class="step-header"><p>Ukážka: viacero polí v jednom riadku</p></div><div class="row"><div class="col"><div class="form-group mb-3"><label for="meno-1">Meno:</label> <span class="form-control emailInput-text">Vlad</span></div></div>
        <div class="col"><div class="form-group mb-3"><label for="priezvisko-1">Priezvisko:</label> <span class="form-control emailInput-text">Priezvisko-${randomNumber}</span></div></div></div><div class="row"><div class="col"><div class="form-group mb-3"><label for="email-1">Email:</label> <span class="form-control emailInput-text">test@balat.sk</span></div>
        </div><div class="col">&nbsp;</div></div><div class="row"><div class="col"><div class="form-group mb-3"><label for="adresa-1">Adresa:</label> <span class="form-control emailInput-text">Askaban</span></div></div></div></div>
    `;

    const actualHtml = await getSubmitedFormPreview(I);

    I.say("Compare actual vs expected form HTML");
    compareTwoHtml(I, actualHtml, expectedHtml);
});

async function getSubmitedFormPreview(I) {
    I.click( locate("a").withChild("i.ti-eye") );
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");

    return await I.executeScript(() => {
        let form = document.querySelector('form.multistep-form');
        return form ? form.innerHTML : null;
    });
}
async function getSubmitedFormPreview_plain(I) {
    I.click( locate("a").withChild("i.ti-eye") );
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");

    return await I.executeScript(() => {
        let form = document.querySelector("body");
        return form ? form.innerHTML : null;
    });
}

function checkEmailWithForm(I, expectedHtml) {
    I.executeScript((formName) => {
        const selector = "form[name='formMailForm-" + formName + "']";
        const form = document.querySelector(selector);
        return form ? form.outerHTML : null;
    }, newMultistepFormName).then(actualHtml => {
        I.say("Compare actual vs expected EMAIL form HTML");
        compareTwoHtml(I, actualHtml, expectedHtml);
    });
}

function compareTwoHtml(I, actualHtml, expectedHtml) {
    const normalize = html =>
        html
            .replace(/\s*<br>\s*/gi, '<br>')
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

function createAndFillFormItem(I, DT,  DTE, fieldType, required, label, value, tooltip, placeholder) {
    I.click(DT.btn.formItems_add_button);
    DTE.waitForEditor("formItemsDataTable");
    fillFormItem(I, DTE, fieldType, required, label, value, tooltip, placeholder);
}

function fillFormItem(I, DTE, fieldType, required, label, value, tooltip, placeholder) {
    selectFieldType(I, fieldType);

    if(required !== null) {
        if (required) { I.checkOption("#DTE_Field_required_0"); }
        else { I.uncheckOption("#DTE_Field_required_0"); }
    }

    if(label !== null) { DTE.fillQuill("label", label); }

    I.clickCss("#pills-dt-formItemsDataTable-advanced-tab");
    if(value !== null) { I.fillField("#DTE_Field_value", value); }
    if(tooltip !== null) { DTE.fillQuill("tooltip", tooltip); }
    if(placeholder !== null) { I.fillField("#DTE_Field_placeholder", placeholder); }

    DTE.save();
}

function selectFieldType(I, value) { selectValue(I, value, ".DTE_Field_Name_fieldType") }

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

function checkEditorTabVsibility(I, seeTabs) {
    I.say("Checking Editor tabs visibility");
    seeTabs.forEach(tabTitle => { I.seeElement("#pills-dt-formsDataTable-" + tabTitle + "-tab"); });
}