Feature('apps.inquiry.inquiry');

var randomNumber;
var questionName = "name-autotest-";
var answerA = "answerA";
var answerB = "answerB";
var testGroup = "auto_test_group";

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        questionName = "name-autotest-" + randomNumber;
    }
});

Scenario('zakladne testy @baseTest', async ({I, DataTables}) => {
    I.amOnPage("/apps/inquiry/admin/");
    await DataTables.baseTest({
        dataTable: 'inquiryDataTable',
        perms: 'menuInquiry',
        createSteps: function(I, options) {
            I.click("#pills-dt-inquiryDataTable-advanced-tab");
            I.fillField("#DTE_Field_questionGroup", "autotest");
            I.click("#pills-dt-inquiryDataTable-basic-tab");
        }
    });
});

Scenario('test inner table', async ({I, DT, DTE}) => {
    I.amOnPage("/apps/inquiry/admin/");

    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("inquiryDataTable");
    I.click("#pills-dt-inquiryDataTable-answers-tab");

    //Check not null field
    addAnswer(I, "");
    I.see("Chyba: niektoré polia neobsahujú správne hodnoty.");
    I.click("#datatableFieldDTE_Field_editorFields-answers_modal > div > div > div.DTE_Header.modal-header > button");

    //Add two answers
    addAnswer(I, answerA);
    addAnswer(I, answerB);

    //Close question
    I.click("#inquiryDataTable_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-outline-secondary.btn-close-editor");

    I.click("div.dt-buttons button.buttons-create");
    DTE.waitForEditor("inquiryDataTable");
    I.click("#pills-dt-inquiryDataTable-answers-tab");

    //Check that tmp answers was removed
    I.dontSee(answerA);
    I.dontSee(answerB);

    //Add two answers
    addAnswer(I, answerA);
    addAnswer(I, answerB);

    //Set group
    I.click("#pills-dt-inquiryDataTable-advanced-tab");
    I.click("#DTE_Field_questionGroup");
    I.fillField("#DTE_Field_questionGroup", testGroup);

    //Set question name
    I.click("#pills-dt-inquiryDataTable-basic-tab");
    I.click("#DTE_Field_questionText");
    DTE.fillQuill("questionText", questionName);

    //Save
    DTE.save();

    //Filter this entity
    DT.filterContains("questionText", questionName);
    I.click(questionName);
    DTE.waitForEditor("inquiryDataTable");
    I.click("#pills-dt-inquiryDataTable-answers-tab");

    //Check that answers are there
    I.see(answerA);
    I.see(answerB);

    //Save
    DTE.save();

    //CHECK IF INQUIRY is shown in webpage correctly
    I.amOnPage('/apps/anketa/anketa-autotest.html');

    I.see("ANKETA");
    I.see(questionName.toUpperCase());
    I.see(answerA);
    I.see(answerB);
    I.see("0%");
    I.dontSee("100%");

    //
    I.say("skus zahlasovat za moznost A");
    I.click(answerA);
    I.acceptPopup();

    I.see("0%");
    I.see("100%");
    I.see("Celkový počet hlasujúcich: 1");

    //
    I.say("Sprav hlasovanie opakovane, nezmie sa zmenit");
    I.click(answerA);
    I.acceptPopup();

    I.see("0%");
    I.see("100%");
    I.see("Celkový počet hlasujúcich: 1");

    //
    I.say("Over pocty v administracii");
    I.amOnPage("/apps/inquiry/admin/");
    DT.waitForLoader("inquiryDataTable");
    I.click(questionName);
    I.seeInField("#DTE_Field_totalClicks", "1");

    I.click("#pills-dt-inquiryDataTable-answers-tab");
    I.waitForText(answerA, "#datatableFieldDTE_Field_editorFields-answers");
    I.click(answerA);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-answers");
    I.seeInField("#DTE_Field_answerClicks", "1");
    DTE.cancel("datatableFieldDTE_Field_editorFields-answers", true);
    I.click(answerB);
    DTE.waitForEditor("datatableFieldDTE_Field_editorFields-answers");
    I.seeInField("#DTE_Field_answerClicks", "0");
    DTE.cancel("datatableFieldDTE_Field_editorFields-answers", true);
});

