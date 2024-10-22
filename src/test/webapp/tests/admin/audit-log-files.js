Feature('admin.audit-log-files');

Before(({ login }) => {
    login('admin');
});

Scenario("Test of log file", async ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/audit-log-files/");

    DT.waitForLoader();

    const fileFullPath = await I.grabTextFrom("table#datatableInit > tbody > tr:nth-child(1) > td:nth-child(1) > div > a");
    const logFolder = await I.grabTextFrom("#absolutePath");

    let separator = "/";
    if (logFolder.indexOf("\\") !== -1) separator = "\\";

    I.click(fileFullPath);
    I.waitForElement("#modalIframe");
    within("#modalIframe > div.modal-dialog", () => {
        I.seeElement( locate("h5.modal-title").withText(logFolder + separator + fileFullPath.split(" ")[0]) );
        I.waitForElement("#modalIframeIframeElement");
        I.switchTo("#modalIframeIframeElement");
            I.waitForText("Exp", 20);
            I.waitForText("Riadky pred", 20);
            I.see("Riadky po");
    });
});