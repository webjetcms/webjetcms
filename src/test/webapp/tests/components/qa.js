Feature('components.qa');

var randomNumber

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/apps/qa/admin/");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('zakladne testy', async ({I, DataTables, DTE}) => {
    await DataTables.baseTest({
        dataTable: 'qaDataTable',
        perms: 'menuQa',
        requiredFields: ['question'],
        createSteps: function(I, options) {
            DTE.fillField("groupName", "skupina1");
            DTE.fillField("categoryName", "kategoria1");
            DTE.fillField("fromName", "Web tester");
            DTE.fillField("fromEmail", "tester@balat.sk");

            I.click("#pills-dt-qaDataTable-answer-tab");
            DTE.fillQuill("answerToEmail", "Toto je odpoved do emailu.");
            //oznac emailu ako bold
            I.pressKey('ArrowLeft');
            for (var i=0; i<6; i++) {
                I.pressKey(['Shift', 'ArrowLeft']);
            }
            I.click("div.DTE_Field_Name_answerToEmail button.ql-bold");

            I.click("Áno", "div.DTE_Field_Name_publishOnWeb");
            DTE.fillQuill("answer", "Toto je odpoved na web stranku.");
        },
        editSteps: function(I, options) {
            I.click("#pills-dt-qaDataTable-answer-tab");
            I.wait(1);
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

Scenario('from struts to Spring', async ({I, DataTables, DTE}) => {
    I.amOnPage("/apps/otazky-odpovede/");
    I.wait(1);
    I.waitForElement("#fromName1");

    var name = "Feri_Baci_" + randomNumber;
    I.clickCss("#fromName1");
    I.fillField("#fromName1", name);

    var email  = "neviem@nepoviem.sk";
    I.clickCss("#fromEmail1");
    I.fillField("#fromEmail1", email);

    var question  = "Kde by som kupil borovicku_?_" + randomNumber;
    I.clickCss("#question1");
    I.fillField("#question1", question);

    I.clickCss("#allowPublishOnWeb1");

    //Send form with new question
    I.clickCss("#qaForm input.btn.btn-primary");

    I.wait(1);

    //Go check save question
    I.amOnPage("/apps/qa/admin/");

    DTE.waitForLoader();

    I.fillField("input.dt-filter-question", question);
    I.pressKey('Enter', "input.dt-filter-question");

    I.see(question);
    I.see(name);

    //Check set email and checked allowPublishOnWeb
    I.click(locate(".dt-row-edit").withText(question));

    DTE.waitForEditor("qaDataTable");

    //I see does not work ... soo I use grabValue and assertEquals
    let thisEmail = await I.grabValueFrom("#DTE_Field_fromEmail");
    I.assertEqual(thisEmail, email);

    I.clickCss("#pills-dt-qaDataTable-answer-tab");

    I.seeCheckboxIsChecked("#DTE_Field_allowPublishOnWeb_0");

    //Close editor
    DTE.cancel();

    //Delete question
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see(question, "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});