Feature('apps.dmail.unsubscribed-approval');

var baseUrl, randomName_1, randomName_2, randomName_3, randomName_4;
const defaultText = 'Ľutujeme, že odchádzate! Ak zostanete prihlásený, budete mať prístup k exkluzívnym novinkám, aktualizáciám a špeciálnym informáciám. Ste si istý, že chcete pokračovať v odhlásení?';
const changedText = 'Toto je zmeneny text';
let linksFromEmail = [];

Before(async ({ I, DT, login }) => {
    login('admin');
    if (typeof randomName_1 == "undefined") {
        let randomNumber1 = I.getRandomText();
        let randomNumber2 = I.getRandomText();
        let randomNumber3 = I.getRandomText();
        let randomNumber4 = I.getRandomText();
        randomName_1 = `autotest-${randomNumber1}`;
        randomName_2 = `autotest-${randomNumber2}`;
        randomName_3 = `autotest-${randomNumber3}`;
        randomName_4 = `autotest-${randomNumber4}`;
        baseUrl = await I.grabCurrentUrl();
   }
   DT.addContext('recipients','#datatableFieldDTE_Field_recipientsTab_wrapper');
});

Scenario('Preparation - create random subscribers', ({ I, DT, DTE }) => {
    I.amOnPage('/apps/dmail/admin/?id=2744');
    DTE.waitForEditor('campaingsDataTable')
    I.clickCss('#pills-dt-campaingsDataTable-receivers-tab');
    DT.waitForLoader();

    [randomName_1, randomName_2, randomName_3, randomName_4].forEach(randomName => {
        I.click(DT.btn.recipients_add_button);
        DTE.waitForEditor('datatableFieldDTE_Field_recipientsTab')
        DTE.fillField('recipientEmail', randomName + '@fexpost.com');
        DTE.save('datatableFieldDTE_Field_recipientsTab');
    });
    DT.filterContains('recipientEmail', 'autotest-');
    I.clickCss('#datatableFieldDTE_Field_recipientsTab_wrapper button.buttons-select-all');
    I.click(DT.btn.recipients_resend_button);
    I.waitForElement('#toast-container-webjet');
    I.clickCss('[id^="confirmationYes"]', "#toast-container-webjet");
    DTE.save('campaingsDataTable');
});


Scenario('Test unsubscribe text - default, empty, edited', async ({ Apps, DTE, I }) => {
    await setUnsubscribeText(Apps, DTE, I, defaultText);

    I.amOnPage('/newsletter/odhlasenie-z-newsletra.html');
    I.waitForElement('.ly-content .container', 10);
    I.fillField('#unsubscribeEmail', randomName_1+'@fexpost.com');
    I.seeElement(locate('a').withText('Nie, chcem zostať'));
    I.see(defaultText);
    I.click(locate('.bSubmit').withAttr({'value':'Odhlásiť sa z odberu'}));
    I.waitForElement(locate('fieldset > .alert.alert-success').withText('Email úspešne odhlásený.'), 10);

    await setUnsubscribeText(Apps, DTE, I, '');
    I.amOnPage('/newsletter/odhlasenie-z-newsletra.html');
    I.waitForElement('.ly-content .container', 10);
    I.fillField('#unsubscribeEmail', randomName_2+'@fexpost.com');
    I.dontSeeElement(locate('a').withText('Nie, chcem zostať'));
    I.dontSee(defaultText);
    I.click(locate('.bSubmit').withAttr({'value':'Odhlásiť sa z odberu'}));
    I.waitForElement(locate('fieldset > .alert.alert-success').withText('Email úspešne odhlásený.'), 10);

    await setUnsubscribeText(Apps, DTE, I, changedText);
    I.amOnPage('/newsletter/odhlasenie-z-newsletra.html');
    I.waitForElement('.ly-content .container', 10);
    I.see(changedText);
});

Scenario('Preparation - set confirmation to false', async ({ I, Apps, DTE }) => {
    await setConfirmation(Apps, DTE, I, false);
});

Scenario('Email - unsubscribe without confirmation', async ({ I, Document, TempMail }) => {
    I.logout();
    I.closeOtherTabs();
    I.switchTo();

    TempMail.login(randomName_3);
    TempMail.openLatestEmail();

    I.click(locate('#info a').withText('odhlásenie'));
    await Document.waitForTab();
    I.switchToNextTab();

    await Document.fixLocalhostUrl(baseUrl);
    linksFromEmail.push(await I.grabCurrentUrl());

    I.waitForText(changedText);
    I.dontSeeElement(locate('a').withText('Nie, chcem zostať'));
    I.seeElement(locate('input').withAttr({'value':'Obnoviť odber'}));
    I.see('Email úspešne odhlásený.');
});

