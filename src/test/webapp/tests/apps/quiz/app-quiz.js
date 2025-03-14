Feature('apps.auiz.app-quiz');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('testovanie app - Dotazniky' , async ({ I, DTE, Apps }) => {
    Apps.insertApp('Dotazníky', '#components-quiz-title' );

    const defaultParams = {
        'quizId' : '1',
        'showAllAnswers' : 'false'
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.see('AKO VEĽMI CHCEŠ OTESTOVAŤ TENTO DOTAZNÍK');

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        'quizId' : '2',
        'showAllAnswers' : 'true'
    };

    I.seeElement(locate('.nav-link').withText('Dotazníky'));

    DTE.selectOption('quizId', 'Answer_quiz');
    DTE.clickSwitch('showAllAnswers_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    I.see('AKÉ ZVIERA JE NA OBRÁZKU');
});