Scenario('test inner table-delete', ({I, DT}) => {
    //Delete question
    I.amOnPage("/apps/inquiry/admin/");
    DT.filterContains("questionText", questionName);
    I.see(questionName);
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

function addAnswer(I, answer) {
    I.click("#datatableFieldDTE_Field_editorFields-answers_wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success.buttons-divider");
    I.click("#DTE_Field_answerText");
    I.fillField("#DTE_Field_answerText", answer);
    I.click("#datatableFieldDTE_Field_editorFields-answers_modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");
}

Scenario('test inquiry groups', ({I}) => {
    I.amOnPage("/apps/anketa/?NO_WJTOOLBAR=true");
    I.see("AKO SA VÁM PÁČI WEBJET");
    I.see("Je super");
    I.see("Neviem, nepoznám");

    I.dontSee("KOĽKO OČÍ MÁ PES?");
    I.dontSee("jedno");
    I.dontSee("dve");
    I.dontSee("tri");

    I.dontSee("Anketa mirroring");

    I.amOnPage("/apps/anketa/anketa-pes.html?NO_WJTOOLBAR=true");
    I.dontSee("AKO SA VÁM PÁČI WEBJET");
    I.dontSee("Je super");
    I.dontSee("Neviem, nepoznám");

    I.see("KOĽKO OČÍ MÁ PES?");
    I.see("jedno");
    I.see("dve");
    I.see("tri");

    I.dontSee("Anketa mirroring");
});

Scenario('test domainId', ({I, Document}) => {
    I.amOnPage("/apps/inquiry/admin/");
    I.see("Ako sa vám páči WebJET");
    I.see("Koľko očí má pes?");
    I.dontSee("Anketa mirroring");

    Document.switchDomain("mirroring.tau27.iway.sk");
    I.dontSee("Ako sa vám páči WebJET");
    I.dontSee("Koľko očí má pes?");
    I.see("Anketa mirroring");
});

Scenario('odhlasenie', ({I}) => {
    I.logout();
});

Scenario('test struts refactor', ({I}) => {

    //
    I.say("struts redirect");
    I.amOnPage("/admin/v9/");
    I.executeScript(()=>{
        window.location.href = '/inquiry.answer.do?resultUrl=/apps/anketa/anketa-autotest.html';
    });
    I.waitForText("Ukážka ankety. Toto je akože text web stránky.", 20);
    I.seeInCurrentUrl("fail=1");

});

Scenario('testovanie app - Inquiry', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Anketa', '#components-inquiry-title');

    const defaultParams = {
        group: '',
        imagesLength: '10',
        percentageFormat: '0',
        orderBy: 'answer_id',
        order: 'ascending',
        width: '100%',
        random: 'true',
        totalClicks: 'true',
        displayVoteResults: 'true',
        style: '01',
        color: '01'
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see("AKO SA VÁM PÁČI WEBJET");
    I.seeElement(locate(".bar_fill").first());
    I.seeElement("//div[@class='inquiry' and contains(@style, 'width:100%')]");

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        group: 'inquiry',
        imagesLength: '5',
        percentageFormat: '0.0',
        orderBy: 'answer_text',
        order: 'ascending',
        width: '70%',
        random: 'false',
        totalClicks: 'false',
        displayVoteResults: 'false',
        style: '02',
        color: '03'
    };

    DTE.selectOption('group', changedParams.group);
    I.click(locate('div').withChild('#DTE_Field_group'));
    DTE.fillField('imagesLength', changedParams.imagesLength);
    DTE.selectOption('percentageFormat', changedParams.percentageFormat);
    DTE.selectOption('orderBy', 'Textu otázky');
    DTE.fillField('width', changedParams.width);
    DTE.clickSwitch('random_0');
    DTE.clickSwitch('totalClicks_0');
    DTE.clickSwitch('displayVoteResults_0');
    Apps.switchToTabByIndex(1);
    I.clickCss('label[for=DTE_Field_style_1]');
    I.clickCss('label[for=DTE_Field_color_2]');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')
    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see("HOW DO YOU LIKE WEBJET");
    I.dontSeeElement(locate(".bar_fill").first());
    I.seeElement("//div[@class='inquiry' and contains(@style, 'width:70%')]");
});



