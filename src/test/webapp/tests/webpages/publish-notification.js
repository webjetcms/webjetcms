const moment = require("moment");

Feature('webpages.publish-notification');

const testWebpageAdmin = "/admin/v9/webpages/web-pages-list/?docid=104831";
const testWebpageShow = "/test-stavov/test_publish_notification.html";
const beforePublish = "Before publish content";
const afterPublish = "After publish content";
const format = "DD.MM.YYYY HH:mm";
const adminlogPublishMsg = "publishStatus: Webpage was published";

let randomNumber;

Before(({ I }) => {
    I.relogin('publishNotification');
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('Publicable notification test + audit logs', async ({ I, DT, DTE, TempMail, Document }) => {
    I.say("Set base body");
    await setBodyContent(I, DTE, beforePublish + randomNumber);

    I.say("Check content on page");
    I.amOnPage(testWebpageShow);
    I.waitForText(beforePublish + randomNumber, 10);

    I.say("Change page body");
    await setBodyContent(I, DTE, afterPublish + randomNumber, false);

    I.say('Add publish date');
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.waitForVisible("#DTE_Field_publishStartDate");

    let date = moment().add(2, 'minutes');
    I.checkOption("#DTE_Field_editorFields-publishAfterStart_0");
    I.fillField("#DTE_Field_publishStartDate", date.format(format) + ":00");
    DTE.save();

    I.say("Check that body of webpage was not changed YET");
    I.amOnPage(testWebpageShow);
    I.waitForText(beforePublish + randomNumber, 10);
    I.dontSee(afterPublish + randomNumber);

    I.say("NOW wait for publish time");
    await I.waitForTime(date.valueOf());
    I.wait(5);

    I.say("Now after publish check, that content WAS CHANGED");
    I.amOnPage(testWebpageShow);
    I.waitForText(afterPublish + randomNumber, 10);
    I.dontSee(beforePublish + randomNumber);

    let currentUrl = await I.grabCurrentUrl();

    I.say("Check send mail");
    TempMail.login('webjetcmsNotif');
    TempMail.openLatestEmail();
    I.see("Publikovaná stránka");
    I.see("Vaša stránka test_publish_notification (ID: 104831) bola práve publikovaná, podľa nastaveného termínu na " + date.format(format) + ". Pre zobrazenie stránky kliknite na nasledujúci odkaz:");
    I.waitForElement(locate("a").withText("test_publish_notification"), 10);

    //
    I.say("Test link to page");
    I.click(locate("a").withText("test_publish_notification"));
    I.wait(2);
    TempMail.deleteCurrentEmail();
    I.switchToNextTab();

    //on iwcm.interway.sk host change domain
    await Document.fixLocalhostUrl(currentUrl)

    I.waitForText(afterPublish + randomNumber, 10);
    I.closeCurrentTab();

    I.say('Check that admin log was created correctly for this action');
    I.amOnPage('/admin/v9/apps/audit-search/');
    DT.filterSelect("logType", "SAVEDOC");
    DT.filter("from-createDate", date.format(format) + ":00");
    DT.filter("description", adminlogPublishMsg);
    I.see("UPDATE: id: 104831");
});

async function setBodyContent(I, DTE, content, doSave = true) {
    I.amOnPage(testWebpageAdmin);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-content-tab");
    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(content);

    if(doSave == true) DTE.save();
}