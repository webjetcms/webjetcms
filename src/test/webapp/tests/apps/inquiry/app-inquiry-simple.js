Feature('apps.app-inquiry-simple');

var randomNumber;
var inquiryName;

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        inquiryName = "inquiry-simple-autotest-" + randomNumber;
    }

});
Scenario('Test new inquiry simple', async ({I, DT, DTE, Apps, Document}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=40744");
    DT.waitForLoader();

    Apps.insertApp('Anketa ľahko', '#components-inquirysimple-title', null , false);

    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.fillField("#DTE_Field_name", inquiryName);

    I.say("Add answers");
    I.clickCss("#pills-dt-component-datatable-answers-tab");
    addQuestion(I, DTE, "ANSWER_A-" + randomNumber);
    addQuestion(I, DTE, "ANSWER_B-" + randomNumber);
    addQuestion(I, DTE, "ANSWER_C-" + randomNumber);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");

    I.say("Check inquiry in next tab");
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForText(inquiryName, 10);
    I.see("ANSWER_A-" + randomNumber + " 0 hlasov / 0.0 %");
    I.see("ANSWER_B-" + randomNumber + " 0 hlasov / 0.0 %");
    I.see("ANSWER_C-" + randomNumber + " 0 hlasov / 0.0 %");

    I.say("Do vote first option and check result");
    I.clickCss("input.inputradio");
    I.clickCss("button.btn.btn-primary");
    I.waitForText("ANSWER_A-" + randomNumber + " 1 hlasov / 100 %", 5);

    I.say("Try vote again");
    I.clickCss("button.btn.btn-primary");
    I.waitForText("Už ste hlasovali", 5);

    I.say("Go back and test inquiry");
    I.switchToPreviousTab();
    I.closeOtherTabs();

    I.say("Go add new option")
    Apps.openAppEditor();
    I.clickCss("#pills-dt-component-datatable-answers-tab");
    addQuestion(I, DTE, "ANSWER_D-" + randomNumber);

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_ok");

    I.say("Test new opytion was added and test that old option vote is still there");
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForText(inquiryName, 10);
    I.see("ANSWER_A-" + randomNumber + " 1 hlasov / 100.0 %");
    I.see("ANSWER_B-" + randomNumber + " 0 hlasov / 0.0 %");
    I.see("ANSWER_C-" + randomNumber + " 0 hlasov / 0.0 %");
    I.see("ANSWER_D-" + randomNumber + " 0 hlasov / 0.0 %");

    I.say('And test you still can vote');
    I.clickCss("input.inputradio");
    I.clickCss("button.btn.btn-primary");
    I.waitForText("Už ste hlasovali", 5);
});

function addQuestion(I, DTE, question) {
    I.clickCss("#datatableFieldDTE_Field_editorData_wrapper button.buttons-create");
    DTE.waitForEditor("datatableFieldDTE_Field_editorData");
    I.fillField("#datatableFieldDTE_Field_editorData_modal #DTE_Field_question", question);
    I.clickCss("#datatableFieldDTE_Field_editorData_modal button.btn-primary");
}