Feature('webpages.publishing');

var folder_name, subfolder_one, subfolder_two, auto_webPage, randomNumber, auto_title;

const publishableWebpageUrl = "/test-stavov/test-casoveho-publikovania.html";
const uncheckedPublishableWebpageUrl = "/test-stavov/test-casoveho-publikovania-nezaskrnute.html";

Before(({ I, login }) => {
     login('admin');
     if (typeof folder_name == "undefined") {
          randomNumber = I.getRandomText();
          I.say("randomNumber="+randomNumber);
          folder_name = 'name-autotest-' + randomNumber;
          subfolder_one = 'subone-autotest-' + randomNumber;
          subfolder_two = 'subtwo-autotest-' + randomNumber;
          auto_webPage = 'webPage-autotest-' + randomNumber;
          auto_title = 'title-autotest-' + randomNumber;
     }
});

Scenario('overit notifikacie pri publikovani so zadanym datumom zaciatku', ({ I, DTE }) => {
     const docId = 22955;
     I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
     DTE.waitForEditor();

     I.waitForElement(".toast-close-button", 10);
     I.see("Existuje rozpracovaná alebo neschválená verzia tejto stránky");
     I.clickCss(".toast-close-button");
     I.dontSee("Existuje rozpracovaná alebo neschválená verzia tejto stránky");

     I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
     I.clickCss('#trEditor');
     I.type('<!-- This is an autotest -->');

     I.say("Set webpage to be published after pre-set date");
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     I.checkOption("#DTE_Field_editorFields-publishAfterStart_0");

     DTE.save();

     I.say("Verify that notification is shown");
     I.waitForText("Stránka bola uložená, bude automaticky publikovaná do verejnej časti web stránky 01.12.2030 6:00:00", 10, "div.toast-message");
     I.clickCss("button.toast-close-button");

     I.say("Check that actual content of webpage does not changed");
     I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
     DTE.waitForEditor();

     I.switchTo(".cke_wysiwyg_frame.cke_reset");
     I.see('Test publikovania');
     I.dontSee('<!-- This is an autotest -->');
     I.switchTo();
});

async function setPublishPageDefault(webpageText, webpageTitle, I, DTE, docId) {
     I.say(`nastav zakladny text stranky id:${docId}, ak by predchadzajuci beh padol`);
     I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
     DTE.waitForEditor();
     I.see(webpageTitle, "#datatableInit_modal div.DTE_Header h5.modal-title");

     await DTE.fillCkeditor("<p>"+webpageText+"</p>");

     I.clickCss("#pills-dt-datatableInit-basic-tab");
     I.checkOption("#DTE_Field_available_0");
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     DTE.fillField("publishStartDate", "");
     I.uncheckOption("#DTE_Field_editorFields-publishAfterStart_0");
     DTE.fillField("publishEndDate", "");
     I.uncheckOption("#DTE_Field_editorFields-disableAfterEnd_0");
     DTE.save();

     if(docId === 22956)
          I.amOnPage(publishableWebpageUrl);
     else
          I.amOnPage(uncheckedPublishableWebpageUrl);

     I.waitForText(webpageText, 10);
}