Scenario('Preparation - set confirmation to true', async ({ I, Apps, DTE }) => {
    await setConfirmation(Apps, DTE, I, true);
});

Scenario('Email - unsubscribe with confirmation', async ({ I, Document, TempMail }) => {
    I.logout();
    TempMail.login(randomName_4);
    TempMail.openLatestEmail();
    I.click(locate('#info a').withText('odhlásenie'));
    await Document.waitForTab();
    I.switchToNextTab();
    await Document.fixLocalhostUrl(baseUrl);
    linksFromEmail.push(await I.grabCurrentUrl());
    I.waitForText(changedText, 10);
    I.seeElement(locate('a').withText('Nie, chcem zostať'));
    I.seeElement(locate('input#unsubscribeEmail').withAttr({'readonly':'readonly'}));
    I.click(locate('.bSubmit').withAttr({'value':'Odhlásiť sa z odberu'}));
    I.waitForText('Email úspešne odhlásený.', 10);
});

Scenario('Verify if the emails are in unsubscribed mails', ({ Apps, DT, I }) => {
    Apps.openAppEditor('87513');
    I.clickCss('#pills-dt-component-datatable-unsubscribed-tab');
    DT.waitForLoader();
    [randomName_1, randomName_2, randomName_3, randomName_4].forEach(randomName => {
        DT.filterContains('email', randomName + '@fexpost.com');
        I.waitForText('Záznamy 1 až 1 z 1', 20);
        DT.checkTableCell('datatableFieldDTE_Field_unsubscribedEmails', 1, 2, randomName + '@fexpost.com');
    });
});

Scenario('Check if unsubscription can be reverted', ({ I, Apps, DT }) => {
    I.logout();
    I.say("Revert unsubscription, linksFromEmail.length=" + linksFromEmail.length);
    linksFromEmail.forEach((linkFromEmail) => {
        I.say("Revert unsubscription, linkFromEmail=" + linkFromEmail);
        I.amOnPage(linkFromEmail);
        I.click(locate('.bSubmit').withAttr({'value':'Odhlásiť sa z odberu'}));
        I.waitForText('Email úspešne odhlásený.', 10);
        I.click(locate('.bSubmit').withAttr({'value':'Obnoviť odber'}));
        I.waitForText("Boli ste znovu prihlásený k odberu noviniek!");
    })
    I.relogin('admin');
    Apps.openAppEditor('87513');
    I.clickCss('#pills-dt-component-datatable-unsubscribed-tab');
    DT.waitForLoader();
    [randomName_3, randomName_4].forEach(randomName => {
        DT.filterContains('email', randomName + '@fexpost.com');
        I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 20);
    });

});

Scenario('Revert - remove autotest subscribers and set default unsubscribe text', async ({ I, Apps, DT, DTE }) => {
    I.amOnPage('/apps/dmail/admin/?id=2744');
    DTE.waitForEditor('campaingsDataTable')
    I.clickCss('#pills-dt-campaingsDataTable-receivers-tab');
    DT.waitForLoader();
    DT.filterContains('recipientEmail', 'autotest-');
    I.clickCss('#datatableFieldDTE_Field_recipientsTab_wrapper button.buttons-select-all');
    I.click(DT.btn.recipients_delete_button);
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 30);
    DTE.save('campaingsDataTable');

    await setUnsubscribeText(Apps, DTE, I, defaultText);
});

async function setConfirmation(Apps, DTE, I, shouldBeChecked ){
    I.closeOtherTabs();
    I.switchTo();
    Apps.openAppEditor('87513');

    if (shouldBeChecked) I.checkOption('#DTE_Field_confirmUnsubscribe_0');
    else I.uncheckOption('#DTE_Field_confirmUnsubscribe_0');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    DTE.save();
}

async function setUnsubscribeText(Apps, DTE, I, unsubscribeText){
    Apps.openAppEditor('87513');
    //DTE.fillQuill('confirmUnsubscribeText', unsubscribeText);
    await fillUnsubscribeQuill(I, unsubscribeText);
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');
    DTE.save();
}

async function fillUnsubscribeQuill(I, value){
    I.switchTo();
    await I.executeScript((value) => {
        const selector = '#DTE_Field_confirmUnsubscribeText > div.editor.ql-container.ql-snow > div.ql-editor > p';
        document.querySelector(".cke_dialog_ui_iframe").contentWindow.document.querySelector("#editorComponent").contentWindow.document.querySelector(selector).innerText = value;
    }, value);
    I.switchTo("#editorComponent")
}