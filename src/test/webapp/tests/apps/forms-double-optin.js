Feature('apps.forms-double-optin');

var username = 'name.autotest.' ;
var text = 'Dobry den, toto je skusobny text';
var randomNumber;
var date;
var email;

Before(({ I, TempMail }) => {
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        username += randomNumber;
        text += "-"+randomNumber;
        email = username + TempMail.getTempMailDomain();
        date = I.formatDate(new Date().getTime());
    }
});

Scenario("Formular @singlethread", async ({ I, DT, TempMail}) => {
    I.say('Fill formular with testing data');
    I.amOnPage("/apps/formular/formular-doubleoptin.html");
    I.waitForElement('form[name="formMailForm"]');
    I.fillField('input[name="meno"]', username);
    I.clearField('input[name="email"]');
    I.fillField('input[name="email"]', email);
    I.fillField('textarea[name="otazka"]', text);
    I.click('form[name=formMailForm] input[name="bSubmit"]');

    I.waitForText("Formulár bol odoslaný, na váš email sme odoslali správu, v ktorej je potrebné potvrdiť odoslanie kliknutím na odkaz", 10);

    I.say('Open webjet e-mail and check if fields were sent successfuly');
    await TempMail.login('webjetcms');
    TempMail.openLatestEmail();
    I.see('Formulár-doubleoptin');
    I.see(username);
    I.see(email);
    I.see(text);
    TempMail.deleteCurrentEmail();

    //
    I.say('Check if e-mail is NOT confirmed');
    checkEmailConfirmation(I, DT, false);

    //
    I.say('Confirm e-mail');
    await confirmEmail(TempMail, I);
    I.waitForText('Potvrdenie súhlasu úspešné', 10);
    I.see("Vaše potvrdenie súhlasu so spracovaním osobných údajov bolo úšpešné");

    //
    I.say('Check if e-mail is confirmed');
    checkEmailConfirmation(I, DT, true);

    //
    I.say('Try to confirm e-mail again');
    await confirmEmail(TempMail, I);
    I.waitForText('Potvrdenie súhlasu už bolo zaznamenané', 10);
    I.see("Vaše potvrdenie súhlasu so spracovaním osobných údajov už bolo zaznamenané");
});


Scenario("Delete testing data @singlethread", async ({ I, DT, DTE, TempMail }) => {
    await TempMail.login(username);
    await TempMail.destroyInbox();

    I.relogin('admin');
    I.amOnPage('/apps/form/admin/#/detail/Formular-doubleoptin');
    DT.waitForLoader();
    DT.filterStartsWith("col_meno", username);
    I.clickCss('.buttons-select-all');

    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForModalClose("forumDataTable_modal");
    DT.waitForLoader();
});

async function confirmEmail(TempMail, I) {
    await TempMail.login(username);
    TempMail.openLatestEmail();
    I.see('Stranka s textom e-mailu');
    I.clickCss('//a[text()="ODKAZ"]');
    I.switchToNextTab();
    I.closeOtherTabs();
}

function checkEmailConfirmation(I, DT, isConfirmed) {
    I.relogin('admin');
    I.amOnPage('/apps/form/admin/#/detail/Formular-doubleoptin');
    DT.waitForLoader();
    DT.filterStartsWith("col_meno", username);
    if (isConfirmed) {
        // if div is present e-mail was confirmed
        I.seeElement(locate('tr > td:nth-child(4) > div.datatable-column-width').withText(date));
        I.dontSeeElement("#form-detail tr.is-disabled");
    } else {
        I.seeElement("#form-detail tr.is-disabled");
        I.dontSeeElement('tr > td:nth-child(4) > div');
    }
}
