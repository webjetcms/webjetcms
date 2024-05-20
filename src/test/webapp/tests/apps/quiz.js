Feature('apps.quiz');

var randomNumber;

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Test dotaznika typu Spravna Odpoved", async ({ I }) => {
    I.logout();
    I.amOnPage("/apps/dotaznik/dotaznik_answer.html");
    I.waitForText("DOTAZNIK_ANSWER");

    //1.
    I.say("Check question 1.");
    I.see("AKÉ ZVIERA JE NA OBRÁZKU");
    I.seeElement("img[src='/images/gallery/test/koala.jpg']");

    //Should see only 4 elements
    await checkNumberOfOptions(I, 4);
    checkOptions(I, ["Koala", "Mačka", "Kengura", "Panda"]);
    selectOption(I, "Koala", true);

    //2.
    I.say("Check question 2.");
    I.seeElement("img[src='/images/gallery/test/penguins.jpg']");

    //Should see only 3 elements
    await checkNumberOfOptions(I, 3);
    checkOptions(I, ["Pštrosiu rodinku", "Nejakých tučniakov", "IT analytikov"]);
    selectOption(I, "Pštrosiu rodinku", false);

    I.say("Go back and check that option is still selected");
    I.clickCss("a.btn.back");
    I.seeElement("a.btn.next");
    I.seeElement("a.btn.back");
    I.clickCss("a.btn.next");

    //3.
    I.say("Check question 3.");
    I.seeElement("img[src='/images/gallery/test/desert.jpg']");

    //Should see only 3 elements
    await checkNumberOfOptions(I, 3);
    checkOptions(I, ["Piesok", "Kopec", "Nejaký kamenný útvar"]);
    selectOption(I, "Kopec", false);

    //Other
    I.say("Check results");
    I.see("Vyhodnotenie testu Answer_quiz :");

    I.seeElement( locate("li#countCorrectAnsw > strong").withText("1") );
    I.seeElement( locate("li#countIncorrectAnsw > strong").withText("2") );

    I.say("Check correct answers");
    I.clickCss("a.btn.correctAnswers_show");
    checkAnswer(I, 2, 1, ".correct");
    checkAnswer(I, 3, 1, ".wrong");
    checkAnswer(I, 7, 2, ".wrong");
});