Scenario('overit ze s casovym publikovanim sa stranka ulozi a zobrazi v historii a nasledne vypublikuje @singlethread @current', async ({ I, DT, DTE, Document }) => {
     const webpageTitle = "Test casoveho publikovania";
     const docId = 22956;
     const webpageText = `Test casoveho publikovania stranky ID:${docId}`;
     const publishAfterMinutes = 3;
     const keepPublishedMinutes = 10;

     await setPublishPageDefault(webpageText, webpageTitle, I, DTE, docId);

     I.say("nastav datum publikovania now+2 minuty");
     I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
     DTE.waitForEditor();
     I.see(webpageTitle, "#datatableInit_modal div.DTE_Header h5.modal-title");

     I.say("Nastavujem casove publikovanie a vypnutie zobrazovania")
     let startTime = (new Date()).getTime();
     let publishStartTime = startTime + (publishAfterMinutes * 60 * 1000);
     let publishEndTime = startTime + ( (publishAfterMinutes + keepPublishedMinutes) * 60 * 1000);
     let publishStartDateText = I.formatDateTime(publishStartTime);
     let publishEndDateText = I.formatDateTime(publishEndTime);
     let webpageTextPublish = `Test vypublikovania ID:${docId} at ${publishStartDateText}`;

     await DTE.fillCkeditor("<p>" + webpageTextPublish + "</p>");
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     DTE.fillField("publishStartDate", publishStartDateText);
     I.pressKey("Tab");
     I.checkOption("#DTE_Field_editorFields-publishAfterStart_0");
     DTE.fillField("publishEndDate", publishEndDateText);
     I.pressKey("Tab");
     I.checkOption("#DTE_Field_editorFields-disableAfterEnd_0");
     DTE.save();

     I.say("over zobrazenie info spravy");
     I.see("Stránka bola uložená, bude automaticky publikovaná do verejnej časti web stránky", "div.toast-message");
     I.toastrClose();

     I.wait(2);

     I.say("Overujem zobrazenie v historii");
     I.click(webpageTitle, "#datatableInit_wrapper");
     DTE.waitForEditor();
     I.wait(5);
     I.click("Editovať poslednú verziu", "#toast-container-webjet");
     I.waitForInvisible("#toast-container-webjet", 200);
     I.wait(2);
     DTE.waitForEditor();
     I.wait(5);
     I.clickCss("#pills-dt-datatableInit-history-tab");
     I.waitForInvisible("#datatableFieldDTE_Field_editorFields-history_processing", 200);

     //v history tabulke v stlpci Bude publikovane
     I.waitForText(publishStartDateText, 20, "#datatableFieldDTE_Field_editorFields-history tr td:nth-child(3)");
     I.see(publishStartDateText, "#datatableFieldDTE_Field_editorFields-history tr td:nth-child(3)");
     I.see(publishEndDateText, "#datatableFieldDTE_Field_editorFields-history tr td:nth-child(4)");

     await checkAudit(I, DT, Document, webpageText);

     I.say("over zobrazenie povodneho textu, pockaj 2 minuty a over novy text");
     I.amOnPage(publishableWebpageUrl);
     I.waitForText(webpageText, 10);
     I.dontSee(webpageTextPublish);

     await I.waitForTime(publishStartTime);
     I.wait(5);
     I.amOnPage(publishableWebpageUrl);
     I.waitForText(webpageTextPublish, 10);
     I.dontSee(webpageText);

     I.say("Overujem nastavenie checkboxu v stranke");
     I.amOnPage("/admin/");
     I.relogin('admin');
     I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
     DTE.waitForEditor();
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     I.seeCheckboxIsChecked("#DTE_Field_editorFields-disableAfterEnd_0");

     await checkAudit(I, DT, Document, webpageTextPublish, true);

     I.say("over vypnutie zobrazovania");
     await I.waitForTime(publishEndTime);
     I.wait(5);
     I.amOnPage(publishableWebpageUrl);
     I.see("Chyba 404 - požadovaná stránka neexistuje");
});

Scenario('overenie spravania stranky pri nezaskrtnutych casovych moznostiach publikovania @singlethread', async ({ I, DTE, DT }) => {
     const webpageTitle = "Test casoveho publikovania nezaskrnute";
     const docId = 106117;
     const webpageText = `Test casoveho publikovania stranky ID:${docId}`;
     const publishAfterMinutes = 3;
     const keepPublishedMinutes = 2;

     await setPublishPageDefault(webpageText, webpageTitle, I, DTE, docId);

     I.say("nastav datum publikovania now+2 minuty");
     I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
     DTE.waitForEditor();
     I.see(webpageTitle, "#datatableInit_modal div.DTE_Header h5.modal-title");

     I.say("Nastavujem casove publikovanie a vypnutie zobrazovania")
     let startTime = (new Date()).getTime();
     let publishStartTime = startTime + (publishAfterMinutes * 60 * 1000);
     let publishEndTime = startTime + ( (publishAfterMinutes + keepPublishedMinutes) * 60 * 1000);
     let publishStartDateText = I.formatDateTime(publishStartTime);
     let publishEndDateText = I.formatDateTime(publishEndTime);
     let webpageTextPublish = `Test vypublikovania ID:${docId} at ${publishStartDateText}`;

     await DTE.fillCkeditor("<p>" + webpageTextPublish + "</p>");
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     DTE.fillField("publishStartDate", publishStartDateText);
     I.pressKey("Tab");
     DTE.fillField("publishEndDate", publishEndDateText);
     I.pressKey("Tab");

     DTE.save();
     I.wait(2);

     I.say("Overujem zobrazenie v historii");
     DT.filterId('id', docId);
     I.clickCss('td.dt-row-edit.required');
     DTE.waitForEditor();
     I.clickCss("#pills-dt-datatableInit-history-tab");
     I.waitForInvisible("#datatableFieldDTE_Field_editorFields-history_processing", 200);

     //v history tabulke v stlpci Bude publikovane
     I.waitForText("Bude publikované");
     I.dontSee(publishStartDateText, "#datatableFieldDTE_Field_editorFields-history tr td:nth-child(3)");
     I.dontSee(publishStartDateText, "#datatableFieldDTE_Field_editorFields-history tr td:nth-child(3)");
     I.dontSee(publishEndDateText, "#datatableFieldDTE_Field_editorFields-history tr td:nth-child(4)");

     I.logout();

     I.say("over zobrazenie povodneho textu, pockaj 2 minuty a nezmeneny text");
     I.amOnPage(uncheckedPublishableWebpageUrl);
     I.waitForText(webpageTextPublish, 10);
     I.dontSee(webpageText);

     await I.waitForTime(publishStartTime);
     I.wait(5);
     I.amOnPage(uncheckedPublishableWebpageUrl);
     I.waitForText(webpageTextPublish, 10);
     I.dontSee(webpageText);

     I.say("Overujem nastavenie checkboxu v stranke");
     I.amOnPage("/admin/");
     I.relogin('admin');
     I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
     DTE.waitForEditor();
     I.clickCss("#pills-dt-datatableInit-perex-tab");
     I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-disableAfterEnd_0");

     I.logout();

     I.say("over nevypnutie zobrazovania");
     await I.waitForTime(publishEndTime);
     I.wait(5);
     I.amOnPage(uncheckedPublishableWebpageUrl);
     I.dontSee(webpageText);
     I.see(webpageTextPublish);
});

