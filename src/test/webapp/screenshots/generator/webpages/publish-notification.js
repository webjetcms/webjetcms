const moment = require("moment");

Feature('webpage.publish-notification');

const testWebpageAdmin = "/admin/v9/webpages/web-pages-list/?docid=104831";
const testWebpageShow = "/test-stavov/test_publish_notification.html";
const format = "DD.MM.YYYY HH:mm";
const adminlogPublishMsg = "publishStatus: Webpage was published";

let randomNumber;

Before(({ I }) => {
    if (typeof randomNumber=="undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('Publish notification screens', async ({ I, DT, DTE, TempMail, Document }) => {
    I.relogin("publishNotification");
    I.say("Set publish action");
    I.amOnPage(testWebpageAdmin);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.waitForVisible("#DTE_Field_publishStartDate");

    let date = moment().add(2, 'minutes');
    I.checkOption("#DTE_Field_editorFields-publishAfterStart_0");
    I.fillField("#DTE_Field_publishStartDate", date.format(format) + ":00");
    DTE.save();

    I.say("NOW wait for publish time");
    I.amOnPage(testWebpageShow);
    await I.waitForTime(date.valueOf());
    I.wait(5);
    I.amOnPage(testWebpageShow);

    I.say("Check send mail");
    TempMail.login('webjetcmsnotif');
    TempMail.openLatestEmail();

    Document.screenshotElement("div.letter", "/redactor/webpages/editor/publish-email-notification.png");

    TempMail.deleteCurrentEmail();

    I.say('Check that admin log was created correctly for this action');
    I.amOnPage('/admin/v9/apps/audit-search/');
    DT.filterSelect("logType", "SAVEDOC");
    DT.filterContains("from-createDate", date.format(format) + ":00");
    DT.filterContains("description", adminlogPublishMsg);

    Document.screenshot("/redactor/webpages/editor/publish-audit-logs.png");
});

Scenario('logout', async ({ I }) => {
    I.logout();
});