Scenario('Dotaznik zakladne testy', async ({I, DataTables}) => {
    I.amOnPage("/apps/quiz/admin/");
    await DataTables.baseTest({
        dataTable: 'quizDataTable',
        perms: 'cmp_quiz',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

var quizQuestions = "#datatableFieldDTE_Field_quizQuestions_";
var quizResults = "#datatableFieldDTE_Field_quizResults_";

Scenario("Test zobrazovania fieldov a stlpcov", async ({ I, DT, DTE }) => {
    I.amOnPage("/apps/quiz/admin/");

    openNewQuiz(I, DTE);

    I.say("Check inner table columns")
    I.clickCss("#pills-dt-quizDataTable-questions-tab");
    I.waitForElement(quizQuestions + "wrapper", 10);

    I.say("check visible columns of quizQuestion inner table");
    seeInnerColumn(I, "rightAnswer", true);
    seeInnerColumn(I, "option1", true);
    seeInnerColumn(I, "option2", true);
    seeInnerColumn(I, "rate1", false);
    seeInnerColumn(I, "rate2", false);

    I.clickCss(quizQuestions + "wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.buttons-create");
    I.waitForElement(quizQuestions + "modal", 10);

    I.say("check visible fields of quizQuestion inner table editor");
    seeInnerField(I, "rightAnswer", true);
    seeInnerField(I, "option1", true);
    seeInnerField(I, "option2", true);
    seeInnerField(I, "rate1", false);
    seeInnerField(I, "rate2", false);
    //Cancel
    I.clickCss(quizQuestions + "modal > div > div > div.DTE_Header.modal-header > button");

    I.say("Change quiz type");
    I.clickCss("#pills-dt-quizDataTable-main-tab");
    I.waitForElement("div.DTE_Field_Name_quizType", 10);
    I.selectOption('#DTE_Field_quizType', 'Bodovaná odpoveď');

    I.say("Check inner table columns")
    questionsTab(I);

    I.say("check visible columns of quizQuestion inner table");
    seeInnerColumn(I, "rightAnswer", false);
    seeInnerColumn(I, "option1", true);
    seeInnerColumn(I, "option2", true);
    seeInnerColumn(I, "rate1", true);
    seeInnerColumn(I, "rate2", true);

    I.clickCss(quizQuestions + "wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.buttons-create");
    I.waitForElement(quizQuestions + "modal", 10);

    I.say("check visible fields of quizQuestion inner table editor");
    seeInnerField(I, "rightAnswer", false);
    seeInnerField(I, "option1", true);
    seeInnerField(I, "option2", true);
    seeInnerField(I, "rate1", true);
    seeInnerField(I, "rate2", true);
});

Scenario("Test priradenia otazok a odpovedi k ESTE nevytvorenemu dotazniku", async ({ I, DT, DTE }) => {
    I.amOnPage("/apps/quiz/admin/");
    let quizName = "quiz_" + randomNumber + "-autotest";
    let quizQuestionName = "quizQuestionName_" + randomNumber + "-autotest";
    let quizAnswerName = "quizQuestionName_" + randomNumber + "-autotest";

    I.say("Open dialog");
    openNewQuiz(I, DTE);

    I.say("Show quiz questions");
    questionsTab(I);

    I.say("add quiz question");
    I.clickCss(quizQuestions + "wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.buttons-create");
    I.waitForElement(quizQuestions + "modal", 10);
    I.wait(1);

    I.clickCss("#DTE_Field_question > div.editor.ql-container > div.ql-editor");
    I.fillField("#DTE_Field_question", quizQuestionName);

    I.clickCss("#DTE_Field_option1 > div.editor.ql-container > div.ql-editor");
    I.fillField("#DTE_Field_option1", "option1");

    I.clickCss("#DTE_Field_option2 > div.editor.ql-container > div.ql-editor");
    I.fillField("#DTE_Field_option2", "option2");
    //TODO - pridanie a otestovanie obrazku (aktu=alne nefunguje vlo6enie cey linku)
    I.clickCss(quizQuestions + "modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");
    DT.waitForLoader();

    I.say("Show quiz answers");
    answersTab(I);

    I.say("add quiz answer");
    I.clickCss(quizResults + "wrapper > div.dt-header-row.clearfix > div > div.col-auto > div > button.buttons-create");
    I.waitForElement(quizResults + "modal", 10);
    I.wait(1);
    I.fillField("#DTE_Field_scoreFrom", 1);
    I.fillField("#DTE_Field_scoreTo", 10);
    I.fillField("#DTE_Field_description", quizAnswerName);
    I.clickCss(quizResults + "modal > div > div > div.DTE_Footer.modal-footer > div.DTE_Form_Buttons > button.btn.btn-primary");
    DT.waitForLoader();

    I.say("Close quiz without saving");
    DTE.cancel();

    I.say("Test if saved question/answer is not visible for another quiz");
    DT.filter("name", "RAted_quiz");
    I.click("Rated_quiz");
    DTE.waitForEditor("quizDataTable");

    questionsTab(I);
    DT.filter("question", quizQuestionName);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    answersTab(I);
    DT.filter("description", quizAnswerName);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
    DTE.cancel();

    I.say("Try create QUIZ and check that last saved question/answer IS THERE");
    openNewQuiz(I, DTE);
    questionsTab(I);
    DT.filter("question", quizQuestionName);
    I.see(quizQuestionName);

    answersTab(I);
    DT.filter("description", quizAnswerName);
    I.see(quizAnswerName);

    I.say("Save new quiz");
    I.clickCss("#pills-dt-quizDataTable-main-tab");
    I.fillField("#DTE_Field_name", quizName);
    DTE.save();

    I.say("Check, that nnew saved quiz CONTAINS question/answer");
    DT.filter("name", quizName);
    I.see(quizName);
    I.click(quizName);
    DTE.waitForEditor("quizDataTable");

    questionsTab(I);
    DT.filter("question", quizQuestionName);
    I.see(quizQuestionName);

    answersTab(I);
    DT.filter("description", quizAnswerName);
    I.see(quizAnswerName);

    I.say('Delete this quiz');
    DTE.cancel();
    DT.filter("name", quizName);
    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
});

Scenario("Test dotaznika typu Bodovana Odpoved", async ({ I }) => {
    //there must be conf. value spamProtectionTimeout-quiz=5 to pass this test without SpamProtection fail
    I.logout();
    I.amOnPage("/apps/dotaznik/dotaznik_rated.html");
    I.waitForText("DOTAZNIK_RATED");

    //1.
    I.say("Check question 1.");
    I.see("ÚSEČKA JE");
    //Should see only 5 elements
    await checkNumberOfOptions(I, 5);
    checkOptions(I, ["Úsečka je časť priamky.", "Úsečka je časť priamky medzi dvoma bodmi.", "Neviem."]);
    selectOption(I, "Úsečka je časť priamky.", true);

    //2.
    I.say("Check question 2.");
    I.see("KRUH JE");
    await checkNumberOfOptions(I, 5);
    checkOptions(I, ["Kruh je štvorec.", "Kruh je množina bodov.", "Kruh je množina bodov, ktorých vzdialenosť od pevného bodu (stredu kružnice) je menšia alebo rovná pevne danému kladnému číslu (nazývaným polomer)."]);
    selectOption(I, "Kruh je množina bodov, ktorých vzdialenosť od pevného bodu (stredu kružnice) je menšia alebo rovná pevne danému kladnému číslu (nazývaným polomer).", false);

    //3.
    I.say("Check question 3.");
    I.see("ČO JE TOTO");
    I.seeElement("img[src='/images/gallery/test/dsc04082.jpeg']");

    //Should see only 4 elements
    await checkNumberOfOptions(I, 4);
    checkOptions(I, ["Audi", "Tesla", "BMW"]);
    selectOption(I, "BMW", false);

    //Other
    I.say("Check obtained points");
    I.see("Vyhodnotenie testu Rated_quiz :");
    I.see("V teste \"Rated_quiz\" ste dosiahli 11b. Nižšie si môžete prečítať vyhodnotenie.");

    I.say("Check correct answers");
    I.clickCss("a.btn.correctAnswers_show");
    checkAnswer(I, 14, 1, ".correct.correct");
    checkAnswer(I, 14, 2, ".answToQ2.correct");
    checkAnswer(I, 14, 3, ".answToQ3.correct");
    checkAnswer(I, 14, 4, ".answToQ4.wrong");
    checkAnswer(I, 14, 5, ".answToQ5.wrong");

    checkAnswer(I, 16, 1, ".wrong");
    checkAnswer(I, 16, 2, ".wrong");
    checkAnswer(I, 16, 3, ".correct");
    checkAnswer(I, 16, 4, ".wrong.wrong");
});

Scenario("Test quiz stat sekcie", async ({ I, DT }) => {
    I.amOnPage("/apps/quiz/admin/");

    I.say("Filter  Rated_quiz and go to it's statistics");
    DT.filter("id", 7);
    DT.filter("name", "Rated_quiz");
    I.see("Rated_quiz");
    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-statistics");

    I.say("Check that graphs (and table) are here");
    I.waitForElement("div#quizStat-correctAnswers", 10);
    I.waitForElement("div#quizStat-answersInTime", 10);
    I.waitForElement("div#quizStat-pointsInTime", 10);
    I.waitForElement("#quizStatDataTable", 10);

    I.say("Check ext filter");
    I.seeElement(locate("span.statPageTitle").withText("Rated_quiz"));
    I.seeElement("div.dt-extfilter-dayDate");

    I.say("Set date  and check values");
    I.fillField({css: "input.dt-filter-to-dayDate"}, "20.12.2023");
    I.clickCss("button.dt-filtrujem-dayDate");
    DT.checkTableRow("quizStatDataTable", 1, ["Kruh je", "", "61", "13", "82,43", "5,93", "8,00"]);
    DT.checkTableRow("quizStatDataTable", 2, ["Úsečka je", "", "61", "13", "82,43", "2,95", "5,00"]);
    DT.checkTableRow("quizStatDataTable", 3, ["Čo je toto", "/images/gallery/test/dsc04082.jpeg", "12", "62", "16,22", "0,16", "1,00"]);

    I.say("Set date  and check values");
    I.fillField({css: "input.dt-filter-from-dayDate"}, "18.12.2023");
    I.clickCss("button.dt-filtrujem-dayDate");
    DT.checkTableRow("quizStatDataTable", 1, ["Kruh je", "", "56", "11", "83,58", "6,04", "8,00"]);
    DT.checkTableRow("quizStatDataTable", 2, ["Úsečka je", "", "58", "9", "86,57", "3,07", "5,00"]);
    DT.checkTableRow("quizStatDataTable", 3, ["Čo je toto", "/images/gallery/test/dsc04082.jpeg", "9", "58", "13,43", "0,13", "1,00"]);

    I.say("GO back throu clicable title");
    I.forceClick(locate("a").withChild( locate("span.statPageTitle") ) );
    I.waitForElement("#quizDataTable", 10);
    DT.filter("id", 2);
    DT.filter("name", "Answer_quiz");
    I.see("Answer_quiz");
    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-statistics");

    I.say("Check that graphs (and table) are here");
    I.waitForElement("div#quizStat-correctAnswers", 10);
    I.waitForElement("div#quizStat-answersInTime", 10);
    I.waitForElement("div#quizStat-rightInTime", 10);
    I.waitForElement("div#quizStat-wrongInTime", 10);
    I.waitForElement("#quizStatDataTable", 10);

    I.say("Check ext filter");
    I.seeElement(locate("span.statPageTitle").withText("Answer_quiz"));
    I.seeElement("div.dt-extfilter-dayDate");

    I.say("Set date  and check values");
    I.fillField({css: "input.dt-filter-to-dayDate"}, "20.12.2023");
    I.clickCss("button.dt-filtrujem-dayDate");
    DT.checkTableRow("quizStatDataTable", 1, ["Aké zviera je na obrázku", "/images/gallery/test/koala.jpg", "45", "17", "72,58"]);
    DT.checkTableRow("quizStatDataTable", 2, ["Na obrázku vidíme", "/images/gallery/test/penguins.jpg", "25", "37", "40,32"]);
    DT.checkTableRow("quizStatDataTable", 3, ["Čo je to", "/images/gallery/test/desert.jpg", "19", "43", "30,65"]);

    I.say("Set date  and check values");
    I.fillField({css: "input.dt-filter-from-dayDate"}, "17.12.2023");
    I.clickCss("button.dt-filtrujem-dayDate");
    DT.checkTableRow("quizStatDataTable", 1, ["Aké zviera je na obrázku", "/images/gallery/test/koala.jpg", "42", "16", "72,41"]);
    DT.checkTableRow("quizStatDataTable", 2, ["Na obrázku vidíme", "/images/gallery/test/penguins.jpg", "21", "37", "36,21"]);
    DT.checkTableRow("quizStatDataTable", 3, ["Čo je to", "/images/gallery/test/desert.jpg", "15", "43", "35,86"]);
});

function checkAnswer(I, answId, optionId, classes) {
    I.seeElement( "div#answer" + answId + " > div.questionAnswers > div.row > div.answToQ" + optionId  + classes );
}

function selectOption(I, option, isFirstQuestion) {
    I.dontSeeElement("a.btn.next");

    if (isFirstQuestion) I.dontSeeElement("a.btn.back");
    else I.seeElement("a.btn.back");

    I.click( locate("label").withText(option) );

    I.seeElement("a.btn.next");
    I.clickCss("a.btn.next");
}

async function checkNumberOfOptions(I, number) {
    const question =  await I.grabNumberOfVisibleElements( locate("div.questionOptions > label") );
    I.assertEqual(question, number);
}

function checkOptions(I, optionsArr) {
    for(let i = 0; i < optionsArr.length; i++) {
        checkOption(I, optionsArr[i]);
    }
}

function checkOption(I, optionText) {
    I.seeElement( locate("label").withText(optionText) );
}

function openNewQuiz(I, DTE) {
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("quizDataTable");
}

function questionsTab(I) {
    I.clickCss("#pills-dt-quizDataTable-questions-tab");
    I.waitForElement(quizQuestions + "wrapper", 10);
}

function answersTab(I) {
    I.clickCss("#pills-dt-quizDataTable-results-tab");
    I.waitForElement(quizResults + "wrapper", 10);
}

function seeInnerColumn(I, columnName, see) {
    let key = quizQuestions + "wrapper > div:nth-child(2) > div > div > div.dataTables_scroll > div.dataTables_scrollHead > div > table > thead > tr:nth-child(1) > th.dt-th-" + columnName;
    if (see) {
        I.seeElement(key)
    } else {
        I.dontSeeElement(key);
    }
}

function seeInnerField(I, fieldName, see) {
    let key = quizQuestions + "modal div.DTE_Body form div.DTE_Field_Name_" + fieldName;
    if (see) {
        I.seeElement(key)
    } else {
        I.dontSeeElement(key);
    }
}