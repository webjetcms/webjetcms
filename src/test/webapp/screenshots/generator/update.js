Feature('settings.update');

Before(({ I, login }) =>{
    login('admin');
    I.amOnPage("/admin/v9/settings/update/");
});

Scenario('Screens', async ({ I, Document }) => {
    I.waitForElement("#version-selector");
    I.wait(1);

    I.click("2023.40");

    Document.screenshot("/sysadmin/update/main-page.png");

    Document.screenshotElement("#submitButton", "/sysadmin/update/submit-button.png");
    Document.screenshotElement("span.badge.bg-success", "/sysadmin/update/badge.png");

    I.resizeWindow(1300, 1300);
    Document.screenshotElement("#UPLOAD_selector", "/sysadmin/update/upload-selector.png");

    I.resizeWindow(1000, 800);
    I.click("Aktualizovať zo súboru");
    I.wait(1);

    Document.screenshot("/sysadmin/update/upload-page.png");

    Document.screenshotElement("button.btn.btn-sm.btn-primary", "/sysadmin/update/file-submit-button.png");
});