Scenario('casove odpublikovanie existujucej stranky @singlethread', async ({ I, DTE }) => {
     const webpageTitle = "Test casoveho publikovania";
     const docId = 22956;
     const webpageText = `Test casoveho publikovania stranky ID:${docId}`;
     const publishAfterMinutes = 3;

     await setPublishPageDefault(webpageText, webpageTitle, I, DTE, docId);

     I.say("Nastavujem vypnutie zobrazovania")
     I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${docId}`);
     DTE.waitForEditor();

     let startTime = (new Date()).getTime();
     let publishEndTime = startTime + (publishAfterMinutes * 60 * 1000);
     let publishEndDateText = I.formatDateTime(publishEndTime);

     I.clickCss("#pills-dt-datatableInit-perex-tab");
     DTE.fillField("publishEndDate", publishEndDateText);
     I.pressKey("Tab");
     I.checkOption("#DTE_Field_editorFields-disableAfterEnd_0");
     DTE.save();

     I.say("Overujem zobrazenie v historii");
     I.click(webpageTitle);
     DTE.waitForEditor();
     I.clickCss("#pills-dt-datatableInit-history-tab");
     I.waitForInvisible("#datatableFieldDTE_Field_editorFields-history_processing", 200);

     //v history tabulke v stlpci Bude publikovane
     I.waitForText(publishEndDateText, 30, "#datatableFieldDTE_Field_editorFields-history tr td:nth-child(4)");

     I.logout();

     I.say("over zobrazenie povodneho textu, pockaj 2 minuty a over novy text");
     I.amOnPage(publishableWebpageUrl);
     I.see(webpageText);

     I.say("over vypnutie zobrazovania");
     await I.waitForTime(publishEndTime);
     I.wait(5);
     I.amOnPage(publishableWebpageUrl);
     I.see("Chyba 404 - požadovaná stránka neexistuje");
});

async function createPublishPage(title, I, DTE, DT) {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=67');
     I.click(DT.btn.add_button);
     DTE.waitForEditor();

     DTE.fillField('title', title);
     I.clickCss('#pills-dt-datatableInit-content-tab');
     await DTE.fillCkeditor("<p> Tato stranka bude odpublikovana v buducnosti konkretne zajtra </p>");

     I.clickCss("#pills-dt-datatableInit-basic-tab");
     I.checkOption("#DTE_Field_available_0");
     I.clickCss("#pills-dt-datatableInit-perex-tab");

     const tomorrow = new Date();
     tomorrow.setDate(tomorrow.getDate() + 1);

     const pad = num => num.toString().padStart(2, '0');

     const formattedDate = `${pad(tomorrow.getDate())}.${pad(tomorrow.getMonth() + 1)}.${tomorrow.getFullYear()} ` +
                           `${pad(tomorrow.getHours())}:${pad(tomorrow.getMinutes())}:${pad(tomorrow.getSeconds())}`;

     I.checkOption("#DTE_Field_editorFields-publishAfterStart_0");
     DTE.fillField("publishStartDate", formattedDate);
     DTE.save();
}

Scenario('Webpages - Future publication validation and deletion', async ({ I, DTE, DT }) => {
     await createPublishPage(auto_title, I, DTE, DT);
     DT.filterContains('title', auto_title);
     I.clickCss('.buttons-select-all');
     I.click(DT.btn.edit_button);
     DTE.waitForEditor();

     // Overenie notifikácie o budúcej publikácii
     I.see("Upozornenie", "div.toast-title");
     I.see("Stránka bola uložená, bude automaticky publikovaná do verejnej časti web stránky", "div.toast-message");
     DTE.cancel();
     I.toastrClose();

     // Mazanie stránky
     I.click(DT.btn.delete_button);
     I.click("Zmazať", "div.DTE_Action_Remove");
     I.dontSeeElement('.DTE_Form_Error');
});

async function checkAudit(I, DT, Document, text, isDisabled = false) {
     I.amOnPage("/admin/v9/apps/audit-awaiting-publish-webpages/");
     let currentUrl = await I.grabCurrentUrl();
     DT.filterContains("title", "Test casoveho publikovania");
     I.clickCss("td.dt-select-td.sorting_1");
     I.clickCss("button.buttons-history-preview");
     if (isDisabled) {
          const classes = await I.grabAttributeFrom('.dt-scroll-body > table > tbody > tr', 'class');
          I.assertContain(classes, 'is-disabled');
     }
     I.wait(2);
     I.switchToNextTab();

     //on iwcm.interway.sk host change domain
     await Document.fixLocalhostUrl(currentUrl)

     I.see("TEST CASOVEHO PUBLIKOVANIA", "div.container h1");
     I.see(text, "div.container p");
     I.closeCurrentTab();
     I.logout();
}
